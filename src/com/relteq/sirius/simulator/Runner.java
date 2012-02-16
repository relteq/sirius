/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.io.FileNotFoundException;

final class Runner {

	public static void main(String[] args) {

		long time = System.currentTimeMillis();

		// process input parameters
		if(!parseInput(args)){
			SiriusError.printErrorMessage();
			return;
		}

		// load configuration file ................................
		_Scenario scenario = ObjectFactory.createAndLoadScenario(Global.configfilename);

		// did load succeed?
		if(!scenario.isloaded){
			SiriusError.setErrorHeader("Initialization failed.");
			SiriusError.printErrorMessage();
			return;
		}
			
		// validate scenario ......................................
		scenario.validate();
		if(!scenario.isvalid){
			SiriusError.setErrorHeader("Validation failed.");
			SiriusError.printErrorMessage();
			return;
		}
		
		// loop through simulation runs ............................
		for(int i=0;i<Global.numRepititions;i++){
			
			// reset scenario
			scenario.reset();
			if(!scenario.isreset){
				SiriusError.setErrorHeader("Reset failed.");
				SiriusError.printErrorMessage();
				return;
			}

			// open output files
	        if(scenario.simulationMode==_Scenario.ModeType.normal){
	        	Global.outputwriter = new OutputWriter(SiriusMath.round(Global.outdt/scenario.simdtinseconds));
				try {
					Global.outputwriter.open(Global.outputfile_density,Global.outputfile_outflow,Global.outputfile_inflow);
				} catch (FileNotFoundException e) {
					SiriusError.addErrorMessage("Unable to open output file.");
					SiriusError.printErrorMessage();
					return;
				}
	        }
	        	
			// run scenario		
			scenario.run();

            // close output files
	        if(scenario.simulationMode==_Scenario.ModeType.normal)
	        	Global.outputwriter.close();

			// or save scenario (in warmup mode)
	        if(scenario.simulationMode==_Scenario.ModeType.warmupFromIC || scenario.simulationMode==_Scenario.ModeType.warmupFromZero){
//	    		String outfile = "C:\\Users\\gomes\\workspace\\auroralite\\data\\config\\out.xml";
//	    		Utils.save(scenario, outfile);
	        }
	        
		}
		
		System.out.println("done in " + (System.currentTimeMillis()-time));
		
	}

	private static boolean parseInput(String[] args){

		if(args.length==0){
			String str;
			str = "Usage:" + "\n";
			str += "-----\n" + "\n";
			str += "args[0]: Configuration file name." + "\n";
			str += "args[1]: Output file name." + "\n";
			str += "         Default: <config file name>_output.csv." + "\n";
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
			SiriusError.addErrorMessage(str);
			return false;
		}
		
		// Configuration file name
		if(args.length>0)	
			Global.setconfigfilename(args[0]);
		else
			Global.setconfigfilename(Defaults.CONFIGFILE);	

		// Output file name		
		if(args.length>1)	
			Global.setoutputfilename(args[1]);
		else
			Global.setoutputfilename(Defaults.OUTPUTFILE);
		
		// Start time [seconds after midnight]
		if(args.length>2){
			double timestart = Double.parseDouble(args[2]);
			Global.timestart = SiriusMath.round(timestart*10.0)/10.0;	// round to the nearest decisecond
		}
		else
			Global.timestart = Defaults.TIME_INIT;

		// Duration [seconds]	
		if(args.length>3)
			Global.timeend = Global.timestart + Double.parseDouble(args[3]);
		else
			Global.timeend = Global.timestart + Defaults.DURATION;
		
		// Output sampling time [seconds]
		if(args.length>4){
			double outdt = Double.parseDouble(args[4]);
			Global.outdt = SiriusMath.round(outdt*10.0)/10.0;		// round to the nearest decisecond	
		}
		else
			Global.outdt = Defaults.OUT_DT;

		// Number of simulations
		if(args.length>5){
			Global.numRepititions = Integer.parseInt(args[5]);
		}
		else
			Global.numRepititions = 1;
		
		return true;
	}

}
