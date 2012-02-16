/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.sensor;

import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._Sensor;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class SensorLoopStation extends _Sensor {
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public SensorLoopStation(_Scenario myScenario,String networkId,String linkId) {
		super.populateFromParameters(myScenario,_Sensor.Type.static_point,networkId, linkId);
	}
	
	public SensorLoopStation(_Scenario myScenario,com.relteq.sirius.jaxb.Sensor s) {
		super.populateFromJaxb(myScenario,s, _Sensor.Type.static_point);	
	}
	
	/////////////////////////////////////////////////////////////////////
	// InterfaceSensor
	/////////////////////////////////////////////////////////////////////	

	@Override
	public boolean validate() {
		if(myLink==null)
			return false;
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
