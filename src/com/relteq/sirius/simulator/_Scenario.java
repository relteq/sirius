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
	/** @y.exclude */	protected int outsteps;				// [-] number of simulation steps per output step
	/** @y.exclude */	protected double timestart;			// [sec] start of the simulation
	/** @y.exclude */	protected double timeend;			// [sec] end of the simulation
	/** @y.exclude */	protected String outputfileprefix;
	/** @y.exclude */	protected Random random = new Random();
	/** @y.exclude */	protected _Scenario.ModeType simulationMode;
	/** @y.exclude */	protected _Scenario.UncertaintyType uncertaintyModel;
	/** @y.exclude */	protected int numVehicleTypes;			// number of vehicle types
	/** @y.exclude */	protected boolean global_control_on;	// global control switch
	/** @y.exclude */	protected double global_demand_knob;	// scale factor for all demands
	/** @y.exclude */	protected double simdtinseconds;		// [sec] simulation time step 
	/** @y.exclude */	protected double simdtinhours;			// [hr]  simulation time step 	
	/** @y.exclude */	protected boolean scenariolocked=false;		// true when the simulation is running
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
		if(getFundamentalDiagramProfileSet()!=null)
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
			String vtordername = vtypeorder.getVehicleType().get(i).getName();
			List<VehicleType> settingsname = getSettings().getVehicleTypes().getVehicleType();
			for(j=0;j<settingsname.size();j++){
				if(settingsname.get(j).getName().equals(vtordername)){
					vehicletypeindex[i] =  j;
					break;
				}
			}			
		}
		return vehicletypeindex;
	}
	
	/** @y.exclude */
	public boolean validate() {
					
		if(this.simulationMode==null){
			SiriusErrorLog.addErrorMessage("Null simulation mode.");
			return false;
		}
		
		// check that outdt is a multiple of simdt
		if(!SiriusMath.isintegermultipleof(outdt,simdtinseconds)){
			SiriusErrorLog.addErrorMessage("outdt (" + outdt + ") must be an interger multiple of simulation dt (" + simdtinseconds + ").");
			return false;
		}
		
		// validate network
		if( getNetworkList()!=null)
			for(Network network : getNetworkList().getNetwork())
				if(!((_Network)network).validate()){
					SiriusErrorLog.addErrorMessage("Network validation failure.");
					return false;
				}

		// validate initial density profile
		if(getInitialDensityProfile()!=null)
			if(!((_InitialDensityProfile) getInitialDensityProfile()).validate()){
				SiriusErrorLog.addErrorMessage("InitialDensityProfile validation failure.");
				return false;
			}

		// validate capacity profiles	
		if(getDownstreamBoundaryCapacitySet()!=null)
			for(CapacityProfile capacityProfile : getDownstreamBoundaryCapacitySet().getCapacityProfile())
				if(!((_CapacityProfile)capacityProfile).validate()){
					SiriusErrorLog.addErrorMessage("DownstreamBoundaryCapacitySet validation failure.");
					return false;
				}
		
		// validate demand profiles
		if(getDemandProfileSet()!=null)
			if(!((_DemandProfileSet)getDemandProfileSet()).validate()){
				SiriusErrorLog.addErrorMessage("DemandProfileSet validation failure.");
				return false;
			}

		// validate split ratio profiles
		if(getSplitRatioProfileSet()!=null)
			if(!((_SplitRatioProfileSet)getSplitRatioProfileSet()).validate()){
				SiriusErrorLog.addErrorMessage("SplitRatioProfileSet validation failure.");
				return false;
			}

		// validate fundamental diagram profiles
		if(getFundamentalDiagramProfileSet()!=null)
			for(FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
				if(!((_FundamentalDiagramProfile)fd).validate()){
					SiriusErrorLog.addErrorMessage("FundamentalDiagramProfileSet validation failure.");
					return false;
				}

		// validate controllers
		if(!_controllerset.validate())
			return false;

		return true;
	}
	
	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////
	
	/** Run the scenario <code>numRepetitions</code> times, save output to text files.
	 * 
	 * <p> The scenario is reset and run multiple times in sequence. All
	 * probabilistic quantities are re-sampled between runs. Output files are
	 * created with a common prefix with the index of the simulation appended to 
	 * the file name.
	 * 
	 * @param numRepetitions 	The integer number of simulations to run.
	 * @throws SiriusException 
	 */
	public void run(int numRepetitions) throws SiriusException{
		run_internal(numRepetitions,true,false);
	}

	/** Run the scenario once, return the state trajectory.
	 * <p> The scenario is reset and run once. 
	 * @return An object with the history of densities and flows for all links in the scenario.
	 * @throws SiriusException 
	 */
	public SiriusStateTrajectory run() throws SiriusException{
		return run_internal(1,false,true);
	}
	
	/** Advance the simulation <i>n</i> steps.
	 * 
	 * <p> This function moves the simulation forward <i>n</i> time steps and stops.
	 * The first parameter provides the number of time steps to advance. The second parameter
	 * is a boolean that resets the clock and the scenario.  
	 * @param n Number of simulation steps to advance.
	 * @param fromstart <code>true</code> to reset the scenario before advancing, <code>false</code> otherwise. 
	 * @return <code>true</code> if the simulation advanced without problem; <code>false</code> A problem was encountered, or the end of the simulation was reached. 
	 * @throws SiriusException 
	 */
	public boolean advanceNSteps(int n,boolean fromstart) throws SiriusException{
		
		 // Allow only if simulation is running
		 if(!scenariolocked)
			 throw new SiriusException("Lock the scenario first.");
		 
		return advanceNSteps_internal(n,fromstart,false,false,null,null);
	}

	/** Save the scenario to XML.
	 * 
	 * @param filename The name of the configuration file.
	 * @throws SiriusException 
	 */
	public void saveToXML(String filename) throws SiriusException{
        try {
        	JAXBContext context = JAXBContext.newInstance("aurora.jaxb");
        	Marshaller m = context.createMarshaller();
        	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        	m.marshal(this,new FileOutputStream(filename));
        } catch( JAXBException je ) {
        	throw new SiriusException(je.getMessage());
        } catch (FileNotFoundException e) {
        	throw new SiriusException(e.getMessage());
        }
	}
	
	/** Current simulation time in seconds.
	 * @return Simulation time in seconds after midnight.
	 */
	public double getTimeInSeconds() {
		if(clock==null)
			return Double.NaN;
		return clock.getT();
	}
	
	/** Time elapsed since the begining of the simulation in seconds.
	 * @return Simulation time in seconds after start time.
	 */
	public double getTimeElapsedInSeconds() {
		if(clock==null)
			return Double.NaN;
		return clock.getTElapsed();
	}
	
	/** Current simulation time step.
	 * @return	Integer number of time steps since the start of the simulation. 
	 */
	public int getCurrentTimeStep() {
		if(clock==null)
			return 0;
		return clock.getCurrentstep();
	}

	/** Total number of time steps that will be simulated, regardless of the simulation mode.
	 * @return	Integer number of time steps to simulate.
	 */
	public int getTotalTimeStepsToSimulate(){
		 return clock.getTotalSteps();
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

	/** Get a reference to a controller by its id.
	 * @param id Id of the controller.
	 * @return A reference to the controller if it exists, <code>null</code> otherwise.
	 */
	public _Controller getControllerWithId(String id){
		if(_controllerset==null)
			return null;
		for(_Controller c : _controllerset.get_Controllers()){
			if(c.id.equals(id))
				return c;
		}
		return null;
	}
	
	/** Get a reference to an event by its id.
	 * @param id Id of the event.
	 * @return A reference to the event if it exists, <code>null</code> otherwise.
	 */
	public _Event getEventWithId(String id){
		if(_eventset==null)
			return null;
		for(_Event e : _eventset._sortedevents){
			if(e.getId().equals(id))
				return e;
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

	/** Get a reference to a signal by its composite id.
	 * 
	 * @param network_id String id of the network containing the signal. 
	 * @param id String id of the signal. 
	 * @return Reference to the signal if it exists, <code>null</code> otherwise
	 */
	public _Signal getSignalWithCompositeId(String network_id,String id){
		if(getNetworkList()==null)
			return null;
		_Network network = getNetworkWithId(network_id);
		if(network==null)
			if(getNetworkList().getNetwork().size()==1)
				return ((_Network) getNetworkList().getNetwork().get(0)).getSignalWithId(id);
			else
				return null;
		else	
			return network.getSignalWithId(id);
	}
	
	/** Get a reference to a signal by the composite id of its node.
	 * 
	 * @param network_id String id of the network containing the signal. 
	 * @param id String id of the node under the signal. 
	 * @return Reference to the signal if it exists, <code>null</code> otherwise
	 */
	public _Signal getSignalForNodeId(String network_id,String node_id){
		if(getNetworkList()==null)
			return null;
		_Network network = getNetworkWithId(network_id);
		if(network==null)
			if(getNetworkList().getNetwork().size()==1)
				return ((_Network) getNetworkList().getNetwork().get(0)).getSignalWithNodeId(node_id);
			else
				return null;
		else	
			return network.getSignalWithNodeId(node_id);
	}

	/** Add a controller to the scenario.
	 * 
	 * <p>Controllers can only be added if a) the scenario is not currently running, and
	 * b) the controller is valid. 
	 * @param C The controller
	 * @return <code>true</code> if the controller was successfully added, <code>false</code> otherwise. 
	 */
	public boolean addController(_Controller C){
		if(scenariolocked)
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
		if(scenariolocked)
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

	/////////////////////////////////////////////////////////////////////
	// private
	/////////////////////////////////////////////////////////////////////

	private SiriusStateTrajectory run_internal(int numRepetitions,boolean writefiles,boolean returnstate) throws SiriusException{
		
		if(returnstate && numRepetitions>1)
			throw new SiriusException("run with multiple repetitions and returning state not allowed.");
		
		SiriusStateTrajectory state = null;
		
		// lock the scenario
        scenariolocked = true;

		// loop through simulation runs ............................
		for(int i=0;i<numRepetitions;i++){

			OutputWriter outputwriter  = null;
			
			// open output files
	        if( writefiles && simulationMode.compareTo(_Scenario.ModeType.normal)==0 ){
	        	outputwriter = new OutputWriter(this);
				try {
					outputwriter.open(outputfileprefix,String.format("%d",i));
				} catch (FileNotFoundException e) {
					throw new SiriusException("Unable to open output file.");
				}
	        }
	        
	        // allocate state
	        if(returnstate)
	        	state = new SiriusStateTrajectory(this);
	
			// reset the simulation
			advanceNSteps_internal(0,true,writefiles,returnstate,outputwriter,state);
			
			// advance to end of simulation
			while( advanceNSteps_internal(1,false,writefiles,returnstate,outputwriter,state) ){					
			}
			
            // close output files
	        if(writefiles){
	        	if(simulationMode.compareTo(_Scenario.ModeType.normal)==0)
		        	outputwriter.close();
				// or save scenario (in warmup mode)
		        if(simulationMode.compareTo(_Scenario.ModeType.warmupFromIC)==0 || simulationMode.compareTo(_Scenario.ModeType.warmupFromZero)==0 ){
		    		//	    		String outfile = "C:\\Users\\gomes\\workspace\\auroralite\\data\\config\\out.xml";
		    		//	    		Utils.save(scenario, outfile);
		        }
	        }
	        
		}		
        scenariolocked = false;

		return state;
	}
	
	// advance the simulation by n steps.
	// parameters....
	// n: number of steps to advance.
	// doreset: call scenario reset if true
	// writefiles: write result to text files if true
	// returnstate: recored and return the state trajectory if true
	// outputwriter: output writing class 
	// state: state trajectory container
	// returns....
	// true if scenario advanced n steps without error
	// false if scenario reached t_end without error before completing n steps
	// throws ....
	// SiriusException for all errors
	private boolean advanceNSteps_internal(int n,boolean doreset,boolean writefiles,boolean returnstate,OutputWriter outputwriter,SiriusStateTrajectory state) throws SiriusException{
		
		// reset
		if(doreset)
			if(!reset())
				throw new SiriusException("Reset failed.");
        
		// advance n steps
		for(int k=0;k<n;k++){

			// export initial condition
	        if(simulationMode.compareTo(ModeType.normal)==0)
	        	if( clock.getCurrentstep()==0 )
	        		recordstate(writefiles,returnstate,outputwriter,state,false);
	               	
        	// update scenario
        	update();

            // update time (before write to output)
        	clock.advance();
        	
            if(simulationMode.compareTo(ModeType.normal)==0 )
	        	if( clock.getCurrentstep()%outsteps == 0 )
	        		recordstate(writefiles,returnstate,outputwriter,state,true);
        	
        	if(clock.expired())
        		return false;
		}
	      
		return true;
	}
	
	private void recordstate(boolean writefiles,boolean returnstate,OutputWriter outputwriter,SiriusStateTrajectory state,boolean exportflows) throws SiriusException {
		if(writefiles)
			outputwriter.recordstate(clock.getT(),exportflows,outsteps);
		if(returnstate)
			state.recordstate(clock.getCurrentstep(),clock.getT(),exportflows,outsteps);
	}

}