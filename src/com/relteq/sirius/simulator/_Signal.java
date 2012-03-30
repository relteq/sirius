/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;
import java.util.HashMap;

public final class _Signal extends com.relteq.sirius.jaxb.Signal {

	public static enum CommandType {hold,forceoff};
	protected static enum BulbColor {GREEN,YELLOW,RED,DARK};
	public static enum NEMA {NULL,_1,_2,_3,_4,_5,_6,_7,_8};
	
	protected HashMap<NEMA,SignalPhase> nema2phase;
	
	protected _Network myNetwork;

	protected _Node myNode;
	protected PhaseController myPhaseController;	// used to control capacity on individual links
	
	protected SignalPhase [] phase;
	
	// local copy of the command, subject to checks
	protected boolean [] hold_approved;
	protected boolean [] forceoff_approved;
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	/** @y.exclude */	
	protected void populate(_Scenario myScenario,_Network myNetwork) {
		
		this.myNetwork = myNetwork;
		this.myNode = myNetwork.getNodeWithId(getNodeId());

		int i;
		int totalphases = getPhase().size();
		
		if(totalphases==0)
			return;
		
		// ignore phases without targets, or !permissive && !protected
		// (instead of throwing validation error, because network editor 
		// generates phases like this).
		boolean [] isvalid = new boolean[totalphases];
		int numlinks;
		int numvalid = 0;
		for(i=0;i<getPhase().size();i++){
			com.relteq.sirius.jaxb.Phase p = getPhase().get(i);
			isvalid[i] = true;
			isvalid[i] &= p.isPermissive() || p.isProtected();
			if(p.getLinks()==null)
				numlinks = 0;
			else if(p.getLinks().getLinkReference()==null)
				numlinks = 0;
			else 
				numlinks = p.getLinks().getLinkReference().size();
			isvalid[i] &= numlinks>0;
			numvalid += isvalid[i] ? 1 : 0;
		}
		
		phase = new SignalPhase[numvalid];
		nema2phase = new HashMap<NEMA,SignalPhase>(numvalid);
		int c = 0;
		for(i=0;i<getPhase().size();i++){
			if(!isvalid[i])
				continue;
			phase[c] = new SignalPhase(myNode,this,myScenario.simdtinseconds);
			phase[c].populateFromJaxb(myScenario,getPhase().get(i));
			nema2phase.put(phase[c].myNEMA,phase[c]);
			c++;
		}
		
		hold_approved = new boolean[phase.length];
		forceoff_approved = new boolean[phase.length];
		
		// create myPhaseController. This is used to implement flow control on target links
		myPhaseController = new PhaseController(this);
		
		// nema2phase
		
		
	}

	protected void reset() {
		for(SignalPhase p : phase)
			p.reset();
	}
	
	protected boolean validate() {
		
		if(myNode==null){
			SiriusErrorLog.addErrorMessage("Incorrect node reference in signal.");
			return false;
		}
		
		if(phase==null){
			SiriusErrorLog.addErrorMessage("Signal contains no valid phases.");
			return false;
		}
						
		for(SignalPhase p : phase)
			if(!p.validate()){
				SiriusErrorLog.addErrorMessage("Invalid phase in signal.");
				return false;
			}
		
		return true;
	}

	protected void update() {

		int i;
		
		// 0) Advance all phase timers ...........................................
		for(SignalPhase p:phase)
			p.bulbtimer.advance();
		
		// 1) Update detector stations ............................................
		/*
		for(i=0;i<8;i++)
			phase.get(i).UpdateDetectorStations();
		*/
		
		// 2) Read phase calls .....................................................
/*
		// Update stopline calls
		for(i=0;i<8;i++){
			if( phase.get(i).Recall() ){
				hasstoplinecall[i] = true;
				continue;
			}
			if( phase.get(i).StoplineStation()!=null && phase.get(i).StoplineStation().GotCall() )
				hasstoplinecall[i] = true;
			else
				hasstoplinecall[i] = false;
		}

		// Update approach calls
		for(i=0;i<8;i++){
			if( phase.get(i).ApproachStation()!=null && phase.get(i).ApproachStation().GotCall() )
				hasapproachcall[i] = true;
			else
				hasapproachcall[i] = false;
		}

		// Update conflicting calls
		boolean[] currentconflictcall = new boolean[8];
		for(i=0;i<8;i++)
			currentconflictcall[i] = CheckForConflictingCall(i);
		for(i=0;i<8;i++){
			if(  !hasconflictingcall[i] && currentconflictcall[i] )
				conflictingcalltime[i] = (float)(myNode.getMyNetwork().getSimTime()*3600f);
			hasconflictingcall[i] = currentconflictcall[i];
		}	
*/	
		
		// 3) Update permitted holds ............................................
		for(SignalPhase pA:phase){
			pA.permithold = true;
			for(SignalPhase pB:phase)
				if(!isCompatible(pA,pB) && !pB.permitopposinghold )
					pA.permithold = false;
		}
		
		// 4) Update signal commands ...................................................
		
		// Throw away conflicting hold pairs 
		// (This is purposely drastic to create an error)
		for(SignalPhase pA:phase){
			if(pA.hold_requested){
				for(SignalPhase pB:phase){
					if( pB.hold_requested && !isCompatible(pA,pB) ){
						pA.hold_requested = false;
						pB.hold_requested = false;
					}
				}
			}
		}

		// Deal with simultaneous hold and forceoff (RHODES needs this)
		for(SignalPhase pA:phase){
			if( pA.hold_requested && pA.forceoff_requested ){
				pA.forceoff_requested = false;
			}
		}

		// Make local relaying copy
		for(i=0;i<phase.length;i++){
			hold_approved[i]     = phase[i].hold_requested;
			forceoff_approved[i] = phase[i].forceoff_requested;
		}

		// No transition if no permission
		for(i=0;i<phase.length;i++)
			if( !phase[i].permithold )
				hold_approved[i] = false;

		// No transition if green time < mingreen
		for(i=0;i<phase.length;i++)
			if( phase[i].bulbcolor.compareTo(BulbColor.GREEN)==0  && SiriusMath.lessthan(phase[i].bulbtimer.getT(),phase[i].mingreen) )
				forceoff_approved[i] = false;

		
		// Update all phases
		for(i=0;i<phase.length;i++)
			phase[i].update(hold_approved[i],forceoff_approved[i]);

		// Remove serviced commands 
		for(SignalPhase pA: phase){
			if(pA.bulbcolor.compareTo(_Signal.BulbColor.GREEN)==0)
				pA.hold_requested = false;
			if(pA.bulbcolor.compareTo(_Signal.BulbColor.YELLOW)==0 || pA.bulbcolor.compareTo(_Signal.BulbColor.RED)==0 )
				pA.forceoff_requested = false;
		}
	
		// Set permissive opposing left turn to yellow
		// opposing is yellow if I am green or yellow, and I am through, and opposing is permissive
		// opposing is red if I am red and it is not protected
		for(i=0;i<phase.length;i++){
			SignalPhase p = phase[i];
			SignalPhase o = phase[i].opposingPhase;
			if(o==null)
				continue;
			switch(p.bulbcolor){
				case GREEN:
				case YELLOW:
					if(p.isthrough && o.permissive)
						o.setPhaseColor(_Signal.BulbColor.YELLOW);
					break;
				case RED:
					if(!o.protectd)
						o.setPhaseColor(_Signal.BulbColor.RED);
					break;
			}
		}
		
		System.out.print(myNetwork.myScenario.getTimeInSeconds() + "\t");
		for(SignalPhase pA:phase){
			System.out.print(pA.myNEMA+":"+pA.getBulbcolor().toString() + "\t");
		}
		System.out.println("");
		
	}

	/////////////////////////////////////////////////////////////////////
	// protected
	/////////////////////////////////////////////////////////////////////
	
	protected boolean register(){
		return myPhaseController.register();
	}
	
	protected SignalPhase getPhaseForNEMA(NEMA nema){
		for(SignalPhase p:phase){
			if(p!=null)
				if(p.myNEMA.compareTo(nema)==0)
					return p;
		}
		return null;
	}
	
	/////////////////////////////////////////////////////////////////////
	// internal class
	/////////////////////////////////////////////////////////////////////
	
	// Each signal communicates with links via a PhaseController.
	// Phase controller does two things: a) it registers the signal control,
	// and b) it implements the phase indication. 
	class PhaseController extends _Controller {

		private HashMap<_Link,Integer> target2index;
		private HashMap<_Signal.NEMA,Integer[]> nema2indices;

		public PhaseController(_Signal mySignal){
			
			int i,j;
			
			// populate target2index
			int index = 0;
			target2index = new HashMap<_Link,Integer>();
			for(i=0;i<mySignal.phase.length;i++)
				for(j=0;j<mySignal.phase[i].targetlinks.length;j++)
					target2index.put(mySignal.phase[i].targetlinks[j],index++);
			
			// populate nema2indices
			nema2indices = new HashMap<_Signal.NEMA,Integer[]>();
			for(i=0;i<mySignal.phase.length;i++){
				Integer [] indices = new Integer[mySignal.phase[i].targetlinks.length];
				for(j=0;j<mySignal.phase[i].targetlinks.length;j++)
					indices[j] = target2index.get(mySignal.phase[i].targetlinks[j]);
				nema2indices.put(mySignal.phase[i].myNEMA,indices);
			}
			
			control_maxflow = new Double[target2index.size()];
		}
		
		@Override public void populate(Object jaxbobject) {}
		@Override public void update() throws SiriusException {}

		@Override
		public boolean register() {
	        for(_Link link : target2index.keySet())
	        	if(!link.registerFlowController(this,target2index.get(link)))
	        		return false;
			return true;
		}
		
		protected void setPhaseColor(_Signal.NEMA nema,_Signal.BulbColor color){
			
			Integer [] indices = nema2indices.get(nema);
			if(indices==null)
				return;
			
			double maxflow;
			switch(color){
				case GREEN:
				case YELLOW:
					maxflow = Double.POSITIVE_INFINITY;
					break;
				case RED:
				case DARK:
					maxflow = 0d;
					break;
				default:
					maxflow = 0d;
					break;			
			}
			
			for(Integer index:indices)
				control_maxflow[index] = maxflow;

		}
		
	}
	
	/////////////////////////////////////////////////////////////////////
	// public methods
	/////////////////////////////////////////////////////////////////////
	
	public SignalPhase getPhaseByNEMA(_Signal.NEMA nema){
		return nema2phase.get(nema);
	}

	public void requestCommand(ArrayList<_Signal.Command> command){
		for(_Signal.Command c : command){
			SignalPhase p = nema2phase.get(c.nema);
			if(p==null)
				continue;
			
			switch(c.type){
			case forceoff:
				p.forceoff_requested = true;
				p.actualyellowtime = c.yellowtime>=0 ? c.yellowtime : p.getYellowtime();
				p.actualredcleartime = c.redcleartime>=0 ? c.redcleartime : p.getRedcleartime();				
				break;

			case hold:
				p.hold_requested = true;
				break;
				
			}
		}
	}
	
	public String getMyNetworkId() {
		if(myNetwork!=null)
			return myNetwork.getId();
		else
			return null;
	}
	
	/////////////////////////////////////////////////////////////////////
	// static NEMA methods
	/////////////////////////////////////////////////////////////////////
	
	public static _Signal.NEMA String2NEMA(String str){
		if(str==null)
			return _Signal.NEMA.NULL;
		if(str.isEmpty())
			return _Signal.NEMA.NULL;
		if(!str.startsWith("_"))
			str = "_"+str;
		_Signal.NEMA nema;
		try{
			nema = _Signal.NEMA.valueOf(str);
		}
		catch(IllegalArgumentException  e){
			nema = _Signal.NEMA.NULL;
		}
		return nema;
	}
	
	public static boolean isCompatible(SignalPhase pA,SignalPhase pB)
	{
		_Signal.NEMA nemaA = pA.myNEMA;
		_Signal.NEMA nemaB = pB.myNEMA;
		
		if(nemaA.compareTo(nemaB)==0)
			return true;

		if( !pA.protectd || !pB.protectd )
			return true;

		switch(nemaA){
		case _1:
		case _2:
			if(nemaB.compareTo(NEMA._5)==0 || nemaB.compareTo(NEMA._6)==0)
				return true;
			else
				return false;
		case _3:
		case _4:
			if(nemaB.compareTo(NEMA._7)==0 || nemaB.compareTo(NEMA._8)==0 )
				return true;
			else
				return false;
		case _5:
		case _6:
			if(nemaB.compareTo(NEMA._1)==0 || nemaB.compareTo(NEMA._2)==0 )
				return true;
			else
				return false;
		case _7:
		case _8:
			if(nemaB.compareTo(NEMA._3)==0 || nemaB.compareTo(NEMA._4)==0 )
				return true;
			else
				return false;
		}
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	public static class Command implements Comparable {
		public _Signal.CommandType type;
		public _Signal.NEMA nema;
		public float time;
		public float yellowtime;
		public float redcleartime;

		public Command(_Signal.CommandType type,_Signal.NEMA phase,float time){
			this.type = type;
			this.nema = phase;
			this.time = time;
			this.yellowtime = -1f;
			this.redcleartime = -1f;
		}
		
		public Command(_Signal.CommandType type,_Signal.NEMA phase,float time,float yellowtime,float redcleartime){
			this.type = type;
			this.nema = phase;
			this.time = time;
			this.yellowtime = yellowtime;
			this.redcleartime = redcleartime;
		}
		
		@Override
		public int compareTo(Object arg0) {
			
			if(arg0==null)
				return 1;
			
			int compare;
			Command that = (Command) arg0;
			
			// first ordering by time stamp
			Float thiststamp = this.time;
			Float thattstamp = that.time;
			compare = thiststamp.compareTo(thattstamp);
			if(compare!=0)
				return compare;

			// second ordering by phase
			_Signal.NEMA thistphase = this.nema;
			_Signal.NEMA thattphase = that.nema;
			compare = thistphase.compareTo(thattphase);
			if(compare!=0)
				return compare;
			
			// third ordering by type
			CommandType thisttype = this.type;
			CommandType thatttype = that.type;
			compare = thisttype.compareTo(thatttype);
			if(compare!=0)
				return compare;

			// fourth ordering by yellowtime
			Float thistyellowtime = this.yellowtime;
			Float thattyellowtime = that.yellowtime;
			compare = thistyellowtime.compareTo(thattyellowtime);
			if(compare!=0)
				return compare;

			// fifth ordering by redcleartime
			Float thistredcleartime = this.redcleartime;
			Float thattredcleartime = that.redcleartime;
			compare = thistredcleartime.compareTo(thattredcleartime);
			if(compare!=0)
				return compare;
			
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj==null)
				return false;
			else
				return this.compareTo((Command) obj)==0;
		}
		
	}
	
}



