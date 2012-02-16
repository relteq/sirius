package com.relteq.sirius.event;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Node;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class Event_Node_Split_Ratio extends _Event {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Node_Split_Ratio(_Scenario myScenario, Event jaxbE) {
		super.populateFromJaxb(myScenario,jaxbE, _Event.Type.node_split_ratio);	
	}
	
	public Event_Node_Split_Ratio(_Scenario myScenario) {
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
			if(s.getMyType()!=_ScenarioElement.Type.node){
				System.out.println("wrong target type.");
				return false;
			}
		}
		return true;
	}

	@Override
	public void activate() {
		if(isResetToNominal()){
			for(_ScenarioElement s : targets){
				_Node targetnode = (_Node) s.getReference();
				targetnode.removeEventSplitratio();
			}
		}
		else{
			for(_ScenarioElement s : targets){
				_Node targetnode = (_Node) s.getReference();
				Double3DMatrix splitratio = new Double3DMatrix(0,0,0,0d);
				if(targetnode.validateSplitRatioMatrix(splitratio))
					targetnode.setEventSplitratio(splitratio);
			}
		}		
	}
}
