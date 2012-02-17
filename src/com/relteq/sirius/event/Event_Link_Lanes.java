package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Link;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class Event_Link_Lanes extends _Event {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Link_Lanes(_Scenario myScenario, Event jaxbE) {
		super.populateFromJaxb(myScenario,jaxbE, _Event.Type.link_lanes);
	}
	
	public Event_Link_Lanes(_Scenario myScenario) {
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
			if(s.getMyType()!=_ScenarioElement.Type.link){
				System.out.println("wrong target type.");
				return false;
			}
		}
		return true;
	}

	@Override
	public void activate() {
		for(_ScenarioElement s : targets){
			_Link targetlink = (_Link) s.getReference();
			if(getLaneCountChange()!=null){
				if(isResetToNominal()){
					double originallanes = ((com.relteq.sirius.jaxb.Link)targetlink).getLanes().doubleValue();
					activateLinkLanesEvent(targetlink,originallanes);
				}
				if(getLaneCountChange().getDelta()!=null){
					double deltalanes = getLaneCountChange().getDelta().doubleValue();
					activateLinkLanesDeltaEvent(targetlink,deltalanes);
				}
			}
		}		
	}
}
