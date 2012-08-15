package com.relteq.sirius.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class SiriusFormatter {

	public static String csv(Double [] V,String delim){
		String str="";
		if(V.length==0)
			return str;
		for(int i=0;i<V.length-1;i++)
			str += V[i] + delim;
		str += V[V.length-1];
		return str;
	}

	public static String csv(Double [][] V,String delim1,String delim2){
		String str="";
		if(V.length==0)
			return str;
		for(int i=0;i<V.length-1;i++)
			str += SiriusFormatter.csv(V[i], delim1) + delim2;
		str += SiriusFormatter.csv(V[V.length-1], delim1);
		return str;
	}

	public static ArrayList<ArrayList<Double>> readCSV(String filename,String delim) throws IOException{
		
			ArrayList<ArrayList<Double>> data = new ArrayList<ArrayList<Double>>();
			BufferedReader in = new BufferedReader(new FileReader(filename));
		    String line;
		    double val;
		    while ((line = in.readLine()) != null){		    	
		    	ArrayList<Double> dataline = new ArrayList<Double>();
		    	String f[] = line.split(delim,-1);
		    	for(String ff : f){
		    		try {
						val = Double.parseDouble(ff);
					} catch (NumberFormatException e) {
						val = Double.NaN;
					}
		    		dataline.add(val);
		    	}
		    	data.add(dataline);
		    }
		    in.close();
		    return data;
	}
	
	public static ArrayList<ArrayList<ArrayList<Double>>> readCSV(String filename,String delim1,String delim2) throws IOException{
		
		ArrayList<ArrayList<ArrayList<Double>>> data = new ArrayList<ArrayList<ArrayList<Double>>>();
		BufferedReader in = new BufferedReader(new FileReader(filename));
	    String line;
	    double val;
	    while ((line = in.readLine()) != null){		    	
	    	ArrayList<ArrayList<Double>> dataline = new ArrayList<ArrayList<Double>>();
	    	String F[] = line.split(delim1,-1);
	    	for(String f : F){
	    		ArrayList<Double> datablock = new ArrayList<Double>();
	    		String G[] = f.split(delim2,-1);
		    	for(String g : G){
		    		try {
						val = Double.parseDouble(g);
					} catch (NumberFormatException e) {
						val = Double.NaN;
					}
			    	datablock.add(val);
		    	}
		    	dataline.add(datablock);
	    	}
	    	data.add(dataline);
	    }
	    in.close();
	    return data;
}

}
