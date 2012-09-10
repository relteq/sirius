package com.relteq.sirius.simulator;

import java.util.ArrayList;

public final class SiriusErrorLog {
	
	private static boolean haserror;
	private static boolean haswarning;
	private static enum level {Warning,Error};
	private static ArrayList<SiriusError> error = new ArrayList<SiriusError>();

	public static void clearErrorMessage(){
		error.clear();
		haserror = false;
		haswarning = false;
	}
	
	public static boolean haserror(){
		return haserror;
	}
	
	public static boolean haswarning(){
		return haswarning;
	}
	
	public static boolean hasmessage(){
		return !error.isEmpty();
	}

	public static String format(){
		String str = "";
		int c;
		if(haserror){
			str += "----------------------------------------\n";
			str += "ERRORS\n";
			str += "----------------------------------------\n";
			c=0;
			for(int i=0;i<error.size();i++){
				SiriusError e = error.get(i);
				if(e.mylevel.compareTo(SiriusErrorLog.level.Error)==0)
					str += ++c + ") " + e.description +"\n";
			}
		}
		if(haswarning){
			str += "----------------------------------------\n";
			str += "WARNINGS\n";
			str += "----------------------------------------\n";
			c=0;
			for(int i=0;i<error.size();i++){
				SiriusError e = error.get(i);
				if(e.mylevel.compareTo(SiriusErrorLog.level.Warning)==0)
					str += ++c + ") " + e.description + "\n";
			}
			
		}
		if (haserror || haswarning)
			str += "----------------------------------------\n";
		return str;
	}
	
	public static void print(){
		System.out.println(SiriusErrorLog.format());
	}

	public static void addError(String str){
		error.add(new SiriusError(str,SiriusErrorLog.level.Error));
		haserror = true;
	}

	public static void addWarning(String str){
		error.add(new SiriusError(str,SiriusErrorLog.level.Warning));
		haswarning = true;
	}
	
	public static class SiriusError {
		String description;
		SiriusErrorLog.level mylevel;
		public SiriusError(String description,SiriusErrorLog.level mylevel){
			this.description = description;
			this.mylevel = mylevel;
		}
	}

}
