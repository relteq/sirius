package com.relteq.sirius.event;

import java.util.ArrayList;
import java.util.List;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Link;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

public class Event_Link_Lanes extends _Event {

	protected boolean resetToNominal;
	protected double deltalanes;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Event_Link_Lanes(){
	}
			
	public Event_Link_Lanes(_Scenario myScenario,List<_Link> links,boolean isrevert,double deltalanes) {
		this.targets = new ArrayList<_ScenarioElement>();
		this.resetToNominal = isrevert;
		for(_Link link : links)
			this.targets.add(ObjectFactory.createScenarioElement(link));
		this.deltalanes = deltalanes;
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Object jaxbobject) {
		Event jaxbe = (Event) jaxbobject;
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
		for(_ScenarioElement s : targets){
			if(s.getMyType().compareTo(_ScenarioElement.Type.link)!=0){
				SiriusErrorLog.addErrorMessage("wrong target type.");
				return false;
			}
		}
				
		return true;
	}

	@Override
	public void activate() throws SiriusException{
		double newlanes;
		for(_ScenarioElement s : targets){
			_Link targetlink = (_Link) s.getReference();
			if(resetToNominal)
				newlanes = ((com.relteq.sirius.jaxb.Link)targetlink).getLanes().doubleValue();
			else
				newlanes =  targetlink.get_Lanes();
			newlanes += deltalanes;
			setLinkLanes(targetlink,newlanes);
		}		
	}


}
