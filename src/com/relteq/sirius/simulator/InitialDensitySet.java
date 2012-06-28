/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class InitialDensitySet extends com.relteq.sirius.jaxb.InitialDensitySet {

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

}
