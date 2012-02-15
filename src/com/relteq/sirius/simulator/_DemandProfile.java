/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class _DemandProfile extends com.relteq.sirius.jaxb.DemandProfile {

	private _Link myLinkOrigin;
	private double dtinseconds;			// not really necessary
	private int samplesteps;
	private Double2DMatrix demand_nominal;	// [veh]
	private boolean isdone; 
	private int stepinitial;
	private double _knob;
	private Double std_dev_add;				// [veh]
	private Double std_dev_mult;			// [veh]
	private boolean isdeterministic;		// true if the profile is deterministic

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	public void set_knob(double _knob) {
		this._knob = Math.max(_knob,0.0);
	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate() {

		isdone = false;
		
		// required
		myLinkOrigin = Utils.getLinkWithCompositeId(getNetworkIdOrigin(),getLinkIdOrigin());

		// sample demand distribution, convert to vehicle units
		demand_nominal = new Double2DMatrix(getContent());
		demand_nominal.multiplyscalar(Utils.simdtinhours);

		// optional dt
		if(getDt()!=null){
			dtinseconds = getDt().floatValue();					// assume given in seconds
			samplesteps = Utils.round(dtinseconds/Utils.simdtinseconds);
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
			std_dev_add = getStdDevAdd().doubleValue()*Utils.simdtinhours;
		else
			std_dev_add = Double.POSITIVE_INFINITY;		// so that the other will always win the min
		
		if(getStdDevMult()!=null)
			std_dev_mult = getStdDevMult().doubleValue()*Utils.simdtinhours;
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

		stepinitial = Utils.round((starttime-Utils.timestart)/Utils.simdtinseconds);
		
	}

	protected boolean validate() {
		
		if(demand_nominal.isEmpty())
			return true;
		
		if(myLinkOrigin==null){
			System.out.println("Bad link id in demand profile: " + getLinkIdOrigin());
			return false;
		}
		
		// check dtinseconds
		if( dtinseconds<=0 ){
			System.out.println("Demand profile dt should be positive: " + getLinkIdOrigin());
			return false;	
		}
		
		if(!Utils.isintegermultipleof(dtinseconds,Utils.simdtinseconds)){
			System.out.println("Demand dt should be multiple of sim dt: " + getLinkIdOrigin());
			return false;	
		}
		
		// check dimensions
		if(demand_nominal.getnVTypes()!=Utils.numVehicleTypes){
			System.out.println("Incorrect dimensions for demand on link " + getLinkIdOrigin());
			return false;
		}
		
		// check non-negative
		if(demand_nominal.hasNaN()){
			System.out.println("Illegal values in demand profile for link " + getLinkIdOrigin());
			return false;
		}

		return true;
	}

	protected void reset() {
		isdone = false;
		
		// set knob back to its original value
		_knob = getKnob().doubleValue();	
	}
	
	protected void update() {
		if(isdone || demand_nominal.isEmpty())
			return;
		if(Utils.clock.istimetosample(samplesteps,stepinitial)){
			int n = demand_nominal.getnTime()-1;
			int step = Utils.floor((Utils.clock.getCurrentstep()-stepinitial)/samplesteps);
			step = Math.max(0,step);
			if(step<n)
				myLinkOrigin.setSourcedemandFromVeh( sampleAtTimeStep(step) );
			if(step>=n && !isdone){
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
		if(Utils.theScenario.getSplitRatioProfileSet()!=null)
			vehicletypeindex = ((_DemandProfileSet)Utils.theScenario.getDemandProfileSet()).vehicletypeindex;
		
		Double [] demandvalue = demand_nominal.sampleAtTime(k,vehicletypeindex);
		
		if(isdeterministic)
			return demandvalue;
			
		// use smallest between multiplicative and additive standard deviations
		Double [] std_dev_apply = new Double [Utils.numVehicleTypes];
		for(int j=0;j<Utils.numVehicleTypes;j++)
			std_dev_apply[j] = Math.min( demandvalue[j]*std_dev_mult , std_dev_add );
		
		// sample the distribution
		switch(Utils.uncertaintyModel){
		case uniform:
			for(int j=0;j<Utils.numVehicleTypes;j++)
				demandvalue[j] += std_dev_apply[j]*Math.sqrt(3)*(2*Utils.random.nextDouble()-1);
			break;

		case gaussian:
			for(int j=0;j<Utils.numVehicleTypes;j++)
				demandvalue[j] += std_dev_apply[j]*Utils.random.nextGaussian();
			break;
		}
		
		// apply the knob and non-negativity
		for(int j=0;j<Utils.numVehicleTypes;j++)
			demandvalue[j] = Math.max(0.0, demandvalue[j]*_knob);
		
		return demandvalue;
	}
	
}
