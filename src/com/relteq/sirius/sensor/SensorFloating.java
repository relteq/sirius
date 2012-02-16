/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.sensor;

import com.relteq.sirius.jaxb.Sensor;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._Sensor;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class SensorFloating extends _Sensor {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public SensorFloating(_Scenario myScenario,String networkId,String linkId) {
		super.populateFromParameters(myScenario,_Sensor.Type.moving_point, networkId, linkId);
	}
	
	public SensorFloating(_Scenario myScenario,Sensor s) {
		super.populateFromJaxb(myScenario,s, _Sensor.Type.moving_point);
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
