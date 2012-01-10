package aurora.simulator;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import aurora.jaxb.*;

public class _Scenario extends aurora.jaxb.Scenario {

	protected boolean isloadedandinitialized=false;		// true if configuration file has been loaded
	protected boolean isvalid=false;					// true if it has passed validation
	protected boolean isreadytorun=false;				// true if scenario has passed validation and reset.	
	
	protected ArrayList<_Controller> controllers;
	
	/////////////////////////////////////////////////////////////////////
	// interface
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public _Network getNetwork() {
		return (_Network) network;
	}

	@Override
	public _Settings getSettings() {
		return (_Settings) settings;
	}
	
	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	protected void initialize() {

		if(isloadedandinitialized)
			return;
		
		// settings
		getSettings().initialize();
		
		// network
		getNetwork().initialize();
				
		// initialize profiles 
		if(getInitialDensityProfile()!=null)
			((_InitialDensityProfile)getInitialDensityProfile()).initialize();
		
		if(getSplitRatioProfileSet()!=null)
			for( Splitratios splitratios : getSplitRatioProfileSet().getSplitratios() )
				((_SplitRatiosProfile) splitratios).initialize();

		if(getCapacityProfileSet()!=null)
			for( Capacity capacity : getCapacityProfileSet().getCapacity() )
				((_CapacityProfile) capacity).initialize();

		if(getDemandProfileSet()!=null)
			for( Demand demand : getDemandProfileSet().getDemand() )
				((_DemandProfile) demand).initialize();
		
		// initialize controllers
		if(getControllerSet()!=null){
			controllers = new ArrayList<_Controller>();
			for(Controller controller : getControllerSet().getController()){

				// assign type
				_Controller C = null;
				Types.Controller myType;
		    	try {
					myType = Types.Controller.valueOf(controller.getType());
				} catch (IllegalArgumentException e) {
					myType = Types.Controller.none;
					return;
				}	
				// generate controller
				switch(myType){
					case ALINEA:
						C = new aurora.control.ControllerAlinea(controller,myType);
					break;
				}
				controllers.add(C);
			}
			
			// tell network elements that are controlled
			for(_Controller C : controllers){
				switch(C.getMyTargetType()){
				case LINK:
					((_Link)C.target).iscontrolled = true;
					break;
				case NODE:
					((_Node)C.target).iscontrolled = true;
					break;					
				}
			}
		}

		// initialize events
		if(getEventSet()!=null)
			for(Event event : getEventSet().getEvent() )
				((_Event) event).initialize();
		
		isloadedandinitialized = true;
		
		
	}
	
	protected boolean validate() {
		if(!isloadedandinitialized){
			System.out.println("Load scenario first.");
			return false;
		}

		if(isvalid)
			return true;
		
		// validate settings
		if(!getSettings().validate())
			return false;
		
		// validate network
		if(!getNetwork().validate())
			return false;

		// validate initial density profile
		if(getInitialDensityProfile()!=null)
			((_InitialDensityProfile) getInitialDensityProfile()).validate();

		// validate capacity profiles	
		if(getCapacityProfileSet()!=null)
			for(Capacity capacity : getCapacityProfileSet().getCapacity())
				if(!((_CapacityProfile)capacity).validate())
					return false;
		
		// validate demand profiles
		if(getDemandProfileSet()!=null)
			for(Demand demand : getDemandProfileSet().getDemand())
				if(!((_DemandProfile)demand).validate())
					return false;

		// validate split ratio profiles
		if(getSplitRatioProfileSet()!=null)
			for(Splitratios splitratio : getSplitRatioProfileSet().getSplitratios())
				if(!((_SplitRatiosProfile)splitratio).validate())
					return false;
		
		// validate controllers
		if(controllers!=null)
			for(_Controller controller : controllers)
				if(!controller.validate())
					return false;

		// validate events
		if(getEventSet()!=null)
			for(Event event : getEventSet().getEvent())
				if(!((_Event)event).validate())
					return false;
		
		isvalid = true;
		return true;
	}

	protected void reset() {
		if(!isvalid){
			System.out.println("This scenario has not passed validation yet. Use validate().");
			return;
		}
		
		// output files
		try {
			Utils.outputwriter.open(Utils.outputfile_density,Utils.outputfile_outflow,Utils.outputfile_inflow);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		// reset network
		getNetwork().reset();
		
		// reset controllers
		if(controllers!=null)
			for(_Controller controller : controllers)
				controller.reset();

		// reset events
		if(getEventSet()!=null)
			for(Event event : getEventSet().getEvent())
				((_Event)event).reset();
		
		isreadytorun = true;
	}	
	
	protected void update() {	

        // sample profiles .............................	
    	if(getCapacityProfileSet()!=null)
        	for(Capacity capacity:getCapacityProfileSet().getCapacity())
        		((_CapacityProfile) capacity).update();

    	if(getDemandProfileSet()!=null)
        	for(Demand demand : getDemandProfileSet().getDemand())
        		((_DemandProfile) demand).update();

    	if(getSplitRatioProfileSet()!=null)
        	for(Splitratios splitratio : getSplitRatioProfileSet().getSplitratios())
        		((_SplitRatiosProfile) splitratio).update();
    	
        // activate events that are due
    	if(getEventSet()!=null)
        	for(Event event : getEventSet().getEvent())
        		((_Event) event).update();
    	
        // update controllers
    	if(controllers!=null)
        	for(_Controller controller : controllers)
        		controller.update();

        // update the network state......................
        getNetwork().update();
        
	}

	/////////////////////////////////////////////////////////////////////
	// methods
	/////////////////////////////////////////////////////////////////////
	
	// Run the scenario
	protected void run(){
		
        if(!isreadytorun){
			System.out.println("Use reset().");
			return;
        }
        
        Utils.clock = new Clock(getSettings().getTimeInitialinHours(),getSettings().getTimeMaxInHours(),Utils.simdt);

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
            //if(Utils.clock.istimetosample(Utils.outputwriter.getOutsteps()))
        	if((Utils.clock.getCurrentstep()==1) || ((Utils.clock.getCurrentstep()-1)%Utils.outputwriter.getOutsteps()==0))
                Utils.outputwriter.recordstate(Utils.clock.getT(),true);
        }
                
        Utils.outputwriter.close();
        isreadytorun = false;

	}

}
