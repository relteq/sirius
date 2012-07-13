/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class SplitRatioProfileSet extends com.relteq.sirius.jaxb.SplitRatioProfileSet {

	protected Scenario myScenario;
	protected Integer [] vehicletypeindex; 	// index of vehicle types into global list

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario) {

		this.myScenario = myScenario;
		
		if(getSplitratioProfile().isEmpty())
			return;

		vehicletypeindex = myScenario.getVehicleTypeIndices(getVehicleTypeOrder());
		
		for(com.relteq.sirius.jaxb.SplitratioProfile sr : getSplitratioProfile())
			((SplitRatioProfile) sr).populate(myScenario);
	}

	protected boolean validate() {

		// check that all vehicle types are accounted for
		if(vehicletypeindex.length!=myScenario.getNumVehicleTypes()){
			SiriusErrorLog.addError("Vehicle types list in demand profile id=" +getId()+ " does not match that of settings.");
			return false;
		}
		
		for(com.relteq.sirius.jaxb.SplitratioProfile sr : getSplitratioProfile())
			if(!((SplitRatioProfile)sr).validate())
				return false;
		
		return true;
	}

	protected void update() {
    	for(com.relteq.sirius.jaxb.SplitratioProfile sr : getSplitratioProfile())
    		((SplitRatioProfile) sr).update();	
	}
	
}