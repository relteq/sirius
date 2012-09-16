/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

/** Link class.
* 
* @author Gabriel Gomes (gomes@path.berkeley.edu)
*/
public final class Link extends com.relteq.sirius.jaxb.Link {

	/** @y.exclude */ 	protected Network myNetwork;
	/** @y.exclude */ 	protected Node begin_node;
	/** @y.exclude */ 	protected Node end_node;

	// link type
	protected Link.Type myType;
	/** Type of link. */
	public static enum Type	{freeway,HOV,HOT,onramp,offramp,freeway_connector,street,intersection_approach,heavy_vehicle,electric_toll};
	
	/** @y.exclude */ 	protected double _length;							// [miles]
	/** @y.exclude */ 	protected double _lanes;							// [-]
	/** @y.exclude */ 	protected FundamentalDiagram [] FDfromProfile;		// profile fundamental diagram
	/** @y.exclude */ 	protected FundamentalDiagram FDfromEvent;			// event fundamental diagram
	/** @y.exclude */ 	protected FundamentalDiagramProfile myFDprofile;	// reference to fundamental diagram profile (used to rescale future FDs upon lane change event)
	/** @y.exclude */ 	protected boolean activeFDevent;					// true if an FD event is active on this link,
																			// true  means FD points to FDfromEvent 
																			// false means FD points to FDfromprofile
    // flow into the link
	/** @y.exclude */ 	protected Double [][] inflow;    		// [veh]	numEnsemble x numVehTypes
	/** @y.exclude */ 	protected Double [] sourcedemand;		// [veh] 	numVehTypes
    
    // demand and actual flow out of the link   
	/** @y.exclude */ 	protected Double [][] outflowDemand;   	// [veh] 	numEnsemble x numVehTypes
	/** @y.exclude */ 	protected Double [][] outflow;    		// [veh]	numEnsemble x numVehTypes
    
    // contoller
	/** @y.exclude */ 	protected int control_maxflow_index;
	/** @y.exclude */ 	protected int control_maxspeed_index;
	/** @y.exclude */ 	protected Controller myFlowController;
	/** @y.exclude */ 	protected Controller mySpeedController;
   
	/** @y.exclude */ 	protected Double [][] density;    			// [veh]	numEnsemble x numVehTypes
	/** @y.exclude */ 	protected Double []spaceSupply;        		// [veh]	numEnsemble
	/** @y.exclude */ 	protected boolean issource; 				// [boolean]
	/** @y.exclude */ 	protected boolean issink;     				// [boolean]
	/** @y.exclude */ 	protected Double [][] cumulative_density;	// [veh] 	numEnsemble x numVehTypes
	/** @y.exclude */ 	protected Double [][] cumulative_inflow;	// [veh] 	numEnsemble x numVehTypes
	/** @y.exclude */ 	protected Double [][] cumulative_outflow;	// [veh] 	numEnsemble x numVehTypes
	       
	/////////////////////////////////////////////////////////////////////
	// protected default constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected Link(){}
	
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected void reset_cumulative(){
		int n1 = myNetwork.myScenario.numEnsemble;
		int n2 = myNetwork.myScenario.getNumVehicleTypes();
    	cumulative_density = SiriusMath.zeros(n1,n2);
    	cumulative_inflow  = SiriusMath.zeros(n1,n2);
    	cumulative_outflow = SiriusMath.zeros(n1,n2);
	}

	/** @y.exclude */
	protected boolean registerFlowController(Controller c,int index){
		if(myFlowController!=null)
			return false;
		else{
			myFlowController = c;
			control_maxflow_index = index;
			return true;
		}
	}

	/** @y.exclude */
	protected boolean registerSpeedController(Controller c,int index){
		if(mySpeedController!=null)
			return false;
		else{
			mySpeedController = c;
			control_maxspeed_index = index;
			return true;
		}
	}
	
	/** @y.exclude */
	protected boolean deregisterFlowController(Controller c){
		if(myFlowController!=c)
			return false;
		else{
			myFlowController = null;			
			return true;
		}
	}

	/** @y.exclude */
	protected boolean deregisterSpeedController(Controller c){
		if(mySpeedController!=c)
			return false;
		else{
			mySpeedController = null;			
			return true;
		}
	}

	/** @y.exclude */
	protected FundamentalDiagram currentFD(int ensemble){
		if(activeFDevent)
			return FDfromEvent;
		else
			return FDfromProfile[ensemble];
	}
	
	/** @y.exclude */
    protected void setFundamentalDiagramProfile(FundamentalDiagramProfile fdp){
    	if(fdp==null)
    		return;
    	myFDprofile = fdp;
    }

	/** @throws SiriusException 
	 * @y.exclude */
    protected void setFundamentalDiagramFromProfile(FundamentalDiagram fd) throws SiriusException{
    	if(fd==null)
    		return;
    	
    	// sample the fundamental digram
    	for(int e=0;e<myNetwork.myScenario.numEnsemble;e++)
    		FDfromProfile[e] = fd.perturb();
    	
//    	if(!activeFDevent)				
//    		FD = FDfromProfile;				// point to the profile
    }

	/** @throws SiriusException 
	 * @y.exclude */
    protected void activateFundamentalDiagramEvent(com.relteq.sirius.jaxb.FundamentalDiagram fd) throws SiriusException {
    	if(fd==null)
    		throw new SiriusException("Null parameter.");
    	
    	FDfromEvent = new FundamentalDiagram(this,currentFD(0));		// copy current FD 
    	// note: we are copying from the zeroth FD for simplicity. The alternative is to 
    	// carry numEnsemble event FDs.
    	FDfromEvent.copyfrom(fd);			// replace values with those defined in the event
    	
    	SiriusErrorLog.clearErrorMessage();
    	FDfromEvent.validate();
		if(SiriusErrorLog.haserror())
			throw new SiriusException("Fundamental diagram event could not be validated.");
		
		activeFDevent = true;
    }

	/** @throws SiriusException 
	 * @y.exclude */
    protected void revertFundamentalDiagramEvent() throws SiriusException{
    	if(!activeFDevent)
    		return;
    	activeFDevent = false;
//		FD = FDfromProfile;				// point the fd back at the profile    	
    }

	/** @throws SiriusException 
	 * @y.exclude */
	protected void set_Lanes(double newlanes) throws SiriusException{
		for(int e=0;e<myNetwork.myScenario.numEnsemble;e++)
			if(getDensityJamInVeh(e)*newlanes/get_Lanes() < getTotalDensityInVeh(e))
				throw new SiriusException("ERROR: Lanes could not be set.");

		myFDprofile.set_Lanes(newlanes);	// adjust present and future fd's
		for(int e=0;e<myNetwork.myScenario.numEnsemble;e++)
			FDfromProfile[e].setLanes(newlanes);
//		FD.setLanes(newlanes);
		_lanes = newlanes;					// adjust local copy of lane count
	}
	
	// this is used by CapacityProfile only.
	// no FDs in this link may have capacities that exceed c.
	protected void setCapacityFromVeh(double c) {
		for(FundamentalDiagram fd : FDfromProfile)
			fd._capacity = fd._capacity<c ? fd._capacity : c;
		if(FDfromEvent!=null)
			FDfromEvent._capacity = FDfromEvent._capacity<c ? FDfromEvent._capacity : c;
	}
	
	/////////////////////////////////////////////////////////////////////
	// supply and demand calculation
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected void updateOutflowDemand(){
        
		int numVehicleTypes = myNetwork.myScenario.getNumVehicleTypes();
		
		double totaldensity;
        double totaloutflow;
        double control_maxspeed;
        double control_maxflow;
        
        FundamentalDiagram FD;
        
        for(int e=0;e<myNetwork.myScenario.numEnsemble;e++){

        	FD = currentFD(e);
        	
            totaldensity = SiriusMath.sum(density[e]);

            // case empty link
            if( SiriusMath.lessorequalthan(totaldensity,0d) ){
            	outflowDemand[e] =  SiriusMath.zeros(numVehicleTypes);        		
            	continue;
            }

            // compute total flow leaving the link in the absence of flow control
            if( totaldensity < FD.getDensityCriticalInVeh() ){
            	if(mySpeedController!=null && mySpeedController.ison){
            		// speed control sets a bound on freeflow speed
                	control_maxspeed = mySpeedController.control_maxspeed[control_maxspeed_index];
            		totaloutflow = totaldensity * Math.min(FD.getVfNormalized(),control_maxspeed);	
            	}
            	else
            		totaloutflow = totaldensity * FD.getVfNormalized();
            }
            else{
            	totaloutflow = Math.max(FD._getCapacityInVeh()-FD._getCapacityDropInVeh(),0d);
                if(mySpeedController!=null && mySpeedController.ison){	// speed controller
                	control_maxspeed = mySpeedController.control_maxspeed[control_maxspeed_index];
                	totaloutflow = Math.min(totaloutflow,control_maxspeed*FD.getDensityCriticalInVeh());
                }
            }

            // flow controller
            if(myFlowController!=null && myFlowController.ison){
            	control_maxflow = myFlowController.control_maxflow[control_maxflow_index];
            	totaloutflow = Math.min( totaloutflow , control_maxflow );
            }    

            // flow uncertainty model
            if(myNetwork.myScenario.has_flow_unceratinty){

            	double delta_flow=0.0;
            	double std_dev_flow = myNetwork.myScenario.std_dev_flow;
	            
				switch(myNetwork.myScenario.uncertaintyModel){
				case uniform:
					delta_flow = SiriusMath.sampleZeroMeanUniform(std_dev_flow);
					break;
		
				case gaussian:
					delta_flow = SiriusMath.sampleZeroMeanGaussian(std_dev_flow);
					break;
				}
	            
				totaloutflow = Math.max( 0d , totaloutflow + delta_flow );
				totaloutflow = Math.min( totaloutflow , totaldensity );
            }

            // split among types
            outflowDemand[e] = SiriusMath.times(density[e],totaloutflow/totaldensity);
        }

        return;
    }

	/** @y.exclude */
    protected void updateSpaceSupply(){
		double totaldensity;
		FundamentalDiagram FD;
    	for(int e=0;e<myNetwork.myScenario.numEnsemble;e++){
    		FD = currentFD(e);
        	totaldensity = SiriusMath.sum(density[e]);
            spaceSupply[e] = FD.getWNormalized()*(FD._getDensityJamInVeh() - totaldensity);
            spaceSupply[e] = Math.min(spaceSupply[e],FD._getCapacityInVeh());
            
            // flow uncertainty model
            if(myNetwork.myScenario.has_flow_unceratinty){
            	double delta_flow=0.0;
            	double std_dev_flow = myNetwork.myScenario.std_dev_flow;
	            
				switch(myNetwork.myScenario.uncertaintyModel){
				case uniform:
					delta_flow = SiriusMath.sampleZeroMeanUniform(std_dev_flow);
					break;
		
				case gaussian:
					delta_flow = SiriusMath.sampleZeroMeanGaussian(std_dev_flow);
					break;
				}
				spaceSupply[e] = Math.max( 0d , spaceSupply[e] + delta_flow );
				spaceSupply[e] = Math.min( spaceSupply[e] , FD._getDensityJamInVeh() - totaldensity);
            }
    	}
    }
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////    

	/** @y.exclude */
	protected void populate(Network myNetwork) {

        this.myNetwork = myNetwork;
        
        // link type
        this.myType = Link.Type.valueOf(getType());
        
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
	}

	/** @y.exclude */
	protected void validate() {
		
		if(!issource && begin_node==null)
			SiriusErrorLog.addError("Incorrect begin node id=" + getBegin().getNodeId() + " in link id=" + getId() + ".");

		if(!issink && end_node==null)
			SiriusErrorLog.addError("Incorrect e d node id=" + getEnd().getNodeId() + " in link id=" + getId() + ".");
		
		if(_length<=0)
			SiriusErrorLog.addError("Non-positive length in link id=" + getId() + ".");
		
		if(_lanes<=0)
			SiriusErrorLog.addError("Non-positive number of lanes in link id=" + getId() + ".");		
	}

	/** @y.exclude */
	protected void resetState(Scenario.ModeType simulationMode) {
		
		Scenario myScenario = myNetwork.myScenario;
		
		int n1 = myScenario.numEnsemble;
		int n2 = myScenario.getNumVehicleTypes();
		
		switch(simulationMode){
		
		case warmupFromZero:			// in warmupFromZero mode the simulation start with an empty network
			density = SiriusMath.zeros(n1,n2);
			break;

		case warmupFromIC:				// in warmupFromIC and normal modes, the simulation starts 
		case normal:					// from the initial density profile 
			density = new Double[n1][n2];
			for(int i=0;i<n1;i++)
				if(myScenario.getInitialDensitySet()!=null)
					density[i] = ((InitialDensitySet)myScenario.getInitialDensitySet()).getDensityForLinkIdInVeh(myNetwork.getId(),getId());	
				else 
					density[i] = SiriusMath.zeros(myScenario.getNumVehicleTypes());
			break;
			
		default:
			break;
				
		}

		// reset other quantities
        inflow 				= SiriusMath.zeros(n1,n2);
        outflow 			= SiriusMath.zeros(n1,n2);
        sourcedemand 		= SiriusMath.zeros(n2);
        outflowDemand 		= SiriusMath.zeros(n1,n2);
        spaceSupply 		= SiriusMath.zeros(n1);
        
        // for correct export of initial condition
        cumulative_density 	= SiriusMath.makecopy(density);
        cumulative_inflow 	= SiriusMath.zeros(n1,n2);
        cumulative_outflow 	= SiriusMath.zeros(n1,n2);

		return;
	}

	/** @y.exclude */
	protected void resetLanes(){
		_lanes = getLanes().doubleValue();
	}

	/** @y.exclude */
	protected void resetFD(){
//    	FD = new FundamentalDiagram(this);
 //       FD.settoDefault();		// set to default
		FDfromProfile = new FundamentalDiagram [myNetwork.myScenario.numEnsemble];
    	activeFDevent = false;
	}

	/** @y.exclude */
	protected void update() {
		
        if(issink)
            outflow = outflowDemand;
        
        if(issource){
        	for(int e=0;e<this.myNetwork.myScenario.numEnsemble;e++)
        		inflow[e] = sourcedemand.clone();
        }
                
        for(int e=0;e<myNetwork.myScenario.numEnsemble;e++){
        	  for(int j=0;j<myNetwork.myScenario.getNumVehicleTypes();j++){
              	density[e][j] += inflow[e][j] - outflow[e][j];
              	cumulative_density[e][j] += density[e][j];
              	cumulative_inflow[e][j] += inflow[e][j];
              	cumulative_outflow[e][j] += outflow[e][j];
              }
        }
	}

	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////
	
	// Link type ........................
	
	public Link.Type getMyType() {
		return myType;
	}
	
	public static boolean isFreewayType(Link link){
		
		if(link==null)
			return false;
		
		Link.Type linktype = link.getMyType();
		
		return  linktype.compareTo(Link.Type.intersection_approach)!=0 &&
				linktype.compareTo(Link.Type.offramp)!=0 &&
				linktype.compareTo(Link.Type.onramp)!=0 &&
				linktype.compareTo(Link.Type.street)!=0;		
	}
	
	
	// Link geometry ....................
	
	/** network that contains this link */
	public Network getMyNetwork() {
		return myNetwork;
	}

	/** upstream node of this link  */
	public Node getBegin_node() {
		return begin_node;
	}

	/** downstream node of this link */
	public Node getEnd_node() {
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
	 * @param ensemble Ensemble number 
	 * @return number of vehicles of each type in the link. <code>null</code> if something goes wrong.
	 */
	public Double[] getDensityInVeh(int ensemble) {
		try{
			return density[ensemble].clone();
		} catch(Exception e){
			return null;
		}
	}

	/** Total of vehicles in normalized units (vehicles/link). 
	 * The return value equals the sum of {@link Link#getDensityInVeh}.
	 * @return total number of vehicles in the link. 0 if something goes wrong.
	 */
	public double getTotalDensityInVeh(int ensemble) {
		try{
			if(density!=null)
				return SiriusMath.sum(density[ensemble]);
			else
				return 0d;
		} catch(Exception e){
			return 0d;
		}
	}
	
	/** Total of vehicles in (vehicles/mile). 
	 * @return total density of vehicles in the link. 0 if something goes wrong.
	 */
	public double getTotalDensityInVPM(int ensemble) {
		return getTotalDensityInVeh(ensemble)/_length;
	}
	
	/** Number of vehicles per vehicle type exiting the link 
	 * during the current time step. The return array is indexed by 
	 * vehicle type in the order given in the <code>settings</code> 
	 * portion of the input file. 
	 * @return array of exiting flows per vehicle type. <code>null</code> if something goes wrong.
	 */
	public Double[] getOutflowInVeh(int ensemble) {
		try{
			return outflow[ensemble].clone();
		} catch(Exception e){
			return null;
		}
	}

	/** Total number of vehicles exiting the link during the current
	 * time step.  The return value equals the sum of 
	 * {@link Link#getOutflowInVeh}.
	 * @return total number of vehicles exiting the link in one time step. 0 if something goes wrong.
	 * 
	 */
	public double getTotalOutflowInVeh(int ensemble) {
		try{
			return SiriusMath.sum(outflow[ensemble]);
		} catch(Exception e){
			return 0d;
		}
	}

	/** Number of vehicles per vehicle type entering the link 
	 * during the current time step. The return array is indexed by 
	 * vehicle type in the order given in the <code>settings</code> 
	 * portion of the input file. 
	 * @return array of entering flows per vehicle type. <code>null</code> if something goes wrong.
	 */
	public Double[] getInflowInVeh(int ensemble) {
		try{
			return inflow[ensemble].clone();
		} catch(Exception e){
			return null;
		}
	}

	/** Total number of vehicles entering the link during the current
	 * time step.  The return value equals the sum of 
	 * {@link Link#getInflowInVeh}.
	 * @return total number of vehicles entering the link in one time step. 0 if something goes wrong.
	 * 
	 */
	public double getTotalInlowInVeh(int ensemble) {
		try{
			return SiriusMath.sum(inflow[ensemble]);
		} catch(Exception e){
			return 0d;
		}
	}

	/** Average speed of traffic in the link in mile/hour. 
	 * The return value is computed by dividing the total outgoing 
	 * link flow by the total link density. 
	 * @return average link speed. 0  if something goes wrong.
	 */
	public double computeSpeedInMPH(int ensemble){
		try{
			double totaldensity = SiriusMath.sum(density[ensemble]);
			double speed;
			if( SiriusMath.greaterthan(totaldensity,0d) )
				speed = SiriusMath.sum(outflow[ensemble])/totaldensity;
			else
				speed = currentFD(ensemble).getVfNormalized();
			return speed*_length/myNetwork.myScenario.getSimDtInHours();
		} catch(Exception e){
			return 0d;
		}
	}

	// Fundamental diagram ....................
	
	/** Jam density in vehicle/link. */
	public double getDensityJamInVeh(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD._getDensityJamInVeh();
	}

	/** Critical density in vehicle/link. */
	public double getDensityCriticalInVeh(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else		
			return FD.getDensityCriticalInVeh();
	}

	/** Capacity drop in vehicle/simulation time step */
	public double getCapacityDropInVeh(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD._getCapacityDropInVeh();
	}

	/** Capacity in vehicle/simulation time step */
	public double getCapacityInVeh(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD._getCapacityInVeh();
	}

	/** Jam density in vehicle/mile/lane. */
	public double getDensityJamInVPMPL(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD._getDensityJamInVeh()/getLengthInMiles()/_lanes;
	}

	/** Critical density in vehicle/mile/lane. */
	public double getDensityCriticalInVPMPL(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD.getDensityCriticalInVeh()/getLengthInMiles()/_lanes;
	}

	/** Capacity drop in vehicle/hr/lane. */
	public double getCapacityDropInVPHPL(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD._getCapacityDropInVeh()/myNetwork.myScenario.getSimDtInHours()/_lanes;
	}

	/** Capacity in vehicles per hour. */
	public double getCapacityInVPH(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD._getCapacityInVeh() / myNetwork.myScenario.getSimDtInHours();
	}

	/** Capacity in vehicle/hr/lane. */
	public double getCapacityInVPHPL(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD._getCapacityInVeh()/myNetwork.myScenario.getSimDtInHours()/_lanes;
	}

	/** Freeflow speed in normalized units (link/time step). */
	public double getNormalizedVf(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD.getVfNormalized();
	}

	/** Freeflow speed in mile/hr. */
	public double getVfInMPH(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD.getVfNormalized()*getLengthInMiles()/myNetwork.myScenario.getSimDtInHours();
	}

	/** Congestion wave speed in normalized units (link/time step). */
	public double getNormalizedW(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD.getWNormalized();
	}

	/** Congestion wave speed in mile/hr. */
	public double getWInMPH(int ensemble) {
		FundamentalDiagram FD = currentFD(ensemble);
		if(FD==null)
			return Double.NaN;
		else
			return FD.getWNormalized()*getLengthInMiles()/myNetwork.myScenario.getSimDtInHours();
	}
	
	/** Set input flow for source links.
	 *  [This is API call is being made available for implementation of path-based DTA.
	 *  The goal is to replace it later with an internal solution.]
	 */
	public void setSourcedemandFromVeh(Double[] sourcedemand) {
		this.sourcedemand = sourcedemand;		
	}
	
	/** Replace link density with given values.
	 *  [This is API call is being made available for implementation of particle filtering.
	 *  Use with caution.]
	 */
	public void overrideDensityWithVeh(Double[] x,int ensemble){
		if(ensemble<0 || ensemble>=density.length)
			return;
		if(x.length!=density[0].length)
			return;
		for(int i=0;i<x.length;i++)
			density[ensemble][i] = x[i];
	}

}
