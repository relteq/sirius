/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.sensor;

import com.relteq.sirius.simulator.Types;
import com.relteq.sirius.simulator._Link;
import com.relteq.sirius.simulator._Sensor;

public class SensorLoopStation extends _Sensor {

	_Link myLink;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public SensorLoopStation(com.relteq.sirius.jaxb.Sensor c,Types.Sensor myType) {
		super(c,myType);
		
		if(myLinks.size()!=1){
			myLink = myLinks.get(0);
		}
		
	}
	
	/////////////////////////////////////////////////////////////////////
	// InterfaceSensor
	/////////////////////////////////////////////////////////////////////	

	@Override
	public boolean validate() {
		if(myLinks.size()!=1){
			System.out.println("Loop sensor must reside on a single link");
			return false;
		}
		return true;
	}

	@Override
	public void reset() {
		return;
	}

	@Override
	public void update() {
		return;
	}

	@Override
	public Double[] getDensityInVeh() {
		return myLink.getDensityInVeh();
	}

	@Override
	public double getTotalDensityInVeh() {
		return myLink.getTotalDensityInVeh();
	}

	@Override
	public Double[] getFlowInVeh() {
		return myLink.getOutflowInVeh();
	}

	@Override
	public double getTotalFlowInVeh() {
		return myLink.getTotalOutflowInVeh();
	}

	@Override
	public double getNormalizedSpeed() {
		return myLink.computeSpeedInMPH();
	}
		
}
