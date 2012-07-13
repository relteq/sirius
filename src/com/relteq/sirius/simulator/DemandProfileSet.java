/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class DemandProfileSet extends com.relteq.sirius.jaxb.DemandProfileSet {

	protected Scenario myScenario;
	protected Integer [] vehicletypeindex; 	// index of vehicle types into global list

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario) {
		
		this.myScenario = myScenario;;

		if(getDemandProfile().isEmpty())
			return;

		vehicletypeindex = myScenario.getVehicleTypeIndices(getVehicleTypeOrder());
		
		for(com.relteq.sirius.jaxb.DemandProfile dp : getDemandProfile())
			((DemandProfile) dp).populate(myScenario);
	}

	protected void reset() {
		for(com.relteq.sirius.jaxb.DemandProfile dp : getDemandProfile())
			((DemandProfile) dp).reset();
	}
	
	protected void validate() {

		// check that all vehicle types are accounted for
		if(vehicletypeindex.length!=myScenario.getNumVehicleTypes())
			SiriusErrorLog.addError("List of vehicle types in demand profile id=" + this.getId() + " does not match that of settings.");
		
		for(com.relteq.sirius.jaxb.DemandProfile dp : getDemandProfile())
			((DemandProfile)dp).validate();		
	}

	protected void update() {
    	for(com.relteq.sirius.jaxb.DemandProfile dp : getDemandProfile())
    		((DemandProfile) dp).update(false);	
	}
	
}
