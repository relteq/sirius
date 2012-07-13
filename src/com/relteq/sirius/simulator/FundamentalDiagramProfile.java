/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

final class FundamentalDiagramProfile extends com.relteq.sirius.jaxb.FundamentalDiagramProfile {

	protected Scenario myScenario;
	protected Link myLink;
	protected double dtinseconds;			// not really necessary
	protected int samplesteps;
	protected ArrayList<FundamentalDiagram> FD;
	protected boolean isdone; 
	protected int stepinitial;

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	// scale present and future fundamental diagrams to new lane value
	protected void set_Lanes(double newlanes){
		if(newlanes<=0 || FD.isEmpty())
			return;
		int step = SiriusMath.floor((myScenario.clock.getCurrentstep()-stepinitial)/samplesteps);
		step = Math.max(0,step);
		for(int i=step;i<FD.size();i++){
			FD.get(i).setLanes(newlanes);
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	protected void populate(Scenario myScenario) throws SiriusException {
		
		this.myScenario = myScenario;
		isdone = false;
		
		// required
		myLink = myScenario.getLinkWithId(getLinkId());
		
		if(myLink==null)
			return;
		
		myLink.setFundamentalDiagramProfile(this);
		
		// optional dt
		if(getDt()!=null){
			dtinseconds = getDt().floatValue();					// assume given in seconds
			samplesteps = SiriusMath.round(dtinseconds/myScenario.getSimDtInSeconds());
		}
		else{ 	// only allow if it contains only one fd
			if(getFundamentalDiagram().size()==1){
				dtinseconds = Double.POSITIVE_INFINITY;
				samplesteps = Integer.MAX_VALUE;
			}
			else{
				dtinseconds = -1.0;		// this triggers the validation error
				samplesteps = -1;
				return;
			}
		}
		
		//  read fundamental diagrams
		FD = new ArrayList<FundamentalDiagram>();
		for(com.relteq.sirius.jaxb.FundamentalDiagram fd : getFundamentalDiagram()){
			FundamentalDiagram _fd = new FundamentalDiagram(myLink);	// create empty fd
	        _fd.settoDefault();					// set to default
			_fd.copyfrom(fd);					// copy and normalize
			FD.add(_fd);
		}
		
	}
	
	protected boolean validate() {
		
		if(myLink==null){
			SiriusErrorLog.addError("Bad link id in fundamental diagram: " + getLinkId());
			return false;
		}
		
		// check dtinseconds
		if( dtinseconds<=0 ){
			SiriusErrorLog.addError("Non-positive dt in fundamental diagram profile for link id=" + getLinkId());
			return false;	
		}
		
		if(!SiriusMath.isintegermultipleof(dtinseconds,myScenario.getSimDtInSeconds())){
			SiriusErrorLog.addError("Time step in fundamental diagram profile for link id=" + getLinkId() + " is not a multiple of simulation time step.");
			return false;	
		}
		
		// check fundamental diagrams
		for(FundamentalDiagram fd : FD)
			if(!fd.validate())
				return false;

		return true;
	}

	protected void reset() throws SiriusException {
		isdone = false;
		
		// read start time, convert to stepinitial
		double profile_starttime;	// [sec]
		if( getStartTime()!=null){
			profile_starttime = getStartTime().floatValue();
		}
		else
			profile_starttime = 0f;

		stepinitial = SiriusMath.round((profile_starttime-myScenario.getTimeStart())/myScenario.getSimDtInSeconds());
		
		if(FD!=null)
			for(FundamentalDiagram fd : FD)
				fd.reset(myScenario.uncertaintyModel);
		
		// assign the fundamental diagram to the link
		//update();	
		
	}

	protected void update() throws SiriusException {
		if(myLink==null)
			return;
		if(isdone || FD.isEmpty())
			return;
		if(myScenario.clock.istimetosample(samplesteps,stepinitial)){
			int n = FD.size()-1;
			int step = SiriusMath.floor((myScenario.clock.getCurrentstep()-stepinitial)/samplesteps);
			step = Math.max(0,step);
			if(step<n)
				myLink.setFundamentalDiagramFromProfile( FD.get(step) );
			if(step>=n && !isdone){
				myLink.setFundamentalDiagramFromProfile( FD.get(n) );
				isdone = true;
			}
		}
	}
	
}
