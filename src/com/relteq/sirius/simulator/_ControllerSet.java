/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

import com.relteq.sirius.jaxb.Controller;

public class _ControllerSet extends com.relteq.sirius.jaxb.ControllerSet {

	private ArrayList<_Controller> _controllers = new ArrayList<_Controller>();
	
	/////////////////////////////////////////////////////////////////////
	// interface
	/////////////////////////////////////////////////////////////////////
	
	public ArrayList<_Controller> get_Controllers(){
		return _controllers;
	}
	
	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void initialize() {

		if(Utils.theScenario.getControllerSet()!=null){
			for(Controller controller : Utils.theScenario.getControllerSet().getController()){
	
				// assign type
				Types.Controller myType;
		    	try {
					myType = Types.Controller.valueOf(controller.getType());
				} catch (IllegalArgumentException e) {
					myType = Types.Controller.NULL;
					return;
				}	
				// generate controller
				_Controller C = null;
				switch(myType){
					case IRM_alinea:
						C = new com.relteq.sirius.control.ControllerAlinea(controller,myType);
					break;
				}
				if(myType!=Types.Controller.NULL)
					_controllers.add(C);
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
