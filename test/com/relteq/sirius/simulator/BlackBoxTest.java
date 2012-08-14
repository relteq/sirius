package com.relteq.sirius.simulator;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BlackBoxTest {

	private static String CONF_SUFFIX = ".xml";
	private static String [] outfile = {"_density_0.txt" , 
								  	    "_inflow_0.txt" , 
									    "_outflow_0.txt" , 
									    "_time_0.txt"};
	
	private String fixture_folder = "C:\\Users\\gomes\\workspace\\sirius\\data\\test\\fixture\\";
	private String output_folder = "C:\\Users\\gomes\\workspace\\sirius\\data\\test\\output\\";
	private String config_folder = "C:\\Users\\gomes\\workspace\\sirius\\data\\config\\";
	private String configfilename = "_scenario_2009_02_12";
	
//	@BeforeClass
//	public static void beforeClass() {
//		System.out.println("Before Class");
//	}
//
//	@Before
//	public void beforeMethod() {
//		System.out.println("Before Method");
//	}
	
	@Test
	public void testClassTwo() {
		
		String [] args = {config_folder+configfilename+CONF_SUFFIX, 
				output_folder+configfilename,
				String.format("%d", 0), 
				String.format("%d", 3600), 
				String.format("%d", 300),
				String.format("%d", 1) };
		
		com.relteq.sirius.simulator.Runner.main(args);

		for(String str : outfile){
			String filename = configfilename + str;
			File file1 = new File(fixture_folder+filename);
			File file2 = new File(output_folder+filename);
			try {
				assertTrue("The files differ!", FileUtils.contentEquals(file1, file2));
			} catch (IOException e) {
				fail("problem reading a file");
			}
		}
		
	}

//	@After
//	public void afterMethod() {
//		System.out.println("After method");
//	}
//	
//	@AfterClass
//	public static void afterClass() {
//		System.out.println("After class");
//	}

}
