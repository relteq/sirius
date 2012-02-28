
package com.relteq.sirius.sensor;

import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._Sensor;

public class SensorFloating extends _Sensor {

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public SensorFloating(){
	}
	
	public SensorFloating(_Scenario myScenario,String networkId,String linkId){
		if(myScenario==null)
			return;
		this.myScenario  = myScenario;
		// this.id = GENERATE AN ID;
	    this.myType = _Sensor.Type.moving_point;
	    this.myLink = myScenario.getLinkWithCompositeId(networkId,linkId);
	}
		
	/////////////////////////////////////////////////////////////////////
	// InterfaceSensor
	/////////////////////////////////////////////////////////////////////	

	@Override
	public void populate(Object jaxbobject) {
	}
	
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

}
