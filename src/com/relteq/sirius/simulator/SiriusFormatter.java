package com.relteq.sirius.simulator;

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
}
