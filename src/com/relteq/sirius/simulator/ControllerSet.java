/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;
import java.util.Collections;

final class ControllerSet extends com.relteq.sirius.jaxb.ControllerSet {

	protected Scenario myScenario;
	protected ArrayList<Controller> controllers = new ArrayList<Controller>();
	
	protected enum OperationType {Deactivate,Activate}; 
	protected ArrayList<ActivationCommand> activations;
	protected ArrayList<Integer> activeControllerIndex;
	protected int activationindex;
	
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	protected ArrayList<Controller> get_Controllers(){
		return controllers;
	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario) {

		this.myScenario = myScenario;
		this.activations = new ArrayList<ActivationCommand>();
		this.activeControllerIndex = new ArrayList<Integer>();
		int tempindex = 0;		
		if(myScenario.getControllerSet()!=null){
			for(com.relteq.sirius.jaxb.Controller controller : myScenario.getControllerSet().getController()){
	
				// assign type
				Controller.Type myType;
		    	try {
					myType = Controller.Type.valueOf(controller.getType());
				} catch (IllegalArgumentException e) {
					continue;
				}	
				
				// generate controller
				if(myType!=null){
					Controller C = ObjectFactory.createControllerFromJaxb(myScenario,controller,myType);
					if(C!=null){
						controllers.add(tempindex,C);									
						for (Controller.ActivationTimes acttimes : C.activationTimes){
							if (acttimes!=null){								
								activations.add(new ActivationCommand(tempindex,acttimes.getBegintime(),OperationType.Activate));
								activations.add(new ActivationCommand(tempindex,acttimes.getEndtime(),OperationType.Deactivate));
							}
						}
						tempindex++;
					}
				}
			}			
		}
		
		Collections.sort(activations);
		activationindex=0;
	}

	protected void validate() {
		for(Controller controller : controllers)
			controller.validate();		
	}
	
	protected boolean register(){

		// For controllers that do not have activation times, validation does the initial registration,		
		// This is not added to the list of active controllers, because it is always active, thought we may need to change that.
		// We also validate each controller for its internal paramters.
		for(Controller controller : controllers){			
			if (controller.activationTimes==null)
				if (!controller.register()){
					SiriusErrorLog.addError("Controller registration failure, controller " + controller.getId());
					return false;
				}
		}

		// Check whether any two controllers are accessing the same link at any particular time.
		// An easy way to validate is to run through the sequence of activations and check registering/deregisterintg success.
		// We require all the controllers that are always active to be registered first!
		boolean validated = true;
		for (ActivationCommand activecmd : activations){
			if (activecmd!=null){
				if (activecmd.getOperation().equals(OperationType.Activate)){
					validated = controllers.get(activecmd.getIndex()).register();
					activeControllerIndex.add((Integer) activecmd.getIndex());				
				}
				else{
					validated = controllers.get(activecmd.getIndex()).deregister();
					controllers.get(activecmd.getIndex()).ison=false;
					activeControllerIndex.remove(activeControllerIndex.indexOf((Integer) activecmd.getIndex()));					
				}
				if (!validated){
					SiriusErrorLog.addError("Multiple controllers accessing the same link at the same time. Controller registration failure, controller " + controllers.get(activecmd.getIndex()).getId());
					return false;
				}
			}	
		}
		
		// However, you need to deregister the last set of registered controllers
		for(Integer controllerindex : activeControllerIndex)
			if(controllerindex!=null)
				controllers.get(controllerindex).deregister();
		
		activeControllerIndex.clear();
		
		return true;
	}

	protected void reset() {
		//reset controllers
		for(Controller controller : controllers)
			controller.reset();
		
		// Deregister previous active controllers
		for(Integer controllerindex : activeControllerIndex)
			if(controllerindex!=null)
				controllers.get(controllerindex).deregister();
		
		// Set activation index to zero, and process all events upto the starttime.
		activationindex = 0;
		processActivations(myScenario.clock.getStartTime());  	
		
	}
	
	// Process all events upto time t, starting from the activationindex
	protected void processActivations(double t){
		
		while (activationindex<activations.size() && activations.get(activationindex).getTime()<=t){
			ActivationCommand activecmd=activations.get(activationindex);
			
			if (activecmd!=null){
				if (activecmd.getOperation().equals(OperationType.Activate)){
					controllers.get(activecmd.getIndex()).register();
					controllers.get(activecmd.getIndex()).ison=true;
					controllers.get(activecmd.getIndex()).reset();
					activeControllerIndex.add((Integer) activecmd.getIndex()); 
				}
				else{
					controllers.get(activecmd.getIndex()).deregister();
					controllers.get(activecmd.getIndex()).ison=false;					
					activeControllerIndex.remove(activeControllerIndex.indexOf((Integer) activecmd.getIndex())); 
				}
			}
			activationindex++;
		}
	
	}
	
	protected void update() throws SiriusException {
		processActivations(myScenario.clock.getT());			
		
    	for(Controller controller : controllers)
    		if(controller.ison && myScenario.clock.istimetosample(controller.samplesteps,0))
    			controller.update();
	}
	
	/////////////////////////////////////////////////////////////////////
	// Setup a class to keep track of Activation/Deactivation times
	/////////////////////////////////////////////////////////////////////
	
	protected class ActivationCommand implements Comparable<ActivationCommand> {
		protected int index;
		protected double time;		
		protected OperationType operation;
		
		public int getIndex() {
			return index;
		}
		
		public double getTime() {
			return time;
		}
		
		public OperationType getOperation() {
			return operation;
		}
		
		public ActivationCommand(int index, double time, OperationType operation) {
			super();
			this.index = index;
			this.time = time;
			this.operation = operation;
		}
		
		public int compareTo(ActivationCommand o) {
			//first compare by times
			int compare = ((Double) time).compareTo((Double)o.getTime());
			//then compare by operation type - deactivation takes precedence (as defined in the order before.
			if (compare==0){
				compare = operation.compareTo(o.getOperation());				
			}
			return compare;
		}	
		
	}
}
