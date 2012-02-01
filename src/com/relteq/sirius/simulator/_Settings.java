/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.math.BigDecimal;
import java.util.ArrayList;

import com.relteq.sirius.jaxb.ObjectFactory;
import com.relteq.sirius.jaxb.VehicleType;

public class _Settings extends com.relteq.sirius.jaxb.Settings {
	
	protected ArrayList<VehicleType> vehicleTypes = new ArrayList<VehicleType>();
	
	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	protected void initialize() {
		
		// vehicle types
		if(getVehicleTypes()!=null)
			for(VehicleType vtype : getVehicleTypes().getVehicleType())
				vehicleTypes.add(vtype);
		else{
			ObjectFactory O = new ObjectFactory();
			VehicleType vtype = O.createVehicleType();
			vtype.setName("Standard vehicle");
			vtype.setWeight(new BigDecimal(1));
			vehicleTypes.add(vtype);
		}

	}

	protected boolean validate() {
		
		for(VehicleType v : vehicleTypes){
			if(v.getWeight().doubleValue()<=0.0)
				return false;
		}
		
		return true;
	}

}
