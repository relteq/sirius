package com.relteq.sirius.simulator;

import java.util.ArrayList;

public final class SiriusErrorLog {
	
	private static boolean haserror;
	private static boolean haswarning;
	private static enum level {Warning,Error};
	private static ArrayList<SiriusError> error = new ArrayList<SiriusError>();

	protected static void clearErrorMessage(){
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
		if(!error.isEmpty()){
			for(int i=0;i<error.size();i++){
				System.out.println(i+1 + ") " 
						+ error.get(i).mylevel.toString() + ": "
						+ error.get(i).description );
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
	
	public static class SiriusError{
		String description;
		SiriusErrorLog.level mylevel;
		public SiriusError(String description,SiriusErrorLog.level mylevel){
			this.description = description;
			this.mylevel = mylevel;
		}
	}

}
