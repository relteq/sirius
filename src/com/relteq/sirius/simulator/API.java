package com.relteq.sirius.simulator;

import java.util.List;

import com.relteq.sirius.jaxb.VehicleType;

public class API {

	public static _Scenario getTheScenario() {
		return Global.theScenario;
	}
	
	public double getTime() {
		if(Global.clock==null)
			return Double.NaN;
		return Global.clock.getT();
	}

	public int getCurrentTimeStep() {
		if(Global.clock==null)
			return 0;
		return Global.clock.getCurrentstep();
	}
	
	public static String getConfigFileName() {
		return Global.configfilename;
	}

	public static int getNumVehicleTypes() {
		if(Global.theScenario==null)
			return -1;
		return Global.theScenario.numVehicleTypes;
	}

	public static double getSimDtInSeconds() {
		if(Global.theScenario==null)
			return Double.NaN;
		return Global.theScenario.simdtinseconds;
	}

	public static double getSimDtInHours() {
		if(Global.theScenario==null)
			return Double.NaN;
		return Global.theScenario.simdtinhours;
	}

	public static double getOutDt() {
		return Global.outdt;
	}

	public static double getTimeStart() {
		return Global.timestart;
	}

	public static double getTimeEnd() {
		return Global.timeend;
	}
	
	// In lieu of id, we are using the name.
	public static _Controller getControllerWithName(String id){
		if(Global.theScenario==null)
			return null;
		if(Global.theScenario._controllerset==null)
			return null;
		for(_Controller c : Global.theScenario._controllerset.get_Controllers()){
			if(c.name.equals(id))
				return c;
		}
		return null;
	}
	
	public static _Node getNodeWithCompositeId(String network_id,String id){
		if(Global.theScenario==null)
			return null;
		if(Global.theScenario.getNetworkList()==null)
			return null;
		_Network network = Global.theScenario.getNetworkWithId(network_id);
		if(network==null)
			if(Global.theScenario.getNetworkList().getNetwork().size()==1)
				return ((_Network) Global.theScenario.getNetworkList().getNetwork().get(0)).getNodeWithId(id);
			else
				return null;
		else	
			return network.getNodeWithId(id);
	}

	public static _Link getLinkWithCompositeId(String network_id,String id){
		if(Global.theScenario==null)
			return null;
		if(Global.theScenario.getNetworkList()==null)
			return null;
		_Network network = Global.theScenario.getNetworkWithId(network_id);
		if(network==null)
			if(Global.theScenario.getNetworkList().getNetwork().size()==1)
				return ((_Network) Global.theScenario.getNetworkList().getNetwork().get(0)).getLinkWithId(id);
			else
				return null;
		else	
			return network.getLinkWithId(id);
	}
	
	public static _Sensor getSensorWithCompositeId(String network_id,String id){
		if(Global.theScenario==null)
			return null;
		if(Global.theScenario.getNetworkList()==null)
			return null;
		_Network network = Global.theScenario.getNetworkWithId(network_id);
		if(network==null)
			if(Global.theScenario.getNetworkList().getNetwork().size()==1)
				return ((_Network) Global.theScenario.getNetworkList().getNetwork().get(0)).getSensorWithId(id);
			else
				return null;
		else	
			return network.getSensorWithId(id);
	}
	
	public static int getVehicleTypeIndex(String name){
		if(Global.theScenario==null)
			return -1;
		if(Global.theScenario.getSettings()==null)
			return -1;
		if(Global.theScenario.getSettings().getVehicleTypes()==null)
			return -1;
		List<VehicleType> vt = Global.theScenario.getSettings().getVehicleTypes().getVehicleType();
		for(int i=0;i<vt.size();i++){
			if(vt.get(i).getName().equals(name))
				return i;
		}
		return -1;
	}
	
	public static void addController(_Controller C){
		if(Global.theScenario==null)
			return;
		if(Global.theScenario.isrunning)
			return;
		if(C==null)
			return;
		if(C.myType==_Controller.Type.NULL)
			return;
		
		// validate
		if(!C.validate())
			return;
		// add
		Global.theScenario._controllerset._controllers.add(C);
	}
	
	public static void addEvent(_Event E){
		if(Global.theScenario==null)
			return;
		if(Global.theScenario.isrunning)
			return;
		if(E==null)
			return;
		if(E.myType==_Event.Type.NULL)
			return;
		
		// validate
		if(!E.validate())
			return;
		
		// add event to list
		Global.theScenario._eventset.addEvent(E);
	}
	
	public static void addSensor(_Sensor S){
		if(Global.theScenario==null)
			return;
		if(Global.theScenario.isrunning)
			return;
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
