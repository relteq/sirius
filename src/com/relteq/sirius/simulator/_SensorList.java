/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

final class _SensorList extends com.relteq.sirius.jaxb.SensorList {

	private ArrayList<_Sensor> _sensors = new ArrayList<_Sensor>();

	/////////////////////////////////////////////////////////////////////
	// interface
	/////////////////////////////////////////////////////////////////////
	
	public ArrayList<_Sensor> get_Sensors(){
		return _sensors;
	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(_Network myNetwork) {
		if(myNetwork.getSensorList()!=null){
			for(com.relteq.sirius.jaxb.Sensor sensor : myNetwork.getSensorList().getSensor()){
	
				// assign type
				_Sensor.Type myType;
		    	try {
					myType = _Sensor.Type.valueOf(sensor.getType());
				} catch (IllegalArgumentException e) {
					myType = _Sensor.Type.NULL;
					return;
				}	
				// generate controller
				_Sensor S = null;
				switch(myType){
					case static_point:
						S = new com.relteq.sirius.sensor.SensorLoopStation(sensor,myType);
					break;
				}
				if(myType!=_Sensor.Type.NULL)
					_sensors.add(S);
			}
		}
	}

	protected boolean validate() {
		for(_Sensor sensor : _sensors)
			if(!sensor.validate())
				return false;
		return true;
	}

	protected void reset() {
		for(_Sensor sensor : _sensors)
			sensor.reset();
	}

	protected void update() {
    	for(_Sensor sensor : _sensors)
    		sensor.update();
	}
	
}
