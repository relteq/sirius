/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;
import java.util.Collections;

import com.relteq.sirius.jaxb.Event;

final class _EventSet extends com.relteq.sirius.jaxb.EventSet {

	private boolean isdone;			// true if we are done with events
	private int currentevent;
	private ArrayList<_Event> _sortedevents = new ArrayList<_Event>();
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	@SuppressWarnings("unchecked")
	protected void populate() {
		
		// populate the events
		if(Utils.theScenario.getEventSet()!=null)
			for(Event event : Utils.theScenario.getEventSet().getEvent() ){
				
				// keep only enabled events
				if(event.isEnabled()){

					_Event.Type myType;
			    	try {
						myType = _Event.Type.valueOf(event.getType());
					} catch (IllegalArgumentException e) {
						myType = _Event.Type.NULL;
						return;
					}	
					
					// generate event
					if(myType!=_Event.Type.NULL)
						_sortedevents.add(new _Event(event,myType));
				}
			}
		
		// sort the events by timestamp, etc
		Collections.sort(_sortedevents);
		
		isdone = _sortedevents.isEmpty();
		currentevent = 0;
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
		isdone = false;
	}

	protected void update() {

		if(isdone)
			return;
		
		if(getEvent().isEmpty())
			return;

		// check whether next event is due
		while(_sortedevents.get(currentevent).getTimestampstep()==Utils.clock.getCurrentstep()){
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
