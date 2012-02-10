/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.control;

import com.relteq.sirius.jaxb.Parameter;
import com.relteq.sirius.jaxb.ScenarioElement;
import com.relteq.sirius.simulator.Utils;
import com.relteq.sirius.simulator._Controller;
import com.relteq.sirius.simulator._Link;
import com.relteq.sirius.simulator._Sensor;

public class ControllerAlinea extends _Controller {

	private double targetvehicles;		// [veh]
	private double gain;				// [-]
	private _Link mainlinelink = null;
	private _Sensor mainlinesensor = null;
	private _Sensor queuesensor = null;
	private boolean usesensor;
	
	private _Link onramplink = null;
	
	boolean hasmainlinelink;		// true if config file contains entry for mainlinelink
	boolean hasmainlinesensor; 		// true if config file contains entry for mainlinesensor
	boolean hasqueuesensor; 		// true if config file contains entry for queuesensor

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public ControllerAlinea(com.relteq.sirius.jaxb.Controller c,com.relteq.sirius.simulator.Types.Controller myType) {
		super(c,myType);

		hasmainlinelink = false;
		hasmainlinesensor = false;
		hasqueuesensor = false;
		
		// There should be only one target element, and it is the onramp
		if(c.getTargetElements().getScenarioElement().size()==1){
			ScenarioElement s = c.getTargetElements().getScenarioElement().get(0);
			onramplink = Utils.getLinkWithCompositeId(s.getNetworkId(),s.getId());	
		}
		
		// Feedback elements can be "mainlinesensor","mainlinelink", and "queuesensor"
		if(!c.getFeedbackElements().getScenarioElement().isEmpty()){
			
			for(ScenarioElement s:c.getFeedbackElements().getScenarioElement()){
				
				if( s.getUsage().equalsIgnoreCase("mainlinesensor") &&
				    s.getType().equalsIgnoreCase("sensor") && mainlinesensor==null){
					mainlinesensor=Utils.getSensorWithCompositeId(s.getNetworkId(),s.getId());
					hasmainlinesensor = true;
				}

				if( s.getUsage().equalsIgnoreCase("mainlinelink") &&
					s.getType().equalsIgnoreCase("link") && mainlinelink==null){
					mainlinelink=Utils.getLinkWithCompositeId(s.getNetworkId(),s.getId());
					hasmainlinelink = true;
				}

				if( s.getUsage().equalsIgnoreCase("queuesensor") &&
					s.getType().equalsIgnoreCase("sensor")  && queuesensor==null){
					queuesensor=Utils.getSensorWithCompositeId(s.getNetworkId(),s.getId());
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
		if(c.getParameters()!=null)
			for(Parameter p : c.getParameters().getParameter())
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
		gain = gain_in_mph * Utils.getSimDtInHours() /linklength;
		
	}
	
	/////////////////////////////////////////////////////////////////////
	// _AuroraComponent
	/////////////////////////////////////////////////////////////////////

	@Override
	public boolean validate() {
		if(!super.validate())
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
		if(this.onramplink==null)
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
		super.update();
		
		double mainlinedensity;
		if(usesensor)
			mainlinedensity = mainlinesensor.getTotalDensityInVeh();
		else
			mainlinedensity = mainlinelink.getTotalDensityInVeh();

		double desiredvehrate = onramplink.getTotalOutflowInVeh() + gain*(targetvehicles-mainlinedensity);
		setLinkMaxFlow(desiredvehrate);
	}

}
