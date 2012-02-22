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

/** DESCRIPTION OF THE CLASS
*
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public final class _Scenario extends com.relteq.sirius.jaxb.Scenario {

	protected Clock clock;
	protected String configfilename;
	protected double outdt;				// [sec] output sampling time
	protected double timestart;			// [sec] start of the simulation
	protected double timeend;			// [sec] end of the simulation
	protected OutputWriter outputwriter = null;
	protected String outputfile_density;
	protected String outputfile_outflow;
	protected String outputfile_inflow;
	
	protected Random random = new Random();

	protected _Scenario.ModeType simulationMode;
	protected _Scenario.UncertaintyType uncertaintyModel;
	
	protected int numVehicleTypes;			// number of vehicle types
	protected boolean global_control_on;	// global control switch
	protected double simdtinseconds;		// [sec] simulation time step 
	protected double simdtinhours;			// [hr]  simulation time step 	
	
	protected boolean isloaded=false;		// true if configuration file has been loaded
	protected boolean isvalid=false;		// true if it has passed validation
	protected boolean isrunning=false;		// true when the simulation is running
	
	protected _ControllerSet _controllerset = new _ControllerSet();
	protected _EventSet _eventset = new _EventSet();	// holds time sorted list of events
	
	protected static enum ModeType {NULL, normal, 
									    warmupFromZero , 
									    warmupFromIC };

	protected static enum UncertaintyType { NULL, uniform, 
          										gaussian }
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	// populate methods copy data from the jaxb state to extended objects. 
	// They do not throw exceptions or report mistakes. Data errors should be
	// circumvented and left for the validation to report.
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

	// prepare scenario for simulation:
	// set the state of the scenario to the initial condition
	// sample profiles
	// open output files
	protected boolean reset() {
		
		if(!isloaded){
			SiriusError.addErrorMessage("Load scenario first.");
			return false;
		}
		
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
	// API
	/////////////////////////////////////////////////////////////////////
	
	/** DESCRIPTION
	 * 
	 */
	public void validate() {
		
		if(!isloaded){
			SiriusError.setErrorHeader("Load failed.");
			SiriusError.printErrorMessage();
			return;
		}
		
		if(isvalid)
			return;
		
		// check that outdt is a multiple of simdt
		if(!SiriusMath.isintegermultipleof(outdt,simdtinseconds)){
//			Utils.addErrorMessage("Aborting: outdt must be an interger multiple of simulation dt.");
//			Utils.printErrorMessage();
			return;
		}
		
		// validate network
		if( getNetworkList()!=null)
			for(Network network : getNetworkList().getNetwork())
				if(!((_Network)network).validate())
					return;

		// validate initial density profile
		if(getInitialDensityProfile()!=null)
			if(!((_InitialDensityProfile) getInitialDensityProfile()).validate())
				return;

		// validate capacity profiles	
		if(getDownstreamBoundaryCapacitySet()!=null)
			for(CapacityProfile capacityProfile : getDownstreamBoundaryCapacitySet().getCapacityProfile())
				if(!((_CapacityProfile)capacityProfile).validate())
					return;
		
		// validate demand profiles
		if(getDemandProfileSet()!=null)
			if(!((_DemandProfileSet)getDemandProfileSet()).validate())
				return;

		// validate split ratio profiles
		if(getSplitRatioProfileSet()!=null)
			if(!((_SplitRatioProfileSet)getSplitRatioProfileSet()).validate())
				return;

		// validate fundamental diagram profiles
		if(getFundamentalDiagramProfileSet()!=null)
			for(FundamentalDiagramProfile fd : getFundamentalDiagramProfileSet().getFundamentalDiagramProfile())
				if(!((_FundamentalDiagramProfile)fd).validate())
					return;

		// validate controllers
		if(!_controllerset.validate())
			return;

		// validate events
		if(!_eventset.validate())
			return;

		isvalid = true;
	}
	
	/** DESCRIPTION
	 * 
	 * @param numRepetitions 	The integer number of simulations to run.
	 */
	public void run(int numRepetitions){

		if(!isvalid){
			SiriusError.setErrorHeader("Validate first.");
			SiriusError.printErrorMessage();
			return;
		}
		
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
	public String getConfigFileName() {
		return configfilename;
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
	 * @return			XXX
	 */
	public int getVehicleTypeIndex(String name){
		if(getSettings()==null)
			return -1;
		if(getSettings().getVehicleTypes()==null)
			return -1;
		List<VehicleType> vt = getSettings().getVehicleTypes().getVehicleType();
		for(int i=0;i<vt.size();i++){
			if(vt.get(i).getName().equals(name))
				return i;
		}
		return -1;
	}
	
	/** DESCRIPTION
	 * 
	 * @param C			XXX
	 */
	public void addController(_Controller C){
		if(isrunning)
			return;
		if(C==null)
			return;
		if(C.myType==_Controller.Type.NULL)
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
		if(E.myType==_Event.Type.NULL)
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
		if(S.myType==_Sensor.Type.NULL)
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