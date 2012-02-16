package com.relteq.sirius.simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import com.relteq.sirius.jaxb.Controller;
import com.relteq.sirius.jaxb.DemandProfile;
import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.jaxb.Network;
import com.relteq.sirius.jaxb.ScenarioElement;
import com.relteq.sirius.jaxb.Sensor;

public final class ObjectFactory {

	/////////////////////////////////////////////////////////////////////
	// protected
	/////////////////////////////////////////////////////////////////////
		
	protected static _Controller createControllerFromJaxb(Controller jaxbC,_Controller.Type myType) {		
		
		_Controller C;
		switch(myType){
			case IRM_alinea:
				C = new com.relteq.sirius.control.Controller_IRM_Alinea(jaxbC);
				break;
				
			case IRM_time_of_day:
				C = null; // new com.relteq.sirius.control.Controller_IRM_TOD(jaxbC);
				break;
				
			case IRM_traffic_responsive:
				C = null; // new com.relteq.sirius.control.Controller_IRM_TrafficResponsive(jaxbC);
				break;
	
			case CRM_swarm:
				C = null; // new com.relteq.sirius.control.Controller_IRM_SWARM(jaxbC);
				break;
				
			case CRM_hero:
				C = null; // new com.relteq.sirius.control.Controller_IRM_HERO(jaxbC);
				break;
				
			case VSL_time_of_day:
				C = null; // new com.relteq.sirius.control.Controller_VSL_TOD(jaxbC);
				break;
				
			case SIG_pretimed:
				C = null; // new com.relteq.sirius.control.Controller_SIG_Pretimed(jaxbC);
				break;
				
			case SIG_actuated:
				C = null; // new com.relteq.sirius.control.Controller_SIG_Actuated(jaxbC);
				break;
				
			default:
				C = null;
				break;
		}
		
		return C;

	}
		
	protected static _Event createEventFromJaxb(Event jaxbE,_Event.Type myType) {	
		_Event E = new _Event();
		E.myType = myType;
		E.timestampstep = SiriusMath.round(jaxbE.getTstamp().floatValue()/API.getSimDtInSeconds());		// assume in seconds
		E.targets = new ArrayList<_ScenarioElement>();
		for(ScenarioElement s : jaxbE.getTargetElements().getScenarioElement() )
			E.targets.add(new _ScenarioElement(s));
		return E;
	}

	protected static _Sensor createSensorFromJaxb(Sensor jaxbS,_Sensor.Type myType) {	
		
		_Sensor S;
		switch(myType){
			case static_point:
				S = new com.relteq.sirius.sensor.SensorLoopStation(jaxbS);
				break;

			case static_area:
				S = null; 
				break;

			case moving_point:
				S = new com.relteq.sirius.sensor.SensorFloating(jaxbS);
				break;
				
			default:
				S = null;
				break;
		}
		
		return S;
		
	}

	/////////////////////////////////////////////////////////////////////
	// public
	/////////////////////////////////////////////////////////////////////

	public static _Scenario createAndLoadScenario(String filename) {

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
        File schemaLocation = new File(Global.schemafile);
        try{
        	Schema schema = factory.newSchema(schemaLocation);
        	u.setSchema(schema);
        } catch(SAXException e){
        	SiriusError.addErrorMessage("Schema not found.");
        	return null;
        }
        
        // read and return ...........................................................
        _Scenario S = new _Scenario();
        try {
            u.setProperty("com.sun.xml.internal.bind.ObjectFactory",new _JaxbObjectFactory());            
        	S = (_Scenario) u.unmarshal( new FileInputStream(filename) );
        } catch( JAXBException je ) {
        	SiriusError.addErrorMessage("JAXB threw an exception when loading the configuration file.");
        	if(je.getLinkedException()!=null)
        		SiriusError.addErrorMessage(je.getLinkedException().getMessage());
            return null;
        } catch (FileNotFoundException e) {
        	SiriusError.addErrorMessage("Configuration file not found.");
        	return null;
		}

        // copy data to static variables ..............................................
        S.controlon = true;
        //Utils.theScenario = S;
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
        Global.clock = new Clock(Global.timestart,Global.timeend,S.simdtinseconds);

        // populate the scenario ....................................................
        S.populate();
        
        // check that load was successful
        S.isloaded = checkLoad(S);
        
        return S;
		
	}
	
	public static _Controller createController_IRM_Alinea(_Link onramplink, _Link mainlinelink,_Sensor mainlinesensor,_Sensor queuesensor,double gain){
		return  new com.relteq.sirius.control.Controller_IRM_Alinea(onramplink,mainlinelink,mainlinesensor,queuesensor,gain);
	}
	
	public static _Sensor createSensor_LoopStation(String networkId,String linkId){
		return new com.relteq.sirius.sensor.SensorLoopStation(networkId,linkId);
	}

	public static _Sensor createSensor_Floating(String networkId,String linkId){
		_Sensor S = new com.relteq.sirius.sensor.SensorFloating(networkId,linkId);
		return S;
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
		
        double time_ic = ((_InitialDensityProfile)scenario.getInitialDensityProfile()).timestamp;
		if(Global.timestart==time_ic){
			scenario.simulationMode = _Scenario.ModeType.normal;
		}
		else{
			// it is a warmup. we need to decide on start and end times
			Global.timeend = Global.timestart;
			if(time_ic<Global.timestart){	// go from ic to timestart
				Global.timestart = time_ic;
				scenario.simulationMode = _Scenario.ModeType.warmupFromIC;
			}
			else{							// start at earliest demand profile
				Global.timestart = Double.POSITIVE_INFINITY;
				for(DemandProfile D : scenario.getDemandProfileSet().getDemandProfile())
					Global.timestart = Math.min(Global.timestart,D.getStartTime().doubleValue());
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
		if(Global.timestart>=Global.timeend){
			SiriusError.setErrorHeader("Empty simulation period.");
			return false;
		}
		
		return true;
		
	}
	
}
