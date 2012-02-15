/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.*;

public final class _Scenario extends com.relteq.sirius.jaxb.Scenario {
	
	protected _ControllerSet _controllerset = new _ControllerSet();
	protected _EventSet _eventset = new _EventSet();	// holds time sorted list of events
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	// populate methods copy data from the jaxb state to extended objects. 
	// They do not throw exceptions or report mistakes. Data errors should be
	// circumvented and left for the validation to report.
	protected void populate() {
		
		// network list
		if(getNetworkList()!=null)
			for( Network network : getNetworkList().getNetwork() )
				((_Network) network).populate();
	
		// split ratio profile set (must follow network)
		if(getSplitRatioProfileSet()!=null)
			((_SplitRatioProfileSet) getSplitRatioProfileSet()).populate();
		
		// boundary capacities (must follow network)
		if(getDownstreamBoundaryCapacitySet()!=null)
			for( CapacityProfile capacityProfile : getDownstreamBoundaryCapacitySet().getCapacityProfile() )
				((_CapacityProfile) capacityProfile).populate();

		if(getDemandProfileSet()!=null)
			((_DemandProfileSet) getDemandProfileSet()).populate();
		
		// fundamental diagram profiles 
		if(getFundamentalDiagramProfileSet()!=null)
			for(FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
				((_FundamentalDiagramProfile) fd).populate();
		
		// initial density profile 
		if(getInitialDensityProfile()!=null)
			((_InitialDensityProfile) getInitialDensityProfile()).populate();
		
		// initialize controllers and events
		_controllerset.populate();
		_eventset.populate();
		
	}
	
	// validation methods check consistency of the input data. 
	// They generate error messages.
	protected void validate() {
		
//		if(!isreset){
//			System.out.println("Reset scenario first.");
//			return;
//		}

		if(!Utils.isloadedandinitialized){
			System.out.println("Scenario has not been correctly loaded.");
			return;
		}
		
		if(Utils.isvalid)
			return;
		
		// check that outdt is a multiple of simdt
		if(!Utils.isintegermultipleof(Utils.outdt,Utils.simdtinseconds)){
//			Utils.addErrorMessage("Aborting: outdt must be an interger multiple of simulation dt.");
//			Utils.printErrorMessage();
			return;
		}
		
		// validate network
		if( getNetworkList()!=null)
			for(Network network : getNetworkList().getNetwork())
				if(!((_Network)network).validate())
					return;

		// validate initial density profile
		if(getInitialDensityProfile()!=null)
			if(!((_InitialDensityProfile) getInitialDensityProfile()).validate())
				return;

		// validate capacity profiles	
		if(getDownstreamBoundaryCapacitySet()!=null)
			for(CapacityProfile capacityProfile : getDownstreamBoundaryCapacitySet().getCapacityProfile())
				if(!((_CapacityProfile)capacityProfile).validate())
					return;
		
		// validate demand profiles
		if(getDemandProfileSet()!=null)
			if(!((_DemandProfileSet)getDemandProfileSet()).validate())
				return;

		// validate split ratio profiles
		if(getSplitRatioProfileSet()!=null)
			if(!((_SplitRatioProfileSet)getSplitRatioProfileSet()).validate())
				return;

		// validate fundamental diagram profiles
		if(getFundamentalDiagramProfileSet()!=null)
			for(FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
				if(!((_FundamentalDiagramProfile)fd).validate())
					return;

		// validate controllers
		if(!_controllerset.validate())
			return;

		// validate events
		if(!_eventset.validate())
			return;

		Utils.isvalid = true;
	}

	// prepare scenario for simulation:
	// set the state of the scenario to the initial condition
	// sample profiles
	// open output files
	protected void reset() {
		
		if(Utils.isreset)
			return;
		
		if(!Utils.isloadedandinitialized){
			Utils.addErrorMessage("Load scenario first.");
			return;
		}
		
		// reset the clock
		Utils.clock.reset();
		
		// reset network
		for(Network network : getNetworkList().getNetwork())
			((_Network)network).reset();
		
		// reset demand profiles
		if(getDemandProfileSet()!=null)
			((_DemandProfileSet)getDemandProfileSet()).reset();

		// reset fundamental diagrams
		for(FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
			((_FundamentalDiagramProfile)fd).reset();
		
		// reset controllers
		Utils.controlon = true;
		_controllerset.reset();

		// reset events
		_eventset.reset();
		
		Utils.isreset = true;
	}	
	
	protected void update() {	

        // sample profiles .............................	
    	if(this.getDownstreamBoundaryCapacitySet()!=null)
        	for(CapacityProfile capacityProfile : getDownstreamBoundaryCapacitySet().getCapacityProfile())
        		((_CapacityProfile) capacityProfile).update();

    	if(getDemandProfileSet()!=null)
        	for(DemandProfile demandProfile : getDemandProfileSet().getDemandProfile())
        		((_DemandProfile) demandProfile).update();

    	if(getSplitRatioProfileSet()!=null)
    		((_SplitRatioProfileSet) getSplitRatioProfileSet()).update();        		

    	if(getFundamentalDiagramProfileSet()!=null)
        	for(FundamentalDiagramProfile fdProfile : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
        		((_FundamentalDiagramProfile) fdProfile).update();
    	
        // update controllers
    	if(Utils.controlon)
    		_controllerset.update();

    	// update events
    	_eventset.update();
    	
        // update the network state......................
		for(Network network : getNetworkList().getNetwork())
			((_Network) network).update();
        
	}

	/////////////////////////////////////////////////////////////////////
	// methods
	/////////////////////////////////////////////////////////////////////
	
	// Run the scenario
	protected void run(){
		
        if(!Utils.isreset){
			System.out.println("Use reset().");
			return;
        }
        
        // write initial condition
        //Utils.outputwriter.recordstate(Utils.clock.getT(),false);
        
        while( !Utils.clock.expired() ){

            // update time (before write to output)
            Utils.clock.advance();
        	
        	// update scenario
        	update();

            // update time (before write to output)
            // Utils.clock.advance();
        	
            // write output .............................
            if(Utils.simulationMode==Utils.ModeType.normal)
            	//if(Utils.clock.istimetosample(Utils.outputwriter.getOutsteps()))
	        	if((Utils.clock.getCurrentstep()==1) || ((Utils.clock.getCurrentstep()-1)%Utils.outputwriter.getOutsteps()==0))
	                Utils.outputwriter.recordstate(Utils.clock.getT(),true);
        }
        
        Utils.isreset = false;
        
	}

}
