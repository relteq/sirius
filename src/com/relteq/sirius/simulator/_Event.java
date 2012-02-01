/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

import com.relteq.sirius.jaxb.DemandProfile;
import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.jaxb.ScenarioElement;

@SuppressWarnings("rawtypes")
public class _Event extends com.relteq.sirius.jaxb.Event implements Comparable {

	protected Types.Event myType;
	protected int timestampstep;
	protected ArrayList<_ScenarioElement> targets;
	
	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////
	
	public _Event(){}
	
	public _Event(Event e,Types.Event myType){
		this.myType = myType;
		timestampstep = Utils.round(e.getTstamp().floatValue()/Utils.simdtinseconds);		// assume in seconds
		targets = new ArrayList<_ScenarioElement>();
		for(ScenarioElement s : e.getTargetElements().getScenarioElement() )
			targets.add(new _ScenarioElement(s));
	}
	
	/////////////////////////////////////////////////////////////////////
	// interface
	/////////////////////////////////////////////////////////////////////
	
	public Types.Event getMyType() {
		return myType;
	}
	
	public int getTimestampstep() {
		return timestampstep;
	}

	/////////////////////////////////////////////////////////////////////
	// reset / validate / activate
	/////////////////////////////////////////////////////////////////////
	
	protected boolean validate() {
		// check that there are targets assigned
		if(targets.isEmpty()){
			System.out.println("No targets assigned.");
			return false;
		}
		
		// check each target is valid
		for(_ScenarioElement s : targets){
			
			if(s.reference==null){
				System.out.println("Invalid target.");
				return false;
			}
			
			switch(this.myType){	
			
			// these apply to links only
			case fundamental_diagram:
			case link_demand_knob:
			case link_lanes:
				if(s.myType!=Types.ScenarioElement.link){
					System.out.println("wrong target type.");
					return false;
				}
				break;
				
			// these apply to nodes only
			case node_split_ratio:
				if(s.myType!=Types.ScenarioElement.node){
					System.out.println("wrong target type.");
					return false;
				}
				break;
			
			// these apply to controllers only
			case control_toggle:
				if(s.myType!=Types.ScenarioElement.controller){
					System.out.println("wrong target type.");
					return false;
				}
				break;
			}

		}
		return true;
	}

	protected void activate() {

		switch(myType){
		
		// Link events =====================================================
		case fundamental_diagram:
			for(_ScenarioElement s : targets){
				_Link targetlink = (_Link) s.reference;
				if(isResetToNominal()){
					targetlink.deactivateFDEvent();
				}
				else{
					_FundamentalDiagram eventFD = new _FundamentalDiagram(targetlink);
					eventFD.copyfrom((targetlink).getCurrentFD());		// copy current FD
					eventFD.copyfrom(this.getFundamentalDiagram());		// replace values with those defined in the event
					if(eventFD.validate()){								// validate the result
						//targetlink.setEventFundamentalDiagram(eventFD);
						targetlink.activateFDEvent(eventFD);
					}
				}
				break;
			}
		
		// ....................................
		case link_demand_knob:
			for(_ScenarioElement s : targets){
		    	if(Utils.theScenario.getDemandProfileSet()!=null){
		        	for(DemandProfile profile : Utils.theScenario.getDemandProfileSet().getDemandProfile()){
		        		if(profile.getLinkIdOrigin().equals(s.id)){
		        			double newknob;
		        			if(isResetToNominal())
		        				newknob = profile.getKnob().doubleValue();
		        			else
		        				newknob = getKnob().getValue().doubleValue();
		        			((_DemandProfile) profile).set_knob( newknob );
		        			break;
		        		}
		        	}
		    	}
			}
			break;

		// ....................................
		case link_lanes:
			for(_ScenarioElement s : targets){
				_Link targetlink = (_Link) s.reference;
				if(getLaneCountChange()!=null){
					if(isResetToNominal()){
						double originallanes = ((com.relteq.sirius.jaxb.Link)targetlink).getLanes().doubleValue();
						targetlink.set_Lanes(originallanes);
					}
					if(getLaneCountChange().getDelta()!=null){
						double deltalanes = getLaneCountChange().getDelta().doubleValue();
						targetlink.setLanesDelta(deltalanes);
					}
				}
			}
			break;
		
		// ....................................
		case control_toggle:
			for(_ScenarioElement s : targets){
				_Controller c = Utils.getControllerWithId(s.id);
				c.ison = getOnOffSwitch().getValue().equalsIgnoreCase("on");
			}			
			break;
		
		// Node events =====================================================
		case node_split_ratio:
			
			if(isResetToNominal()){
				for(_ScenarioElement s : targets){
					_Node targetnode = (_Node) s.reference;
					targetnode.removeEventSplitratio();
				}
			}
			else{
				for(_ScenarioElement s : targets){
					_Node targetnode = (_Node) s.reference;
					Double3DMatrix splitratio = new Double3DMatrix(0,0,0,0d);
					if(_SplitRatiosProfile.validateSplitRatioMatrix(splitratio,targetnode))
						targetnode.setEventSplitratio(splitratio);
				}
			}
			break;
			
		// Global events =====================================================
			
		case global_demand_knob:
	    	if(Utils.theScenario.getDemandProfileSet()!=null)
	        	for(DemandProfile profile : Utils.theScenario.getDemandProfileSet().getDemandProfile() ){
	        		double knobvalue;
	    			if(isResetToNominal())
	    				knobvalue = ((_DemandProfile) profile).getKnob().doubleValue();
	    			else
	    				knobvalue = getKnob().getValue().doubleValue();
	        		((_DemandProfile) profile).set_knob( knobvalue );
	        	}
			break;
		
       	// ....................................
		case global_control_toggle:
			Utils.controlon = getOnOffSwitch().getValue().equalsIgnoreCase("on");
			break;
		}
	}

	/////////////////////////////////////////////////////////////////////
	// Comparable
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public int compareTo(Object arg0) {
		
		if(arg0==null)
			return 1;
		
		int compare;
		_Event that = ((_Event) arg0);
		
		// first ordering by time stamp
		Integer thiststamp = this.getTimestampstep();
		Integer thattstamp = that.getTimestampstep();
		compare = thiststamp.compareTo(thattstamp);
		if(compare!=0)
			return compare;

		// second ordering by event type
		Types.Event thiseventtype = this.getMyType();
		Types.Event thateventtype = that.getMyType();
		compare = thiseventtype.compareTo(thateventtype);
		if(compare!=0)
			return compare;
		
		// third ordering by number of targets
		Integer thisnumtargets = this.targets.size();
		Integer thatnumtargets = that.targets.size();
		compare = thisnumtargets.compareTo(thatnumtargets);
		if(compare!=0)
			return compare;
		
		// fourth ordering by target type
		for(int i=0;i<thisnumtargets;i++){
			Types.ScenarioElement thistargettype = this.targets.get(i).myType;
			Types.ScenarioElement thattargettype = that.targets.get(i).myType;
			compare = thistargettype.compareTo(thattargettype);
			if(compare!=0)
				return compare;		
		}

		// fifth ordering by target id
		for(int i=0;i<thisnumtargets;i++){
			String thistargetId = this.targets.get(i).id;
			String thattargetId = that.targets.get(i).id;
			compare = thistargetId.compareTo(thattargetId);
			if(compare!=0)
				return compare;
		}

		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null)
			return false;
		else
			return this.compareTo((_Event) obj)==0;
	}

}
