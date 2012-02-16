/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.relteq.sirius.jaxb.*;

public final class _Scenario extends com.relteq.sirius.jaxb.Scenario {

	protected _Scenario.ModeType simulationMode;
	protected _Scenario.UncertaintyType uncertaintyModel;
	
	protected int numVehicleTypes;			// number of vehicle types
	protected boolean controlon;			// global control switch
	protected double simdtinseconds;		// [sec] simulation time step 
	protected double simdtinhours;			// [hr]  simulation time step 	
	
	protected boolean isloaded=false;	// true if configuration file has been loaded
	protected boolean isreset=false;	// true if scenario passed reset.	
	protected boolean isrunning=false;	// true when the simulation is running
	protected boolean isvalid=false;	// true if it has passed validation
	
	protected _ControllerSet _controllerset = new _ControllerSet();
	protected _EventSet _eventset = new _EventSet();	// holds time sorted list of events
	
	protected static enum ModeType {NULL, normal, 
									    warmupFromZero , 
									    warmupFromIC };

	protected static enum UncertaintyType { NULL, uniform, 
          										gaussian }
	
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

		if(!isloaded){
			System.out.println("Scenario has not been correctly loaded.");
			return;
		}
		
		if(isvalid)
			return;
		
		// check that outdt is a multiple of simdt
		if(!SiriusMath.isintegermultipleof(Global.outdt,simdtinseconds)){
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

		isvalid = true;
	}

	// prepare scenario for simulation:
	// set the state of the scenario to the initial condition
	// sample profiles
	// open output files
	protected void reset() {
		
		if(isreset)
			return;
		
		if(!isloaded){
			SiriusError.addErrorMessage("Load scenario first.");
			return;
		}
		
		// reset the clock
		Global.clock.reset();
		
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
		controlon = true;
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
    		((_SplitRatioProfileSet) getSplitRatioProfileSet()).update();        		

    	if(getFundamentalDiagramProfileSet()!=null)
        	for(FundamentalDiagramProfile fdProfile : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
        		((_FundamentalDiagramProfile) fdProfile).update();
    	
        // update controllers
    	if(controlon)
    		_controllerset.update();

    	// update events
    	_eventset.update();
    	
        // update the network state......................
		for(Network network : getNetworkList().getNetwork())
			((_Network) network).update();
        
	}

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

	protected _Network getNetworkWithId(String id){
		if(id==null)
			return null;
		if(getNetworkList()==null)
			return null;
		id.replaceAll("\\s","");
		for(Network network : getNetworkList().getNetwork()){
			if(network.getId().equals(id))
				return (_Network) network;
		}
		return null;
	}
	
	// Run the scenario
	protected void run(){
		
        if(!isreset){
			System.out.println("Use reset().");
			return;
        }
        
        isrunning = true;
        
        // write initial condition
        //Utils.outputwriter.recordstate(Utils.clock.getT(),false);
        
        while( !Global.clock.expired() ){

            // update time (before write to output)
        	Global.clock.advance();
        	
        	// update scenario
        	update();

            // update time (before write to output)
            // Utils.clock.advance();
        	
            // write output .............................
            if(simulationMode==ModeType.normal)
            	//if(Utils.clock.istimetosample(Utils.outputwriter.getOutsteps()))
	        	if((Global.clock.getCurrentstep()==1) || ((Global.clock.getCurrentstep()-1)%Global.outputwriter.getOutsteps()==0))
	        		Global.outputwriter.recordstate(Global.clock.getT(),true);
        }
        
        isreset = false;
        isrunning = false;
        
	}

	public void save(String filename){
        try {
        	JAXBContext context = JAXBContext.newInstance("aurora.jaxb");
        	Marshaller m = context.createMarshaller();
        	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        	m.marshal(this,new FileOutputStream(filename));
        } catch( JAXBException je ) {
            je.printStackTrace();
            return;
        } catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
        }
	}
	
}