package com.relteq.sirius.simulator;

import java.util.ArrayList;

public class _test {

	public static void main(String[] args) {
		
		String configfilename; 
		String outputfileprefix = "out";;
		double timestart = 0d;; 			// [sec] after midnight
		double timeend = 3600d;			// [sec] after midnight
		double outdt = 300d;;			// [sec]
		
		try {

			//configfilename = "C:\\Gabriel\\traffic-estimation\\particle_filtering\\scenario\\scenario_2.xml";
			configfilename = "C:\\Users\\gomes\\workspace\\sirius\\data\\config\\_smalltest_multipletypes.xml";
			
			// load the scenario
			Scenario scenario = ObjectFactory.createAndLoadScenario(configfilename);
									
			// check if it loaded
			if(scenario==null)
				return;

			// make list of all links and origin links 
			ArrayList<String> link_origin = new ArrayList<String> ();
			ArrayList<String> link_ids = new ArrayList<String> ();
			for(com.relteq.sirius.jaxb.Link jlink : scenario.getNetworkList().getNetwork().get(0).getLinkList().getLink()){
				Link link = (Link) jlink;
				link_ids.add(jlink.getId());
				if(link.issource)
					link_origin.add(link.getId());
			}	
			
			int numLinks = link_ids.size();
			int numVehTypes = scenario.getNumVehicleTypes();
			
			// create demand profiles
			int numTimes = 10;
			ArrayList<DemandProfile> demands = new ArrayList<DemandProfile>();
			for(com.relteq.sirius.jaxb.Link jlink : scenario.getNetworkList().getNetwork().get(0).getLinkList().getLink()){
				Link link = (Link) jlink;
				if(link.issource){
					Double [][] demand = new Double [numTimes][numVehTypes];  // number of time intervals X number of vehicle types
					DemandProfile d = ObjectFactory.createDemandProfile(scenario,link.getId(),demand,0f,30f,1f,0f,0f);
					demands.add(d);
					scenario.addDemandProfile(d);
				}
			}	
			
			// create my initial condition object
			String [] a_link_ids = link_ids.toArray(new String[link_ids.size()]);
			Double [][] init_density = new Double[numLinks][numVehTypes];
			int i,j;
			for(i=0;i<init_density.length;i++)
				for(j=0;j<init_density[0].length;j++)
					init_density[i][j]=0d;
			
			InitialDensitySet myIC = ObjectFactory.createInitialDensitySet(scenario,0, a_link_ids, scenario.getVehicleTypeNames(), init_density);
			
			// attach it to the scenario
			scenario.setInitialDensitySet(myIC);
			
			// zero initial condition
			scenario.run(timestart,timeend,outdt,outputfileprefix);


			// 10 initial condition
			init_density = myIC.getInitial_density();
			for(i=0;i<numLinks;i++)
				for(j=0;j<numVehTypes;j++)
					init_density[i][j] = 10d;
			
			scenario.run(timestart,timeend,outdt,outputfileprefix);
			
			// still need modifiers for fd parameters and sink capacities
			
			// set demands to 10 vph
			for(DemandProfile d : demands)
				d.demand_nominal.set(0,0,10 * scenario.getSimDtInHours());
			scenario.run(timestart,timeend,outdt,outputfileprefix);

		} catch (SiriusException e) {
			if(SiriusErrorLog.haserror())
				SiriusErrorLog.print();
			else
				e.printStackTrace();
		}	
		
		System.out.println("done");
	}
		
}
