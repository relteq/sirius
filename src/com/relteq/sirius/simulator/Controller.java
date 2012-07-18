/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;
import java.util.Collections;

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
	
	/** Activation times for this controller */
	protected ArrayList<ActivationTimes> activationTimes;
	
	/** Table of parameters. */
	protected Table table;
	
	
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
			return false;
		else
			return link.registerFlowController(this,index);
	}

   	/** Use this method within {@link InterfaceController#deregister} to deregister
   	 * speed control with a target link. The return value is <code>true</code> if
   	 * the deregistration is successful, and <code>false</code> otherwise. 
   	 * @param link The target link for speed control.
   	 * @return A boolean indicating success of the deregistration. 
   	 */
	protected boolean deregisterSpeedController(Link link){
		if(link!=null)			
			return link.deregisterSpeedController(this);
		else
			return false;
	}
	
	/** Use this method within {@link InterfaceController#deregister} to deregister
   	 * flow control with a target link. The return value is <code>true</code> if
   	 * the deregistration is successful, and <code>false</code> otherwise. 
   	 * @param link The target link for flow control.
   	 * @return A boolean indicating success of the deregistration. 
   	 */
	protected boolean deregisterFlowController(Link link){
		if(link==null)
			return false;
		else
			return link.deregisterFlowController(this);
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
	
	
	// Returns the start and end times of the controller.
	
	
	protected double myStartTime(){
		double starttime=myScenario.getTimeStart();
		for (int ActTimesIndex = 0; ActTimesIndex < activationTimes.size(); ActTimesIndex++ )
			if (ActTimesIndex == 0)
				starttime=activationTimes.get(ActTimesIndex).getBegintime();
			else
				starttime=Math.min(starttime,activationTimes.get(ActTimesIndex).getBegintime());
		
		return starttime;
	}
	
	protected double myEndTime(){
		double endtime=myScenario.getTimeEnd();
		for (int ActTimesIndex = 0; ActTimesIndex < activationTimes.size(); ActTimesIndex++ )
			if (ActTimesIndex == 0)
				endtime=activationTimes.get(ActTimesIndex).getEndtime();
			else
				endtime=Math.max(endtime,activationTimes.get(ActTimesIndex).getEndtime());
		
		return endtime;
	}
	/////////////////////////////////////////////////////////////////////
	// InterfaceComponent
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected final void populateFromJaxb(Scenario myScenario,com.relteq.sirius.jaxb.Controller c,Controller.Type myType){
		this.myScenario = myScenario;
		this.myType = myType;
		this.id = c.getId();
		this.ison = false; //c.isEnabled(); 
		this.activationTimes=new ArrayList<ActivationTimes>();
		dtinseconds = c.getDt().floatValue();		// assume given in seconds
		samplesteps = SiriusMath.round(dtinseconds/myScenario.getSimDtInSeconds());		
		
		// Copy table
		if (c.getTable()!=null)
			this.table = new Table(c.getTable());
		// Get activation times and sort	
		if (c.getActivationIntervals()!=null)
			for (com.relteq.sirius.jaxb.Interval tinterval : c.getActivationIntervals().getInterval()){			
				if(tinterval!=null){				
					activationTimes.add(new ActivationTimes(tinterval.getStartTime().doubleValue(),tinterval.getEndTime().doubleValue()));
				}
			}		
			
		Collections.sort(activationTimes);
		
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
		
		// check that type was read correctly
		if(myType==null){
			SiriusErrorLog.addErrorMessage("Controller has the wrong type.");
			return false;
		}
		
		// check that the target is valid
		if(targets==null){
			SiriusErrorLog.addErrorMessage("Target is invalid or has multiple controllers.");
			return false;
		}
		
		// check that sample dt is an integer multiple of network dt
		if(!SiriusMath.isintegermultipleof(dtinseconds,myScenario.getSimDtInSeconds())){
			SiriusErrorLog.addErrorMessage("Controller sample time must be integer multiple of simulation time step.");
			return false;
		}
		
		// check that activation times are valid.
		for (int ActTimesIndex = 0; ActTimesIndex < activationTimes.size(); ActTimesIndex++ ){
			if(!activationTimes.get(ActTimesIndex).validate()){
				SiriusErrorLog.addErrorMessage("ActivationTimes must have a valid time interval.");
				return false;
			}
			if (ActTimesIndex<activationTimes.size()-1){
				if(!activationTimes.get(ActTimesIndex).validateWith(activationTimes.get(ActTimesIndex+1))){
					SiriusErrorLog.addErrorMessage("Activation Periods of the controllers must not overlap.");
					return false;
				}
			}
		}
		return true;
	}

	/** @y.exclude */
	public void reset() {
		//switch on conroller if it is always on by default.
		if (activationTimes==null)
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
	
	@SuppressWarnings("rawtypes")
	/** Creates a new class that stores begin and end times for each period of controller activation */
	protected class ActivationTimes implements Comparable{
		/** Start time for each activation interval */
		protected double begintime; 
		/** End time for each activation interval */
		protected double endtime;
		
		protected ActivationTimes(double begintime, double endtime) {
			super();
			this.begintime = begintime;
			this.endtime = endtime;
		}
		public double getBegintime() {
			return begintime;
		}
		protected void setBegintime(double begintime) {
			this.begintime = begintime;
		}
		public double getEndtime() {
			return endtime;
		}
		protected void setEndtime(double endtime) {
			this.endtime = endtime;
		}		
		
		protected boolean validate(){			
			if (begintime-endtime>=0)
				return false;
			return true;			  
		}
		
		protected boolean validateWith(ActivationTimes that){			
			if (Math.max(this.begintime-that.getEndtime(), that.getBegintime()-this.endtime)<0)  // Assumption - activation times is sorted before this is invoked, should remove this assumption later.
				return false;
			return true;			  
		}
		/////////////////////////////////////////////////////////////////////
		// Comparable
		/////////////////////////////////////////////////////////////////////		
		
		public int compareTo(Object arg0) {
			if(arg0==null)
				return 1;
			ActivationTimes that = (ActivationTimes) arg0;
			
			// Order first by begintimes.
			int compare = ((Double) this.getBegintime()).compareTo((Double) that.getBegintime());
		
			if (compare!=0)
				return compare;
				
		    // Order next by endtimes.
			compare = ((Double) this.getEndtime()).compareTo((Double) that.getEndtime());
				
			return compare;
				
				
		}
		
	}
	
	
}
