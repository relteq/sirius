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
import java.util.Properties;

import com.relteq.sirius.simulator.Link;

public final class TextOutputWriter extends OutputWriterBase {
	protected Writer out_time = null;
	protected Writer out_density = null;
	protected Writer out_outflow = null;
	protected Writer out_inflow = null;
	protected static String delim = "\t";
	private String prefix;

	public TextOutputWriter(Scenario scenario, Properties props){
		super(scenario);
		if (null != props) prefix = props.getProperty("prefix");
		if (null == prefix) prefix = "output";
	}

	@Override
	public void open(int run_id) throws SiriusException {
		String suffix = String.format("_%d.txt", run_id);
		try {
			out_time = new OutputStreamWriter(new FileOutputStream(prefix+"_time"+suffix));
			out_density = new OutputStreamWriter(new FileOutputStream(prefix+"_density"+suffix));
			out_outflow = new OutputStreamWriter(new FileOutputStream(prefix+"_outflow"+suffix));
			out_inflow = new OutputStreamWriter(new FileOutputStream(prefix+"_inflow"+suffix));
		} catch (FileNotFoundException exc) {
			throw new SiriusException(exc.getMessage());
		}
	}

	@Override
	public void recordstate(double time, boolean exportflows, int outsteps) throws SiriusException {
		
		if(scenario==null)
			return;
		
		Double [] numbers;
		double invsteps;
		
		if(scenario.clock.getCurrentstep()==1)
			invsteps = 1f;
		else
			invsteps = 1f/((double)outsteps);
			
		try {
			out_time.write(String.format("%f\n",time));
			for(com.relteq.sirius.jaxb.Network network : scenario.getNetworkList().getNetwork()){
				List<com.relteq.sirius.jaxb.Link> links = network.getLinkList().getLink();

				int n = links.size();
				Link link;
				for(int i=0;i<n-1;i++){
					link = (Link) links.get(i);
					numbers = SiriusMath.times(link.cumulative_density[0],invsteps);
					out_density.write(format(numbers,":")+TextOutputWriter.delim);
					if(exportflows){
						numbers = SiriusMath.times(link.cumulative_outflow[0],invsteps);
						out_outflow.write(format(numbers,":")+TextOutputWriter.delim);
						numbers = SiriusMath.times(link.cumulative_inflow[0],invsteps);
						out_inflow.write(format(numbers,":")+TextOutputWriter.delim);
					}
					link.reset_cumulative();
				}
				
				link = (Link) links.get(n-1);
				numbers = SiriusMath.times(link.cumulative_density[0],invsteps);
				out_density.write(format(numbers,":")+"\n");
				if(exportflows){
					numbers = SiriusMath.times(link.cumulative_outflow[0],invsteps);
					out_outflow.write(format(numbers,":")+"\n");
					numbers = SiriusMath.times(link.cumulative_inflow[0],invsteps);
					out_inflow.write(format(numbers,":")+"\n");
				}
				link.reset_cumulative();	
			}
			
		} catch (IOException e) {
			throw new SiriusException(e.getMessage());
		}
	}

	public void close(){
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
