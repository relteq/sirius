package com.relteq.sirius.event;

import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator.Event;
import com.relteq.sirius.simulator.Scenario;

public class Event_Global_Demand_Knob extends Event {

	protected boolean resetToNominal;
	protected Double newknob;

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Global_Demand_Knob(){
	}
	
	public Event_Global_Demand_Knob(Scenario myScenario,double newknob) {
		this.myScenario = myScenario;
		this.newknob = newknob;
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Object jaxbobject) {
		com.relteq.sirius.jaxb.Event jaxbe = (com.relteq.sirius.jaxb.Event) jaxbobject;
		com.relteq.sirius.simulator.Parameters params = (com.relteq.sirius.simulator.Parameters) jaxbe.getParameters();
		// reset_to_nominal
		if (null != params && params.has("reset_to_nominal"))
			this.resetToNominal = params.get("reset_to_nominal").equalsIgnoreCase("true");
		else
			this.resetToNominal = false;
		// knob
		if (null != params && params.has("knob"))
			newknob = Double.valueOf(params.get("knob"));
		else 
			newknob = Double.NaN;
	}
	
	@Override
	public void validate() {
		super.validate();

		if(newknob<0)
			SiriusErrorLog.addError("UNDEFINED ERROR MESSAGE.");
	}

	@Override
	public void activate() throws SiriusException{
		setGlobalDemandEventKnob(newknob);
	}
}
