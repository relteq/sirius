/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

import com.relteq.sirius.jaxb.FundamentalDiagram;

final class _FundamentalDiagramProfile extends com.relteq.sirius.jaxb.FundamentalDiagramProfile {

	protected _Link myLink;
	protected double dtinseconds;			// not really necessary
	protected int samplesteps;
	protected ArrayList<_FundamentalDiagram> FD;
	protected boolean isdone; 
	protected int stepinitial;

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	// scale present and future fundamental diagrams to new lane value
	protected void set_Lanes(double newlanes){
		if(newlanes<=0 || isdone || FD.isEmpty())
			return;
		int step = SiriusMath.floor((Global.clock.getCurrentstep()-stepinitial)/samplesteps);
		step = Math.max(0,step);
		for(int i=step;i<FD.size();i++){
			FD.get(i).setLanes(newlanes);
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	protected void populate() {
		
		isdone = false;
		
		// required
		myLink = API.getLinkWithCompositeId(getNetworkId(), getLinkId());
		myLink.setFundamentalDiagramProfile(this);
		
		// optional dt
		if(getDt()!=null){
			dtinseconds = getDt().floatValue();					// assume given in seconds
			samplesteps = SiriusMath.round(dtinseconds/API.getSimDtInSeconds());
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
		FD = new ArrayList<_FundamentalDiagram>();
		for(FundamentalDiagram fd : getFundamentalDiagram()){
			_FundamentalDiagram _fd = new _FundamentalDiagram(myLink);	// create empty fd
	        _fd.settoDefault();					// set to default
			_fd.copyfrom(fd);					// copy and normalize
			FD.add(_fd);
		}
	
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

		stepinitial = SiriusMath.round((starttime-API.getTimeStart())/API.getSimDtInSeconds());
		
		// update so that the link gets the first value of the parameters.
		// this is need so that the initial density profile can validate. 
		update();	
		
		
	}
	
	protected boolean validate() {
		
		if(myLink==null){
			System.out.println("Bad link id in fundamental diagram: " + getLinkId());
			return false;
		}
		
		// check dtinseconds
		if( dtinseconds<=0 ){
			System.out.println("Demand profile dt should be positive: " + getLinkId());
			return false;	
		}
		
		if(!SiriusMath.isintegermultipleof(dtinseconds,API.getSimDtInSeconds())){
			System.out.println("Demand dt should be multiple of sim dt: " + getLinkId());
			return false;	
		}
		
		// check fundamental diagrams
		for(_FundamentalDiagram fd : FD)
			if(!fd.validate())
				return false;

		return true;
	}

	protected void reset() {
		isdone = false;
		
		for(_FundamentalDiagram fd : FD)
			fd.reset();
		
		// assign the fundamental diagram to the link
		update();	
		
	}

	protected void update() {
		if(isdone || FD.isEmpty())
			return;
		if(Global.clock.istimetosample(samplesteps,stepinitial)){
			int n = FD.size()-1;
			int step = SiriusMath.floor((Global.clock.getCurrentstep()-stepinitial)/samplesteps);
			step = Math.max(0,step);
			if(step<n)
				myLink.setProfileFundamentalDiagram( FD.get(step) );
			if(step>=n && !isdone){
				myLink.setProfileFundamentalDiagram( FD.get(n) );
				isdone = true;
			}
		}
	}
	
}
