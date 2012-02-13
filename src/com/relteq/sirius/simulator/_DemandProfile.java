/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

public class _DemandProfile extends com.relteq.sirius.jaxb.DemandProfile {

	private _Link myLinkOrigin;
	private double dtinseconds;			// not really necessary
	private int samplesteps;
	private Double2DMatrix demand;		// [veh]
	private boolean isdone; 
	private int stepinitial;
	private double _knob;

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
		demand = new Double2DMatrix(getContent());
		demand.multiplyscalar(Utils.simdtinhours);
		
		// optional dt
		if(getDt()!=null){
			dtinseconds = getDt().floatValue();					// assume given in seconds
			samplesteps = Utils.round(dtinseconds/Utils.simdtinseconds);
		}
		else{ 	// allow only if it contains one time step
			if(demand.getnTime()==1){
				dtinseconds = Double.POSITIVE_INFINITY;
				samplesteps = Integer.MAX_VALUE;
			}
			else{
				dtinseconds = -1.0;		// this triggers the validation error
				samplesteps = -1;
				return;
			}
		}
		
		_knob = getKnob().doubleValue();
		
		// read start time, convert to stepinitial
		double starttime;	// [sec]
		if( getStartTime()!=null){
			starttime = getStartTime().floatValue();
//			if(starttime>0 && starttime<=24){
//				System.out.println("Warning: Initial time given in hours. Changing to seconds.");
//				starttime *= 3600f;
//			}
		}
		else
			starttime = 0f;

		stepinitial = Utils.round((starttime-Utils.timestart)/Utils.simdtinseconds);
		
	}

	protected boolean validate() {
		
		if(demand.isEmpty())
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
		if(demand.getnVTypes()!=Utils.numVehicleTypes){
			System.out.println("Incorrect dimensions for demand on link " + getLinkIdOrigin());
			return false;
		}
		
		// check non-negative
		if(demand.hasNaN()){
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
		if(isdone || demand.isEmpty())
			return;
		if(Utils.clock.istimetosample(samplesteps,stepinitial)){
			int n = demand.getnTime()-1;
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
		
		Double [] x = Utils.times( demand.sampleAtTime(k,vehicletypeindex) , _knob);
		return x;
	}
	
}
