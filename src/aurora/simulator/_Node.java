package aurora.simulator;

import java.util.ArrayList;

public class _Node extends aurora.jaxb.Node {

	protected Types.Node myType;
    protected boolean iscontrolled;	

	// network references
	protected _Link [] output_link;
	protected _Link [] input_link;
	
	protected Float3DMatrix sampledSRprofile;
	protected Float3DMatrix splitratio;
	protected boolean istrivialsplit;
	protected boolean hasSRprofile;

	protected int nIn;
	protected int nOut;

    // used in update()
	protected Float [][] inDemand;			// [nIn][nTypes]
	protected float [] outSupply;			// [nOut]
	protected float [] outDemandKnown;		// [nOut]
	protected float [] dsratio;				// [nOut]
	protected Float [][] outFlow; 			// [nOut][nTypes]
	protected boolean [][] iscontributor;	// [nIn][nOut]
	protected ArrayList<Integer> unknownind = new ArrayList<Integer>();		// [unknown splits]
	protected ArrayList<Float> unknown_dsratio = new ArrayList<Float>();	// [unknown splits]	
	protected ArrayList<Integer> minind_to_nOut= new ArrayList<Integer>();	// [min unknown splits]
    protected ArrayList<Integer> minind_to_unknown= new ArrayList<Integer>();	// [min unknown splits]
    protected ArrayList<Float> sendtoeach = new ArrayList<Float>();			// [min unknown splits]
    
	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////

	public Types.Node getMyType() {
		return myType;
	}
    
    public _Link[] getOutput_link() {
		return output_link;
	}

	public _Link[] getInput_link() {
		return input_link;
	}

	public int getnIn() {
		return nIn;
	}

	public int getnOut() {
		return nOut;
	}
    
	/////////////////////////////////////////////////////////////////////
	// protected
	/////////////////////////////////////////////////////////////////////

    protected void setSampledSRProfile(Float3DMatrix s){
    	sampledSRprofile = s;
    }

	protected void setHasSRprofile(boolean hasSRprofile) {
		if(!istrivialsplit){
			this.hasSRprofile = hasSRprofile;
			this.sampledSRprofile = new Float3DMatrix(nIn,nOut,Utils.numVehicleTypes,0f);
			this.sampledSRprofile.normalizeSplitRatioMatrix();	// GCG REMOVE THIS AFTER CHANGING 0->NaN
		}
	}
    
	private void computeLinkFlows(){

        // There should be no unknown splits by now ....................
        //if(any(any(any(SR<0))) || any(any(any(isnan(SR))))  )
        //    error('!!!')
        
    	int i,j,k;

        // input i contributes to output j .............................
    	for(i=0;i<splitratio.getN1();i++){
        	for(j=0;j<splitratio.getN2();j++){
        		iscontributor[i][j] = false;
        		for(Float z:splitratio.get(i,j)){
        			if(z>0){
                		iscontributor[i][j] = true;
                		break;
        			}
        		}
        	}
    	}
	
        float [] applyratio = new float[nIn];

        for(i=0;i<nIn;i++)
        	applyratio[i] = Float.NEGATIVE_INFINITY;
        
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
        		outFlow[j][k] = 0f;
            	for(i=0;i<nIn;i++){
            		outFlow[j][k] += inDemand[i][k]*splitratio.get(i,j,k);
            	}
        	}
        }
    }

    private void resolveUnassignedSplits_A(){
    	
    	int i,j,k;
    	int numunknown;	
    	float dsmax, dsmin;
    	float [] sr_new = new float[nOut];
    	float remainingSplit;
    	float num;
    	
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
	        		Float sr = splitratio.get(i,j,k);
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
	            	dsmax = Float.NEGATIVE_INFINITY;
	            	dsmin = Float.POSITIVE_INFINITY;
	            	for(Float r : unknown_dsratio){
	            		dsmax = Math.max(dsmax,r);
	            		dsmin = Math.min(dsmax,r);
	            	}
	                
	                if(dsmax==dsmin)
	                    break;
	                    
                	// indices of smallest dsratio
                	minind_to_nOut.clear();
                	minind_to_unknown.clear();
	            	sendtoeach.clear();		// flow needed to bring each dsmin up to dsmax
	            	float sumsendtoeach = 0f;
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
	            	float sendtotal = Math.min(inDemand[i][k]*remainingSplit , sumsendtoeach );
                    
                    // scale down sendtoeach
                    // store split ratio
                    for(int z=0;z<minind_to_nOut.size();z++){
                    	float send = sendtoeach.get(z)*sendtotal/sumsendtoeach;  
                    	float addsplit = send/inDemand[i][k];
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
	            	float totalcapacity = 0f;
	            	float splitforeach;
                    for(Integer jj : unknownind)
                    	totalcapacity += output_link[jj].capacity;
                    for(Integer jj : unknownind){
                    	splitforeach = remainingSplit*output_link[jj].capacity/totalcapacity;
                    	sr_new[jj] += splitforeach;
                    	outDemandKnown[jj] += inDemand[i][k]*splitforeach;
                    }
                    remainingSplit = 0;
                    */
	            	float totalsupply = 0f;
	            	float splitforeach;
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
    
	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////
    
    protected void initialize() {
    	// Note: It is assumed that this comes *before* SplitRatioProfile.initialize
		
    	try {
			myType = Types.Node.valueOf(getType());
		} catch (IllegalArgumentException e) {
			myType = null;
			return;
		}
    	    	
    	if(myType==Types.Node.T)
    		return;
    	
		nOut = 0;
		if(getOutputs()!=null){
			nOut = getOutputs().getOutput().size();
			output_link = new _Link[nOut];
			for(int i=0;i<nOut;i++){
				aurora.jaxb.Output output = getOutputs().getOutput().get(i);
				output_link[i] = Utils.getLinkWithId(output.getLinkId());
			}
		}

		nIn = 0;
		if(getInputs()!=null){
			nIn = getInputs().getInput().size();
			input_link = new _Link[nIn];
			for(int i=0;i<nIn;i++){
				aurora.jaxb.Input input = getInputs().getInput().get(i);
				input_link[i] = Utils.getLinkWithId(input.getLinkId());
			}
		}
		
		iscontrolled = false;
		
		inDemand = new Float[nIn][Utils.numVehicleTypes];
		outSupply = new float[nOut];
		outDemandKnown = new float[nOut];
		dsratio = new float[nOut];
		iscontributor = new boolean[nIn][nOut];
		outFlow = new Float[nOut][Utils.numVehicleTypes];
		istrivialsplit = nOut==1;
		hasSRprofile = false;
		sampledSRprofile = null;
		
		//splitratio = new Float3DMatrix(nIn,nOut,Utils.numVehicleTypes,1f/((float)nOut));
		
		//////
		splitratio = new Float3DMatrix(nIn,nOut,Utils.numVehicleTypes,0f);
		splitratio.normalizeSplitRatioMatrix();
		//////
		
	}
    
	protected boolean validate() {
		
		if(myType==Types.Node.T)
			return true;
		
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
		
		if(myType!=Types.Node.T && nIn==0){
			System.out.println("No inputs into node " + getId());
			return false;
		}

		if(myType!=Types.Node.T && nOut==0){
			System.out.println("No outputs from node " + getId());
			return false;
		}
		
		return true;
	}

	protected void update() {
		
        if(myType==Types.Node.T)
            return;

        int i,j,k;

        // collect input demands and output supplies ...................
		for(i=0;i<nIn;i++)
			inDemand[i] = input_link[i].outflowDemand;
		
		for(j=0;j<nOut;j++)
			outSupply[j] = output_link[j].spaceSupply;
		
		// solve unknown split ratios if they are non-trivial ..............
		if(!istrivialsplit){	

	        // Set current split ratio matrix ..............................
			if(hasSRprofile)
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
            input_link[i].outflow = inDemand[i];
        
        // assign flow to output links .................................
        for (j=0;j<nOut;j++)
            output_link[j].inflow = outFlow[j];

	}

	protected void reset() {		
	}

}
