/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.Splitratio;

public class _SplitRatioProfile extends com.relteq.sirius.jaxb.SplitratioProfile {

	public _Node myNode;
	private double dtinseconds;				// not really necessary
	private int samplesteps;
	
	private Double2DMatrix [][] profile;		// profile[i][j] is the 2D split matrix for
												// input link i, output link j. The first dimension 
												// of the Double2DMatrix is time, the second in vehicle type.
	
	private Double3DMatrix currentSplitRatio; 	// current split ratio matrix with dimension [inlink x outlink x vehicle type]
	
	private int laststep;
	
	private boolean isdone; 
	private int stepinitial;

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate() {

		isdone = false;
		
		// required
		myNode = Utils.getNodeWithCompositeId(getNetworkId(),getNodeId());

		if(myNode==null)
			return;

		// optional dt
		if(getDt()!=null){
			dtinseconds = getDt().floatValue();					// assume given in seconds
			samplesteps = Utils.round(dtinseconds/Utils.simdtinseconds);
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
		
		// read start time, convert to stepinitial
		double starttime;
		if( getStartTime()!=null){
			starttime = getStartTime().floatValue();	// assume given in seconds
//			if(starttime>0 && starttime<=24){
//				System.out.println("Warning: Initial time given in hours. Changing to seconds.");
//				starttime *= 3600f;
//			}
		}
		else
			starttime = 0f;

		stepinitial = Utils.round((starttime-Utils.timestart)/Utils.simdtinseconds);
		
		profile = new Double2DMatrix[myNode.getnIn()][myNode.getnOut()];
		int in_index,out_index;
		laststep = 0;
		for(Splitratio sr : getSplitratio()){
			in_index = myNode.getInputLinkIndex(sr.getLinkIn());
			out_index = myNode.getOutputLinkIndex(sr.getLinkOut());
			if(in_index<0 || out_index<0)
				continue; 
			profile[in_index][out_index] = new Double2DMatrix(sr.getContent());
			if(!profile[in_index][out_index].isEmpty())
				laststep = Math.max(laststep,profile[in_index][out_index].getnTime());
		}
		
		currentSplitRatio = new Double3DMatrix(myNode.getnIn(),myNode.getnOut(),Utils.numVehicleTypes,Double.NaN);
		
		// inform the node
		myNode.setHasSRprofile(true);
		
	}

	protected void reset() {
	}

	protected boolean validate() {
		
		if(myNode==null){
			Utils.addErrorMessage("Bad node id in split ratio profile: " + getNodeId());
			return false;
		}
		
		// check link ids
		int in_index, out_index;
		for(Splitratio sr : getSplitratio()){
			in_index = myNode.getInputLinkIndex(sr.getLinkIn());
			out_index = myNode.getOutputLinkIndex(sr.getLinkOut());
			if(in_index<0 || out_index<0){
				Utils.addErrorMessage("Bad link id in split ratio profile: " + getNodeId());
				return false;
			}
		}

		// check dtinhours
		if( dtinseconds<=0 ){
			System.out.println("Split ratio profile dt should be positive: " + getNodeId());
			return false;	
		}

		if(!Utils.isintegermultipleof(dtinseconds,Utils.simdtinseconds)){
			System.out.println("Split ratio dt should be multiple of sim dt: " + getNodeId());
			return false;	
		}
		
		// check split ratio dimensions and values
		for(in_index=0;in_index<profile.length;in_index++){
			for(out_index=0;out_index<profile[in_index].length;out_index++){
				if(profile[in_index][out_index].getnVTypes()!=Utils.numVehicleTypes){
					System.out.println("Split ratio profile does not contain values for all vehicle types: " + getNodeId());
					return false;
				}
			}
		}
		
		return true;
	}

	protected void update() {
		if(isdone)
			return;
		if(Utils.clock.istimetosample(samplesteps,stepinitial)){
			int step = Utils.floor((Utils.clock.getCurrentstep()-stepinitial)/samplesteps);
			step = Math.max(0,step);
			currentSplitRatio = sampleAtTimeStep( Math.min( step , laststep-1) );
			
System.out.println(Utils.simdtinseconds + " node=" + myNode.getId() + " " + currentSplitRatio.toString());
			
			
			normalizeSplitRatioMatrix(currentSplitRatio);
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
		Double3DMatrix X = new Double3DMatrix(myNode.getnIn(),myNode.getnOut(),
				                              Utils.numVehicleTypes,Double.NaN);	// initialize all unknown
		
		// get vehicle type order from SplitRatioProfileSet
		Integer [] vehicletypeindex = null;
		if(Utils.theScenario.getSplitRatioProfileSet()!=null)
			vehicletypeindex = ((_SplitRatioProfileSet)Utils.theScenario.getSplitRatioProfileSet()).vehicletypeindex;
		
		int i,j,lastk;
		for(i=0;i<myNode.getnIn();i++){
			for(j=0;j<myNode.getnOut();j++){
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
	
	/////////////////////////////////////////////////////////////////////
	// static methods
	/////////////////////////////////////////////////////////////////////
	
	public static boolean validateSplitRatioMatrix(Double3DMatrix X,_Node myNode){

		int i,j,k;
		Double value;
		
		// dimension
		if(X.getnIn()!=myNode.getnIn() || X.getnOut()!=myNode.getnOut() || X.getnVTypes()!=Utils.numVehicleTypes){
			System.out.println("Split ratio for node " + myNode.getId() + " has incorrect dimension");
			return false;
		}
		
		// range
		for(i=0;i<X.getnIn();i++){
			for(j=0;j<X.getnOut();j++){
				for(k=0;k<X.getnVTypes();k++){
					value = X.get(i,j,k);
					if( !value.isNaN() && (value>1 || value<0) ){
						System.out.println("Split ratio values must be in [0,1]");
						return false;
					}
				}
			}
		}
		
		return true;
	}

    public static void normalizeSplitRatioMatrix(Double3DMatrix X){

    	int i,j,k;
		boolean hasNaN;
		int countNaN;
		int idxNegative;
		double sum;
    	
    	for(i=0;i<X.getnIn();i++)
    		for(k=0;k<Utils.numVehicleTypes;k++){
				hasNaN = false;
				countNaN = 0;
				idxNegative = -1;
				sum = 0.0f;
				for (j = 0; j < X.getnOut(); j++)
					if (X.get(i,j,k).isNaN()) {
						countNaN++;
						idxNegative = j;
						if (countNaN > 1)
							hasNaN = true;
					}
					else
						sum += X.get(i,j,k);
				
				if (countNaN==1) {
					X.set(i,idxNegative,k,Math.max(0f, (1-sum)));
					sum += X.get(i,idxNegative,k);
				}
				
				if ((!hasNaN) && (sum==0.0)) {	
					X.set(i,0,k,1d);
					//for (j=0; j<n2; j++)			
					//	data[i][j][k] = 1/((double) n2);
					continue;
				}
				
				if ((!hasNaN) && (sum<1.0)) {
					for (j=0;j<X.getnOut();j++)
						X.set(i,j,k,(double) (1/sum) * X.get(i,j,k));
					continue;
				}
				
				if (sum >= 1.0)
					for (j=0; j<X.getnOut(); j++)
						if (X.get(i,j,k).isNaN())
							X.set(i,j,k,0d);
						else
							X.set(i,j,k,(double) (1/sum) * X.get(i,j,k));
    		}
    }
}
