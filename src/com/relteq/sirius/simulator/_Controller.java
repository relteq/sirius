/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

import com.relteq.sirius.jaxb.Controller;
import com.relteq.sirius.jaxb.ScenarioElement;

/** DESCRIPTION OF THE CLASS
*
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public abstract class _Controller implements InterfaceController {
	
	public static enum Type {NULL, IRM_alinea,
						   		      IRM_time_of_day,
								      IRM_traffic_responsive,
								      CRM_swarm,
								      CRM_hero,
								      VSL_time_of_day,
								      SIG_pretimed,
								      SIG_actuated };
										  
	protected static enum QueueControlType	{NULL, queue_override,
											       proportional,
											       proportional_integral  };
					
	protected _Scenario myScenario;										       
	protected String name;			// This is used only for controller on/off events.
									// would prefer to reference contorllers by id. 
	protected _Controller.Type myType;
	protected ArrayList<_ScenarioElement> targets;
	protected ArrayList<_ScenarioElement> feedbacks;

	protected double dtinseconds;
	protected int samplesteps;
	protected boolean ison;
	
	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////
	
	public final _Controller.Type getMyType() {
		return myType;
	}
	
	/////////////////////////////////////////////////////////////////////
	// actuation
	/////////////////////////////////////////////////////////////////////
	
	protected final void setLinkMaxFlow(double desiredvehrate){
		if(!ison)
			return;
		double rate = desiredvehrate;
//		rate = Math.max(rate, minrate);
//		rate = Math.min(rate, maxrate);
		for(_ScenarioElement s: targets){
			((_Link)s.reference).setControl_maxflow( rate );
		}
	}
	
//	protected final void setNodeSplitRatio(...){
//	if(!ison)
//	return;
//	}
	
//	protected final void set LinkMaxSpeed(){
//	if(!ison)
//	return;	
//	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / validate / reset / update
	/////////////////////////////////////////////////////////////////////

	protected final void populateFromJaxb(_Scenario myScenario,Controller c,_Controller.Type myType){
		this.myScenario = myScenario;
		this.myType = myType;
		this.name = c.getName();
		this.ison = true;
		dtinseconds = c.getDt().floatValue();		// assume given in seconds
		samplesteps = SiriusMath.round(dtinseconds/myScenario.getSimDtInSeconds());

		// store targets ......
		targets = new ArrayList<_ScenarioElement>();
		if(c.getTargetElements()!=null)
			for(ScenarioElement s : c.getTargetElements().getScenarioElement() ){
				_ScenarioElement se = ObjectFactory.createScenarioElementFromJaxb(myScenario,s);
				if(se!=null)
					targets.add(se);
			}

		// store feedbacks ......
		feedbacks = new ArrayList<_ScenarioElement>();
		if(c.getFeedbackElements()!=null)
			for(ScenarioElement s : c.getFeedbackElements().getScenarioElement()){
				_ScenarioElement se = ObjectFactory.createScenarioElementFromJaxb(myScenario,s);
				if(se!=null)
					feedbacks.add(se);	
			}
		
		// register controller with targets
		for(_ScenarioElement s : targets){
			boolean registersuccess = false;
			switch(s.myType){
			case link:
				_Link link = myScenario.getLinkWithCompositeId(s.network_id,s.id);
				registersuccess = link.registerController();
			case node:
				_Node node = myScenario.getNodeWithCompositeId(s.network_id,s.id);
				registersuccess = node.registerController();	
			}
			if(!registersuccess){
				targets = null;		// cause validation failure.
				return;
			}
		}

	}
	
	public boolean validate() {
		
		// check that the target is valid
		if(targets==null){
			System.out.println("Target is invalid or has multiple controllers.");
			return false;
		}
		
		// check that sample dt is an integer multiple of network dt
		if(!SiriusMath.isintegermultipleof(dtinseconds,myScenario.getSimDtInSeconds())){
			System.out.println("Controller sample time must be integer multiple of simulation time step.");
			return false;
		}
		
		return true;
	}

	public void reset() {
		ison = true;
	}
	
	public void update() {
	}

}
