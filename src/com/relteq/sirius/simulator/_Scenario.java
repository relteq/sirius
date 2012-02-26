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
	 * @y.exclude
	 */
	protected void populate() {
		
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
	 * @y.exclude
	 */
	protected boolean reset() {
		
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
	
	/** @y.exclude
	 */
	protected void update() {	

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
	
	/** DESCRIPTION
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

	/** DESCRIPTION
	 * @y.exclude
	 */
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
	
	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////
	
	/** 
	 * @y.exclude
	 */
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

		// validate events
		if(!_eventset.validate())
			return false;

		return true;
	}
	
	/** DESCRIPTION
	 * 
	 * @param numRepetitions 	The integer number of simulations to run.
	 */
	public void run(int numRepetitions){
		
        isrunning = true;

		// loop through simulation runs ............................
		for(int i=0;i<numRepetitions;i++){
			
			// reset scenario
			if(!reset()){
				SiriusError.setErrorHeader("Reset failed.");
				SiriusError.printErrorMessage();
				return;
			}

			// open output files
	        if(simulationMode==_Scenario.ModeType.normal){
	        	outputwriter = new OutputWriter(this,SiriusMath.round(outdt/simdtinseconds));
				try {
					outputwriter.open(outputfile_density,outputfile_outflow,outputfile_inflow);
				} catch (FileNotFoundException e) {
					SiriusError.addErrorMessage("Unable to open output file.");
					SiriusError.printErrorMessage();
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

	/** DESCRIPTION
	 * 
	 * @param filename	XXX
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
	
	/** DESCRIPTION
	 * 
	 * @return			XXX
	 */
	public double getTime() {
		if(clock==null)
			return Double.NaN;
		return clock.getT();
	}

	/** DESCRIPTION
	 * 
	 * @return			XXX
	 */
	public int getCurrentTimeStep() {
		if(clock==null)
			return 0;
		return clock.getCurrentstep();
	}

	/** DESCRIPTION
	 * 
	 * @return			XXX
	 */
	public int getNumVehicleTypes() {
		return numVehicleTypes;
	}

	/** DESCRIPTION
	 * 
	 * @return			XXX
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

	/** DESCRIPTION
	 * 
	 * @return			XXX
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
	
	/** DESCRIPTION
	 * 
	 * @return			XXX
	 */
	public double getSimDtInSeconds() {
		return simdtinseconds;
	}

	/** DESCRIPTION
	 * 
	 * @return			XXX
	 */
	public double getSimDtInHours() {
		return simdtinhours;
	}

	/** DESCRIPTION
	 * 
	 * @return			XXX
	 */
	public double getOutDt() {
		return outdt;
	}

	/** DESCRIPTION
	 * 
	 * @return			XXX
	 */
	public double getTimeStart() {
		return timestart;
	}

	/** DESCRIPTION
	 * 
	 * @return			XXX
	 */
	public double getTimeEnd() {
		return timeend;
	}
	
	/** DESCRIPTION
	 * 
	 * @param id		XXX
	 * @return			XXX
	 */
	public _Controller getControllerWithName(String id){
		if(_controllerset==null)
			return null;
		for(_Controller c : _controllerset.get_Controllers()){
			if(c.name.equals(id))
				return c;
		}
		return null;
	}
	
	/** DESCRIPTION
	 * 
	 * @param network_id	XXX
	 * @param id			XXX
	 * @return				XXX
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

	/** DESCRIPTION
	 * 
	 * @param network_id	XXX
	 * @param id			XXX
	 * @return				XXX
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
	
	/** DESCRIPTION
	 * 
	 * @param network_id	XXX
	 * @param id			XXX
	 * @return				XXX
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
	
	/** DESCRIPTION
	 * 
	 * @param C			XXX
	 * @y.exclude
	 */
	public void addController(_Controller C){
		if(isrunning)
			return;
		if(C==null)
			return;
		if(C.myType==null)
			return;
		
		// validate
		if(!C.validate())
			return;
		// add
		_controllerset._controllers.add(C);
	}
	
	/** DESCRIPTION
	 * 
	 * @param E			XXX
	 */
	public void addEvent(_Event E){
		if(isrunning)
			return;
		if(E==null)
			return;
		if(E.myType==null)
			return;
		// validate
		if(!E.validate())
			return;
		
		// add event to list
		_eventset.addEvent(E);
	}
	
	/** DESCRIPTION
	 * 
	 * @param S 		XXX
	 */
	public void addSensor(_Sensor S){
		if(S==null)
			return;
		if(S.myType==null)
			return;
		if(S.myLink==null)
			return;
		if(S.myLink.myNetwork==null)
			return;

		// validate
		if(!S.validate())
			return;
		
		// add sensor to list
		S.myLink.myNetwork._sensorlist._sensors.add(S);
	}
	
}