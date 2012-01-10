package aurora.simulator;

interface AuroraCompdonent {

	// initialize properties of subclasses of jaxb classes
	void initialize();

	// consistency checks
	boolean validate();

	// preparation for running
	void reset();

	// run the component.
	void update();

}
