package com.relteq.sirius.data;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/** Class for holding five minute flow and speed data associated with a VDS.
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public class FiveMinuteData {

	protected boolean isaggregate;	// true if object holds only averages over all lanes
	protected int vds;
	protected ArrayList<Long> time = new ArrayList<Long>();
	protected ArrayList<ArrayList<Float>> flw = new ArrayList<ArrayList<Float>>();		// [veh/hr/lane]
	protected ArrayList<ArrayList<Float>> spd = new ArrayList<ArrayList<Float>>();		// [mile/hr]

	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////
	
	public FiveMinuteData(int vds,boolean isaggregate) {
		this.vds=vds;
		this.isaggregate = isaggregate;
	}
	
	/////////////////////////////////////////////////////////////////////
	// getters
	/////////////////////////////////////////////////////////////////////
	
	public int getNumDataPoints(){
		return time.size();
	}

	/** get aggregate flow vlaue in [veh/hr/lane]
	 * @param time index
	 * @return a float, or <code>NaN</code> if something goes wrong.
	 * */
	public float getAggFlw(int i){
		try{
			if(isaggregate)
				return flw.get(0).get(i);
			else
				return Float.NaN;
		}
		catch(Exception e){
			return Float.NaN;
		}
	}

	/** get aggregate speed value in [mph]
	 * @param time index
	 * @return a float, or <code>NaN</code> if something goes wrong.
	 * */
	public float getAggSpd(int i){
		try{
			if(isaggregate)
				return spd.get(0).get(i);
			else
				return Float.NaN;
		}
		catch(Exception e){
			return Float.NaN;
		}
	}	

	/** get aggregate density value in [veh/mile/lane]
	 * @param time index
	 * @return a float, or <code>NaN</code> if something goes wrong.
	 * */
	public float getAggDty(int i){
		try{
			if(isaggregate)
				return flw.get(0).get(i)/spd.get(0).get(i);
			else
				return Float.NaN;
		}
		catch(Exception e){
			return Float.NaN;
		}
	}

	/////////////////////////////////////////////////////////////////////
	// putters
	/////////////////////////////////////////////////////////////////////
	
	/** add aggregate flow value in [veh/hr/lane]
	 * @param value of flow
	 * */
	protected void addAggFlw(float val){
		if(flw.isEmpty())
			flw.add(new ArrayList<Float>());
		if(isaggregate)
			flw.get(0).add(val);
	}
	
	/** add aggregate speed value in [mile/hr]
	 * @param value of speed
	 * */
	protected void addAggSpd(float val){
		if(spd.isEmpty())
			spd.add(new ArrayList<Float>());
		if(isaggregate)
			spd.get(0).add(val);
	}	
	
	/** add array of per lane flow values in [veh/hr/lane]	
	 * @param array of flow values.
	 * @param index to begining of sub-array.
	 * @param index to end of sub-array.
	 * */
	protected void addPerLaneFlw(ArrayList<Float> row,int start,int end){
		ArrayList<Float> x = new ArrayList<Float>();
		for(int i=start;i<end;i++)
			x.add(row.get(i));
		flw.add(x);
	}

	/** add array of per lane speed values in [mph]
	 * @param array of speed values.
	 * @param index to begining of sub-array.
	 * @param index to end of sub-array.
	 * */
	protected void addPerLaneSpd(ArrayList<Float> row,int start,int end){
		ArrayList<Float> x = new ArrayList<Float>();
		for(int i=start;i<end;i++)
			x.add(row.get(i));
		spd.add(x);
	}
	
	/////////////////////////////////////////////////////////////////////
	// file I/O
	/////////////////////////////////////////////////////////////////////
	
	/** Write aggregate values to a text file.
	 * @param File name.
	 * */
	public void writeAggregateToFile(String filename) throws Exception{
		Writer out = new OutputStreamWriter(new FileOutputStream(filename+"_"+vds+".txt"));
		for(int i=0;i<time.size();i++)
			out.write(time.get(i)+"\t"+getAggFlw(i)+"\t"+getAggDty(i)+"\t"+getAggSpd(i)+"\n");
		out.close();
	}

}
