package aurora.simulator;

public class RunnerFloat {

	public static void main(String[] args) {

		long time = System.currentTimeMillis();
		
		if(args.length>0)	
			Utils.setconfigfilename(args[0]);
		else
			Utils.setconfigfilename(Defaults.CONFIGFILE);
					
		if(args.length>1)	
			Utils.setoutputfilename(args[1]);
		else
			Utils.setoutputfilename(Defaults.OUTPUTFILE);
		
		// load configuration file ................................
		_Scenario scenario = Utils.load(Utils.configfilename);
		
		if(!scenario.isloadedandinitialized){
			System.out.println("Load failed.");
			return;
		}
		
		// validate scenario ......................................
		if(!scenario.validate()){
			System.out.println("Validation failed.");
			return;
		}
		
		// open output file .......................................
		int outsteps = Utils.round(scenario.getSettings().getOutdt()/Utils.simdt);
		Utils.outputwriter = new OutputWriter(outsteps);
				
		// reset and run scenario .................................
		scenario.reset();
		scenario.run();

		// save scenario ..........................................
//		String outfile = "C:\\Users\\gomes\\workspace\\auroralite\\data\\config\\out.xml";
//		Utils.save(scenario, outfile);
		
		System.out.println("done in " + (System.currentTimeMillis()-time));
		
	}

}
