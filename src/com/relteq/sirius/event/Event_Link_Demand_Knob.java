package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.DemandProfile;
import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Link;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

public class Event_Link_Demand_Knob extends _Event {

	protected boolean resetToNominal;
	protected Double newknob;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Link_Demand_Knob(){
	}
	
	public Event_Link_Demand_Knob(_Scenario myScenario,double newknob) {
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
		
		// check each target is valid
		for(_ScenarioElement s : targets){
			if(s.getMyType().compareTo(_ScenarioElement.Type.link)!=0){
				SiriusErrorLog.addErrorMessage("wrong target type.");
				return false;
			}
			if(!((_Link)s.getReference()).isSource()){
				SiriusErrorLog.addErrorMessage("demand event attached to non-source link.");
				return false;
				
			}
		}
		return true;
	}

	@Override
	public void activate() throws SiriusException {
		for(_ScenarioElement s : targets){
	    	if(myScenario.getDemandProfileSet()!=null){
	        	for(DemandProfile profile : myScenario.getDemandProfileSet().getDemandProfile()){
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
