
package com.relteq.sirius.sensor;

import com.relteq.sirius.simulator.SiriusMath;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.Sensor;

public class SensorLoopStation extends Sensor {
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public  SensorLoopStation(){
	}

	public SensorLoopStation(Scenario myScenario,String networkId,String linkId){
		if(myScenario==null)
			return;
		this.myScenario  = myScenario;
		// this.id = GENERATE AN ID;
	    this.myType = Sensor.Type.static_point;
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
	public Double[] getDensityInVPM() {
		return SiriusMath.times(myLink.getDensityInVeh(),1/myLink.getLengthInMiles());
	}
	
	@Override
	public double getTotalDensityInVPM() {
		return myLink.getTotalDensityInVeh()/myLink.getLengthInMiles();
	}

	@Override
	public Double[] getFlowInVPH() {
		return SiriusMath.times(myLink.getOutflowInVeh(),1/myScenario.getSimDtInHours());
	}

	@Override
	public double getTotalFlowInVPH() {
		return myLink.getTotalOutflowInVeh()/myScenario.getSimDtInHours();
	}

	@Override
	public double getSpeedInMPH() {
		return myLink.computeSpeedInMPH();
	}
		
}
