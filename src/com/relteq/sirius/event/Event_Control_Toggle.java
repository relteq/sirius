package com.relteq.sirius.event;

import java.util.ArrayList;
import java.util.List;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator._Controller;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

public class Event_Control_Toggle extends _Event {

	protected boolean ison; 
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Control_Toggle(){
	}
		
	public Event_Control_Toggle(_Scenario myScenario,float timestampinseconds,List <_Controller> controllers,boolean ison) {
		this.myScenario = myScenario;
		this.ison = ison;
		this.myType = _Event.Type.control_toggle;
		this.timestampstep = (int) Math.round(timestampinseconds/myScenario.getSimDtInSeconds());
		this.targets = new ArrayList<_ScenarioElement>();
			for(_Controller controller : controllers )
				this.targets.add(ObjectFactory.createScenarioElement(controller));	
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Object jaxbobject) {
		Event jaxbe = (Event) jaxbobject;
		if(jaxbe.getOnOffSwitch()!=null)
			this.ison = jaxbe.getOnOffSwitch().getValue().equalsIgnoreCase("on");
		else 
			this.ison = true;
	}
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		
		// check each target is valid
		for(_ScenarioElement s : targets){
			if(s.getMyType().compareTo(_ScenarioElement.Type.controller)!=0){
				SiriusErrorLog.addErrorMessage("wrong target type.");
				return false;
			}
		}
		return true;
	}

	@Override
	public void activate() throws SiriusException{
		for(_ScenarioElement s : targets){
			_Controller c = myScenario.getControllerWithId(s.getId());
			setControllerIsOn(c, ison);
		}			
	}
	
}