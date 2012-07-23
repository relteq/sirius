/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;
import java.util.List;

public final class InitialDensitySet extends com.relteq.sirius.jaxb.InitialDensitySet {

	private Scenario myScenario;
	private Double [][] initial_density; 	// [veh/mile] indexed by link and type
	private Link [] link;					// ordered array of references
	private Integer [] vehicletypeindex; 	// index of vehicle types into global list
	protected double timestamp;

	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////

	protected void populate(Scenario myScenario){
		
		int i;
		
		this.myScenario = myScenario;
		
		// count links in initial density list that are also in the scenario
		int numLinks = getDensity().size();
		int numLinks_exist = 0;
		Link [] templink = new Link[numLinks];
		for(i=0;i<numLinks;i++){
			com.relteq.sirius.jaxb.Density density = getDensity().get(i);
			templink[i] = myScenario.getLinkWithId(density.getLinkId());
			if(templink[i]!=null)
				numLinks_exist++;
		}
		
		// allocate
		initial_density = new Double [numLinks_exist][];
		link = new Link [numLinks_exist];
		vehicletypeindex = myScenario.getVehicleTypeIndices(getVehicleTypeOrder());

		// copy profile information to arrays in extended object
		int c = 0;
		for(i=0;i<numLinks;i++){
			if(templink[i]!=null){
				com.relteq.sirius.jaxb.Density density = getDensity().get(i);
				link[c] = templink[i];
				Double1DVector D = new Double1DVector(density.getContent(),":");
				initial_density[c] = D.getData();
				c++;
			}
		}
		
		// round to the nearest decisecond
		if(getTstamp()!=null)
			timestamp = SiriusMath.round(getTstamp().doubleValue()*10.0)/10.0;
		else
			timestamp = 0.0;
		
	}

	protected void validate() {
		
		int i;
		
		// check that all vehicle types are accounted for
		if(vehicletypeindex.length!=myScenario.getNumVehicleTypes())
			SiriusErrorLog.addError("List of vehicle types in initial density profile id=" + getId() + " does not match that of settings.");
		
		// check that vehicle types are valid
		for(i=0;i<vehicletypeindex.length;i++)
			if(vehicletypeindex[i]<0)
				SiriusErrorLog.addError("Bad vehicle type name in initial density profile id=" + getId());
		
		// check size of data
		if(link!=null)
			for(i=0;i<link.length;i++)
				if(initial_density[i].length!=vehicletypeindex.length)
					SiriusErrorLog.addError("Number of density values does not match number of vehicle types in initial density profile id=" + getId());

		// check that values are between 0 and jam density
		int j;
		Double sum;
		Double x;
		for(i=0;i<initial_density.length;i++){
			
			if(link[i]==null){
				SiriusErrorLog.addWarning("Unknown link id in initial density profile");
				continue;
			}
			
			if(link[i].issource)	// does not apply to source links
				continue;
			
			sum = 0.0;
			for(j=0;j<vehicletypeindex.length;j++){
				x = initial_density[i][j];
				if(x<0)
					SiriusErrorLog.addError("Negative value found in initial density profile for link id=" + link[i].getId());
				if( x.isNaN())
					SiriusErrorLog.addError("Invalid value found in initial density profile for link id=" + link[i].getId());
				sum += x;
			}
			
			// NOTE: REMOVED THIS CHECK TEMPORARILY. NEED TO DECIDE HOW TO DO IT 
			// WITH ENSEMBLE FUNDAMENTAL DIAGRAMS
//			if(sum>link[i].getDensityJamInVPMPL())
//				SiriusErrorLog.addErrorMessage("Initial density exceeds jam density.");

		}		
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
						vehicletypeindex[jjj].intValue(),
						initial_density[iii][jjj] * link[iii].getLengthInMiles()));
		return data;
	}

}
