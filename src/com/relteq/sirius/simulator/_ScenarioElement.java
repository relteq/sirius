/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

public final class _ScenarioElement {
	
	public _ScenarioElement() {
	}
	
	public static enum Type {NULL, link, 
									  node,
									  controller,
									  sensor,
									  event,
									  signal };
		    
	protected _Scenario myScenario;
	protected _ScenarioElement.Type myType;
	protected String network_id = "";
	protected String id;
	protected Object reference;
	public _Scenario getMyScenario() {
		return myScenario;
	}
	public _ScenarioElement.Type getMyType() {
		return myType;
	}
	public String getNetwork_id() {
		return network_id;
	}
	public String getId() {
		return id;
	}
	public Object getReference() {
		return reference;
	}
	
}
