package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Scenario;

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
	public void populate(Object jaxbobject) {
		Event jaxbe = (Event) jaxbobject;
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
