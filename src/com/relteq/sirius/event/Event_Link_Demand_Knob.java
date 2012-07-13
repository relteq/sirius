package com.relteq.sirius.event;

import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator.Event;
import com.relteq.sirius.simulator.Link;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.ScenarioElement;

public class Event_Link_Demand_Knob extends Event {

	protected boolean resetToNominal;
	protected Double newknob;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Link_Demand_Knob(){
	}
	
	public Event_Link_Demand_Knob(Scenario myScenario,double newknob) {
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
	public void validate() {

		super.validate();
		
		// check each target is valid
		for(ScenarioElement s : targets){
			if(s.getMyType().compareTo(ScenarioElement.Type.link)!=0)
				SiriusErrorLog.addError("Wrong target type for event id=" +getId() +".");
			if(!((Link)s.getReference()).isSource())
				SiriusErrorLog.addError("Demand event id=" +getId()+ " attached to non-source link.");
		}
	}

	@Override
	public void activate() throws SiriusException {
		for(ScenarioElement s : targets){
	    	if(myScenario.getDemandProfileSet()!=null){
	        	for(com.relteq.sirius.jaxb.DemandProfile profile : myScenario.getDemandProfileSet().getDemandProfile()){
	        		if(profile.getLinkIdOrigin().equals(s.getId())){
	        			if(resetToNominal)
	        				setDemandProfileEventKnob(profile,profile.getKnob().doubleValue());
	        			else
	        				setDemandProfileEventKnob(profile,newknob);	        			
	        			break;
	        		}
	        	}
	    	}
		}		
	}
}
