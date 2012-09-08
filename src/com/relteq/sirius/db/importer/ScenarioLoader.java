package com.relteq.sirius.db.importer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Transaction;

import com.relteq.sirius.jaxb.Point;
import com.relteq.sirius.jaxb.Position;
import com.relteq.sirius.om.*;
import com.relteq.sirius.simulator.SiriusException;

/**
 * Imports a scenario
 */
public class ScenarioLoader {
	Connection conn = null;

	private Long project_id;
	/**
	 * @return the project id
	 */
	private Long getProjectId() {
		return project_id;
	}

	private VehicleTypes [] vehicle_type = null;
	private Map<String, Long> network_id = null;
	private Map<String, Nodes> nodes = null;
	private Map<String, Links> links = null;

	private Long getDBNodeId(String id) {
		return nodes.get(id).getNodeId();
	}
	private Long getDBLinkId(String id) {
		return links.get(id).getLinkId();
	}

	public ScenarioLoader() {
		project_id = Long.valueOf(0);
	}

	private static Logger logger = Logger.getLogger(ScenarioLoader.class);

	/**
	 * Loads a scenario from a file
	 * @param filename the configuration (scenario) file name
	 * @return the imported scenario
	 * @throws SiriusException
	 */
	public static Scenarios load(String filename) throws SiriusException {
		com.relteq.sirius.simulator.Scenario scenario =
				com.relteq.sirius.simulator.ObjectFactory.createAndLoadScenario(filename);
		if (null == scenario)
			throw new SiriusException("Could not load a scenario from file " + filename);
		logger.info("Configuration file '" + filename + "' parsed");
		Scenarios db_scenario = new ScenarioLoader().load(scenario);
		logger.info("Scenario imported, ID=" + db_scenario.getId());
		return db_scenario;
	}

	public Scenarios load(com.relteq.sirius.simulator.Scenario scenario) throws SiriusException {
		com.relteq.sirius.db.Service.ensureInit();
		try {
			conn = Transaction.begin();
			Scenarios db_scenario = save(scenario);
			Transaction.commit(conn);
			conn = null;
			return db_scenario;
		} catch (TorqueException exc) {
			throw new SiriusException(exc);
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
	 * @throws SiriusException
	 */
	private Scenarios save(com.relteq.sirius.simulator.Scenario scenario) throws TorqueException, SiriusException {
		if (null == scenario) return null;
		Scenarios db_scenario = new Scenarios();
		db_scenario.setProjectId(getProjectId());
		db_scenario.setName(scenario.getName());
		db_scenario.setDescription(scenario.getDescription());
		db_scenario.save(conn);
		db_scenario.setVehicleTypeSets(save(scenario.getSettings().getVehicleTypes()));
		save(scenario.getNetworkList(), db_scenario);
		db_scenario.setNetworkConnectionSets(save(scenario.getNetworkConnections()));
		db_scenario.setSignalSets(save(scenario.getSignalList()));
		db_scenario.setSensorSets(save(scenario.getSensorList()));
		db_scenario.setSplitRatioProfileSets(save(scenario.getSplitRatioProfileSet()));
		db_scenario.setWeavingFactorSets(save(scenario.getWeavingFactorSet()));
		db_scenario.setInitialDensitySets(save(scenario.getInitialDensitySet()));
		db_scenario.setFundamentalDiagramProfileSets(save(scenario.getFundamentalDiagramProfileSet()));
		db_scenario.setDemandProfileSets(save(scenario.getDemandProfileSet()));
		db_scenario.setDownstreamBoundaryCapacityProfileSets(save(scenario.getDownstreamBoundaryCapacityProfileSet()));
		db_scenario.setControllerSets(save(scenario.getControllerSet()));
		db_scenario.setEventSets(save(scenario.getEventSet()));
		db_scenario.save(conn);
		return db_scenario;
	}

	/**
	 * Imports vehicle types
	 * @param vtypes
	 * @return the imported vehicle type set
	 * @throws TorqueException
	 */
	private VehicleTypeSets save(com.relteq.sirius.jaxb.VehicleTypes vtypes) throws TorqueException {
		VehicleTypeSets db_vts = new VehicleTypeSets();
		db_vts.setProjectId(getProjectId());
		db_vts.save(conn);
		if (null == vtypes) {
			vtypes = new com.relteq.sirius.jaxb.VehicleTypes();
			com.relteq.sirius.jaxb.VehicleType vt = new com.relteq.sirius.jaxb.VehicleType();
			vt.setName("SOV");
			vt.setWeight(new BigDecimal(1));
			vtypes.getVehicleType().add(vt);
		}
		List<com.relteq.sirius.jaxb.VehicleType> vtlist = vtypes.getVehicleType();
		vehicle_type = new VehicleTypes[vtlist.size()];
		int ind = 0;
		for (com.relteq.sirius.jaxb.VehicleType vt : vtlist)
			vehicle_type[ind++] = save(vt, db_vts);
		return db_vts;
	}

	/**
	 * Imports a vehicle type
	 * @param vt the vehicle type to be imported
	 * @param db_vts an imported vehicle type set
	 * @return the imported (or already existing) vehicle type
	 * @throws TorqueException
	 */
	private VehicleTypes save(com.relteq.sirius.jaxb.VehicleType vt, VehicleTypeSets db_vts) throws TorqueException {
		Criteria crit = new Criteria();
		crit.add(VehicleTypesPeer.PROJECT_ID, getProjectId());
		crit.add(VehicleTypesPeer.NAME, vt.getName());
		crit.add(VehicleTypesPeer.WEIGHT, vt.getWeight());
		@SuppressWarnings("unchecked")
		List<VehicleTypes> db_vt_l = VehicleTypesPeer.doSelect(crit, conn);
		VehicleTypes db_vtype = null;
		if (db_vt_l.isEmpty()) {
			db_vtype = new VehicleTypes();
			db_vtype.setProjectId(getProjectId());
			db_vtype.setName(vt.getName());
			db_vtype.setWeight(vt.getWeight());
			db_vtype.setStandard(Boolean.FALSE);
			db_vtype.save(conn);
		} else {
			// TODO what if db_vt_l.size() > 1
			db_vtype = db_vt_l.get(0);
		}
		VehicleTypesInSets db_vtins = new VehicleTypesInSets();
		db_vtins.setVehicleTypeSets(db_vts);
		db_vtins.setVehicleTypes(db_vtype);
		db_vtins.save(conn);
		return db_vtype;
	}

	/**
	 * Imports a network list
	 * @param nl
	 * @throws TorqueException
	 * @throws SiriusException
	 */
	private void save(com.relteq.sirius.jaxb.NetworkList nl, Scenarios db_scenario) throws TorqueException, SiriusException {
		network_id = new HashMap<String, Long>(nl.getNetwork().size());
		nodes = new HashMap<String, Nodes>();
		links = new HashMap<String, Links>();
		for (com.relteq.sirius.jaxb.Network network : nl.getNetwork()) {
			NetworkSets db_ns = new NetworkSets();
			db_ns.setScenarios(db_scenario);
			db_ns.setNetworks(save(network));
			db_ns.save(conn);
		}
	}

	/**
	 * Imports a network
	 * @param network
	 * @return the imported network
	 * @throws TorqueException
	 * @throws SiriusException
	 */
	private Networks save(com.relteq.sirius.jaxb.Network network) throws TorqueException, SiriusException {
		Networks db_network = new Networks();
		db_network.setProjectId(getProjectId());
		db_network.setName(network.getName());
		db_network.setDescription(network.getDescription());
		java.util.Date now = java.util.Calendar.getInstance().getTime();
		db_network.setTimeCreated(now);
		db_network.setTimeModified(now);
		db_network.save(conn);
		network_id.put(network.getId(), Long.valueOf(db_network.getId()));
		for (com.relteq.sirius.jaxb.Node node : network.getNodeList().getNode()) {
			save(node, db_network);
		}
		for (com.relteq.sirius.jaxb.Link link : network.getLinkList().getLink()) {
			save(link, db_network);
		}
		return db_network;
	}

	/**
	 * Imports a node
	 * @param node
	 * @param db_network
	 * @throws TorqueException
	 * @throws SiriusException
	 */
	private void save(com.relteq.sirius.jaxb.Node node, Networks db_network) throws TorqueException, SiriusException {
		if (nodes.containsKey(node.getId())) throw new SiriusException("Node " + node.getId() + " already exists");
		NodeFamilies db_nf = new NodeFamilies();
		db_nf.setId(NodeFamiliesPeer.nextId(NodeFamiliesPeer.ID, conn));
		db_nf.save(conn);
		Nodes db_node = new Nodes();
		db_node.setNodeFamilies(db_nf);
		db_node.setNetworks(db_network);
		// TODO save node description, postmile, model
		// TODO node.getPosition() -> db_node.setGeometry();
		db_node.setGeometry("");
		db_node.setDetailLevel(1);
		if (null != node.getName()) {
			NodeName db_nname = new NodeName();
			db_nname.setName(node.getName());
			db_node.addNodeName(db_nname, conn);
		}
		if (null != node.getType()) {
			NodeType db_ntype = new NodeType();
			db_ntype.setType(node.getType());
			db_node.addNodeType(db_ntype, conn);
		}
		db_node.save(conn);
		nodes.put(node.getId(), db_node);
	}

	/**
	 * Imports a link
	 * @param link
	 * @param db_network
	 * @throws TorqueException
	 * @throws SiriusException
	 */
	private void save(com.relteq.sirius.jaxb.Link link, Networks db_network) throws TorqueException, SiriusException {
		if (links.containsKey(link.getId())) throw new SiriusException("Link " + link.getId() + " already exists");
		LinkFamilies db_lf = new LinkFamilies();
		db_lf.setId(LinkFamiliesPeer.nextId(LinkFamiliesPeer.ID, conn));
		db_lf.save(conn);
		Links db_link = new Links();
		db_link.setLinkFamilies(db_lf);
		db_link.setNetworks(db_network);
		db_link.setBeginNodeId(getDBNodeId(link.getBegin().getNodeId()));
		db_link.setEndNodeId(getDBNodeId(link.getEnd().getNodeId()));
		// TODO save link name, road name, description, type, model, display lane offset
		// TODO revise: shape -> geometry
		db_link.setGeometry(null == link.getShape() ? "" : link.getShape());
		db_link.setLength(link.getLength());
		db_link.setDetailLevel(1);
		if (null != link.getType()) {
			LinkType db_ltype = new LinkType();
			db_ltype.setType(link.getType());
			db_link.addLinkType(db_ltype, conn);
		}
		if (null != link.getLanes()) {
			LinkLanes db_llanes = new LinkLanes();
			db_llanes.setLanes(link.getLanes());
			db_link.addLinkLanes(db_llanes, conn);
		}
		db_link.save(conn);
		links.put(link.getId(), db_link);
	}

	/**
	 * Imports a signal list
	 * @param sl
	 * @return the imported signal set
	 * @throws TorqueException
	 */
	private SignalSets save(com.relteq.sirius.jaxb.SignalList sl) throws TorqueException {
		if (null == sl) return null;
		SignalSets db_ss = new SignalSets();
		db_ss.setProjectId(getProjectId());
		// TODO db_sl.setName();
		// TODO db_sl.setDescription();
		db_ss.save(conn);
		for (com.relteq.sirius.jaxb.Signal signal : sl.getSignal())
			save(signal, db_ss);
		return db_ss;
	}

	/**
	 * Imports a signal
	 * @param signal
	 * @param db_ss an imported signal set
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Signal signal, SignalSets db_ss) throws TorqueException {
		Signals db_signal = new Signals();
		db_signal.setNodeId(getDBNodeId(signal.getNodeId()));
		db_signal.setSignalSets(db_ss);
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
		db_phase.setNema(phase.getNema().intValue());
		db_phase.setIsProtected(phase.isProtected());
		db_phase.setPermissive(phase.isPermissive());
		db_phase.setLag(phase.isLag());
		db_phase.setRecall(phase.isRecall());
		db_phase.setMinGreenTime(phase.getMinGreenTime());
		db_phase.setYellowTime(phase.getYellowTime());
		db_phase.setRedClearTime(phase.getRedClearTime());
		db_phase.save(conn);
		for (com.relteq.sirius.jaxb.LinkReference lr : phase.getLinkReferences().getLinkReference())
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
		db_lr.setPhases(db_phase);
		db_lr.setLinkId(getDBLinkId(lr.getId()));
		db_lr.save(conn);
	}

	/**
	 * Imports a sensor list
	 * @param sl
	 * @param db_network
	 * @return the imported sensor set
	 * @throws TorqueException
	 */
	private SensorSets save(com.relteq.sirius.jaxb.SensorList sl) throws TorqueException {
		if (null == sl) return null;
		SensorSets db_ss = new SensorSets();
		db_ss.setProjectId(getProjectId());
		// TODO db_ss.setName();
		// TODO db_ss.setDescription();
		db_ss.save(conn);
		for (com.relteq.sirius.jaxb.Sensor sensor : sl.getSensor()) {
			save(sensor, db_ss);
		}
		return db_ss;
	}

	/**
	 * Imports a sensor
	 * @param sensor
	 * @param db_ss
	 */
	private void save(com.relteq.sirius.jaxb.Sensor sensor, SensorSets db_ss) {
		// TODO Auto-generated method stub
	}

	/**
	 * Imports initial densities
	 * @param idset
	 * @return the imported initial density set
	 * @throws TorqueException
	 */
	private InitialDensitySets save(com.relteq.sirius.jaxb.InitialDensitySet idset) throws TorqueException {
		if (null == idset) return null;
		InitialDensitySets db_idsets = new InitialDensitySets();
		db_idsets.setProjectId(getProjectId());
		db_idsets.setName(idset.getName());
		db_idsets.setDescription(idset.getDescription());
		// TODO if (null != idset.getVehicleTypeOrder()) ...;
		for (com.relteq.sirius.simulator.InitialDensitySet.Tuple tuple :
				((com.relteq.sirius.simulator.InitialDensitySet) idset).getData()) {
			InitialDensities db_density = new InitialDensities();
			db_density.setInitialDensitySets(db_idsets);
			db_density.setLinkId(getDBLinkId(tuple.getLinkId()));
			db_density.setVehicleTypes(vehicle_type[tuple.getVehicleTypeIndex()]);
			db_density.setDensity(new BigDecimal(tuple.getDensity()));
		}
		db_idsets.save(conn);
		return db_idsets;
	}

	/**
	 * Imports weaving factors
	 * @param wfset
	 * @return the imported weaving factor set
	 * @throws TorqueException
	 */
	private WeavingFactorSets save(com.relteq.sirius.jaxb.WeavingFactorSet wfset) throws TorqueException {
		if (null == wfset) return null;
		WeavingFactorSets db_wfset = new WeavingFactorSets();
		db_wfset.setProjectId(getProjectId());
		db_wfset.setName(wfset.getName());
		db_wfset.setDescription(wfset.getDescription());
		db_wfset.save(conn);
		// TODO if (null != wfset.getVehicleTypeOrder()) ...;
		for (com.relteq.sirius.jaxb.Weavingfactors wf : wfset.getWeavingfactors()) {
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
		Data1D data1d = new Data1D(wf.getContent(), ":");
		if (!data1d.isEmpty()) {
			BigDecimal[] data = data1d.getData();
			for (int i = 0; i < data.length; ++i) {
				WeavingFactors db_wf = new WeavingFactors();
				db_wf.setWeavingFactorSets(db_wfset);
				db_wf.setInLinkId(getDBLinkId(wf.getLinkIn()));
				db_wf.setOutLinkId(getDBLinkId(wf.getLinkOut()));
				db_wf.setVehicleTypes(vehicle_type[i]);
				db_wf.setFactor(data[i]);
				db_wf.save(conn);
			}
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
		db_srps.setProjectId(getProjectId());
		db_srps.setName(srps.getName());
		db_srps.setDescription(srps.getDescription());
		db_srps.save(conn);
		// TODO if (null != srps.getVehicleTypeOrder()) ...;
		for (com.relteq.sirius.jaxb.SplitratioProfile srp : srps.getSplitratioProfile())
			save(srp, db_srps);
		return db_srps;
	}

	/**
	 * Imports a split ratio profile
	 * @param srp
	 * @param db_srps an already imported split ratio profile set
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.SplitratioProfile srp, SplitRatioProfileSets db_srps) throws TorqueException {
		SplitRatioProfiles db_srp = new SplitRatioProfiles();
		db_srp.setSplitRatioProfileSets(db_srps);
		Nodes db_node = nodes.get(srp.getNodeId());
		db_srp.setNodeId(db_node.getNodeId());
		db_srp.setNetworkId(db_node.getNetworkId());
		// TODO db_srp.setDestinationLinkId();
		db_srp.setDt(srp.getDt());
		db_srp.setStartTime(srp.getStartTime());
		db_srp.save(conn);
		for (com.relteq.sirius.jaxb.Splitratio sr : srp.getSplitratio()) {
			BigDecimal[][] data = new Data2D(sr.getContent(), new String[] {",", ":"}).getData();
			if (null != data) {
				for (int t = 0; t < data.length; ++t) {
					for (int vtn = 0; vtn < data[t].length; ++vtn) {
						SplitRatios db_sr = new SplitRatios();
						// common
						db_sr.setSplitRatioProfiles(db_srp);
						db_sr.setInLinkId(getDBLinkId(sr.getLinkIn()));
						db_sr.setOutLinkId(getDBLinkId(sr.getLinkOut()));
						// unique
						db_sr.setVehicleTypes(vehicle_type[vtn]);
						db_sr.setOrdinal(Integer.valueOf(t));
						db_sr.setSplitRatio(data[t][vtn]);

						db_sr.save(conn);
					}
				}
			}
		}
	}

	/**
	 * Imports a fundamental diagram profile set
	 * @param fdps
	 * @return the imported FD profile set
	 * @throws TorqueException
	 */
	private FundamentalDiagramProfileSets save(com.relteq.sirius.jaxb.FundamentalDiagramProfileSet fdps) throws TorqueException {
		FundamentalDiagramProfileSets db_fdps = new FundamentalDiagramProfileSets();
		db_fdps.setProjectId(getProjectId());
		db_fdps.setName(fdps.getName());
		db_fdps.setDescription(fdps.getDescription());
		db_fdps.save(conn);
		for (com.relteq.sirius.jaxb.FundamentalDiagramProfile fdprofile : fdps.getFundamentalDiagramProfile())
			save(fdprofile, db_fdps);
		return db_fdps;
	}

	/**
	 * Imports a fundamental diagram profile
	 * @param fdprofile
	 * @param db_fdps an already imported FD profile set
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.FundamentalDiagramProfile fdprofile, FundamentalDiagramProfileSets db_fdps) throws TorqueException {
		FundamentalDiagramProfiles db_fdprofile = new FundamentalDiagramProfiles();
		db_fdprofile.setFundamentalDiagramProfileSets(db_fdps);
		db_fdprofile.setLinkId(getDBLinkId(fdprofile.getLinkId()));
		// TODO db_fdprofile.setNetworkId();
		db_fdprofile.setDt(fdprofile.getDt());
		db_fdprofile.setStartTime(fdprofile.getStartTime());
		db_fdprofile.save(conn);
		int num = 0;
		for (com.relteq.sirius.jaxb.FundamentalDiagram fd : fdprofile.getFundamentalDiagram())
			save(fd, db_fdprofile, num++);
	}

	/**
	 * Imports a fundamental diagram
	 * @param fd
	 * @param db_fdprofile an already imported FD profile
	 * @param number order of the FD
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.FundamentalDiagram fd, FundamentalDiagramProfiles db_fdprofile, int number) throws TorqueException {
		FundamentalDiagrams db_fd = new FundamentalDiagrams();
		db_fd.setFundamentalDiagramProfiles(db_fdprofile);
		db_fd.setNumber(number);
		db_fd.setFreeFlowSpeed(fd.getFreeFlowSpeed());
		// TODO: revise critical speed
		db_fd.setCriticalSpeed(fd.getFreeFlowSpeed());
		db_fd.setCongestionWaveSpeed(fd.getCongestionSpeed());
		db_fd.setCapacity(fd.getCapacity());
		db_fd.setJamDensity(fd.getJamDensity());
		db_fd.setCapacityDrop(fd.getCapacityDrop());
		db_fd.setCapacityStd(fd.getStdDevCapacity());
		db_fd.save(conn);
	}

	/**
	 * Imports a demand profile set
	 * @param dpset
	 * @return the imported demand profile set
	 * @throws TorqueException
	 */
	private DemandProfileSets save(com.relteq.sirius.jaxb.DemandProfileSet dpset) throws TorqueException {
		if (null == dpset) return null;
		DemandProfileSets db_dpset = new DemandProfileSets();
		db_dpset.setProjectId(getProjectId());
		db_dpset.setName(dpset.getName());
		db_dpset.setDescription(dpset.getDescription());
		db_dpset.save(conn);
		// TODO if (null != dpset.getVehicleTypeOrder()) ...;
		for (com.relteq.sirius.jaxb.DemandProfile dp : dpset.getDemandProfile())
			save(dp, db_dpset);
		return db_dpset;
	}

	/**
	 * Imports a demand profile
	 * @param dp a demand profile
	 * @param db_dpset an already imported demand profile set
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.DemandProfile dp, DemandProfileSets db_dpset) throws TorqueException {
		DemandProfiles db_dp = new DemandProfiles();
		db_dp.setDemandProfileSets(db_dpset);
		db_dp.setOriginLinkId(getDBLinkId(dp.getLinkIdOrigin()));
		// TODO db_dp.setNetworkId();
		// TODO db_dp.setDestinationLinkId();
		db_dp.setDt(dp.getDt());
		db_dp.setStartTime(dp.getStartTime());
		db_dp.setKnob(dp.getKnob());
		db_dp.setStdDeviationAdditive(dp.getStdDevAdd());
		db_dp.setStdDeviationMultiplicative(dp.getStdDevMult());
		db_dp.save(conn);
		Data2D data2d = new Data2D(dp.getContent(), new String[] {",", ":"});
		if (!data2d.isEmpty()) {
			BigDecimal[][] data = data2d.getData();
			for (int t = 0; t < data.length; ++t) {
				for (int vtn = 0; vtn < data[t].length; ++vtn) {
					Demands db_demand = new Demands();
					db_demand.setDemandProfiles(db_dp);
					db_demand.setVehicleTypes(vehicle_type[vtn]);
					db_demand.setNumber(t);
					db_demand.setDemand(data[t][vtn]);
					db_demand.save(conn);
				}
			}
		}
	}

	/**
	 * Imports a network connection list
	 * @param nconns
	 * @return the imported network connection set
	 * @throws TorqueException
	 */
	private NetworkConnectionSets save(com.relteq.sirius.jaxb.NetworkConnections nconns) throws TorqueException {
		if (null == nconns) return null;
		NetworkConnectionSets db_ncs = new NetworkConnectionSets();
		db_ncs.setProjectId(getProjectId());
		db_ncs.setName(nconns.getName());
		db_ncs.setDescription(nconns.getDescription());
		db_ncs.save(conn);
		for (com.relteq.sirius.jaxb.Networkpair np : nconns.getNetworkpair())
			save(np, db_ncs);
		return db_ncs;
	}

	/**
	 * Imports network connections
	 * @param np
	 * @param db_ncs an already imported network connection set
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Networkpair np, NetworkConnectionSets db_ncs) throws TorqueException {
		for (com.relteq.sirius.jaxb.Linkpair lp : np.getLinkpair()) {
			NetworkConnections db_nc = new NetworkConnections();
			db_nc.setNetworkConnectionSets(db_ncs);
			db_nc.setFromNetworkId(network_id.get(np.getNetworkA()));
			db_nc.setFromLinkId(getDBLinkId(lp.getLinkA()));
			db_nc.setToNetworkId(network_id.get(np.getNetworkB()));
			db_nc.setToLinkId(getDBLinkId(lp.getLinkB()));
			db_nc.save(conn);
		}
	}

	/**
	 * Imports downstream boundary capacity profiles
	 * @param dbcps
	 * @return the imported downstream boundary capacity profile set
	 * @throws TorqueException
	 */
	private DownstreamBoundaryCapacityProfileSets save(com.relteq.sirius.jaxb.DownstreamBoundaryCapacityProfileSet dbcps) throws TorqueException {
		if (null == dbcps) return null;
		DownstreamBoundaryCapacityProfileSets db_dbcps = new DownstreamBoundaryCapacityProfileSets();
		db_dbcps.setProjectId(getProjectId());
		db_dbcps.setName(dbcps.getName());
		db_dbcps.setDescription(dbcps.getDescription());
		db_dbcps.save(conn);
		for (com.relteq.sirius.jaxb.CapacityProfile cp : dbcps.getCapacityProfile())
			save(cp, db_dbcps);
		return db_dbcps;
	}

	/**
	 * Imports a downstream boundary capacity profile
	 * @param cp
	 * @param db_dbcps an already imported capacity profile set
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.CapacityProfile cp, DownstreamBoundaryCapacityProfileSets db_dbcps) throws TorqueException {
		DownstreamBoundaryCapacityProfiles db_dbcp = new DownstreamBoundaryCapacityProfiles();
		db_dbcp.setDownstreamBoundaryCapacityProfileSets(db_dbcps);
		db_dbcp.setLinkId(getDBLinkId(cp.getLinkId()));
		db_dbcp.setDt(cp.getDt());
		db_dbcp.setStartTime(cp.getStartTime());
		db_dbcp.save(conn);
		// TODO delimiter = ':' or ','?
		Data1D data1d = new Data1D(cp.getContent(), ":");
		if (!data1d.isEmpty()) {
			BigDecimal[] data = data1d.getData();
			for (int number = 0; number < data.length; ++number) {
				DownstreamBoundaryCapacities db_dbc = new DownstreamBoundaryCapacities();
				db_dbc.setDownstreamBoundaryCapacityProfiles(db_dbcp);
				db_dbc.setNumber(number);
				db_dbc.setDownstreamBoundaryCapacity(data[number]);
				db_dbc.save(conn);
			}
		}
	}

	private ControllerSets save(com.relteq.sirius.jaxb.ControllerSet cset) {
		// TODO Auto-generated method stub
		return null;
	}

	private EventSets save(com.relteq.sirius.jaxb.EventSet eset) {
		// TODO Auto-generated method stub
		return null;
	}

	protected static class Data1D {
		private BigDecimal[] data = null;

		/**
		 * @param str a serialized vector
		 * @param delim a delimiter
		 */
		public Data1D(String str, String delim) {
			if (null == str) logger.error("str == null");
			else if (null == delim) logger.error("delim == null");
			else {
				StringTokenizer st = new StringTokenizer(str, delim);
				data = new BigDecimal[st.countTokens()];
				for (int i = 0; st.hasMoreTokens(); ++i)
					data[i] = new BigDecimal(st.nextToken());
			}
		}

		public boolean isEmpty() {
			return null == data;
		}

		public BigDecimal[] getData() {
			return data;
		}
	}

	protected static class Data2D {
		private BigDecimal[][] data = null;

		/**
		 * @param str a serialized matrix
		 * @param delim delimiters, default: ',' for the 1st dimension, ':' for the second
		 */
		public Data2D(String str, String[] delim) {
			if (null == delim) delim = new String[] {",", ":"};
			if (null == str) logger.error("str == null");
			else if (2 != delim.length) logger.error("delim.length != 2");
			else {
				str = str.replaceAll("\\s", "");
				StringTokenizer st1 = new StringTokenizer(str, delim[0]);
				data = new BigDecimal[st1.countTokens()][];
				for (int i = 0; st1.hasMoreTokens(); ++i) {
					StringTokenizer st2 = new StringTokenizer(st1.nextToken(), delim[1]);
					data[i] = new BigDecimal[st2.countTokens()];
					for (int j = 0; st2.hasMoreTokens(); ++j)
						data[i][j] = new BigDecimal(st2.nextToken());
				}
			}
		}

		public boolean isEmpty() {
			return null == data;
		}

		public BigDecimal[][] getData() {
			return data;
		}
	}

}
