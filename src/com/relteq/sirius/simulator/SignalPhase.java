package com.relteq.sirius.simulator;

import com.relteq.sirius.simulator.Signal.NEMA;

public class SignalPhase {
	
	// references ....................................................
	protected Node myNode;
	protected Signal mySignal;
	protected Link [] targetlinks;	// THIS SHOULD BE TARGET INDICES TO THE SIGNAL PHASE CONTROLLER
	
	// properties ....................................................
	
	protected boolean protectd		= false;
	protected boolean isthrough		= false;
	protected boolean recall		= false;
	protected boolean permissive	= false;
	protected boolean lag 			= false;

	// dual ring structure
	protected int myRingGroup		= -1;
	protected SignalPhase opposingPhase;
	protected Signal.NEMA myNEMA   = Signal.NEMA.NULL;
	
	// Basic timing parameters
	protected float mingreen 			= 0f;
	protected float yellowtime 			= 0f;
	protected float redcleartime 		= 0f;
	protected float actualyellowtime 	= 0f;
	protected float actualredcleartime 	= 0f;

	// timers
	protected Clock bulbtimer;

	// State
	protected Signal.BulbColor bulbcolor;
	
	//protected int [] myControlIndex;

	// Detectors
	//protected DetectorStation ApproachStation = null;
	//protected DetectorStation StoplineStation = null;
	//protected Vector<Integer> ApproachStationIds;
	//protected Vector<Integer> StoplineStationIds;
	
	// Detector memory
	protected boolean hasstoplinecall		= false;
	protected boolean hasapproachcall		= false;
	protected boolean hasconflictingcall	= false;
	protected float conflictingcalltime		= 0f;

	// Controller memory
	protected boolean hold_requested 		= false;
	protected boolean forceoff_requested	= false;

	// Safety
	protected boolean permitopposinghold 	= true;
	protected boolean permithold			= true;

	protected int numapproachloops = 0;	
	
	public SignalPhase(Node myNode,Signal mySignal,double dt){
		this.myNode = myNode;
		this.mySignal = mySignal;
		this.bulbtimer = new Clock(0d,Double.POSITIVE_INFINITY,dt);		
	}
	
	protected final void populateFromJaxb(Scenario myScenario,com.relteq.sirius.jaxb.Phase jaxbPhase){
	
		int numlinks = jaxbPhase.getLinks().getLinkReference().size();
		this.targetlinks = new Link[numlinks];
		for(int i=0;i<numlinks;i++){
			com.relteq.sirius.jaxb.LinkReference linkref = jaxbPhase.getLinks().getLinkReference().get(i);
			targetlinks[i] = myScenario.getLinkWithId(linkref.getId());
		}
		
		if(jaxbPhase.getNema()!=null)
			myNEMA = Signal.String2NEMA(jaxbPhase.getNema().toString());
		else
			myNEMA = Signal.NEMA.NULL;
		
		if(jaxbPhase.getMinGreenTime()!=null)
			this.mingreen = jaxbPhase.getMinGreenTime().floatValue();
		else
			this.mingreen = Defaults.mingreen;
		
		if(jaxbPhase.getRedClearTime()!=null)
			this.redcleartime = jaxbPhase.getRedClearTime().floatValue();
		else
			this.redcleartime = Defaults.redcleartime;

		if(jaxbPhase.getYellowTime()!=null)
			this.yellowtime = jaxbPhase.getYellowTime().floatValue();
		else
			this.yellowtime = Defaults.yellowtime;

		this.lag = jaxbPhase.isLag();
		this.permissive = jaxbPhase.isPermissive();
		this.protectd = jaxbPhase.isProtected();
		this.recall = jaxbPhase.isRecall();
		
		// actual yellow and red clear times
		this.actualyellowtime   = yellowtime;
		this.actualredcleartime = redcleartime;
		
		
		// dual ring structure: opposingPhase, isthrough, myRingGroup
		switch(myNEMA){
		case _1:
			opposingPhase = mySignal.getPhaseForNEMA(NEMA._2);
			isthrough = false;
			myRingGroup = 0;
			break;
		case _2:
			opposingPhase = mySignal.getPhaseForNEMA(NEMA._1);
			isthrough = true;
			myRingGroup = 0;
			break;
		case _3:
			opposingPhase = mySignal.getPhaseForNEMA(NEMA._4);
			isthrough = false;
			myRingGroup = 1;
			break;
		case _4:
			opposingPhase = mySignal.getPhaseForNEMA(NEMA._3);
			isthrough = true;
			myRingGroup = 1;
			break;
		case _5:
			opposingPhase = mySignal.getPhaseForNEMA(NEMA._6);
			isthrough = false;
			myRingGroup = 0;
			break;
		case _6:
			opposingPhase = mySignal.getPhaseForNEMA(NEMA._5);
			isthrough = true;
			myRingGroup = 0;
			break;
		case _7:
			opposingPhase = mySignal.getPhaseForNEMA(NEMA._8);
			isthrough = false;
			myRingGroup = 1;
			break;
		case _8:
			opposingPhase = mySignal.getPhaseForNEMA(NEMA._7);
			isthrough = true;
			myRingGroup = 1;
			break;
		}		
	}
	
	protected void reset() {

		hasstoplinecall		= false;
		hasapproachcall		= false;
		hasconflictingcall	= false;
		conflictingcalltime	= 0f;
		hold_requested 		= false;
		forceoff_requested	= false;
		permithold			= true;
		permitopposinghold  = false;

		setPhaseColor(Signal.BulbColor.RED);
		bulbtimer.reset();
		
	}

	protected void validate() {

		// check that there are links attached
		if(targetlinks==null || targetlinks.length==0)
			SiriusErrorLog.addError("No valid target link for phase NEMA=" + getMyNEMA() + " in signal id=" + mySignal.getId());
		
		// target links are valid
		if(targetlinks!=null)
			for(int i=0;i<targetlinks.length;i++)
				if(targetlinks[i]==null)
					SiriusErrorLog.addError("Unknown link reference in phase NEMA=" + getMyNEMA() + " in signal id=" + mySignal.getId());
		
		// myNEMA is valid
		if(myNEMA.compareTo(Signal.NEMA.NULL)==0)
			SiriusErrorLog.addError("Invalid NEMA code in phase NEMA=" + getMyNEMA() + " in signal id=" + mySignal.getId());
		
		// numbers are positive
		if( mingreen<0 )
			SiriusErrorLog.addError("Negative mingreen=" + mingreen + " in signal id=" + mySignal.getId());

		if( yellowtime<0 )
			SiriusErrorLog.addError("Negative yellowtime=" + yellowtime + " in signal id=" + mySignal.getId());

		if( redcleartime<0 )
			SiriusErrorLog.addError("Negative redcleartime=" + redcleartime + " in signal id=" + mySignal.getId());
	}
	
//	 -------------------------------------------------------------------------------------------------
	
	protected void updatePermitOpposingHold(){
	
		switch(bulbcolor){

		case GREEN:
			// iff I am about to go off and there is no transition time
			permitopposinghold = forceoff_requested && actualyellowtime==0 && redcleartime==0;
			break;
		case YELLOW:
			// iff near end yellow time and there is no red clear time
			permitopposinghold =  SiriusMath.greaterorequalthan(bulbtimer.getT(),actualyellowtime-bulbtimer.dt) && redcleartime==0 ;
			break;
		case RED:	
			// iff near end of red clear time and not starting again.
			permitopposinghold =  SiriusMath.greaterorequalthan(bulbtimer.getT(),redcleartime-bulbtimer.dt) && !hold_requested;
			break;
		}	

		
	}

	protected void update(boolean hold_approved,boolean forceoff_approved)
	{
		double bulbt = bulbtimer.getT();

		if(!protectd){
			if(permissive)
				return;
			else{
				setPhaseColor(Signal.BulbColor.RED);
				return;
			}
		}
		
		// execute this state machine until "done". May be more than once if 
		// some state has zero holding time (eg yellowtime=0)
		boolean done=false;
		
		while(!done){
			
			switch(bulbcolor){
	
			// .............................................................................................
			case GREEN:
	
				setPhaseColor(Signal.BulbColor.GREEN);
				
//				permitopposinghold = false;
					
				// Force off 
				if( forceoff_approved ){ 
					setPhaseColor(Signal.BulbColor.YELLOW);
					mySignal.completedPhases.add(mySignal.new PhaseData(myNEMA, mySignal.myScenario.clock.getT() - bulbtimer.getT(), bulbtimer.getT()));
					bulbtimer.reset();
					//FlushAllStationCallsAndConflicts();
					done = actualyellowtime>0;
				}
				else
					done = true;

				break;
	
			// .............................................................................................
			case YELLOW:
				
				setPhaseColor(Signal.BulbColor.YELLOW);
				
				// set permitopposinghold one step ahead of time so that other phases update correctly next time.
//				permitopposinghold = false;
				
				
//				if( SiriusMath.greaterorequalthan(bulbt,actualyellowtime-bulbtimer.dt) && redcleartime==0)
//					permitopposinghold = true;

				// yellow time over, go immediately to red if redcleartime==0
				if( SiriusMath.greaterorequalthan(bulbt,actualyellowtime) ){
					setPhaseColor(Signal.BulbColor.RED);
					bulbtimer.reset();
					done = redcleartime>0;
				}
				else
					done = true;
				break;
	
			// .............................................................................................
			case RED:
	
				setPhaseColor(Signal.BulbColor.RED);
	
				//if( SiriusMath.greaterorequalthan(bulbt,redcleartime-myNode.getMyNetwork().getTP()*3600f  && !goG )
//				if( SiriusMath.greaterorequalthan(bulbt,redcleartime-bulbtimer.dt) && !hold_approved )
//					permitopposinghold = true;
//				else
//					permitopposinghold = false;
	
				// if hold, set to green, go to green, etc.
				if( hold_approved ){ 
					setPhaseColor(Signal.BulbColor.GREEN);
					bulbtimer.reset();
	
					// Unregister calls (for reading conflicting calls)
					//FlushAllStationCallsAndConflicts(); // GCG ?????
					
					done = !forceoff_approved;
				}
				else
					done = true;
	
				break;
			}
			
		}
	}
	
	protected void setPhaseColor(Signal.BulbColor color){
		mySignal.myPhaseController.setPhaseColor(myNEMA,color);
		bulbcolor = color;
	}

	public float getYellowtime() {
		return yellowtime;
	}

	public float getRedcleartime() {
		return redcleartime;
	}

	public float getMingreen() {
		return mingreen;
	}

	public Signal.NEMA getMyNEMA() {
		return myNEMA;
	}
	
	public float getActualyellowtime() {
		return actualyellowtime;
	}

	public void setActualyellowtime(float actualyellowtime) {
		this.actualyellowtime = actualyellowtime;
	}
	
	public float getActualredcleartime() {
		return actualredcleartime;
	}

	public void setActualredcleartime(float actualredcleartime) {
		this.actualredcleartime = actualredcleartime;
	}

	public Signal.BulbColor getBulbcolor() {
		return bulbcolor;
	}
		
}
