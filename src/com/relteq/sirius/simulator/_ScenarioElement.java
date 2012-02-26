/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.ScenarioElement;

public final class _ScenarioElement extends ScenarioElement {
	
	/** @y.exclude */	protected _Scenario myScenario;
	/** @y.exclude */	protected _ScenarioElement.Type myType;
	/** @y.exclude */	protected String network_id = "";
	/** @y.exclude */	protected String id;
	/** @y.exclude */	protected Object reference;

	public static enum Type {  link, 
							   node, 
							   controller,
							   sensor,
							   event,
							   signal };
							   
	/////////////////////////////////////////////////////////////////////
	// protected constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected _ScenarioElement(){}
	
	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////

	/** DESCRIPTION
	 * 
	 */
	public _Scenario getMyScenario() {
		return myScenario;
	}

	/** DESCRIPTION
	 * 
	 */
	public _ScenarioElement.Type getMyType() {
		return myType;
	}

	/** DESCRIPTION
	 * 
	 */
	public String getNetwork_id() {
		return network_id;
	}

	/** DESCRIPTION
	 * 
	 */
	public String getId() {
		return id;
	}

	/** DESCRIPTION
	 * 
	 */
	public Object getReference() {
		return reference;
	}
	
}
