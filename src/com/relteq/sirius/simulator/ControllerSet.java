/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

final class ControllerSet extends com.relteq.sirius.jaxb.ControllerSet {

	protected Scenario myScenario;
	protected ArrayList<Controller> controllers = new ArrayList<Controller>();
	
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
					if(C!=null)
						controllers.add(C);
				}
			}
		}
	}

	protected boolean validate() {
		for(Controller controller : controllers)
			if(!controller.validate()){
				SiriusErrorLog.addErrorMessage("Controller validation failure, controller " + controller.getId());
				return false;
			}
		return true;
	}

	protected void reset() {
		for(Controller controller : controllers)
			controller.reset();
	}

	protected void update() throws SiriusException {
    	for(Controller controller : controllers)
    		if(controller.ison && myScenario.clock.istimetosample(controller.samplesteps,0))
    			controller.update();
	}
	
}
