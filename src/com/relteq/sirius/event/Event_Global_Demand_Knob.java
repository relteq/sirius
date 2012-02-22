package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.DemandProfile;
import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Scenario;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class Event_Global_Demand_Knob extends _Event {

	protected boolean resetToNominal;
	protected Double newknob;

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Global_Demand_Knob(){
	}
	
	public Event_Global_Demand_Knob(_Scenario myScenario,double newknob) {
		this.myScenario = myScenario;
		this.newknob = newknob;
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Event e) {
		this.resetToNominal = e.isResetToNominal();
		if(e.getKnob()!=null)
			newknob = e.getKnob().getValue().doubleValue();
		else 
			newknob = Double.NaN;
	}
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		return true;
	}

	@Override
	public void activate() {
    	if(myScenario.getDemandProfileSet()!=null){
        	for(DemandProfile profile : myScenario.getDemandProfileSet().getDemandProfile() ){
    			if(resetToNominal)
    				setDemandProfileEventKnob(profile,profile.getKnob().doubleValue());
    			else
    				setDemandProfileEventKnob(profile,newknob);
        	}	
    	}
	}
}
