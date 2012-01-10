package simulator;

import java.math.BigDecimal;

import jaxb.Link;

public class _Link extends Link implements AuroraComponent {

	// network references
    public _Node begin_node;
    public _Node end_node;

    public float length;				// [miles]
    public float densityCritical; 		// [veh]           
    //public float flowMax;         		// [veh]               
    public float densityJam;     		// [veh] 
    public float capacityDrop;     		// [veh] 
    public float qmax;            		// [veh] 
    public float vf;                   	// [-]
    public float w;                    	// [-]
    
    // flow into the link
    // inflow points to either sourcedemand or node outflow
    public Float [] inflow;    			// [veh]	1 x numVehTypes
    public Float [] sourcedemand;		// [veh] 	1 x numVehTypes
    
    // demand and actual flow out of the link   
    public Float [] outflowDemand;     	// [veh] 	1 x numVehTypes
    public Float [] outflow;    		// [veh]	1 x numVehTypes 
    
    // state
    public Float [] density;    		// [veh]	1 x numVehTypes

    // flow evaluation
    public float spaceSupply;          	// [veh]
    public float capacity;          	// [veh] 
    
    public boolean issource; 			// [boolean]
    public boolean issink;     			// [boolean]

	private Float [] cumulative_inflow;		// [veh] 	1 x numVehTypes
	private Float [] cumulative_outflow;	// [veh] 	1 x numVehTypes

	/////////////////////////////////////////////////////////////////////
	// getters and setters 
	/////////////////////////////////////////////////////////////////////
    
    public Float[] getCumInflowInVeh() {
		return cumulative_inflow;
	}

	public Float[] getCumOutflowInVeh() {
		return cumulative_outflow;
	}

	public Float[] getDensityInVeh() {
		return density;
	}
    
    public void setSourceDemand(Float [] values){
    	sourcedemand = values;
    }

	public void setCapacity(float C){
    	capacity = C;
    }

	public void reset_cumulative(){
		for(int j=0;j<Utils.numVehicleTypes;j++){
			cumulative_inflow[j] = 0f;
			cumulative_outflow[j] = 0f;
		}
	}

	/////////////////////////////////////////////////////////////////////
	// supply and demand calculation
	/////////////////////////////////////////////////////////////////////
    
     public void updateOutflowDemand(){
         
    	// GG: This is to match aurora2, but should be removed
    	// Aurora2 has a different link model for sources than for regular links.
 		if(issource){
 			outflowDemand = sourcedemand.clone();
 			for(int k=0;k<Utils.numVehicleTypes;k++)
 				outflowDemand[k] += density[k];
 			return;
 		}
 
        float dcrit = capacity / vf;
        float totaldensity = Utils.sum(density);
        if( totaldensity < dcrit )
        	outflowDemand = Utils.times(density,vf);
        else{
            if(totaldensity>0){
                Float [] splits = Utils.times(density,1/totaldensity);
                float cap = Math.max(capacity-capacityDrop,0f);
                outflowDemand = Utils.times(splits,cap);
            }
            else
            	outflowDemand = Utils.times(density,0f);
        }
        return;
    }
    
    public void updateSpaceSupply(){
        float totaldensity = Utils.sum(density);
        spaceSupply = w*(densityJam - totaldensity);
        spaceSupply = Math.min(spaceSupply,capacity);
    }
    
	/////////////////////////////////////////////////////////////////////
	// _AuroraComponent
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public void initialize() {

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
        cumulative_inflow 	= new Float[Utils.numVehicleTypes];
        cumulative_outflow 	= new Float[Utils.numVehicleTypes];
        capacity = flowMax;
        
	}
    
    @Override
	public boolean validate() {
		if(vf>1 || w>1){
			System.out.println("CFL condition violated");
			return false;
		}
		return true;
	}

	@Override
	public void reset() {
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
			cumulative_inflow[j] = 0f;
			cumulative_outflow[j] = 0f;
		}
		return;
	}

	@Override
	public void update() {
		
        if(issink)
            outflow = outflowDemand;
        
        if(issource)
            inflow = sourcedemand.clone();
        
        for(int j=0;j<Utils.numVehicleTypes;j++){
        	cumulative_inflow[j] += inflow[j];
        	cumulative_outflow[j] += outflow[j];
        	density[j] += inflow[j] - outflow[j];
        }
	}

}
