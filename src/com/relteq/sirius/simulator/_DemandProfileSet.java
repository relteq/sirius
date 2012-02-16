/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.DemandProfile;
import com.relteq.sirius.jaxb.DemandProfileSet;

final class _DemandProfileSet extends DemandProfileSet {

	protected _Scenario myScenario;
	protected Integer [] vehicletypeindex; 	// index of vehicle types into global list

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(_Scenario myScenario) {
		
		this.myScenario = myScenario;;

		int i;
		if(getDemandProfile().isEmpty())
			return;

		// use <VehicleTypesOrder> if it is there, otherwise assume order given in <settings>
		int numTypes;
		if(getVehicleTypeOrder()!=null){
			numTypes = getVehicleTypeOrder().getVehicleType().size();
			vehicletypeindex = new Integer[numTypes];
			for(i=0;i<numTypes;i++)
				vehicletypeindex[i] = myScenario.getVehicleTypeIndex(getVehicleTypeOrder().getVehicleType().get(i).getName());
		}
		else{
			numTypes = myScenario.getNumVehicleTypes();
			vehicletypeindex = new Integer[numTypes];
			for(i=0;i<numTypes;i++)
				vehicletypeindex[i] = i;
		}
		
		for(DemandProfile dp : getDemandProfile())
			((_DemandProfile) dp).populate(myScenario);
	}

	protected void reset() {
		for(DemandProfile dp : getDemandProfile())
			((_DemandProfile) dp).reset();
	}
	
	protected boolean validate() {

		// check that all vehicle types are accounted for
		if(vehicletypeindex.length!=myScenario.getNumVehicleTypes()){
			System.out.println("Demand profile list of vehicle types does not match that of settings.");
			return false;
		}
		
		if(getDemandProfile().isEmpty())
			return true;
		
		for(DemandProfile dp : getDemandProfile())
			if(!((_DemandProfile)dp).validate())
				return false;
		
		return true;
	}

	protected void update() {
    	for(DemandProfile dp : getDemandProfile())
    		((_DemandProfile) dp).update();	
	}
	
}
