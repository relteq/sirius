package com.relteq.sirius.simulator;

import java.util.ArrayList;
import java.util.HashMap;

import com.relteq.sirius.data.DataFileReader;
import com.relteq.sirius.data.FiveMinuteData;
import com.relteq.sirius.sensor.DataSource;
import com.relteq.sirius.sensor.SensorLoopStation;

public class SensorList extends com.relteq.sirius.jaxb.SensorList {

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario) {
		for(int i=0;i<getSensor().size();i++){
			com.relteq.sirius.jaxb.Sensor sensor = getSensor().get(i);
			Sensor.Type myType = Sensor.Type.valueOf(sensor.getType());
			getSensor().set(i,ObjectFactory.createSensorFromJaxb(myScenario,sensor,myType));
		}
	}

	protected void validate() {
		for (com.relteq.sirius.jaxb.Sensor sensor : getSensor())
			((Sensor) sensor).validate();
	}

	protected void reset() throws SiriusException {
		for (com.relteq.sirius.jaxb.Sensor sensor : getSensor())
			((Sensor) sensor).reset();
	}

	protected void update() throws SiriusException {
		for(com.relteq.sirius.jaxb.Sensor sensor : getSensor())
			((Sensor)sensor).update();
	}

	/////////////////////////////////////////////////////////////////////
	// Load data
	/////////////////////////////////////////////////////////////////////
	
	protected void loadSensorData(Scenario scenario) throws Exception {
	
		HashMap <Integer,FiveMinuteData> data = new HashMap <Integer,FiveMinuteData> ();
		ArrayList<DataSource> datasources = new ArrayList<DataSource>();
		ArrayList<String> uniqueurls  = new ArrayList<String>();
		
		// construct list of stations to extract from datafile 
		for(com.relteq.sirius.jaxb.Sensor sensor : scenario.getSensorList().getSensor()){
			if(((Sensor) sensor).getMyType().compareTo(Sensor.Type.static_point)!=0)
				continue;
			SensorLoopStation S = (SensorLoopStation) sensor;
			int myVDS = S.getVDS();				
			data.put(myVDS, new FiveMinuteData(myVDS,true));	
			for(com.relteq.sirius.sensor.DataSource d : S.get_datasources()){
				String myurl = d.getUrl();
				int indexOf = uniqueurls.indexOf(myurl);
				if( indexOf<0 ){
					DataSource newdatasource = new DataSource(d);
					newdatasource.add_to_for_vds(myVDS);
					datasources.add(newdatasource);
					uniqueurls.add(myurl);
				}
				else{
					datasources.get(indexOf).add_to_for_vds(myVDS);
				}
			}
		}
		
		// Read 5 minute data to "data"
		DataFileReader P = new DataFileReader();
		P.Read5minData(data,datasources);
	}
}
