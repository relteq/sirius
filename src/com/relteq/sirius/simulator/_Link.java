/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

public class _Link extends com.relteq.sirius.jaxb.Link {

	private Types.Link myType;
	
	// network references
	private _Node begin_node;
	private _Node end_node;

	private double _length;					// [miles]
	private double _lanes;					// [-]
	private _FundamentalDiagram FD;			// pointer to current fundamental diagram
	private _FundamentalDiagram FDprofile;	// pointer to profile fundamental diagram
	private boolean eventactive;
	private _FundamentalDiagramProfile myFDProfile;	// needed lane change events

    // flow into the link
    // inflow points to either sourcedemand or node outflow
    private Double [] inflow;    			// [veh]	1 x numVehTypes
    private Double [] sourcedemand;			// [veh] 	1 x numVehTypes
    
    // demand and actual flow out of the link   
    private Double [] outflowDemand;   		// [veh] 	1 x numVehTypes
    private Double [] outflow;    			// [veh]	1 x numVehTypes
    
    // contoller
    private boolean iscontrolled;	
    private double control_maxflow;			// [veh]		
    private double control_maxspeed;		// [-]
    
    // state
    private Double [] density;    			// [veh]	1 x numVehTypes

    // flow evaluation
    private double spaceSupply;        		// [veh]
    
    private boolean issource; 				// [boolean]
    private boolean issink;     			// [boolean]

    private Double [] cumulative_density;	// [veh] 	1 x numVehTypes
    private Double [] cumulative_inflow;	// [veh] 	1 x numVehTypes
    private Double [] cumulative_outflow;	// [veh] 	1 x numVehTypes

	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////
    
    public _Link(){
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

	public double getLengthInMiles() {
		return _length;
	}

	public Double[] getCumDensityInVeh() {
		return cumulative_density;
	}

	public Double[] getCumInflowInVeh() {
		return cumulative_inflow;
	}

	public Double[] getCumOutflowInVeh() {
		return cumulative_outflow;
	}

	public double getLinkLength() {
		return _length;
	}

	public double get_Lanes() {
		return _lanes;
	}

	public Double[] getOutflowInVeh() {
		return outflow;
	}

	public double getTotalOutflowInVeh() {
		return Utils.sum(outflow);
	}
	
	public double computeSpeedInMPH(){
		double totaldensity = Utils.sum(density);
		double speed;
		if(totaldensity>Utils.EPSILON)
			speed = Utils.sum(outflow)/totaldensity;
		else
			speed = FD.getVf();
		return speed*_length/Utils.simdtinhours;
	}

	public double getDensityCriticalInVeh() {
		return FD._getDensityCritical();
	}

	public double getDensityJamInVeh() {
		return FD._getDensityJam();
	}

	public double getCapacityDropInVeh() {
		return FD._getCapacityDrop();
	}

	public double getCapacityInVeh() {
		return FD._getCapacity();
	}
	
	public double getDensityCriticalInVPMPL() {
		return FD._getDensityCritical()/getLengthInMiles()/_lanes;
	}
	
	public double getDensityJamInVehVPMPL() {
		return FD._getDensityJam()/getLengthInMiles()/_lanes;
	}
	
	public double getCapacityDropInVPHPL() {
		return FD._getCapacityDrop()/Utils.simdtinhours/_lanes;
	}

	public double getCapacityInVPHPL() {
		return FD._getCapacity()/Utils.simdtinhours/_lanes;
	}
	
	public double getNormalizedVf() {
		return FD.getVf();
	}

	public double getVfInMPH() {
		return FD.getVf()*getLengthInMiles()/Utils.simdtinhours;
	}
	
	public double getNormalizedW() {
		return FD.getW();
	}

	public double getWInMPH() {
		return FD.getW()*getLengthInMiles()/Utils.simdtinhours;
	}

	public boolean isIssource() {
		return issource;
	}

	public boolean isIssink() {
		return issink;
	}

	public boolean isControlled() {
		return iscontrolled;
	}
	
	public Double[] getOutflowDemand() {
		return outflowDemand;
	}

	public double getSpaceSupply() {
		return spaceSupply;
	}

    public _FundamentalDiagram getCurrentFD() {
		return FD;
	}
    
	/////////////////////////////////////////////////////////////////////
	// InterfaceSensor
	/////////////////////////////////////////////////////////////////////

	public Double[] getDensityInVeh() {
		return density;
	}

	public double getTotalDensityInVeh() {
		return Utils.sum(density);
	}
	
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

	protected void reset_cumulative(){
    	cumulative_density = Utils.zeros(Utils.numVehicleTypes);
    	cumulative_inflow  = Utils.zeros(Utils.numVehicleTypes);
    	cumulative_outflow = Utils.zeros(Utils.numVehicleTypes);
	}
    
	protected boolean registerController(){
		if(iscontrolled)		// used to detect multiple controllers
			return false;
		else{
			iscontrolled = true;
			return true;
		}
	}
	
	protected void set_Lanes(double newlanes){
		if(newlanes<=0)
			return;
		myFDProfile.set_Lanes(newlanes);	// adjust present and future fd's
		_lanes = newlanes;					// change number of lanes
	}
	
	protected void setLanesDelta(double x){
		set_Lanes(_lanes+x);
	}
	
	protected void setIscontrolled(boolean iscontrolled) {
		this.iscontrolled = iscontrolled;
	}
	
	protected void setSourcedemandFromVeh(Double[] sourcedemand) {
		this.sourcedemand = sourcedemand;
	}

	protected void setControl_maxflow(double control_maxflow) {
		this.control_maxflow = control_maxflow;
	}

	protected void setControl_maxspeed(double control_maxspeed) {
		this.control_maxspeed = control_maxspeed;
	}

	protected void setInflow(Double[] inflow) {
		this.inflow = inflow;
	}

	protected void setOutflow(Double[] outflow) {
		this.outflow = outflow;
	}

    protected void setProfileFundamentalDiagram(_FundamentalDiagram fd){
    	FDprofile = fd;
    	if(!eventactive)
    		FD = FDprofile;
    }
    
    protected void setFundamentalDiagramProfile(_FundamentalDiagramProfile fdp){
    	myFDProfile = fdp;
    }

    protected void activateFDEvent(_FundamentalDiagram fd){
    	eventactive = true;
    	FD = fd;
    }
    
    protected void deactivateFDEvent(){
    	eventactive = false;
    	FD = FDprofile;
    }    
    
	/////////////////////////////////////////////////////////////////////
	// supply and demand calculation
	/////////////////////////////////////////////////////////////////////

	protected void updateOutflowDemand(){
        
		////////////////////////////////////////
    	// GG: This is to match aurora2, but should be removed
    	// Aurora2 has a different link model for sources than for regular links.
 		if(issource){
 			outflowDemand = sourcedemand.clone();
 			double sum = 0d;
 			for(int k=0;k<Utils.numVehicleTypes;k++){
 				outflowDemand[k] += density[k];
 				sum += outflowDemand[k];
 			}
 			if(sum>FD._getCapacity()){
 				double ratio = FD._getCapacity()/sum;
 				for(int k=0;k<Utils.numVehicleTypes;k++)
 					outflowDemand[k] *= ratio;
 			}
 			return;
 		}
 		////////////////////////////////////
 		
 		

        double totaldensity = Utils.sum(density);
        
        // case empty link
        if(totaldensity<=Utils.EPSILON){
        	outflowDemand =  Utils.zeros(Utils.numVehicleTypes);        		
        	return;
        }
        
        // compute total flow leaving the link in the absence of flow control
        double totaloutflow;
        if( totaldensity < FD._getDensityCritical() ){
        	if(iscontrolled)
        		// speed control sets a bound on freeflow speed
        		totaloutflow = totaldensity * Math.min(FD.getVf(),control_maxspeed);	
        	else
        		totaloutflow = totaldensity * FD.getVf();
        }
        else{
        	totaloutflow = Math.max(FD._getCapacity()-FD._getCapacityDrop(),0d);
            if(iscontrolled)
            	totaloutflow = Math.min(totaloutflow,control_maxspeed*FD._getDensityCritical());
        }        
        
        // add controller
        if(iscontrolled)
        	totaloutflow = Math.min( totaloutflow , control_maxflow );
        
        // split among types
        outflowDemand = Utils.times(density,totaloutflow/totaldensity);
        
        return;
    }
    
    protected void updateSpaceSupply(){
    	double totaldensity = Utils.sum(density);
        spaceSupply = FD.getW()*(FD._getDensityJam() - totaldensity);
        spaceSupply = Math.min(spaceSupply,FD._getCapacity());
    }
	
	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////    
    
	protected void initialize(_Network myNetwork) {
		
        iscontrolled = false;
        
		// assign type
    	try {
			myType = Types.Link.valueOf(getType());
		} catch (IllegalArgumentException e) {
			myType = null;
			return;
		}

		// make network connections
		issource = getBegin()==null;
        issink = getEnd()==null;
		if(!issource)
			begin_node = myNetwork.getNodeWithId(getBegin().getNodeId());
		if(!issink)
			end_node = myNetwork.getNodeWithId(getEnd().getNodeId());

		// lanes and length
		_lanes = getLanes().doubleValue();
		_length = getLength().doubleValue();
//		if(_length>10){
//			System.out.println("Warning: Length of link " + getId() + " given in feet. Changing to miles.");
//			_length /= 5280f;
//			setLength(new BigDecimal(_length));
//		}
        
        // initial density, demand, and capacity
        density 			= new Double[Utils.numVehicleTypes];
        inflow 				= new Double[Utils.numVehicleTypes];
        outflow 			= new Double[Utils.numVehicleTypes];
        sourcedemand 		= new Double[Utils.numVehicleTypes];
        cumulative_density 	= new Double[Utils.numVehicleTypes];
        cumulative_inflow 	= new Double[Utils.numVehicleTypes];
        cumulative_outflow 	= new Double[Utils.numVehicleTypes];
		
        // initialize control
        resetControl();
        
	}
    
	protected boolean validate() {
		
		if(!issource && begin_node==null){
			Utils.addErrorMessage("Incorrect begin node id in link " + getId());
			return false;
		}

		if(!issink && end_node==null){
			Utils.addErrorMessage("Incorrect end node id in link " + getId());
			return false;
		}
		
		if(_length<=0){
			Utils.addErrorMessage("Link length must be positive: Link " + getId());
			return false;
		}
		
		if(_lanes<=0){
			Utils.addErrorMessage("Link lanes must be positive: Link " + getId());
			return false;
		}
		
		return true;
	}

	protected void resetState() {
				
		switch(Utils.simulationMode){
		
		case warmupFromZero:			// in warmupFromZero mode the simulation start with an empty network
			density = Utils.zeros(Utils.numVehicleTypes);
			break;

		case warmupFromIC:				// in warmupFromIC and normal modes, the simulation starts 
		case normal:					// from the initial density profile 
			_InitialDensityProfile profile = (_InitialDensityProfile) Utils.theScenario.getInitialDensityProfile();
			if(profile!=null)
				density = profile.getDensityForLinkIdInVeh(getId());	
			else 
				density = Utils.zeros(Utils.numVehicleTypes);
			break;
			
		default:
			break;
				
		}

		// reset other quantities
		for(int j=0;j<Utils.numVehicleTypes;j++){
			inflow[j] = 0d;
			outflow[j] = 0d;
			sourcedemand[j] = 0d;
			cumulative_density[j] = 0d;
			cumulative_inflow[j] = 0d;
			cumulative_outflow[j] = 0d;
		}

		return;
	}

	protected void resetLanes(){
		_lanes = getLanes().doubleValue();
	}
	
	protected void resetFD(){
    	FD = new _FundamentalDiagram(this);		// set to default
        FD.settoDefault();
    	eventactive = false;
	}

	protected void resetControl(){
        control_maxflow  = Double.POSITIVE_INFINITY;
        control_maxspeed = Double.POSITIVE_INFINITY;
	}
	
	protected void update() {
		
        if(issink)
            outflow = outflowDemand;
        
        if(issource)
            inflow = sourcedemand.clone();
        
        for(int j=0;j<Utils.numVehicleTypes;j++){
        	density[j] += inflow[j] - outflow[j];
        	cumulative_density[j] += density[j];
        	cumulative_inflow[j] += inflow[j];
        	cumulative_outflow[j] += outflow[j];
        }

	}

}
