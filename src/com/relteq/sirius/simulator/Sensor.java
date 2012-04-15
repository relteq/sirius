/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

/** Base implementation of {@link InterfaceSensor}.
 * 
 * <p> This is the base class for all sensors contained in a scenario. 
 * It provides a full default implementation of <code>InterfaceSensor</code>
 * so that extended classes need only implement a portion of the interface.
 *
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public class Sensor extends com.relteq.sirius.jaxb.Sensor implements InterfaceComponent,InterfaceSensor {
   			
	/** The scenario that contains this sensor. */
	protected Scenario myScenario;	

	/** Unique identifier.  */
	//protected String id;

	/** Sensor type. */
	protected Sensor.Type myType;
	
	/** Current link where the sensor is located. */
	protected Link myLink = null;

	/** Type of sensor. */
	public static enum Type	{  
	/** see {@link ObjectFactory#createSensor_LoopStation} 	*/	static_point,
	                                                            static_area,
	/** see {@link ObjectFactory#createSensor_Floating} 	*/  moving_point };
	   	   
//	protected static enum DataSourceType {NULL, PeMSDataClearinghouse,
//										        CaltransDBX,
//										        BHL };
				   	   	       
	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected Sensor(){
	}		  

	/////////////////////////////////////////////////////////////////////
	// InterfaceSensor
	/////////////////////////////////////////////////////////////////////

	/** Default implementation of {@link InterfaceSensor#getDensityInVPM()} 
	 * @return <code>null</code>
	 * */
	@Override
	public Double[] getDensityInVPM() {
		return null;
	}

	/** Default implementation of {@link InterfaceSensor#getTotalDensityInVPM()} 
	 * @return <code>Double.NaN</code>
	 * */
	@Override
	public double getTotalDensityInVPM() {
		return Double.NaN;
	}

	/** Default implementation of {@link InterfaceSensor#getFlowInVPH()} 
	 * @return <code>null</code>
	 * */
	@Override
	public Double[] getFlowInVPH() {
		return null;
	}

	/** Default implementation of {@link InterfaceSensor#getTotalFlowInVPH()} 
	 * @return <code>Double.NaN</code>
	 * */
	@Override
	public double getTotalFlowInVPH() {
		return Double.NaN;
	}

	/** Default implementation of {@link InterfaceSensor#getSpeedInMPH()} 
	 * @return <code>Double.NaN</code>
	 * */
	public double getSpeedInMPH() {
		return Double.NaN;
	}
	
	/** Default implementation of {@link InterfaceSensor#getTotalDensityInVeh()} 
	 * @return <code>Double.NaN</code>
	 * */
	@Override
	public double getTotalDensityInVeh() {
		return Double.NaN;
	}
	
	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////

	/** The scenario that contains this sensor.
	 * @return id String
	 * */
	public Scenario getMyScenario() {
		return myScenario;
	}
	
	/** Unique identifier. 
	 * @return id String
	 * */
//	public String getId() {
//		return id;
//	}

	/** Sensor type. 
	 * @return type _Sensor.Type
	 * */
	public Sensor.Type getMyType() {
		return myType;
	}

	/** Current link where the sensor is located. 
	 * <p> This value may change in time if the sensor is mobile.
	 * @return link  _Link
	 * */
	public Link getMyLink() {
		return myLink;
	}

	/** Load sensor data from its data source.
	 */
	public void loadData() throws SiriusException{

	}
	
	/////////////////////////////////////////////////////////////////////
	// populate
	/////////////////////////////////////////////////////////////////////
	
	/** @y.exclude */
	protected final void populateFromJaxb(Scenario myScenario,com.relteq.sirius.jaxb.Sensor s,Sensor.Type myType){
		this.myScenario = myScenario;
		this.myType = myType;
		this.id = s.getId();
		if(s.getLinkReference()!=null)
			myLink = myScenario.getLinkWithCompositeId(s.getLinkReference().getNetworkId(),s.getLinkReference().getId());
	}

	@Override
	public void populate(Object jaxbobject) {
		return;
	}

	@Override
	public boolean validate() {
		return true;
	}

	@Override
	public void reset() throws SiriusException {
		return;
	}

	@Override
	public void update() throws SiriusException {
		return;
	}
	
}
