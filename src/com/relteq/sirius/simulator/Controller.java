/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

/** Base implementation of {@link InterfaceController}.
 * 
 * <p> This is the base class for all controllers contained in a scenario. 
 * It provides a full default implementation of <code>InterfaceController</code>
 * so that extended classes need only implement a portion of the interface.
 *
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public abstract class Controller implements InterfaceComponent,InterfaceController {
	
	/** Scenario that contains this controller */
	protected Scenario myScenario;										       								       
	
	/** Id of the controller. */
	protected String id;
										
	/** Controller type. */
	protected Controller.Type myType;
	
	/** List of scenario elements affected by this controller */
	protected ArrayList<ScenarioElement> targets;
	
	/** List of scenario elements that provide input to this controller */
	protected ArrayList<ScenarioElement> feedbacks;
	
	/** Maximum flow for link targets, in vehicles per simulation time period. Indexed by target.  */
	protected Double [] control_maxflow;
	
	/** Maximum flow for link targets, in normalized units. Indexed by target.  */
	protected Double [] control_maxspeed;
	
	/** Controller update period in seconds */
	protected double dtinseconds;
	
	/** Controller update period in number of simulation steps */
	protected int samplesteps;
	
	/** On/off switch for this controller */
	protected boolean ison;
	
	/** Controller algorithm. The three-letter prefix indicates the broad class of the 
	 * controller.  
	 * <ul>
	 * <li> IRM, isolated ramp metering </li>
	 * <li> CRM, coordinated ramp metering </li>
	 * <li> VSL, variable speed limits </li>
	 * <li> SIG, signal control (intersections) </li>
	 * </ul>
	 */
	protected static enum Type {  
	  /** see {@link ObjectFactory#createController_IRM_Alinea} 			*/ 	IRM_alinea,
	  /** see {@link ObjectFactory#createController_IRM_Time_of_Day} 		*/ 	IRM_time_of_day,
	  /** see {@link ObjectFactory#createController_IRM_Traffic_Responsive}	*/ 	IRM_traffic_responsive,
	  /** see {@link ObjectFactory#createController_CRM_SWARM}				*/ 	CRM_swarm,
      /** see {@link ObjectFactory#createController_CRM_HERO}				*/ 	CRM_hero,
      /** see {@link ObjectFactory#createController_VSL_Time_of_Day}		*/ 	VSL_time_of_day,
      /** see {@link ObjectFactory#createController_SIG_Pretimed}			*/ 	SIG_pretimed,
      /** see {@link ObjectFactory#createController_SIG_Actuated}			*/ 	SIG_actuated };
								      
//	protected static enum QueueControlType	{NULL, queue_override,
//											       proportional,
//											       proportional_integral  };
	
	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	 protected Controller(){}
	
	/** @y.exclude */
	 protected Controller(ArrayList<ScenarioElement> targets){
		 this.targets = targets;
		 this.control_maxflow  = new Double [targets.size()];
		 this.control_maxspeed = new Double [targets.size()];
	 }
	
		 
	/////////////////////////////////////////////////////////////////////
	// registration
	/////////////////////////////////////////////////////////////////////

   	/** Use this method within {@link InterfaceController#register} to register
   	 * flow control with a target link. The return value is <code>true</code> if
   	 * the registration is successful, and <code>false</code> otherwise. 
   	 * @param link The target link for flow control.
   	 * @param index The index of the link in the controller's list of targets.
   	 * @return A boolean indicating success of the registration. 
   	 */
	protected boolean registerFlowController(Link link,int index){
		if(link==null)
			return true;
		else
			return link.registerFlowController(this,index);
	}

   	/** Use this method within {@link InterfaceController#register} to register
   	 * speed control with a target link. The return value is <code>true</code> if
   	 * the registration is successful, and <code>false</code> otherwise. 
   	 * @param link The target link for speed control.
   	 * @param index The index of the link in the controller's list of targets.
   	 * @return A boolean indicating success of the registration. 
   	 */
	protected boolean registerSpeedController(Link link,int index){
		if(link==null)
			return true;
		else
			return link.registerSpeedController(this,index);
	}
	
	
//   	/** DESCRIPTION
//   	 * 
//   	 */
//	protected boolean registerSplitRatioController(_Node node,int index){
//		if(node==null)
//			return true;
//		else
//			return node.registerSplitRatioController(this,index);
//	}
	
	/////////////////////////////////////////////////////////////////////
	// InterfaceComponent
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected final void populateFromJaxb(Scenario myScenario,com.relteq.sirius.jaxb.Controller c,Controller.Type myType){
		this.myScenario = myScenario;
		this.myType = myType;
		this.id = c.getId();
		this.ison = true;
		dtinseconds = c.getDt().floatValue();		// assume given in seconds
		samplesteps = SiriusMath.round(dtinseconds/myScenario.getSimDtInSeconds());

		// store targets ......
		targets = new ArrayList<ScenarioElement>();
		if(c.getTargetElements()!=null)
			for(com.relteq.sirius.jaxb.ScenarioElement s : c.getTargetElements().getScenarioElement() ){
				ScenarioElement se = ObjectFactory.createScenarioElementFromJaxb(myScenario,s);
				if(se!=null)
					targets.add(se);
			}
		
		control_maxflow  = new Double [targets.size()];
		control_maxspeed = new Double [targets.size()];

		// store feedbacks ......
		feedbacks = new ArrayList<ScenarioElement>();
		if(c.getFeedbackElements()!=null)
			for(com.relteq.sirius.jaxb.ScenarioElement s : c.getFeedbackElements().getScenarioElement()){
				ScenarioElement se = ObjectFactory.createScenarioElementFromJaxb(myScenario,s);
				if(se!=null)
					feedbacks.add(se);	
			}

	}

	/** @y.exclude */
	public boolean validate() {
		
		// check that type was reaad correctly
		if(myType==null){
			SiriusErrorLog.addError("Controller with id=" + getId() + " has the wrong type.");
			return false;
		}
		
		// check that the target is valid
		if(targets==null){
			SiriusErrorLog.addError("Invalid target for controller id=" + getId());
			return false;
		}
		
		// check that sample dt is an integer multiple of network dt
		if(!SiriusMath.isintegermultipleof(dtinseconds,myScenario.getSimDtInSeconds())){
			SiriusErrorLog.addError("Time step for controller id=" +getId() + " is not a multiple of the simulation time step.");
			return false;
		}
		return true;
	}

	/** @y.exclude */
	public void reset() {
		ison = true;
	}
	
	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////

	public String getId() {
		return id;
	}	

	public Controller.Type getMyType() {
		return myType;
	}

	public ArrayList<ScenarioElement> getTargets() {
		return targets;
	}

	public ArrayList<ScenarioElement> getFeedbacks() {
		return feedbacks;
	}
	
	public double getDtinseconds() {
		return dtinseconds;
	}

	public boolean isIson() {
		return ison;
	}
	
}
