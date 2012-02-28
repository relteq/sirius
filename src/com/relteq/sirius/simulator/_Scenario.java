/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/


package com.relteq.sirius.simulator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Random;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.relteq.sirius.jaxb.*;

/** This class provides methods for loading, manipulating, and running scenarios. 
 * <p>
 * A scenario is a collection of,
 * <ul>
 * <li> networks (nodes, links, sensors, and signals), </li>
 * <li> network connections, </li>
 * <li> initial conditions, </li>
 * <li> weaving factor profiles, </li>
 * <li> split ratio profiles, </li>
 * <li> downstream boundary conditions, </li> 
 * <li> events, </li>
 * <li> controllers, </li>
 * <li> fundamental diagram profiles, </li>
 * <li> path segments, </li>
 * <li> decision points, </li>
 * <li> routes, </li>
 * <li> background demand profiles, and </li>
 * <li> OD demand profiles. </li>
*  </ul>
* @author Gabriel Gomes
* @version VERSION NUMBER
*/
public final class _Scenario extends com.relteq.sirius.jaxb.Scenario {

	/** @y.exclude */	protected Clock clock;
	/** @y.exclude */	protected String configfilename;
	/** @y.exclude */	protected double outdt;				// [sec] output sampling time
	/** @y.exclude */	protected double timestart;			// [sec] start of the simulation
	/** @y.exclude */	protected double timeend;			// [sec] end of the simulation
	/** @y.exclude */	protected OutputWriter outputwriter = null;
	/** @y.exclude */	protected String outputfile_density;
	/** @y.exclude */	protected String outputfile_outflow;
	/** @y.exclude */	protected String outputfile_inflow;
	/** @y.exclude */	protected Random random = new Random();
	/** @y.exclude */	protected _Scenario.ModeType simulationMode;
	/** @y.exclude */	protected _Scenario.UncertaintyType uncertaintyModel;
	/** @y.exclude */	protected int numVehicleTypes;			// number of vehicle types
	/** @y.exclude */	protected boolean global_control_on;	// global control switch
	/** @y.exclude */	protected double global_demand_knob;	// scale factor for all demands
	/** @y.exclude */	protected double simdtinseconds;		// [sec] simulation time step 
	/** @y.exclude */	protected double simdtinhours;			// [hr]  simulation time step 	
	/** @y.exclude */	protected boolean isrunning=false;		// true when the simulation is running
	/** @y.exclude */	protected _ControllerSet _controllerset = new _ControllerSet();
	/** @y.exclude */	protected _EventSet _eventset = new _EventSet();	// holds time sorted list of events
	/** @y.exclude */	protected static enum ModeType {  normal, 
								    					  warmupFromZero , 
								    					  warmupFromIC };
	/** @y.exclude */	protected static enum UncertaintyType { uniform, 
          														gaussian }
	
	/////////////////////////////////////////////////////////////////////
	// protected constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected _Scenario(){}
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	/** populate methods copy data from the jaxb state to extended objects. 
	 * They do not throw exceptions or report mistakes. Data errors should be 
	 * circumvented and left for the validation to report.
	 * @throws SiriusException 
	 * @y.exclude
	 */
	protected void populate() throws SiriusException {
		
		// network list
		if(getNetworkList()!=null)
			for( Network network : getNetworkList().getNetwork() )
				((_Network) network).populate(this);
	
		// split ratio profile set (must follow network)
		if(getSplitRatioProfileSet()!=null)
			((_SplitRatioProfileSet) getSplitRatioProfileSet()).populate(this);
		
		// boundary capacities (must follow network)
		if(getDownstreamBoundaryCapacitySet()!=null)
			for( CapacityProfile capacityProfile : getDownstreamBoundaryCapacitySet().getCapacityProfile() )
				((_CapacityProfile) capacityProfile).populate(this);

		if(getDemandProfileSet()!=null)
			((_DemandProfileSet) getDemandProfileSet()).populate(this);
		
		// fundamental diagram profiles 
		if(getFundamentalDiagramProfileSet()!=null)
			for(FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
				((_FundamentalDiagramProfile) fd).populate(this);
		
		// initial density profile 
		if(getInitialDensityProfile()!=null)
			((_InitialDensityProfile) getInitialDensityProfile()).populate(this);
		
		// initialize controllers and events
		_controllerset.populate(this);
		_eventset.populate(this);
		
	}

	/** Prepare scenario for simulation:
	 * set the state of the scenario to the initial condition
	 * sample profiles
	 * open output files
	 * @return success		A boolean indicating whether the scenario was successfuly reset.
	 * @throws SiriusException 
	 * @y.exclude
	 */
	protected boolean reset() throws SiriusException {
		
		// reset the clock
		clock.reset();
		
		// reset network
		for(Network network : getNetworkList().getNetwork())
			((_Network)network).reset();
		
		// reset demand profiles
		if(getDemandProfileSet()!=null)
			((_DemandProfileSet)getDemandProfileSet()).reset();

		// reset fundamental diagrams
		for(FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
			((_FundamentalDiagramProfile)fd).reset();
		
		// reset controllers
		global_control_on = true;
		_controllerset.reset();

		// reset events
		_eventset.reset();
		
		return true;
		
	}	
	
	/** 
	 * @throws SiriusException 
	 * @y.exclude
	 */
	protected void update() throws SiriusException {	

        // sample profiles .............................	
    	if(getDownstreamBoundaryCapacitySet()!=null)
        	for(CapacityProfile capacityProfile : getDownstreamBoundaryCapacitySet().getCapacityProfile())
        		((_CapacityProfile) capacityProfile).update();

    	if(getDemandProfileSet()!=null)
    		((_DemandProfileSet)getDemandProfileSet()).update();

    	if(getSplitRatioProfileSet()!=null)
    		((_SplitRatioProfileSet) getSplitRatioProfileSet()).update();        		

    	if(getFundamentalDiagramProfileSet()!=null)
        	for(FundamentalDiagramProfile fdProfile : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
        		((_FundamentalDiagramProfile) fdProfile).update();
    	
        // update controllers
    	if(global_control_on)
    		_controllerset.update();

    	// update events
    	_eventset.update();
    	
        // update the network state......................
		for(Network network : getNetworkList().getNetwork())
			((_Network) network).update();
        
	}

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	/** Retrieve a network with a given id.
	 * @param id The string id of the network
	 * @return The corresponding network if it exists, <code>null</code> otherwise.
	 * 
	 */
	protected _Network getNetworkWithId(String id){
		if(id==null)
			return null;
		if(getNetworkList()==null)
			return null;
		id.replaceAll("\\s","");
		for(Network network : getNetworkList().getNetwork()){
			if(network.getId().equals(id))
				return (_Network) network;
		}
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	// excluded from API
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	public Integer [] getVehicleTypeIndices(VehicleTypeOrder vtypeorder){
		
		Integer [] vehicletypeindex;
		
		// single vehicle types in setting and no vtypeorder, return 0
		if(vtypeorder==null && numVehicleTypes==1){
			vehicletypeindex = new Integer[numVehicleTypes];
			vehicletypeindex[0]=0;
			return vehicletypeindex;
		}
		
		// multiple vehicle types in setting and no vtypeorder, return 0...n
		if(vtypeorder==null && numVehicleTypes>1){
			vehicletypeindex = new Integer[numVehicleTypes];
			for(int i=0;i<numVehicleTypes;i++)
				vehicletypeindex[i] = i;	
			return vehicletypeindex;	
		}
		
		// vtypeorder is not null
		int numTypesInOrder = vtypeorder.getVehicleType().size();
		int i,j;
		vehicletypeindex = new Integer[numTypesInOrder];
		for(i=0;i<numTypesInOrder;i++)
			vehicletypeindex[i] = -1;			

		if(getSettings()==null)
			return vehicletypeindex;

		if(getSettings().getVehicleTypes()==null)
			return vehicletypeindex;
		
		for(i=0;i<numTypesInOrder;i++){
			List<VehicleType> vt = getSettings().getVehicleTypes().getVehicleType();
			for(j=0;j<vt.size();j++){
				if(vt.get(j).getName().equals(name)){
					vehicletypeindex[i] =  j;
					break;
				}
			}			
		}
		return vehicletypeindex;
	}
	
	/** @y.exclude */
	public boolean validate() {
					
		// check that outdt is a multiple of simdt
		if(!SiriusMath.isintegermultipleof(outdt,simdtinseconds)){
//			Utils.addErrorMessage("Aborting: outdt must be an interger multiple of simulation dt.");
//			Utils.printErrorMessage();
			return false;
		}
		
		// validate network
		if( getNetworkList()!=null)
			for(Network network : getNetworkList().getNetwork())
				if(!((_Network)network).validate())
					return false;

		// validate initial density profile
		if(getInitialDensityProfile()!=null)
			if(!((_InitialDensityProfile) getInitialDensityProfile()).validate())
				return false;

		// validate capacity profiles	
		if(getDownstreamBoundaryCapacitySet()!=null)
			for(CapacityProfile capacityProfile : getDownstreamBoundaryCapacitySet().getCapacityProfile())
				if(!((_CapacityProfile)capacityProfile).validate())
					return false;
		
		// validate demand profiles
		if(getDemandProfileSet()!=null)
			if(!((_DemandProfileSet)getDemandProfileSet()).validate())
				return false;

		// validate split ratio profiles
		if(getSplitRatioProfileSet()!=null)
			if(!((_SplitRatioProfileSet)getSplitRatioProfileSet()).validate())
				return false;

		// validate fundamental diagram profiles
		if(getFundamentalDiagramProfileSet()!=null)
			for(FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
				if(!((_FundamentalDiagramProfile)fd).validate())
					return false;

		// validate controllers
		if(!_controllerset.validate())
			return false;

//		// validate events
//		if(!_eventset.validate())
//			return false;

		return true;
	}
	
	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////
	
	/** Run the scenario <code>numRepetitions</code> times.
	 * 
	 * <p> The scenario is reset and run multiple times in sequence. All
	 * probabilistic quantities are resampled between runs. Output files are
	 * created with a common prefix with the index of the simulation appended to 
	 * the file name.
	 * 
	 * @param numRepetitions 	The integer number of simulations to run.
	 */
	public void run(int numRepetitions){
		
		try{
			
	        isrunning = true;
	
			// loop through simulation runs ............................
			for(int i=0;i<numRepetitions;i++){
				
				// reset scenario
				if(!reset()){
					SiriusErrorLog.setErrorHeader("Reset failed.");
					SiriusErrorLog.printErrorMessage();
					return;
				}
	
				// open output files
		        if(simulationMode==_Scenario.ModeType.normal){
		        	outputwriter = new OutputWriter(this,SiriusMath.round(outdt/simdtinseconds));
					try {
						outputwriter.open(outputfile_density,outputfile_outflow,outputfile_inflow);
					} catch (FileNotFoundException e) {
						SiriusErrorLog.addErrorMessage("Unable to open output file.");
						SiriusErrorLog.printErrorMessage();
						return;
					}
		        }
		        	
		        // write initial condition
		        //Utils.outputwriter.recordstate(Utils.clock.getT(),false);
		        
		        while( !clock.expired() ){
	
		            // update time (before write to output)
		        	clock.advance();
		        	      	        	
		        	// update scenario
		        	update();
	
		            // update time (before write to output)
		            // Utils.clock.advance();
		        	
		            // write output .............................
		            if(simulationMode==ModeType.normal)
		            	//if(Utils.clock.istimetosample(Utils.outputwriter.getOutsteps()))
			        	if((clock.getCurrentstep()==1) || ((clock.getCurrentstep()-1)%outputwriter.getOutsteps()==0))
			        		outputwriter.recordstate(clock.getT(),true);
		        }
	        
	            // close output files
		        if(simulationMode==_Scenario.ModeType.normal)
		        	outputwriter.close();
	
				// or save scenario (in warmup mode)
		        if(simulationMode==_Scenario.ModeType.warmupFromIC || simulationMode==_Scenario.ModeType.warmupFromZero){
	//	    		String outfile = "C:\\Users\\gomes\\workspace\\auroralite\\data\\config\\out.xml";
	//	    		Utils.save(scenario, outfile);
		        }
			}
		
	        isrunning = false;
	        
	        
		}
		catch(SiriusException e){
			SiriusErrorLog.addErrorMessage("ERROR CAUGHT");
			return;
		}
		
	}

	/** Save the scenario to XML.
	 * 
	 * @param filename The name of the configuration file.
	 */
	public void saveToXML(String filename){
        try {
        	JAXBContext context = JAXBContext.newInstance("aurora.jaxb");
        	Marshaller m = context.createMarshaller();
        	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        	m.marshal(this,new FileOutputStream(filename));
        } catch( JAXBException je ) {
            je.printStackTrace();
            return;
        } catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
        }
	}
	
	/** Current simulation time in seconds.
	 * @return Simulation time in seconds after midnight.
	 */
	public double getTime() {
		if(clock==null)
			return Double.NaN;
		return clock.getT();
	}

	/** Current simulation time step.
	 * @return	Integer number of time steps since the start of the simulation. 
	 */
	public int getCurrentTimeStep() {
		if(clock==null)
			return 0;
		return clock.getCurrentstep();
	}

	/** Number of vehicle types included in the scenario.
	 * @return Integer number of vehicle types
	 */
	public int getNumVehicleTypes() {
		return numVehicleTypes;
	}

	/** Vehicle type names.
	 * @return	Array of strings with the names of the vehicles types.
	 */
	public String [] getVehicleTypeNames(){
		String [] vehtypenames = new String [numVehicleTypes];
		if(getSettings()==null || getSettings().getVehicleTypes()==null)
			vehtypenames[0] = Defaults.vehicleType;
		else
			for(int i=0;i<getSettings().getVehicleTypes().getVehicleType().size();i++)
				vehtypenames[i] = getSettings().getVehicleTypes().getVehicleType().get(i).getName();
		return vehtypenames;
	}
	
	/** Vehicle type weights.
	 * @return	Array of doubles with the weights of the vehicles types.
	 */
	public Double [] getVehicleTypeWeights(){
		Double [] vehtypeweights = new Double [numVehicleTypes];
		if(getSettings()==null || getSettings().getVehicleTypes()==null)
			vehtypeweights[0] = 1d;
		else
			for(int i=0;i<getSettings().getVehicleTypes().getVehicleType().size();i++)
				vehtypeweights[i] = getSettings().getVehicleTypes().getVehicleType().get(i).getWeight().doubleValue();
		return vehtypeweights;
	}
	
	
	/** Vehicle type index from name
	 * @return integer index of the vehicle type.
	 */
	public int getVehicleTypeIndex(String name){
		String [] vehicleTypeNames = getVehicleTypeNames();
		if(vehicleTypeNames==null)
			return 0;
		if(vehicleTypeNames.length<=1)
			return 0;
		for(int i=0;i<vehicleTypeNames.length;i++)
			if(vehicleTypeNames[i].equals(name))
				return i;
		return -1;
	}
	
	/** Size of the simulation time step in seconds.
	 * @return Simulation time step in seconds. 
	 */
	public double getSimDtInSeconds() {
		return simdtinseconds;
	}

	/** Size of the simulation time step in hours.
	 * @return Simulation time step in hours. 
	 */
	public double getSimDtInHours() {
		return simdtinhours;
	}

	/** Size of the output time step in seconds.
	 * @return Output time step in seconds. 
	 */
	public double getOutDt() {
		return outdt;
	}

	/** Start time of the simulation.
	 * @return Start time in seconds. 
	 */
	public double getTimeStart() {
		return timestart;
	}

	/** End time of the simulation.
	 * @return End time in seconds. 
	 * @return			XXX
	 */
	public double getTimeEnd() {
		return timeend;
	}

	/** Get a reference to a controller by its name.
	 * <p> This method will soon be replaced with id search, since names
	 * are not guaranteed to be unique. 
	 * @param name Name of the controller.
	 * @return A reference to the controller if it exists, <code>null</code> otherwise.
	 */
	public _Controller getControllerWithName(String name){
		if(_controllerset==null)
			return null;
		for(_Controller c : _controllerset.get_Controllers()){
			if(c.name.equals(name))
				return c;
		}
		return null;
	}

	/** Get a reference to a node by its composite id.
	 * 
	 * @param network_id String id of the network containing the node. 
	 * @param id String id of the node. 
	 * @return Reference to the node if it exists, <code>null</code> otherwise
	 */
	public _Node getNodeWithCompositeId(String network_id,String id){
		if(getNetworkList()==null)
			return null;
		_Network network = getNetworkWithId(network_id);
		if(network==null)
			if(getNetworkList().getNetwork().size()==1)
				return ((_Network) getNetworkList().getNetwork().get(0)).getNodeWithId(id);
			else
				return null;
		else	
			return network.getNodeWithId(id);
	}

	/** Get a reference to a link by its composite id.
	 * 
	 * @param network_id String id of the network containing the link. 
	 * @param id String id of the link. 
	 * @return Reference to the link if it exists, <code>null</code> otherwise
	 */
	public _Link getLinkWithCompositeId(String network_id,String id){
		if(getNetworkList()==null)
			return null;
		_Network network = getNetworkWithId(network_id);
		if(network==null)
			if(getNetworkList().getNetwork().size()==1)
				return ((_Network) getNetworkList().getNetwork().get(0)).getLinkWithId(id);
			else
				return null;
		else	
			return network.getLinkWithId(id);
	}

	/** Get a reference to a sensor by its composite id.
	 * 
	 * @param network_id String id of the network containing the sensor. 
	 * @param id String id of the sensor. 
	 * @return Reference to the sensor if it exists, <code>null</code> otherwise
	 */
	public _Sensor getSensorWithCompositeId(String network_id,String id){
		if(getNetworkList()==null)
			return null;
		_Network network = getNetworkWithId(network_id);
		if(network==null)
			if(getNetworkList().getNetwork().size()==1)
				return ((_Network) getNetworkList().getNetwork().get(0)).getSensorWithId(id);
			else
				return null;
		else	
			return network.getSensorWithId(id);
	}

	/** Add a controller to the scenario.
	 * 
	 * <p>Controllers can only be added if a) the scenario is not currently running, and
	 * b) the controller is valid. 
	 * @param C The controller
	 * @return <code>true</code> if the controller was successfully added, <code>false</code> otherwise. 
	 */
	public boolean addController(_Controller C){
		if(isrunning)
			return false;
		if(C==null)
			return false;
		if(C.myType==null)
			return false;
		
		// validate
		if(!C.validate())
			return false;
		
		// add
		_controllerset._controllers.add(C);
		
		return true;
	}

	/** Add an event to the scenario.
	 * 
	 * <p>Events are not added if the scenario is running. This method does not validate the event.
	 * @param E The event
	 * @return <code>true</code> if the event was successfully added, <code>false</code> otherwise. 
	 */
	public boolean addEvent(_Event E){
		if(isrunning)
			return false;
		if(E==null)
			return false;
		if(E.myType==null)
			return false;
		
		// add event to list
		_eventset.addEvent(E);
		
		return true;
	}

	/** Add a sensor to the scenario.
	 * 
	 * <p>Sensors can only be added if a) the scenario is not currently running, and
	 * b) the sensor is valid. 
	 * @param S The sensor
	 * @return <code>true</code> if the sensor was successfully added, <code>false</code> otherwise. 
	 */
	public boolean addSensor(_Sensor S){
		if(S==null)
			return false;
		if(S.myType==null)
			return false;
		if(S.myLink==null)
			return false;
		if(S.myLink.myNetwork==null)
			return false;
 
		// validate
		if(!S.validate())
			return false;
		
		// add sensor to list
		S.myLink.myNetwork._sensorlist._sensors.add(S);
		
		return true;
	}

}