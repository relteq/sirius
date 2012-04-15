package com.relteq.sirius.control;

import com.relteq.sirius.simulator.Controller;
import com.relteq.sirius.simulator.Scenario;

public class Controller_VSL_Time_of_Day extends Controller {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
		
	public Controller_VSL_Time_of_Day() {
		// TODO Auto-generated constructor stub
	}
	
	public Controller_VSL_Time_of_Day(Scenario myScenario) {
		this.myScenario = myScenario;
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceController
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public void populate(Object jaxbobject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean register() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

}
