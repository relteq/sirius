/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.Sensor;

/** Simple implementation of {@link InterfaceSensor}.
 * 
 * <p> This is the base class for all sensors contained in a scenario. 
 * It provides a full default implementation of <code>InterfaceSensor</code>
 * so that extended classes need only implement a portion of the interface.
 *
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public abstract class _Sensor implements InterfaceComponent,InterfaceSensor {
   			
	/** The scenario that contains this sensor. */
	protected _Scenario myScenario;	

	/** Unique identifier.  */
	protected String id;

	/** Sensor type. */
	protected _Sensor.Type myType;
	
	/** Current link where the sensor is located. */
	protected _Link myLink = null;


	public static enum Type	{  
		/** fixed point detector station, such as a loop detector station.*/	static_point,
		/** fixed area detector, such as a camera or radar detector.	  */	static_area,
		/** moving detector, such as a probe vehicle or cell phone.		  */	moving_point };
	   	   	   		
//	protected static enum DataSourceType {NULL, PeMSDataClearinghouse,
//										        CaltransDBX,
//										        BHL };
				   	   	       
	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected _Sensor(){}		  

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
	
	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////

	/** The scenario that contains this sensor.
	 * @return id String
	 * */
	public _Scenario getMyScenario() {
		return myScenario;
	}
	
	/** Unique identifier. 
	 * @return id String
	 * */
	public String getId() {
		return id;
	}

	/** Sensor type. 
	 * @return type _Sensor.Type
	 * */
	public _Sensor.Type getMyType() {
		return myType;
	}

	/** Current link where the sensor is located. 
	 * <p> This value may change in time if the sensor is mobile.
	 * @return link  _Link
	 * */
	public _Link getMyLink() {
		return myLink;
	}

	/////////////////////////////////////////////////////////////////////
	// populate
	/////////////////////////////////////////////////////////////////////

//	protected final void populateFromParameters(_Scenario myScenario,_Sensor.Type myType,String networkId,String linkId){
//		this.myScenario = myScenario;
//		this.myType = myType;
//		//id = API.generateSensorId();
//		myLink = myScenario.getLinkWithCompositeId(networkId,linkId);
//	}
	
	/** @y.exclude */
	protected final void populateFromJaxb(_Scenario myScenario,Sensor s,_Sensor.Type myType){
		this.myScenario = myScenario;
		this.myType = myType;
		this.id = s.getId();
		if(s.getLinkReference()!=null)
			myLink = myScenario.getLinkWithCompositeId(s.getLinkReference().getNetworkId(),s.getLinkReference().getId());
	}
	
}
