package com.relteq.sirius.simulator;

import java.util.ArrayList;

public final class SiriusMath {
	
	private static final double EPSILON = (double) 1e-4;
	
 	public static Double [] zeros(int n){
		Double [] answ = new Double [n];
		for(int i=0;i<n;i++)
			answ[i] = 0.0;
		return answ;	
	}
	
	public static Double sum(Double [] V){
		Double answ = 0d;
		for(int i=0;i<V.length;i++)
			answ += V[i];
		return answ;
	}
	
	public static Double [] times(Double [] V,double a){
		Double [] answ = new Double [V.length];
		for(int i=0;i<V.length;i++)
			answ[i] = a*V[i];
		return answ;
	}
	
	public static int ceil(double a){
		return (int) Math.ceil(a-SiriusMath.EPSILON);
	}
	
	public static int floor(double a){
		return (int) Math.floor(a+SiriusMath.EPSILON);
	}
	
	public static int round(double a){
		return (int) Math.round(a);
	}
	
	public static boolean any (boolean [] x){
		for(int i=0;i<x.length;i++)
			if(x[i])
				return true;
		return false;
	}
	
	public static boolean all (boolean [] x){
		for(int i=0;i<x.length;i++)
			if(!x[i])
				return false;
		return true;
	}
	
	public static boolean[] not(boolean [] x){
		boolean [] y = x.clone();
		for(int i=0;i<y.length;i++)
			y[i] = !y[i];
		return y;
	}
	
	public static int count(boolean [] x){
		int s = 0;
		for(int i=0;i<x.length;i++)
			if(x[i])
				s++;
		return s;
	}
	
	public static ArrayList<Integer> find(boolean [] x){
		ArrayList<Integer> r = new ArrayList<Integer>();
		for(int i=0;i<x.length;i++)
			if(x[i])
				r.add(i);
		return r;
	}
	
	public static boolean isintegermultipleof(Double A,Double a){
		if(A.isInfinite())
			return true;
		return SiriusMath.equals( SiriusMath.round(A/a) , A/a );
	}
	
	public static boolean equals(double a,double b){
		return Math.abs(a-b) < SiriusMath.EPSILON;
	}
	
	public static boolean greaterthan(double a,double b){
		return a > b + SiriusMath.EPSILON;
	}

	public static boolean greaterorequalthan(double a,double b){
		return !lessthan(a,b);
	}
	
	public static boolean lessthan(double a,double b){
		return a < b - SiriusMath.EPSILON;
	}

	public static boolean lessorequalthan(double a,double b){
		return !greaterthan(a,b);
	}
	
	// greatest common divisor of two integers
	public static int gcd(int p, int q) {
		if (q == 0) {
			return p;
		}
		return gcd(q, p % q);
	}

}
