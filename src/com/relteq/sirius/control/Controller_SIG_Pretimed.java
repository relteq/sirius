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
public class Controller_SIG_Pretimed extends _Controller {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_SIG_Pretimed(_Scenario myScenario,Controller c) {
		super.populateFromJaxb(myScenario,c, _Controller.Type.SIG_pretimed);
	}
	

	public Controller_SIG_Pretimed(_Scenario myScenario) {
		// TODO Auto-generated constructor stub
	}

}
