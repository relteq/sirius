package com.relteq.sirius.simulator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import com.relteq.sirius.control.*;
import com.relteq.sirius.event.*;
import com.relteq.sirius.sensor.*;

/** Factory methods for creating scenarios, controllers, events, sensors, and scenario elements. 
 * <p>
 * Use the static methods in this class to load a scenario from XML, and to programmatically generate events, controllers, sensors, and scenario elements.
 * 
* @author Gabriel Gomes
*/
public final class ObjectFactory {

	/////////////////////////////////////////////////////////////////////
	// private default constructor
	/////////////////////////////////////////////////////////////////////

	private ObjectFactory(){}
							  
	/////////////////////////////////////////////////////////////////////
	// protected
	/////////////////////////////////////////////////////////////////////
	
	/** @y.exclude */
	protected static Controller createControllerFromJaxb(Scenario myScenario,com.relteq.sirius.jaxb.Controller jaxbC,Controller.Type myType) {		
		if(myScenario==null)
			return null;
		Controller C;
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
	protected static Event createEventFromJaxb(Scenario myScenario,com.relteq.sirius.jaxb.Event jaxbE,Event.Type myType) {	
		if(myScenario==null)
			return null;
		Event E;
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
	protected static Sensor createSensorFromJaxb(Scenario myScenario,com.relteq.sirius.jaxb.Sensor jaxbS,Sensor.Type myType) {	
		if(myScenario==null)
			return null;
		Sensor S;
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
	protected static ScenarioElement createScenarioElementFromJaxb(Scenario myScenario,com.relteq.sirius.jaxb.ScenarioElement jaxbS){
		if(myScenario==null)
			return null;
		ScenarioElement S = new ScenarioElement();
		S.myScenario = myScenario;
		S.setId(jaxbS.getId().trim());
		S.myType = ScenarioElement.Type.valueOf(jaxbS.getType());
		switch(S.myType){
		case link:
			S.reference = myScenario.getLinkWithId(S.getId());
			break;
		case node:
			S.reference = myScenario.getNodeWithId(S.getId());
			break;
		case sensor:
			S.reference = myScenario.getSensorWithId(S.getId());
			break;
		case signal:
			S.reference = myScenario.getSignalWithId(S.getId());
			break;
		case controller:
			S.reference = myScenario.getControllerWithId(S.getId());
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
	 * @return scenario				Scenario object.
	 */
	public static Scenario createAndLoadScenario(String configfilename) {

		JAXBContext context;
		Unmarshaller u;
    	
    	// create unmarshaller .......................................................
        try {
        	context = JAXBContext.newInstance("com.relteq.sirius.jaxb");
            u = context.createUnmarshaller();
        } catch( JAXBException je ) {
        	SiriusErrorLog.addError("Failed to create context for JAXB unmarshaller.");
            SiriusErrorLog.print();
            return null;
        }
        
        // schema assignment ..........................................................
        try{
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            ClassLoader classLoader = ObjectFactory.class.getClassLoader();            
        	Schema schema = factory.newSchema(classLoader.getResource("sirius.xsd"));
        	u.setSchema(schema);
        } catch(SAXException e){
        	SiriusErrorLog.addError("Schema not found.");
            SiriusErrorLog.print();
        	return null;
        }
        
        // process configuration file name ...........................................
		if(!configfilename.endsWith(".xml"))
			configfilename += ".xml";

        // read and return ...........................................................
        Scenario S = new Scenario();
        try {
        	setObjectFactory(u, new JaxbObjectFactory());
        	S = (Scenario) u.unmarshal( new FileInputStream(configfilename) );
        } catch( JAXBException je ) {
        	SiriusErrorLog.addError("JAXB threw an exception when loading the configuration file.");
        	if(je.getLinkedException()!=null)
        		SiriusErrorLog.addError(je.getLinkedException().getMessage());
            SiriusErrorLog.print();
            return null;
        } catch (FileNotFoundException e) {
        	SiriusErrorLog.addError("Configuration file not found.");
            SiriusErrorLog.print();
        	return null;
		}
        
        if(S==null){
        	SiriusErrorLog.addError("Unknown load error.");
            SiriusErrorLog.print();
        	return null;
		}

        // copy in input parameters ..................................................
        S.configfilename = configfilename;

		return process(S);
	}

	/**
	 * Updates a scenario loaded by JAXB
	 * @param S a scenario
	 * @return the updated scenario or null if an error occurred
	 */
	public static Scenario process(Scenario S) {
        // copy data to static variables ..............................................
        S.global_control_on = true;
        S.simdtinseconds = computeCommonSimulationTimeInSeconds(S);
        S.simdtinhours = S.simdtinseconds/3600.0;
        S.uncertaintyModel = Scenario.UncertaintyType.uniform;
        S.global_demand_knob = 1d;
        S.numVehicleTypes = 1;
        
        if(S.getSettings()!=null)
	        if(S.getSettings().getVehicleTypes()!=null)
	            if(S.getSettings().getVehicleTypes().getVehicleType()!=null) 
	        		S.numVehicleTypes = S.getSettings().getVehicleTypes().getVehicleType().size();
	            	
	            	
        // populate the scenario ....................................................
        try{
        	S.populate();
        } catch (SiriusException e){
        	SiriusErrorLog.addError(e.getMessage());
            SiriusErrorLog.print();
        	return null;
        }
        
        // register controllers with their targets ..................................
        boolean registersuccess = true;
        for(Controller controller : S.controllerset.controllers)
        	registersuccess &= controller.register();
    	if(S.getSignalList()!=null)
        	for(com.relteq.sirius.jaxb.Signal signal:S.getSignalList().getSignal())
        		registersuccess &= ((Signal)signal).register();
        
        if(!registersuccess){
        	SiriusErrorLog.addError("Controller registration failure.");
            SiriusErrorLog.print();
        	return null;
        }
		
		// validate scenario ......................................
        SiriusErrorLog.clearErrorMessage();
        S.validate();
        
        // print errors and warnings
        SiriusErrorLog.print();
        
        if(SiriusErrorLog.haserror())
        	return null;
        else
        	return S;	
	}

	public static void setObjectFactory(Unmarshaller unmrsh, Object factory) throws PropertyException {
		final String classname = unmrsh.getClass().getName();
		String propnam = classname.startsWith("com.sun.xml.internal") ?//
				"com.sun.xml.internal.bind.ObjectFactory" ://
				"com.sun.xml.bind.ObjectFactory";
		unmrsh.setProperty(propnam, factory);
	}

	/////////////////////////////////////////////////////////////////////
	// public: controller
	/////////////////////////////////////////////////////////////////////
	
	/** [NOT IMPLEMENTED]. HERO coordinated ramp metering..
	 * 
	 * @return			_Controller object
	 */
	public static Controller createController_CRM_HERO(Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_CRM_HERO(myScenario);
	}

	/** [NOT IMPLEMENTED] SWARM coordinated ramp metering.
	 * 
	 * @return			_Controller object
	 */
	public static Controller createController_CRM_SWARM(Scenario myScenario){
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
	public static Controller createController_IRM_Alinea(Scenario myScenario,Link onramplink, Link mainlinelink,Sensor mainlinesensor,Sensor queuesensor,double gain){
		return  new com.relteq.sirius.control.Controller_IRM_Alinea(myScenario,onramplink,mainlinelink,mainlinesensor,queuesensor,gain);
	}
	
	/** [NOT IMPLEMENTED] Time of day isolated ramp metering.
	 * 
	 * @return			_Controller object
	 */
	public static Controller createController_IRM_Time_of_Day(Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_IRM_Time_of_Day(myScenario);
	}

	/** [NOT IMPLEMENTED] Traffic responsive isolated ramp metering.
	 * 
	 * @return			_Controller object
	 */
	public static Controller createController_IRM_Traffic_Responsive(Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_IRM_Traffic_Responsive(myScenario);
	}

	/** [NOT IMPLEMENTED] Actuated signal control.
	 * 
	 * @return			_Controller object
	 */
	public static Controller createController_SIG_Actuated(Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_SIG_Actuated(myScenario);
	}

	/** [NOT IMPLEMENTED] Pretimed signal control.
	 * 
	 * @return			_Controller object
	 */
	public static Controller createController_SIG_Pretimed(Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_SIG_Pretimed(myScenario);
	}

	/** [NOT IMPLEMENTED] Time of day variable speed limits.
	 * 
	 * @return			_Controller object
	 */
	public static Controller createController_VSL_Time_of_Day(Scenario myScenario){
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
	public static Event createEvent_Control_Toggle(Scenario myScenario,float timestampinseconds,List <Controller> controllers,boolean ison) {
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
	public static Event createEvent_Fundamental_Diagram(Scenario myScenario,List <Link> links,double freeflowSpeed,double congestionSpeed,double capacity,double densityJam,double capacityDrop,double stdDevCapacity) {		
		return  new com.relteq.sirius.event.Event_Fundamental_Diagram(myScenario,links,freeflowSpeed,congestionSpeed,capacity,densityJam,capacityDrop,stdDevCapacity);
	}
	
	/** Revert to original parameters for a list of links.
	 * 
	 * @param myScenario		The scenario.
	 * @param links				List of _Link objects.
	 * @return					_Event object
	 */
	public static Event createEvent_Fundamental_Diagram_Revert(Scenario myScenario,List <Link> links) {		
		return  new com.relteq.sirius.event.Event_Fundamental_Diagram(myScenario,links);
	}
	
	/** On/Off switch for <i>all</i> controllers. 
	 * <p> This is equivalent to passing the full set of controllers to {@link ObjectFactory#createEvent_Control_Toggle}.
	 *
	 * @param myScenario		The scenario.
	 * @return					_Event object
	 */
	public static Event createEvent_Global_Control_Toggle(Scenario myScenario,boolean ison){
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
	public static Event createEvent_Global_Demand_Knob(Scenario myScenario,double newknob){
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
	public static Event createEvent_Link_Demand_Knob(Scenario myScenario,double newknob){
		return  new com.relteq.sirius.event.Event_Link_Demand_Knob(myScenario,newknob);
	}	
	
	/** Change the number of lanes on a particular link.
	 * 
	 * @param myScenario		The scenario.
	 * @param links 			List of links to change.
	 * @param deltalanes		Number of lanes to add to each link in the list
	 * @return					_Event object
	 */
	public static Event createEvent_Link_Lanes(Scenario myScenario,List<Link> links,boolean isrevert,double deltalanes){
		return  new com.relteq.sirius.event.Event_Link_Lanes(myScenario,links,isrevert,deltalanes);
	}	
	
	/** Change the split ratio matrix on a node.
	 * 
	 * @param myScenario		The scenario.
	 * @param node				The node
	 * @param inlink			String id of the input link 
	 * @param vehicleType		String name of the vehicle type
	 * @param splits			An array of splits for every link exiting the node.
	 * @return					_Event object
	 */		
	public static Event createEvent_Node_Split_Ratio(Scenario myScenario,Node node,String inlink,String vehicleType,ArrayList<Double>splits){
		return  new com.relteq.sirius.event.Event_Node_Split_Ratio(myScenario,node,inlink,vehicleType,splits);
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
	 * @param linkId			The id of the link where the sensor is placed.
	 * @return					_Sensor object
	 */
	public static Sensor createSensor_LoopStation(Scenario myScenario,String linkId){
		return new com.relteq.sirius.sensor.SensorLoopStation(myScenario,linkId);
	}

	/** Create a floating detector.
	 * 
	 * <p> This sensor models a sensor that moves with the traffic stream. This sensor type can be used
	 * to model probe vehicles. The network and link ids in the parameter list correspond to the initial
	 * position of the sensor.
	 * 
	 * @param myScenario		The scenario.
	 * @param linkId			The id of the link where the sensor is placed.
	 * @return			XXX
	 */
	public static Sensor createSensor_Floating(Scenario myScenario,String linkId){
		Sensor S = new com.relteq.sirius.sensor.SensorFloating(myScenario,linkId);
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
	public static ScenarioElement createScenarioElement(Node node){
		if(node==null)
			return null;
		ScenarioElement se = new ScenarioElement();
		se.myScenario = node.getMyNetwork().myScenario;
		se.myType = ScenarioElement.Type.node;
//		se.setNetworkId(node.myNetwork.getId());
		se.reference = node;
		return se;
	}
	
	/** Container for a link.
	 * 
	 * @param link		The link.
	 * @return			_ScenarioElement object
	 */
	public static ScenarioElement createScenarioElement(Link link){
		if(link==null)
			return null;
		ScenarioElement se = new ScenarioElement();
		se.myScenario = link.getMyNetwork().myScenario;
		se.myType = ScenarioElement.Type.link;
//		se.setNetworkId(link.myNetwork.getId());
		se.reference = link;
		return se;
	}

	/** Container for a sensor.
	 * 
	 * @param sensor	The sensor.
	 * @return			_ScenarioElement object
	 */
	public static ScenarioElement createScenarioElement(Sensor sensor){
		if(sensor==null)
			return null;
		ScenarioElement se = new ScenarioElement();
		se.myScenario = sensor.myScenario;
		se.myType = ScenarioElement.Type.sensor;
//		if(sensor.myLink!=null)
//			se.setNetworkId(sensor.myLink.myNetwork.getId());
		se.reference = sensor;
		return se;
	}
	
	/** Container for a controller.
	 * 
	 * @param controller	The controller.
	 * @return			_ScenarioElement object
	 */
	public static ScenarioElement createScenarioElement(Controller controller){
		if(controller==null)
			return null;
		ScenarioElement se = new ScenarioElement();
		se.myType = ScenarioElement.Type.controller;
		se.reference = controller;
		return se;
	}

	/** Container for an event.
	 * 
	 * @param event	The event.
	 * @return			_ScenarioElement object
	 */
	public static ScenarioElement createScenarioElement(Event event){
		if(event==null)
			return null;
		ScenarioElement se = new ScenarioElement();
		se.myType = ScenarioElement.Type.event;
		se.reference = event;
		return se;
	}

	/////////////////////////////////////////////////////////////////////
	// private
	/////////////////////////////////////////////////////////////////////

	// returns greatest common divisor among network time steps.
	// The time steps are rounded to the nearest decisecond.
	private static double computeCommonSimulationTimeInSeconds(Scenario scenario){
		
		if(scenario.getNetworkList()==null)
			return Double.NaN;
		
		if(scenario.getNetworkList().getNetwork().size()==0)
			return Double.NaN;
			
		// loop through networks calling gcd
		double dt;
		List<com.relteq.sirius.jaxb.Network> networkList = scenario.getNetworkList().getNetwork();
		int tengcd = 0;		// in deciseconds
		for(int i=0;i<networkList.size();i++){
			dt = networkList.get(i).getDt().doubleValue();	// in seconds
	        if( SiriusMath.lessthan( Math.abs(dt) ,0.1) ){
	        	SiriusErrorLog.addError("Warning: Network dt given in hours. Changing to seconds.");
				dt *= 3600;
	        }
			tengcd = SiriusMath.gcd( SiriusMath.round(dt*10.0) , tengcd );
		}
    	return ((double)tengcd)/10.0;
	}
	
}