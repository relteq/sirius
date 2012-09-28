package com.relteq.sirius.event;

import java.util.ArrayList;
import java.util.List;

import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator.Event;
import com.relteq.sirius.simulator.Link;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.ScenarioElement;

public class Event_Link_Lanes extends Event {

	protected boolean resetToNominal;
	protected double deltalanes;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Event_Link_Lanes(){
	}
			
	public Event_Link_Lanes(Scenario myScenario,List<Link> links,boolean isrevert,double deltalanes) {
		this.targets = new ArrayList<ScenarioElement>();
		this.resetToNominal = isrevert;
		for(Link link : links)
			this.targets.add(ObjectFactory.createScenarioElement(link));
		this.deltalanes = deltalanes;
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Object jaxbobject) {
		com.relteq.sirius.jaxb.Event jaxbe = (com.relteq.sirius.jaxb.Event) jaxbobject;
		com.relteq.sirius.simulator.Parameters params = (com.relteq.sirius.simulator.Parameters) jaxbe.getParameters();
		// reset_to_nominal
		if (null != params && params.has("reset_to_nominal"))
			this.resetToNominal = params.get("reset_to_nominal").equalsIgnoreCase("true");
		else
			this.resetToNominal = false;
		// lane_count_change
		if (null != params && params.has("lane_count_change"))
			this.deltalanes = Double.valueOf(params.get("lane_count_change"));
		else
			this.deltalanes = 0.0;
	}

	@Override
	public void validate() {
		
		super.validate();
		
		// check each target is valid
		for(ScenarioElement s : targets){
			if(s.getMyType().compareTo(ScenarioElement.Type.link)!=0)
				SiriusErrorLog.addError("wrong target type for event id=" +getId() +".");
		}
	}

	@Override
	public void activate() throws SiriusException{
		double newlanes;
		for(ScenarioElement s : targets){
			Link targetlink = (Link) s.getReference();
			if(resetToNominal)
				newlanes = ((com.relteq.sirius.jaxb.Link)targetlink).getLanes().doubleValue();
			else
				newlanes =  targetlink.get_Lanes();
			newlanes += deltalanes;
			setLinkLanes(targetlink,newlanes);
		}		
	}


}
