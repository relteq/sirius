package simulator;

import java.io.FileNotFoundException;

import jaxb.Capacity;
import jaxb.Demand;
import jaxb.Event;
import jaxb.Scenario;
import jaxb.Controller;
import jaxb.Splitratios;

public class _Scenario extends Scenario implements AuroraComponent {

	public boolean isloadedandinitialized=false;	// true if configuration file has been loaded
	public boolean isvalid=false;					// true if it has passed validation
	public boolean isreadytorun=false;				// true if scenario has passed validation and reset.
		
	/////////////////////////////////////////////////////////////////////
	// getters and setters 
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
	// _AuroraComponent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void initialize() {

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
		if(getControllerSet()!=null)
			for(Controller controller : getControllerSet().getController())
				((_Controller) controller).initialize();

		// initialize events
		if(getEventSet()!=null)
			for(Event event : getEventSet().getEvent() )
				((_Event) event).initialize();
		
		isloadedandinitialized = true;
		
		
	}
	
	@Override
	public boolean validate() {
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
		if(getControllerSet()!=null)
			for(Controller controller : getControllerSet().getController())
				if(!((_Controller)controller).validate())
					return false;

		// validate events
		if(getEventSet()!=null)
			for(Event event : getEventSet().getEvent())
				if(!((_Event)event).validate())
					return false;
		
		isvalid = true;
		return true;
	}

	@Override
	public void reset() {
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
		if(getControllerSet()!=null)
			for(Controller controller : getControllerSet().getController())
				((_Controller)controller).reset();

		// reset events
		if(getEventSet()!=null)
			for(Event event : getEventSet().getEvent())
				((_Event)event).reset();
		
		isreadytorun = true;
	}	
	
	@Override
	public void update() {	

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
    	if(getControllerSet()!=null)
        	for(Controller controller : getControllerSet().getController())
        		((_Controller) controller).update();

        // update the network state......................
        getNetwork().update();
        
	}

	/////////////////////////////////////////////////////////////////////
	// methods
	/////////////////////////////////////////////////////////////////////
	
	// Run the scenario
	public void run(){
		
        if(!isreadytorun){
			System.out.println("Use reset().");
			return;
        }
        
        Utils.clock = new Clock(getSettings().getTimeInitialinHours(),getSettings().getTimeMaxInHours(),Utils.simdt);

        // write initial condition
        Utils.outputwriter.recordstate(Utils.clock.getT(),false);
        
        while( !Utils.clock.expired() ){

            // update time (before write to output)
            Utils.clock.advance();
        	
        	// update scenario
        	update();

            // update time (before write to output)
            // Utils.clock.advance();
            
            // write output .............................
            if(Utils.clock.istimetosample(Utils.outputwriter.getOutsteps()))
                Utils.outputwriter.recordstate(Utils.clock.getT(),true);
        }
                
        Utils.outputwriter.close();
        isreadytorun = false;

	}

}
