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

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public final class ObjectFactory {

	private static String schemafile = "data/schema/sirius.xsd";
	
	
	/////////////////////////////////////////////////////////////////////
	// protected
	/////////////////////////////////////////////////////////////////////
		
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
	// public
	/////////////////////////////////////////////////////////////////////

	public static _Scenario createAndLoadScenario(String configfilename,String outputfileprefix,double timestart,double timeend,double outdt) {

		JAXBContext context;
		Unmarshaller u;
    	
    	// create unmarshaller .......................................................
        try {
        	context = JAXBContext.newInstance("com.relteq.sirius.jaxb");
            u = context.createUnmarshaller();
        } catch( JAXBException je ) {
        	SiriusError.addErrorMessage("Failed to create context for JAXB unmarshaller.");
            return null;
        }
        
        // schema assignment ..........................................................
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        File schemaLocation = new File(ObjectFactory.schemafile);
        try{
        	Schema schema = factory.newSchema(schemaLocation);
        	u.setSchema(schema);
        } catch(SAXException e){
        	SiriusError.addErrorMessage("Schema not found.");
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
        	SiriusError.addErrorMessage("JAXB threw an exception when loading the configuration file.");
        	if(je.getLinkedException()!=null)
        		SiriusError.addErrorMessage(je.getLinkedException().getMessage());
            return null;
        } catch (FileNotFoundException e) {
        	SiriusError.addErrorMessage("Configuration file not found.");
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
        S.populate();
        
        // register controllers with their targets ..................................
        boolean registersuccess = true;
        for(_Controller controller : S._controllerset._controllers)
        	registersuccess &= controller.register();
        
        if(!registersuccess){
        	System.out.println("Conflicting controllers");
        	return null;
        }
        
        // check that load was successful
        S.isloaded = checkLoad(S);
        
        return S;
		
	}
	
	public static _Controller createController_CRM_HERO(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_CRM_HERO(myScenario);
	}

	public static _Controller createController_CRM_SWARM(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_CRM_SWARM(myScenario);
	}

	public static _Controller createController_IRM_Alinea(_Scenario myScenario,_Link onramplink, _Link mainlinelink,_Sensor mainlinesensor,_Sensor queuesensor,double gain){
		return  new com.relteq.sirius.control.Controller_IRM_Alinea(myScenario,onramplink,mainlinelink,mainlinesensor,queuesensor,gain);
	}
	
	public static _Controller createController_IRM_Time_of_Day(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_IRM_Time_of_Day(myScenario);
	}

	public static _Controller createController_IRM_Traffic_Responsive(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_IRM_Traffic_Responsive(myScenario);
	}

	public static _Controller createController_SIG_Actuated(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_SIG_Actuated(myScenario);
	}

	public static _Controller createController_SIG_Pretimed(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_SIG_Pretimed(myScenario);
	}

	public static _Controller createController_VSL_Time_of_Day(_Scenario myScenario){
		return  new com.relteq.sirius.control.Controller_VSL_Time_of_Day(myScenario);
	}
	
	public static _Event createEvent_Control_Toggle(_Scenario myScenario,boolean ison){
		return  new com.relteq.sirius.event.Event_Control_Toggle(myScenario,ison);
	}	

	public static _Event createEvent_Fundamental_Diagram(_Scenario myScenario){
		return  new com.relteq.sirius.event.Event_Fundamental_Diagram(myScenario);
	}	
	
	public static _Event createEvent_Global_Control_Toggle(_Scenario myScenario,boolean ison){
		return  new com.relteq.sirius.event.Event_Global_Control_Toggle(myScenario,ison);
	}	
	
	public static _Event createEvent_Global_Demand_Knob(_Scenario myScenario,double newknob){
		return  new com.relteq.sirius.event.Event_Global_Demand_Knob(myScenario,newknob);
	}	
	
	public static _Event createEvent_Link_Demand_Knob(_Scenario myScenario,double newknob){
		return  new com.relteq.sirius.event.Event_Link_Demand_Knob(myScenario,newknob);
	}	
	
	public static _Event createEvent_Link_Lanes(_Scenario myScenario,double deltalanes){
		return  new com.relteq.sirius.event.Event_Link_Lanes(myScenario,deltalanes);
	}	
	
	public static _Event createEvent_Node_Split_Ratio(_Scenario myScenario,boolean isreset,_Node node,Double3DMatrix splitratio){
		return  new com.relteq.sirius.event.Event_Node_Split_Ratio(myScenario,isreset,node,splitratio);
	}	
	
	public static _Sensor createSensor_LoopStation(_Scenario myScenario,String networkId,String linkId){
		return new com.relteq.sirius.sensor.SensorLoopStation(myScenario,networkId,linkId);
	}

	public static _Sensor createSensor_Floating(_Scenario myScenario,String networkId,String linkId){
		_Sensor S = new com.relteq.sirius.sensor.SensorFloating(myScenario,networkId,linkId);
		return S;
	}
	
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

//	public static _ScenarioElement createScenarioElement(_Signal signal){
//		if(signal==null)
//			return null;
//		_ScenarioElement se = new _ScenarioElement();
//		se.myScenario = signal.getMyNetwork().myScenario;
//		se.myType = _ScenarioElement.Type.sensor;
//		se.network_id = signal.myNetwork.getId();
//		se.id = "";
//		se.reference = signal;
//		return se;
//	}
	
	public static _ScenarioElement createScenarioElement(_Controller controller){
		if(controller==null)
			return null;
		_ScenarioElement se = new _ScenarioElement();
		se.myType = _ScenarioElement.Type.controller;
		se.reference = controller;
		return se;
	}

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
				System.out.println("Warning: Network dt given in hours. Changing to seconds.");
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
		
		scenario.simulationMode = _Scenario.ModeType.NULL;
		
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
			SiriusError.setErrorHeader("Load failed.");
			return false;
		}
	
		// check timestart < timeend (depends on simulation mode)
		if(scenario.timestart>=scenario.timeend){
			SiriusError.setErrorHeader("Empty simulation period.");
			return false;
		}
		
		return true;
		
	}
	
}