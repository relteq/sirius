/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

/** Container class for components used as targets or feedback in controllers and events. 
 * 
 * <p>This class provides a container for links, nodes, controllers, sensors, events, and signals
 * that appear in the target or feedback list of controllers and events.
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public final class ScenarioElement extends com.relteq.sirius.jaxb.ScenarioElement {
	
	/** @y.exclude */	protected Scenario myScenario;
	/** @y.exclude */	protected ScenarioElement.Type myType;
	/** @y.exclude */	protected Object reference;

	/** Type of scenario element. */
	public static enum Type {  
	/** see {@link _Link} 		*/ link, 
	/** see {@link _Node} 		*/ node, 
	/** see {@link _Controller} */ controller,
	/** see {@link _Sensor} 	*/ sensor,
	/** see {@link _Event} 		*/ event,
	/** see {@link _Signal} 	*/ signal };
							   
	/////////////////////////////////////////////////////////////////////
	// protected constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected ScenarioElement(){}
	
	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////

	/** The scenario that contains the referenced component.
	 *  @return The scenario.
	 */
	public Scenario getMyScenario() {
		return myScenario;
	}

	/** The type of the referenced component.
	 * <p> i.e. link, node, controller, sensor, event, or signal.
	 * @return Component type. 
	 */
	public ScenarioElement.Type getMyType() {
		return myType;
	}

	/** The string id of the network that contains the component.
	 * <p> Returns the id of the parent network if the component is a link, node, sensor,
	 * or signal. 
	 * Otherwise it returns <code>null</code>.
	 * @return Network id, or <code>null</code>.
	 */
	@Override
	public String getNetworkId() {
		if(myType==null && type!=null)
			myType = ScenarioElement.Type.valueOf(type);
		if(myType.compareTo(ScenarioElement.Type.link)==0 || 
		   myType.compareTo(ScenarioElement.Type.node)==0 || 
		   myType.compareTo(ScenarioElement.Type.sensor)==0 || 
		   myType.compareTo(ScenarioElement.Type.signal)==0 ){
			return super.getNetworkId();
		}
		else
			return null;
	}

	/** Reference to the component.
	 * @return A java Object.
	 */
	public Object getReference() {
		return reference;
	}
	
}
