package com.relteq.sirius.simulator;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

public class BlackBoxTest {
	
	private static String [] config_names = {
											 "_scenario_2009_02_12",
											 "Albany & Berkeley_sirius",
										 	 "_smalltest_multipletypes",
											 "complete",
											 "test_event",
		                                     "_scenario_constantsplits" };
		
	private static String CONF_SUFFIX = ".xml";
	private static String [] outfile = {"_density_0.txt" , 
								  	    "_inflow_0.txt" , 
									    "_outflow_0.txt" , 
									    "_time_0.txt"};
	
	private String fixture_folder = "C:\\Users\\gomes\\workspace\\sirius\\data\\test\\fixture\\";
	private String output_folder = "C:\\Users\\gomes\\workspace\\sirius\\data\\test\\output\\";
	private String config_folder = "C:\\Users\\gomes\\workspace\\sirius\\data\\config\\";
	
	@Test
	public void testSimulator() {
		
		for(String config_name : config_names ){

			String [] args = {config_folder+config_name+CONF_SUFFIX, 
					output_folder+config_name,
					String.format("%d", 0), 
					String.format("%d", 3600), 
					String.format("%d", 300),
					String.format("%d", 1) };
			
			System.out.println("Running " + config_name);
			com.relteq.sirius.simulator.Runner.main(args);

			for(String str : outfile){
				String filename = config_name + str;
				try {
					System.out.println("Checking " + filename);
					System.out.println("Reading " + fixture_folder+filename);
					ArrayList<ArrayList<ArrayList<Double>>> A = SiriusFormatter.readCSV(fixture_folder+filename,"\t",":");
					System.out.println("Reading " + output_folder+filename);
					ArrayList<ArrayList<ArrayList<Double>>> B = SiriusFormatter.readCSV(output_folder+filename,"\t",":");
					assertTrue("The files are not equal.",SiriusMath.equals3D(A,B));
				} catch (IOException e) {
					System.out.println(e.getMessage());
					fail("problem reading a file");
				}
			}
		}
	}

}
