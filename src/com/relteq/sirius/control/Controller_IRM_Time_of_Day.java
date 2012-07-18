package com.relteq.sirius.control;

import java.util.Arrays;

import com.relteq.sirius.simulator.Controller;
import com.relteq.sirius.simulator.Link;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.Sensor;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.Table;


public class Controller_IRM_Time_of_Day extends Controller {
	
	private Link onramplink = null;	
	private Sensor queuesensor = null;	
	private boolean hasqueuesensor;
	
	private double[] todMeteringRates_normalized;			
	private double[] todActivationTimes;
	private int todActivationIndx;	
	
	
	private boolean istablevalid;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_IRM_Time_of_Day() {
		// TODO Auto-generated constructor stub
	}
	
	public Controller_IRM_Time_of_Day(Scenario myScenario,Link onramplink,Sensor queuesensor,Table todtable){

		this.myScenario = myScenario;
		this.onramplink 	= onramplink;
		this.queuesensor 	= queuesensor;
		
		hasqueuesensor    = queuesensor!=null;		
			
		
		// Time of day table.
		this.table = todtable;
		
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
		if(jaxbc.getFeedbackElements()!=null)
			return;				
		
		hasqueuesensor = false;
		
		// There should be only one target element, and it is the onramp
		if(jaxbc.getTargetElements().getScenarioElement().size()==1){
			com.relteq.sirius.jaxb.ScenarioElement s = jaxbc.getTargetElements().getScenarioElement().get(0);
			onramplink = myScenario.getLinkWithId(s.getId());	
		}
				
		this.extractTable();
		
	    
		
	}

	private void extractTable(){
		
		// read parameters from table, and also validate
		
		int timeIndx = table.getColumnNo("StartTime");
		int rateIndx = table.getColumnNo("MeteringRates");
		
		
		istablevalid=table.checkTable() && (table.getNoColumns()!=2? false:true) && (timeIndx!=-1) && (rateIndx!=-1);
		
		// need a valid table to parse
		if (!istablevalid) 
			return;
		
		
		// read table, initialize values. 
		todMeteringRates_normalized=new double[table.getNoRows()];
		todActivationTimes=new double[table.getNoRows()];		
		todActivationIndx=0;
		
		for (int i=0;i<table.getNoRows();i++){
			todMeteringRates_normalized[i]=Double.parseDouble(table.getTableElement(i,rateIndx))* myScenario.getSimDtInHours(); // in veh per sim step
			todActivationTimes[i]=Double.parseDouble(table.getTableElement(i,timeIndx)); // in sec
			// check that table values are valid.			
			if ((i>0 && todActivationTimes[i]<=todActivationTimes[i-1])||(todMeteringRates_normalized[i]<0))
				istablevalid=false;					
		}			
		
		if (todActivationTimes[0]>this.myStartTime())
			istablevalid=false;
		}
	
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;

		// must have exactly one target
		if(targets.size()!=1)
			return false;

		// bad queue sensor id
		if(hasqueuesensor && queuesensor==null)
			return false;		
		
		// Target link id not found, or number of targets not 1.
		if(onramplink==null)
			return false;
			
		// has valid tables	
		
		if(!istablevalid){
			SiriusErrorLog.addErrorMessage("Controller has an invalid TOD table.");			
		}
		return istablevalid;
	}
	
	
	@Override
	public void reset() {
		super.reset();
		todActivationIndx=0;
		while (todActivationIndx<todActivationTimes.length-1 && todActivationTimes[todActivationIndx+1] <=myScenario.getTimeStart())
			todActivationIndx++;
		control_maxflow[0]=todMeteringRates_normalized[todActivationIndx];
	}

	@Override
	public void update() {
		while (todActivationIndx<todActivationTimes.length-1 && todActivationTimes[todActivationIndx+1] <=myScenario.getTimeInSeconds())
			control_maxflow[0]=todMeteringRates_normalized[++todActivationIndx];		
	}
	
	@Override
	public boolean register() {
		return registerFlowController(onramplink,0);
	}
	
	public boolean deregister() {
		return deregisterFlowController(onramplink);
	}
	

}
