package com.relteq.sirius.simulator;

import java.util.Random;

final class Global {
	
	protected static Clock clock;
	protected static _Scenario theScenario;
	protected static String schemafile = "data/schema/sirius.xsd";
	protected static Random random = new Random();

	protected static String configfilename;
	protected static int numRepititions;			// [#] number of repititions of the simulation
	protected static double outdt;				// [sec] output sampling time
	protected static double timestart;			// [sec] start of the simulation
	protected static double timeend;			// [sec] end of the simulation

	protected static OutputWriter outputwriter = null;
	protected static String outputfile_density;
	protected static String outputfile_outflow;
	protected static String outputfile_inflow;
	   
	/////////////////////////////////////////////////////////////////////
  	// protected interface 
  	/////////////////////////////////////////////////////////////////////

	protected static void setconfigfilename(String name){
		if(!name.endsWith(".xml"))
			name += ".xml";
		configfilename = name;
	}
	
	protected static void setoutputfilename(String name){
		if(name.endsWith(".csv"))
			name = name.substring(0,name.length()-4);
		outputfile_density = name + "_density.txt";
		outputfile_outflow = name + "_outflow.txt";
		outputfile_inflow = name + "_inflow.txt";
	}
	
}
