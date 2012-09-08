package com.relteq.sirius.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator.Event;
import com.relteq.sirius.simulator.Link;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.ScenarioElement;

public class Event_Fundamental_Diagram extends Event {

	protected boolean resetToNominal;
	protected com.relteq.sirius.jaxb.FundamentalDiagram FD;
	  
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Fundamental_Diagram(){
	}
	
	public Event_Fundamental_Diagram(Scenario myScenario,List <Link> links,double freeflowSpeed,double congestionSpeed,double capacity,double densityJam,double capacityDrop,double stdDevCapacity) {		
		this.FD = new com.relteq.sirius.jaxb.FundamentalDiagram();
		this.FD.setFreeFlowSpeed(new BigDecimal(freeflowSpeed));
		this.FD.setCongestionSpeed(new BigDecimal(congestionSpeed));
		this.FD.setCapacity(new BigDecimal(capacity));
		this.FD.setJamDensity(new BigDecimal(densityJam));
		this.FD.setCapacityDrop(new BigDecimal(capacityDrop));
		this.FD.setStdDevCapacity(new BigDecimal(stdDevCapacity));
		this.resetToNominal = false;
		this.targets = new ArrayList<ScenarioElement>();
		for(Link link : links)
			this.targets.add(ObjectFactory.createScenarioElement(link));
	}
	
	public Event_Fundamental_Diagram(Scenario myScenario,List <Link> links) {		
		this.resetToNominal = true;
		for(Link link : links)
			this.targets.add(ObjectFactory.createScenarioElement(link));
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
		// FD
		if (null != params) {
			this.FD = new com.relteq.sirius.jaxb.FundamentalDiagram();
			if (params.has("capacity")) this.FD.setCapacity(new BigDecimal(params.get("capacity")));
			if (params.has("capacity_drop")) this.FD.setCapacityDrop(new BigDecimal(params.get("capacity_drop")));
			if (params.has("congestion_speed")) this.FD.setCongestionSpeed(new BigDecimal(params.get("congestion_speed")));
			if (params.has("jam_density")) this.FD.setJamDensity(new BigDecimal(params.get("jam_density")));
			if (params.has("free_flow_speed")) this.FD.setFreeFlowSpeed(new BigDecimal(params.get("free_flow_speed")));
		} else
			this.FD = null;
	}
	
	@Override
	public void validate() {
		
		super.validate();
		
		// check each target is valid
		for(ScenarioElement s : targets)
			if(s.getMyType().compareTo(ScenarioElement.Type.link)!=0)
				SiriusErrorLog.addError("Wrong target type for event id=" +getId() +".");
		
		// check that new fundamental diagram does not invalidate current state
		
	}

	@Override
	public void activate() throws SiriusException{
		for(ScenarioElement s : targets){
			Link targetlink = (Link) s.getReference();
			if(resetToNominal)
				revertLinkFundamentalDiagram(targetlink);
			else
				setLinkFundamentalDiagram(targetlink,FD);
		}
		
	}
}
