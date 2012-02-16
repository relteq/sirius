package com.relteq.sirius.simulator;

import java.util.ArrayList;

final class SiriusError {
	
	private static String errorheader = new String();
	private static ArrayList<String> errormessage = new ArrayList<String>();
	
	protected static void clearErrorMessage(){
		errormessage.clear();
	}

	protected static void printErrorMessage(){
		if(!errorheader.isEmpty())
			System.out.println("Error: " + errorheader);
		if(!errormessage.isEmpty()){
			if(errormessage.size()==1)
				System.out.println(errormessage.get(0));
			else
				for(int i=0;i<errormessage.size();i++)
					System.out.println(i+1 + ") " + errormessage.get(i));
		}
	}

	protected static void addErrorMessage(String str){
		errormessage.add(str);
	}

	protected static void setErrorHeader(String str){
		errorheader = str;
	}
	
}
