/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

final class SensorList extends com.relteq.sirius.jaxb.SensorList {

	protected ArrayList<Sensor> sensors = new ArrayList<Sensor>();
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Network myNetwork) {
		if(myNetwork.getSensorList()!=null){
			for(com.relteq.sirius.jaxb.Sensor sensor : myNetwork.getSensorList().getSensor()){
	
				// assign type
				Sensor.Type myType;
		    	try {
					myType = Sensor.Type.valueOf(sensor.getType());
				} catch (IllegalArgumentException e) {
					SiriusErrorLog.addErrorMessage("Warning: sensor has wrong type. Ignoring.");
					continue;
				}	
				
				// generate sensor
				if(myType!=null){
					Sensor S = ObjectFactory.createSensorFromJaxb(myNetwork.myScenario,sensor,myType);
					if(S!=null)
						sensors.add(S);
				}
			}
		}
	}

	protected boolean validate() {
		for(Sensor sensor : sensors)
			if(!sensor.validate()){
				SiriusErrorLog.addErrorMessage("Sensor validation failure, sensor " + sensor.getId());
				return false;
			}
		return true;
	}

	protected void reset() throws SiriusException {
		for(Sensor sensor : sensors)
			sensor.reset();
	}

	protected void update() throws SiriusException {
    	for(Sensor sensor : sensors)
    		sensor.update();
	}
	
}
