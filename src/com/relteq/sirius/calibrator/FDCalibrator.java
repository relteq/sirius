package com.relteq.sirius.calibrator;

import java.math.BigDecimal;
import java.util.*;

import com.relteq.sirius.data.pems.DataReader;
import com.relteq.sirius.data.pems.FiveMinuteData;
import com.relteq.sirius.jaxb.FundamentalDiagramProfileSet;
import com.relteq.sirius.sensor.DataSource;
import com.relteq.sirius.sensor.SensorLoopStation;
import com.relteq.sirius.simulator.*;

/** Fundamental diagram calibration routine.
 * 
 * <p> Implements fundamental diagram fitting based on data referenced by sensors.
* @author Gabriel Gomes
* @version VERSION NUMBER
*/
public class FDCalibrator {
	protected String configfilename;
	protected String outputfilename;
	protected ArrayList<DataSource> datasources = new ArrayList<DataSource>();
	protected Scenario scenario;
	protected HashMap <Integer,FiveMinuteData> data = new HashMap <Integer,FiveMinuteData> ();
	
	public FDCalibrator(String configfilename,String outputfilename){
		this.configfilename = configfilename;
		this.outputfilename = outputfilename;
	}

	// execution .................................
	public void run() throws Exception{
		
		if(!readScenario())									// 1. read the original network file 
			return;	
		
		loadTrafficData();									// 2. read pems 5 minute file
		
															// 3. run calibration routine
		for(com.relteq.sirius.jaxb.Sensor sensor : scenario.getSensorList().getSensor()){
			SensorLoopStation S = (SensorLoopStation) sensor;
			if(S.getMyType().compareTo(Sensor.Type.static_point)!=0)
				continue;
			calibrate(S);
		}
		
		propagate();									// 4. extend parameters to the rest of the network
		export(); 										// 5. export to configuration file
		
		System.out.println("done");
	}

	// step 1
	private boolean readScenario() {
		scenario = ObjectFactory.createAndLoadScenario(configfilename);
		if(scenario==null || SiriusErrorLog.haserror()){
			SiriusErrorLog.print();
			return false;
		}
		return true;
	}

	// step 2
	public void loadTrafficData() throws Exception {
		
		ArrayList<String> uniqueurls  = new ArrayList<String>();
		
		// construct list of stations to extract from datafile 
		for(com.relteq.sirius.jaxb.Sensor sensor : scenario.getSensorList().getSensor()){
			if(((Sensor) sensor).getMyType().compareTo(Sensor.Type.static_point)!=0)
				continue;
			SensorLoopStation S = (SensorLoopStation) sensor;
			int myVDS = S.getVDS();				
			data.put(myVDS, new FiveMinuteData(myVDS,true));	
			for(com.relteq.sirius.sensor.DataSource d : S.get_datasources()){
				String myurl = d.getUrl();
				int indexOf = uniqueurls.indexOf(myurl);
				if( indexOf<0 ){
					DataSource newdatasource = new DataSource(d);
					newdatasource.add_to_for_vds(myVDS);
					datasources.add(newdatasource);
					uniqueurls.add(myurl);
				}
				else{
					datasources.get(indexOf).add_to_for_vds(myVDS);
				}
			}
		}
		
		// Read 5 minute data to "data"
		DataReader P = new DataReader();
		P.Read5minData(data,datasources);
	}

	// step 3
	public void calibrate(SensorLoopStation S) {
		int i;
		int vds = S.getVDS();

		// output:
		float vf;			// [mph]
		float w;			// [mph]
		float q_max;		// [veh/hr/lane]
	    float rho_crit;		// [veh/mile/lane]
	   
		float w_min = 10;			// [mph]
		float w_max = 19;			// [mph]

		// get data
		FiveMinuteData D = data.get(vds);
		int numdatapoints = D.getNumDataPoints();

		// degenerate case
		if(numdatapoints==0)
			return;  

		// organize into an array of DataPoint
		ArrayList<DataPoint> datavec = new ArrayList<DataPoint>();
		for(i=0;i<numdatapoints;i++)
			datavec.add(new DataPoint(D.getAggDty(i),D.getAggFlw(i),D.getAggSpd(i)));
		
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
		S.setFD(vf,w,q_max);
		
	}
  
	// step 4
	public void propagate(){
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
				FD.setCapacity(new BigDecimal(S.getQ_max()));
				FD.setCapacityDrop(new BigDecimal(0));
				FD.setCongestionSpeed(new BigDecimal(S.getW()));
				FD.setJamDensity(new BigDecimal(S.getRho_jam()));
				FD.setJamDensity(new BigDecimal(S.getVf()));
				FD.setStdDevCapacity(new BigDecimal(0));
				
				FDprof.getFundamentalDiagram().add(FD);
				FDprofileset.getFundamentalDiagramProfile().add(FDprof);
			}
		}
		
		scenario.setFundamentalDiagramProfileSet(FDprofileset);
	}

	// step 5
	private void export() throws Exception{
		scenario.saveToXML(outputfilename);
	}
	
	// private routines.................................

	// compute the p'th percentile qty (p in [0,1])
	private float percentile(String qty,List<DataPoint> x,float p){
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

	private void growout(String upordn,GrowLink G,HashMap<String,GrowLink> H){
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

	// internal classes ...............................
	public class DataPoint implements Comparable<Object> {
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

	public class GrowLink {
		public Link link;
		public Sensor sensor;
		public boolean isgrowable = false; // link possibly connected to unassigned links
		public boolean isassigned = false; // network is divided into assigned and unassigned subnetworks
		public GrowLink(Link l){link=l;}
	}

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
		
		FDCalibrator C = new FDCalibrator(args[0],args[1]);
		try {
			C.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
			
}
