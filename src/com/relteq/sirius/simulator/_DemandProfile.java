/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class _DemandProfile extends com.relteq.sirius.jaxb.DemandProfile {

	protected _Scenario myScenario;
	protected _Link myLinkOrigin;
	protected double dtinseconds;			// not really necessary
	protected int samplesteps;
	protected Double2DMatrix demand_nominal;	// [veh]
	protected boolean isdone; 
	protected int stepinitial;
	protected double _knob;
	protected Double std_dev_add;				// [veh]
	protected Double std_dev_mult;			// [veh]
	protected boolean isdeterministic;		// true if the profile is deterministic

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	protected void set_knob(double _knob) {
		this._knob = Math.max(_knob,0.0);
		
		// resample the profile
		update(true);
	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(_Scenario myScenario) {

		this.myScenario = myScenario;
		
		isdone = false;
		
		// required
		myLinkOrigin = myScenario.getLinkWithCompositeId(getNetworkIdOrigin(),getLinkIdOrigin());

		// sample demand distribution, convert to vehicle units
		demand_nominal = new Double2DMatrix(getContent());
		demand_nominal.multiplyscalar(myScenario.getSimDtInHours());

		// optional dt
		if(getDt()!=null){
			dtinseconds = getDt().floatValue();					// assume given in seconds
			samplesteps = SiriusMath.round(dtinseconds/myScenario.getSimDtInSeconds());
		}
		else{ 	// allow only if it contains one time step
			if(demand_nominal.getnTime()==1){
				dtinseconds = Double.POSITIVE_INFINITY;
				samplesteps = Integer.MAX_VALUE;
			}
			else{
				dtinseconds = -1.0;		// this triggers the validation error
				samplesteps = -1;
				return;
			}
		}
		
		// optional uncertainty model
		if(getStdDevAdd()!=null)
			std_dev_add = getStdDevAdd().doubleValue()*myScenario.getSimDtInHours();
		else
			std_dev_add = Double.POSITIVE_INFINITY;		// so that the other will always win the min
		
		if(getStdDevMult()!=null)
			std_dev_mult = getStdDevMult().doubleValue()*myScenario.getSimDtInHours();
		else
			std_dev_mult = Double.POSITIVE_INFINITY;	// so that the other will always win the min
		
		isdeterministic = (getStdDevAdd()==null || std_dev_add==0.0) && 
						  (getStdDevMult()==null || std_dev_mult==0.0);
		
		_knob = getKnob().doubleValue();
		
		// read start time, convert to stepinitial
		double starttime;	// [sec]
		if( getStartTime()!=null)
			starttime = getStartTime().floatValue();
		else
			starttime = 0f;

		stepinitial = SiriusMath.round((starttime-myScenario.getTimeStart())/myScenario.getSimDtInSeconds());
		
	}

	protected boolean validate() {
		
		if(demand_nominal.isEmpty())
			return true;
		
		if(myLinkOrigin==null){
			SiriusErrorLog.addErrorMessage("Bad link id in demand profile: " + getLinkIdOrigin());
			return false;
		}
		
		// check dtinseconds
		if( dtinseconds<=0 ){
			SiriusErrorLog.addErrorMessage("Demand profile dt should be positive: " + getLinkIdOrigin());
			return false;	
		}
		
		if(!SiriusMath.isintegermultipleof(dtinseconds,myScenario.getSimDtInSeconds())){
			SiriusErrorLog.addErrorMessage("Demand dt should be multiple of sim dt: " + getLinkIdOrigin());
			return false;	
		}
		
		// check dimensions
		if(demand_nominal.getnVTypes()!=myScenario.getNumVehicleTypes()){
			SiriusErrorLog.addErrorMessage("Incorrect dimensions for demand on link " + getLinkIdOrigin());
			return false;
		}
		
		// check non-negative
		if(demand_nominal.hasNaN()){
			SiriusErrorLog.addErrorMessage("Illegal values in demand profile for link " + getLinkIdOrigin());
			return false;
		}

		return true;
	}

	protected void reset() {
		isdone = false;
		
		// set knob back to its original value
		_knob = getKnob().doubleValue();	
	}
	
	protected void update(boolean forcesample) {
		if(!forcesample)
			if(isdone || demand_nominal.isEmpty())
				return;
		if(forcesample || myScenario.clock.istimetosample(samplesteps,stepinitial)){
			int n = demand_nominal.getnTime()-1;
			int step = SiriusMath.floor((myScenario.clock.getCurrentstep()-stepinitial)/samplesteps);
			step = Math.max(0,step);
			if(step<n)
				myLinkOrigin.setSourcedemandFromVeh( sampleAtTimeStep(step) );
			if( forcesample || (step>=n && !isdone) ){
				myLinkOrigin.setSourcedemandFromVeh( sampleAtTimeStep(n) );
				isdone = true;
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////
	
	private Double [] sampleAtTimeStep(int k){
		
		// get vehicle type order from SplitRatioProfileSet
		Integer [] vehicletypeindex = null;
		if(myScenario.getSplitRatioProfileSet()!=null)
			vehicletypeindex = ((_DemandProfileSet)myScenario.getDemandProfileSet()).vehicletypeindex;
		
		Double [] demandvalue = demand_nominal.sampleAtTime(k,vehicletypeindex);
		
		if(!isdeterministic){
			
			// use smallest between multiplicative and additive standard deviations
			Double [] std_dev_apply = new Double [myScenario.getNumVehicleTypes()];
			for(int j=0;j<myScenario.getNumVehicleTypes();j++)
				std_dev_apply[j] = Math.min( demandvalue[j]*std_dev_mult , std_dev_add );
			
			// sample the distribution
			switch(myScenario.uncertaintyModel){
			case uniform:
				for(int j=0;j<myScenario.getNumVehicleTypes();j++)
					demandvalue[j] += std_dev_apply[j]*Math.sqrt(3)*(2*myScenario.random.nextDouble()-1);
				break;
	
			case gaussian:
				for(int j=0;j<myScenario.getNumVehicleTypes();j++)
					demandvalue[j] += std_dev_apply[j]*myScenario.random.nextGaussian();
				break;
			}
		}
		
		// apply the knob and non-negativity
		for(int j=0;j<myScenario.getNumVehicleTypes();j++)
			demandvalue[j] = Math.max(0.0,myScenario.global_demand_knob*demandvalue[j]*_knob);
		
		return demandvalue;
	}
	
}
