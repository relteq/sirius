/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.FundamentalDiagram;

/** Link model.
* 
* @author Gabriel Gomes (gomes@path.berkeley.edu)
*/
public final class _Link extends com.relteq.sirius.jaxb.Link {

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

	/** @y.exclude 
	protected void setSourcedemandFromVeh(Double[] sourcedemand) {
		this.sourcedemand = sourcedemand;		
	} */
	
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

	/** @throws SiriusException 
	 * @y.exclude */
    protected void setFundamentalDiagramFromProfile(_FundamentalDiagram fd) throws SiriusException{
    	if(fd==null)
    		return;
//    	if(!fd.validate())
//			throw new SiriusException("ERROR: Fundamental diagram event could not be validated");
    		
    	FDfromProfile = fd;				// update the profile pointer
    	if(!activeFDevent)				
    		FD = FDfromProfile;			// update the fd pointer
    }

	/** @throws SiriusException 
	 * @y.exclude */
    protected void activateFundamentalDiagramEvent(FundamentalDiagram fd) throws SiriusException {
    	if(fd==null)
    		throw new SiriusException("Null parameter.");
    	
    	FDfromEvent = new _FundamentalDiagram(this);
    	FDfromEvent.copyfrom(FD);			// copy current FD
    	FDfromEvent.copyfrom(fd);			// replace values with those defined in the event
    	
		if(!FDfromEvent.validate())
			throw new SiriusException("ERROR: Fundamental diagram event could not be validated");
		activeFDevent = true;
	    FD = FDfromEvent;
    }

	/** @throws SiriusException 
	 * @y.exclude */
    protected void revertFundamentalDiagramEvent() throws SiriusException{
    	if(!activeFDevent)
    		return;
    	
    	if(!FDfromProfile.validate())
			throw new SiriusException("ERROR: Fundamental diagram event could not be validated");
 
    	activeFDevent = false;
		FD = FDfromProfile;				// point the fd back at the profile
		FDfromEvent = null;
    	
    }

	/** @throws SiriusException 
	 * @y.exclude */
	protected void set_Lanes(double newlanes) throws SiriusException{
		if(getDensityJamInVeh()*newlanes/get_Lanes() < getTotalDensityInVeh())
			throw new SiriusException("ERROR: Lanes could not be set.");

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

        this.myNetwork = myNetwork;

//		// assign type
//    	try {
//			myType = _Link.Type.valueOf(getType());
//		} catch (IllegalArgumentException e) {
//			myType = null;
//			return;
//		}

		// make network connections
		begin_node = myNetwork.getNodeWithId(getBegin().getNodeId());
		end_node = myNetwork.getNodeWithId(getEnd().getNodeId());
        
		// nodes must populate before links
		if(begin_node!=null)
			issource = begin_node.isTerminal;
		if(end_node!=null)
			issink = end_node.isTerminal;

		// lanes and length
		if(getLanes()!=null)
			_lanes = getLanes().doubleValue();
		if(getLength()!=null)
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
			SiriusErrorLog.addErrorMessage("Incorrect begin node id in link.");
			return false;
		}

		if(!issink && end_node==null){
			SiriusErrorLog.addErrorMessage("Incorrect end node id in link.");
			return false;
		}
		
		if(_length<=0){
			SiriusErrorLog.addErrorMessage("Non-positive length.");
			return false;
		}
		
		if(_lanes<=0){
			SiriusErrorLog.addErrorMessage("Non-positive number of lanes.");
			return false;
		}
		
		return true;
	}

	/** @y.exclude */
	protected void resetState(_Scenario.ModeType simulationMode) {
		
		_Scenario myScenario = myNetwork.myScenario;
		
		switch(simulationMode){
		
		case warmupFromZero:			// in warmupFromZero mode the simulation start with an empty network
			density = SiriusMath.zeros(myScenario.getNumVehicleTypes());
			break;

		case warmupFromIC:				// in warmupFromIC and normal modes, the simulation starts 
		case normal:					// from the initial density profile 
			if(myScenario.getInitialDensityProfile()!=null)
				density = ((_InitialDensityProfile)myScenario.getInitialDensityProfile()).getDensityForLinkIdInVeh(myNetwork.getId(),getId());	
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
	
	/** network that contains this link */
	public _Network getMyNetwork() {
		return myNetwork;
	}

	/** upstream node of this link  */
	public _Node getBegin_node() {
		return begin_node;
	}

	/** downstream node of this link */
	public _Node getEnd_node() {
		return end_node;
	}

	/** Length of this link in miles */
	public double getLengthInMiles() {
		return _length;
	}

	/** Number of lanes in this link */
	public double get_Lanes() {
		return _lanes;
	}

	/** <code>true</code> if this link is a source of demand into the network */
	public boolean isSource() {
		return issource;
	}

	/** <code>true</code> if this link is a sink of demand from the network */
	public boolean isSink() {
		return issink;
	}
	
	// Link state .......................

	/** Density of vehicles per vehicle type in normalized units (vehicles/link/type). 
	 * The return array is indexed by vehicle type in the order given in the 
	 * <code>settings</code> portion of the input file. 
	 * @return number of vehicles of each type in the link. 
	 */
	public Double[] getDensityInVeh() {
		return density;
	}

	/** Total of vehicles in normalized units (vehicles/link). 
	 * The return value equals the sum of {@link _Link#getDensityInVeh}.
	 * @return total number of vehicles in the link.
	 */
	public double getTotalDensityInVeh() {
		if(density!=null)
			return SiriusMath.sum(density);
		else
			return 0d;
	}
	
	/** Number of vehicles per vehicle type exiting the link 
	 * during the current time step. The return array is indexed by 
	 * vehicle type in the order given in the <code>settings</code> 
	 * portion of the input file. 
	 * @return array of exiting flows per vehicle type. 
	 */
	public Double[] getOutflowInVeh() {
		return outflow;
	}

	/** Total number of vehicles exiting the link during the current
	 * time step.  The return value equals the sum of 
	 * {@link _Link#getOutflowInVeh}.
	 * @return total number of vehicles exiting the link in one time step.
	 * 
	 */
	public double getTotalOutflowInVeh() {
		return SiriusMath.sum(outflow);
	}

	/** Average speed of traffic in the link in mile/hour. 
	 * The return value is computed by dividing the total outgoing 
	 * link flow by the total link density. 
	 * @return average link speed.
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
	
	/** Jam density in vehicle/link. */
	public double getDensityJamInVeh() {
		return FD._getDensityJamInVeh();
	}

	/** Critical density in vehicle/link. */
	public double getDensityCriticalInVeh() {
		return FD.getDensityCriticalInVeh();
	}

	/** Capacity drop in vehicle/simulation time step */
	public double getCapacityDropInVeh() {
		return FD._getCapacityDropInVeh();
	}

	/** Capacity in vehicle/simulation time step */
	public double getCapacityInVeh() {
		return FD._getCapacityInVeh();
	}

	/** Jam density in vehicle/mile/lane. */
	public double getDensityJamInVPMPL() {
		return FD._getDensityJamInVeh()/getLengthInMiles()/_lanes;
	}

	/** Critical density in vehicle/mile/lane. */
	public double getDensityCriticalInVPMPL() {
		return FD.getDensityCriticalInVeh()/getLengthInMiles()/_lanes;
	}

	/** Capacity drop in vehicle/hr/lane. */
	public double getCapacityDropInVPHPL() {
		return FD._getCapacityDropInVeh()/myNetwork.myScenario.getSimDtInHours()/_lanes;
	}

	/** Capacity in vehicles per hour. */
	public double getCapacityInVPH() {
		return FD._getCapacityInVeh() / myNetwork.myScenario.getSimDtInHours();
	}

	/** Capacity in vehicle/hr/lane. */
	public double getCapacityInVPHPL() {
		return FD._getCapacityInVeh()/myNetwork.myScenario.getSimDtInHours()/_lanes;
	}

	/** Freeflow speed in normalized units (link/time step). */
	public double getNormalizedVf() {
		return FD.getVfNormalized();
	}

	/** Freeflow speed in mile/hr. */
	public double getVfInMPH() {
		return FD.getVfNormalized()*getLengthInMiles()/myNetwork.myScenario.getSimDtInHours();
	}

	/** Congestion wave speed in normalized units (link/time step). */
	public double getNormalizedW() {
		return FD.getWNormalized();
	}

	/** Congestion wave speed in mile/hr. */
	public double getWInMPH() {
		return FD.getWNormalized()*getLengthInMiles()/myNetwork.myScenario.getSimDtInHours();
	}
	
	/** Set input flow for source links.
	 *  [This is API call is being made available for implementation of path-based DTA.
	 *  The goal is to replace it later with an internal solution.]
	 */
	public void setSourcedemandFromVeh(Double[] sourcedemand) {
		this.sourcedemand = sourcedemand;		
	}
	
}
