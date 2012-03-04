package com.relteq.sirius.simulator;

import java.util.List;

import com.relteq.sirius.jaxb.Link;
import com.relteq.sirius.jaxb.Network;

/** Class used for storing the state trajectory of a scenario. 
 * <p>
* @author Gabriel Gomes
*/
public final class SiriusStateTrajectory {

	protected _Scenario myScenario;
	protected int numNetworks;								// number of networks in the scenario
	protected NewtorkStateTrajectory [] networkState;		// array of states trajectories for networks
	protected int numVehicleTypes; 							// size of 2nd dimension of networkState
	protected int numTime; 									// size of 3rd dimension of networkState

	/////////////////////////////////////////////////////////////////////
	// construction
	/////////////////////////////////////////////////////////////////////
	
	public SiriusStateTrajectory(_Scenario myScenario) {
		if(myScenario==null)
			return;
		if(myScenario.getNetworkList()==null)
			return;
		if(myScenario.getNetworkList().getNetwork()==null)
			return;
		this.myScenario = myScenario;
		//this.outsteps = myScenario.o
		
		this.numNetworks = myScenario.getNetworkList().getNetwork().size();
		this.numVehicleTypes = myScenario.getNumVehicleTypes();
		this.numTime = myScenario.getTotalTimeStepsToSimulate();
		
		this.networkState = new NewtorkStateTrajectory[numNetworks];
		for(int i=0;i<numNetworks;i++){
			int numLinks = myScenario.getNetworkList().getNetwork().get(i).getLinkList().getLink().size();
			this.networkState[i] = new NewtorkStateTrajectory(numLinks);
		}
	}

	/////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////
	
	public Double getDensity(int netindex,int i,int j,int k) {
		if(netindex<0 || netindex>=numNetworks)
			return Double.NaN;
		NewtorkStateTrajectory  N = networkState[netindex];
		if(i<0 || i>=N.getNumLinks() || j<0 || j>=numVehicleTypes || k<0 || k>=numTime)
			return Double.NaN;
		else
			return N.density[i][j][k];
	}

	public Double getFlow(int netindex,int i,int j,int k) {
		if(netindex<0 || netindex>=numNetworks)
			return Double.NaN;
		NewtorkStateTrajectory  N = networkState[netindex];
		if(i<0 || i>=N.getNumLinks() || j<0 || j>=numVehicleTypes || k<0 || k>=numTime)
			return Double.NaN;
		else
			return N.flow[i][j][k];
	}

	protected void recordstate(int timestep,double time,boolean exportflows,int outsteps) {
		
		int i,j;
		double invsteps;
		
		if(timestep==1)
			invsteps = 1f;
		else
			invsteps = 1f/((double)outsteps);

		for(int netindex=0;netindex<numNetworks;netindex++){
			Network network = myScenario.getNetworkList().getNetwork().get(netindex);
			List<Link> links = network.getLinkList().getLink();
			for(i=0;i<networkState[netindex].getNumLinks();i++){
				_Link link = (_Link) links.get(i);				
				for(j=0;j<numVehicleTypes;j++){
					networkState[netindex].density[i][j][timestep] = link.cumulative_density[j]*invsteps;
					if(exportflows)
						networkState[netindex].flow[i][j][timestep] = link.cumulative_outflow[j]*invsteps;
				}
			}
			netindex++;
		}
	}

	/////////////////////////////////////////////////////////////////////
	// internal class
	/////////////////////////////////////////////////////////////////////
	
	public class NewtorkStateTrajectory{

		protected int numLinks; 		// size of 1st dimension
		protected Double[][][] density; // [veh]
		protected Double[][][] flow; 	// [veh]

		public NewtorkStateTrajectory(int numLinks) {
			this.numLinks = numLinks;
			this.density = new Double[numLinks][numVehicleTypes][numTime];
			this.flow = new Double[numLinks][numVehicleTypes][numTime];
		}
		public int getNumLinks() {
			return numLinks;
		}
	}
	
}
