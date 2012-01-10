package simulator;

public interface AuroraComponent {
	
	// initialize properties of subclasses of jaxb classes
	public void initialize();

	// consistency checks
	public boolean validate();
	
	// preparation for running
	public void reset();
	
	// run the component.
	public void update();
	
}
