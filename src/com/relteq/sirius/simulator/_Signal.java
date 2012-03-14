/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class _Signal extends com.relteq.sirius.jaxb.Signal {
 
	protected static enum BulbColor {GREEN,YELLOW,RED,DARK};
	protected static enum NEMA {NULL,_1,_2,_3,_4,_5,_6,_7,_8};
	
	/** @y.exclude */ 	protected _Network myNetwork;

	private _Node myNode;
	//public BaseSignalController myController;
	
	private SignalPhase [] phase;

	// local copy of the command, subject to checks
	private boolean [] goG;
	private boolean [] goY;
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
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
		int c = 0;
		for(i=0;i<getPhase().size();i++){
			if(!isvalid[i])
				continue;
			phase[c]  = new SignalPhase(myNode,this,myScenario.simdtinseconds);
			phase[c].populateFromJaxb(myScenario,getPhase().get(i));
			c++;
		}
		
		goG = new boolean[phase.length];
		goY = new boolean[phase.length];
		
		// dual ring structure: opposingPhase, isthrough, myRignGroup
		for(SignalPhase p : phase){
			switch(p.myNEMA){
			case _1:
				p.opposingPhase = getPhaseForNEMA(NEMA._2);
				p.isthrough = false;
				p.myRingGroup = 0;
				break;
			case _2:
				p.opposingPhase =  getPhaseForNEMA(NEMA._1);
				p.isthrough = true;
				p.myRingGroup = 0;
				break;
			case _3:
				p.opposingPhase =  getPhaseForNEMA(NEMA._4);
				p.isthrough = false;
				p.myRingGroup = 1;
				break;
			case _4:
				p.opposingPhase =  getPhaseForNEMA(NEMA._3);
				p.isthrough = true;
				p.myRingGroup = 1;
				break;
			case _5:
				p.opposingPhase =  getPhaseForNEMA(NEMA._6);
				p.isthrough = false;
				p.myRingGroup = 0;
				break;
			case _6:
				p.opposingPhase = getPhaseForNEMA(NEMA._5);
				p.isthrough = true;
				p.myRingGroup = 0;
				break;
			case _7:
				p.opposingPhase = getPhaseForNEMA(NEMA._8);
				p.isthrough = false;
				p.myRingGroup = 1;
				break;
			case _8:
				p.opposingPhase = getPhaseForNEMA(NEMA._7);
				p.isthrough = true;
				p.myRingGroup = 1;
				break;
			}
			
		}
		
		
		
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
			if(pA.hold){
				for(SignalPhase pB:phase){
					if( pB.hold && !isCompatible(pA,pB) ){
						pA.hold = false;
						pB.hold = false;
					}
				}
			}
		}

		// Deal with simultaneous hold and forceoff (expected by RHODES)
		for(SignalPhase pA:phase){
			if( pA.hold && pA.forceoff ){
				pA.forceoff = false;
			}
		}

		// Make local relaying copy
		for(i=0;i<phase.length;i++){
			goG[i] = phase[i].hold;
			goY[i] = phase[i].forceoff;
		}

		// No transition if no permission
		for(i=0;i<phase.length;i++){
			if( !phase[i].permithold )
				goG[i] = false;
		}

		// No transition if green time < mingreen
		for(i=0;i<phase.length;i++){
			if( goY[i] && phase[i].bulbcolor==BulbColor.GREEN  && phase[i].bulbtimer.getT()<phase[i].mingreen-0.001){
				goY[i] = false;
			}
		}

		// Update all phases
		for(i=0;i<phase.length;i++)
			phase[i].processCommand(goG[i],goY[i]);

		// Remove serviced commands 
		for(SignalPhase pA: phase){
			if(pA.bulbcolor==_Signal.BulbColor.GREEN)
				pA.hold = false;
			if(pA.bulbcolor==_Signal.BulbColor.YELLOW || pA.bulbcolor==_Signal.BulbColor.RED)
				pA.forceoff = false;
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
						o.setYellow();
					break;
				case RED:
					if(!o.protectd)
						o.setRed();
					break;
			}
		}
		
	}

	/////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////
	
	private boolean isCompatible(SignalPhase pA,SignalPhase pB)
	{
		_Signal.NEMA nemaA = pA.myNEMA;
		_Signal.NEMA nemaB = pB.myNEMA;
		
		if(nemaA==nemaB)
			return true;

		if( !pA.protectd || !pB.protectd )
			return true;

		switch(nemaA){
		case _1:
		case _2:
			if(nemaB==NEMA._5 || nemaB==NEMA._6)
				return true;
			else
				return false;
		case _3:
		case _4:
			if(nemaB==NEMA._7 || nemaB==NEMA._8)
				return true;
			else
				return false;
		case _5:
		case _6:
			if(nemaB==NEMA._1 || nemaB==NEMA._2)
				return true;
			else
				return false;
		case _7:
		case _8:
			if(nemaB==NEMA._3 || nemaB==NEMA._4)
				return true;
			else
				return false;
		}
		return false;
	}
	
	private SignalPhase getPhaseForNEMA(NEMA nema){
		for(SignalPhase p:phase){
			if(p.myNEMA==nema)
				return p;
		}
		return null;
	}
	
}
