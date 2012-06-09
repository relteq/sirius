/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;
import java.util.List;

public final class InitialDensityProfile extends com.relteq.sirius.jaxb.InitialDensityProfile {

	protected Scenario myScenario;
	protected Double [][] initial_density; 	// [veh/mile] indexed by link and type
	protected Link [] link;				// ordered array of references
	protected Integer [] vehicletypeindex; 	// index of vehicle types into global list
	protected double timestamp;

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	protected void populate(Scenario myScenario){
		
		int i;
		
		this.myScenario = myScenario;
		
		// allocate
		int numLinks = getDensity().size();
		initial_density = new Double [numLinks][];
		link = new Link [numLinks];
		vehicletypeindex = myScenario.getVehicleTypeIndices(getVehicleTypeOrder());

		// copy profile information to arrays in extended object
		for(i=0;i<numLinks;i++){
			com.relteq.sirius.jaxb.Density density = getDensity().get(i);
			link[i] = myScenario.getLinkWithCompositeId(density.getNetworkId(),density.getLinkId());
			Double1DVector D = new Double1DVector(density.getContent(),":");
			initial_density[i] = D.getData();
		}
		
		// round to the nearest decisecond
		if(getTstamp()!=null)
			timestamp = SiriusMath.round(getTstamp().doubleValue()*10.0)/10.0;
		else
			timestamp = 0.0;
		
	}

	protected boolean validate() {
		
		int i;
		
		// check that all vehicle types are accounted for
		if(vehicletypeindex.length!=myScenario.getNumVehicleTypes()){
			SiriusErrorLog.addErrorMessage("Demand profile list of vehicle types does not match that of settings.");
			return false;
		}
		
		// check that vehicle types are valid
		for(i=0;i<vehicletypeindex.length;i++){
			if(vehicletypeindex[i]<0){
				SiriusErrorLog.addErrorMessage("Bad vehicle type name.");
				return false;
			}
		}
		
		// check that links are valid
		for(i=0;i<link.length;i++){
			if(link[i]==null){
				SiriusErrorLog.addErrorMessage("Bad link id");
				return false;
			}
		}
		
		// check size of data
		for(i=0;i<link.length;i++){
			if(initial_density[i].length!=vehicletypeindex.length){
				SiriusErrorLog.addErrorMessage("Wrong number of data points.");
				return false;
			}
		}

		// check that values are between 0 and jam density
		int j;
		Double sum;
		Double x;
		for(i=0;i<initial_density.length;i++){
			
			if(link[i].issource)	// does not apply to source links
				continue;
			
			sum = 0.0;
			for(j=0;j<vehicletypeindex.length;j++){
				x = initial_density[i][j];
				if(x<0 || x.isNaN()){
					SiriusErrorLog.addErrorMessage("Invalid initial density.");
					return false;
				}
				sum += x;
			}
			
			// NOTE: REMOVED THIS CHECK TEMPORARILY. NEED TO DECIDE HOW TO DO IT 
			// WITH ENSEMBLE FUNDAMENTAL DIAGRAMS
//			if(sum>link[i].getDensityJamInVPMPL()){
//				SiriusErrorLog.addErrorMessage("Initial density exceeds jam density.");
//				return false;
//			}
		}
		
		return true;
	}

	protected void reset() {
	}

	protected void update() {
	}
	
	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////
	
	public Double [] getDensityForLinkIdInVeh(String network_id,String linkid){
		Double [] d = SiriusMath.zeros(myScenario.getNumVehicleTypes());
		for(int i=0;i<link.length;i++){
			if(link[i].getId().equals(linkid) && link[i].myNetwork.getId().equals(network_id)){
				for(int j=0;j<vehicletypeindex.length;j++)
					d[vehicletypeindex[j]] = initial_density[i][j]*link[i].getLengthInMiles();
				return d;
			}
		}
		return d;
	}

	public class Tuple {
		private String link_id;
		private String network_id;
		private int vehicle_type_index;
		private double density;
		public Tuple(String link_id, String network_id, int vehicle_type_index,
				double density) {
			this.link_id = link_id;
			this.network_id = network_id;
			this.vehicle_type_index = vehicle_type_index;
			this.density = density;
		}
		/**
		 * @return the link id
		 */
		public String getLinkId() {
			return link_id;
		}
		/**
		 * @return the network id
		 */
		public String getNetworkId() {
			return network_id;
		}
		/**
		 * @return the vehicle type index
		 */
		public int getVehicleTypeIndex() {
			return vehicle_type_index;
		}
		/**
		 * @return the density, in vehicles
		 */
		public double getDensity() {
			return density;
		}
	}

	/**
	 * Constructs a list of initial densities,
	 * along with the corresponding link identifiers and vehicle types
	 * @return a list of <code/><link id, network id, vehicle type index, density></code> tuples
	 */
	public List<Tuple> getData() {
		List<Tuple> data = new ArrayList<Tuple>(link.length * vehicletypeindex.length);
		for (int iii = 0; iii < link.length; ++iii)
			for (int jjj = 0; jjj < vehicletypeindex.length; ++jjj)
				data.add(new Tuple(link[iii].getId(), link[iii].myNetwork.getId(),
						vehicletypeindex[iii].intValue(),
						initial_density[iii][jjj] * link[iii].getLengthInMiles()));
		return data;
	}

}
