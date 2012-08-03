package com.relteq.sirius.simulator;

public class _test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String configfilename = "C:\\Users\\gomes\\workspace\\sirius\\data\\config\\_scenario_constantsplits.xml";
		
		// load configuration file ................................
		Scenario scenario = ObjectFactory.createAndLoadScenario(configfilename);

		// check if it loaded
		if(scenario==null)
			return;
		
		try {
			scenario.loadSensorData();
		} catch (SiriusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
