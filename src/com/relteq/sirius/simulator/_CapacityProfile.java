/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

public class _CapacityProfile extends com.relteq.sirius.jaxb.CapacityProfile {

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
		myLink = Utils.getLinkWithCompositeId(getNetworkId(),getLinkId());
		dtinseconds = getDt().floatValue();					// assume given in seconds
		samplesteps = Utils.round(dtinseconds/Utils.simdtinseconds);
		isdone = false;
		
		// read start time, convert to stepinitial
		double starttime;
		if( getStartTime()!=null){
			starttime = getStartTime().floatValue();
//			if(starttime>0 && starttime<=24){
//				System.out.println("Warning: Initial time given in hours. Changing to seconds.");
//				starttime *= 3600f;
//			}
		}
		else
			starttime = 0f;

		stepinitial = (int) Math.round((starttime-Utils.timestart)/Utils.simdtinseconds);
		
		// read capacity and convert to vehicle units
		capacity = new Double1DVector(getContent(),",");	// true=> reshape to vector along k, define length
		capacity.multiplyscalar(Utils.simdtinhours*myLink.get_Lanes());
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

		if(!Utils.isintegermultipleof(dtinseconds,Utils.simdtinseconds)){
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
		if(Utils.clock.istimetosample(samplesteps,stepinitial)){
			int n = capacity.getLength()-1;
			int step = Utils.floor((Utils.clock.getCurrentstep()-stepinitial)/samplesteps);
			if(step<n)
				myLink.getCurrentFD().setCapacityFromVeh( capacity.get(step) );
			if(step>=n && !isdone){
				myLink.getCurrentFD().setCapacityFromVeh( capacity.get(n) );
				isdone = true;
			}
		}
	}

}
