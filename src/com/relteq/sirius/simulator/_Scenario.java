/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.*;

public class _Scenario extends com.relteq.sirius.jaxb.Scenario {

	protected boolean isloadedandinitialized=false;		// true if configuration file has been loaded
	protected boolean isreset=false;					// true if scenario passed reset.	
	protected boolean isvalid=false;					// true if it has passed validation
	
	protected _Settings _settings = new _Settings();
	protected _ControllerSet _controllerset = new _ControllerSet();
	protected _EventSet _eventset = new _EventSet();	// holds time sorted list of events
	
	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	// initialize methods copy data from the jaxb state to extended objects. 
	// They do not throw exceptions or report mistakes. Data errors should be
	// circumvented and left for the validation to report.
	protected void initialize() {

		if(isloadedandinitialized)
			return;
		
		// settings
		_settings.initialize();
		
		// network list
		if(getNetworkList()!=null)
			for( Network network : getNetworkList().getNetwork() )
				((_Network) network).initialize();
				
		// split ratio profiles (must follow network)
		if(getSplitRatioProfileSet()!=null)
			for( SplitratioProfile splitratioProfile : getSplitRatioProfileSet().getSplitratioProfile() )
				((_SplitRatiosProfile) splitratioProfile).initialize();		
		
		// boundary capacities (must follow network)
		if(getDownstreamBoundaryCapacitySet()!=null)
			for( CapacityProfile capacityProfile : getDownstreamBoundaryCapacitySet().getCapacityProfile() )
				((_CapacityProfile) capacityProfile).initialize();

		if(getDemandProfileSet()!=null)
			for( DemandProfile demandProfile : getDemandProfileSet().getDemandProfile() )
				((_DemandProfile) demandProfile).initialize();
		
		// fundamental diagram profiles 
		if(getFundamentalDiagramProfileSet()!=null)
			for(FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
				((_FundamentalDiagramProfile) fd).initialize();
		
		// initial density profile 
		if(getInitialDensityProfile()!=null)
			((_InitialDensityProfile) getInitialDensityProfile()).initialize();
		
		// initialize controllers and events
		_controllerset.initialize();
		_eventset.initialize();
		
		isloadedandinitialized = true;		
	}
	
	// validation methods check consistency of the input data. 
	// They generate error messages.
	protected void validate() {
		
		if(!isreset){
			System.out.println("Reset scenario first.");
			return;
		}

		if(isvalid)
			return;
		
		// validate settings
		if(!_settings.validate())
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
			for(DemandProfile demandProfile : getDemandProfileSet().getDemandProfile())
				if(!((_DemandProfile)demandProfile).validate())
					return;

		// validate split ratio profiles
		if(getSplitRatioProfileSet()!=null)
			for(SplitratioProfile splitratioProfile : getSplitRatioProfileSet().getSplitratioProfile())
				if(!((_SplitRatiosProfile)splitratioProfile).validate())
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

		isvalid = true;
	}

	// prepare scenario for simulation:
	// set the state of the scenario to the initial condition
	// sample profiles
	// open output files
	protected void reset() {
		
		if(isreset)
			return;
		
		if(!isloadedandinitialized){
			Utils.addErrorMessage("Load scenario first.");
			return;
		}
		
		// reset the clock
		Utils.clock.reset();
		
		// reset network
		for(Network network : getNetworkList().getNetwork())
			((_Network)network).reset();
		
		// reset demand profiles
		for(DemandProfile demandProfile : getDemandProfileSet().getDemandProfile())
			((_DemandProfile)demandProfile).reset();

		// reset fundamental diagrams
		for(FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
			((_FundamentalDiagramProfile)fd).reset();
		
		// reset controllers
		Utils.controlon = true;
		_controllerset.reset();

		// reset events
		_eventset.reset();
		
		isreset = true;
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
        	for(SplitratioProfile splitratioProfile : getSplitRatioProfileSet().getSplitratioProfile())
        		((_SplitRatiosProfile) splitratioProfile).update();
    	

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
		
        if(!isreset){
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
            if(Utils.simulationMode==Types.Mode.normal)
            	//if(Utils.clock.istimetosample(Utils.outputwriter.getOutsteps()))
	        	if((Utils.clock.getCurrentstep()==1) || ((Utils.clock.getCurrentstep()-1)%Utils.outputwriter.getOutsteps()==0))
	                Utils.outputwriter.recordstate(Utils.clock.getT(),true);
        }
        
        isreset = false;
        
	}

}
