package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.jaxb.FundamentalDiagram;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Link;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class Event_Fundamental_Diagram extends _Event {

	protected boolean resetToNominal;
	protected FundamentalDiagram FD;
	  
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Fundamental_Diagram(){
	}
	
	public Event_Fundamental_Diagram(_Scenario myScenario) {
		// XXXXX
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Event e) {
		this.resetToNominal = e.isResetToNominal();
		this.FD = e.getFundamentalDiagram();
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
			if(resetToNominal)
				revertLinkFundamentalDiagram(targetlink);
			else
				setLinkFundamentalDiagram(targetlink,FD);
		}
		
	}
}
