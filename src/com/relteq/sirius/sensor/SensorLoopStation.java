
package com.relteq.sirius.sensor;

import com.relteq.sirius.jaxb.Sensor;
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

	public  SensorLoopStation(){
	}

	public SensorLoopStation(_Scenario myScenario,String networkId,String linkId){
		if(myScenario==null)
			return;
		this.myScenario  = myScenario;
		// this.id = GENERATE AN ID;
	    this.myType = _Sensor.Type.static_point;
	    this.myLink = myScenario.getLinkWithCompositeId(networkId,linkId);
	}
	
	/////////////////////////////////////////////////////////////////////
	// InterfaceSensor
	/////////////////////////////////////////////////////////////////////	

	@Override
	public void populate(Sensor s) {
	}
	
	@Override
	public Double[] getDensityInVeh() {
		return myLink.getDensityInVeh();
	}
	
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

	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////	

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
