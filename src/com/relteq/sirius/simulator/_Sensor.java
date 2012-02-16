/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.Sensor;

public abstract class _Sensor implements InterfaceSensor {

	public static enum Type	{NULL, static_point,
						   	   	       static_area,
						   	   	       moving_point };
	   	   	   		
	protected static enum DataSourceType {NULL, PeMSDataClearinghouse,
										        CaltransDBX,
										        BHL };
   											   
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

	public final void populateFromParameters(_Sensor.Type myType,String networkId,String linkId){
		this.myType = myType;
		//id = API.generateSensorId();
		myLink = API.getLinkWithCompositeId(networkId,linkId);
	}
	
	public final void populateFromJaxb(Sensor s,_Sensor.Type myType){
		this.myType = myType;
		this.id = s.getId();
		if(s.getLinkReference()!=null)
			myLink = API.getLinkWithCompositeId(s.getLinkReference().getNetworkId(),s.getLinkReference().getId());
	}
	
	
}
