/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.sensor;

import com.relteq.sirius.jaxb.Sensor;
import com.relteq.sirius.simulator._Sensor;

public class SensorFloating extends _Sensor {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public SensorFloating(Sensor s, com.relteq.sirius.simulator.Types.Sensor myType) {
		super(s, myType);
		// TODO Auto-generated constructor stub
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceSensor
	/////////////////////////////////////////////////////////////////////	
	
	@Override
	public boolean validate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Double[] getDensityInVeh() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getTotalDensityInVeh() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Double[] getFlowInVeh() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getTotalFlowInVeh() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getNormalizedSpeed() {
		// TODO Auto-generated method stub
		return 0;
	}

}
