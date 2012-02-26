package com.relteq.sirius.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.jaxb.FundamentalDiagram;
import com.relteq.sirius.simulator.ObjectFactory;
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
	
	public Event_Fundamental_Diagram(_Scenario myScenario,List <_Link> links,double freeflowSpeed,double congestionSpeed,double capacity,double densityJam,double capacityDrop,double stdDevCapacity) {		
		this.FD = new FundamentalDiagram();
		this.FD.setFreeflowSpeed(new BigDecimal(freeflowSpeed));
		this.FD.setCongestionSpeed(new BigDecimal(congestionSpeed));
		this.FD.setCapacity(new BigDecimal(capacity));
		this.FD.setDensityJam(new BigDecimal(densityJam));
		this.FD.setCapacityDrop(new BigDecimal(capacityDrop));
		this.FD.setStdDevCapacity(new BigDecimal(stdDevCapacity));
		this.resetToNominal = false;
		this.targets = new ArrayList<_ScenarioElement>();
		for(_Link link : links)
			this.targets.add(ObjectFactory.createScenarioElement(link));
	}
	
	public Event_Fundamental_Diagram(_Scenario myScenario,List <_Link> links) {		
		this.resetToNominal = true;
		for(_Link link : links)
			this.targets.add(ObjectFactory.createScenarioElement(link));
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Object jaxbobject) {
		Event jaxbe = (Event) jaxbobject;
		this.resetToNominal = jaxbe.isResetToNominal();
		this.FD = jaxbe.getFundamentalDiagram();
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
