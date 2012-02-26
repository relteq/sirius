/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.FundamentalDiagram;

/** DESCRIPTION OF THE CLASS
*
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public final class _Link extends com.relteq.sirius.jaxb.Link {

	public static enum Type	{  freeway,
						       HOV,
						       HOT,
						       onramp,
						       offramp,
						       freeway_connector,
						       street,
						       intersection_apporach,
						       other };	
		   
//	public static enum DynamicsType	{NULL, CTM,
//										   region_tracking,
//										   discrete_departure };

	/** @y.exclude */ 	protected _Link.Type myType;
	/** @y.exclude */ 	protected _Network myNetwork;
	/** @y.exclude */ 	protected _Node begin_node;
	/** @y.exclude */ 	protected _Node end_node;

	/** @y.exclude */ 	protected double _length;							// [miles]
	/** @y.exclude */ 	protected double _lanes;							// [-]
	/** @y.exclude */ 	protected _FundamentalDiagram FD;					// current fundamental diagram
	/** @y.exclude */ 	protected _FundamentalDiagram FDfromProfile;		// profile fundamental diagram
	/** @y.exclude */ 	protected _FundamentalDiagram FDfromEvent;			// event fundamental diagram
	/** @y.exclude */ 	protected _FundamentalDiagramProfile myFDprofile;	// reference to fundamental diagram profile (used to rescale future FDs upon lane change event)
	/** @y.exclude */ 	protected boolean activeFDevent;					// true if an FD event is active on this link,
																			// true  means FD points to FDfromEvent 
																			// false means FD points to FDfromprofile
    // flow into the link
	/** @y.exclude */ 	protected Double [] inflow;    			// [veh]	1 x numVehTypes
	/** @y.exclude */ 	protected Double [] sourcedemand;		// [veh] 	1 x numVehTypes
    
    // demand and actual flow out of the link   
	/** @y.exclude */ 	protected Double [] outflowDemand;   	// [veh] 	1 x numVehTypes
	/** @y.exclude */ 	protected Double [] outflow;    		// [veh]	1 x numVehTypes
    
    // contoller
	/** @y.exclude */ 	protected int control_maxflow_index;
	/** @y.exclude */ 	protected int control_maxspeed_index;
	/** @y.exclude */ 	protected _Controller myFlowController;
	/** @y.exclude */ 	protected _Controller mySpeedController;
   
	/** @y.exclude */ 	protected Double [] density;    		// [veh]	1 x numVehTypes
	/** @y.exclude */ 	protected double spaceSupply;        	// [veh]
	/** @y.exclude */ 	protected boolean issource; 			// [boolean]
	/** @y.exclude */ 	protected boolean issink;     			// [boolean]
	/** @y.exclude */ 	protected Double [] cumulative_density;	// [veh] 	1 x numVehTypes
	/** @y.exclude */ 	protected Double [] cumulative_inflow;	// [veh] 	1 x numVehTypes
	/** @y.exclude */ 	protected Double [] cumulative_outflow;	// [veh] 	1 x numVehTypes

	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected _Link(){}
	
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected void reset_cumulative(){
    	cumulative_density = SiriusMath.zeros(myNetwork.myScenario.getNumVehicleTypes());
    	cumulative_inflow  = SiriusMath.zeros(myNetwork.myScenario.getNumVehicleTypes());
    	cumulative_outflow = SiriusMath.zeros(myNetwork.myScenario.getNumVehicleTypes());
	}

	/** @y.exclude */
	protected boolean registerFlowController(_Controller c,int index){
		if(myFlowController!=null)
			return false;
		else{
			myFlowController = c;
			control_maxflow_index = index;
			return true;
		}
	}

	/** @y.exclude */
	protected boolean registerSpeedController(_Controller c,int index){
		if(mySpeedController!=null)
			return false;
		else{
			mySpeedController = c;
			control_maxspeed_index = index;
			return true;
		}
	}

	/** @y.exclude */
	protected void setSourcedemandFromVeh(Double[] sourcedemand) {
		this.sourcedemand = sourcedemand;		
	}

	/** @y.exclude */
	protected void setInflow(Double[] inflow) {
		this.inflow = inflow;
	}

	/** @y.exclude */
	protected void setOutflow(Double[] outflow) {
		this.outflow = outflow;
	}

	/** @y.exclude */
    protected void setFundamentalDiagramProfile(_FundamentalDiagramProfile fdp){
    	if(fdp==null)
    		return;
    	myFDprofile = fdp;
    }

	/** @y.exclude */
    protected void setFundamentalDiagramFromProfile(_FundamentalDiagram fd){
    	if(fd==null)
    		return;
    	FDfromProfile = fd;				// update the profile pointer
    	if(!activeFDevent)				
    		FD = FDfromProfile;			// update the fd pointer
    }

	/** @y.exclude */
    protected void activateFundamentalDiagramEvent(FundamentalDiagram fd){
    	if(fd==null)
    		return;
    	FDfromEvent = new _FundamentalDiagram(this);
    	FDfromEvent.copyfrom(FD);			// copy current FD
    	FDfromEvent.copyfrom(fd);			// replace values with those defined in the event
		if(FDfromEvent.validate()){			// validate the result
	    	activeFDevent = true;
	    	FD = FDfromEvent;
		}	
    }

	/** @y.exclude */
    protected void revertFundamentalDiagramEvent(){
    	if(activeFDevent){
	    	activeFDevent = false;
    		FD = FDfromProfile;				// point the fd back at the profile
    		FDfromEvent = null;
    	}
    }

	/** @y.exclude */
	protected void set_Lanes(double newlanes){
		if(newlanes<0)
			return;
		myFDprofile.set_Lanes(newlanes);	// adjust present and future fd's
		if(FDfromEvent!=null)
			FDfromEvent.setLanes(newlanes);	// adjust the event fd.
		_lanes = newlanes;					// adjust local copy of lane count
	}
	
	/////////////////////////////////////////////////////////////////////
	// supply and demand calculation
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected void updateOutflowDemand(){
        
		int numVehicleTypes = myNetwork.myScenario.getNumVehicleTypes();
		
		////////////////////////////////////////
    	// GG: This is to match aurora2, but should be removed
    	// Aurora2 has a different link model for sources than for regular links.
// 		if(issource){
// 			outflowDemand = sourcedemand.clone();
// 			double sum = 0d;
// 			for(int k=0;k<numVehicleTypes;k++){
// 				outflowDemand[k] += density[k];
// 				sum += outflowDemand[k];
// 			}
// 			if(sum>FD._getCapacityInVeh()){
// 				double ratio = FD._getCapacityInVeh()/sum;
// 				for(int k=0;k<numVehicleTypes;k++)
// 					outflowDemand[k] *= ratio;
// 			}
// 			return;
// 		}
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
        	if(mySpeedController!=null && mySpeedController.ison){
        		// speed control sets a bound on freeflow speed
            	double control_maxspeed = mySpeedController.control_maxspeed[control_maxspeed_index];
        		totaloutflow = totaldensity * Math.min(FD.getVfNormalized(),control_maxspeed);	
        	}
        	else
        		totaloutflow = totaldensity * FD.getVfNormalized();
        }
        else{
        	totaloutflow = Math.max(FD._getCapacityInVeh()-FD._getCapacityDropInVeh(),0d);
            if(mySpeedController!=null && mySpeedController.ison){	// speed controller
            	double control_maxspeed = mySpeedController.control_maxspeed[control_maxspeed_index];
            	totaloutflow = Math.min(totaloutflow,control_maxspeed*FD.getDensityCriticalInVeh());
            }
        }
        
        // flow controller
        if(myFlowController!=null && myFlowController.ison){
        	double control_maxflow = myFlowController.control_maxflow[control_maxflow_index];
        	totaloutflow = Math.min( totaloutflow , control_maxflow );
        }

        
        // split among types
        outflowDemand = SiriusMath.times(density,totaloutflow/totaldensity);
        
        return;
    }

	/** @y.exclude */
    protected void updateSpaceSupply(){
    	double totaldensity = SiriusMath.sum(density);
        spaceSupply = FD.getWNormalized()*(FD._getDensityJamInVeh() - totaldensity);
        spaceSupply = Math.min(spaceSupply,FD._getCapacityInVeh());
    }
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////    

	/** @y.exclude */
	protected void populate(_Network myNetwork) {

		if(getBegin()==null)
			return;
		if(getEnd()==null)
			return;
				
        this.myNetwork = myNetwork;
        
		// assign type
    	try {
			myType = _Link.Type.valueOf(getType());
		} catch (IllegalArgumentException e) {
			myType = null;
			return;
		}

		// make network connections
		begin_node = myNetwork.getNodeWithId(getBegin().getNodeId());
		end_node = myNetwork.getNodeWithId(getEnd().getNodeId());
        
		issource = _Node.Type.valueOf(begin_node.getType()) ==_Node.Type.terminal;
		issink = _Node.Type.valueOf(end_node.getType()) ==_Node.Type.terminal;

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
		
	}

	/** @y.exclude */
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

	/** @y.exclude */
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

	/** @y.exclude */
	protected void resetLanes(){
		_lanes = getLanes().doubleValue();
	}

	/** @y.exclude */
	protected void resetFD(){
    	FD = new _FundamentalDiagram(this);
        FD.settoDefault();		// set to default
    	activeFDevent = false;
	}

	/** @y.exclude */
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
	
	// Link geometry ....................
	
	/** DESCRIPTION 
	 * 
	 */
	public _Link.Type getMyType() {
		return myType;
	}
	/** DESCRIPTION 
	 * 
	 */
    
	public _Network getMyNetwork() {
		return myNetwork;
	}

	/** DESCRIPTION 
	 * 
	 */
	public _Node getBegin_node() {
		return begin_node;
	}

	/** DESCRIPTION 
	 * 
	 */
	public _Node getEnd_node() {
		return end_node;
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getLengthInMiles() {
		return _length;
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getLinkLength() {
		return _length;
	}

	/** DESCRIPTION 
	 * 
	 */
	public double get_Lanes() {
		return _lanes;
	}

	/** DESCRIPTION 
	 * 
	 */
	public boolean isSource() {
		return issource;
	}

	/** DESCRIPTION 
	 * 
	 */
	public boolean isSink() {
		return issink;
	}
	
	// Link state .......................

	/** DESCRIPTION 
	 * 
	 */
	public Double[] getDensityInVeh() {
		return density;
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getTotalDensityInVeh() {
		return SiriusMath.sum(density);
	}
	
	/** DESCRIPTION 
	 * 
	 */
	public Double[] getOutflowInVeh() {
		return outflow;
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getTotalOutflowInVeh() {
		return SiriusMath.sum(outflow);
	}

	/** DESCRIPTION 
	 * 
	 */
	public double computeSpeedInMPH(){
		double totaldensity = SiriusMath.sum(density);
		double speed;
		if( SiriusMath.greaterthan(totaldensity,0d) )
			speed = SiriusMath.sum(outflow)/totaldensity;
		else
			speed = FD.getVfNormalized();
		return speed*_length/myNetwork.myScenario.getSimDtInHours();
	}

	// Fundamental diagram ....................
	
	/** DESCRIPTION 
	 * 
	 */
	public double getDensityJamInVeh() {
		return FD._getDensityJamInVeh();
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getDensityCriticalInVeh() {
		return FD.getDensityCriticalInVeh();
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getCapacityDropInVeh() {
		return FD._getCapacityDropInVeh();
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getCapacityInVeh() {
		return FD._getCapacityInVeh();
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getDensityJamInVPMPL() {
		return FD._getDensityJamInVeh()/getLengthInMiles()/_lanes;
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getDensityCriticalInVPMPL() {
		return FD.getDensityCriticalInVeh()/getLengthInMiles()/_lanes;
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getCapacityDropInVPHPL() {
		return FD._getCapacityDropInVeh()/myNetwork.myScenario.getSimDtInHours()/_lanes;
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getCapacityInVPHPL() {
		return FD._getCapacityInVeh()/myNetwork.myScenario.getSimDtInHours()/_lanes;
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getNormalizedVf() {
		return FD.getVfNormalized();
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getVfInMPH() {
		return FD.getVfNormalized()*getLengthInMiles()/myNetwork.myScenario.getSimDtInHours();
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getNormalizedW() {
		return FD.getWNormalized();
	}

	/** DESCRIPTION 
	 * 
	 */
	public double getWInMPH() {
		return FD.getWNormalized()*getLengthInMiles()/myNetwork.myScenario.getSimDtInHours();
	}

}
