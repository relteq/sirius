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

	public static void print(){

		int c;
		if(haserror){
			System.out.println("----------------------------------------");
			System.out.println("ERRORS");
			System.out.println("----------------------------------------");
			c=0;
			for(int i=0;i<error.size();i++){
				SiriusError e = error.get(i);
				if(e.mylevel.compareTo(SiriusErrorLog.level.Error)==0)
					System.out.println(++c + ") " + e.description );
			}
		}
		if(haswarning){
			System.out.println("----------------------------------------");
			System.out.println("WARNINGS");
			System.out.println("----------------------------------------");
			c=0;
			for(int i=0;i<error.size();i++){
				SiriusError e = error.get(i);
				if(e.mylevel.compareTo(SiriusErrorLog.level.Warning)==0)
					System.out.println(++c + ") " + e.description );
			}
			
		}
		
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
