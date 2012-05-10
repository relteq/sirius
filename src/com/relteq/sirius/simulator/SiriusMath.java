package com.relteq.sirius.simulator;

import java.util.ArrayList;

public final class SiriusMath {
	
	private static final double EPSILON = (double) 1e-4;
	
 	public static Double [] zeros(int n1){
		Double [] answ = new Double [n1];
		for(int i=0;i<n1;i++)
			answ[i] = 0.0;
		return answ;	
	}
 	
 	public static Double [][] zeros(int n1,int n2){
		Double [][] answ = new Double [n1][n2];
		int i,j;
		for(i=0;i<n1;i++)
			for(j=0;j<n2;j++)
				answ[i][j] = 0.0;
		return answ;
	}
	
	public static Double sum(Double [] V){
		Double answ = 0d;
		for(int i=0;i<V.length;i++)
			if(V[i]!=null)
				answ += V[i];
		return answ;
	}
	
	public static Double [] sum(Double [][] V,int dim){
		if(V==null)
			return null;
		if(V.length==0)
			return null;
		if(V[0].length==0)
			return null;
		Double [] answ;
		int i,j;
		int n1 = V.length;
		int n2 = V[0].length;
		switch(dim){
		case 1:
			answ = new Double[n2];
			for(i=0;i<V.length;i++)
				for(j=0;j<V[i].length;j++)
					if(V[i][j]!=null)
						answ[j] += V[i][j];
			return answ;
		case 2:
			answ = new Double[n1];
			for(i=0;i<V.length;i++)
				for(j=0;j<V[i].length;j++)
					if(V[i][j]!=null)
						answ[i] += V[i][j];
			return answ;
		default:
			return null;
		}
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
		if(A==0)
			return true;
		if(a==0)
			return false;
		boolean result;
		result = SiriusMath.equals( SiriusMath.round(A/a) , A/a );
		result &=  A/a>0;
		return result;
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

	// deep copy a double array
	public static Double[][] makecopy(Double [][]x){
		if(x.length==0)
			return null;
		if(x[0].length==0)
			return null;
		int n1 = x.length;
		int n2 = x[0].length;
		Double [][] y = new Double[n1][n2];
		int i,j;
		for(i=0;i<n1;i++)
			for(j=0;j<n2;j++)
				y[i][j]=x[i][j];
		return y;
	}
}
