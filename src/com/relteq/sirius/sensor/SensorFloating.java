
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
	public void populate(Sensor s) {
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
