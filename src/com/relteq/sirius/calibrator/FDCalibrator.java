package com.relteq.sirius.calibrator;

import java.math.BigDecimal;
import java.util.*;

import com.relteq.sirius.jaxb.FundamentalDiagramProfileSet;
import com.relteq.sirius.sensor.SensorLoopStation;
import com.relteq.sirius.simulator.*;

/** Fundamental diagram calibration routine.
 * 
 * <p> Implements fundamental diagram fitting based on data referenced by sensors.
* @author Gabriel Gomes
* @version VERSION NUMBER
*/
public class FDCalibrator {
	
	/////////////////////////////////////////////////////////////////////////////////
	// API
	/////////////////////////////////////////////////////////////////////////////////
	
	/** run calibration on all sensors and propagate result to links.
	 * 
	 */
	public static void calibrate(Scenario scenario) throws SiriusException {

		HashMap <SensorLoopStation,FDParameters> sensorFD = new HashMap <SensorLoopStation,FDParameters> ();
		
		// read pems 5 minute file
		scenario.loadSensorData();
		
		// run calibration routine
		for(com.relteq.sirius.jaxb.Sensor sensor : scenario.getSensorList().getSensor()){
			SensorLoopStation S = (SensorLoopStation) sensor;
			if(S.getMyType().compareTo(Sensor.Type.static_point)!=0)
				continue;
			sensorFD.put(S,calibrate_sensor(S));
		}
		
		// extend parameters to the rest of the network
		propagate(scenario,sensorFD);

	}

	/////////////////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////////////////
	
	private static FDParameters calibrate_sensor(SensorLoopStation S) {
		int i;

		// output:
		float vf;			// [mph]
		float w;			// [mph]
		float q_max;		// [veh/hr/lane]
	    float rho_crit;		// [veh/mile/lane]
	   
		float w_min = 10;			// [mph]
		float w_max = 19;			// [mph]

		// get data
		int numdatapoints = S.getNumDataPoints();

		// degenerate case
		if(numdatapoints==0)
			return new FDParameters();  

		// organize into an array of DataPoint
		ArrayList<DataPoint> datavec = new ArrayList<DataPoint>();
		for(i=0;i<numdatapoints;i++)
			datavec.add(new DataPoint(S.getDataAggDtyInVPMPL(i),S.getDataAggFlwInVPHPL(i),S.getDataAggSpdInMPH(i)));
		
		// Find free-flow velocity ...............................

		// maximum flow and its corresponding density
		DataPoint maxflw = new DataPoint(0f,Float.NEGATIVE_INFINITY,0f);
		for(i=0;i<numdatapoints;i++)
			if(datavec.get(i).flw>maxflw.flw)
				maxflw.setval(datavec.get(i));

		q_max = maxflw.flw;

		// split data into congested and freeflow regimes ...............
		ArrayList<DataPoint> congestion = new ArrayList<DataPoint>();		// congestion states
		ArrayList<DataPoint> freeflow = new ArrayList<DataPoint>();			// freeflow states
		for(i=0;i<numdatapoints;i++)
			if(datavec.get(i).dty>=maxflw.dty)
				congestion.add(datavec.get(i));
			else
				freeflow.add(datavec.get(i));

		vf = percentile("spd",freeflow,0.5f);
		rho_crit = q_max/vf;

		// BINNING
		ArrayList<DataPoint> supercritical = new ArrayList<DataPoint>(); 	// data points above rho_crit
		for(i=0;i<numdatapoints;i++)
			if(datavec.get(i).dty>=rho_crit)
				supercritical.add(datavec.get(i));

		// sort supercritical w.r.t. density
		Collections.sort(supercritical);

		int numsupercritical = supercritical.size();
		int Bin_width = 10;
		int step=Bin_width;
		ArrayList<DataPoint> BinData = new ArrayList<DataPoint>();
		for(i=0;i<numsupercritical;i+=Bin_width){

			if(i+Bin_width>=numsupercritical)
		        step = numsupercritical-i;

		    if(step!=0){
		    	List<DataPoint> Bin = (List<DataPoint>) supercritical.subList(i,i+step);
		    	if(!Bin.isEmpty()){
			        float a = 2.5f*percentile("flw",Bin,0.75f) - 1.5f*percentile("flw",Bin,0.25f); 			        
			        float b = percentile("flw",Bin,1f);
			        BinData.add(new DataPoint(percentile("dty",Bin,0.5f),Math.min(a,b),Float.NaN));
		    	}
		    }
		}

		// Do constrained LS
		ArrayList<Float> ai = new ArrayList<Float>();
		ArrayList<Float> bi = new ArrayList<Float>();
		for(i=0;i<BinData.size();i++){
			bi.add(q_max - BinData.get(i).flw);
			ai.add(BinData.get(i).dty - rho_crit);
		}

		if(BinData.size()>0){
			float sumaibi = 0;
			float sumaiai = 0;
			for(i=0;i<BinData.size();i++){
				sumaibi += ai.get(i)*bi.get(i);
				sumaiai += ai.get(i)*ai.get(i);
			}
			w = sumaibi/sumaiai;
			w = Math.max(w,w_min);
			w = Math.min(w,w_max);
		}
		else{
		    w  = Float.NaN;
		}

		// store parameters in sensor
		return new FDParameters(vf,w,q_max);
	}
  
	private static void propagate(Scenario scenario,HashMap<SensorLoopStation,FDParameters> sensorFD){
		int i;
		boolean done;
		
		// create new fd profile set
		FundamentalDiagramProfileSet FDprofileset = new FundamentalDiagramProfileSet();
							
		// populate the grow network
		ArrayList<GrowLink> arraygrownetwork = new ArrayList<GrowLink>();
		HashMap<String,GrowLink> hashgrownetwork = new HashMap<String,GrowLink>();
			for(com.relteq.sirius.jaxb.Network network : scenario.getNetworkList().getNetwork()){
			for(com.relteq.sirius.jaxb.Link link : network.getLinkList().getLink()){
				GrowLink G = new GrowLink((Link) link);
				hashgrownetwork.put(link.getId(),G);
				arraygrownetwork.add(G);
			}
		}

		// initialize the grow network with sensored links 
		for(com.relteq.sirius.jaxb.Sensor sensor : scenario.getSensorList().getSensor()){
			SensorLoopStation S = (SensorLoopStation) sensor;
			if(S.getVDS()!=0 & S.getMyLink()!=null){
				String linkid = S.getMyLink().getId();
				GrowLink G = hashgrownetwork.get(linkid);
				G.sensor = S;
				G.isassigned = true;
				G.isgrowable = true;
			}
		}
		
		// repeatedly traverse network until all assigned links cannot be grown
		done = false;
		while(!done){
			done = true;

			// step through all links
			for(i=0;i<arraygrownetwork.size();i++) {

				GrowLink G = arraygrownetwork.get(i);

				// continue if G is assigned and not growable, or if G is unassigned
				if( (G.isassigned & !G.isgrowable) | !G.isassigned)
					continue;

				done = false;

				// so G is assigned and growable, expand to its upstream and downstream links
				growout("up",G,hashgrownetwork);
				growout("dn",G,hashgrownetwork);
				G.isgrowable = false;
			}
		}

		// copy parameters to the links
		for(i=0;i<arraygrownetwork.size();i++){
			GrowLink G = arraygrownetwork.get(i);
			if(G.isassigned){
				com.relteq.sirius.jaxb.FundamentalDiagramProfile FDprof = new com.relteq.sirius.jaxb.FundamentalDiagramProfile();
				com.relteq.sirius.jaxb.FundamentalDiagram FD = new com.relteq.sirius.jaxb.FundamentalDiagram();

				FDprof.setLinkId(G.link.getId());
				FDprof.setDt(new BigDecimal(300));
				FDprof.setStartTime(new BigDecimal(0));
				
				SensorLoopStation S = (SensorLoopStation) G.sensor;
				FDParameters FDp = (FDParameters) sensorFD.get(S);
				
				FD.setCapacity(new BigDecimal(FDp.getQ_max()));
				FD.setCapacityDrop(new BigDecimal(0));
				FD.setCongestionSpeed(new BigDecimal(FDp.getW()));
				FD.setJamDensity(new BigDecimal(FDp.getRho_jam()));
				FD.setJamDensity(new BigDecimal(FDp.getVf()));
				FD.setStdDevCapacity(new BigDecimal(0));
				
				FDprof.getFundamentalDiagram().add(FD);
				FDprofileset.getFundamentalDiagramProfile().add(FDprof);
			}
		}
		
		scenario.setFundamentalDiagramProfileSet(FDprofileset);
	}

	// compute the p'th percentile qty (p in [0,1])
	private static float percentile(String qty,List<DataPoint> x,float p){
		ArrayList<Float> values = new ArrayList<Float>();
		int numdata = x.size();
		if(qty.equals("spd"))
			for(int i=0;i<numdata;i++)
				values.add(x.get(i).spd);
		if(qty.equals("flw"))
			for(int i=0;i<numdata;i++)
				values.add(x.get(i).flw);
		if(qty.equals("dty"))
			for(int i=0;i<numdata;i++)
				values.add(x.get(i).dty);

		Collections.sort(values);

		if(p==0)
			return values.get(0);
		if(p==1)
			return values.get(numdata-1);

		int z = (int) Math.floor(numdata*p);
		if(numdata*p==z)
			return (values.get(z-1)+values.get(z))/2f;
		else
			return values.get(z);
	}

	private static void growout(String upordn,GrowLink G,HashMap<String,GrowLink> H){
		Node node;
		Link [] newlinks = null;
		if(upordn.equals("up")){			// grow in upstream direction
			node = G.link.getBegin_node();
			if(node!=null)
				newlinks = node.getInput_link();
		}
		else{								// grow in downstream direction
			node = G.link.getEnd_node();
			if(node!=null)
				newlinks = node.getOutput_link();
		}

		if(newlinks==null)
			return;

		for(int i=0;i<newlinks.length;i++){
			GrowLink nG = H.get(newlinks[i].getId());
			// propagate if nG is unassigned and of the same type as G
			
			if( !nG.isassigned & nG.link.getType().compareTo(G.link.getType())==0 ){
				nG.sensor = G.sensor;
				nG.isassigned = true;
				nG.isgrowable = true;
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////
	// nested classes
	/////////////////////////////////////////////////////////////////////////////////
	
	private static class DataPoint implements Comparable<Object> {
		float dty;
		float flw;
		float spd;
		public DataPoint(float d,float f,float s){
			dty=d;
			flw=f;
			spd=s;
		}
		public void setval(DataPoint x){
			this.dty=x.dty;
			this.flw=x.flw;
			this.spd=x.spd;
		}
		@Override
		public String toString() {
			return String.format("%f",dty);
		}
		@Override
		public int compareTo(Object x) {
			float thatdty = ((DataPoint) x).dty;
			if(this.dty==thatdty)
				return 0;
			else if(this.dty>thatdty)
				return 1;
			else
				return -1;
		}
	}

	private static class GrowLink {
		public Link link;
		public Sensor sensor;
		public boolean isgrowable = false; // link possibly connected to unassigned links
		public boolean isassigned = false; // network is divided into assigned and unassigned subnetworks
		public GrowLink(Link l){link=l;}
	}

	/////////////////////////////////////////////////////////////////////////////////
	// main
	/////////////////////////////////////////////////////////////////////////////////
	
	public static void main(String[] args) {
		if(args.length<2){
			String str;
			str = "Usage:" + "\n";
			str += "-----\n" + "\n";
			str += "args[0]: Configuration file name. (required)\n";
			str += "args[1]: Output file name.\n";
			System.out.println(str);
			return;
		}
		
		String configfilename = args[0];
		String outputfilename = args[1];
		Scenario scenario;
		
		try {

			
			// read the original network file 
			scenario = ObjectFactory.createAndLoadScenario(configfilename);
			if(scenario==null)
				return;	
			
			// run calibrator
			calibrate(scenario);

			// export to configuration file
			scenario.saveToXML(outputfilename);
			
			System.out.println("done");
			
		} catch (SiriusException e) {
			e.printStackTrace();
		}
	}	

}
