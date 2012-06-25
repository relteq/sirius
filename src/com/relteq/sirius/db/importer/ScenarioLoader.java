package com.relteq.sirius.db.importer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Transaction;

import com.relteq.sirius.jaxb.Point;
import com.relteq.sirius.jaxb.Position;
import com.relteq.sirius.om.InitialDensities;
import com.relteq.sirius.om.InitialDensitySets;
import com.relteq.sirius.om.LinkFamilies;
import com.relteq.sirius.om.Links;
import com.relteq.sirius.om.NetworkLists;
import com.relteq.sirius.om.Networks;
import com.relteq.sirius.om.NodeFamilies;
import com.relteq.sirius.om.Nodes;
import com.relteq.sirius.om.PhaseLinks;
import com.relteq.sirius.om.Phases;
import com.relteq.sirius.om.Scenarios;
import com.relteq.sirius.om.SensorLists;
import com.relteq.sirius.om.SignalLists;
import com.relteq.sirius.om.Signals;
import com.relteq.sirius.om.SplitRatioProfileSets;
import com.relteq.sirius.om.SplitRatioProfiles;
import com.relteq.sirius.om.SplitRatios;
import com.relteq.sirius.om.VehicleTypeFamilies;
import com.relteq.sirius.om.VehicleTypeLists;
import com.relteq.sirius.om.VehicleTypes;
import com.relteq.sirius.om.VehicleTypesInLists;
import com.relteq.sirius.om.VehicleTypesPeer;
import com.relteq.sirius.om.WeavingFactorSets;
import com.relteq.sirius.om.WeavingFactors;
import com.relteq.sirius.simulator.Double2DMatrix;
import com.relteq.sirius.simulator.InitialDensityProfile;
import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.SiriusException;

/**
 * Imports a scenario
 */
public class ScenarioLoader {
	Connection conn = null;

	/**
	 * @return a universally unique random string identifier
	 */
	protected String uuid() {
		return com.relteq.sirius.db.util.UUID.generate();
	}

	private String project_id;
	private String scenario_id = null;
	/**
	 * @return the project id
	 */
	private String getProjectId() {
		return project_id;
	}
	/**
	 * @return the generated scenario id
	 */
	private String getScenarioId() {
		return scenario_id;
	}
	private String [] vehicle_type_id = null;
	private Map<String, String> network_id = null;
	private Map<String, String> link_family_id = null;
	private Map<String, String> node_family_id = null;

	private ScenarioLoader() {
		project_id = "default";
	}

	/**
	 * Loads a scenario from a file
	 * @param filename the configuration (scenario) file name
	 * @throws SiriusException
	 */
	public static void load(String filename) throws SiriusException {
		ScenarioLoader sl = new ScenarioLoader();
		try {
			com.relteq.sirius.db.Service.init();
			sl.load(ObjectFactory.createAndLoadScenario(filename));
			com.relteq.sirius.db.Service.shutdown();
		} catch (TorqueException exc) {
			throw new SiriusException(exc.getMessage(), exc);
		}
	}

	public Scenarios load(Scenario scenario) throws TorqueException {
		try {
			conn = Transaction.begin();
			Scenarios db_scenario = save(scenario);
			Transaction.commit(conn);
			conn = null;
			return db_scenario;
		} finally {
			if (null != conn) {
				Transaction.safeRollback(conn);
				conn = null;
			}
		}
	}

	/**
	 * Imports a scenario
	 * @param scenario
	 * @throws TorqueException
	 */
	protected Scenarios save(Scenario scenario) throws TorqueException {
		Scenarios db_scenario = new Scenarios();
		db_scenario.setId(scenario_id = uuid());
		db_scenario.setProjectId(getProjectId());
		db_scenario.setName(scenario.getName());
		db_scenario.setDescription(scenario.getDescription());
		db_scenario.save(conn);
		db_scenario.setVehicleTypeLists(save(scenario.getSettings().getVehicleTypes()));
		save(scenario.getNetworkList());
		db_scenario.setInitialDensitySets(save(scenario.getInitialDensityProfile()));
		db_scenario.setWeavingFactorSets(save(scenario.getWeavingFactorsProfile()));
		db_scenario.setSplitRatioProfileSets(save(scenario.getSplitRatioProfileSet()));
		db_scenario.save(conn);
		return db_scenario;
	}

	/**
	 * Imports vehicle types
	 * @param vtypes
	 * @return the imported vehicle type list
	 * @throws TorqueException
	 */
	protected VehicleTypeLists save(com.relteq.sirius.jaxb.VehicleTypes vtypes) throws TorqueException {
		VehicleTypeLists db_vtl = new VehicleTypeLists();
		db_vtl.setId(uuid());
		db_vtl.setProjectId(getProjectId());
		db_vtl.save(conn);
		if (null == vtypes) {
			vtypes = new com.relteq.sirius.jaxb.VehicleTypes();
			com.relteq.sirius.jaxb.VehicleType vt = new com.relteq.sirius.jaxb.VehicleType();
			vt.setName("SOV");
			vt.setWeight(new BigDecimal(1));
			vtypes.getVehicleType().add(vt);
		}
		List<com.relteq.sirius.jaxb.VehicleType> vtlist = vtypes.getVehicleType();
		vehicle_type_id = new String[vtlist.size()];
		int ind = 0;
		for (com.relteq.sirius.jaxb.VehicleType vt : vtlist)
			vehicle_type_id[ind++] = save(vt, db_vtl).getId();
		return db_vtl;
	}

	/**
	 * Imports a vehicle type
	 * @param vt the vehicle type to be imported
	 * @param db_vtl an imported vehicle type list
	 * @return the imported (or already existing) vehicle type
	 * @throws TorqueException
	 */
	private VehicleTypes save(com.relteq.sirius.jaxb.VehicleType vt, VehicleTypeLists db_vtl) throws TorqueException {
		Criteria crit = new Criteria();
		crit.add(VehicleTypesPeer.PROJECT_ID, getProjectId());
		crit.add(VehicleTypesPeer.NAME, vt.getName());
		crit.add(VehicleTypesPeer.WEIGHT, vt.getWeight());
		@SuppressWarnings("unchecked")
		List<VehicleTypes> db_vt_l = VehicleTypesPeer.doSelect(crit);
		VehicleTypes db_vtype = null;
		if (db_vt_l.isEmpty()) {
			VehicleTypeFamilies db_vtf = new VehicleTypeFamilies();
			db_vtf.setId(uuid());
			db_vtf.save(conn);
			db_vtype = new VehicleTypes();
			db_vtype.setVehicleTypeFamilies(db_vtf);
			db_vtype.setProjectId(getProjectId());
			db_vtype.setName(vt.getName());
			db_vtype.setWeight(vt.getWeight());
			db_vtype.save(conn);
		} else {
			// TODO what if db_vt_l.size() > 1
			db_vtype = db_vt_l.get(0);
		}
		VehicleTypesInLists db_vtinl = new VehicleTypesInLists();
		db_vtinl.setVehicleTypeLists(db_vtl);
		db_vtinl.setVehicleTypeId(db_vtype.getId());
		db_vtinl.save(conn);
		return db_vtype;
	}

	/**
	 * Imports a network list
	 * @param nl
	 * @throws TorqueException
	 */
	protected void save(com.relteq.sirius.jaxb.NetworkList nl) throws TorqueException {
		network_id = new HashMap<String, String>(nl.getNetwork().size());
		link_family_id = new HashMap<String, String>();
		node_family_id = new HashMap<String, String>();
		for (com.relteq.sirius.jaxb.Network network : nl.getNetwork()) {
			NetworkLists db_nl = new NetworkLists();
			db_nl.setScenarioId(getScenarioId());
			db_nl.setNetworks(save(network));
			db_nl.save(conn);
		}
	}

	private String getLinkFamily(String id) throws TorqueException {
		if (!link_family_id.containsKey(id)) {
			LinkFamilies db_lf = new LinkFamilies();
			String lfid = uuid();
			db_lf.setId(lfid);
			db_lf.save(conn);
			link_family_id.put(id, lfid);
		}
		return link_family_id.get(id);
	}

	private String getNodeFamily(String id) throws TorqueException {
		if (!node_family_id.containsKey(id)) {
			NodeFamilies db_nf = new NodeFamilies();
			String nfid = uuid();
			db_nf.setId(nfid);
			db_nf.save(conn);
			node_family_id.put(id, nfid);
		}
		return node_family_id.get(id);
	}

	/**
	 * Imports a network
	 * @param network
	 * @return the imported network
	 * @throws TorqueException
	 */
	protected Networks save(com.relteq.sirius.jaxb.Network network) throws TorqueException {
		Networks db_network = new Networks();
		String id = uuid();
		network_id.put(network.getId(), id);
		db_network.setId(id);
		db_network.setProjectId(getProjectId());
		db_network.setName(network.getName());
		db_network.setDescription(network.getDescription());
		db_network.save(conn);
		for (com.relteq.sirius.jaxb.Node node : network.getNodeList().getNode()) {
			save(node, db_network);
		}
		for (com.relteq.sirius.jaxb.Link link : network.getLinkList().getLink()) {
			save(link, db_network);
		}
		save(network.getSignalList());
		save(network.getSensorList());
		return db_network;
	}

	/**
	 * Imports a node
	 * @param node
	 * @param db_network
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Node node, Networks db_network) throws TorqueException {
		Nodes db_node = new Nodes();
		db_node.setId(getNodeFamily(node.getId()));
		db_node.setNetworks(db_network);
		db_node.setName(node.getName());
		db_node.setDescription(node.getDescription());
		db_node.setType(node.getType());
		Position pos = node.getPosition();
		if (null != pos && 1 == pos.getPoint().size()) {
			Point point = pos.getPoint().get(0);
			db_node.setLatitude(point.getLat());
			db_node.setLongitude(point.getLng());
			db_node.setElevation(point.getElevation());
		}
		db_node.setPostmile(node.getPostmile());
		db_node.setModel("STANDARD");
		db_node.save(conn);
	}

	/**
	 * Imports a link
	 * @param link
	 * @param db_network
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Link link, Networks db_network) throws TorqueException {
		Links db_link = new Links();
		db_link.setId(getLinkFamily(link.getId()));
		db_link.setNetworks(db_network);
		db_link.setBeginNodeId(node_family_id.get(link.getBegin().getNodeId()));
		db_link.setEndNodeId(node_family_id.get(link.getEnd().getNodeId()));
		db_link.setName(link.getName());
		db_link.setRoadName(link.getRoadName());
		db_link.setDescription(link.getDescription());
		db_link.setType(link.getType());
		if (null != link.getLinkGeometry()) db_link.setShape(link.getLinkGeometry().toString());
		db_link.setLanes(link.getLanes());
		db_link.setLength(link.getLength());
		db_link.setModel(link.getDynamics().getType());
		db_link.setDisplayLaneOffset(link.getLaneOffset());
		db_link.save(conn);
	}

	/**
	 * Imports a signal list
	 * @param sl
	 * @return the imported signal list
	 * @throws TorqueException
	 */
	private SignalLists save(com.relteq.sirius.jaxb.SignalList sl) throws TorqueException {
		if (null == sl) return null;
		SignalLists db_sl = new SignalLists();
		db_sl.setId(uuid());
		db_sl.setProjectId(getProjectId());
		// TODO db_sl.setName();
		// TODO db_sl.setDescription();
		db_sl.save(conn);
		for (com.relteq.sirius.jaxb.Signal signal : sl.getSignal())
			save(signal, db_sl);
		return db_sl;
	}

	/**
	 * Imports a signal
	 * @param signal
	 * @param db_sl an imported signal list
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Signal signal, SignalLists db_sl) throws TorqueException {
		Signals db_signal = new Signals();
		db_signal.setId(uuid());
		db_signal.setNodeId(node_family_id.get(signal.getNodeId()));
		db_signal.setSignalLists(db_sl);
		db_signal.save(conn);
		for (com.relteq.sirius.jaxb.Phase phase : signal.getPhase()) {
			save(phase, db_signal);
		}
	}

	/**
	 * Imports a signal phase
	 * @param phase
	 * @param db_signal
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Phase phase, Signals db_signal) throws TorqueException {
		Phases db_phase = new Phases();
		db_phase.setSignals(db_signal);
		db_phase.setPhase(phase.getNema().intValue());
		db_phase.setIs_protected(phase.isProtected());
		db_phase.setPermissive(phase.isPermissive());
		db_phase.setLag(phase.isLag());
		db_phase.setRecall(phase.isRecall());
		db_phase.setMinGreenTime(phase.getMinGreenTime());
		db_phase.setYellowTime(phase.getYellowTime());
		db_phase.setRedClearTime(phase.getRedClearTime());
		db_phase.save(conn);
		for (com.relteq.sirius.jaxb.LinkReference lr : phase.getLinks().getLinkReference())
			save(lr, db_phase);
	}

	/**
	 * Imports a link reference (for a signal phase)
	 * @param lr the link reference
	 * @param db_phase the imported phase
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.LinkReference lr, Phases db_phase) throws TorqueException {
		PhaseLinks db_lr = new PhaseLinks();
		db_lr.setSignalId(db_phase.getSignalId());
		db_lr.setPhase(db_phase.getPhase());
		db_lr.setLinkId(link_family_id.get(lr.getId()));
		db_lr.save(conn);
	}

	/**
	 * Imports a sensor list
	 * @param sl
	 * @param db_network
	 * @return the imported sensor list
	 * @throws TorqueException
	 */
	private SensorLists save(com.relteq.sirius.jaxb.SensorList sl) throws TorqueException {
		if (null == sl) return null;
		SensorLists db_sl = new SensorLists();
		db_sl.setId(uuid());
		db_sl.save(conn);
		for (com.relteq.sirius.jaxb.Sensor sensor : sl.getSensor()) {
			save(sensor, db_sl);
		}
		return db_sl;
	}

	/**
	 * Imports a sensor
	 * @param sensor
	 * @param db_sl
	 */
	private void save(com.relteq.sirius.jaxb.Sensor sensor, SensorLists db_sl) {
		// TODO Auto-generated method stub
	}

	/**
	 * Imports initial densities
	 * @param idprofile
	 * @return the imported initial density set
	 * @throws TorqueException
	 */
	protected InitialDensitySets save(com.relteq.sirius.jaxb.InitialDensityProfile idprofile) throws TorqueException {
		if (null == idprofile) return null;
		InitialDensitySets db_idsets = new InitialDensitySets();
		db_idsets.setId(uuid());
		db_idsets.setProjectId(getProjectId());
		db_idsets.setName(idprofile.getName());
		db_idsets.setDescription(idprofile.getDescription());
		for (InitialDensityProfile.Tuple tuple : ((InitialDensityProfile) idprofile).getData()) {
			InitialDensities db_density = new InitialDensities();
			db_density.setInitialDensitySets(db_idsets);
			db_density.setLinkId(link_family_id.get(tuple.getLinkId()));
			db_density.setVehicleTypeId(vehicle_type_id[tuple.getVehicleTypeIndex()]);
			db_density.setDensity(new BigDecimal(tuple.getDensity()));
		}
		db_idsets.save(conn);
		return db_idsets;
	}

	/**
	 * Imports weaving factors
	 * @param wfprofile
	 * @return the imported weaving factor set
	 * @throws TorqueException
	 */
	protected WeavingFactorSets save(com.relteq.sirius.jaxb.WeavingFactorsProfile wfprofile) throws TorqueException {
		if (null == wfprofile) return null;
		WeavingFactorSets db_wfset = new WeavingFactorSets();
		db_wfset.setId(uuid());
		db_wfset.setName(wfprofile.getName());
		db_wfset.setDescription(wfprofile.getDescription());
		db_wfset.save(conn);
		// TODO import weaving factors
		for (com.relteq.sirius.jaxb.Weavingfactors wf : wfprofile.getWeavingfactors()) {
			save(wf, db_wfset);
		}
		return db_wfset;
	}

	/**
	 * Imports weaving factors
	 * @param wf weaving factors to be imported
	 * @param db_wfset an already imported weaving factor set
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Weavingfactors wf, WeavingFactorSets db_wfset) throws TorqueException {
		com.relteq.sirius.simulator.Double1DVector factor_vector = new com.relteq.sirius.simulator.Double1DVector(wf.getContent(), ":");
		if (factor_vector.isEmpty()) return;
		for (Double factor : factor_vector.getData()) {
			WeavingFactors db_wf = new WeavingFactors();
			db_wf.setWeavingFactorSets(db_wfset);
			// TODO db_wf.setInLinkId();
			// TODO db_wf.setOutLinkId();
			db_wf.setFactor(new BigDecimal(factor));
			// TODO db_wf.save(conn);
		}
	}

	/**
	 * Imports split ratio profiles
	 * @param srps
	 * @return the imported split ratio profile set
	 * @throws TorqueException
	 */
	private SplitRatioProfileSets save(com.relteq.sirius.jaxb.SplitRatioProfileSet srps) throws TorqueException {
		if (null == srps) return null;
		SplitRatioProfileSets db_srps = new SplitRatioProfileSets();
		db_srps.setId(uuid());
		db_srps.setProjectId(getProjectId());
		db_srps.setName(srps.getName());
		db_srps.setDescription(srps.getDescription());
		db_srps.save(conn);
		for (com.relteq.sirius.jaxb.SplitratioProfile srp : srps.getSplitratioProfile()) {
			SplitRatioProfiles db_srp = new SplitRatioProfiles();
			db_srp.setId(uuid());
			db_srp.setSplitRatioProfileSets(db_srps);
			db_srp.setNodeId(node_family_id.get(srp.getNodeId()));
			db_srp.setDt(srp.getDt());
			db_srp.setStartTime(srp.getStartTime());
			db_srp.save(conn);
			for (com.relteq.sirius.jaxb.Splitratio sr : srp.getSplitratio()) {
				Double2DMatrix data = new Double2DMatrix(sr.getContent());
				if (!data.isEmpty()) {
					for (int t = 0; t < data.getnTime(); ++t) {
						Time ts = new Time(t * 1000);
						for (int vtn = 0; vtn < data.getnVTypes(); ++vtn) {
							SplitRatios db_sr = new SplitRatios();
							db_sr.setSplitRatioProfiles(db_srp);
							db_sr.setInLinkId(link_family_id.get(sr.getLinkIn()));
							db_sr.setOutLinkId(link_family_id.get(sr.getLinkOut()));
							db_sr.setVehicleTypeId(vehicle_type_id[vtn]);
							db_sr.setTs(ts);
							db_sr.setSplitRatio(new BigDecimal(data.get(t, vtn)));
							db_sr.save(conn);
						}
					}
				}
			}
		}
		return db_srps;
	}
}
