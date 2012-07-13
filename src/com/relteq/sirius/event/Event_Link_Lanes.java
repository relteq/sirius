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
		this.resetToNominal = jaxbe.isResetToNominal();
		if(jaxbe.getLaneCountChange()!=null)
			this.deltalanes = jaxbe.getLaneCountChange().getDelta().doubleValue();
		else
			this.deltalanes = 0.0;
	}

	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		
		// check each target is valid
		for(ScenarioElement s : targets){
			if(s.getMyType().compareTo(ScenarioElement.Type.link)!=0){
				SiriusErrorLog.addError("wrong target type for event id=" +getId() +".");
				return false;
			}
		}
				
		return true;
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
