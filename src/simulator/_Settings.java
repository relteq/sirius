package simulator;

import java.math.BigDecimal;
import java.util.ArrayList;

import jaxb.Display;
import jaxb.ObjectFactory;
import jaxb.Settings;
import jaxb.Vtype;

public class _Settings extends Settings implements AuroraComponent {
	
	private float outdt;			// [hr]
	private float timeinit;			// [hr]
	private float timemax;			// [hr]
	private ArrayList<Vtype> vehicleTypes = new ArrayList<Vtype>();

	/////////////////////////////////////////////////////////////////////
	// getters and setters 
	/////////////////////////////////////////////////////////////////////

    public float getTimeMaxInHours() {
    	return timemax;
    }

    public float getOutdt() {
		return outdt;
	}

	public void setOutdt(float outdt) {
		this.outdt = outdt;
	}

	public float getTimeInitialinHours() {
    	return timeinit;
    }

	/////////////////////////////////////////////////////////////////////
	// _AuroraComponent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void initialize() {
		
		// vehicle types
		if(getVehicleTypes()!=null)
			for(Vtype vtype : getVehicleTypes().getVtype())
				vehicleTypes.add(vtype);
		else{
			ObjectFactory O = new ObjectFactory();
			Vtype vtype = O.createVtype();
			vtype.setName("Standard vehicle");
			vtype.setWeight(new BigDecimal(1));
			vehicleTypes.add(vtype);
		}

		// timemax, timeinit, outdt

    	Display display = getDisplay();
    	float dt;
		if( display!=null && display.getTimeout()!=null){
			dt = display.getDt().floatValue();
			if(dt<0.1){
				System.out.println("Warning: Output dt given in hours. Changing to seconds.");
				dt *= 3600f;
			}
			outdt = dt/3600f;
		}
		else
			outdt = Defaults.OUT_DT;
		
		if( display!=null && display.getTimeMax()!=null){
			dt = display.getTimeMax().floatValue();
			if(dt<=24){
				System.out.println("Warning: Simulation time given in hours. Changing to seconds.");
				dt *= 3600f;
			}
			timemax = dt/3600f;
		}
		else
			timemax = Defaults.TIME_MAX;

		if( display!=null && display.getTimeInitial()!=null){
			dt = display.getTimeInitial().floatValue();
			if(dt>0 && dt<=24){
				System.out.println("Warning: Initial time given in hours. Changing to seconds.");
				dt *= 3600f;
			}
			timeinit = dt/3600f;
		}
		else
			timeinit = Defaults.TIME_INIT;


	}

	@Override
	public boolean validate() {
		 
		if(timeinit<0){
			System.out.println("timeinit should be non-negative");
			return false;
		}
		
		if(timemax<0){
			System.out.println("timemax should be non-negative");
			return false;
		}

		if(outdt<0){
			System.out.println("outdt should be non-negative");
			return false;
		}
		
		if(!Utils.isintegermultipleof(outdt,Utils.simdt)){
			System.out.println("outdt must be an interger multiple of simulation dt.");
			return false;
		}
		
		return true;
	}

	@Override
	public void reset() {		
	}
	
	@Override
	public void update() {
	}

}
