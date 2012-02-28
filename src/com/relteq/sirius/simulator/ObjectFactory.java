package com.relteq.sirius.simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import com.relteq.sirius.control.*;
import com.relteq.sirius.event.*;
import com.relteq.sirius.sensor.*;
import com.relteq.sirius.jaxb.Controller;
import com.relteq.sirius.jaxb.DemandProfile;
import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.jaxb.Network;
import com.relteq.sirius.jaxb.ScenarioElement;
import com.relteq.sirius.jaxb.Sensor;

/** Factory methods for creating scenarios, controllers, events, sensors, and scenario elements. 
 * <p>
 * Use the static methods in this class to load a scenario from XML, and to programmatically generate events, controllers, sensors, and scenario elements.
 * 
* @author Gabriel Gomes
*/
public final class ObjectFactory {

	private static String schemafile = "data/schema/sirius.xsd";

	/////////////////////////////////////////////////////////////////////
	// private default constructor
	/////////////////////////////////////////////////////////////////////

	private ObjectFactory(){}
							  
	/////////////////////////////////////////////////////////////////////
	// protected
	/////////////////////////////////////////////////////////////////////
	
	/** @y.exclude */
	protected static _Controller createControllerFromJaxb(_Scenario myScenario,Controller jaxbC,_Controller.Type myType) {		
		if(myScenario==null)
			return null;
		_Controller C;
		switch(myType){
			case IRM_alinea:
				C = new Controller_IRM_Alinea();
				break;
				
			case IRM_time_of_day:
				C = new Controller_IRM_Time_of_Day();
				break;
				
			case IRM_traffic_responsive:
				C = new Controller_IRM_Traffic_Responsive();
				break;
	
			case CRM_swarm:
				C = new Controller_CRM_SWARM();
				break;
				
			case CRM_hero:
				C = new Controller_CRM_HERO();
				break;
				
			case VSL_time_of_day:
				C = new Controller_VSL_Time_of_Day();
				break;
				
			case SIG_pretimed:
				C = new Controller_SIG_Pretimed();
				break;
				
			case SIG_actuated:
				C = new Controller_SIG_Actuated();
				break;
				
			default:
				C = null;
				break;
		}
		C.populateFromJaxb(myScenario, jaxbC, myType);
		C.populate(jaxbC);
		return C;

	}

	/** @y.exclude */
	protected static _Event createEventFromJaxb(_Scenario myScenario,Event jaxbE,_Event.Type myType) {	
		if(myScenario==null)
			return null;
		_Event E;
		switch(myType){
			case fundamental_diagram:
				E = new Event_Fundamental_Diagram();
				break;

			case link_demand_knob:
				E = new Event_Link_Demand_Knob();
				break;

			case link_lanes:
				E = new Event_Link_Lanes();
				break;

			case node_split_ratio:
				E = new Event_Node_Split_Ratio();
				break;

			case control_toggle:
				E = new Event_Control_Toggle();
				break;

			case global_control_toggle:
				E = new Event_Global_Control_Toggle();
				break;

			case global_demand_knob:
				E = new Event_Global_Demand_Knob();
				break;
				
			default:
				E = null;
				break;
		}
		E.populateFromJaxb(myScenario, jaxbE, myType);
		E.populate(jaxbE);
		return E;
	}

	/** @y.exclude */
	protected static _Sensor createSensorFromJaxb(_Scenario myScenario,Sensor jaxbS,_Sensor.Type myType) {	
		if(myScenario==null)
			return null;
		_Sensor S;
		switch(myType){
			case static_point:
				S = new SensorLoopStation();
				break;

			case static_area:
				S = null; 
				break;

			case moving_point:
				S = new SensorFloating();
				break;
				
			default:
				S = null;
				break;
		}
		S.populateFromJaxb(myScenario, jaxbS, myType);
		S.populate(jaxbS);
		return S;
	}

	/** @y.exclude */
	protected static _ScenarioElement createScenarioElementFromJaxb(_Scenario myScenario,ScenarioElement jaxbS){
		if(myScenario==null)
			return null;
		_ScenarioElement S = new _ScenarioElement();
		S.myScenario = myScenario;
		S.id = jaxbS.getId();
		S.network_id = jaxbS.getNetworkId();
		S.myType = _ScenarioElement.Type.valueOf(jaxbS.getType());
		switch(S.myType){
		case link:
			S.reference = myScenario.getLinkWithCompositeId(S.network_id,S.id);
			break;
		case node:
			S.reference = myScenario.getNodeWithCompositeId(S.network_id,S.id);
			break;
		case sensor:
			S.reference = myScenario.getSensorWithCompositeId(S.network_id,S.id);
			break;
//		case signal:
//			S.reference = myScenario.getSignalWithCompositeId(S.network_id,S.id);
//			break;
		case controller:
			S.reference = myScenario.getControllerWithName(S.id);
			break;
			
		default:
			S.reference = null;	
		}
		return S;
	}
	
	/////////////////////////////////////////////////////////////////////
	// public: scenario
	/////////////////////////////////////////////////////////////////////

	/** Loads and validates scenarios from XML. 
	 * <p>
	 * This method does the following,
	 * <ol>
	 * <li> Unmarshalls the configuration file to populate JAXB objects, </li>
	 * <li> Determines the simulation mode (see below),</li>
	 * <li> Registers controllers with their targets (calls to InterfaceController.register()), </li>
	 * <li> Validates the scenario and all its components (calls to validate() on all scenario components). </li>
	 * </ol>
	 * <p>
	 * The simulation mode can be <i>normal</i>, <i>warmup from initial condition</i>, or <i> warmup from zero density</i>, 
	 * depending on the values of <code>timestart</code>, <code>timeend</code>, and the time stamp on the initial density profile (time_ic). In the <i>normal</i> mode,
	 * the simulator initializes the network with densities provided in the initial density profile, and produces as output the evolution of the density
	 * state from <code>timestart</code> to <code>timeend</code>. The warmup modes are executed whenever <code>timestart</code> does not match the timestamp of the initial density profile. 
	 * In these modes the objective is to generate a configuration file with an initial density profile corresponding to <code>timestart</code>. If <code>timestart</code>&gt time_ic, 
	 * the network is initialized with the given initial density profile and run from time_ic to <code>timestart</code>. If <code>timestart</code>&lt time_ic, the simulation is
	 * is initialized with zero density and run from the earliest timestamp of all demand profiles (timestart_demand) to <code>timestart</code>, assuming timestart_demand&lt<code>timestart</code>.
	 * If <code>timestart</code>&lt time_ic and timestart_demand&gt<code>timestart</code>, an error is produced.
	 * <p>
	 * <table border="1">
	 * <tr> <th>Simulation mode</th>   <th>Condition</th> 			 		 		<th>Initial condition</th>			<th>Start time</th> 			<th>End time</th> 				<th>Output</th>	</tr>
	 * <tr> <td>normal</td>			   <td><code>timestart</code>==time_ic</td>	 	<td>initial density profile</td>	<td><code>timestart</code></td>	<td><i>timeend</i></td>			<td>state</td>	</tr>
	 * <tr> <td> warmup from ic	</td>  <td><code>timestart</code>&gt time_ic</td>	<td>initial density profile</td>	<td>time_ic</td>				<td><code>timestart</code></td>	<td>configuration file</td>	</tr>
	 * <tr> <td> warmup from zero</td> <td><code>timestart</code>&lt time_ic</td>  	<td>zero density</td>				<td>timestart_demand</td>		<td><code>timestart</code></td>	<td>configuration file</td>	</tr>
	 * </table> 
	 * 
	 * @param configfilename		The name of the XML configuration file.
	 * @param outputfileprefix		A prefix to be used for all simulation output files.
	 * @param timestart				The start time of the simulation in seconds after midnight.
	 * @param timeend				The end time of the simulation in seconds after midnight.
	 * @param outdt					The period in seconds at which simulation output is written. 
	 * @return scenario				_Scenario object.
	 */
	public static _Scenario createAndLoadScenario(String configfilename,String outputfileprefix,double timestart,double timeend,double outdt) {

		JAXBContext context;
		Unmarshaller u;
    	
    	// create unmarshaller .......................................................
        try {
        	context = JAXBContext.newInstance("com.relteq.sirius.jaxb");
            u = context.createUnmarshaller();
        } catch( JAXBException je ) {
        	SiriusErrorLog.addErrorMessage("Failed to create context for JAXB unmarshaller.");
            return null;
        }
        
        // schema assignment ..........................................................
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        File schemaLocation = new File(ObjectFactory.schemafile);
        try{
        	Schema schema = factory.newSchema(schemaLocation);
        	u.setSchema(schema);
        } catch(SAXException e){
        	SiriusErrorLog.addErrorMessage("Schema not found.");
        	return null;
        }
        
        // process configuration file name ...........................................
		if(!configfilename.endsWith(".xml"))
			configfilename += ".xml";

        // read and return ...........................................................
        _Scenario S = new _Scenario();
        try {
            u.setProperty("com.sun.xml.internal.bind.ObjectFactory",new _JaxbObjectFactory());            
        	S = (_Scenario) u.unmarshal( new FileInputStream(configfilename) );
        } catch( JAXBException je ) {
        	SiriusErrorLog.addErrorMessage("JAXB threw an exception when loading the configuration file.");
        	if(je.getLinkedException()!=null)
        		SiriusErrorLog.addErrorMessage(je.getLinkedException().getMessage());
            return null;
        } catch (FileNotFoundException e) {
        	SiriusErrorLog.addErrorMessage("Configuration file not found.");
        	return null;
		}

        // copy in input parameters ..................................................
        S.configfilename = configfilename;
		if(outputfileprefix.endsWith(".csv"))
			outputfileprefix = outputfileprefix.substring(0,outputfileprefix.length()-4);
		S.outputfile_density = outputfileprefix + "_density.txt";
		S.outputfile_outflow = outputfileprefix + "_outflow.txt";
		S.outputfile_inflow = outputfileprefix + "_inflow.txt";
        S.timestart = timestart;
        S.timeend = timeend;
        S.outdt = outdt;
        
        // copy data to static variables ..............................................
        S.global_control_on = true;
        S.simdtinseconds = computeCommonSimulationTimeInSeconds(S);
        S.simdtinhours = S.simdtinseconds/3600.0;
        S.uncertaintyModel = _Scenario.UncertaintyType.uniform;
        S.global_demand_knob = 1d;
        if(S.getSettings().getVehicleTypes()==null)
            S.numVehicleTypes = 1;
        else
        	if(S.getSettings().getVehicleTypes().getVehicleType()!=null) 
        		S.numVehicleTypes = S.getSettings().getVehicleTypes().getVehicleType().size();
        
        // simulation mode
        setSimulationMode(S);

		// create the clock
        S.clock = new Clock(S.timestart,S.timeend,S.simdtinseconds);

        // populate the scenario ....................................................
        try{
        	S.populate();
        } catch (SiriusException e){
        	SiriusErrorLog.addErrorMessage("ERROR CAUGHT.");
        	return null;
        }
        
        // register controllers with their targets ..................................
        boolean registersuccess = true;
        for(_Controller controller : S._controllerset._controllers)
        	registersuccess &= controller.register();
        
        if(!registersuccess){
        	SiriusErrorLog.addErrorMessage("Conflicting controllers");
        	return null;
        }
        
        // check that load was successful        
		if(!checkLoad(S)){
			SiriusErrorLog.setErrorHeader("Load failed.");
			SiriusErrorLog.printErrorMessage();
			return null;
		}
		
		// validate scenario ......................................
		if(!S.validate()){
			SiriusErrorLog.setErrorHeader("Validation failed.");
			SiriusErrorLog.printErrorMessage();
			return null;
		}
		
        return S;
		
	}

	/////////////////////////////////////////////////////////////////////
	// public: controller
	/////////////////////////////////////////////////////////////////////
	
	/** [NOT IMPLEMENTED]. HERO coordinated ramp metering..
	 * 
	 * @return			_Controller object
	 */
	public static _Controller createController_CRM_HERO(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_CRM_HERO(myScenario);
	}

	/** [NOT IMPLEMENTED] SWARM coordinated ramp metering.
	 * 
	 * @return			_Controller object
	 */
	public static _Controller createController_CRM_SWARM(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_CRM_SWARM(myScenario);
	}

	/** Alinea isolated ramp metering.
	 * 
	 * <p> Generates a controller executing the Alinea algorithm. Feedback for the controller is taken
	 * either from <code>mainlinelink</code> or <code>mainlinesensor</code>, depending on which is 
	 * specified. Hence exactly one of the two must be non-null. A queue override algorithm will be 
	 * employed if the <code>queuesensor</code> is non-null. The gain, defined in mile/hr units, is
	 * normalized within the algorithm by dividing by the length a the mainline link (or by the link where the 
	 * sensor resides in the case of sensor feedback).
	 * 
	 * @param myScenario		The scenario that contains the controller and its referenced links.
	 * @param onramplink		The onramp link being controlled.
	 * @param mainlinelink		The mainline link used for feedback (optional, use <code>null</code> to omit).
	 * @param mainlinesensor	The onramp sensor used for feedback (optional, use <code>null</code> to omit).
	 * @param queuesensor		The sensor on the onramp used to detect queue spillover optional, use <code>null</code> to omit).
	 * @param gain				The gain for the integral controller in mile/hr.
	 * @return					_Controller object
	 */
	public static _Controller createController_IRM_Alinea(_Scenario myScenario,_Link onramplink, _Link mainlinelink,_Sensor mainlinesensor,_Sensor queuesensor,double gain){
		return  new com.relteq.sirius.control.Controller_IRM_Alinea(myScenario,onramplink,mainlinelink,mainlinesensor,queuesensor,gain);
	}
	
	/** [NOT IMPLEMENTED] Time of day isolated ramp metering.
	 * 
	 * @return			_Controller object
	 */
	public static _Controller createController_IRM_Time_of_Day(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_IRM_Time_of_Day(myScenario);
	}

	/** [NOT IMPLEMENTED] Traffic responsive isolated ramp metering.
	 * 
	 * @return			_Controller object
	 */
	public static _Controller createController_IRM_Traffic_Responsive(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_IRM_Traffic_Responsive(myScenario);
	}

	/** [NOT IMPLEMENTED] Actuated signal control.
	 * 
	 * @return			_Controller object
	 */
	public static _Controller createController_SIG_Actuated(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_SIG_Actuated(myScenario);
	}

	/** [NOT IMPLEMENTED] Pretimed signal control.
	 * 
	 * @return			_Controller object
	 */
	public static _Controller createController_SIG_Pretimed(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_SIG_Pretimed(myScenario);
	}

	/** [NOT IMPLEMENTED] Time of day variable speed limits.
	 * 
	 * @return			_Controller object
	 */
	public static _Controller createController_VSL_Time_of_Day(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_VSL_Time_of_Day(myScenario);
	}

	/////////////////////////////////////////////////////////////////////
	// public: event
	/////////////////////////////////////////////////////////////////////
	
	/** On/Off switch for controllers.
	 * 
	 * Turns all controllers included in the <code>controllers</code> array on or off,
	 * depending on the value of <code>ison</code>, at time <code>timestampinseconds</code>.
	 * Here "off" means that the control commands are ignored by their targets, and that the 
	 * controller's update function is not called. 
	 * 
	 * @param myScenario			The scenario.
	 * @param timestampinseconds	Activation time for the event.
	 * @param controllers			List of target _Controller objects.
	 * @param ison					<code>true</code> turns controllers on, <code>false</code> turns controllers off.
	 * @return						_Event object
	 */
	public static _Event createEvent_Control_Toggle(_Scenario myScenario,float timestampinseconds,List <_Controller> controllers,boolean ison) {
		return  new com.relteq.sirius.event.Event_Control_Toggle(myScenario,timestampinseconds,controllers,ison);
	}	

	/** Change the model parameters of a list of links.
	 * 
	 * <p> Use this event to modify any subset of the fundamental diagram parameters of a list of links.
	 * The new parameters should be expressed in per-lane units. Use <code>null</code> in place of any
	 * of the input parameters to indicate that the current value of the parameter should be kept. The
	 * 
	 * @param myScenario		The scenario.
	 * @param links				List of _Link objects.
	 * @param freeflowSpeed		Freeflow speed in [mile/hr]
	 * @param congestionSpeed	Congestion wave speed in [mile/hr]
	 * @param capacity			Capacity in [veh/hr/lane]
	 * @param densityJam		Jam density in [veh/mile/lane]
	 * @param capacityDrop		Capacity drop in [veh/hr/lane]
	 * @param stdDevCapacity	Standard deviation for the capacity in [veh/hr/lane]
	 * @return					_Event object
	 */
	public static _Event createEvent_Fundamental_Diagram(_Scenario myScenario,List <_Link> links,double freeflowSpeed,double congestionSpeed,double capacity,double densityJam,double capacityDrop,double stdDevCapacity) {		
		return  new com.relteq.sirius.event.Event_Fundamental_Diagram(myScenario,links,freeflowSpeed,congestionSpeed,capacity,densityJam,capacityDrop,stdDevCapacity);
	}
	
	/** Revert to original parameters for a list of links.
	 * 
	 * @param myScenario		The scenario.
	 * @param links				List of _Link objects.
	 * @return					_Event object
	 */
	public static _Event createEvent_Fundamental_Diagram_Revert(_Scenario myScenario,List <_Link> links) {		
		return  new com.relteq.sirius.event.Event_Fundamental_Diagram(myScenario,links);
	}
	
	/** On/Off switch for <i>all</i> controllers. 
	 * <p> This is equivalent to passing the full set of controllers to {@link ObjectFactory#createEvent_Control_Toggle}.
	 *
	 * @param myScenario		The scenario.
	 * @return					_Event object
	 */
	public static _Event createEvent_Global_Control_Toggle(_Scenario myScenario,boolean ison){
		return  new com.relteq.sirius.event.Event_Global_Control_Toggle(myScenario,ison);
	}	
	
	/** Adjust the global demand knob.
	 * 
	 * <p>The amount of traffic entering the network at a given source equals the nominal profile value 
	 * multiplied by both the knob for the profile and the global knob. Use this event to make 
	 * changes to the global demand knob.
	 * 
	 * @param myScenario		The scenario.
	 * @param newknob 			New value of the knob.
	 * @return			_Event object
	 */
	public static _Event createEvent_Global_Demand_Knob(_Scenario myScenario,double newknob){
		return  new com.relteq.sirius.event.Event_Global_Demand_Knob(myScenario,newknob);
	}	
	
	/** Adjust the knob for the demand profile applied to a particular link.
	 * 
	 * <p>Use this event to scale the demand profile applied to a given link.
	 * 
	 * @param myScenario		The scenario.
	 * @param newknob 			New value of the knob.
	 * @return					_Event object
	 */
	public static _Event createEvent_Link_Demand_Knob(_Scenario myScenario,double newknob){
		return  new com.relteq.sirius.event.Event_Link_Demand_Knob(myScenario,newknob);
	}	
	
	/** Change the number of lanes on a particular link.
	 * 
	 * @param myScenario		The scenario.
	 * @param links 			List of links to change.
	 * @param deltalanes		Number of lanes to add to each link in the list
	 * @return					_Event object
	 */
	public static _Event createEvent_Link_Lanes(_Scenario myScenario,List<_Link> links,boolean isrevert,double deltalanes){
		return  new com.relteq.sirius.event.Event_Link_Lanes(myScenario,links,isrevert,deltalanes);
	}	
	
	/** Change the split ratio matrix on a node.
	 * 
	 * @param myScenario		The scenario.
	 * @param node				The node
	 * @param splitratio		A Double3DMatrix with the new split ratio matrix.
	 * @return					_Event object
	 */		
	public static _Event createEvent_Node_Split_Ratio(_Scenario myScenario,_Node node,Double3DMatrix splitratio){
		return  new com.relteq.sirius.event.Event_Node_Split_Ratio(myScenario,node,splitratio);
	}	
	
	/////////////////////////////////////////////////////////////////////
	// public: sensor
	/////////////////////////////////////////////////////////////////////

	/** Create a fixed loop detector station.
	 * 
	 * <p> This sensor models a loop detector station with loops covering all lanes at a particular
	 * location on a link. 
	 * 
	 * @param myScenario		The scenario.
	 * @param networkId			The id of the network that contains the link.
	 * @param linkId			The id of the link where the sensor is placed.
	 * @return					_Sensor object
	 */
	public static _Sensor createSensor_LoopStation(_Scenario myScenario,String networkId,String linkId){
		return new com.relteq.sirius.sensor.SensorLoopStation(myScenario,networkId,linkId);
	}

	/** Create a floating detector.
	 * 
	 * <p> This sensor models a sensor that moves with the traffic stream. This sensor type can be used
	 * to model probe vehicles. The network and link ids in the parameter list correspond to the initial
	 * position of the sensor.
	 * 
	 * @param myScenario		The scenario.
	 * @param networkId			The id of the network that contains the link.
	 * @param linkId			The id of the link where the sensor is placed.
	 * @return			XXX
	 */
	public static _Sensor createSensor_Floating(_Scenario myScenario,String networkId,String linkId){
		_Sensor S = new com.relteq.sirius.sensor.SensorFloating(myScenario,networkId,linkId);
		return S;
	}

	/////////////////////////////////////////////////////////////////////
	// public: scenario element
	/////////////////////////////////////////////////////////////////////
	
	/** Container for a node.
	 * 
	 * @param node		The node.
	 * @return			ScenarioElement object
	 */
	public static _ScenarioElement createScenarioElement(_Node node){
		if(node==null)
			return null;
		_ScenarioElement se = new _ScenarioElement();
		se.myScenario = node.getMyNetwork().myScenario;
		se.myType = _ScenarioElement.Type.node;
		se.network_id = node.myNetwork.getId();
		se.reference = node;
		return se;
	}
	
	/** Container for a link.
	 * 
	 * @param link		The link.
	 * @return			_ScenarioElement object
	 */
	public static _ScenarioElement createScenarioElement(_Link link){
		if(link==null)
			return null;
		_ScenarioElement se = new _ScenarioElement();
		se.myScenario = link.getMyNetwork().myScenario;
		se.myType = _ScenarioElement.Type.link;
		se.network_id = link.myNetwork.getId();
		se.reference = link;
		return se;
	}

	/** Container for a sensor.
	 * 
	 * @param sensor	The sensor.
	 * @return			_ScenarioElement object
	 */
	public static _ScenarioElement createScenarioElement(_Sensor sensor){
		if(sensor==null)
			return null;
		_ScenarioElement se = new _ScenarioElement();
		se.myScenario = sensor.myScenario;
		se.myType = _ScenarioElement.Type.sensor;
		if(sensor.myLink!=null)
			se.network_id = sensor.myLink.myNetwork.getId();
		se.reference = sensor;
		return se;
	}
	
	/** Container for a controller.
	 * 
	 * @param controller	The controller.
	 * @return			_ScenarioElement object
	 */
	public static _ScenarioElement createScenarioElement(_Controller controller){
		if(controller==null)
			return null;
		_ScenarioElement se = new _ScenarioElement();
		se.myType = _ScenarioElement.Type.controller;
		se.reference = controller;
		return se;
	}

	/** Container for an event.
	 * 
	 * @param event	The event.
	 * @return			_ScenarioElement object
	 */
	public static _ScenarioElement createScenarioElement(_Event event){
		if(event==null)
			return null;
		_ScenarioElement se = new _ScenarioElement();
		se.myType = _ScenarioElement.Type.event;
		se.reference = event;
		return se;
	}

	/////////////////////////////////////////////////////////////////////
	// private
	/////////////////////////////////////////////////////////////////////

	// returns greatest common divisor among network time steps.
	// The time steps are rounded to the nearest decisecond.
	private static double computeCommonSimulationTimeInSeconds(_Scenario scenario){
		
		if(scenario.getNetworkList()==null)
			return Double.NaN;
		
		if(scenario.getNetworkList().getNetwork().size()==0)
			return Double.NaN;
			
		// loop through networks calling gcd
		double dt;
		List<Network> networkList = scenario.getNetworkList().getNetwork();
		int tengcd = 0;		// in deciseconds
		for(int i=0;i<networkList.size();i++){
			dt = networkList.get(i).getDt().doubleValue();	// in seconds
	        if( SiriusMath.lessthan(dt,0.1) ){
	        	SiriusErrorLog.addErrorMessage("Warning: Network dt given in hours. Changing to seconds.");
				dt *= 3600;
	        }
			tengcd = SiriusMath.gcd( SiriusMath.round(dt*10.0) , tengcd );
		}
    	return ((double)tengcd)/10.0;
	}
	
	// Simulation mode is normal <=> start time == initial profile time stamp
	private static void setSimulationMode(_Scenario scenario){

		if(scenario==null)
			return;
		
		scenario.simulationMode = null;
		
        double time_ic;
        if(scenario.getInitialDensityProfile()!=null)
        	time_ic = ((_InitialDensityProfile)scenario.getInitialDensityProfile()).timestamp;
        else
        	time_ic = 0.0;
       
		if(scenario.timestart==time_ic){
			scenario.simulationMode = _Scenario.ModeType.normal;
		}
		else{
			// it is a warmup. we need to decide on start and end times
			scenario.timeend = scenario.timestart;
			if(time_ic<scenario.timestart){	// go from ic to timestart
				scenario.timestart = time_ic;
				scenario.simulationMode = _Scenario.ModeType.warmupFromIC;
			}
			else{							// start at earliest demand profile
				scenario.timestart = Double.POSITIVE_INFINITY;
				for(DemandProfile D : scenario.getDemandProfileSet().getDemandProfile())
					scenario.timestart = Math.min(scenario.timestart,D.getStartTime().doubleValue());
				scenario.simulationMode = _Scenario.ModeType.warmupFromZero;
			}		
		}
	}
	
	private static boolean checkLoad(_Scenario scenario){
		
		if(scenario==null){
			SiriusErrorLog.setErrorHeader("Load failed.");
			return false;
		}
	
		// check timestart < timeend (depends on simulation mode)
		if(scenario.timestart>=scenario.timeend){
			SiriusErrorLog.setErrorHeader("Empty simulation period.");
			return false;
		}
		
		return true;
		
	}
	
}