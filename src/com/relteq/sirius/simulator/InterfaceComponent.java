package com.relteq.sirius.simulator;

/** Common interface for controllers, sensors, and events.
*
* @author Gabriel Gomes (gomes@path.berkeley.edu)
*/
public interface InterfaceComponent {

	/** Populate the component with configuration data. 
	 * 
	 * <p> Called once by {@link ObjectFactory#createAndLoadScenario}.
	 * It is passed a JAXB object with data loaded from the configuration file. 
	 * Use this function to populate and initialize all fields in the
	 * component. 
	 * 
	 * @param jaxbobject Object
	 */
	public void populate(Object jaxbobject);
	
	/** Validate the component.
	 * 
	 * <p> Called once by {@link ObjectFactory#createAndLoadScenario}.
	 * It checks the validity of the configuration parameters.
	 * 
	 * @return <code>true</code> if the data is valid, <code>false</code> otherwise. 
	 */
	public boolean validate();

	/** Prepare the component for simulation.
	 * 
	 * <p> Called by {@link _Scenario#run} each time a new simulation run is started.
	 * It is used to initialize the internal state of the component.
	 * <p> Because events are state-less, the {@link _Event} class provides a default 
	 * implementation of this method, so it need not be implemented by other event classes.
	 */
	public void reset();

	/** Update the state of the component.
	 * 
	 * <p> Called by {@link _Scenario#run} at each simulation time step.
	 * This function updates the internal state of the component.
	 * <p> Because events are state-less, the {@link _Event} class provides a default 
	 * implementation of this method, so it need not be implemented by other event classes.
	 */
	public void update();
	
}
