/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

import com.relteq.sirius.jaxb.Controller;
import com.relteq.sirius.jaxb.ScenarioElement;

/** Simple implementation of {@link InterfaceController}.
 * 
 * <p> This is the base class for all controllers contained in a scenario. 
 * It provides a full default implementation of <code>InterfaceController</code>
 * so that extended classes need only implement a portion of the interface.
 *
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public abstract class _Controller implements InterfaceComponent,InterfaceController {
	
	protected _Scenario myScenario;										       								       
	protected String name;						// This is used only for controller on/off events.
																	// would prefer to reference contorllers by id. 
	protected _Controller.Type myType;
	protected ArrayList<_ScenarioElement> targets;
	protected ArrayList<_ScenarioElement> feedbacks;
	protected Double [] control_maxflow;		// [veh/simultaion time period] indexed by target	
	protected Double [] control_maxspeed;		// [-]	 indexed by target
	protected double dtinseconds;
	protected int samplesteps;
	protected boolean ison;
	protected static enum Type {  IRM_alinea,
					   		      IRM_time_of_day,
							      IRM_traffic_responsive,
							      CRM_swarm,
							      CRM_hero,
							      VSL_time_of_day,
							      SIG_pretimed,
							      SIG_actuated };
								      
//	protected static enum QueueControlType	{NULL, queue_override,
//											       proportional,
//											       proportional_integral  };
	
	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	 protected _Controller(){}
							      
	/////////////////////////////////////////////////////////////////////
	// registration
	/////////////////////////////////////////////////////////////////////

   	/** DESCRIPTION
   	 * 
   	 */
	protected boolean registerFlowController(_Link link,int index){
		if(link==null)
			return false;
		else
			return link.registerFlowController(this,index);
	}

   	/** DESCRIPTION
   	 * 
   	 */
	protected boolean registerSpeedController(_Link link,int index){
		if(link==null)
			return false;
		else
			return link.registerSpeedController(this,index);
	}

//   	/** DESCRIPTION
//   	 * 
//   	 */
//	protected boolean registerSplitRatioController(_Node node,int index){
//		if(node==null)
//			return false;
//		else
//			return node.registerSplitRatioController(this,index);
//	}
	
	/////////////////////////////////////////////////////////////////////
	// InterfaceComponent
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
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
		
		control_maxflow  = new Double [targets.size()];
		control_maxspeed = new Double [targets.size()];

		// store feedbacks ......
		feedbacks = new ArrayList<_ScenarioElement>();
		if(c.getFeedbackElements()!=null)
			for(ScenarioElement s : c.getFeedbackElements().getScenarioElement()){
				_ScenarioElement se = ObjectFactory.createScenarioElementFromJaxb(myScenario,s);
				if(se!=null)
					feedbacks.add(se);	
			}

	}

	/** @y.exclude */
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

	/** @y.exclude */
	public void reset() {
		ison = true;
	}

}
