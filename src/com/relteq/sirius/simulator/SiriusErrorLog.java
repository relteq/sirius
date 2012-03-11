package com.relteq.sirius.simulator;

import java.util.ArrayList;

public final class SiriusErrorLog {
	
	private static String errorheader = new String();
	private static ArrayList<String> errormessage = new ArrayList<String>();

	public static void clearErrorMessage(){
		errormessage.clear();
	}
	
	public static boolean haserror(){
		return !errormessage.isEmpty();
	}

	public static void printErrorMessage(){
		if(!errorheader.isEmpty())
			System.out.println(errorheader);
		if(!errormessage.isEmpty()){
			if(errormessage.size()==1)
				System.out.println(errormessage.get(0));
			else
				for(int i=0;i<errormessage.size();i++)
					System.out.println(i+1 + ") " + errormessage.get(i));
		}
	}

	public static void addErrorMessage(String str){
		errormessage.add(str);
	}

	public static void setErrorHeader(String str){
		errorheader = str;
	}
	
}
