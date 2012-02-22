package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.DemandProfile;
import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Link;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
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
	public void populate(Event e) {
		this.resetToNominal = e.isResetToNominal();
		if(e.getKnob()!=null)
			newknob = e.getKnob().getValue().doubleValue();
		else 
			newknob = Double.NaN;
		
	}

	@Override
	public boolean validate() {

		if(!super.validate())
			return false;
		
		// check each target is valid
		for(_ScenarioElement s : targets){
			if(s.getMyType()!=_ScenarioElement.Type.link){
				System.out.println("wrong target type.");
				return false;
			}
			if(!((_Link)s.getReference()).isSource()){
				System.out.println("demand event attached to non-source link.");
				return false;
				
			}
		}
		return true;
	}

	@Override
	public void activate() {
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
