
package com.relteq.sirius.sensor;

import java.util.ArrayList;

import com.relteq.sirius.data.FiveMinuteData;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusMath;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.Sensor;

public class SensorLoopStation extends com.relteq.sirius.simulator.Sensor {
	
	private int VDS;								// PeMS vehicle detector station number
	private ArrayList<com.relteq.sirius.sensor.DataSource> _datasources = new ArrayList<com.relteq.sirius.sensor.DataSource>();
	private FiveMinuteData data;
	
	private Double [] cumulative_inflow;	// [veh] 	numEnsemble
	private Double [] cumulative_outflow;	// [veh] 	numEnsemble
	       
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public  SensorLoopStation(){
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

		if(jaxbs.getParameters()!=null)
			for(com.relteq.sirius.jaxb.Parameter param : jaxbs.getParameters().getParameter()){
				if(param.getName().compareToIgnoreCase("vds")==0)
					this.VDS = Integer.parseInt(param.getValue());
			}
		
		if (null != jaxbs.getTable()) {
			if ("data_sources" == jaxbs.getTable().getName()) {
				com.relteq.sirius.simulator.Table table = (com.relteq.sirius.simulator.Table) jaxbs.getTable();
				java.util.Map<String, Integer> imap = table.getColumnNameToIndexMap();
				if (imap != null && imap.get("url") != null && imap.get("format") != null) {
					int ind_url = imap.get("url").intValue();
					int ind_format = imap.get("format").intValue();
					for (com.relteq.sirius.jaxb.Row row : table.getRowList()) {
						this._datasources.add(new DataSource(row.getColumn().get(ind_url), row.getColumn().get(ind_format)));
					}
				}
			} else com.relteq.sirius.simulator.SiriusErrorLog.addWarning("sensor " + jaxbs.getId() + ": table name: " + jaxbs.getTable().getName());
		}
	}
	
	@Override
	public void validate() {
		if(myLink==null)
			SiriusErrorLog.addWarning("Unknown link reference for sensor id=" + getId() +".");
	}

	@Override
	public void reset() {
		cumulative_inflow = new Double [myScenario.getNumEnsemble()];
		cumulative_outflow = new Double [myScenario.getNumEnsemble()];
		for(int i=0;i<this.myScenario.getNumEnsemble();i++){
			cumulative_inflow[i] = 0d;
			cumulative_outflow[i] = 0d;
		}
		return;
	}

	@Override
	public void update() {		
		if(myLink==null)
			return;
		for(int i=0;i<this.myScenario.getNumEnsemble();i++){
			cumulative_inflow[i] += myLink.getTotalInlowInVeh(i);
			cumulative_outflow[i] += myLink.getTotalOutflowInVeh(i);
		}
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

//	@Override
//	public double getOccupancy(int ensemble) {
//		return myLink.getTotalDensityInVeh(ensemble)/myLink.getLengthInMiles()/this.getRho_jam()*100;
//	}
	
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

	public double getCumulativeInflowInVeh(int ensemble){
		return cumulative_inflow[ensemble];
	}

	public void resetCumulativeInflowInVeh(){
		for(int i=0;i<myScenario.getNumEnsemble();i++)
			cumulative_inflow[i] = 0d;
	}
	
	public double getCumulativeOutflowInVeh(int ensemble){
		return cumulative_outflow[ensemble];
	}

	public void resetCumulativeOutflowInVeh(){
		for(int i=0;i<myScenario.getNumEnsemble();i++)
			cumulative_outflow[i] = 0d;
	}
	
	public int getVDS() {
		return VDS;
	}

	public ArrayList<com.relteq.sirius.sensor.DataSource> get_datasources() {
		return _datasources;
	}
	
	/////////////////////////////////////////////////////////////////////
	// data
	/////////////////////////////////////////////////////////////////////
	
	public void set5minData(FiveMinuteData indata){
		data = indata;
	}
	
	public int getNumDataPoints(){
		return data==null ? 0 : data.getNumDataPoints();
	}
	
	public ArrayList<Long> getDataTime(){
		return data==null ? null : data.getTime();
	}

	/** get aggregate flow vlaue in [veh/hr]
	 * @param time index
	 * @return a float, or <code>NaN</code> if something goes wrong.
	 * */
	public float getDataAggFlwInVPH(int i){
		return data==null ? Float.NaN : data.getAggFlwInVPH(i);
	}

	/** get aggregate flow vlaue in [veh/hr/lane]
	 * @param time index
	 * @return a float, or <code>NaN</code> if something goes wrong.
	 * */
	public float getDataAggFlwInVPHPL(int i){
		return data==null ? Float.NaN : data.getAggFlwInVPHPL(i);
	}
	
	/** get aggregate speed value in [mph]
	 * @param time index
	 * @return a float, or <code>NaN</code> if something goes wrong.
	 * */
	public float getDataAggSpdInMPH(int i){
		return data==null ? Float.NaN : data.getAggSpd(i);

	}	

	/** get aggregate density value in [veh/mile]
	 * @param time index
	 * @return a float, or <code>NaN</code> if something goes wrong.
	 * */
	public float getDataAggDtyInVPM(int i){
		return data==null ? Float.NaN : data.getAggDtyInVPM(i);
	}

	/** get aggregate density value in [veh/mile]
	 * @param time index
	 * @return a float, or <code>NaN</code> if something goes wrong.
	 * */
	public float getDataAggDtyInVPMPL(int i){
		return data==null ? Float.NaN : data.getAggDtyInVPMPL(i);
	}

	//////////////////////////////
	
}
