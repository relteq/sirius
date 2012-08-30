package com.relteq.sirius.event;

import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator.Event;
import com.relteq.sirius.simulator.Scenario;

public class Event_Global_Control_Toggle extends Event {

	protected boolean ison;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Global_Control_Toggle(){
	}
	
	public Event_Global_Control_Toggle(Scenario myScenario,boolean ison) {
		this.myScenario = myScenario;
		this.ison = ison;
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Object jaxbobject) {
		com.relteq.sirius.jaxb.Event jaxbe = (com.relteq.sirius.jaxb.Event) jaxbobject;
		com.relteq.sirius.simulator.Parameters params = (com.relteq.sirius.simulator.Parameters) jaxbe.getParameters();
		if (null != params && params.has("on_off_switch"))
			this.ison = params.get("on_off_switch").equalsIgnoreCase("on");
		else
			this.ison = true;
	}
	
	@Override
	public void validate() {
		super.validate();
	}

	@Override
	public void activate() throws SiriusException{
		setGlobalControlIsOn(ison);
	}
}
