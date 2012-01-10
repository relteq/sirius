package aurora.simulator;

import java.math.BigDecimal;

public class _Link extends aurora.jaxb.Link {

	protected Types.Link myType;
    protected boolean iscontrolled;	
	
	// network references
	protected _Node begin_node;
	protected _Node end_node;

	protected float length;				// [miles]
	protected float densityCritical; 	// [veh]           
	protected float densityJam;     	// [veh] 
	protected float capacityDrop;     	// [veh] 
    protected float qmax;            	// [veh] 
    protected float vf;                 // [-]
    protected float w;                  // [-]
    
    // flow into the link
    // inflow points to either sourcedemand or node outflow
    protected Float [] inflow;    			// [veh]	1 x numVehTypes
    protected Float [] sourcedemand;		// [veh] 	1 x numVehTypes
    
    // demand and actual flow out of the link   
    protected Float [] outflowDemand;   	// [veh] 	1 x numVehTypes
    protected Float [] outflow;    			// [veh]	1 x numVehTypes
    
    // contoller
    public float control_maxflow;			// [veh]		
    public float control_maxspeed;			// [-]
    
    // state
    protected Float [] density;    			// [veh]	1 x numVehTypes

    // flow evaluation
    protected float spaceSupply;        	// [veh]
    protected float capacity;          		// [veh] 
    
    protected boolean issource; 			// [boolean]
    protected boolean issink;     			// [boolean]

    protected Float [] cumulative_density;	// [veh] 	1 x numVehTypes
    protected Float [] cumulative_inflow;	// [veh] 	1 x numVehTypes
    protected Float [] cumulative_outflow;	// [veh] 	1 x numVehTypes

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

    protected void reset_cumulative(){
		for(int j=0;j<Utils.numVehicleTypes;j++){
			cumulative_density[j] = 0f;
			cumulative_inflow[j] = 0f;
			cumulative_outflow[j] = 0f;
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////
	    
	public Types.Link getMyType() {
		return myType;
	}
    
	public _Node getBegin_node() {
		return begin_node;
	}

	public _Node getEnd_node() {
		return end_node;
	}

	public float getLengthInMiles() {
		return length;
	}

	public Float[] getDensityInVeh() {
		return density;
	}

	public float getTotalDensityInVeh() {
		return Utils.sum(density);
	}
	
	public Float[] getOutflowInVeh() {
		return outflow;
	}

	public float getTotalOutflowInVeh() {
		return Utils.sum(outflow);
	}
	
	public float computeSpeedInMPH(){
		float totaldensity = Utils.sum(density);
		float speed;
		if(totaldensity>Utils.EPSILON)
			speed = Utils.sum(outflow)/totaldensity;
		else
			speed = vf;
		return speed*length/Utils.simdt;
	}

	public Float[] getCumDensityInVeh() {
		return cumulative_density;
	}

	public Float[] getCumInflowInVeh() {
		return cumulative_inflow;
	}

	public Float[] getCumOutflowInVeh() {
		return cumulative_outflow;
	}

	public float getLinkLength() {
		return length;
	}

	public float getDensityCritical() {
		return densityCritical;
	}

	public float getDensityJam() {
		return densityJam;
	}

	public float getCapacityDrop() {
		return capacityDrop;
	}

	public float getLinkQmax() {
		return qmax;
	}

	public float getVf() {
		return vf;
	}

	public float getW() {
		return w;
	}

	public boolean isControlled() {
		return iscontrolled;
	}

	public float getCapacity() {
		return capacity;
	}

	public boolean isIssource() {
		return issource;
	}

	public boolean isIssink() {
		return issink;
	}
	
	/////////////////////////////////////////////////////////////////////
	// supply and demand calculation
	/////////////////////////////////////////////////////////////////////

	protected void updateOutflowDemand(){
         
    	// GG: This is to match aurora2, but should be removed
    	// Aurora2 has a different link model for sources than for regular links.
 		if(issource){
 			outflowDemand = sourcedemand.clone();
 			float sum = 0f;
 			for(int k=0;k<Utils.numVehicleTypes;k++){
 				outflowDemand[k] += density[k];
 				sum += outflowDemand[k];
 			}
 			if(sum>capacity){
 				float ratio = capacity/sum;
 				for(int k=0;k<Utils.numVehicleTypes;k++)
 					outflowDemand[k] *= ratio;
 			}
 			return;
 		}
 
        float dcrit = capacity / vf;
        float totaldensity = Utils.sum(density);
        if( totaldensity < dcrit )
        	outflowDemand = Utils.times(density,vf);
        else{
            if(totaldensity>0){
                Float [] splits = Utils.times(density,1f/totaldensity);
                float cap = Math.max(capacity-capacityDrop,0f);
                outflowDemand = Utils.times(splits,cap);
            }
            else
            	outflowDemand = Utils.times(density,0f);
        }
        return;
    }
    
    protected void updateSpaceSupply(){
    	float totaldensity = Utils.sum(density);
        spaceSupply = w*(densityJam - totaldensity);
        spaceSupply = Math.min(spaceSupply,capacity);
    }
	
	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////
    
	protected void initialize() {

		// assign type
    	try {
			myType = Types.Link.valueOf(getType());
		} catch (IllegalArgumentException e) {
			myType = null;
			return;
		}
		iscontrolled = false;

		// make network connections
		begin_node = Utils.getNodeWithId(getBegin().getNodeId());
		end_node = Utils.getNodeWithId(getEnd().getNodeId());
		
		//int lanes = getLanes().intValue();
		length = getLength().floatValue();
		if(length>10){
			System.out.println("Warning: Length of link " + getId() + " given in feet. Changing to miles.");
			length /= 5280f;
			setLength(new BigDecimal(length));
		}
		
		// values from file
		densityCritical = Float.parseFloat(getFd().getDensityCritical());	// [veh/mile]
		densityJam = Float.parseFloat(getFd().getDensityJam());				// [veh/mile]
		float flowMax = Float.parseFloat(getFd().getFlowMax());				// [veh/hr]
		capacityDrop = getFd().getCapacityDrop().floatValue();				// [veh/hr]

		// normalize
		densityCritical *= length;
        densityJam 		*= length;
        flowMax 		*= Utils.simdt;
        capacityDrop 	*= Utils.simdt;
        
        vf = flowMax / densityCritical;					// [0,1]
        w = flowMax / (densityJam-densityCritical);		// [0,1]
        
        issource = begin_node.getType().equals("T");
        issink = end_node.getType().equals("T");
        
        // initial density, demand, and capacity
        density 			= new Float[Utils.numVehicleTypes];
        inflow 				= new Float[Utils.numVehicleTypes];
        outflow 			= new Float[Utils.numVehicleTypes];
        sourcedemand 		= new Float[Utils.numVehicleTypes];
        cumulative_density 	= new Float[Utils.numVehicleTypes];
        cumulative_inflow 	= new Float[Utils.numVehicleTypes];
        cumulative_outflow 	= new Float[Utils.numVehicleTypes];
        capacity = flowMax;
        
	}
    
	protected boolean validate() {
		if(vf>1 || w>1){
			System.out.println("CFL condition violated");
			return false;
		}
		return true;
	}

	protected void reset() {
		Float [] mydensity = null;
		
		_InitialDensityProfile profile = (_InitialDensityProfile) Utils.theScenario.getInitialDensityProfile();
		if(profile!=null)
			mydensity = profile.getDensityForLinkId(getId());		
		for(int j=0;j<Utils.numVehicleTypes;j++){
			if(profile!=null)
				density[j] = mydensity[j];
			else
				density[j] = 0f;
			inflow[j] = 0f;
			outflow[j] = 0f;
			sourcedemand[j] = 0f;
			cumulative_density[j] = 0f;
			cumulative_inflow[j] = 0f;
			cumulative_outflow[j] = 0f;
		}
		return;
	}

	protected void update() {
		
        if(issink)
            outflow = outflowDemand;
        
        if(issource)
            inflow = sourcedemand.clone();
          
//        if(this.getId().equals("-84") && Utils.clock.getCurrentstep()>=191
//        		&& Utils.clock.getCurrentstep()<200){
//        	System.out.println("\t" + inflow[0]);
//        }  
        
        for(int j=0;j<Utils.numVehicleTypes;j++){
        	density[j] += inflow[j] - outflow[j];
        	cumulative_density[j] += density[j];
        	cumulative_inflow[j] += inflow[j];
        	cumulative_outflow[j] += outflow[j];
        }
        
        
        
        
	}

}
