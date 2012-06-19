/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class SplitRatioProfile extends com.relteq.sirius.jaxb.SplitratioProfile {

	protected Scenario myScenario;
	protected Node myNode;
	
	protected double dtinseconds;				// not really necessary
	protected int samplesteps;
	
	protected Double2DMatrix [][] profile;		// profile[i][j] is the 2D split matrix for
												// input link i, output link j. The first dimension 
												// of the Double2DMatrix is time, the second in vehicle type.
	
	protected Double3DMatrix currentSplitRatio; 	// current split ratio matrix with dimension [inlink x outlink x vehicle type]
	
	protected int laststep;
	
	protected boolean isdone; 
	protected int stepinitial;

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario) {

		if(getSplitratio().isEmpty())
			return;
		
		if(myScenario==null)
			return;
		
		this.myScenario = myScenario;
		
		// required
		myNode = myScenario.getNodeWithId(getNodeId());

		isdone = false;
		
		if(myNode==null)
			return;

		// optional dt
		if(getDt()!=null){
			dtinseconds = getDt().floatValue();					// assume given in seconds
			samplesteps = SiriusMath.round(dtinseconds/myScenario.getSimDtInSeconds());
		}
		else{ 	// only allow if it contains only one fd
			if(getSplitratio().size()==1){
				dtinseconds = Double.POSITIVE_INFINITY;
				samplesteps = Integer.MAX_VALUE;
			}
			else{
				dtinseconds = -1.0;		// this triggers the validation error
				samplesteps = -1;
				return;
			}
		}
		
		profile = new Double2DMatrix[myNode.nIn][myNode.nOut];
		int in_index,out_index;
		laststep = 0;
		for(com.relteq.sirius.jaxb.Splitratio sr : getSplitratio()){
			in_index = myNode.getInputLinkIndex(sr.getLinkIn());
			out_index = myNode.getOutputLinkIndex(sr.getLinkOut());
			if(in_index<0 || out_index<0)
				continue; 
			profile[in_index][out_index] = new Double2DMatrix(sr.getContent());
			if(!profile[in_index][out_index].isEmpty())
				laststep = Math.max(laststep,profile[in_index][out_index].getnTime());
		}
		
		currentSplitRatio = new Double3DMatrix(myNode.nIn,myNode.nOut,myScenario.getNumVehicleTypes(),Double.NaN);
		
		// inform the node
		myNode.setHasSRprofile(true);
		
	}

	protected void reset() {
		
		// read start time, convert to stepinitial
		double starttime;
		if( getStartTime()!=null)
			starttime = getStartTime().floatValue();	// assume given in seconds
		else
			starttime = 0f;
		
		stepinitial = SiriusMath.round((starttime-myScenario.getTimeStart())/myScenario.getSimDtInSeconds());
	}

	protected boolean validate() {

		if(getSplitratio().isEmpty())
			return true;
		
		if(myNode==null){
			SiriusErrorLog.addErrorMessage("Bad node id in split ratio profile: " + getNodeId());
			return false;
		}
		
		// check link ids
		int in_index, out_index;
		for(com.relteq.sirius.jaxb.Splitratio sr : getSplitratio()){
			in_index = myNode.getInputLinkIndex(sr.getLinkIn());
			out_index = myNode.getOutputLinkIndex(sr.getLinkOut());
			if(in_index<0 || out_index<0){
				SiriusErrorLog.addErrorMessage("Bad link id in split ratio profile: " + getNodeId());
				return false;
			}
		}

		// check dtinhours
		if( dtinseconds<=0 ){
			SiriusErrorLog.addErrorMessage("Split ratio profile dt should be positive: " + getNodeId());
			return false;	
		}

		if(!SiriusMath.isintegermultipleof(dtinseconds,myScenario.getSimDtInSeconds())){
			SiriusErrorLog.addErrorMessage("Split ratio dt should be multiple of sim dt: " + getNodeId());
			return false;	
		}
		
		// check split ratio dimensions and values
		for(in_index=0;in_index<profile.length;in_index++){
			for(out_index=0;out_index<profile[in_index].length;out_index++){
				if(profile[in_index][out_index].getnVTypes()!=myScenario.getNumVehicleTypes()){
					SiriusErrorLog.addErrorMessage("Split ratio profile does not contain values for all vehicle types: " + getNodeId());
					return false;
				}
			}
		}
		
		return true;
	}

	protected void update() {
		if(profile==null)
			return;
		if(isdone)
			return;
		if(myScenario.clock.istimetosample(samplesteps,stepinitial)){
			int step = SiriusMath.floor((myScenario.clock.getCurrentstep()-stepinitial)/samplesteps);
			step = Math.max(0,step);
			currentSplitRatio = sampleAtTimeStep( Math.min( step , laststep-1) );
			myNode.normalizeSplitRatioMatrix(currentSplitRatio);
			myNode.setSampledSRProfile(currentSplitRatio);
			isdone = step>=laststep-1;
		}		
	}

	/////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////
	
	// for time sample k, returns a 3D matrix with dimensions inlink x outlink x vehicle type
	private Double3DMatrix sampleAtTimeStep(int k){
		if(myNode==null)
			return null;
		Double3DMatrix X = new Double3DMatrix(myNode.nIn,myNode.nOut,
				myScenario.getNumVehicleTypes(),Double.NaN);	// initialize all unknown
		
		// get vehicle type order from SplitRatioProfileSet
		Integer [] vehicletypeindex = null;
		if(myScenario.getSplitRatioProfileSet()!=null)
			vehicletypeindex = ((SplitRatioProfileSet)myScenario.getSplitRatioProfileSet()).vehicletypeindex;
		
		int i,j,lastk;
		for(i=0;i<myNode.nIn;i++){
			for(j=0;j<myNode.nOut;j++){
				if(profile[i][j]==null)						// nan if not defined
					continue;
				if(profile[i][j].isEmpty())					// nan if no data
					continue;
				lastk = Math.min(k,profile[i][j].getnTime()-1);	// hold last value
				X.setAllVehicleTypes(i,j,profile[i][j].sampleAtTime(lastk,vehicletypeindex));
			}
		}
		return X;
	}

}
