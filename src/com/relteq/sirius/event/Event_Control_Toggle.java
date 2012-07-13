package com.relteq.sirius.event;

import java.util.ArrayList;
import java.util.List;

import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator.Controller;
import com.relteq.sirius.simulator.Event;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.ScenarioElement;

public class Event_Control_Toggle extends Event {

	protected boolean ison; 
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Control_Toggle(){
	}
		
	public Event_Control_Toggle(Scenario myScenario,float timestampinseconds,List <Controller> controllers,boolean ison) {
		this.myScenario = myScenario;
		this.ison = ison;
		this.myType = Event.Type.control_toggle;
		this.timestampstep = (int) Math.round(timestampinseconds/myScenario.getSimDtInSeconds());
		this.targets = new ArrayList<ScenarioElement>();
			for(Controller controller : controllers )
				this.targets.add(ObjectFactory.createScenarioElement(controller));	
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Object jaxbobject) {
		com.relteq.sirius.jaxb.Event jaxbe = (com.relteq.sirius.jaxb.Event) jaxbobject;
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
		for(ScenarioElement s : targets){
			if(s.getMyType().compareTo(ScenarioElement.Type.controller)!=0){
				SiriusErrorLog.addError("Wrong target type for event id=" +getId() +".");
				return false;
			}
		}
		return true;
	}

	@Override
	public void activate() throws SiriusException{
		for(ScenarioElement s : targets){
			Controller c = myScenario.getControllerWithId(s.getId());
			setControllerIsOn(c, ison);
		}			
	}
	
}