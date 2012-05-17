package com.relteq.sirius.sensor;

import java.util.ArrayList;

public class DataSource {
	private String urlname;
	private DataSource.Format format;
	private ArrayList<Integer> for_vds = new ArrayList<Integer>();

	public static enum Format { NULL, 
								PeMSDataClearinghouse,
								CaltransDBX,
								BHL };
	
	public DataSource(String urlname,String formatstr) {
		this.urlname = urlname;
		if(formatstr.compareTo("PeMS Data Clearinghouse")==0)
			format = DataSource.Format.PeMSDataClearinghouse;

		if(formatstr.compareTo("Caltrans DBX")==0)
			format = DataSource.Format.CaltransDBX;

		if(formatstr.compareTo("BHL")==0)
			format = DataSource.Format.BHL;		
	}

	public DataSource(com.relteq.sirius.sensor.DataSource d) {
		this.urlname = d.getUrl();
		this.format = d.getFormat();
	}
	
	public String getUrl() {
		return urlname;
	}

	public DataSource.Format getFormat() {
		return format;
	}
	
	public ArrayList<Integer> getFor_vds() {
		return for_vds;
	}
	
	public void add_to_for_vds(int vds){
		for_vds.add(vds);
	}
	
}
