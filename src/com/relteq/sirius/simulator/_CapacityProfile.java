/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class _CapacityProfile extends com.relteq.sirius.jaxb.CapacityProfile {

	private _Link myLink;
	private double dtinseconds;				// not really necessary
	private int samplesteps;
	private Double1DVector capacity;		// [veh]
	private boolean isdone;
	private int stepinitial;

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate() {
		myLink = API.getLinkWithCompositeId(getNetworkId(),getLinkId());
		dtinseconds = getDt().floatValue();					// assume given in seconds
		samplesteps = SiriusMath.round(dtinseconds/API.getSimDtInSeconds());
		isdone = false;
		
		// read start time, convert to stepinitial
		double starttime;
		if( getStartTime()!=null)
			starttime = getStartTime().floatValue();
		else
			starttime = 0f;

		stepinitial = (int) Math.round((starttime-API.getTimeStart())/API.getSimDtInSeconds());
		
		// read capacity and convert to vehicle units
		capacity = new Double1DVector(getContent(),",");	// true=> reshape to vector along k, define length
		capacity.multiplyscalar(API.getSimDtInHours()*myLink.get_Lanes());
	}
	
	protected boolean validate() {
		
		if(capacity.isEmpty())
			return true;
		
		if(myLink==null){
			System.out.println("Bad link id in capacity profile: " + getLinkId());
			return false;
		}
		
		// check dtinseconds
		if( dtinseconds<=0 ){
			System.out.println("Capacity profile dt should be positive: " + getLinkId());
			return false;	
		}

		if(!SiriusMath.isintegermultipleof(dtinseconds,API.getSimDtInSeconds())){
			System.out.println("Capacity dt should be multiple of sim dt: " + getLinkId());
			return false;	
		}
		
		// check non-negative
		if(capacity.hasNaN()){
			System.out.println("Capacity profile has illegal values: " + getLinkId());
			return false;
		}

		return true;
	}

	protected void reset() {
		isdone = false;
	}
	
	protected void update() {
		if(isdone || capacity.isEmpty())
			return;
		if(Global.clock.istimetosample(samplesteps,stepinitial)){
			int n = capacity.getLength()-1;
			int step = SiriusMath.floor((Global.clock.getCurrentstep()-stepinitial)/samplesteps);
			if(step<n)
				myLink.FD.setCapacityFromVeh( capacity.get(step) );
			if(step>=n && !isdone){
				myLink.FD.setCapacityFromVeh( capacity.get(n) );
				isdone = true;
			}
		}
	}

}
