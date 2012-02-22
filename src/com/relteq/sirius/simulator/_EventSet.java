/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;
import java.util.Collections;

import com.relteq.sirius.jaxb.Event;

final class _EventSet extends com.relteq.sirius.jaxb.EventSet {

	protected _Scenario myScenario;
	protected boolean isdone;			// true if we are done with events
	protected int currentevent;
	protected ArrayList<_Event> _sortedevents = new ArrayList<_Event>();
	
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	@SuppressWarnings("unchecked")
	protected void addEvent(_Event E){
				
		// add event to list
		_sortedevents.add(E);
		
		// re-sort
		Collections.sort(_sortedevents);

	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	@SuppressWarnings("unchecked")
	protected void populate(_Scenario myScenario) {
		
		this.myScenario = myScenario;

		if(myScenario.getEventSet()!=null){
			for(Event event : myScenario.getEventSet().getEvent() ){
				
				// keep only enabled events
				if(event.isEnabled()){
	
					// assign type
					_Event.Type myType;
			    	try {
						myType = _Event.Type.valueOf(event.getType());
					} catch (IllegalArgumentException e) {
						System.out.println("Warning: event has wrong type. Ignoring.");
						continue;
					}	
					
					// generate event
					if(myType!=_Event.Type.NULL){
						_Event E = ObjectFactory.createEventFromJaxb(myScenario,event,myType);
						if(E!=null)
							_sortedevents.add(E);
					}
				}
			}
		}
		
		// sort the events by timestamp, etc
		Collections.sort(_sortedevents);
	}

	protected boolean validate() {
		_Event previousevent = null;
		for(_Event event : _sortedevents){
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
		isdone = _sortedevents.isEmpty();
	}

	protected void update() {

		if(isdone)
			return;
		
		if(_sortedevents.isEmpty()){
			isdone=true;
			return;
		}

		// check whether next event is due
		while(myScenario.clock.getCurrentstep()>=_sortedevents.get(currentevent).timestampstep){
			_sortedevents.get(currentevent).activate(); 
			currentevent++;
			
			// don't come back if done
			if(currentevent==_sortedevents.size()){
				isdone=true;
				break;
			}
		}
	}

}
