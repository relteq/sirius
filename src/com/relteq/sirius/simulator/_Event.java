/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

import com.relteq.sirius.jaxb.DemandProfile;
import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.jaxb.FundamentalDiagram;
import com.relteq.sirius.jaxb.ScenarioElement;

/** Simple implementation of {@link InterfaceEvent}.
 * 
 * <p> This is the base class for all events contained in a scenario. 
 * It provides a full default implementation of <code>InterfaceEvent</code>
 * so that extended classes need only implement a portion of the interface.
 *
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
@SuppressWarnings("rawtypes")
public abstract class _Event extends com.relteq.sirius.jaxb.Event implements Comparable,InterfaceComponent,InterfaceEvent {

	/** Scenario that contains this event */
	protected _Scenario myScenario;
	
	/** Event type. */
	protected _Event.Type myType;
	
	/** Activation time of the event, in number of simulation time steps. */
	protected int timestampstep;
	
	/** List of targets for the event. */
	protected ArrayList<_ScenarioElement> targets;
	
	/** Possible event type. */
	public static enum Type	{  
		/** see {@link ObjectFactory#createEvent_Fundamental_Diagram} 	*/ fundamental_diagram,
		/** see {@link ObjectFactory#createEvent_Link_Demand_Knob} 		*/ link_demand_knob,
		/** see {@link ObjectFactory#createEvent_Link_Lanes} 			*/ link_lanes, 
		/** see {@link ObjectFactory#createEvent_Node_Split_Ratio} 		*/ node_split_ratio,
		/** see {@link ObjectFactory#createEvent_Control_Toggle} 		*/ control_toggle,
		/** see {@link ObjectFactory#createEvent_Global_Control_Toggle} */ global_control_toggle,
		/** see {@link ObjectFactory#createEvent_Global_Demand_Knob} 	*/ global_demand_knob };
		   
	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected _Event(){}
							  
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	/** @y.exclude */
	protected void populateFromJaxb(_Scenario myScenario,Event jaxbE,_Event.Type myType){
		this.myScenario = myScenario;
		this.myType = myType;
		this.timestampstep = SiriusMath.round(jaxbE.getTstamp().floatValue()/myScenario.getSimDtInSeconds());		// assume in seconds
		this.targets = new ArrayList<_ScenarioElement>();
		if(jaxbE.getTargetElements()!=null)
			for(ScenarioElement s : jaxbE.getTargetElements().getScenarioElement() )
				this.targets.add(ObjectFactory.createScenarioElementFromJaxb(myScenario,s));
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceComponent
	/////////////////////////////////////////////////////////////////////
	
	public boolean validate() {
		
		if(myType==null){
			SiriusErrorLog.addErrorMessage("Event has bad type.");
			return false;
		}
			
		// check that there are targets assigned to non-global events
		if(myType!=_Event.Type.global_control_toggle && myType!=_Event.Type.global_demand_knob)
			if(targets.isEmpty()){
				SiriusErrorLog.addErrorMessage("No targets assigned.");
				return false;
			}
		
		// check each target is valid
		for(_ScenarioElement s : targets){
			if(s.reference==null){
				SiriusErrorLog.addErrorMessage("Invalid target.");
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void reset() {
		return;
	}

	@Override
	public void update() {
		return;
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
		Integer thiststamp = this.timestampstep;
		Integer thattstamp = that.timestampstep;
		compare = thiststamp.compareTo(thattstamp);
		if(compare!=0)
			return compare;

		// second ordering by event type
		_Event.Type thiseventtype = this.myType;
		_Event.Type thateventtype = that.myType;
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
			_ScenarioElement.Type thistargettype = this.targets.get(i).myType;
			_ScenarioElement.Type thattargettype = that.targets.get(i).myType;
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

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////	

	protected void setGlobalControlIsOn(boolean ison){
		myScenario.global_control_on = ison;
	}
	
	protected void setControllerIsOn(_Controller c,boolean ison){
		if(c==null)
			return;
		c.ison = ison;
	}

    protected void setLinkLanes(_Link link,double lanes) throws SiriusException{
		if(link==null)
			return;
    	link.set_Lanes(lanes);
    }
    	
	protected void setLinkFundamentalDiagram(_Link link,FundamentalDiagram newFD) throws SiriusException{
		if(link==null)
			return;
		link.activateFundamentalDiagramEvent(newFD);
	}
	
    protected void revertLinkFundamentalDiagram(_Link link) throws SiriusException{
    	if(link==null)
    		return;
    	link.revertFundamentalDiagramEvent();
    }    

	protected void setNodeEventSplitRatio(_Node node,Double3DMatrix x) {
		if(node==null)
			return;
		if(!node.validateSplitRatioMatrix(x))
			return;
		node.setSplitratio(x);
		node.hasactivesplitevent = true;
	}

	protected void revertNodeEventSplitRatio(_Node node) {
		if(node==null)
			return;
		if(node.hasactivesplitevent){
			node.resetSplitRatio();
			node.hasactivesplitevent = false;
		}
	}
	
    protected void setDemandProfileEventKnob(DemandProfile profile,Double knob){
		if(profile==null)
			return;
		if(knob.isNaN())
			return;
		((_DemandProfile) profile).set_knob(knob);
    }
    
    protected void setGlobalDemandEventKnob(Double knob){
    	myScenario.global_demand_knob = knob;
    }
	
}
