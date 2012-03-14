package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.LinkReference;

public class SignalPhase {
	
	// references ....................................................
	//private AbstractNodeComplex myNetwork;
	protected _Node myNode;
	protected _Signal mySignal;
	protected _Link [] targetlinks;
	
	// properties ....................................................
	
	protected boolean protectd		= false;
	protected boolean isthrough		= false;
	protected boolean recall		= false;
	protected boolean permissive	= false;
	protected boolean lag 			= false;

	// dual ring structure
	protected int myRingGroup		= -1;
	protected SignalPhase opposingPhase;
	protected _Signal.NEMA myNEMA   = _Signal.NEMA.NULL;
	
	// Basic timing parameters
	protected float mingreen 			= 0f;
	protected float yellowtime 			= 0f;
	protected float redcleartime 		= 0f;
	protected float actualyellowtime 	= 0f;
	protected float actualredcleartime 	= 0f;

	// timers
	protected Clock bulbtimer;

	// State
	protected _Signal.BulbColor bulbcolor;
	
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
	protected boolean hold 					= false;
	protected boolean forceoff				= false;

	// Safety
	protected boolean permitopposinghold 	= false;
	protected boolean permithold			= false;

	protected int numapproachloops = 0;	
	
	public SignalPhase(_Node myNode,_Signal mySignal,double dt){
		this.myNode = myNode;
		this.mySignal = mySignal;
		this.bulbtimer = new Clock(0d,Double.POSITIVE_INFINITY,dt);
	}
	
	protected final void populateFromJaxb(_Scenario myScenario,com.relteq.sirius.jaxb.Phase jaxbPhase){
	
		int numlinks = jaxbPhase.getLinks().getLinkReference().size();
		this.targetlinks = new _Link[numlinks];
		for(int i=0;i<numlinks;i++){
			LinkReference linkref = jaxbPhase.getLinks().getLinkReference().get(i);
			targetlinks[i] = myScenario.getLinkWithCompositeId(linkref.getNetworkId(),linkref.getId());
		}
		
		if(jaxbPhase.getNema()!=null)
			try{
				myNEMA = _Signal.NEMA.valueOf("_"+jaxbPhase.getNema().toString());
			} catch(IllegalArgumentException e){
				myNEMA = _Signal.NEMA.NULL;
			}
		else
			myNEMA = _Signal.NEMA.NULL;
		
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
		this.actualredcleartime = yellowtime;
		this.actualredcleartime = redcleartime;
		
	}
	
	protected void reset() {

		hasstoplinecall		= false;
		hasapproachcall		= false;
		hasconflictingcall	= false;
		conflictingcalltime	= 0f;
		hold 				= false;
		forceoff			= false;
		permitopposinghold 	= false;
		permithold			= false;

		setRed();
		bulbtimer.reset();
		
	}

	protected boolean validate() {

		// check that there are links attached
		if(targetlinks==null || targetlinks.length==0)
			return false;
		
		// target links are valid
		for(int i=0;i<targetlinks.length;i++)
			if(targetlinks[i]==null)
				return false;

		
		// myNEMA is valid
		if(myNEMA==_Signal.NEMA.NULL)
			return false;
		
		// numbers are positive
		if( mingreen<0 || yellowtime<0 || redcleartime<0 )
			return false;
		
		return true;
	}
	
//	 -------------------------------------------------------------------------------------------------
	protected void processCommand(boolean goG,boolean goY)
	{
		double bulbt = bulbtimer.getT();

		if(!protectd){
			if(permissive)
				return;
			else{
				setRed();
				return;
			}
		}

		switch(bulbcolor){

		// .............................................................................................
		case GREEN:

			setGreen();
			permitopposinghold = false;

			// Force off 
			if( goY ){ 
				setYellow();
				bulbtimer.reset();
				//FlushAllStationCallsAndConflicts();
			}

			break;

		// .............................................................................................
		case YELLOW:
			
			setYellow();
			permitopposinghold = false;

			// if timer>=yellowtime-EPS, go to red (Set permissive opposing left turn to yellow), reset timer
			if( SiriusMath.greaterorequalthan(bulbt,actualyellowtime) ){
				setRed();
				bulbtimer.reset();
			}
			break;

		// .............................................................................................
		case RED:

			setRed();

			//if( SiriusMath.greaterorequalthan(bulbt,redcleartime-myNode.getMyNetwork().getTP()*3600f  && !goG )
			if( SiriusMath.greaterorequalthan(bulbt,redcleartime) && !goG )
				permitopposinghold = true;
			else
				permitopposinghold = false;

			// if hold, set to green, go to green, etc.
			if( goG ){ 
				setGreen();
				bulbtimer.reset();

				// Unregister calls (for reading conflicting calls)
				//FlushAllStationCallsAndConflicts(); // GCG ?????
			}

			break;
		}
	}
	
	protected void setGreen() {
		//for (int i=0; i<targetlinks.length; i++)
		//	mySignal.myController.setControlInput(myControlIndex.get(i), links.get(i).getCapacityValue().getCenter() );
		bulbcolor = _Signal.BulbColor.GREEN;
	}

	protected void setYellow(){
		//for (int i=0; i<targetlinks.length; i++)
		//	mySignal.myController.setControlInput(myControlIndex.get(i),links.get(i).getCapacityValue().getCenter() );
		bulbcolor = _Signal.BulbColor.YELLOW;
	}

	protected void setRed() {
		//for (int i=0; i<targetlinks.length; i++)
		//	mySignal.myController.setControlInput(myControlIndex.get(i),0.0);
		bulbcolor = _Signal.BulbColor.RED;
	}
	
}
