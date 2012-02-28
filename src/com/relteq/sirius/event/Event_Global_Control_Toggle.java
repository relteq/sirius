package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Scenario;

public class Event_Global_Control_Toggle extends _Event {

	protected boolean ison;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Global_Control_Toggle(){
	}
	
	public Event_Global_Control_Toggle(_Scenario myScenario,boolean ison) {
		this.myScenario = myScenario;
		this.ison = ison;
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Object jaxbobject) {
		Event jaxbe = (Event) jaxbobject;
		if(jaxbe.getOnOffSwitch()!=null)
			this.ison = jaxbe.getOnOffSwitch().getValue().equalsIgnoreCase("on");
		else
			this.ison = true;
	}
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		return true;
	}

	@Override
	public void activate() throws SiriusException{
		setGlobalControlIsOn(ison);
	}
}
