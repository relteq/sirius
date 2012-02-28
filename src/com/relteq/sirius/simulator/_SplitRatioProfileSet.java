/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.SplitRatioProfileSet;
import com.relteq.sirius.jaxb.SplitratioProfile;

final class _SplitRatioProfileSet extends SplitRatioProfileSet {

	protected _Scenario myScenario;
	protected Integer [] vehicletypeindex; 	// index of vehicle types into global list

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(_Scenario myScenario) {

		this.myScenario = myScenario;
		
		if(getSplitratioProfile().isEmpty())
			return;

		vehicletypeindex = myScenario.getVehicleTypeIndices(getVehicleTypeOrder());
		
		for(SplitratioProfile sr : getSplitratioProfile())
			((_SplitRatioProfile) sr).populate(myScenario);
	}

	protected boolean validate() {

		// check that all vehicle types are accounted for
		if(vehicletypeindex.length!=myScenario.getNumVehicleTypes()){
			SiriusErrorLog.addErrorMessage("Demand profile list of vehicle types does not match that of settings.");
			return false;
		}
		
		if(getSplitratioProfile().isEmpty())
			return true;
		
		for(SplitratioProfile sr : getSplitratioProfile())
			if(!((_SplitRatioProfile)sr).validate())
				return false;
		
		return true;
	}

	protected void update() {
    	for(SplitratioProfile sr : getSplitratioProfile())
    		((_SplitRatioProfile) sr).update();	
	}
	
}