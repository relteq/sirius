package com.relteq.sirius.event;

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
		this.resetToNominal = jaxbe.isResetToNominal();
		if(jaxbe.getKnob()!=null)
			newknob = jaxbe.getKnob().getValue().doubleValue();
		else 
			newknob = Double.NaN;
	}
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		if(newknob<0)
			return false;
		return true;
	}

	@Override
	public void activate() throws SiriusException{
		setGlobalDemandEventKnob(newknob);
	}
}
