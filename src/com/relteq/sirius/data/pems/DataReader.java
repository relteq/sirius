package com.relteq.sirius.data.pems;

import java.net.*;
import java.io.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

import com.relteq.sirius.sensor.DataSource;

/** Use the Read5minData() method of this class to read five-minute data from data sources.
* @author Gabriel Gomes
* @version VERSION NUMBER
*/
public class DataReader {

	private ColumnFormat PeMSDataClearingHouse = new ColumnFormat(",",5,8,9,10,8,false);
	private ColumnFormat CaltransDbx 		   = new ColumnFormat("\t",6,20,22,23,8,true);

	/////////////////////////////////////////////////////////////////////
	// public methods
	/////////////////////////////////////////////////////////////////////
	
    public void Read5minData(HashMap <Integer,FiveMinuteData> data,ArrayList<DataSource> datasources) throws Exception {

    	// step through data file
    	int count = 0;
    	for(DataSource datasource : datasources){
    		DataSource.Format dataformat = datasource.getFormat();
    		count++;
    		switch(dataformat){
    		case PeMSDataClearinghouse:
    			ReadDataSource(data,datasource,PeMSDataClearingHouse);
    			break;
    		case CaltransDBX:
    			ReadDataSource(data,datasource,CaltransDbx);
    			break;
//    		case BHL:
//    			ReadDataSource(data,datasource,BHL);
//    			break;
    		}
    	}
    	 
    }
	       
	/////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////
    
    private static Date ConvertTime(final String timestr) {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        ParsePosition pp = new ParsePosition(0);
        return formatter.parse(timestr,pp);
    }

    private static void ReadDataSource(HashMap <Integer,FiveMinuteData> data,DataSource datasource, ColumnFormat format) throws NumberFormatException, IOException{
		int lane;
    	String line,str;
    	int indexof;
        Calendar calendar = Calendar.getInstance();
    	float totalflw,totalspd;
    	float val;
    	long time;
    	int actuallanes;
    	boolean hasflw,hasspd; //,hasocc;
    	    	
    	URL url = new URL(datasource.getUrl());
		URLConnection uc = url.openConnection();
		BufferedReader fin = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		if(format.hasheader)
			line=fin.readLine(); 	// discard the header
    	while ((line=fin.readLine()) != null) {
            String f[] = line.split(format.delimiter,-1);
            int vds = Integer.parseInt(f[1]);
            indexof = datasource.getFor_vds().indexOf(vds);
       
            if(indexof<0)
            	continue;
            
    		calendar.setTime(ConvertTime(f[0]));
    		time = calendar.getTime().getTime()/1000;
    		    
        	ArrayList<Float> laneflw = new ArrayList<Float>();
//        	ArrayList<Float> laneocc = new ArrayList<Float>();
        	ArrayList<Float> lanespd = new ArrayList<Float>();
        
        	// store in lane-wise ArrayList
        	actuallanes = 0;
            totalflw = 0;
            totalspd = 0;
            int index;
            for (lane=0;lane<format.maxlanes;lane++) {
            	
            	index = format.laneblocksize*(lane+1)+format.flwoffset;
            	str = f[index];
            	hasflw = !str.isEmpty();
            	if(hasflw){
            		val = Float.parseFloat(str)*12f;
            		laneflw.add(val);
            		totalflw += val;
            	}
            	else
                	laneflw.add(Float.NaN); 
            	
//            	index = format.laneblocksize*(lane+1)+format.occoffset;
//            	str = f[index];
//            	hasocc = !str.isEmpty();
//            	if(hasocc)
//            		laneocc.add(Float.parseFloat(str));
//            	else
//            		laneocc.add(Float.NaN); 
            	
            	index = format.laneblocksize*(lane+1)+format.spdoffset;
            	str = f[index];
            	hasspd = !str.isEmpty();
            	if(hasspd){
            		val = Float.parseFloat(str);
            		lanespd.add(val);
            		totalspd += val;
            	}
            	else
            		lanespd.add(Float.NaN); 
            	if(hasflw || hasspd) // || hasocc
            		actuallanes++;
            }

            // find the data structure and store. 
            FiveMinuteData D = data.get(vds);
            if(D.isaggregate && actuallanes>0){
                totalspd /= actuallanes;
                totalflw /= actuallanes;
                D.addAggFlw(totalflw);
//                D.addAggOcc(totalflw/totalspd);
                D.addAggSpd(totalspd);
                D.time.add(time);	
            }
            else{
	            D.addPerLaneFlw(laneflw,0,actuallanes);
//	            D.addPerLaneOcc(laneocc,0,actuallanes);
	            D.addPerLaneSpd(lanespd,0,actuallanes);
	            D.time.add(time);
            }
        }    	
        fin.close();
    }
    
    private class ColumnFormat {
    	public int laneblocksize;
    	public int flwoffset;
    	//public int occoffset;
    	public int spdoffset;
    	public int maxlanes;
    	public boolean hasheader;
    	public String delimiter;

    	public ColumnFormat(String delimiter,int laneblocksize, int flwoffset,int occoffset,int spdoffset, int maxlanes,boolean hasheader) {
    		super();
    		this.delimiter = delimiter;	
    		this.laneblocksize = laneblocksize;
    		this.flwoffset = flwoffset;
    		//this.occoffset = occoffset;
    		this.spdoffset = spdoffset;
    		this.maxlanes = maxlanes;
    		this.hasheader = hasheader;
    	}
    }

}