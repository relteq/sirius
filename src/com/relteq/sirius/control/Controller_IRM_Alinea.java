
package com.relteq.sirius.control;

import com.relteq.sirius.jaxb.Parameter;
import com.relteq.sirius.jaxb.ScenarioElement;
import com.relteq.sirius.simulator._Controller;
import com.relteq.sirius.simulator._Link;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._Sensor;

public class Controller_IRM_Alinea extends _Controller {

	private _Link onramplink = null;
	private _Link mainlinelink = null;
	private _Sensor mainlinesensor = null;
	private _Sensor queuesensor = null;
	private double gain;				// [-]
	
	private double targetvehicles;		// [veh]
	private boolean usesensor;
	
	boolean hasmainlinelink;		// true if config file contains entry for mainlinelink
	boolean hasmainlinesensor; 		// true if config file contains entry for mainlinesensor
	boolean hasqueuesensor; 		// true if config file contains entry for queuesensor

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_IRM_Alinea() {
		// TODO Auto-generated constructor stub
	}
	
	public Controller_IRM_Alinea(_Scenario myScenario,_Link onramplink,_Link mainlinelink,_Sensor mainlinesensor,_Sensor queuesensor,double gain_in_mph){

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

		// normalize the gain 
		double linklength;
		if(usesensor){
			 _Link mylink = mainlinesensor.getMyLink();
			 if(mylink==null)
				 return;
			 linklength = mylink.getLengthInMiles();
			 targetvehicles = mylink.getDensityCriticalInVeh();
		}
		else{
			linklength = mainlinelink.getLengthInMiles();
			targetvehicles = mainlinelink.getDensityCriticalInVeh();
		}
		this.gain = gain_in_mph * myScenario.getSimDtInHours()/linklength;
		
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
			ScenarioElement s = jaxbc.getTargetElements().getScenarioElement().get(0);
			onramplink = myScenario.getLinkWithCompositeId(s.getNetworkId(),s.getId());	
		}
		
		// Feedback elements can be "mainlinesensor","mainlinelink", and "queuesensor"
		if(!jaxbc.getFeedbackElements().getScenarioElement().isEmpty()){
			
			for(ScenarioElement s:jaxbc.getFeedbackElements().getScenarioElement()){
				
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
		
		// read parameters
		double gain_in_mph = 50.0;
		if(jaxbc.getParameters()!=null)
			for(Parameter p : jaxbc.getParameters().getParameter())
				if(p.getName().equals("gain"))
					gain_in_mph = Double.parseDouble(p.getValue());

		
		// normalize the gain and set target to critical density
		double linklength;
		if(usesensor){
			 _Link mylink = mainlinesensor.getMyLink();
			 if(mylink==null)
				 return;
			 linklength = mylink.getLengthInMiles();
			 targetvehicles = mylink.getDensityCriticalInVeh();
		}
		else{
			linklength = mainlinelink.getLengthInMiles();
			targetvehicles = mainlinelink.getDensityCriticalInVeh();
		}
		gain = gain_in_mph * myScenario.getSimDtInHours() /linklength;
	}
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;

		// must have exactly one target
		if(targets.size()!=1)
			return false;
		
		// bad mainline link id
		if(hasmainlinelink && mainlinelink==null)
			return false;

		// bad mainline sensor id
		if(hasmainlinesensor && mainlinesensor==null)
			return false;

		// bad queue sensor id
		if(hasqueuesensor && queuesensor==null)
			return false;		
		
		// both link and sensor feedback
		if(mainlinelink==null && mainlinesensor==null)
			return false;
		
		// sensor is disconnected
		if(usesensor && mainlinesensor.getMyLink()==null)
			 return false;
		
		// no feedback
		if(mainlinelink!=null  && mainlinesensor!=null)
			return false;
		
		// Target link id not found, or number of targets not 1.
		if(onramplink==null)
			return false;
			
		// negative gain
		if(gain<=0f)
			return false;
		
		return true;
	}

	@Override
	public void reset() {
		super.reset();
	}

	@Override
	public void update() {
		
		double mainlinedensity;
		if(usesensor)
			mainlinedensity = mainlinesensor.getTotalDensityInVPM()*mainlinesensor.getMyLink().getLengthInMiles();
		else
			mainlinedensity = mainlinelink.getTotalDensityInVeh();

		control_maxflow[0] = onramplink.getTotalOutflowInVeh() + gain*(targetvehicles-mainlinedensity);
	}

	@Override
	public boolean register() {
		return registerFlowController(onramplink,0);
	}

}
