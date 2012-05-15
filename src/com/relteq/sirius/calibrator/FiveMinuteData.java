package com.relteq.sirius.calibrator;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

public class FiveMinuteData {

	public boolean isaggregate;	// true if object holds only averages over all lanes
	public int vds;
	public ArrayList<Long> time = new ArrayList<Long>();
	public ArrayList<ArrayList<Float>> flw = new ArrayList<ArrayList<Float>>();		// [veh/hr/lane]
	public ArrayList<ArrayList<Float>> spd = new ArrayList<ArrayList<Float>>();		// [mile/hr]
	
	public FiveMinuteData(int vds,boolean isaggregate) {
		this.vds=vds;
		this.isaggregate = isaggregate;
	}
	
	// methods for aggregated data ............................
	
	// add aggregate flow value in [veh/hr/lane]
	public void addAggFlw(float val){
		if(flw.isEmpty())
			flw.add(new ArrayList<Float>());
		if(isaggregate)
			flw.get(0).add(val);
	}
	
//	public void addAggOcc(float val){
//		if(occ.isEmpty())
//			occ.add(new ArrayList<Float>());
//		if(isaggregate)
//			occ.get(0).add(val);		
//	}
	
	// add aggregate speed value in [mile/hr]
	public void addAggSpd(float val){
		if(spd.isEmpty())
			spd.add(new ArrayList<Float>());
		if(isaggregate)
			spd.get(0).add(val);
	}	
	
	// get aggregate flow vlaue in [veh/hr/lane]
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

//	public float getAggOcc(int i){
//		try{
//			if(isaggregate)
//				return occ.get(0).get(i);
//			else
//				return Float.NaN;
//		}
//		catch(Exception e){
//			return Float.NaN;
//		}
//	}

	// get aggregate speed value in [mph]
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

	// get aggregate density value in [veh/mile/lane]
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
	
	public void writeAggregateToFile(String filename) throws Exception{
		Writer out = new OutputStreamWriter(new FileOutputStream(filename+"_"+vds+".txt"));
		for(int i=0;i<time.size();i++)
			out.write(time.get(i)+"\t"+getAggFlw(i)+"\t"+getAggDty(i)+"\t"+getAggSpd(i)+"\n");
		out.close();
	}

	// methods for per lane data .......................................
	
	// add array of per lane flow values in [veh/hr/lane]	
	public void addPerLaneFlw(ArrayList<Float> row,int start,int end){
		ArrayList<Float> x = new ArrayList<Float>();
		for(int i=start;i<end;i++)
			x.add(row.get(i));
		flw.add(x);
	}

	// add array of per lane occupancy values in [mph]
//	public void addPerLaneOcc(ArrayList<Float> row,int start,int end){
//		ArrayList<Float> x = new ArrayList<Float>();
//		for(int i=start;i<end;i++)
//			x.add(row.get(i));
//		occ.add(x);
//	}

	// add array of per lane speed values in [mph]
	public void addPerLaneSpd(ArrayList<Float> row,int start,int end){
		ArrayList<Float> x = new ArrayList<Float>();
		for(int i=start;i<end;i++)
			x.add(row.get(i));
		spd.add(x);
	}
}
