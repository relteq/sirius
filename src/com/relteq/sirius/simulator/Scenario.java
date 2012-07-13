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

/** Load, manipulate, and run scenarios. 
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
public final class Scenario extends com.relteq.sirius.jaxb.Scenario {

	/** @y.exclude */	protected static enum ModeType {  normal, 
		  warmupFromZero , 
		  warmupFromIC };
	/** @y.exclude */	protected static enum UncertaintyType { uniform, 
		  gaussian }

	/** @y.exclude */	protected Clock clock;
	/** @y.exclude */	protected String configfilename;
	/** @y.exclude */	protected Random random = new Random();
	/** @y.exclude */	protected Scenario.UncertaintyType uncertaintyModel;
	/** @y.exclude */	protected int numVehicleTypes;			// number of vehicle types
	/** @y.exclude */	protected boolean global_control_on;	// global control switch
	/** @y.exclude */	protected double global_demand_knob;	// scale factor for all demands
	/** @y.exclude */	protected double simdtinseconds;		// [sec] simulation time step 
	/** @y.exclude */	protected double simdtinhours;			// [hr]  simulation time step 	
	/** @y.exclude */	protected boolean scenariolocked=false;	// true when the simulation is running
	/** @y.exclude */	protected ControllerSet controllerset = new ControllerSet();
	/** @y.exclude */	protected EventSet eventset = new EventSet();	// holds time sorted list of events	
	/** @y.exclude */	protected int numEnsemble;

	/////////////////////////////////////////////////////////////////////
	// protected constructor
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	protected Scenario(){}
	
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
			for( com.relteq.sirius.jaxb.Network network : getNetworkList().getNetwork() )
				((Network) network).populate(this);
		
		// replace jaxb.Sensor with simulator.Sensor
		if(getSensorList()!=null)
			for(int i=0;i<getSensorList().getSensor().size();i++){
				com.relteq.sirius.jaxb.Sensor sensor = getSensorList().getSensor().get(i);
				Sensor.Type myType = Sensor.Type.valueOf(sensor.getType());
				getSensorList().getSensor().set(i,ObjectFactory.createSensorFromJaxb(this,sensor,myType));
			}
		
		if(getSignalList()!=null)
			for(com.relteq.sirius.jaxb.Signal signal : getSignalList().getSignal())
				((Signal) signal).populate(this);
		
		// split ratio profile set (must follow network)
		if(getSplitRatioProfileSet()!=null)
			((SplitRatioProfileSet) getSplitRatioProfileSet()).populate(this);
		
		// boundary capacities (must follow network)
		if(getDownstreamBoundaryCapacityProfileSet()!=null)
			for( com.relteq.sirius.jaxb.CapacityProfile capacityProfile : getDownstreamBoundaryCapacityProfileSet().getCapacityProfile() )
				((CapacityProfile) capacityProfile).populate(this);

		if(getDemandProfileSet()!=null)
			((DemandProfileSet) getDemandProfileSet()).populate(this);
		
		// fundamental diagram profiles 
		if(getFundamentalDiagramProfileSet()!=null)
			for(com.relteq.sirius.jaxb.FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
				((FundamentalDiagramProfile) fd).populate(this);
		
		// initial density profile 
		if(getInitialDensitySet()!=null)
			((InitialDensitySet) getInitialDensitySet()).populate(this);
		
		// populate controllers 
		controllerset.populate(this);

		// populate events 
		eventset.populate(this);
		
	}

	/** Prepare scenario for simulation:
	 * set the state of the scenario to the initial condition
	 * sample profiles
	 * open output files
	 * @return success		A boolean indicating whether the scenario was successfuly reset.
	 * @throws SiriusException 
	 * @y.exclude
	 */
	protected boolean reset(Scenario.ModeType simulationMode) throws SiriusException {
		
		// reset the clock
		clock.reset();
		
		// reset network
		for(com.relteq.sirius.jaxb.Network network : getNetworkList().getNetwork())
			((Network)network).reset(simulationMode);
		
		// sensor list
		if(getSensorList()!=null)
			for (com.relteq.sirius.jaxb.Sensor sensor : getSensorList().getSensor())
				((Sensor) sensor).reset();
			
		// signal list
		if(getSignalList()!=null)
			for (com.relteq.sirius.jaxb.Signal signal : getSignalList().getSignal())
				((Signal) signal).reset();
						
		// reset demand profiles
		if(getDemandProfileSet()!=null)
			((DemandProfileSet)getDemandProfileSet()).reset();

		// reset fundamental diagrams
		if(getFundamentalDiagramProfileSet()!=null)
			for(com.relteq.sirius.jaxb.FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
				((FundamentalDiagramProfile)fd).reset();
		
		// reset controllers
		global_control_on = true;
		controllerset.reset();

		// reset events
		eventset.reset();
		
		return true;
		
	}	
	
	/** 
	 * @throws SiriusException 
	 * @y.exclude
	 */
	protected void update() throws SiriusException {	

        // sample profiles .............................	
    	if(getDownstreamBoundaryCapacityProfileSet()!=null)
        	for(com.relteq.sirius.jaxb.CapacityProfile capacityProfile : getDownstreamBoundaryCapacityProfileSet().getCapacityProfile())
        		((CapacityProfile) capacityProfile).update();

    	if(getDemandProfileSet()!=null)
    		((DemandProfileSet)getDemandProfileSet()).update();

    	if(getSplitRatioProfileSet()!=null)
    		((SplitRatioProfileSet) getSplitRatioProfileSet()).update();        		

    	if(getFundamentalDiagramProfileSet()!=null)
        	for(com.relteq.sirius.jaxb.FundamentalDiagramProfile fdProfile : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
        		((FundamentalDiagramProfile) fdProfile).update();
    	
        // update sensor readings .......................
        // NOTE: ensembles have not been implemented for sensors. They do not apply
        // to the loop sensor, but would make a difference for floating sensors.
		if(getSensorList()!=null)
			for(com.relteq.sirius.jaxb.Sensor sensor : getSensorList().getSensor())
				((Sensor)sensor).update();
        
        // update signals ...............................
		// NOTE: ensembles have not been implemented for signals. They do not apply
		// to pretimed control, but would make a differnece for feedback control. 
		if(getSignalList()!=null)
			for(com.relteq.sirius.jaxb.Signal signal : getSignalList().getSignal())
				((Signal)signal).update();

        // update controllers
    	if(global_control_on)
    		controllerset.update();

    	// update events
    	eventset.update();
    	
        // update the network state......................
		for(com.relteq.sirius.jaxb.Network network : getNetworkList().getNetwork())
			((Network) network).update();
        
	}

	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////
	
	/** Retrieve a network with a given id.
	 * @param id The string id of the network
	 * @return The corresponding network if it exists, <code>null</code> otherwise.
	 * 
	 */
	protected Network getNetworkWithId(String id){
		if(getNetworkList()==null)
			return null;
		if(getNetworkList().getNetwork()==null)
			return null;
		if(id==null && getNetworkList().getNetwork().size()>1)
			return null;
		if(id==null && getNetworkList().getNetwork().size()==1)
			return (Network) getNetworkList().getNetwork().get(0);
		id.replaceAll("\\s","");
		for(com.relteq.sirius.jaxb.Network network : getNetworkList().getNetwork()){
			if(network.getId().equals(id))
				return (Network) network;
		}
		return null;
	}

	/////////////////////////////////////////////////////////////////////
	// excluded from API
	/////////////////////////////////////////////////////////////////////

	/** @y.exclude */
	public Integer [] getVehicleTypeIndices(com.relteq.sirius.jaxb.VehicleTypeOrder vtypeorder){
		
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
			List<com.relteq.sirius.jaxb.VehicleType> settingsname = getSettings().getVehicleTypes().getVehicleType();
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
	public void validate() {
				
		// validate network
		if( getNetworkList()!=null)
			for(com.relteq.sirius.jaxb.Network network : getNetworkList().getNetwork())
				((Network)network).validate();

		// sensor list
		if(getSensorList()!=null)
			for (com.relteq.sirius.jaxb.Sensor sensor : getSensorList().getSensor())
				((Sensor) sensor).validate();

		// signal list
		if(getSignalList()!=null)
			for (com.relteq.sirius.jaxb.Signal signal : getSignalList().getSignal())
				((Signal) signal).validate();
		
		// NOTE: DO THIS ONLY IF IT IS USED. IE DO IT IN THE RUN WITH CORRECT FUNDAMENTAL DIAGRAMS
		// validate initial density profile
//		if(getInitialDensityProfile()!=null)
//			((_InitialDensityProfile) getInitialDensityProfile()).validate();

		// validate capacity profiles	
		if(getDownstreamBoundaryCapacityProfileSet()!=null)
			for(com.relteq.sirius.jaxb.CapacityProfile capacityProfile : getDownstreamBoundaryCapacityProfileSet().getCapacityProfile())
				((CapacityProfile)capacityProfile).validate();
		
		// validate demand profiles
		if(getDemandProfileSet()!=null)
			((DemandProfileSet)getDemandProfileSet()).validate();

		// validate split ratio profiles
		if(getSplitRatioProfileSet()!=null)
			((SplitRatioProfileSet)getSplitRatioProfileSet()).validate();
		
		// validate fundamental diagram profiles
		if(getFundamentalDiagramProfileSet()!=null)
			for(com.relteq.sirius.jaxb.FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
				((FundamentalDiagramProfile)fd).validate();
		
		// validate controllers
		controllerset.validate();

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
	 * @param outputfileprefix 	String prefix for all output files.
	 * @param numRepetitions 	The integer number of simulations to run.
	 * @throws SiriusException 
	 */
	public void run(String outputfileprefix,Double timestart,Double timeend,double outdt,int numRepetitions,OutputWriter_Base.Type outputtype) throws SiriusException{
		RunParameters param = new RunParameters(outputfileprefix,timestart,timeend,outdt,simdtinseconds);
		numEnsemble = 1;
		run_internal(param,numRepetitions,true,false,outputtype);
	}

	/** Run the scenario once, return the state trajectory.
	 * <p> The scenario is reset and run once. 
	 * @return An object with the history of densities and flows for all links in the scenario.
	 * @throws SiriusException 
	 */
	public SiriusStateTrajectory run(Double timestart,Double timeend,double outdt) throws SiriusException{
		RunParameters param = new RunParameters(null,timestart,timeend,outdt,simdtinseconds);
		numEnsemble = 1;
		return run_internal(param,1,false,true,null);
	}
	
	/** Advance the simulation <i>nsec</i> seconds.
	 * 
	 * <p> Move the simulation forward <i>nsec</i> seconds and stops.
	 * Returns <code>true</code> if the operation completes succesfully. Returns <code>false</code>
	 * if the end of the simulation is reached.
	 * @param nsec Number of seconds to advance.
	 * @throws SiriusException 
	 */
	public boolean advanceNSeconds(double nsec) throws SiriusException{	
		
		if(!scenariolocked)
			throw new SiriusException("Run not initialized. Use initialize_run() first.");
		
		if(!SiriusMath.isintegermultipleof(nsec,simdtinseconds))
			throw new SiriusException("nsec (" + nsec + ") must be an interger multiple of simulation dt (" + simdtinseconds + ").");
		int nsteps = SiriusMath.round(nsec/simdtinseconds);		
		return advanceNSteps_internal(ModeType.normal,nsteps,false,false,null,null,-1);
	}

	/** Save the scenario to XML.
	 * 
	 * @param filename The name of the configuration file.
	 * @throws SiriusException 
	 */
	public void saveToXML(String filename) throws SiriusException{
        try {
        	JAXBContext context = JAXBContext.newInstance("com.relteq.sirius.jaxb");
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
	
	/** Time elapsed since the beginning of the simulation in seconds.
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

	/** Start time of the simulation.
	 * @return Start time in seconds. 
	 */
	public double getTimeStart() {
		if(clock==null)
			return Double.NaN;
		else
			return this.clock.getStartTime();
	}

	/** End time of the simulation.
	 * @return End time in seconds. 
	 * @return			XXX
	 */
	public double getTimeEnd() {
		if(clock==null)
			return Double.NaN;
		else
			return this.clock.getEndTime();
	}

	/** Get a reference to a controller by its id.
	 * @param id Id of the controller.
	 * @return A reference to the controller if it exists, <code>null</code> otherwise.
	 */
	public Controller getControllerWithId(String id){
		if(controllerset==null)
			return null;
		for(Controller c : controllerset.get_Controllers()){
			if(c.id.equals(id))
				return c;
		}
		return null;
	}
	
	/** Get a reference to an event by its id.
	 * @param id Id of the event.
	 * @return A reference to the event if it exists, <code>null</code> otherwise.
	 */
	public Event getEventWithId(String id){
		if(eventset==null)
			return null;
		for(Event e : eventset.sortedevents){
			if(e.getId().equals(id))
				return e;
		}
		return null;
	}		

	/** Get a reference to a node by its composite id.
	 * 
	 * @param id String id of the node. 
	 * @return Reference to the node if it exists, <code>null</code> otherwise
	 */
	public Node getNodeWithId(String id){
		if(getNetworkList()==null)
			return null;
		for(com.relteq.sirius.jaxb.Network network : getNetworkList().getNetwork()){
			Node node = ((com.relteq.sirius.simulator.Network) network).getNodeWithId(id);
			if(node!=null)
				return node;
		}
		return null;
	}

	/** Get a reference to a link by its composite id.
	 * 
	 * @param id String id of the link. 
	 * @return Reference to the link if it exists, <code>null</code> otherwise
	 */
	public Link getLinkWithId(String id){
		if(getNetworkList()==null)
			return null;
		for(com.relteq.sirius.jaxb.Network network : getNetworkList().getNetwork()){
			Link link = ((com.relteq.sirius.simulator.Network) network).getLinkWithId(id);
			if(link!=null)
				return link;
		}
		return null;
	}

	/** Get sensor with given id.
	 * @param id String id of the sensor.
	 * @return Sensor object.
	 */
	public Sensor getSensorWithId(String id){
		if(getSensorList()==null)
			return null;
		id.replaceAll("\\s","");
		for(com.relteq.sirius.jaxb.Sensor sensor : getSensorList().getSensor()){
			if(sensor.getId().equals(id))
				return (Sensor) sensor;
		}
		return null;
	}
	
	/** Get signal with given id.
	 * @param id String id of the signal.
	 * @return Signal object.
	 */
	public Signal getSignalWithId(String id){
		if(getSignalList()==null)
			return null;
		id.replaceAll("\\s","");
		for(com.relteq.sirius.jaxb.Signal signal : getSignalList().getSignal()){
			if(signal.getId().equals(id))
				return (Signal) signal;
		}
		return null;
	}

//	/** Get sensors on a given link.
//	 * @param linkid String id of the link.
//	 * @return The list of sensors located in the link.
//	 */
//	public ArrayList<Sensor> getSensorWithLinkId(String linkid){
//		if(getSensorList()==null)
//			return null;
//		ArrayList<Sensor> result = new ArrayList<Sensor>();
//		for(com.relteq.sirius.jaxb.Sensor sensor : getSensorList().getSensor()){
//			Sensor s = (Sensor) sensor;
//			if(s.myLink!=null){
//				if(s.myLink.getId().equals(linkid)){
//					result.add(s);
//					break;
//				}	
//			}
//		}
//		return result;
//	}

//	/** Get one sensor in the given link.
//	 * @param linkid String id of the link.
//	 * @return The first sensor found to be contained in the link. 
//	 */
//	public Sensor getFirstSensorWithLinkId(String linkid){
//		if(getSensorList()==null)
//			return null;
//		for(com.relteq.sirius.jaxb.Sensor sensor : getSensorList().getSensor()){
//			Sensor s = (Sensor) sensor;
//			if(s.myLink!=null){
//				if(s.myLink.getId().equals(linkid)){
//					return s;
//				}
//			}
//		}
//		return null;
//	}


//	/** Get signal on the node with given id.
//	 * @param node_id String id of the node.
//	 * @return Signal object if there is one. <code>null</code> otherwise. 
//	 */
//	public Signal getSignalWithNodeId(String node_id){
//		if(getSignalList()==null)
//			return null;
//		id.replaceAll("\\s","");
//		for(com.relteq.sirius.jaxb.Signal signal : getSignalList().getSignal()){
//			if(signal.getNodeId().equals(node_id))
//				return (Signal)signal;
//		}
//		return null;
//	}
	
	/** Get a reference to a signal by the composite id of its node.
	 * 
	 * @param network_id String id of the network containing the node. 
	 * @param node_id String id of the node. 
	 * @return Reference to the signal if it exists, <code>null</code> otherwise
	 */
	public Signal getSignalWithCompositeNodeId(String network_id,String node_id){
		if(getSignalList()==null)
			return null;
		id.replaceAll("\\s","");
		for(com.relteq.sirius.jaxb.Signal signal : getSignalList().getSignal()){
			if(signal.getNodeId().equals(node_id))
				return (Signal)signal;
		}
		return null;
	}

	/** Add a controller to the scenario.
	 * 
	 * <p>Controllers can only be added if a) the scenario is not currently running, and
	 * b) the controller is valid. 
	 * @param C The controller
	 * @return <code>true</code> if the controller was successfully added, <code>false</code> otherwise. 
	 */
	public boolean addController(Controller C){
		if(scenariolocked)
			return false;
		if(C==null)
			return false;
		if(C.myType==null)
			return false;
		
		// validate
		SiriusErrorLog.clearErrorMessage();
		C.validate();
		SiriusErrorLog.print();
		if(SiriusErrorLog.haserror())
			return false;
		
		// add
		controllerset.controllers.add(C);
		
		return true;
	}

	/** Add an event to the scenario.
	 * 
	 * <p>Events are not added if the scenario is running. This method does not validate the event.
	 * @param E The event
	 * @return <code>true</code> if the event was successfully added, <code>false</code> otherwise. 
	 */
	public boolean addEvent(Event E){
		if(scenariolocked)
			return false;
		if(E==null)
			return false;
		if(E.myType==null)
			return false;
		
		// add event to list
		eventset.addEvent(E);
		
		return true;
	}

//	/** Add a sensor to the scenario.
//	 * 
//	 * <p>Sensors can only be added if a) the scenario is not currently running, and
//	 * b) the sensor is valid. 
//	 * @param S The sensor
//	 * @return <code>true</code> if the sensor was successfully added, <code>false</code> otherwise. 
//	 */
//	public boolean addSensor(Sensor S){
//		if(S==null)
//			return false;
//		if(S.myType==null)
//			return false;
//		if(S.myLink==null)
//			return false;
// 
//		// validate
//		if(!S.validate())
//			return false;
//		
//		// add sensor to list
//		S.getSensorList().getSensor().add(S);
//		
//		return true;
//	}

	/** Get the initial density state for the network with given id.
	 * @param network_id String id of the network
	 * @return A two-dimensional array of doubles where the first dimension is the
	 * link index (ordered as in {@link Network#getListOfLinks}) and the second is the vehicle type 
	 * (ordered as in {@link Scenario#getVehicleTypeNames})
	 */
	public double [][] getInitialDensityForNetwork(String network_id){
				
		Network network = getNetworkWithId(network_id);
		if(network==null)
			return null;
		
		double [][] density = new double [network.getLinkList().getLink().size()][getNumVehicleTypes()];
		InitialDensitySet initprofile = (InitialDensitySet) getInitialDensitySet();

		int i,j;
		for(i=0;i<network.getLinkList().getLink().size();i++){
			if(initprofile==null){
				for(j=0;j<numVehicleTypes;j++)
					density[i][j] = 0d;
			}
			else{
				com.relteq.sirius.jaxb.Link link = network.getLinkList().getLink().get(i);
				Double [] init_density = initprofile.getDensityForLinkIdInVeh(link.getId(),network.getId());
				for(j=0;j<numVehicleTypes;j++)
					density[i][j] = init_density[j];
			}
		}
		return density;                         
	}

	/** Get the current density state for the network with given id.
	 * @param network_id String id of the network
	 * @return A two-dimensional array of doubles where the first dimension is the
	 * link index (ordered as in {@link Network#getListOfLinks}) and the second is the vehicle type 
	 * (ordered as in {@link Scenario#getVehicleTypeNames})
	 */
	public double [][] getDensityForNetwork(String network_id,int ensemble){
		
		Network network = getNetworkWithId(network_id);
		if(network==null)
			return null;
		
		double [][] density = new double [network.getLinkList().getLink().size()][getNumVehicleTypes()];

		int i,j;
		for(i=0;i<network.getLinkList().getLink().size();i++){
			Link link = (Link) network.getLinkList().getLink().get(i);
			Double [] linkdensity = link.getDensityInVeh(ensemble);
			for(j=0;j<numVehicleTypes;j++)
				density[i][j] = linkdensity[j];
		}
		return density;           
		
	}
	
	/** Initialize the run before using {@link Scenario#advanceNSeconds(double)}
	 * 
	 * <p>This method performs certain necessary initialization tasks on the scenario. In particular
	 * it locks the scenario so that elements may not be added mid-run. It also resets the scenario
	 * rolling back all profiles and clocks. 
	 * @param numEnsemble Number of simulations to run in parallel
	 */
	public void initialize_run(int numEnsemble) throws SiriusException{

		scenariolocked = false;
			
		this.numEnsemble = numEnsemble;
		
		// check that no controllers are used 
		if(global_control_on && numEnsemble>1 && getControllerSet()!=null){
			if(!getControllerSet().getController().isEmpty()){
				System.out.println("Warning! This scenario has controllers. " +
						"Currently ensemble runs work only with control turned off. " +
						"Deactivting control and continuing");
				global_control_on = false;
			}
		}
		
        double time_ic;
        if(getInitialDensitySet()!=null)
        	time_ic = ((InitialDensitySet)getInitialDensitySet()).timestamp;
        else
        	time_ic = 0.0;
        
        double timestart = time_ic;			// start at the initial condition time
        Scenario.ModeType simulationMode = Scenario.ModeType.normal;

		if(numEnsemble<=0)
			throw new SiriusException("Number of ensemble runs must be at least 1.");
		
		// create the clock
		clock = new Clock(timestart,Double.POSITIVE_INFINITY,simdtinseconds);
		
		// reset the simulation
		if(!reset(simulationMode))
			throw new SiriusException("Reset failed.");
		
		// lock the scenario
        scenariolocked = true;	
	}
		
	/////////////////////////////////////////////////////////////////////
	// private
	/////////////////////////////////////////////////////////////////////	
	
	private SiriusStateTrajectory run_internal(RunParameters param,int numRepetitions,boolean writefiles,boolean returnstate,OutputWriter_Base.Type outputtype) throws SiriusException{
			
		if(returnstate && numRepetitions>1)
			throw new SiriusException("run with multiple repetitions and returning state not allowed.");
		
		SiriusStateTrajectory state = null;

		// create the clock
		clock = new Clock(param.timestart,param.timeend,simdtinseconds);
		
		// lock the scenario
        scenariolocked = true;	
        
		// loop through simulation runs ............................
		for(int i=0;i<numRepetitions;i++){

			OutputWriter_Base outputwriter  = null;
			
			// open output files
	        if( writefiles && param.simulationMode.compareTo(Scenario.ModeType.normal)==0 ){
	        	switch(outputtype){
	        	case xml:
	        		outputwriter = new OutputWriter(this);
	        		break;
	        	case tabdelim:
	        		outputwriter = new OutputWriter_simple(this);
	        		break;
	        	}
				try {
					outputwriter.open(param.outputfileprefix,String.format("%d",i));
				} catch (FileNotFoundException e) {
					throw new SiriusException("Unable to open output file.");
				}
	        }
	        
	        // allocate state
	        if(returnstate)
	        	state = new SiriusStateTrajectory(this,param.outsteps);
	
			// reset the simulation
			if(!reset(param.simulationMode))
				throw new SiriusException("Reset failed.");
						
			// advance to end of simulation
			while( advanceNSteps_internal(param.simulationMode,1,writefiles,returnstate,outputwriter,state,param.outsteps) ){					
			}
			
            // close output files
	        if(writefiles){
	        	if(param.simulationMode.compareTo(Scenario.ModeType.normal)==0)
		        	outputwriter.close();
				// or save scenario (in warmup mode)
		        if(param.simulationMode.compareTo(Scenario.ModeType.warmupFromIC)==0 || param.simulationMode.compareTo(Scenario.ModeType.warmupFromZero)==0 ){
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
	// throws 
	// SiriusException for all errors
	private boolean advanceNSteps_internal(Scenario.ModeType simulationMode,int n,boolean writefiles,boolean returnstate,OutputWriter_Base outputwriter,SiriusStateTrajectory state,int outsteps) throws SiriusException{

		// advance n steps
		for(int k=0;k<n;k++){

			// export initial condition
	        if(simulationMode.compareTo(ModeType.normal)==0 && outsteps>0 )
	        	if( clock.getCurrentstep()==0 )
	        		recordstate(writefiles,returnstate,outputwriter,state,false,outsteps);
        	
        	// update scenario
        	update();

            // update time (before write to output)
        	clock.advance();
        	
            if(simulationMode.compareTo(ModeType.normal)==0 && outsteps>0 )
	        	if( clock.getCurrentstep()%outsteps == 0 )
	        		recordstate(writefiles,returnstate,outputwriter,state,true,outsteps);
        	
        	if(clock.expired())
        		return false;
		}
	      
		return true;
	}
	
	private void recordstate(boolean writefiles,boolean returnstate,OutputWriter_Base outputwriter,SiriusStateTrajectory state,boolean exportflows,int outsteps) throws SiriusException {
		if(writefiles)
			outputwriter.recordstate(clock.getT(),exportflows,outsteps);
		if(returnstate)
			state.recordstate(clock.getCurrentstep(),clock.getT(),exportflows,outsteps);
	}
	
	private class RunParameters{
		public String outputfileprefix;
		public double timestart;			// [sec] start of the simulation
		public double timeend;				// [sec] end of the simulation
		public int outsteps;				// [-] number of simulation steps per output step
		public Scenario.ModeType simulationMode;
		
		// input parameter outdt [sec] output sampling time
		public RunParameters(String outputfileprefix,double tstart,double tend,double outdt,double simdtinseconds) throws SiriusException{
			
			// check timestart < timeend
			if(tstart>=tend)
				throw new SiriusException("Empty simulation period.");

			// check that outdt is a multiple of simdt
			if(!SiriusMath.isintegermultipleof(outdt,simdtinseconds))
				throw new SiriusException("outdt (" + outdt + ") must be an interger multiple of simulation dt (" + simdtinseconds + ").");
			
			this.outputfileprefix = outputfileprefix;
			this.timestart = tstart;
			this.timeend = tend;
	        this.outsteps = SiriusMath.round(outdt/simdtinseconds);

			// Simulation mode is normal <=> start time == initial profile time stamp
			simulationMode = null;
			
	        double time_ic;
	        if(getInitialDensitySet()!=null)
	        	time_ic = ((InitialDensitySet)getInitialDensitySet()).timestamp;
	        else
	        	time_ic = 0.0;
	       
			if(SiriusMath.equals(timestart,time_ic)){
				simulationMode = Scenario.ModeType.normal;
			}
			else{
				// it is a warmup. we need to decide on start and end times
				timeend = timestart;
				if(time_ic<timestart){	// go from ic to timestart
					timestart = time_ic;
					simulationMode = Scenario.ModeType.warmupFromIC;
				}
				else{							// start at earliest demand profile
					timestart = Double.POSITIVE_INFINITY;
					if(getDemandProfileSet()!=null)
						for(com.relteq.sirius.jaxb.DemandProfile D : getDemandProfileSet().getDemandProfile())
							timestart = Math.min(timestart,D.getStartTime().doubleValue());
					else
						timestart = 0.0;
					simulationMode = Scenario.ModeType.warmupFromZero;
				}		
			}
			if( timeend < timestart )
				timeend = timestart;
		}

	}

	/** Get configuration file name */
	public String getConfigFilename() {
		return configfilename;
	}

}