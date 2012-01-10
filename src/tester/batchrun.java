package tester;

import java.io.IOException;

public class batchrun {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		String testfolder = "C:\\Users\\gomes\\workspace\\auroralite\\data\\test\\";
		String iofolder = "C:\\Users\\gomes\\workspace\\auroralite\\data\\config\\";
		String configfile = iofolder + "scenario_2009_02_12.xml";
		String outputfile_lite = iofolder + "out_lite.csv";
		String outputfile_2 = iofolder + "out_2.csv";
		long time;
		
        String[] command =  new String[3];
        command[0] = "cmd";
        command[1] = "/C";


        // test aurora2.jar
        try {
        	command[2] = "java -jar " + testfolder + "auroralite.jar " + configfile + " " + outputfile_2;
            time = System.currentTimeMillis();
            Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
	        System.out.println("aurora2 done in " + (System.currentTimeMillis()-time)/1000f);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

        // test auroralite.jar
        try {
        	command[2] = "java -jar " + testfolder + "auroralite.jar " + configfile + " " + outputfile_lite;
            time = System.currentTimeMillis();
            Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
	        System.out.println("auroralite done in " + (System.currentTimeMillis()-time)/1000f);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		

	}

}
