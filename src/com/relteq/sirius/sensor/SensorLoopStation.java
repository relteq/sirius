
package com.relteq.sirius.sensor;

import java.util.ArrayList;

import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusMath;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.Sensor;

public class SensorLoopStation extends com.relteq.sirius.simulator.Sensor {
	
	private int VDS;								// PeMS vehicle detector station number
	private ArrayList<com.relteq.sirius.sensor.DataSource> _datasources = new ArrayList<com.relteq.sirius.sensor.DataSource>();
	
	// nominal values
	private static float nom_vf = 65;				// [mph]
	private static float nom_w = 15;				// [mph]
	private static float nom_q_max = 2000;			// [veh/hr/lane]

	private float vf;
	private float w;
	private float q_max;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public  SensorLoopStation(){
		vf = SensorLoopStation.nom_vf;
		w  = SensorLoopStation.nom_w;
		q_max = SensorLoopStation.nom_q_max;
	}

	public SensorLoopStation(Scenario myScenario,String linkId){
		if(myScenario==null)
			return;
		this.myScenario  = myScenario;
	    this.myType = Sensor.Type.static_point;
	    this.myLink = myScenario.getLinkWithId(linkId);
	}
	
	/////////////////////////////////////////////////////////////////////
	// InterfaceSensor
	/////////////////////////////////////////////////////////////////////	

	@Override
	public void populate(Object jaxbobject) {
		
		com.relteq.sirius.jaxb.Sensor jaxbs = (com.relteq.sirius.jaxb.Sensor) jaxbobject;

		for(com.relteq.sirius.jaxb.Parameter param : jaxbs.getParameters().getParameter()){
			if(param.getName().compareToIgnoreCase("vds")==0)
				this.VDS = Integer.parseInt(param.getValue());
		}
		
		if(jaxbs.getDataSources()!=null){
			for(com.relteq.sirius.jaxb.DataSource datasource : jaxbs.getDataSources().getDataSource()){
				try {
					this._datasources.add(new DataSource(datasource.getUrl(),datasource.getFormat()));
				} catch (Exception e) {
					continue;
				}
			}
		}
		
	}
	
	@Override
	public void validate() {
		if(myLink==null)
			SiriusErrorLog.addError("Unknown link reference for sensor id=" + getId() +".");
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


	/////////////////////////////////////////////////////////////////////
	// SensorLoopStation API
	/////////////////////////////////////////////////////////////////////

	public int getVDS() {
		return VDS;
	}

	
	public ArrayList<com.relteq.sirius.sensor.DataSource> get_datasources() {
		return _datasources;
	}

	public void setFD(float vf,float w,float q_max){
		if(!Float.isNaN(vf))
			this.vf = vf;
		if(!Float.isNaN(w))
			this.w = w;
		if(!Float.isNaN(q_max))
			this.q_max = q_max;
	}

	
	public float getVf() {
		return vf;
	}

	
	public float getW() {
		return w;
	}

	
	public float getQ_max() {
		return q_max;
	}

	public float getRho_crit() {
		return q_max/vf;
	}

	public float getRho_jam() {
		return q_max*(1/vf+1/w);
	}
	
}
