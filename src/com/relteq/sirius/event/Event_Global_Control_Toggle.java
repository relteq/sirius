package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Scenario;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class Event_Global_Control_Toggle extends _Event {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Global_Control_Toggle(_Scenario myScenario, Event jaxbE) {
		super.populateFromJaxb(myScenario,jaxbE, _Event.Type.global_control_toggle);
	}
	
	public Event_Global_Control_Toggle(_Scenario myScenario) {
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
		myScenario.controlon = getOnOffSwitch().getValue().equalsIgnoreCase("on");
	}
}
