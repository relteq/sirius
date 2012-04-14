/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;
import java.util.Collections;

final class EventSet extends com.relteq.sirius.jaxb.EventSet {

	protected Scenario myScenario;
	protected boolean isdone;			// true if we are done with events
	protected int currentevent;
	protected ArrayList<Event> sortedevents = new ArrayList<Event>();
	
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	@SuppressWarnings("unchecked")
	protected void addEvent(Event E){
				
		// add event to list
		sortedevents.add(E);
		
		// re-sort
		Collections.sort(sortedevents);

	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	@SuppressWarnings("unchecked")
	protected void populate(Scenario myScenario) {
		
		this.myScenario = myScenario;

		if(myScenario.getEventSet()!=null){
			for(com.relteq.sirius.jaxb.Event event : myScenario.getEventSet().getEvent() ){
				
				// keep only enabled events
				if(event.isEnabled()){
	
					// assign type
					Event.Type myType;
			    	try {
						myType = Event.Type.valueOf(event.getType());
					} catch (IllegalArgumentException e) {
						continue;
					}	
					
					// generate event
					if(myType!=null){
						Event E = ObjectFactory.createEventFromJaxb(myScenario,event,myType);
						if(E!=null)
							sortedevents.add(E);
					}
				}
			}
		}
		
		// sort the events by timestamp, etc
		Collections.sort(sortedevents);
	}

	protected boolean validate() {
		Event previousevent = null;
		for(Event event : sortedevents){
			if(!event.validate())
				return false;
			
			// disallow pairs of events with equal time stamp, target, and type.
			if(previousevent!=null){
				if(event.equals(previousevent))
					return false;
			}
			previousevent = event;
		}
		return true;
	}

	protected void reset() {
		currentevent = 0;
		isdone = sortedevents.isEmpty();
	}

	protected void update() throws SiriusException {

		if(isdone)
			return;
		
		if(sortedevents.isEmpty()){
			isdone=true;
			return;
		}

		// check whether next event is due
		while(myScenario.clock.getCurrentstep()>=sortedevents.get(currentevent).timestampstep){
			
			Event event =  sortedevents.get(currentevent);
			if(event.validate())
				event.activate(); 
			currentevent++;
			
			// don't come back if done
			if(currentevent==sortedevents.size()){
				isdone=true;
				break;
			}
		}
	}

}
