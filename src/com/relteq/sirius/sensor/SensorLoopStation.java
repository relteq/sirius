
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
	public Double[] getDensityInVPM(int ensemble) {
		return SiriusMath.times(myLink.getDensityInVeh(ensemble),1/myLink.getLengthInMiles());
	}

	@Override
	public double getTotalDensityInVeh(int ensemble) {
		return myLink.getTotalDensityInVeh(ensemble);
	}
	
	@Override
	public double getTotalDensityInVPM(int ensemble) {
		return myLink.getTotalDensityInVeh(ensemble)/myLink.getLengthInMiles();
	}

	@Override
	public Double[] getFlowInVPH(int ensemble) {
		return SiriusMath.times(myLink.getOutflowInVeh(ensemble),1/myScenario.getSimDtInHours());
	}

	@Override
	public double getTotalFlowInVPH(int ensemble) {
		return myLink.getTotalOutflowInVeh(ensemble)/myScenario.getSimDtInHours();
	}

	@Override
	public double getSpeedInMPH(int ensemble) {
		return myLink.computeSpeedInMPH(ensemble);
	}
		
}
