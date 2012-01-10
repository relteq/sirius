package simulator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import jaxb.Link;

public class OutputWriter {

	private int outsteps;
	private Writer out_density = null;
	private Writer out_outflow = null;
	private Writer out_inflow = null;
	private static String delim = "\t";

	public OutputWriter(int osteps){
		outsteps = osteps;
	}

	public int getOutsteps() {
		return outsteps;
	}

	public boolean open(String density,String outflow,String inflow) throws FileNotFoundException {
		out_density = new OutputStreamWriter(new FileOutputStream(density));		
		out_outflow = new OutputStreamWriter(new FileOutputStream(outflow));		
		out_inflow = new OutputStreamWriter(new FileOutputStream(inflow));	
		return true;
	}

	public void recordstate(float time,boolean exportflows) {
		Float [] numbers;
		float invsteps = 1f/((float)outsteps);
		List<Link> links = Utils.theScenario.getNetwork().getLinkList().getLink();
		try {
			int n = links.size();
			_Link link;
			for(int i=0;i<n-1;i++){
				link = (_Link) links.get(i);
				out_density.write(format(link.getDensityInVeh(),":")+OutputWriter.delim);
				if(exportflows){
					numbers = Utils.times(link.getCumOutflowInVeh(),invsteps);
					out_outflow.write(format(numbers,":")+OutputWriter.delim);
					numbers = Utils.times(link.getCumInflowInVeh(),invsteps);
					out_inflow.write(format(numbers,":")+OutputWriter.delim);
				}
				link.reset_cumulative();
			}
			link = (_Link) links.get(n-1);
			out_density.write(format(link.getDensityInVeh(),":")+"\n");
			if(exportflows){
				numbers = Utils.times(link.getCumOutflowInVeh(),invsteps);
				out_outflow.write(format(link.getCumOutflowInVeh(),":")+"\n");
				numbers = Utils.times(link.getCumInflowInVeh(),invsteps);
				out_inflow.write(format(link.getCumInflowInVeh(),":")+"\n");
			}
			link.reset_cumulative();
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public void close(){
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

	private String format(Float [] V,String delim){
		String str="";
		if(V.length==0)
			return str;
		for(int i=0;i<V.length-1;i++)
			str += V[i] + delim;
		str += V[V.length-1];
		return str;
	}

}
