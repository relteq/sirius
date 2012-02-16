/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

import com.relteq.sirius.jaxb.Controller;

final class _ControllerSet extends com.relteq.sirius.jaxb.ControllerSet {

	protected _Scenario myScenario;
	protected ArrayList<_Controller> _controllers = new ArrayList<_Controller>();
	
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	protected ArrayList<_Controller> get_Controllers(){
		return _controllers;
	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(_Scenario myScenario) {

		this.myScenario = myScenario;
		
		if(myScenario.getControllerSet()!=null){
			for(Controller controller : myScenario.getControllerSet().getController()){
	
				// assign type
				_Controller.Type myType;
		    	try {
					myType = _Controller.Type.valueOf(controller.getType());
				} catch (IllegalArgumentException e) {
					myType = _Controller.Type.NULL;
					return;
				}	
				
				// generate controller
				if(myType!=_Controller.Type.NULL){
					_Controller C = ObjectFactory.createControllerFromJaxb(myScenario,controller,myType);
					if(C!=null)
						_controllers.add(C);
				}
			}
		}
	}

	protected boolean validate() {
		for(_Controller controller : _controllers)
			if(!controller.validate())
				return false;
		return true;
	}

	protected void reset() {
		for(_Controller controller : _controllers)
			controller.reset();
	}

	protected void update() {
    	for(_Controller controller : _controllers){
    		if(controller.ison)
    			controller.update();
    	}
	}
	
}
