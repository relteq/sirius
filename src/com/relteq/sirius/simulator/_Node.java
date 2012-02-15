/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

public final class _Node extends com.relteq.sirius.jaxb.Node {

	protected static enum Type	{NULL, simple,
								       onramp,
								       offramp,
								       signalized_intersection,
								       unsignalized_intersection };
		   
	private _Node.Type myType;

	// network references
	private _Link [] output_link;
	private _Link [] input_link;
	
	private Double3DMatrix sampledSRprofile;
	private Double3DMatrix splitratio;
	private boolean istrivialsplit;
	private boolean hasSRprofile;

	private int nIn;
	private int nOut;

    // contoller
	private boolean hascontroller;
	private boolean controlleron;
	
	// split event
	private boolean hasactivesplitevent;	// split ratios set by events take precedence over
											// controller split ratios
    // used in update()
	private Double [][] inDemand;		// [nIn][nTypes]
	private double [] outSupply;		// [nOut]
	private double [] outDemandKnown;	// [nOut]
	private double [] dsratio;			// [nOut]
	private Double [][] outFlow; 		// [nOut][nTypes]
	private boolean [][] iscontributor;	// [nIn][nOut]
	private ArrayList<Integer> unknownind = new ArrayList<Integer>();		// [unknown splits]
	private ArrayList<Double> unknown_dsratio = new ArrayList<Double>();	// [unknown splits]	
	private ArrayList<Integer> minind_to_nOut= new ArrayList<Integer>();	// [min unknown splits]
	private ArrayList<Integer> minind_to_unknown= new ArrayList<Integer>();	// [min unknown splits]
	private ArrayList<Double> sendtoeach = new ArrayList<Double>();			// [min unknown splits]
    
	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////

	public _Node.Type getMyType() {
		return myType;
	}
    
    public _Link[] getOutput_link() {
		return output_link;
	}

	public _Link[] getInput_link() {
		return input_link;
	}

	public int getInputLinkIndex(String id){
		for(int i=0;i<getnIn();i++){
			if(input_link[i].getId().equals(id))
				return i;
		}
		return -1;
	}
	
	public int getOutputLinkIndex(String id){
		for(int i=0;i<getnOut();i++){
			if(output_link[i].getId().equals(id))
				return i;
		}
		return -1;
	}
	
	public int getnIn() {
		return nIn;
	}

	public int getnOut() {
		return nOut;
	}
    
	public boolean hasController() {
		return hascontroller;
	}
		
	/////////////////////////////////////////////////////////////////////
	// protected
	/////////////////////////////////////////////////////////////////////
	
	protected boolean registerController(){
		if(hascontroller)		// used to detect multiple controllers
			return false;
		else{
			hascontroller = true;
			controlleron = true;
			return true;
		}
	}
	
    protected void setSampledSRProfile(Double3DMatrix s){
    	sampledSRprofile = s;
    }

	protected void setHasSRprofile(boolean hasSRprofile) {
		if(!istrivialsplit){
			this.hasSRprofile = hasSRprofile;
			this.sampledSRprofile = new Double3DMatrix(nIn,nOut,Utils.numVehicleTypes,0d);
			_SplitRatioProfile.normalizeSplitRatioMatrix(this.sampledSRprofile);	// GCG REMOVE THIS AFTER CHANGING 0->NaN
		}
	}

	protected void setControllerSplitratio(Double3DMatrix x) {
		// to be used only by an active controller,
		// only if there is no split event
		if(controlleron && !hasactivesplitevent){
			setSplitratio(x);
		}
	}

	protected void setEventSplitratio(Double3DMatrix x) {
		setSplitratio(x);
		hasactivesplitevent = true;
	}

	protected void removeEventSplitratio() {
		if(hasactivesplitevent){
			resetSplitRatio();
			hasactivesplitevent = false;
		}
	}
	
	protected void setControllerOn(boolean controlleron) {
		if(hascontroller){
			this.controlleron = controlleron;
			if(!controlleron)
				resetSplitRatio();
		}
	}

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
    
	protected void populate(_Network myNetwork) {
    	// Note: It is assumed that this comes *before* SplitRatioProfile.populate
		
    	try {
			myType = _Node.Type.valueOf(getType());
		} catch (IllegalArgumentException e) {
			myType = null;
			return;
		}
    	    	
//    	if(myType==Types.Node.T)
//    		return;
    	
		nOut = 0;
		if(getOutputs()!=null){
			nOut = getOutputs().getOutput().size();
			output_link = new _Link[nOut];
			for(int i=0;i<nOut;i++){
				com.relteq.sirius.jaxb.Output output = getOutputs().getOutput().get(i);
				output_link[i] = myNetwork.getLinkWithId(output.getLinkId());
			}
		}

		nIn = 0;
		if(getInputs()!=null){
			nIn = getInputs().getInput().size();
			input_link = new _Link[nIn];
			for(int i=0;i<nIn;i++){
				com.relteq.sirius.jaxb.Input input = getInputs().getInput().get(i);
				input_link[i] = myNetwork.getLinkWithId(input.getLinkId());
			}
		}
		
		inDemand = new Double[nIn][Utils.numVehicleTypes];
		outSupply = new double[nOut];
		outDemandKnown = new double[nOut];
		dsratio = new double[nOut];
		iscontributor = new boolean[nIn][nOut];
		outFlow = new Double[nOut][Utils.numVehicleTypes];
		istrivialsplit = nOut==1;
		hasSRprofile = false;
		sampledSRprofile = null;
		
		resetSplitRatio();
		
		hascontroller = false;
		controlleron = false;
		hasactivesplitevent = false;
	}
    
	protected boolean validate() {
		
//		if(myType==Types.Node.T)
//			return true;
		
		if(output_link!=null)
			for(_Link link : output_link){
				if(link==null){
					System.out.println("Incorrect output link id in node " + getId());
					return false;
				}
			}

		if(input_link!=null)
			for(_Link link : input_link){
				if(link==null){
					System.out.println("Incorrect input link id in node " + getId());
					return false;
				}
			}
		
//		if(myType!=Types.Node.T && nIn==0){
//			System.out.println("No inputs into node " + getId());
//			return false;
//		}
//
//		if(myType!=Types.Node.T && nOut==0){
//			System.out.println("No outputs from node " + getId());
//			return false;
//		}
		
		return true;
	}

	protected void update() {
		
//        if(myType==Types.Node.T)
//            return;

        int i,j,k;        
        
        // collect input demands and output supplies ...................
		for(i=0;i<nIn;i++)
			inDemand[i] = input_link[i].getOutflowDemand();
		
		for(j=0;j<nOut;j++)
			outSupply[j] = output_link[j].getSpaceSupply();
		
		// solve unknown split ratios if they are non-trivial ..............
		if(!istrivialsplit){	

	        // Take current split ratio from the profile if the node is
			// not actively controlled. Otherwise the mat has already been 
			// set by the controller.
			if(hasSRprofile && !controlleron && !hasactivesplitevent)
				splitratio.copydata(sampledSRprofile);
			
	        // compute known output demands ................................
	        for(j=0;j<nOut;j++){
	        	outDemandKnown[j] = 0f;
	        	for(i=0;i<nIn;i++)
	        		for(k=0;k<Utils.numVehicleTypes;k++)
	        			if(!splitratio.get(i,j,k).isNaN())
	        				outDemandKnown[j] += splitratio.get(i,j,k) * inDemand[i][k];
	        }
	        
	        // compute and sort output demand/supply ratio .................
	        for(j=0;j<nOut;j++)
	        	dsratio[j] = outDemandKnown[j] / outSupply[j];
	                
	        // fill in unassigned split ratios .............................
	        resolveUnassignedSplits_A();
		}
		
        // compute node flows ..........................................
        computeLinkFlows();
        
        // assign flow to input links ..................................
        for(i=0;i<nIn;i++)
            input_link[i].setOutflow(inDemand[i]);
        
        // assign flow to output links .................................
        for (j=0;j<nOut;j++)
            output_link[j].setInflow(outFlow[j]);

	}

	protected void reset() {		
	}

	/////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////
	
	private void computeLinkFlows(){

        // There should be no unknown splits by now ....................
        //if(any(any(any(SR<0))) || any(any(any(isnan(SR))))  )
        //    error('!!!')
        
    	int i,j,k;

        // input i contributes to output j .............................
    	for(i=0;i<splitratio.getnIn();i++)
        	for(j=0;j<splitratio.getnOut();j++)
        		iscontributor[i][j] = splitratio.getSumOverTypes(i,j)>0;
	
        double [] applyratio = new double[nIn];

        for(i=0;i<nIn;i++)
        	applyratio[i] = Double.NEGATIVE_INFINITY;
        
        for(j=0;j<nOut;j++){
        	
        	// re-compute known output demands .........................
			outDemandKnown[j] = 0f;
            for(i=0;i<nIn;i++)
            	for(k=0;k<Utils.numVehicleTypes;k++)
            		outDemandKnown[j] += inDemand[i][k]*splitratio.get(i,j,k);
            
            // compute and sort output demand/supply ratio .............
            dsratio[j] = Math.max( outDemandKnown[j] / outSupply[j] , 1f );
            
            // reflect ratios back on inputs
            for(i=0;i<nIn;i++)
            	if(iscontributor[i][j])
            		applyratio[i] = Math.max(dsratio[j],applyratio[i]);
            	
        }

        // scale down input demands
        for(i=0;i<nIn;i++)
            for(k=0;k<Utils.numVehicleTypes;k++)
                inDemand[i][k] /= applyratio[i];

        // compute out flows ...........................................   
        for(j=0;j<nOut;j++){
        	for(k=0;k<Utils.numVehicleTypes;k++){
        		outFlow[j][k] = 0d;
            	for(i=0;i<nIn;i++){
            		outFlow[j][k] += inDemand[i][k]*splitratio.get(i,j,k);
            	}
        	}
        }
    }

    private void resolveUnassignedSplits_A(){
    	
    	int i,j,k;
    	int numunknown;	
    	double dsmax, dsmin;
    	double [] sr_new = new double[nOut];
    	double remainingSplit;
    	double num;
    	
    	for(i=0;i<nIn;i++){
	        for(k=0;k<Utils.numVehicleTypes;k++){
	            
	        	// number of outputs with unknown split ratio
	        	numunknown = 0;
	        	for(j=0;j<nOut;j++)
	        		if(splitratio.get(i,j,k).isNaN())
	        			numunknown++;
	        	
	            if(numunknown==0)
	                continue;
	            
	        	// initialize sr_new, save location of unknown entries, compute remaining split
	        	unknownind.clear();
	        	unknown_dsratio.clear();
	        	remainingSplit = 1f;
	        	for(j=0;j<nOut;j++){
	        		Double sr = splitratio.get(i,j,k);
	        		if(sr.isNaN()){
	        			sr_new[j] = 0f;
	        			unknownind.add(j);						// index to unknown output
	        			unknown_dsratio.add(dsratio[j]);		// dsratio for unknown output
	        		}
	        		else {
	        			sr_new[j] = sr;
	        			remainingSplit -= sr;
	        		}
	        	}
	            
	        	// distribute remaining split until there is none left or 
	        	// all dsratios are equalized
	            while(remainingSplit>0){
	                
	            	// find most and least "congested" destinations
	            	dsmax = Double.NEGATIVE_INFINITY;
	            	dsmin = Double.POSITIVE_INFINITY;
	            	for(Double r : unknown_dsratio){
	            		dsmax = Math.max(dsmax,r);
	            		dsmin = Math.min(dsmax,r);
	            	}
	                
	                if(dsmax==dsmin)
	                    break;
	                    
                	// indices of smallest dsratio
                	minind_to_nOut.clear();
                	minind_to_unknown.clear();
	            	sendtoeach.clear();		// flow needed to bring each dsmin up to dsmax
	            	double sumsendtoeach = 0f;
	            	for(int z=1;z<numunknown;z++)
	            		if(unknown_dsratio.get(z)==dsmin){
	            			int index = unknownind.get(z);
	            			minind_to_nOut.add(index);
	            			minind_to_unknown.add(z);
	            			num = dsmax*outSupply[index] - outDemandKnown[index];
	            			sendtoeach.add(num);		            			
	            			sumsendtoeach += num;
	            		}

                    // total that can be sent
	            	double sendtotal = Math.min(inDemand[i][k]*remainingSplit , sumsendtoeach );
                    
                    // scale down sendtoeach
                    // store split ratio
                    for(int z=0;z<minind_to_nOut.size();z++){
                    	double send = sendtoeach.get(z)*sendtotal/sumsendtoeach;  
                    	double addsplit = send/inDemand[i][k];
                    	int ind_nOut = minind_to_nOut.get(z);
                    	int ind_unknown = minind_to_unknown.get(z);
                    	sr_new[ind_nOut] += addsplit;
                    	remainingSplit -= addsplit;
	                    outDemandKnown[ind_nOut] += send;
	                    unknown_dsratio.set( ind_unknown , outDemandKnown[ind_nOut]/outSupply[ind_nOut] );
                    }	                    
	                
	            }
	            
	            // distribute remaining splits proportionally to supplies
	            if(remainingSplit>0){
	            	/*
	            	double totalcapacity = 0f;
	            	double splitforeach;
                    for(Integer jj : unknownind)
                    	totalcapacity += output_link[jj].capacity;
                    for(Integer jj : unknownind){
                    	splitforeach = remainingSplit*output_link[jj].capacity/totalcapacity;
                    	sr_new[jj] += splitforeach;
                    	outDemandKnown[jj] += inDemand[i][k]*splitforeach;
                    }
                    remainingSplit = 0;
                    */
	            	double totalsupply = 0f;
	            	double splitforeach;
                    for(Integer jj : unknownind)
                    	totalsupply += outSupply[jj];
                    for(Integer jj : unknownind){
                    	splitforeach = remainingSplit*outSupply[jj]/totalsupply;
                    	sr_new[jj] += splitforeach;
                    	outDemandKnown[jj] += inDemand[i][k]*splitforeach;
                    }
                    remainingSplit = 0;
	            }
	            
	            // copy to SR
	            for(j=0;j<nOut;j++)
	            	splitratio.set(i,j,k,sr_new[j]);
	        }
    	}
    
    }

    /*
    private Float3DMatrix resolveUnassignedSplits_B(SR){

        // GCG: take care of case single class
        
        for(i=0;i<nIn;i++){
            for(k=0;k<nTypes;k++){
                
                sr_j = SR(i,:,k);                            // 1 x nOut
                
                if(~any(sr_j<0))
                    continue;
                
                sr_pos = sr_j;
                sr_pos(sr_pos<0) = 0;
                phi = find(sr_j<0);             // possible destinations
                
                phi_dsratio = dsratio(phi);
                
                // classes are sorted in order of increasing congestion
                dsratio_class = sort(unique(phi_dsratio),2,'ascend');
                
                // class z has members phi(isinclass(z,:))
                numclasses = length(dsratio_class);
                isinclass = false(numclasses,length(phi));
                for(z=0;z<numclasses;z++)
                    isinclass(z,phi_dsratio==dsratio_class(z)) = true;
                
                // for each class compute the demand needed to get to the next class
                Delta = zeros(numclasses-1,1);
                for(z=0;z<numclasses-1;z++){
                    myphi = phi(isinclass(z,:));
                    Delta(z) = sum( outSupply(myphi)*dsratio_class(z+1) - outDemandKnown(myphi) );
                }
                
                // flow needed to raise classes
                if(numclasses==1)
                    flowtolevel = inf;
                else
                    flowtolevel = [cumsum(Delta.*(1:numclasses-1)) inf];    // 1xnumclasses
                
                // numclassups = n then remainingDemand is sufficient to unite classes 1..n, but not {1..n} and n+1
                remainingSplit = 1-sum(sr_pos);
                remainingDemand = inDemand(i,k)*remainingSplit;
                numclassmerge = find(remainingDemand<flowtolevel,1,'first');
                
                // flowtolevel(numclassmerge-1) is flow used to
                // level off classes. Distribute the remainder
                // equally among unassigned outputs
                if(numclassmerge>1)
                    levelflow = flowtolevel(numclassmerge-1);
                else
                    levelflow = 0;
                
                leftoverperclass = (remainingDemand-levelflow)/numclassmerge;
                
                for(z=0;z<numclasses;z++){
                    
                    flowtoclass = 0;
                    if(numclassmerge>z)
                        flowtoclass = sum(Delta(z:end));
                    
                    if(numclassmerge>=z)
                        flowtoclass = flowtoclass + leftoverperclass;
                    
                    // distribute among class members
                    myphi = phi(isinclass(z,:));
                    phishare = outSupply(myphi)/sum(outSupply(myphi));
                    flowtophi = flowtoclass*phishare;
                    
                    // save in SR matrix
                    if(inDemand(i,k)>0)
                        SR(i,myphi,k) = flowtophi/inDemand(i,k);
                    else{
                        SR(i,myphi,k) = 0;
                        s = sum(SR(i,:,k));
                        if(s>0)
                            SR(i,:,k) = SR(i,:,k)/s;
                        else
                            SR(i,1,k) = 1;
                    }
                }
            }
        }
    }
*/
    
    /*
    private Float3DMatrix resolveUnassignedSplits_C(SR){
    	for(int i=0;i<nIn;i++){
	        for(int k=0;k<nTypes;k++){
	            sr_j = SR(i,:,k);
	            if(~any(sr_j<0))
	                continue;
	            phi = find(sr_j<0);
	            remainingSplit = 1-sum(sr_j(sr_j>=0));
	            phi_dsratio = dsratio(phi);
	            SR(i,phi,k) = phi_dsratio/sum(phi_dsratio)*remainingSplit;
	        }
    	}
    }    
    */
    
    private void resetSplitRatio(){

		//splitratio = new Float3DMatrix(nIn,nOut,Utils.numVehicleTypes,1f/((double)nOut));
		
		//////
		splitratio = new Double3DMatrix(nIn,nOut,Utils.numVehicleTypes,0d);
		_SplitRatioProfile.normalizeSplitRatioMatrix(splitratio);
		//////
    }
    
	private void setSplitratio(Double3DMatrix x) {
		splitratio.copydata(x);
		_SplitRatioProfile.normalizeSplitRatioMatrix(splitratio);
	}

	
}
