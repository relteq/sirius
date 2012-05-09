
package com.relteq.sirius.control;

import com.relteq.sirius.simulator.Controller;
import com.relteq.sirius.simulator.Link;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.Sensor;

public class Controller_IRM_Alinea extends Controller {

	private Link onramplink = null;
	private Link mainlinelink = null;
	private Sensor mainlinesensor = null;
	private Sensor queuesensor = null;
	private double gain_normalized;			// [-]
	
	private boolean targetdensity_given; 	// true if the user specifies the target density in the configuration file.
											// In this case the this value is used and kept constant
											// Otherwise it is assigned the critical density, which may change with fd profile.  
	
	private double targetvehicles;			// [veh/mile/lane]
	private boolean usesensor;
	
	boolean hasmainlinelink;		// true if config file contains entry for mainlinelink
	boolean hasmainlinesensor; 		// true if config file contains entry for mainlinesensor
	boolean hasqueuesensor; 		// true if config file contains entry for queuesensor

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_IRM_Alinea() {
	}

	public Controller_IRM_Alinea(Scenario myScenario,Link onramplink,Link mainlinelink,Sensor mainlinesensor,Sensor queuesensor,double gain_in_mph){

		this.myScenario = myScenario;
		this.onramplink 	= onramplink;
		this.mainlinelink 	= mainlinelink;
		this.mainlinesensor = mainlinesensor;
		this.queuesensor 	= queuesensor;
		
		hasmainlinelink   = mainlinelink!=null;
		hasmainlinesensor = mainlinesensor!=null;
		hasqueuesensor    = queuesensor!=null;
		
		// abort unless there is either one mainline link or one mainline sensor
		if(mainlinelink==null && mainlinesensor==null)
			return;
		if(mainlinelink!=null  && mainlinesensor!=null)
			return;
		
		usesensor = mainlinesensor!=null;
		
		// need the sensor's link for target density
		if(usesensor)
			mainlinelink = mainlinesensor.getMyLink();
		
		gain_normalized = gain_in_mph*myScenario.getSimDtInHours()/mainlinelink.getLengthInMiles();
		
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceController
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public void populate(Object jaxbobject) {

		com.relteq.sirius.jaxb.Controller jaxbc = (com.relteq.sirius.jaxb.Controller) jaxbobject;
		
		if(jaxbc.getTargetElements()==null)
			return;
		if(jaxbc.getTargetElements().getScenarioElement()==null)
			return;
		if(jaxbc.getFeedbackElements()==null)
			return;
		if(jaxbc.getFeedbackElements().getScenarioElement()==null)
			return;
		
		hasmainlinelink = false;
		hasmainlinesensor = false;
		hasqueuesensor = false;
		
		// There should be only one target element, and it is the onramp
		if(jaxbc.getTargetElements().getScenarioElement().size()==1){
			com.relteq.sirius.jaxb.ScenarioElement s = jaxbc.getTargetElements().getScenarioElement().get(0);
			onramplink = myScenario.getLinkWithCompositeId(s.getNetworkId(),s.getId());	
		}
		
		// Feedback elements can be "mainlinesensor","mainlinelink", and "queuesensor"
		if(!jaxbc.getFeedbackElements().getScenarioElement().isEmpty()){
			
			for(com.relteq.sirius.jaxb.ScenarioElement s:jaxbc.getFeedbackElements().getScenarioElement()){
				
				if(s.getUsage()==null)
					return;
				
				if( s.getUsage().equalsIgnoreCase("mainlinesensor") &&
				    s.getType().equalsIgnoreCase("sensor") && mainlinesensor==null){
					mainlinesensor=myScenario.getSensorWithCompositeId(s.getNetworkId(),s.getId());
					hasmainlinesensor = true;
				}

				if( s.getUsage().equalsIgnoreCase("mainlinelink") &&
					s.getType().equalsIgnoreCase("link") && mainlinelink==null){
					mainlinelink=myScenario.getLinkWithCompositeId(s.getNetworkId(),s.getId());
					hasmainlinelink = true;
				}

				if( s.getUsage().equalsIgnoreCase("queuesensor") &&
					s.getType().equalsIgnoreCase("sensor")  && queuesensor==null){
					queuesensor=myScenario.getSensorWithCompositeId(s.getNetworkId(),s.getId());
					hasqueuesensor = true;
				}				
			}
		}
		
		// abort unless there is either one mainline link or one mainline sensor
		if(mainlinelink==null && mainlinesensor==null)
			return;
		if(mainlinelink!=null  && mainlinesensor!=null)
			return;
		
		usesensor = mainlinesensor!=null;
		
		// need the sensor's link for target density
		if(usesensor)
			mainlinelink = mainlinesensor.getMyLink();
		
		if(mainlinelink==null)
			return;
		
		// read parameters
		double gain_in_mph = 50.0;
		targetdensity_given = false;
		if(jaxbc.getParameters()!=null)
			for(com.relteq.sirius.jaxb.Parameter p : jaxbc.getParameters().getParameter()){
				if(p.getName().equals("gain")){
					gain_in_mph = Double.parseDouble(p.getValue());
				}

				if(p.getName().equals("targetdensity")){
					targetvehicles = Double.parseDouble(p.getValue());   // [in vpmpl]
					targetvehicles *= mainlinelink.get_Lanes()*mainlinelink.getLengthInMiles();		// now in [veh]
					targetdensity_given = true;
				}
			}	
		
		// normalize the gain
		gain_normalized = gain_in_mph*myScenario.getSimDtInHours()/mainlinelink.getLengthInMiles();
	}
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;

		// must have exactly one target
		if(targets.size()!=1)
			return false;

		// bad mainline sensor id
		if(hasmainlinesensor && mainlinesensor==null)
			return false;

		// bad queue sensor id
		if(hasqueuesensor && queuesensor==null)
			return false;		
		
		// both link and sensor feedback
		if(hasmainlinelink && hasmainlinesensor)
			return false;
		
		// sensor is disconnected
		if(usesensor && mainlinesensor.getMyLink()==null)
			 return false;
		
		// no feedback
		if(mainlinelink==null)
			return false;
		
		// Target link id not found, or number of targets not 1.
		if(onramplink==null)
			return false;
			
		// negative gain
		if(gain_normalized<=0f)
			return false;
		
		return true;
	}

	@Override
	public void reset() {
		super.reset();
	}

	@Override
	public void update() {
		
		// get mainline density either from sensor or from link
		double mainlinevehicles;		// [veh]
		if(usesensor)
			mainlinevehicles = mainlinesensor.getTotalDensityInVeh(0);
		else
			mainlinevehicles = mainlinelink.getTotalDensityInVeh(0);
				
		// need to read target density each time if not given
		if(!targetdensity_given)
			targetvehicles = mainlinelink.getDensityCriticalInVeh(0);
		
		// metering rate
		control_maxflow[0] = onramplink.getTotalOutflowInVeh(0) + gain_normalized*(targetvehicles-mainlinevehicles);
	}

	@Override
	public boolean register() {
		return registerFlowController(onramplink,0);
	}

}
