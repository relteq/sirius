/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import com.relteq.sirius.jaxb.Link;
import com.relteq.sirius.jaxb.Network;

final class OutputWriter {

	protected _Scenario myScenario;
	protected Writer out_time = null;
	protected Writer out_density = null;
	protected Writer out_outflow = null;
	protected Writer out_inflow = null;
	protected static String delim = "\t";

	public OutputWriter(_Scenario myScenario){
		this.myScenario = myScenario;
		//this.outsteps = osteps;
	}

	protected boolean open(String prefix,String suffix) throws FileNotFoundException {
		suffix = "_"+suffix+".txt";
		out_time = new OutputStreamWriter(new FileOutputStream(prefix+"_time"+suffix));	
		out_density = new OutputStreamWriter(new FileOutputStream(prefix+"_density"+suffix));		
		out_outflow = new OutputStreamWriter(new FileOutputStream(prefix+"_outflow"+suffix));		
		out_inflow = new OutputStreamWriter(new FileOutputStream(prefix+"_inflow"+suffix));	
		return true;
	}

	protected void recordstate(double time,boolean exportflows,int outsteps) {
		
		if(myScenario==null)
			return;
		
		Double [] numbers;
		double invsteps;
		
		if(myScenario.clock.getCurrentstep()==1)
			invsteps = 1f;
		else
			invsteps = 1f/((double)outsteps);
			
		try {
			out_time.write(String.format("%f\n",time));
			for(Network network : myScenario.getNetworkList().getNetwork()){
				List<Link> links = network.getLinkList().getLink();

				int n = links.size();
				_Link link;
				for(int i=0;i<n-1;i++){
					link = (_Link) links.get(i);
					numbers = SiriusMath.times(link.cumulative_density,invsteps);
					out_density.write(format(numbers,":")+OutputWriter.delim);
					if(exportflows){
						numbers = SiriusMath.times(link.cumulative_outflow,invsteps);
						out_outflow.write(format(numbers,":")+OutputWriter.delim);
						numbers = SiriusMath.times(link.cumulative_inflow,invsteps);
						out_inflow.write(format(numbers,":")+OutputWriter.delim);
					}
					link.reset_cumulative();
				}
				
				link = (_Link) links.get(n-1);
				numbers = SiriusMath.times(link.cumulative_density,invsteps);
				out_density.write(format(numbers,":")+"\n");
				if(exportflows){
					numbers = SiriusMath.times(link.cumulative_outflow,invsteps);
					out_outflow.write(format(numbers,":")+"\n");
					numbers = SiriusMath.times(link.cumulative_inflow,invsteps);
					out_inflow.write(format(numbers,":")+"\n");
				}
				link.reset_cumulative();	
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	protected void close(){
		try {
			if(out_time!=null)
				out_time.close();
			if(out_density!=null)
				out_density.close();
			if(out_outflow!=null)
				out_outflow.close();
			if(out_inflow!=null)
				out_inflow.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected String format(Double [] V,String delim){
		String str="";
		if(V.length==0)
			return str;
		for(int i=0;i<V.length-1;i++)
			str += V[i] + delim;
		str += V[V.length-1];
		return str;
	}

}
