/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.io.FileNotFoundException;

import com.relteq.sirius.jaxb.DemandProfile;

public class Runner {
	
	public static void main(String[] args) {

		long time = System.currentTimeMillis();
		
		// process input parameters
		if(!parseInput(args)){
			Utils.printErrorMessage();
			return;
		}

		// load configuration file ................................
		_Scenario scenario = Utils.load(Utils.configfilename);
		
		if(scenario==null){
			Utils.setErrorHeader("Load failed.");
			Utils.printErrorMessage();
			return;
		}
		
		if(!scenario.isloadedandinitialized){
			Utils.setErrorHeader("Initialization failed.");
			Utils.printErrorMessage();
			return;
		}
		
        // set simulation mode .....................................
        
        // normal <=> start time == initial profile time stamp
        double time_ic = ((_InitialDensityProfile)Utils.theScenario.getInitialDensityProfile()).getTimestamp();
		if(Utils.timestart==time_ic){
			Utils.simulationMode = Types.Mode.normal;
		}
		else{
			// it is a warmup. we need to decide on start and end times
			Utils.timeend = Utils.timestart;
			if(time_ic<Utils.timestart){	// go from ic to timestart
				Utils.timestart = time_ic;
				Utils.simulationMode = Types.Mode.warmupFromIC;
			}
			else{							// start at earliest demand profile
				Utils.timestart = Double.POSITIVE_INFINITY;
				for(DemandProfile D : Utils.theScenario.getDemandProfileSet().getDemandProfile())
					Utils.timestart = Math.min(Utils.timestart,D.getStartTime().doubleValue());
				Utils.simulationMode = Types.Mode.warmupFromZero;
			}		
		}
		
		// check timestart < timeend ..............................
		if(Utils.timestart>=Utils.timeend){
			Utils.setErrorHeader("Empty simulation period.");
			Utils.printErrorMessage();
			return;
		}

		// create the clock
        Utils.clock = new Clock(Utils.timestart,Utils.timeend,Utils.simdtinseconds);

		// reset scenario ..........................................
        // (reset before validate only so that the fundamental diagrams get populated
        // and therefore the initial density profiles can validate. try to get rid of this)
		scenario.reset();
		if(!scenario.isreset){
			Utils.setErrorHeader("Reset failed.");
			Utils.printErrorMessage();
			return;
		}
			
		// validate scenario ......................................
		scenario.validate();
		if(!scenario.isvalid){
			Utils.setErrorHeader("Validation failed.");
			Utils.printErrorMessage();
			return;
		}
		
		// loop through simulation runs ............................
		for(int i=0;i<Utils.numRepititions;i++){
			
			// reset 
			if(i>0)
				scenario.reset();
			
			// open output files
	        if(Utils.simulationMode==Types.Mode.normal){
	        	Utils.outputwriter = new OutputWriter(Utils.round(Utils.outdt/Utils.simdtinseconds));
				try {
					Utils.outputwriter.open(Utils.outputfile_density,Utils.outputfile_outflow,Utils.outputfile_inflow);
				} catch (FileNotFoundException e) {
					Utils.addErrorMessage("Unable to open output file.");
					Utils.printErrorMessage();
					return;
				}
	        }
	        	
			// run scenario		
			scenario.run();

            // close output files
	        if(Utils.simulationMode==Types.Mode.normal)
	        	Utils.outputwriter.close();

			// or save scenario (in warmup mode)
	        if(Utils.simulationMode==Types.Mode.warmupFromIC || Utils.simulationMode==Types.Mode.warmupFromZero){
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
			Utils.addErrorMessage(str);
			return false;
		}
		
		// Configuration file name
		if(args.length>0)	
			Utils.setconfigfilename(args[0]);
		else
			Utils.setconfigfilename(Defaults.CONFIGFILE);	

		// Output file name		
		if(args.length>1)	
			Utils.setoutputfilename(args[1]);
		else
			Utils.setoutputfilename(Defaults.OUTPUTFILE);
		
		// Start time [seconds after midnight]
		if(args.length>2){
			double timestart = Double.parseDouble(args[2]);
			Utils.timestart = Utils.round(timestart*10.0)/10.0;	// round to the nearest decisecond
		}
		else
			Utils.timestart = Defaults.TIME_INIT;

		// Duration [seconds]	
		if(args.length>3)
			Utils.timeend = Utils.timestart + Double.parseDouble(args[3]);
		else
			Utils.timeend = Utils.timestart + Defaults.DURATION;
		
		// Output sampling time [seconds]
		if(args.length>4){
			double outdt = Double.parseDouble(args[4]);
			Utils.outdt = Utils.round(outdt*10.0)/10.0;		// round to the nearest decisecond	
		}
		else
			Utils.outdt = Defaults.OUT_DT;

		// Number of simulations
		if(args.length>5){
			Utils.numRepititions = Integer.parseInt(args[5]);
		}
		else
			Utils.numRepititions = 1;
		
		return true;
	}

}
