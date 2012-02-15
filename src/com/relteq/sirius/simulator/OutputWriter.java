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

	protected int outsteps;
	protected Writer out_density = null;
	protected Writer out_outflow = null;
	protected Writer out_inflow = null;
	protected static String delim = "\t";

	public OutputWriter(int osteps){
		outsteps = osteps;
	}

	protected int getOutsteps() {
		return outsteps;
	}

	protected boolean open(String density,String outflow,String inflow) throws FileNotFoundException {
		out_density = new OutputStreamWriter(new FileOutputStream(density));		
		out_outflow = new OutputStreamWriter(new FileOutputStream(outflow));		
		out_inflow = new OutputStreamWriter(new FileOutputStream(inflow));	
		return true;
	}

	protected void recordstate(double time,boolean exportflows) {
		Double [] numbers;
		double invsteps;
		
		if(Utils.clock.getCurrentstep()==1)
			invsteps = 1f;
		else
			invsteps = 1f/((double)outsteps);
			
		try {
			for(Network network : Utils.theScenario.getNetworkList().getNetwork()){
				List<Link> links = network.getLinkList().getLink();

				int n = links.size();
				_Link link;
				for(int i=0;i<n-1;i++){
					link = (_Link) links.get(i);
					numbers = Utils.times(link.getCumDensityInVeh(),invsteps);
					out_density.write(format(numbers,":")+OutputWriter.delim);
					if(exportflows){
						numbers = Utils.times(link.getCumOutflowInVeh(),invsteps);
						out_outflow.write(format(numbers,":")+OutputWriter.delim);
						numbers = Utils.times(link.getCumInflowInVeh(),invsteps);
						out_inflow.write(format(numbers,":")+OutputWriter.delim);
					}
					link.reset_cumulative();
				}
				
				link = (_Link) links.get(n-1);
				numbers = Utils.times(link.getCumDensityInVeh(),invsteps);
				out_density.write(format(numbers,":")+"\n");
				if(exportflows){
					numbers = Utils.times(link.getCumOutflowInVeh(),invsteps);
					out_outflow.write(format(numbers,":")+"\n");
					numbers = Utils.times(link.getCumInflowInVeh(),invsteps);
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
