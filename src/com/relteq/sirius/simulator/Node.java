/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

/** Node class.
*
* @author Gabriel Gomes (gomes@path.berkeley.edu)
*/
public final class Node extends com.relteq.sirius.jaxb.Node {
		   
	/** @y.exclude */ 	protected Network myNetwork;

	// network references
	/** @y.exclude */ 	protected Link [] output_link;
	/** @y.exclude */ 	protected Link [] input_link;
	
	/** @y.exclude */ 	protected Double3DMatrix sampledSRprofile;
	/** @y.exclude */ 	protected Double3DMatrix splitratio;
	/** @y.exclude */ 	protected boolean istrivialsplit;
	/** @y.exclude */ 	protected boolean hasSRprofile;
	/** @y.exclude */ 	protected int nIn;
	/** @y.exclude */ 	protected int nOut;
	/** @y.exclude */ 	protected boolean isTerminal;
	
	/** @y.exclude */ 	protected Signal mySignal = null;

    // controller
	/** @y.exclude */ 	protected boolean hascontroller;
	/** @y.exclude */ 	protected boolean controlleron;
	
	// split event
	/** @y.exclude */ 	protected boolean hasactivesplitevent;	// split ratios set by events take precedence over
																// controller split ratios
    // used in update()
	/** @y.exclude */ 	protected Double [][][] inDemand;		// [ensemble][nIn][nTypes]
	/** @y.exclude */ 	protected double [][] outSupply;		// [ensemble][nOut]
	/** @y.exclude */ 	protected double [][] outDemandKnown;	// [ensemble][nOut]
	/** @y.exclude */ 	protected double [][] dsratio;			// [ensemble][nOut]
	/** @y.exclude */ 	protected Double [][][] outFlow; 		// [ensemble][nOut][nTypes]
	/** @y.exclude */ 	protected boolean [][] iscontributor;	// [nIn][nOut]
	/** @y.exclude */ 	protected ArrayList<Integer> unknownind = new ArrayList<Integer>();		// [unknown splits]
	/** @y.exclude */ 	protected ArrayList<Double> unknown_dsratio = new ArrayList<Double>();	// [unknown splits]	
	/** @y.exclude */ 	protected ArrayList<Integer> minind_to_nOut= new ArrayList<Integer>();	// [min unknown splits]
	/** @y.exclude */ 	protected ArrayList<Integer> minind_to_unknown= new ArrayList<Integer>();	// [min unknown splits]
	/** @y.exclude */ 	protected ArrayList<Double> sendtoeach = new ArrayList<Double>();			// [min unknown splits]

	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected Node(){}
							  
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	/** @y.exclude */ 	
	protected boolean registerController(){
		if(hascontroller)		// used to detect multiple controllers
			return false;
		else{
			hascontroller = true;
			controlleron = true;
			return true;
		}
	}
	
	/** @y.exclude */ 	
    protected void setSampledSRProfile(Double3DMatrix s){
    	sampledSRprofile = s;
    }

    /** @y.exclude */ 	
	protected void setHasSRprofile(boolean hasSRprofile) {
		if(!istrivialsplit){
			this.hasSRprofile = hasSRprofile;
			this.sampledSRprofile = new Double3DMatrix(nIn,nOut,myNetwork.myScenario.getNumVehicleTypes(),0d);
			normalizeSplitRatioMatrix(this.sampledSRprofile);	// GCG REMOVE THIS AFTER CHANGING 0->NaN
		}
	}

	/** @y.exclude */ 	
	protected void setControllerOn(boolean controlleron) {
		if(hascontroller){
			this.controlleron = controlleron;
			if(!controlleron)
				resetSplitRatio();
		}
	}

	/** @y.exclude */ 	
    protected void resetSplitRatio(){

		//splitratio = new Float3DMatrix(nIn,nOut,Utils.numVehicleTypes,1f/((double)nOut));
		
		//////
		splitratio = new Double3DMatrix(nIn,nOut,myNetwork.myScenario.getNumVehicleTypes(),0d);
		normalizeSplitRatioMatrix(splitratio);
		//////
    }
    
    /** @y.exclude */ 	
	protected void setSplitratio(Double3DMatrix x) {
		splitratio.copydata(x);
		normalizeSplitRatioMatrix(splitratio);
	}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
    
	/** @y.exclude */ 	
	protected void populate(Network myNetwork) {
    	// Note: It is assumed that this comes *before* SplitRatioProfile.populate
		
		this.myNetwork = myNetwork;
		
		nOut = 0;
		if(getOutputs()!=null){
			nOut = getOutputs().getOutput().size();
			output_link = new Link[nOut];
			for(int i=0;i<nOut;i++){
				com.relteq.sirius.jaxb.Output output = getOutputs().getOutput().get(i);
				output_link[i] = myNetwork.getLinkWithId(output.getLinkId());
			}
		}

		nIn = 0;
		if(getInputs()!=null){
			nIn = getInputs().getInput().size();
			input_link = new Link[nIn];
			for(int i=0;i<nIn;i++){
				com.relteq.sirius.jaxb.Input input = getInputs().getInput().get(i);
				input_link[i] = myNetwork.getLinkWithId(input.getLinkId());
			}
		}
		
		isTerminal = nOut==0 || nIn==0;

    	if(isTerminal)
    		return;

		iscontributor = new boolean[nIn][nOut];
		istrivialsplit = nOut==1;
		hasSRprofile = false;
		sampledSRprofile = null;
		
		resetSplitRatio();
		
		hascontroller = false;
		controlleron = false;
		hasactivesplitevent = false;
	}
    
	/** @y.exclude */ 	
	protected boolean validate() {
		
		if(isTerminal)
			return true;
		
		if(output_link!=null)
			for(Link link : output_link){
				if(link==null){
					SiriusErrorLog.addError("Incorrect output link id in node id=" + getId());
					return false;
				}
			}

		if(input_link!=null)
			for(Link link : input_link){
				if(link==null){
					SiriusErrorLog.addError("Incorrect input link id in node id=" + getId());
					return false;
				}
			}
		
		if(nIn==0){
			SiriusErrorLog.addError("No inputs into non-terminal node id=" + getId());
			return false;
		}

		if(nOut==0){
			SiriusErrorLog.addError("No outputs from non-terminal node id=" + getId());
			return false;
		}
		
		return true;
	}

	/** @y.exclude */ 	
	protected void update() {
		
        if(isTerminal)
            return;

        int e,i,j,k;        
        int numEnsemble = myNetwork.myScenario.numEnsemble;
        
        // collect input demands and output supplies ...................
        for(e=0;e<numEnsemble;e++){        
    		for(i=0;i<nIn;i++)
    			inDemand[e][i] = input_link[i].outflowDemand[e];
    		for(j=0;j<nOut;j++)
    			outSupply[e][j] = output_link[j].spaceSupply[e];
        }

		// solve unknown split ratios if they are non-trivial ..............
		if(!istrivialsplit){	

	        // Take current split ratio from the profile if the node is
			// not actively controlled. Otherwise the mat has already been 
			// set by the controller.
			if(hasSRprofile && !controlleron && !hasactivesplitevent)
				splitratio.copydata(sampledSRprofile);
			
	        // compute known output demands ................................
			for(e=0;e<numEnsemble;e++)
		        for(j=0;j<nOut;j++){
		        	outDemandKnown[e][j] = 0f;
		        	for(i=0;i<nIn;i++)
		        		for(k=0;k<myNetwork.myScenario.getNumVehicleTypes();k++)
		        			if(!splitratio.get(i,j,k).isNaN())
		        				outDemandKnown[e][j] += splitratio.get(i,j,k) * inDemand[e][i][k];
		        }
	        
	        // compute and sort output demand/supply ratio .................
			for(e=0;e<numEnsemble;e++)
		        for(j=0;j<nOut;j++)
		        	dsratio[e][j] = outDemandKnown[e][j] / outSupply[e][j];
	                
	        // fill in unassigned split ratios .............................
	        resolveUnassignedSplits_A();
		}
		
        // compute node flows ..........................................
        computeLinkFlows();
        
        // assign flow to input links ..................................
		for(e=0;e<numEnsemble;e++)
	        for(i=0;i<nIn;i++)
	            input_link[i].outflow[e]=inDemand[e][i];
        
        // assign flow to output links .................................
		for(e=0;e<numEnsemble;e++)
	        for (j=0;j<nOut;j++)
	            output_link[j].inflow[e] = outFlow[e][j];
	}

	/** @y.exclude */ 	
	protected void reset() {	
		int numVehicleTypes = myNetwork.myScenario.getNumVehicleTypes();
    	int numEnsemble = myNetwork.myScenario.numEnsemble;		
    	inDemand 		= new Double[numEnsemble][nIn][numVehicleTypes];
		outSupply 		= new double[numEnsemble][nOut];
		outDemandKnown 	= new double[numEnsemble][nOut];
		dsratio 		= new double[numEnsemble][nOut];
		outFlow 		= new Double[numEnsemble][nOut][numVehicleTypes];
	}

	/////////////////////////////////////////////////////////////////////
	// operations on split ratio matrices
	/////////////////////////////////////////////////////////////////////
	
	/** @y.exclude */ 	
	protected boolean validateSplitRatioMatrix(Double3DMatrix X){

		int i,j,k;
		Double value;
		
		// dimension
		if(X.getnIn()!=nIn || X.getnOut()!=nOut || X.getnVTypes()!=myNetwork.myScenario.getNumVehicleTypes()){
			SiriusErrorLog.addError("Split ratio for node " + getId() + " has incorrect dimensions.");
			return false;
		}
		
		// range
		for(i=0;i<X.getnIn();i++){
			for(j=0;j<X.getnOut();j++){
				for(k=0;k<X.getnVTypes();k++){
					value = X.get(i,j,k);
					if( !value.isNaN() && (value>1 || value<0) ){
						SiriusErrorLog.addError("Invalid split ratio values for node id=" + getId());
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/** @y.exclude */ 	
    protected void normalizeSplitRatioMatrix(Double3DMatrix X){

    	int i,j,k;
		boolean hasNaN;
		int countNaN;
		int idxNegative;
		double sum;
    	
    	for(i=0;i<X.getnIn();i++)
    		for(k=0;k<myNetwork.myScenario.getNumVehicleTypes();k++){
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
				
				if ( !hasNaN && SiriusMath.equals(sum,0.0) ) {	
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
    
	/////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////
	
	private void computeLinkFlows(){

        // There should be no unknown splits by now ....................
        //if(any(any(any(SR<0))) || any(any(any(isnan(SR))))  )
        //    error('!!!')
        
    	int e,i,j,k;
    	int numEnsemble = myNetwork.myScenario.numEnsemble;
    	int numVehicleTypes = myNetwork.myScenario.getNumVehicleTypes();

        // input i contributes to output j .............................
    	for(i=0;i<splitratio.getnIn();i++)
        	for(j=0;j<splitratio.getnOut();j++)
        		iscontributor[i][j] = splitratio.getSumOverTypes(i,j)>0;
	
        double [][] applyratio = new double[numEnsemble][nIn];

        for(e=0;e<numEnsemble;e++)
	        for(i=0;i<nIn;i++)
	        	applyratio[e][i] = Double.NEGATIVE_INFINITY;
        
        for(e=0;e<numEnsemble;e++)
	        for(j=0;j<nOut;j++){
	        	
	        	// re-compute known output demands .........................
				outDemandKnown[e][j] = 0d;
	            for(i=0;i<nIn;i++)
	            	for(k=0;k<numVehicleTypes;k++)
	            		outDemandKnown[e][j] += inDemand[e][i][k]*splitratio.get(i,j,k);
	            
	            // compute and sort output demand/supply ratio .............
	            dsratio[e][j] = Math.max( outDemandKnown[e][j] / outSupply[e][j] , 1d );
	            
	            // reflect ratios back on inputs
	            for(i=0;i<nIn;i++)
	            	if(iscontributor[i][j])
	            		applyratio[e][i] = Math.max(dsratio[e][j],applyratio[e][i]);
	            	
	        }

        // scale down input demands
        for(e=0;e<numEnsemble;e++)
	        for(i=0;i<nIn;i++)
	            for(k=0;k<numVehicleTypes;k++)
	                inDemand[e][i][k] /= applyratio[e][i];

        // compute out flows ...........................................   
        for(e=0;e<numEnsemble;e++)
	        for(j=0;j<nOut;j++){
	        	for(k=0;k<numVehicleTypes;k++){
	        		outFlow[e][j][k] = 0d;
	            	for(i=0;i<nIn;i++){
	            		outFlow[e][j][k] += inDemand[e][i][k]*splitratio.get(i,j,k);
	            	}
	        	}
	        }
    }

    private void resolveUnassignedSplits_A(){
    	
    	int e,i,j,k;
    	int numunknown;	
    	double dsmax, dsmin;
    	double [] sr_new = new double[nOut];
    	double remainingSplit;
    	double num;
    	
    	// SHOULD ONLY BE CALLED WITH numEnsemble=1!!!
    	
    	for(e=0;e<myNetwork.myScenario.numEnsemble;e++){
	    	for(i=0;i<nIn;i++){
		        for(k=0;k<myNetwork.myScenario.getNumVehicleTypes();k++){
		            
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
		        			unknown_dsratio.add(dsratio[e][j]);		// dsratio for unknown output
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
		                
		                if(SiriusMath.equals(dsmax,dsmin))
		                    break;
		                    
	                	// indices of smallest dsratio
	                	minind_to_nOut.clear();
	                	minind_to_unknown.clear();
		            	sendtoeach.clear();		// flow needed to bring each dsmin up to dsmax
		            	double sumsendtoeach = 0f;
		            	for(int z=1;z<numunknown;z++)
		            		if( SiriusMath.equals(unknown_dsratio.get(z),dsmin) ){
		            			int index = unknownind.get(z);
		            			minind_to_nOut.add(index);
		            			minind_to_unknown.add(z);
		            			num = dsmax*outSupply[e][index] - outDemandKnown[e][index];
		            			sendtoeach.add(num);		            			
		            			sumsendtoeach += num;
		            		}
	
	                    // total that can be sent
		            	double sendtotal = Math.min(inDemand[e][i][k]*remainingSplit , sumsendtoeach );
	                    
	                    // scale down sendtoeach
	                    // store split ratio
	                    for(int z=0;z<minind_to_nOut.size();z++){
	                    	double send = sendtoeach.get(z)*sendtotal/sumsendtoeach;  
	                    	double addsplit = send/inDemand[e][i][k];
	                    	int ind_nOut = minind_to_nOut.get(z);
	                    	int ind_unknown = minind_to_unknown.get(z);
	                    	sr_new[ind_nOut] += addsplit;
	                    	remainingSplit -= addsplit;
		                    outDemandKnown[e][ind_nOut] += send;
		                    unknown_dsratio.set( ind_unknown , outDemandKnown[e][ind_nOut]/outSupply[e][ind_nOut] );
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
	                    	totalsupply += outSupply[e][jj];
	                    for(Integer jj : unknownind){
	                    	splitforeach = remainingSplit*outSupply[e][jj]/totalsupply;
	                    	sr_new[jj] += splitforeach;
	                    	outDemandKnown[e][jj] += inDemand[e][i][k]*splitforeach;
	                    }
	                    remainingSplit = 0;
		            }
		            
		            // copy to SR
		            for(j=0;j<nOut;j++)
		            	splitratio.set(i,j,k,sr_new[j]);
		        }
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
    
	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////

	/** network that containts this node */ 	
	public Network getMyNetwork() {
		return myNetwork;
	}
	    
    /** List of links exiting this node */ 
    public Link[] getOutput_link() {
		return output_link;
	}

    /** List of links entering this node */ 
	public Link[] getInput_link() {
		return input_link;
	}

    /** Index of link with given id in the list of input links of this node */ 
	public int getInputLinkIndex(String id){
		for(int i=0;i<getnIn();i++){
			if(input_link[i]!=null)
				if(input_link[i].getId().equals(id))
					return i;
		}
		return -1;
	}
	
    /** Index of link with given id in the list of output links of this node */ 
	public int getOutputLinkIndex(String id){
		for(int i=0;i<getnOut();i++){
			if(output_link[i]!=null)
				if(output_link[i].getId().equals(id))
					return i;
		}
		return -1;
	}
	
    /** Number of links entering this node */ 
	public int getnIn() {
		return nIn;
	}

    /** Number of links exiting this node */ 
	public int getnOut() {
		return nOut;
	}
    
    /** <code>true</code> iff there is a split ratio controller attached to this link */
	public boolean hasController() {
		return hascontroller;
	}
	
	/** ADDED TEMPORARILY FOR MANUEL'S DTA WORK 
	 * @throws SiriusException */
	public void setSplitRatioMatrix(double [][][] x) throws SiriusException {
		if(x.length!=splitratio.getnIn())
			throw new SiriusException("Node.setSplitRatioMatrix, bad first dimension.");
		if(x[0].length!=splitratio.getnOut())
			throw new SiriusException("Node.setSplitRatioMatrix, bad second dimension.");
		if(x[0][0].length!=splitratio.getnVTypes())
			throw new SiriusException("Node.setSplitRatioMatrix, bad third dimension.");
		int i,j,k;
		for(i=0;i<splitratio.getnIn();i++)
			for(j=0;j<splitratio.getnOut();j++)
				for(k=0;k<splitratio.getnVTypes();k++)
					splitratio.set(i, j, k, x[i][j][k]);
		normalizeSplitRatioMatrix(splitratio);
	}

	public Double [][][] getSplitRatio(){
		if(splitratio==null)
			return null;
		else{
			return splitratio.cloneData();
		}
	}
}
