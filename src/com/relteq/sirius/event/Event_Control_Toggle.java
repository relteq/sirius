package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator._Controller;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class Event_Control_Toggle extends _Event {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Control_Toggle(_Scenario myScenario, Event jaxbE) {
		super.populateFromJaxb(myScenario,jaxbE, _Event.Type.control_toggle);
	}
	
	public Event_Control_Toggle(_Scenario myScenario) {
		// TODO Auto-generated constructor stub
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		
		// check each target is valid
		for(_ScenarioElement s : targets){
			if(s.getMyType()!=_ScenarioElement.Type.controller){
				System.out.println("wrong target type.");
				return false;
			}
		}
		return true;
	}

	@Override
	public void activate() {
		for(_ScenarioElement s : targets){
			_Controller c = myScenario.getControllerWithName(s.getId());
			boolean ison = getOnOffSwitch().getValue().equalsIgnoreCase("on");
			activateControllerOnOffEvent(c, ison);
		}			
	}
	
}