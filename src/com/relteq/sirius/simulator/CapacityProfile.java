/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class CapacityProfile extends com.relteq.sirius.jaxb.CapacityProfile {

	protected Scenario myScenario;
	protected Link myLink;
	protected double dtinseconds;			// not really necessary
	protected int samplesteps;
	protected Double1DVector capacity;		// [veh]
	protected boolean isdone;
	protected int stepinitial;

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario) {
		if(myScenario==null)
			return;
		this.myScenario = myScenario;
		myLink = myScenario.getLinkWithId(getLinkId());
		dtinseconds = getDt().floatValue();					// assume given in seconds
		samplesteps = SiriusMath.round(dtinseconds/myScenario.getSimDtInSeconds());
		isdone = false;
		
		// read capacity and convert to vehicle units
		String str = getContent();
		if(!str.isEmpty()){
			capacity = new Double1DVector(getContent(),",");	// true=> reshape to vector along k, define length
			capacity.multiplyscalar(myScenario.getSimDtInHours()*myLink.get_Lanes());
		}
			
	}
	
	protected void validate() {
		
		if(capacity==null)
			return;
		
		if(capacity.isEmpty())
			return;
		
		if(myLink==null){
			SiriusErrorLog.addWarning("Unknown link id=" + getLinkId() + " in capacity profile.");
			return;
		}
		
		// check dtinseconds
		if( dtinseconds<=0 )
			SiriusErrorLog.addError("Non-positive time step in capacity profile for link id=" + getLinkId());

		if(!SiriusMath.isintegermultipleof(dtinseconds,myScenario.getSimDtInSeconds()))
			SiriusErrorLog.addError("Time step for capacity profile of link id=" + getLinkId() + " is not a multiple of simulation time step.");
		
		// check non-negative
		if(capacity.hasNaN())
			SiriusErrorLog.addError("Capacity profile for link id=" +getLinkId()+ " has illegal values.");

	}

	protected void reset() {
		isdone = false;
		
		// read start time, convert to stepinitial
		double starttime;
		if( getStartTime()!=null)
			starttime = getStartTime().floatValue();
		else
			starttime = 0f;

		stepinitial = (int) Math.round((starttime-myScenario.getTimeStart())/myScenario.getSimDtInSeconds());

	}
	
	protected void update() {
		if(myLink==null)
			return;
		if(capacity==null)
			return;
		if(isdone || capacity.isEmpty())
			return;
		if(myScenario.clock.istimetosample(samplesteps,stepinitial)){
			int n = capacity.getLength()-1;
			int step = SiriusMath.floor((myScenario.clock.getCurrentstep()-stepinitial)/samplesteps);
			if(step<n)
				myLink.setCapacityFromVeh(capacity.get(step));
			if(step>=n && !isdone){
				myLink.setCapacityFromVeh(capacity.get(n));
				isdone = true;
			}
		}
	}

}
