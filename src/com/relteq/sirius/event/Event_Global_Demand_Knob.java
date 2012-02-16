package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Scenario;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class Event_Global_Demand_Knob extends _Event {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Global_Demand_Knob(_Scenario myScenario, Event jaxbE) {
		super.populateFromJaxb(myScenario,jaxbE, _Event.Type.global_demand_knob);
	}
	
	public Event_Global_Demand_Knob(_Scenario myScenario) {
		// TODO Auto-generated constructor stub
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		return true;
	}

	@Override
	public void activate() {
    	if(myScenario.getDemandProfileSet()!=null)
        	for(DemandProfile profile : myScenario.getDemandProfileSet().getDemandProfile() ){
        		double knobvalue;
    			if(isResetToNominal())
    				knobvalue = ((_DemandProfile) profile).getKnob().doubleValue();
    			else
    				knobvalue = getKnob().getValue().doubleValue();
        		((_DemandProfile) profile).set_knob( knobvalue );
        	}		
	}
}
