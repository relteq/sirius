/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.Properties;

public final class Runner {
	
	private static Scenario scenario;

	private static String outputtype = "xml";
	private static String configfilename;
	private static String outputfileprefix;
	private static double timestart;
	private static double timeend;
	private static double outdt;
	private static int numRepetitions;

	public static void main(String[] args) {

		long time = System.currentTimeMillis();

		// process input parameters
		if(!parseInput(args)){
			SiriusErrorLog.print();
			return;
		}

		// load configuration file ................................
		scenario = ObjectFactory.createAndLoadScenario(configfilename);

		// check if it loaded
		if(scenario==null)
			return;

		try {
			Properties owr_props = new Properties();
			if (null != outputfileprefix) owr_props.setProperty("prefix", outputfileprefix);
			owr_props.setProperty("type", outputtype);
			scenario.run(timestart,timeend,outdt,numRepetitions,owr_props);
			System.out.println("done in " + (System.currentTimeMillis()-time));
		} catch (SiriusException e) {
			if(SiriusErrorLog.haserror())
				SiriusErrorLog.print();
			else
				e.printStackTrace();
		}	
		
	}

	public static void debug(String [] args) {
		outputtype = "text";
		main(args);
	}

	private static boolean parseInput(String[] args){

		if(args.length<1){
			String str;
			str = "Usage:" + "\n";
			str += "-----\n" + "\n";
			str += "args[0]: Configuration file name. (required)\n";
			str += "args[1]: Output file name.\n";
			str += "args[2]: Start time [seconds after midnight]." + "\n";
			str += "         Defailt: Minimum start time of all demand profiles." + "\n";
			str += "args[3]: Duration [seconds]." + "\n";
			str += "         Defailt: 86,400 seconds." + "\n";
			str += "args[4]: Output sampling time [seconds]." + "\n";
			str += "         Default: 300 seconds." + "\n";
			str += "args[5]: Number of simulations." + "\n";
			str += "         Default: 1." + "\n";
			str += "\nSimulation modes:" + "\n";
			str += "----------------\n" + "\n";
			str += "Normal mode: Simulation runs in normal mode when the start time equals " +
					"the time stamp of the initial density profile. In this mode, the initial density state" +
					" is taken from the initial density profile, and the simulated state is written to the output file.\n" + "\n";
			str += "Warmup mode: Warmup is executed whenever the start time (st) does not equal the time stamp " +
					"of the initial density profile (tsidp). The purpose of a warmup simulation is to compute the state of the scenario " +
					"at st. If st<tsidp, then the warmup run will start with zero density at the earliest times stamp of all " +
					"demand profiles and run to st. If st>tsidn, then the warmup will start at tsidn with the given initial " +
					"density profile and run to st. The simulation state is not written in warmup mode. The output is a configuration " +
					"file with the state at st contained in the initial density profile." + "\n";
			SiriusErrorLog.addError(str);
			return false;
		}
		
		// Configuration file name	
		configfilename = args[0];

		// Output file name
		if(args.length>1)
			outputfileprefix = args[1];	
		else
			outputfileprefix = "output";
			
		// Start time [seconds after midnight]
		if(args.length>2){
			timestart = Double.parseDouble(args[2]);
			timestart = SiriusMath.round(timestart*10.0)/10.0;	// round to the nearest decisecond
		}
		else
			timestart = Defaults.TIME_INIT;

		// Duration [seconds]	
		if(args.length>3)
			timeend = timestart + Double.parseDouble(args[3]);
		else
			timeend = timestart + Defaults.DURATION;
		
		// Output sampling time [seconds]
		if(args.length>4){
			outdt = Double.parseDouble(args[4]);
			outdt = SiriusMath.round(outdt*10.0)/10.0;		// round to the nearest decisecond	
		}
		else
			outdt = Defaults.OUT_DT;

		// Number of simulations
		if(args.length>5){
			numRepetitions = Integer.parseInt(args[5]);
		}
		else
			numRepetitions = 1;

		return true;
	}

	public static void run_db(String [] args) throws SiriusException, com.relteq.sirius.Runner.InvalidUsageException {
		if (0 == args.length) {
			final String eol = System.getProperty("line.separator");
			throw new com.relteq.sirius.Runner.InvalidUsageException(
					"Usage: simulate|s scenario_id [parameters]" + eol +
					"Parameters:" + eol +
					"\tstart time, sec" + eol +
					"\tduration, sec" + eol +
					"\toutput sampling time, sec" + eol +
					"\tnumber of simulations");
		} else {
			String [] auxargs = new String[args.length + 1];
			auxargs[0] = auxargs[1] = null;
			System.arraycopy(args, 1, auxargs, 2, args.length - 1);
			parseInput(auxargs);
		}

		com.relteq.sirius.db.Service.init();

		scenario = com.relteq.sirius.db.exporter.ScenarioRestorer.getScenario(args[0]);
		
		if (SiriusErrorLog.haserror()) {
			SiriusErrorLog.print();
			return;
		}

		Properties owr_props = new Properties();
		owr_props.setProperty("type", "db");
		scenario.run(timestart, timeend, outdt, numRepetitions, owr_props);

		com.relteq.sirius.db.Service.shutdown();
	}

}
