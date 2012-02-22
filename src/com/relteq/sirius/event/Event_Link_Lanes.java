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

	protected boolean resetToNominal;
	protected double deltalanes;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Link_Lanes(){
	}
	
	public Event_Link_Lanes(_Scenario myScenario,double deltalanes) {
		this.deltalanes = deltalanes;
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Event e) {
		this.resetToNominal = e.isResetToNominal();
		if(e.getLaneCountChange()!=null)
			this.deltalanes = e.getLaneCountChange().getDelta().doubleValue();
		else
			this.deltalanes = 0.0;
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
		}
		return true;
	}

	@Override
	public void activate() {
		for(_ScenarioElement s : targets){
			_Link targetlink = (_Link) s.getReference();
			if(resetToNominal){
				double originallanes = ((com.relteq.sirius.jaxb.Link)targetlink).getLanes().doubleValue();
				setLinkLanes(targetlink,originallanes);
			}
			setLinkDeltaLanes(targetlink,deltalanes);
		}		
	}
}
