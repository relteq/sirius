package com.relteq.sirius.control;

import com.relteq.sirius.jaxb.Controller;
import com.relteq.sirius.simulator._Controller;
import com.relteq.sirius.simulator._Scenario;

/** DESCRIPTION OF THE CLASS
* <p>
* LACKS IMPLEMENTATION
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class Controller_IRM_Time_of_Day extends _Controller {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_IRM_Time_of_Day(_Scenario myScenario,Controller c) {
		super.populateFromJaxb(myScenario,c, _Controller.Type.IRM_time_of_day);
	}
	

	public Controller_IRM_Time_of_Day(_Scenario myScenario) {
		// TODO Auto-generated constructor stub
	}

}