/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.Density;

final class _InitialDensityProfile extends com.relteq.sirius.jaxb.InitialDensityProfile {

	private Double [][] initial_density; 	// [veh/mile] indexed by link and type
	private _Link [] link;					// ordered array of references
	private Integer [] vehicletypeindex; 	// index of vehicle types into global list
	private double timestamp;

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	protected void populate(){
		
		int i;
		
		// allocate
		int numLinks = getDensity().size();
		initial_density = new Double [numLinks][];
		link = new _Link [numLinks];
		
		int numTypes;
		
		// use <VehicleTypesOrder> if it is there, otherwise assume order given in <settings>
		if(getVehicleTypeOrder()!=null){
			numTypes = getVehicleTypeOrder().getVehicleType().size();
			vehicletypeindex = new Integer[numTypes];
			for(i=0;i<numTypes;i++)
				vehicletypeindex[i] = Utils.getVehicleTypeIndex(getVehicleTypeOrder().getVehicleType().get(i).getName());
		}
		else{
			numTypes = Utils.numVehicleTypes;
			vehicletypeindex = new Integer[numTypes];
			for(i=0;i<numTypes;i++)
				vehicletypeindex[i] = i;
		}

		// copy profile information to arrays in extended object
		for(i=0;i<numLinks;i++){
			Density density = getDensity().get(i);
			link[i] = Utils.getLinkWithCompositeId(density.getNetworkId(),density.getLinkId());
			Double1DVector D = new Double1DVector(density.getContent(),":");
			initial_density[i] = D.getData();
		}
		
		// round to the nearest decisecond
		if(getTstamp()!=null)
			timestamp = Utils.round(getTstamp().doubleValue()*10.0)/10.0;
		else
			timestamp = 0.0;
		
	}

	protected boolean validate() {
		
		int i;
		
		// check that all vehicle types are accounted for
		if(vehicletypeindex.length!=Utils.numVehicleTypes){
			System.out.println("Demand profile list of vehicle types does not match that of settings.");
			return false;
		}
		
		// check that vehicle types are valid
		for(i=0;i<vehicletypeindex.length;i++){
			if(vehicletypeindex[i]<0){
				System.out.println("Bad vehicle type name.");
				return false;
			}
		}
		
		// check that links are valid
		for(i=0;i<link.length;i++){
			if(link[i]==null){
				System.out.println("Bad link id");
				return false;
			}
		}
		
		// check size of data
		for(i=0;i<link.length;i++){
			if(initial_density[i].length!=vehicletypeindex.length){
				System.out.println("Wrong number of data points.");
				return false;
			}
		}

		// check that values are between 0 and jam density
		int j;
		Double sum;
		Double x;
		for(i=0;i<initial_density.length;i++){
			sum = 0.0;
			for(j=0;j<vehicletypeindex.length;j++){
				x = initial_density[i][j];
				if(x<0 || x.isNaN()){
					System.out.println("Invalid initial density.");
					return false;
				}
				sum += x;
			}
			if(sum>link[i].getDensityJamInVPMPL()){
				System.out.println("Initial density exceeds jam density.");
				return false;
			}
		}
		
		return true;
	}

	protected void reset() {
	}

	protected void update() {
	}

	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////
	
	public Double [] getDensityForLinkIdInVeh(String linkid){
		Double [] d = Utils.zeros(Utils.numVehicleTypes);
		for(int i=0;i<link.length;i++){
			if(link[i].getId().equals(linkid)){
				for(int j=0;j<vehicletypeindex.length;j++)
					d[vehicletypeindex[j]] = initial_density[i][j]*link[i].getLengthInMiles();
				return d;
			}
		}
		return d;
	}

	public double getTimestamp() {
		return timestamp;
	}
	
}
