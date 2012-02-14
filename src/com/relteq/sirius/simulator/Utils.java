/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;


import org.xml.sax.SAXException;

import com.relteq.sirius.jaxb.Network;
import com.relteq.sirius.jaxb.Scenario;
import com.relteq.sirius.jaxb.VehicleType;

public class Utils {
	
	/////////////////////////////////////////////////////////////////////
	// static data
	/////////////////////////////////////////////////////////////////////
	
	public static final double EPSILON = (double) 1e-4;
	
	public static Clock clock;
	
	public static _Scenario theScenario; 
	public static int numVehicleTypes;			// number of vehicle types
	public static boolean controlon;			// global control switch
	protected static double simdtinseconds;		// [sec] simulation time step 
	protected static double simdtinhours;		// [hr]  simulation time step 	
	protected static int numRepititions;		// [#] number of repititions of the simulation
	
	protected static Types.Mode simulationMode;
	protected static Types.Uncertainty uncertaintyModel;
	
	// from configuration file
	protected static double outdt;				// [sec] output sampling time
	protected static double timestart;			// [sec] start of the simulation
	protected static double timeend;			// [sec] end of the simulation
	
	protected static String configfilename;
	protected static String outputfile_density;
	protected static String outputfile_outflow;
	protected static String outputfile_inflow;
	protected static OutputWriter outputwriter = null;

	private static String schemafile = "data/schema/sirius.xsd";

	private static String errorheader = new String();
	private static ArrayList<String> errormessage = new ArrayList<String>();

	public static Random random = new Random();
	
	/////////////////////////////////////////////////////////////////////
	// error handling
	/////////////////////////////////////////////////////////////////////
	
	protected static void clearErrorMessage(){
		errormessage.clear();
	}

	protected static void printErrorMessage(){
		if(!errorheader.isEmpty())
			System.out.println("Error: " + errorheader);
		if(!errormessage.isEmpty()){
			if(errormessage.size()==1)
				System.out.println(errormessage.get(0));
			else
				for(int i=0;i<errormessage.size();i++)
					System.out.println(i+1 + ") " + errormessage.get(i));
		}
	}

	protected static void addErrorMessage(String str){
		errormessage.add(str);
	}

	protected static void setErrorHeader(String str){
		errorheader = str;
	}
	
	/////////////////////////////////////////////////////////////////////
	// math helpers
	/////////////////////////////////////////////////////////////////////
	
 	public static Double [] zeros(int n){
		Double [] answ = new Double [n];
		for(int i=0;i<n;i++)
			answ[i] = 0.0;
		return answ;	
	}
	
	public static Double sum(Double [] V){
		Double answ = 0d;
		for(int i=0;i<V.length;i++)
			answ += V[i];
		return answ;
	}
	
	public static Double [] times(Double [] V,double a){
		Double [] answ = new Double [V.length];
		for(int i=0;i<V.length;i++)
			answ[i] = a*V[i];
		return answ;
	}
	
	public static int ceil(double a){
		return (int) Math.ceil(a-Utils.EPSILON);
	}
	
	public static int floor(double a){
		return (int) Math.floor(a+Utils.EPSILON);
	}
	
	public static int round(double a){
		return (int) Math.round(a);
	}
	
	public static boolean any (boolean [] x){
		for(int i=0;i<x.length;i++)
			if(x[i])
				return true;
		return false;
	}
	
	public static boolean all (boolean [] x){
		for(int i=0;i<x.length;i++)
			if(!x[i])
				return false;
		return true;
	}
	
	public static boolean[] not(boolean [] x){
		boolean [] y = x.clone();
		for(int i=0;i<y.length;i++)
			y[i] = !y[i];
		return y;
	}
	
	public static int count(boolean [] x){
		int s = 0;
		for(int i=0;i<x.length;i++)
			if(x[i])
				s++;
		return s;
	}
	
	public static ArrayList<Integer> find(boolean [] x){
		ArrayList<Integer> r = new ArrayList<Integer>();
		for(int i=0;i<x.length;i++)
			if(x[i])
				r.add(i);
		return r;
	}
	
	public static boolean isintegermultipleof(Double A,Double a){
		if(A.isInfinite())
			return true;
		return Utils.equals( Utils.round(A/a) , A/a );
	}
	
	public static boolean equals(double a,double b){
		return Math.abs(a-b) < Utils.EPSILON;
	}
	
	public static boolean greaterthan(double a,double b){
		return a > b + Utils.EPSILON;
	}

	public static boolean greaterorequalthan(double a,double b){
		return !lessthan(a,b);
	}
	
	public static boolean lessthan(double a,double b){
		return a < b - Utils.EPSILON;
	}

	public static boolean lessorequalthan(double a,double b){
		return !greaterthan(a,b);
	}
	
	// greatest common divisor of two integers
	public static int gcd(int p, int q) {
		if (q == 0) {
			return p;
		}
		return gcd(q, p % q);
	}
	
	////////////////////////////////////////////////////////////////////////////
	// load and save scenarios
	////////////////////////////////////////////////////////////////////////////

	public static _Scenario load(String filename){
        
		JAXBContext context;
		Unmarshaller u;
    	
    	// create unmarshaller .......................................................
        try {
        	context = JAXBContext.newInstance("com.relteq.sirius.jaxb");
            u = context.createUnmarshaller();
        } catch( JAXBException je ) {
        	Utils.addErrorMessage("Failed to create context for JAXB unmarshaller.");
            return null;
        }
        
        // schema assignment ..........................................................
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        File schemaLocation = new File(Utils.schemafile);
        try{
        	Schema schema = factory.newSchema(schemaLocation);
        	u.setSchema(schema);
        } catch(SAXException e){
        	Utils.addErrorMessage("Schema not found.");
        	return null;
        }
        
        // read and return ...........................................................
        _Scenario S = new _Scenario();
        try {
            u.setProperty("com.sun.xml.internal.bind.ObjectFactory",new _ObjectFactory());            
        	S = (_Scenario) u.unmarshal( new FileInputStream(filename) );
        } catch( JAXBException je ) {
        	Utils.addErrorMessage("JAXB threw an exception when loading the configuration file.");
        	if(je.getLinkedException()!=null)
        		Utils.addErrorMessage(je.getLinkedException().getMessage());
            return null;
        } catch (FileNotFoundException e) {
        	Utils.addErrorMessage("Configuration file not found.");
        	return null;
		}

        // copy data to static variables ..............................................
        Utils.controlon = true;
        Utils.theScenario = S;
        Utils.simdtinseconds = computeCommonSimulationTimeInSeconds();
        Utils.simdtinhours = Utils.simdtinseconds/3600.0;
        Utils.uncertaintyModel = Types.Uncertainty.uniform;
        if(S.getSettings().getVehicleTypes()==null)
            Utils.numVehicleTypes = 1;
        else
        	if(S.getSettings().getVehicleTypes().getVehicleType()!=null) 
        		Utils.numVehicleTypes = S.getSettings().getVehicleTypes().getVehicleType().size();
        
        // populate the scenario ....................................................
        S.populate();
        
        return S;
	}	
	
	public static void save(Scenario scenario,String filename){
        try {
        	JAXBContext context = JAXBContext.newInstance("aurora.jaxb");
        	Marshaller m = context.createMarshaller();
        	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        	m.marshal(scenario,new FileOutputStream(filename));
        } catch( JAXBException je ) {
            je.printStackTrace();
            return;
        } catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
        }
	}

	////////////////////////////////////////////////////////////////////////////
	// set input parameters
	////////////////////////////////////////////////////////////////////////////
	
	protected static void setconfigfilename(String name){
		if(!name.endsWith(".xml"))
			name += ".xml";
		Utils.configfilename = name;
	}
	
	protected static void setoutputfilename(String name){
		if(name.endsWith(".csv"))
			name = name.substring(0,name.length()-4);
		Utils.outputfile_density = name + "_density.txt";
		Utils.outputfile_outflow = name + "_outflow.txt";
		Utils.outputfile_inflow = name + "_inflow.txt";
	}

	////////////////////////////////////////////////////////////////////////////
	// private
	////////////////////////////////////////////////////////////////////////////
	
	// returns greatest common divisor among network time steps.
	// The time steps are rounded to the nearest decisecond.
	private static double computeCommonSimulationTimeInSeconds(){
		
		if(theScenario==null || theScenario.getNetworkList().getNetwork().size()==0)
			return Double.NaN;
			
		// loop through networks calling gcd
		double dt;
		List<Network> networkList = theScenario.getNetworkList().getNetwork();
		int tengcd = 0;		// in deciseconds
		for(int i=0;i<networkList.size();i++){
			dt = networkList.get(i).getDt().doubleValue();	// in seconds
	        if( lessthan(dt,0.1) ){
				System.out.println("Warning: Network dt given in hours. Changing to seconds.");
				dt *= 3600;
	        }
			tengcd = gcd( Utils.round(dt*10.0) , tengcd );
		}
    	return ((double)tengcd)/10.0;
	}
	
	/////////////////////////////////////////////////////////////////////
	// scenario interface
	/////////////////////////////////////////////////////////////////////

//	public static _Settings getSettings() {
//		return (_Settings) theScenario._settings;
//	}
	
	public static com.relteq.sirius.jaxb.Settings getSettings() {
		return theScenario.getSettings();
	}
	

	public static _ControllerSet getControllerSet(){
		return theScenario._controllerset;
	}
	
	public static _EventSet getEventSet(){
		return theScenario._eventset;
	}
	
	public static _Network getNetworkWithId(String id){
		if(id==null)
			return null;
		id.replaceAll("\\s","");
		for(Network network : theScenario.getNetworkList().getNetwork()){
			if(network.getId().equals(id))
				return (_Network) network;
		}
		return null;
	}

	// In lieu of id, we are using the name.
	public static _Controller getControllerWithId(String id){
		for(_Controller c : theScenario._controllerset.get_Controllers()){
			if(c.name.equals(id))
				return c;
		}
		return null;
	}
	
	public static _Node getNodeWithCompositeId(String network_id,String id){
		_Network network = getNetworkWithId(network_id);
		if(network==null)
			if(theScenario.getNetworkList().getNetwork().size()==1)
				return ((_Network) theScenario.getNetworkList().getNetwork().get(0)).getNodeWithId(id);
			else
				return null;
		else	
			return network.getNodeWithId(id);
	}

	public static _Link getLinkWithCompositeId(String network_id,String id){
		_Network network = getNetworkWithId(network_id);
		if(network==null)
			if(theScenario.getNetworkList().getNetwork().size()==1)
				return ((_Network) theScenario.getNetworkList().getNetwork().get(0)).getLinkWithId(id);
			else
				return null;
		else	
			return network.getLinkWithId(id);
	}
	
	public static _Sensor getSensorWithCompositeId(String network_id,String id){
		_Network network = getNetworkWithId(network_id);
		if(network==null)
			if(theScenario.getNetworkList().getNetwork().size()==1)
				return ((_Network) theScenario.getNetworkList().getNetwork().get(0)).getSensorWithId(id);
			else
				return null;
		else	
			return network.getSensorWithId(id);
	}
	
	public static int getVehicleTypeIndex(String name){
		List<VehicleType> vt = Utils.getSettings().getVehicleTypes().getVehicleType();
		for(int i=0;i<vt.size();i++){
			if(vt.get(i).getName().equals(name))
				return i;
		}
		return -1;
	}
	
	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////
	
	public static double getTimeMaxInSeconds() {
    	return timeend;
    }

    public static double getOutdtInSeconds() {
		return outdt;
	}

    public static double getTimeInitialInSeconds() {
    	return timestart;
    }

	public static double getSimDtInSeconds(){
		return Utils.simdtinseconds;
	}
	
	public static double getSimDtInHours(){
		return Utils.simdtinhours;
	}	

}
