/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.Sensor;

/** DESCRIPTION OF THE CLASS
*
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public abstract class _Sensor implements InterfaceSensor {

	public static enum Type	{NULL, static_point,
						   	   	       static_area,
						   	   	       moving_point };
	   	   	   		
	protected static enum DataSourceType {NULL, PeMSDataClearinghouse,
										        CaltransDBX,
										        BHL };
   			
	protected _Scenario myScenario;							       
	protected String id;
	protected _Sensor.Type myType;
	protected _Link myLink = null;

	/////////////////////////////////////////////////////////////////////
	// interface
	/////////////////////////////////////////////////////////////////////

	public String getId() {
		return id;
	}

	public _Sensor.Type getMyType() {
		return myType;
	}

	public _Link getMyLink() {
		return myLink;
	}

	/////////////////////////////////////////////////////////////////////
	// populate
	/////////////////////////////////////////////////////////////////////

	public final void populateFromParameters(_Scenario myScenario,_Sensor.Type myType,String networkId,String linkId){
		this.myScenario = myScenario;
		this.myType = myType;
		//id = API.generateSensorId();
		myLink = myScenario.getLinkWithCompositeId(networkId,linkId);
	}
	
	public final void populateFromJaxb(_Scenario myScenario,Sensor s,_Sensor.Type myType){
		this.myScenario = myScenario;
		this.myType = myType;
		this.id = s.getId();
		if(s.getLinkReference()!=null)
			myLink = myScenario.getLinkWithCompositeId(s.getLinkReference().getNetworkId(),s.getLinkReference().getId());
	}
	
	
}
