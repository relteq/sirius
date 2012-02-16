/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

/** DESCRIPTION OF THE CLASS
*
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public final class _Link extends com.relteq.sirius.jaxb.Link {

	public static enum Type	{NULL, freeway,
								       HOV,
								       HOT,
								       onramp,
								       offramp,
								       freeway_connector,
								       street,
								       intersection_apporach,other };	
		   
	protected static enum DynamicsType	{NULL, CTM,
										       region_tracking,
										       discrete_departure };

	protected _Link.Type myType;
	
	// references
	protected _Network myNetwork;
	protected _Node begin_node;
	protected _Node end_node;

	protected double _length;					// [miles]
	protected double _lanes;					// [-]
	protected _FundamentalDiagram FD;			// pointer to current fundamental diagram
	protected _FundamentalDiagram FDprofile;	// pointer to profile fundamental diagram
	protected boolean eventactive;
	protected _FundamentalDiagramProfile myFDProfile;	// needed lane change events

    // flow into the link
    // inflow points to either sourcedemand or node outflow
	protected Double [] inflow;    			// [veh]	1 x numVehTypes
	protected Double [] sourcedemand;			// [veh] 	1 x numVehTypes
    
    // demand and actual flow out of the link   
	protected Double [] outflowDemand;   		// [veh] 	1 x numVehTypes
	protected Double [] outflow;    			// [veh]	1 x numVehTypes
    
    // contoller
	protected boolean iscontrolled;	
	protected double control_maxflow;			// [veh]		
	protected double control_maxspeed;		// [-]
    
    // state
	protected Double [] density;    			// [veh]	1 x numVehTypes

    // flow evaluation
	protected double spaceSupply;        		// [veh]
    
	protected boolean issource; 				// [boolean]
	protected boolean issink;     			// [boolean]

	protected Double [] cumulative_density;	// [veh] 	1 x numVehTypes
	protected Double [] cumulative_inflow;	// [veh] 	1 x numVehTypes
	protected Double [] cumulative_outflow;	// [veh] 	1 x numVehTypes

	/////////////////////////////////////////////////////////////////////
	// InterfaceSensor
	/////////////////////////////////////////////////////////////////////

	public Double[] getDensityInVeh() {
		return density;
	}

	public double getTotalDensityInVeh() {
		return SiriusMath.sum(density);
	}
	
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

	protected void reset_cumulative(){
    	cumulative_density = SiriusMath.zeros(myNetwork.myScenario.getNumVehicleTypes());
    	cumulative_inflow  = SiriusMath.zeros(myNetwork.myScenario.getNumVehicleTypes());
    	cumulative_outflow = SiriusMath.zeros(myNetwork.myScenario.getNumVehicleTypes());
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
        
		int numVehicleTypes = myNetwork.myScenario.getNumVehicleTypes();
		
		////////////////////////////////////////
    	// GG: This is to match aurora2, but should be removed
    	// Aurora2 has a different link model for sources than for regular links.
 		if(issource){
 			outflowDemand = sourcedemand.clone();
 			double sum = 0d;
 			for(int k=0;k<numVehicleTypes;k++){
 				outflowDemand[k] += density[k];
 				sum += outflowDemand[k];
 			}
 			if(sum>FD._getCapacityInVeh()){
 				double ratio = FD._getCapacityInVeh()/sum;
 				for(int k=0;k<numVehicleTypes;k++)
 					outflowDemand[k] *= ratio;
 			}
 			return;
 		}
 		////////////////////////////////////
 		
        double totaldensity = SiriusMath.sum(density);
        
        // case empty link
        if( SiriusMath.lessorequalthan(totaldensity,0d) ){
        	outflowDemand =  SiriusMath.zeros(numVehicleTypes);        		
        	return;
        }
        
        // compute total flow leaving the link in the absence of flow control
        double totaloutflow;
        if( totaldensity < FD.getDensityCriticalInVeh() ){
        	if(iscontrolled)
        		// speed control sets a bound on freeflow speed
        		totaloutflow = totaldensity * Math.min(FD.getVfNormalized(),control_maxspeed);	
        	else
        		totaloutflow = totaldensity * FD.getVfNormalized();
        }
        else{
        	totaloutflow = Math.max(FD._getCapacityInVeh()-FD._getCapacityDropInVeh(),0d);
            if(iscontrolled)	// speed controller
            	totaloutflow = Math.min(totaloutflow,control_maxspeed*FD.getDensityCriticalInVeh());
        }        
        
        // flow controller
        if(iscontrolled)
        	totaloutflow = Math.min( totaloutflow , control_maxflow );
        
        // split among types
        outflowDemand = SiriusMath.times(density,totaloutflow/totaldensity);
        
        return;
    }
    
    protected void updateSpaceSupply(){
    	double totaldensity = SiriusMath.sum(density);
        spaceSupply = FD.getWNormalized()*(FD._getDensityJamInVeh() - totaldensity);
        spaceSupply = Math.min(spaceSupply,FD._getCapacityInVeh());
    }
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////    
    
	protected void populate(_Network myNetwork) {

        this.myNetwork = myNetwork;
        iscontrolled = false;
        
		// assign type
    	try {
			myType = _Link.Type.valueOf(getType());
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
        
        // initial density, demand, and capacity
		int numVehicleTypes = myNetwork.myScenario.getNumVehicleTypes();
        density 			= new Double[numVehicleTypes];
        inflow 				= new Double[numVehicleTypes];
        outflow 			= new Double[numVehicleTypes];
        sourcedemand 		= new Double[numVehicleTypes];
        cumulative_density 	= new Double[numVehicleTypes];
        cumulative_inflow 	= new Double[numVehicleTypes];
        cumulative_outflow 	= new Double[numVehicleTypes];
		
        // initialize control
        resetControl();
        
	}
    
	protected boolean validate() {
		
		if(!issource && begin_node==null){
			SiriusError.addErrorMessage("Incorrect begin node id in link " + getId());
			return false;
		}

		if(!issink && end_node==null){
			SiriusError.addErrorMessage("Incorrect end node id in link " + getId());
			return false;
		}
		
		if(_length<=0){
			SiriusError.addErrorMessage("Link length must be positive: Link " + getId());
			return false;
		}
		
		if(_lanes<=0){
			SiriusError.addErrorMessage("Link lanes must be positive: Link " + getId());
			return false;
		}
		
		return true;
	}

	protected void resetState() {
		
		_Scenario myScenario = myNetwork.myScenario;
		
		switch(myScenario.simulationMode){
		
		case warmupFromZero:			// in warmupFromZero mode the simulation start with an empty network
			density = SiriusMath.zeros(myScenario.getNumVehicleTypes());
			break;

		case warmupFromIC:				// in warmupFromIC and normal modes, the simulation starts 
		case normal:					// from the initial density profile 
			if(myScenario.getInitialDensityProfile()!=null)
				density = ((_InitialDensityProfile)myScenario.getInitialDensityProfile()).getDensityForLinkIdInVeh(getId());	
			else 
				density = SiriusMath.zeros(myScenario.getNumVehicleTypes());
			break;
			
		default:
			break;
				
		}

		// reset other quantities
		for(int j=0;j<myScenario.getNumVehicleTypes();j++){
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
    	FD = new _FundamentalDiagram(this);
        FD.settoDefault();		// set to default
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
        
        for(int j=0;j<myNetwork.myScenario.getNumVehicleTypes();j++){
        	density[j] += inflow[j] - outflow[j];
        	cumulative_density[j] += density[j];
        	cumulative_inflow[j] += inflow[j];
        	cumulative_outflow[j] += outflow[j];
        }

	}

	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////
	
	public _Link.Type getMyType() {
		return myType;
	}
    
	public _Network getMyNetwork() {
		return myNetwork;
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
		return SiriusMath.sum(outflow);
	}
	
	public double computeSpeedInMPH(){
		double totaldensity = SiriusMath.sum(density);
		double speed;
		if( SiriusMath.greaterthan(totaldensity,0d) )
			speed = SiriusMath.sum(outflow)/totaldensity;
		else
			speed = FD.getVfNormalized();
		return speed*_length/myNetwork.myScenario.getSimDtInHours();
	}

	public double getDensityJamInVeh() {
		return FD._getDensityJamInVeh();
	}
	
	public double getDensityCriticalInVeh() {
		return FD.getDensityCriticalInVeh();
	}

	public double getCapacityDropInVeh() {
		return FD._getCapacityDropInVeh();
	}

	public double getCapacityInVeh() {
		return FD._getCapacityInVeh();
	}
	
	public double getDensityJamInVPMPL() {
		return FD._getDensityJamInVeh()/getLengthInMiles()/_lanes;
	}

	public double getDensityCriticalInVPMPL() {
		return FD.getDensityCriticalInVeh()/getLengthInMiles()/_lanes;
	}
	
	public double getCapacityDropInVPHPL() {
		return FD._getCapacityDropInVeh()/myNetwork.myScenario.getSimDtInHours()/_lanes;
	}

	public double getCapacityInVPHPL() {
		return FD._getCapacityInVeh()/myNetwork.myScenario.getSimDtInHours()/_lanes;
	}
	
	public double getNormalizedVf() {
		return FD.getVfNormalized();
	}

	public double getVfInMPH() {
		return FD.getVfNormalized()*getLengthInMiles()/myNetwork.myScenario.getSimDtInHours();
	}
	
	public double getNormalizedW() {
		return FD.getWNormalized();
	}

	public double getWInMPH() {
		return FD.getWNormalized()*getLengthInMiles()/myNetwork.myScenario.getSimDtInHours();
	}

	public boolean isIsSource() {
		return issource;
	}

	public boolean isIsSink() {
		return issink;
	}

	public boolean isControlled() {
		return iscontrolled;
	}
	
}
