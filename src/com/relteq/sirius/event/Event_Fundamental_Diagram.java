package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._FundamentalDiagram;
import com.relteq.sirius.simulator._Link;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class Event_Fundamental_Diagram extends _Event {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Fundamental_Diagram(_Scenario myScenario, Event jaxbE) {
		super.populateFromJaxb(myScenario,jaxbE, _Event.Type.fundamental_diagram);
	}
	
	public Event_Fundamental_Diagram(_Scenario myScenario) {
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
			if(isResetToNominal()){
				targetlink.deactivateFDEvent();
			}
			else{
				_FundamentalDiagram eventFD = new _FundamentalDiagram(targetlink);
				eventFD.copyfrom((targetlink).FD);		// copy current FD
				eventFD.copyfrom(this.getFundamentalDiagram());		// replace values with those defined in the event
				if(eventFD.validate()){								// validate the result
					//targetlink.setEventFundamentalDiagram(eventFD);
					targetlink.activateFDEvent(eventFD);
				}
			}
		}
		
	}
}
