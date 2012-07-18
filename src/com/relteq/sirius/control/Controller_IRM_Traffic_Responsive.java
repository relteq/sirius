package com.relteq.sirius.control;

import com.relteq.sirius.simulator.Controller;
import com.relteq.sirius.simulator.Link;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.Sensor;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.Table;

public class Controller_IRM_Traffic_Responsive extends Controller {
	
	private Link onramplink = null;
	private Link mainlinelink = null;
	private Sensor mainlinesensor = null;
	private Sensor queuesensor = null;
	private boolean usesensor;
	
	boolean hasmainlinelink;		// true if config file contains entry for mainlinelink
	boolean hasmainlinesensor; 		// true if config file contains entry for mainlinesensor
	boolean hasqueuesensor; 		// true if config file contains entry for queuesensor

	private boolean istablevalid;   // true if a valid table is given

	boolean hasoccthres;
	boolean hasflowthres;
	boolean hasspeedthres;
	
	private double[] trFlowThresh;  // stores flow thresholds corresponding to the traffic responsive controllers.
	private double[] trOccThresh;  // stores occupancy thresholds corresponding to the traffic responsive controllers.
	private double[] trSpeedThresh;  // stores speed thresholds corresponding to the traffic responsive controllers.
	private double[] trMeteringRates_normalized; // normalized metering rates corresponding to the different levels of the traffic responsive controller.
	
	private int trlevelindex; // denotes the current level that is requested by the traffic responsive logic.
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_IRM_Traffic_Responsive() {
		// TODO Auto-generated constructor stub
	}

	public Controller_IRM_Traffic_Responsive(Scenario myScenario,Link onramplink,Link mainlinelink,Sensor mainlinesensor,Sensor queuesensor,Table trtable){

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
		
		// Traffic responsive table.
		this.table = trtable;
		
		this.extractTable();
		
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
			onramplink = myScenario.getLinkWithId(s.getId());	
		}
		
		// Feedback elements can be "mainlinesensor","mainlinelink", and "queuesensor"
		if(!jaxbc.getFeedbackElements().getScenarioElement().isEmpty()){
			
			for(com.relteq.sirius.jaxb.ScenarioElement s:jaxbc.getFeedbackElements().getScenarioElement()){
				
				if(s.getUsage()==null)
					return;
				
				if( s.getUsage().equalsIgnoreCase("mainlinesensor") &&
				    s.getType().equalsIgnoreCase("sensor") && mainlinesensor==null){
					mainlinesensor=myScenario.getSensorWithId(s.getId());
					hasmainlinesensor = true;
				}

				if( s.getUsage().equalsIgnoreCase("mainlinelink") &&
					s.getType().equalsIgnoreCase("link") && mainlinelink==null){
					mainlinelink=myScenario.getLinkWithId(s.getId());
					hasmainlinelink = true;
				}

				if( s.getUsage().equalsIgnoreCase("queuesensor") &&
					s.getType().equalsIgnoreCase("sensor")  && queuesensor==null){
					queuesensor=myScenario.getSensorWithId(s.getId());
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
		
		this.extractTable();
		
		
	}
	
	private void extractTable(){
		// read parameters from table, and also validate
		
		
		int rateIndx = table.getColumnNo("MeteringRates");
		int occIndx = table.getColumnNo("OccupancyThresholds");
		int spdIndx = table.getColumnNo("SpeedThresholds");
		int flwIndx = table.getColumnNo("FlowThresholds");
		
		hasflowthres=(flwIndx!=-1);
		hasspeedthres=(spdIndx!=-1);
		hasoccthres=(occIndx!=-1);		
		
		istablevalid=table.checkTable() && (rateIndx!=-1) && (hasflowthres || hasoccthres || hasspeedthres);
		
		// need a valid table to parse
		if (!istablevalid) 
			return;
		
		
		// read table, initialize values. 
		if (hasflowthres)
			trFlowThresh=new double[table.getNoRows()];
		
		if (hasoccthres)
			trOccThresh=new double[table.getNoRows()];
		
		if (hasspeedthres)
			trSpeedThresh=new double[table.getNoRows()];
		
		
		trMeteringRates_normalized=new double[table.getNoRows()];			
		trlevelindex = 0;
		// extract data from the table and populate
		for (int i=0;i<table.getNoRows();i++){
			trMeteringRates_normalized[i]=Double.parseDouble(table.getTableElement(i,rateIndx))* myScenario.getSimDtInHours(); // in veh per sim step
			if (hasflowthres){
				trFlowThresh[i]=Double.parseDouble(table.getTableElement(i,flwIndx));			// flow in veh/hr	
			}
			if (hasoccthres){
				trOccThresh[i]=Double.parseDouble(table.getTableElement(i,occIndx));  			// occupancy in %
			}
			if (hasspeedthres){
				trSpeedThresh[i]=Double.parseDouble(table.getTableElement(i,spdIndx)); 			// speed in mph
			}

			if (i==0 && ((hasflowthres && trFlowThresh[i]<0) || (hasoccthres && trOccThresh[i]<0) ||
					(hasspeedthres && trSpeedThresh[i]<0)))
					istablevalid=false;
			// decreasing metering rates, and increasing thresholds, where applicable.		
			if ((trMeteringRates_normalized[i]<0) || (i>0 && (trMeteringRates_normalized[i]>trMeteringRates_normalized[i-1])) || 
			(i>0 && !((hasflowthres && trFlowThresh[i]>trFlowThresh[i-1]) || (hasoccthres && trOccThresh[i]>trOccThresh[i-1]) ||
					(hasspeedthres && trSpeedThresh[i]>trSpeedThresh[i-1]))))				
				istablevalid=false;					
		}
		
		// occupancy thresholds should be between 0 and 100.
		if (hasoccthres && trOccThresh[0]<=0 && trOccThresh[trOccThresh.length-1]>100)
			istablevalid=false;
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
		if(!istablevalid){
			SiriusErrorLog.addErrorMessage("Controller has an invalid table.");			
		}
		return istablevalid;
	}
	
	
	@Override
	public void reset() {
		super.reset();
	}

	@Override
	public void update() {
		
		double mainlineocc=Double.POSITIVE_INFINITY;
		double mainlinespeed=Double.POSITIVE_INFINITY;
		double mainlineflow=Double.POSITIVE_INFINITY;
		// get mainline occ/spd/flow either from sensor or from link	
		if (hasoccthres)
			if(usesensor){
				mainlineocc = mainlinesensor.getOccupancy(0);			
			}
			else {
				mainlineocc = mainlinelink.getTotalDensityInVeh(0)/mainlinelink.getDensityJamInVeh(0);
			}
		
		if (hasspeedthres)
			if(usesensor){
				mainlinespeed = mainlinesensor.getSpeedInMPH(0);			
			}
			else {
				mainlinespeed = mainlinelink.getTotalOutflowInVeh(0)/mainlinelink.getTotalDensityInVPM(0)/myScenario.getSimDtInHours();
			}
		
		if (hasflowthres)
			if(usesensor){
				mainlineflow = mainlinesensor.getTotalFlowInVPH(0);			
			}
			else {
				mainlineflow = mainlinelink.getTotalOutflowInVeh(0)/myScenario.getSimDtInHours();
			}		
		
		// metering rate adjustments
		while (trlevelindex >0 && (hasoccthres && mainlineocc<=trOccThresh[trlevelindex]) 
				&& (hasspeedthres && mainlinespeed<=trSpeedThresh[trlevelindex])
				&& (hasflowthres && mainlineflow<=trFlowThresh[trlevelindex]))
			trlevelindex--;
		
		while (trlevelindex <trMeteringRates_normalized.length-1 &&
				((hasoccthres && mainlineocc>trOccThresh[trlevelindex+1]) || 
				(hasspeedthres && mainlinespeed>trSpeedThresh[trlevelindex])
				|| (hasflowthres && mainlineflow>trFlowThresh[trlevelindex])))
			trlevelindex++;
		
		
		control_maxflow[0]=trMeteringRates_normalized[trlevelindex];		

	}
	
	

	@Override
	public boolean register() {
		return registerFlowController(onramplink,0);
	}
	
	public boolean deregister() {
		return deregisterFlowController(onramplink);
	}

}
