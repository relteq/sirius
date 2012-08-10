package com.relteq.sirius.control;

import java.util.ArrayList; 
import java.util.HashSet;
import java.util.Set;

import com.relteq.sirius.simulator.Controller;
import com.relteq.sirius.control.Controller_CRM_HERO;
import com.relteq.sirius.simulator.Link;
import com.relteq.sirius.simulator.Node;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.Sensor;
import com.relteq.sirius.sensor.SensorLoopStation;
import com.relteq.sirius.simulator.SiriusErrorLog;

public class Controller_CRM_HERO extends Controller {

	private Link onrampLink = null;
	private Link mainlineLink = null;
	private Sensor mainlineSensor = null;
	private SensorLoopStation queueSensor = null;

	private boolean useMainlineSensor;
	private boolean useQueueSensor;
	
	boolean hasMainlineLink;		// true if config file contains entry for mainlinelink
	boolean hasMainlineSensor; 		// true if config file contains entry for mainlinesensor
	boolean hasQueueSensor; 		// true if config file contains entry for queuesensor
	
	private static ArrayList<String> controllersOrdered = new ArrayList<String>();                          // Controller's IDs arranged from Downstream to Upstream
	private static ArrayList<Controller_CRM_HERO> controllerList = new ArrayList<Controller_CRM_HERO>();    // Hero Controller List Arranged from Downstream to Upstream
	
	private double mainlineVehiclesCurrent;	
	private double targetVehicles;			// [veh/mile/lane]
	private boolean targetDensity_Given;
	

	private double queueCurrent;
    private double queueSum;            //Sum of queueCurrents of Hero Controllers in a Coordination String   		
	
	private double queueMin;                 // minimum desired queue length when a slave ramp [veh] 	
	
	private double queueMax;                 //max admissible queue length of the HERO controlled ramp [veh/lane]   	
	private boolean queueMax_Given;
	
    private double queueMaxSum;         //Sum of queueMax of Hero Controllers in a Coordination String 
    
	private  status type = status.NOT_USED;   // HERO controlled ramp status: {SLAVE, NOT_USED, MASTER}
	private  status typePrevious = status.NOT_USED; 
	protected enum status {MASTER,
        				   SLAVE,
        				   NOT_USED };   
        							
  	private ArrayList<Integer> setPossibleSlaves = new ArrayList<Integer>(); // List of Upstream Hero Controllers that could be Slaves
	private ArrayList<Integer> setSlaves = new ArrayList<Integer>();    	 //	List of current Slaves
        						  
	private double actThresholdQ = 0.5;   //queue activation threshold of the HERO controlled ramp. 
	private double deactThresholdQ =0.5;  //queue deactivation threshold of the HERO controlled ramp. 
	private double actThresholdM = 0.9;   //mainline activation threshold of the HERO controlled ramp. 
	private double deactThresholdM =0.8;  //mainline deactivation threshold of the HERO controlled ramp.	
	
	private static int timeStep = -1; // Flag to Update Hero Controllers(Must be initialized to a number different than 0)

	private double alineaGainNormalized;	
	private double queueControllerGainNormalized=1;
	private double queueMinControllerGainNormalized=1;
	
	private double minFlow;                 // minimum allowed metering rate [veh/hour/lane]
	private boolean minFlow_Given;
	private double maxFlow;              // maximum allowed metering rate [veh/hour/lane]
	private boolean maxFlow_Given;
	
	private double flowQueue;      //metering rate determined by queue controller
	private double flowQueueMin;   //metering rate determined by minimum queue controller
	private double flowAlinea;     //metering rate determined by ALINEA
	private double flowHero;       //metering rate determined by HERO
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

    public Controller_CRM_HERO() {
    	// TODO Auto-generated constructor stub
    }

	public Controller_CRM_HERO(Scenario myScenario,Link onramplink,Link mainlinelink,Sensor mainlinesensor,Sensor queuesensor,double gain_in_mph){
		// TODO Auto-generated constructor stub		
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceController
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public void populate(Object jaxbobject) {

		com.relteq.sirius.jaxb.Controller jaxbc = (com.relteq.sirius.jaxb.Controller) jaxbobject;
		
		if(jaxbc.getTargetElements()==null)
			return;
		if(jaxbc.getTargetElements().getScenarioElement()==null)
			return;
		if(jaxbc.getFeedbackElements()==null)
			return;
		if(jaxbc.getFeedbackElements().getScenarioElement()==null)
			return;
		
		hasMainlineLink = false;
		hasMainlineSensor = false;
		hasQueueSensor = false;
		
		// There should be only one target element, and it is the onramp
		if(jaxbc.getTargetElements().getScenarioElement().size()==1){
			com.relteq.sirius.jaxb.ScenarioElement s = jaxbc.getTargetElements().getScenarioElement().get(0);
			onrampLink = myScenario.getLinkWithId(s.getId());	
		}
		
		
		// Feedback elements can be "mainlineSensor","mainlineLink", and "queueSensor"
		if(!jaxbc.getFeedbackElements().getScenarioElement().isEmpty()){
			
			for(com.relteq.sirius.jaxb.ScenarioElement s:jaxbc.getFeedbackElements().getScenarioElement()){
				
				if(s.getUsage()==null)
					return;
				
				if( s.getUsage().equalsIgnoreCase("mainlinesensor") &&
				    s.getType().equalsIgnoreCase("sensor") && mainlineSensor==null){
					mainlineSensor=myScenario.getSensorWithId(s.getId());
					hasMainlineSensor = true;
				}

				if( s.getUsage().equalsIgnoreCase("mainlinelink") &&
					s.getType().equalsIgnoreCase("link") && mainlineLink==null){
					mainlineLink=myScenario.getLinkWithId(s.getId());
					hasMainlineLink = true;
				}

				if( s.getUsage().equalsIgnoreCase("queuesensor") &&
					s.getType().equalsIgnoreCase("sensor")  && queueSensor==null){
					queueSensor=(SensorLoopStation)myScenario.getSensorWithId(s.getId());
					hasQueueSensor = true;
				}				
			}
		}
		
		// abort unless there is either one mainline link or one mainline sensor
		if(mainlineLink==null && mainlineSensor==null)
			return;
		if(mainlineLink!=null  && mainlineSensor!=null)
			return;
		
		useMainlineSensor = mainlineSensor!=null;
		
		// need the sensor's link for target density
		if(useMainlineSensor)
			mainlineLink = mainlineSensor.getMyLink();
		
		if(mainlineLink==null)
			return;
					
		// HERO controllersList Initialization (done When First Hero Controller in the list of controllers is populated)
		if(controllersOrdered.size()==0){
			controllersOrdered =orderHeroControllers();  	
			for(int i=0; i< controllersOrdered.size(); i++){
				controllerList.add(null);
			}
		}
		
		
		// Read parameters
				
		double gain_in_mph = 50.0;
		targetDensity_Given = false;
		minFlow_Given=false;
		maxFlow_Given=false;
		queueMax_Given=false;
		
		ArrayList<String> setPossibleSlavesUnordered = new  ArrayList<String>();
		
		if(jaxbc.getParameters()!=null)
			for(com.relteq.sirius.jaxb.Parameter p : jaxbc.getParameters().getParameter()){
								
				if(p.getName().equals("gainAlinea")){
					gain_in_mph = Double.parseDouble(p.getValue());
				}
				
				if(p.getName().equals("gainQueueController")){
					queueControllerGainNormalized = Double.parseDouble(p.getValue());
				}
				
				if(p.getName().equals("gainMinQueueController")){
					queueMinControllerGainNormalized = Double.parseDouble(p.getValue());
				}

				if(p.getName().equals("targetDensity")){
					if(mainlineLink!=null){
						targetVehicles = Double.parseDouble(p.getValue());   // [in vpmpl]
						targetVehicles *= mainlineLink.get_Lanes()*mainlineLink.getLengthInMiles(); // now in [veh]
						targetDensity_Given = true;
					}
				}
				
				if(p.getName().equals("possibleSlave")){
					setPossibleSlavesUnordered.add(p.getValue());
				}
				
				if(p.getName().equals("queueMax")){
					queueMax = Double.parseDouble(p.getValue())*onrampLink.get_Lanes(); //[veh/onramp]
					queueMax_Given=true;
				} 
				
				if(p.getName().equals("actThresholdQ")){
					actThresholdQ = Double.parseDouble(p.getValue());
				}
				
				if(p.getName().equals("deactThresholdQ")){
					deactThresholdQ = Double.parseDouble(p.getValue());
				}
				
				if(p.getName().equals("actThresholdM")){
					actThresholdM = Double.parseDouble(p.getValue());
				}
				
				if(p.getName().equals("deactThresholdM")){
					deactThresholdM = Double.parseDouble(p.getValue());
				}
				
				if(p.getName().equals("minFlow")){
					minFlow = Double.parseDouble(p.getValue())*myScenario.getSimDtInHours()*this.onrampLink.get_Lanes(); //[veh/sim_period/onramp]
				    minFlow_Given=true;
				}
				
				if(p.getName().equals("maxFlow")){
					maxFlow = Double.parseDouble(p.getValue())*myScenario.getSimDtInHours()*this.onrampLink.get_Lanes(); //[veh/sim_period/onramp]
					maxFlow_Given=true;
				}
			}	
		
		
		// Normalize ALINEA Gain
		alineaGainNormalized = gain_in_mph*myScenario.getSimDtInHours()/mainlineLink.getLengthInMiles();
		
		// Set minFlow equal to zero if not given
		if(!minFlow_Given)
			minFlow=1;	
		
		// Orders set of Possible Slaves from Downstream to Upstream
		for(int i=0; i< controllersOrdered.size(); i++){
			for(String unOrderedC: setPossibleSlavesUnordered) {
				if(controllersOrdered.get(i).equals(unOrderedC) ){
					setPossibleSlaves.add(i);
				}
			}
		}
		
		// Add HERO controller to controllerLists in its corresponding Downstream to Upstream Order
		for(int i=0; i< controllersOrdered.size(); i++){
			if(this.id.equals(controllersOrdered.get(i))){
				controllerList.set(i, this);
				break;
			}
		}
	}
	
	
	
	@Override
	public void validate() {
		
		super.validate();

		// must have exactly one target
		if(targets.size()!=1)
			SiriusErrorLog.addError("Numnber of targets for HERO controller id=" + getId()+ " does not equal one.");

		// bad mainline sensor id
		if(hasMainlineSensor && mainlineSensor==null)
			SiriusErrorLog.addError("Bad mainline sensor id in HERO controller id=" + getId()+".");

		// bad queue sensor id
		if(hasQueueSensor && queueSensor==null)
			SiriusErrorLog.addError("Bad queue sensor id in HERO controller id=" + getId()+".");
		
		// both link and sensor feedback
		if(hasMainlineLink && hasMainlineSensor)
			SiriusErrorLog.addError("Both mainline link and mainline sensor are not allowed in HERO controller id=" + getId()+".");
		
		// Mainline sensor is disconnected
		if(useMainlineSensor && mainlineSensor.getMyLink()==null)
			SiriusErrorLog.addError("Mainline sensor is not connected to a link in HERO controller id=" + getId()+ " ");
		
		// Queue sensor is disconnected
		if(queueSensor!=null && queueSensor.getMyLink()==null)
				SiriusErrorLog.addError("Queue sensor in HERO controller id=" + getId()+ " is not connected to a link");

		// no feedback
		if(mainlineLink==null)
			SiriusErrorLog.addError("Invalid mainline link for HERO controller id=" + getId()+ ".");
		
		// Target link id not found, or number of targets not 1.
		if(onrampLink==null)
			SiriusErrorLog.addError("Invalid onramp link for HERO controller id=" + getId()+ ".");
		
		// No queue sensor
		if(queueSensor==null)
			SiriusErrorLog.addError("Invalid/Unavailable queue sensor for HERO controller id=" + getId()+ ".");
		
		// queueSensor link_reference is not the same as onrampLink id
		if(queueSensor!=null && (!queueSensor.getMyLink().getId().equals(onrampLink.getId()) || !queueSensor.getMyLink().getType().equals("onramp")))
			SiriusErrorLog.addError("Queue sensor is not connected to the onramp link of HERO controller id=" + getId()+ " ");		
				
		// negative gain
		if(mainlineLink!=null && alineaGainNormalized<=0f)
			SiriusErrorLog.addError("Non-positive gainAlinea for HERO controller id=" + getId()+ ".");
		
		//Controller Ids not unique
		Set<String> s = new HashSet<String>(controllersOrdered);  
		if(controllersOrdered.size()!=s.size())
			SiriusErrorLog.addError("Controller ID's are not Unique");
		
		//negative inputs
		if(minFlow<0f || maxFlow<0f || queueControllerGainNormalized<0f || queueMinControllerGainNormalized<0f 
				|| alineaGainNormalized<0f || queueMax<0f ||targetVehicles<0f){
			SiriusErrorLog.addError("Negative input value(s) for HERO controller id=" + getId()+ ".");
		}
		
		//Thresholds smaller or equal to zero
		if(actThresholdQ<=0f || deactThresholdQ<=0f || actThresholdM<=0f || deactThresholdM <=0f ) {
			SiriusErrorLog.addError("Negative threshold value(s) for HERO controller id=" + getId()+ ".");
		}
		
		
	}

	@Override
	public void reset() {
		super.reset();
	}

	@Override
	public void update() {
		
		//HERO Controllers targetDensity, mainlineVehicles and queueCurrent Update (Only Executes once for the first Updated HERO Controller)
		if(timeStep!= myScenario.getCurrentTimeStep() ){ 
			timeStep=myScenario.getCurrentTimeStep();
			//System.out.println("current time step " + timeStep);
		
			//Update queueCurrent, mainlineVehiclesCurrent and targetVehiclesList for All Controllers
			updateHeroControllerTargetVehiclesMainlineVehiclesCurrentQueueCurrentMaxFlowAndQueueMax();
			
			//EVERY Tc SECONDS
			for(Integer i=0; i< controllerList.size(); i++){
				                                                        
				//DEFINITION OF MASTER CONTROLLER
				defineMasterController(i);
			
				//DISSOLUTION OF COORDINATION STRING
				dissolveMasterController(i);
				
				//DEFINITION OF COORDINATION STRING
				defineCoordinationString(i);
				
				//DEFINITION OF MINIMUM QUEUE
				defineSlaveControllersMinimumQueue(i);

				// ALINEA Ramp Metering Controller (EQUATION (1))
				controllerList.get(i).flowAlinea = controllerList.get(i).onrampLink.getTotalOutflowInVeh(0)+
						controllerList.get(i).alineaGainNormalized*(controllerList.get(i).targetVehicles-controllerList.get(i).mainlineVehiclesCurrent);
				
				//Queue Controller (EQUATION (2))			
				
				//System.out.println("Output Flow: " + controllerList.get(i).onrampLink.getTotalOutflowInVeh(0)*3 + " CumOutFlow: " + controllerList.get(i).queueSensor.getCumulativeOutflowInVeh(0)); // the 3 is to account for difference between dt sim and dt controller
			
				controllerList.get(i).flowQueue = -controllerList.get(i).queueControllerGainNormalized*(controllerList.get(i).queueMax-controllerList.get(i).queueCurrent)+
						controllerList.get(i).queueSensor.getCumulativeInflowInVeh(0);
				
		        //EQUATION (3) vs EQUATIONS(4) and(5)
				if(controllerList.get(i).type.equals(status.MASTER)|| controllerList.get(i).type.equals(status.NOT_USED))
					controllerList.get(i).flowHero=Math.min(controllerList.get(i).flowAlinea,controllerList.get(i).flowQueue);
				else{
					controllerList.get(i).flowQueueMin = -controllerList.get(i).queueMinControllerGainNormalized*(controllerList.get(i).queueMin-controllerList.get(i).queueCurrent)+
							controllerList.get(i).queueSensor.getCumulativeInflowInVeh(0);
					controllerList.get(i).flowHero = Math.max(Math.min(controllerList.get(i).flowAlinea, controllerList.get(i).flowQueueMin), controllerList.get(i).flowQueue);
				}
				controllerList.get(i).queueSensor.resetCumulativeOutflowInVeh();	
			}
		
		}
//		System.out.println("time "+timeStep+": Controller" +this.id+ "(control_maxflow[0]="+ Math.max(Math.min(this.flowHero, this.maxFlow), this.minFlow)+
//				")-- minFlow:"+ minFlow+ ", alineaFlow:"+ this.flowAlinea +",flowHero:"+this.flowHero +", maxFlow:"+ maxFlow);
		
		//DEFINITION OF HERO METERING RATE
		control_maxflow[0] = Math.max(Math.min(this.flowHero, this.maxFlow), this.minFlow);
	}	
	
	
	
	@Override
	public boolean register() {
		return registerFlowController(onrampLink,0);
	}
 
	/////////////////////////////////////////////////////////////////////
	// Methods related to Controller Ordering from Downstream to Upstream
	/////////////////////////////////////////////////////////////////////	
	
	//Added by RS
	public ArrayList<String> orderHeroControllers() {
		
		ArrayList<Link> linksOrdered = new ArrayList<Link>();
		ArrayList<Node> nodesOrdered = new ArrayList<Node>();
		ArrayList<String> controllersOrdered = new ArrayList<String>();
							
		//Most Downstream Node	
		for(com.relteq.sirius.jaxb.Link aLink: myScenario.getNetworkList().getNetwork().get(0).getLinkList().getLink()) {
        	if( ((Link)aLink).getType().equals("freeway")  && ((Link)aLink).getEnd_node().getType().equals("terminal")                ) { 	
        		nodesOrdered.add(((Link)aLink).getEnd_node());
        		//System.out.println("The Most Downstream Freeway Link is " + aLink.getId()+ " with End Node " + nodesOrdered.get(0).getId());
        		break;
        	}
		}
					
		//Freeway Links and Nodes  Arranged from Downstream to Upstream by Id
		int isTerminalNode=0;	
		while (isTerminalNode==0) {
			for(com.relteq.sirius.jaxb.Link aLink: myScenario.getNetworkList().getNetwork().get(0).getLinkList().getLink()) {
	        	if( ((Link)aLink).getType().equals("freeway") &&  ((Link)aLink).getEnd_node().getId().equals(nodesOrdered.get(nodesOrdered.size()-1).getId())  ) { 
	        		linksOrdered.add(((Link)aLink));
	        		nodesOrdered.add(((Link)aLink).getBegin_node());	 
	        		//System.out.println("Link: "+ aLink.getId() +" Begin Node: " +((Link)aLink).getBegin_node().getId() );
	        		if (((Link)aLink).getBegin_node().getType().equals("terminal")){
	        			isTerminalNode=1;
	        		}
	        		break;
	        	}
			}
		}
		
		// HERO Controllers Arranged from Downstream to Upstream by Controller Id
		for(Node aNode: nodesOrdered){	
			for (com.relteq.sirius.jaxb.Link aLink: aNode.getInput_link()){ 
				//System.out.println("Input Link: "+ aLink.getId());
				if( ((Link)aLink).getType().equals("onramp") ){
					System.out.println("Node "+ aNode.getId()+": "+ aLink.getType()+ " Link "+ aLink.getId() + " has end node "+ ((Link)aLink).getEnd_node().getId()); 
					
					for(com.relteq.sirius.jaxb.Controller aController: myScenario.getControllerSet().getController()) {
						if(aController.getTargetElements().getScenarioElement().get(0).getId().equals(aLink.getId()) && aController.getType().equals("CRM_hero"))   {
							System.out.println("Controller " + aController.getId() + " is of type " + aController.getType()); 
							controllersOrdered.add(aController.getId());
							break;
						}
					}
					break;
				}
			}  		
		}
	return controllersOrdered;
	}	
	
	
	/////////////////////////////////////////////////////////////////////
	// Methods related to HERO Algorithm 
	/////////////////////////////////////////////////////////////////////	
	
	public void updateHeroControllerTargetVehiclesMainlineVehiclesCurrentQueueCurrentMaxFlowAndQueueMax(){
		//Update queueCurrent, mainlineVehiclesCurrent and targetVehiclesList for All Controllers
		for(Integer i=0; i< controllerList.size(); i++){
			// If target density is not given, it uses the mainline critical density
			if(!controllerList.get(i).targetDensity_Given)
				controllerList.get(i).targetVehicles=controllerList.get(i).mainlineLink.getDensityCriticalInVeh(0);
		
			// get Mainline density either from sensor or from link
			if(controllerList.get(i).useMainlineSensor)
				controllerList.get(i).mainlineVehiclesCurrent=controllerList.get(i).mainlineSensor.getTotalDensityInVeh(0);
			else
				controllerList.get(i).mainlineVehiclesCurrent=controllerList.get(i).mainlineLink.getTotalDensityInVeh(0);
			
			// get Queue either from sensor or from link
			if(controllerList.get(i).useQueueSensor)
				controllerList.get(i).queueCurrent=controllerList.get(i).queueSensor.getTotalDensityInVeh(0);
			else
				controllerList.get(i).queueCurrent=controllerList.get(i).onrampLink.getTotalDensityInVeh(0);
			

			if(!controllerList.get(i).maxFlow_Given)
				controllerList.get(i).maxFlow=onrampLink.getCapacityInVPHPL(0)*myScenario.getSimDtInHours()*onrampLink.get_Lanes(); //[veh/sim_period/onramp];

			if(!controllerList.get(i).queueMax_Given)
				controllerList.get(i).queueMax = controllerList.get(i).onrampLink.getDensityJamInVeh(0); //[veh/onramp]
				
		}
	}
	
	public void defineMasterController(Integer controllerIndex){
		Integer i=controllerIndex;
		//DEFINITION OF MASTER CONTROLLER
		if( controllerList.get(i).type.equals(status.NOT_USED) &&
            controllerList.get(i).queueCurrent/controllerList.get(i).queueMax > controllerList.get(i).actThresholdQ &&
            controllerList.get(i).mainlineVehiclesCurrent>controllerList.get(i).actThresholdM*controllerList.get(i).targetVehicles &&
            controllerList.get(i).setPossibleSlaves.size()>0 &&
            controllerList.get(i+1).type.equals(status.NOT_USED)) {            
	
			updateTypeAndPreviousType(i,status.MASTER);
					}
	}
	
	public void dissolveMasterController(Integer masterControllerIndex){
		Integer i = masterControllerIndex;
		//DISSOLUTION OF COORDINATION STRING
		if( controllerList.get(i).type.equals(status.MASTER) &&
			(controllerList.get(i).queueCurrent/controllerList.get(i).queueMax < controllerList.get(i).deactThresholdQ ||
			controllerList.get(i).mainlineVehiclesCurrent<controllerList.get(i).deactThresholdM*controllerList.get(i).targetVehicles) )  {            
			
			updateTypeAndPreviousType(i,status.NOT_USED);
			
			for(Integer slaveControllerIndex: controllerList.get(i).setSlaves){
				updateTypeAndPreviousType(slaveControllerIndex,status.NOT_USED);
			}
		}	
	}
	
	public void calculateQueueSummationAndQueueSummationMax(Integer masterControllerIndex){
		Integer i =masterControllerIndex;
		//Calculates the sum of the current queues and max queues for the members of a coordination string
		double queueSummation=controllerList.get(i).queueCurrent;
		double queueSummationMax=controllerList.get(i).queueMax;
		for(Integer slaveControllerIndex: controllerList.get(i).setSlaves){
			queueSummation +=controllerList.get(slaveControllerIndex).queueCurrent;
			queueSummationMax += controllerList.get(slaveControllerIndex).queueMax;
		}
		controllerList.get(i).queueSum= queueSummation;
		controllerList.get(i).queueMaxSum=queueSummationMax;
	}
	
	public void defineCoordinationString(Integer masterControllerIndex){
		Integer i =masterControllerIndex;
		//DEFINITION OF COORDINATION STRING
		if( controllerList.get(i).type.equals(status.MASTER) &&
			controllerList.get(i).setSlaves.size() < controllerList.get(i).setPossibleSlaves.size() ){
			
			calculateQueueSummationAndQueueSummationMax(i);
			
			if (controllerList.get(i).queueSum/controllerList.get(i).queueMaxSum>controllerList.get(i).actThresholdQ){
				int newSlaveControllerIndex=controllerList.get(i).setPossibleSlaves.get(controllerList.get(i).setSlaves.size());	
				
				if (controllerList.get(newSlaveControllerIndex).type.equals(status.MASTER)){     
					controllerList.get(i).setSlaves.add(newSlaveControllerIndex);
					updateTypeAndPreviousType(newSlaveControllerIndex,status.SLAVE);
					
					for(Integer slaveControllerIndex: controllerList.get(newSlaveControllerIndex).setSlaves){
						if(controllerList.get(i).setPossibleSlaves.contains(slaveControllerIndex)){	
							controllerList.get(i).setSlaves.add(slaveControllerIndex);
							updateTypeAndPreviousType(slaveControllerIndex,status.SLAVE);
							}else{
							updateTypeAndPreviousType(slaveControllerIndex,status.NOT_USED);
						}	
					}
					
					controllerList.get(newSlaveControllerIndex).setSlaves.clear();
					
				} else if(controllerList.get(newSlaveControllerIndex).type.equals(status.NOT_USED)){
					//Master Controller[i] gets as slave Controller[newSlaveControllerIndex]
					controllerList.get(i).setSlaves.add(newSlaveControllerIndex);
					updateTypeAndPreviousType(newSlaveControllerIndex,status.SLAVE);
					

				}else if(controllerList.get(newSlaveControllerIndex).type.equals(status.SLAVE)){
					//System.out.println("time step "+timeStep+": THis HERO Controller is a SLAVE of a Master Controller that is not in setPossibleSlaves");
				}else{
					System.out.println("time step "+timeStep+": ERROR ");
				}	
			}	
		}
	}
	
	public void defineSlaveControllersMinimumQueue(Integer masterControllerIndex){
		Integer i =masterControllerIndex;
		//DEFINITION OF MINIMUM QUEUE
		if( controllerList.get(i).type.equals(status.MASTER)) {
			calculateQueueSummationAndQueueSummationMax(i);
			for(Integer slaveControllerIndex: controllerList.get(i).setSlaves){
				controllerList.get(slaveControllerIndex).queueMin= controllerList.get(slaveControllerIndex).queueMax*
						controllerList.get(i).queueSum/controllerList.get(i).queueMaxSum;
			}
		}	
	}

    public void updateTypeAndPreviousType(Integer controllerIndex, status newStatus){
    	Integer i =controllerIndex;
    	controllerList.get(i).typePrevious=controllerList.get(i).type;	//Update typePrevious
    	controllerList.get(i).type = newStatus;	//Update type	
    	
		System.out.println("time step "+timeStep+ ": " + controllerList.get(i).typePrevious + " Conroller "+i+
				" (id=" + controllerList.get(i).getId() +") was set to " +controllerList.get(i).type);
	}	
	
}

	
