package com.relteq.sirius.db.importer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Transaction;

import com.relteq.sirius.om.*;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.util.Data1D;
import com.relteq.sirius.util.Data2D;

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
	private Map<String, Controllers> controllers = null;
	private Map<String, Sensors> sensors = null;
	private Map<String, Events> events = null;
	private Map<String, Signals> signals = null;

	private Long getDBNodeId(String id) {
		return nodes.get(id).getId();
	}
	private Long getDBLinkId(String id) {
		return links.get(id).getId();
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
		com.relteq.sirius.jaxb.VehicleTypes vtypes = null;
		if (null != scenario.getSettings())
			vtypes = scenario.getSettings().getVehicleTypes();
		db_scenario.setVehicleTypeSets(save(vtypes));
		db_scenario.save(conn);
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
		// TODO db_scenario.setEnkfNoiseParameterSets();
		save(scenario.getDestinationNetworks(), db_scenario);
		save(scenario.getRoutes(), db_scenario);
		db_scenario.save(conn);

		if (null != scenario.getControllerSet())
			for (com.relteq.sirius.jaxb.Controller cntr : scenario.getControllerSet().getController()) {
				save(cntr.getTargetElements(), controllers.get(cntr.getId()));
				save(cntr.getFeedbackElements(), controllers.get(cntr.getId()));
			}
		if (null != scenario.getEventSet())
			for (com.relteq.sirius.jaxb.Event event : scenario.getEventSet().getEvent())
				save(event.getTargetElements(), events.get(event.getId()));

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
			db_vtype = db_vt_l.get(0);
			if (1 < db_vt_l.size())
				logger.warn("Found " + db_vt_l.size() + " vehicle types with name=" + vt.getName() + ", weight=" + vt.getWeight());
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
		db_network.setName(network.getName());
		db_network.setDescription(network.getDescription());
		db_network.setLocked(network.isLocked());
		db_network.save(conn);
		network_id.put(network.getId(), Long.valueOf(db_network.getId()));
		for (com.relteq.sirius.jaxb.Node node : network.getNodeList().getNode())
			save(node, db_network);
		for (com.relteq.sirius.jaxb.Link link : network.getLinkList().getLink())
			save(link, db_network);
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
		db_node.setGeom(pos2str(node.getPosition()));
		db_node.setGeom("");
		db_node.setInSynch(node.isInSynch());

		// node type
		NodeType db_ntype = new NodeType();
		db_ntype.setType(node.getType());
		db_node.addNodeType(db_ntype, conn);

		db_node.save(conn);
		nodes.put(node.getId(), db_node);

		save(node.getRoadwayMarkers(), db_node);
	}

	/**
	 * Imports roadway markers
	 * @param markers
	 * @param db_node an imported node
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.RoadwayMarkers markers, Nodes db_node) throws TorqueException {
		if (null == markers) return;
		for (com.relteq.sirius.jaxb.Marker marker : markers.getMarker())
			if (null == marker.getPostmile()) { // importing node name
				NodeName db_nname = new NodeName();
				db_nname.setNodes(db_node);
				db_nname.setName(marker.getName());
				db_nname.save(conn);
			} else { // importing highway postmile
				PostmileHighways db_pmhw = null;
				Criteria crit = new Criteria();
				crit.add(PostmileHighwaysPeer.HIGHWAY_NAME, marker.getName());
				@SuppressWarnings("unchecked")
				List<PostmileHighways> db_pmhw_l = PostmileHighwaysPeer.doSelect(crit);
				if (!db_pmhw_l.isEmpty()) {
					db_pmhw = db_pmhw_l.get(0);
					if (1 < db_pmhw_l.size())
						logger.warn("There are " + db_pmhw_l.size() + " hoghways with name=" + marker.getName());
				} else {
					db_pmhw = new PostmileHighways();
					db_pmhw.setHighwayName(marker.getName());
					db_pmhw.save(conn);
				}
				Postmiles db_postmile = new Postmiles();
				db_postmile.setNodes(db_node);
				db_postmile.setPostmileHighways(db_pmhw);
				db_postmile.setPostmile(marker.getPostmile());
				db_postmile.save(conn);
			}
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
		db_link.setBegNodeId(getDBNodeId(link.getBegin().getNodeId()));
		db_link.setEndNodeId(getDBNodeId(link.getEnd().getNodeId()));
		db_link.setGeom(null == link.getShape() ? "" : link.getShape()); // TODO revise: shape -> geometry
		db_link.setLength(link.getLength());
		db_link.setDetailLevel(1);
		db_link.setInSynch(link.isInSynch());

		// link type
		LinkType db_ltype = new LinkType();
		db_ltype.setType(link.getType());
		db_link.addLinkType(db_ltype, conn);

		// link lanes
		LinkLanes db_llanes = new LinkLanes();
		db_llanes.setLanes(link.getLanes());
		db_link.addLinkLanes(db_llanes, conn);

		// link lane offset
		if (null != link.getLaneOffset()) {
			LinkLaneOffset db_lloffset = new LinkLaneOffset();
			db_lloffset.setDisplayLaneOffset(link.getLaneOffset());
			db_link.addLinkLaneOffset(db_lloffset, conn);
		}

		db_link.save(conn);
		links.put(link.getId(), db_link);

		save(link.getRoads(), db_link);
	}

	/**
	 * Imports link roads
	 * @param roads
	 * @param db_link an imported link
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Roads roads, Links db_link) throws TorqueException {
		if (null == roads) return;
		for (com.relteq.sirius.jaxb.Road road : roads.getRoad()) {
			LinkName db_lname = new LinkName();
			db_lname.setLinks(db_link);
			db_lname.setName(road.getName());
			db_lname.save(conn);
		}
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
		db_ss.setName(sl.getName());
		db_ss.setDescription(sl.getDescription());
		db_ss.save(conn);
		signals = new HashMap<String, Signals>(sl.getSignal().size());
		for (com.relteq.sirius.jaxb.Signal signal : sl.getSignal())
			signals.put(signal.getId(), save(signal, db_ss));
		return db_ss;
	}

	/**
	 * Imports a signal
	 * @param signal
	 * @param db_ss an imported signal set
	 * @return an imported signal
	 * @throws TorqueException
	 */
	private Signals save(com.relteq.sirius.jaxb.Signal signal, SignalSets db_ss) throws TorqueException {
		Signals db_signal = new Signals();
		db_signal.setNodeId(getDBNodeId(signal.getNodeId()));
		db_signal.setSignalSets(db_ss);
		db_signal.save(conn);
		for (com.relteq.sirius.jaxb.Phase phase : signal.getPhase())
			save(phase, db_signal);
		return db_signal;
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
		if (null != phase.getLinkReferences())
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
		sensors = new HashMap<String, Sensors>(sl.getSensor().size());
		for (com.relteq.sirius.jaxb.Sensor sensor : sl.getSensor())
			sensors.put(sensor.getId(), save(sensor, db_ss));
		return db_ss;
	}

	/**
	 * Imports a sensor
	 * @param sensor
	 * @param db_ss and imported sensor set
	 * @return an imported sensor
	 * @throws TorqueException
	 */
	private Sensors save(com.relteq.sirius.jaxb.Sensor sensor, SensorSets db_ss) throws TorqueException {
		Sensors db_sensor = new Sensors();
		db_sensor.setType(sensor.getType());
		db_sensor.setSensorSets(db_ss);
		db_sensor.setOriginalId(sensor.getOriginalId());
		DataSources db_ds = new DataSources();
		db_ds.setId(DataSourcesPeer.nextId(DataSourcesPeer.ID, conn));
		db_ds.save(conn);
		db_sensor.setDataSources(db_ds);
		db_sensor.setDisplayGeometry(pos2str(sensor.getDisplayPosition()));
		if (null != sensor.getLinkReference())
			db_sensor.setLinkId(getDBLinkId(sensor.getLinkReference().getId()));
		db_sensor.setLinkPosition(sensor.getLinkPosition());
		if (null != sensor.getLaneNumber())
			db_sensor.setLaneNumber(Integer.valueOf(sensor.getLaneNumber().intValue()));
		db_sensor.setHealthStatus(sensor.getHealthStatus());
		db_sensor.save(conn);
		save(sensor.getParameters(), db_sensor);
		save(sensor.getTable(), db_sensor);
		return db_sensor;
	}

	/**
	 * Builds a vehicle type list from vehicle type order
	 * @param order vehicle type order
	 * @return default vehicle type array if order is NULL
	 */
	private VehicleTypes[] reorderVehicleTypes(com.relteq.sirius.jaxb.VehicleTypeOrder order) {
		if (null == order) return vehicle_type;
		VehicleTypes[] reordered_vt = new VehicleTypes[order.getVehicleType().size()];
		int i = 0;
		for (com.relteq.sirius.jaxb.VehicleType vt : order.getVehicleType()) {
			reordered_vt[i] = null;
			for (VehicleTypes db_vt : vehicle_type)
				if (vt.getName().equals(db_vt.getName())) {
					reordered_vt[i] = db_vt;
					break;
				}
			++i;
		}
		return reordered_vt;
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
		db_idsets.save(conn);
		VehicleTypes[] db_vt = reorderVehicleTypes(idset.getVehicleTypeOrder());
		for (com.relteq.sirius.jaxb.Density density : idset.getDensity())
			save(density, db_idsets, db_vt);
		return db_idsets;
	}

	/**
	 * Imports an initial density
	 * @param density
	 * @param db_idsets an imported initial density set
	 * @param db_vt [possibly reordered] vehicle types
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Density density, InitialDensitySets db_idsets, VehicleTypes[] db_vt) throws TorqueException {
		Data1D data1d = new Data1D(density.getContent(), ":");
		if (!data1d.isEmpty()) {
			BigDecimal[] data = data1d.getData();
			if (data.length != db_vt.length)
				logger.warn("initial density [link id=" + density.getLinkId() + "]: data.length=" + data.length + " and vehicle_types.length=" + db_vt.length + " differ");
			for (int i = 0; i < data.length; ++i) {
				InitialDensities db_id = new InitialDensities();
				db_id.setInitialDensitySets(db_idsets);
				db_id.setLinkId(getDBLinkId(density.getLinkId()));
				db_id.setVehicleTypes(db_vt[i]);
				if (null != density.getLinkIdDestination())
					db_id.setDestinationLinkId(getDBLinkId(density.getLinkIdDestination()));
				db_id.setDensity(data[i]);
				db_id.save(conn);
			}
		}
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
		VehicleTypes[] db_vt = reorderVehicleTypes(wfset.getVehicleTypeOrder());
		for (com.relteq.sirius.jaxb.Weavingfactors wf : wfset.getWeavingfactors())
			save(wf, db_wfset, db_vt);
		return db_wfset;
	}

	/**
	 * Imports weaving factors
	 * @param wf weaving factors to be imported
	 * @param db_wfset an already imported weaving factor set
	 * @param db_vt [imported] vehicle type list
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Weavingfactors wf, WeavingFactorSets db_wfset, VehicleTypes[] db_vt) throws TorqueException {
		// TODO delimiter = ':' or ','?
		Data1D data1d = new Data1D(wf.getContent(), ":");
		if (!data1d.isEmpty()) {
			BigDecimal[] data = data1d.getData();
			for (int i = 0; i < data.length; ++i) {
				WeavingFactors db_wf = new WeavingFactors();
				db_wf.setWeavingFactorSets(db_wfset);
				db_wf.setInLinkId(getDBLinkId(wf.getLinkIn()));
				db_wf.setOutLinkId(getDBLinkId(wf.getLinkOut()));
				db_wf.setVehicleTypes(db_vt[i]);
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
		for (com.relteq.sirius.jaxb.SplitratioProfile srp : srps.getSplitratioProfile())
			save(srp, db_srps, reorderVehicleTypes(srps.getVehicleTypeOrder()));
		return db_srps;
	}

	/**
	 * Imports a split ratio profile
	 * @param srp
	 * @param db_srps an already imported split ratio profile set
	 * @param db_vt [imported] vehicle type list
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.SplitratioProfile srp, SplitRatioProfileSets db_srps, VehicleTypes[] db_vt) throws TorqueException {
		SplitRatioProfiles db_srp = new SplitRatioProfiles();
		db_srp.setSplitRatioProfileSets(db_srps);
		Nodes db_node = nodes.get(srp.getNodeId());
		db_srp.setNodeId(db_node.getId());
		db_srp.setNetworkId(db_node.getNetworkId());
		if (null != srp.getLinkIdDestination())
			db_srp.setDestinationLinkId(getDBLinkId(srp.getLinkIdDestination()));
		db_srp.setDt(srp.getDt());
		db_srp.setStartTime(srp.getStartTime());
		db_srp.save(conn);
		for (com.relteq.sirius.jaxb.Splitratio sr : srp.getSplitratio()) {
			BigDecimal[][] data = new Data2D(sr.getContent(), new String[] {",", ":"}).getData();
			if (null != data) {
				for (int t = 0; t < data.length; ++t) {
					if (data[t].length != db_vt.length)
						logger.warn("split ratio data: data[time=" + t + "].length=" + data[t].length + " and vehicle_types.length=" + db_vt.length + " differ");
					for (int vtn = 0; vtn < data[t].length; ++vtn) {
						SplitRatios db_sr = new SplitRatios();
						// common
						db_sr.setSplitRatioProfiles(db_srp);
						db_sr.setInLinkId(getDBLinkId(sr.getLinkIn()));
						db_sr.setOutLinkId(getDBLinkId(sr.getLinkOut()));
						// unique
						db_sr.setVehicleTypes(db_vt[vtn]);
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
		db_fd.setCriticalSpeed(fd.getCriticalSpeed());
		db_fd.setCongestionWaveSpeed(fd.getCongestionSpeed());
		db_fd.setCapacity(fd.getCapacity());
		db_fd.setJamDensity(fd.getJamDensity());
		db_fd.setCapacityDrop(fd.getCapacityDrop());
		db_fd.setCapacityStd(fd.getStdDevCapacity());
		db_fd.setFreeFlowSpeedStd(fd.getStdDevFreeFlowSpeed());
		db_fd.setCongestionWaveSpeedStd(fd.getStdDevCongestionSpeed());
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
		VehicleTypes[] db_vt = reorderVehicleTypes(dpset.getVehicleTypeOrder());
		for (com.relteq.sirius.jaxb.DemandProfile dp : dpset.getDemandProfile())
			save(dp, db_dpset, db_vt);
		return db_dpset;
	}

	/**
	 * Imports a demand profile
	 * @param dp a demand profile
	 * @param db_dpset an already imported demand profile set
	 * @param db_vt [imported] vehicle type list
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.DemandProfile dp, DemandProfileSets db_dpset, VehicleTypes[] db_vt) throws TorqueException {
		DemandProfiles db_dp = new DemandProfiles();
		db_dp.setDemandProfileSets(db_dpset);
		db_dp.setOriginLinkId(getDBLinkId(dp.getLinkIdOrigin()));
		if (null != dp.getDestinationLinkId())
			db_dp.setDestinationLinkId(getDBLinkId(dp.getDestinationLinkId()));
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
					db_demand.setVehicleTypes(db_vt[vtn]);
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
		Data1D data1d = new Data1D(cp.getContent(), ",");
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

	/**
	 * Imports a controller set
	 * @param cset
	 * @return an imported controller set
	 * @throws TorqueException
	 */
	private ControllerSets save(com.relteq.sirius.jaxb.ControllerSet cset) throws TorqueException {
		if (null == cset) return null;
		ControllerSets db_cset = new ControllerSets();
		db_cset.setProjectId(getProjectId());
		db_cset.setName(cset.getName());
		db_cset.setDescription(cset.getDescription());
		db_cset.save(conn);
		controllers = new HashMap<String, Controllers>(cset.getController().size());
		for (com.relteq.sirius.jaxb.Controller controller : cset.getController())
			controllers.put(controller.getId(), save(controller, db_cset));
		return db_cset;
	}

	/**
	 * Imports a controller
	 * @param cntr a controller
	 * @param db_cset an imported controller set
	 * @return an imported controller
	 * @throws TorqueException
	 */
	private Controllers save(com.relteq.sirius.jaxb.Controller cntr, ControllerSets db_cset) throws TorqueException {
		Controllers db_cntr = new Controllers();
		db_cntr.setControllerSets(db_cset);
		db_cntr.setType(cntr.getType());
		db_cntr.setJavaClass(cntr.getJavaClass());
		db_cntr.setName(cntr.getName());
		db_cntr.setDt(cntr.getDt());
		db_cntr.setQueueControllers(save(cntr.getQueueController()));
		db_cntr.setDisplayGeometry(pos2str(cntr.getDisplayPosition()));
		db_cntr.save(conn);
		save(cntr.getParameters(), db_cntr);
		save(cntr.getTable(), db_cntr);
		save(cntr.getActivationIntervals(), db_cntr);
		return db_cntr;
	}

	/**
	 * Imports a queue controller
	 * @param qc
	 * @return an imported queue controller
	 * @throws TorqueException
	 */
	private QueueControllers save(com.relteq.sirius.jaxb.QueueController qc) throws TorqueException {
		if (null == qc) return null;
		QueueControllers db_qc = new QueueControllers();
		db_qc.setType(qc.getType());
		db_qc.setJavaClass(qc.getJavaClass());
		db_qc.save(conn);
		save(qc.getParameters(), db_qc);
		return db_qc;
	}

	/**
	 * Imports controller activation intervals
	 * @param ais activation intervals
	 * @param db_cntr an imported controller
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.ActivationIntervals ais, Controllers db_cntr) throws TorqueException {
		if (null == ais) return;
		for (com.relteq.sirius.jaxb.Interval interval : ais.getInterval()) {
			ControllerActivationIntervals db_cai = new ControllerActivationIntervals();
			db_cai.setControllers(db_cntr);
			db_cai.setStartTime(interval.getStartTime());
			db_cai.setDuration(interval.getEndTime().subtract(interval.getStartTime()));
			db_cai.save(conn);
		}
	}

	/**
	 * Imports an event set
	 * @param eset
	 * @return an imported event set
	 * @throws TorqueException
	 */
	private EventSets save(com.relteq.sirius.jaxb.EventSet eset) throws TorqueException {
		if (null == eset) return null;
		EventSets db_eset = new EventSets();
		db_eset.setProjectId(getProjectId());
		db_eset.setName(eset.getName());
		db_eset.setDescription(eset.getDescription());
		db_eset.save(conn);

		events = new HashMap<String, Events>(eset.getEvent().size());
		for (com.relteq.sirius.jaxb.Event event : eset.getEvent())
			events.put(event.getId(), save(event, db_eset));

		return db_eset;
	}

	/**
	 * Imports an event
	 * @param event
	 * @param db_eset an imported event set
	 * @return an imported event
	 * @throws TorqueException
	 */
	private Events save(com.relteq.sirius.jaxb.Event event, EventSets db_eset) throws TorqueException {
		Events db_event = new Events();
		db_event.setEventSets(db_eset);
		db_event.setType(event.getType());
		db_event.setTstamp(event.getTstamp());
		db_event.setJavaClass(event.getJavaClass());
		db_event.setDescription(event.getDescription());
		db_event.setDisplayGeometry(pos2str(event.getDisplayPosition()));
		db_event.setEnabled(event.isEnabled());
		db_event.save(conn);
		save(event.getParameters(), db_event);
		save(event.getSplitratioEvent(), db_event);
		return db_event;
	}

	/**
	 * Imports a split ratio event
	 * @param srevent
	 * @param db_event an imported event
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.SplitratioEvent srevent, Events db_event) throws TorqueException {
		if (null == srevent) return;
		VehicleTypes[] db_vt = reorderVehicleTypes(srevent.getVehicleTypeOrder());
		for (com.relteq.sirius.jaxb.Splitratio sr : srevent.getSplitratio())
			save(sr, db_event, db_vt);
	}

	/**
	 * Imports a split ratio of a split ratio event
	 * @param sr a split ratio
	 * @param db_event an imported event
	 * @param db_vt vehicle type order
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Splitratio sr, Events db_event, VehicleTypes[] db_vt) throws TorqueException {
		Data1D data1d = new Data1D(sr.getContent(), ":");
		if (!data1d.isEmpty()) {
			BigDecimal[] data = data1d.getData();
			for (int i = 0; i < data.length; ++i) {
				EventSplitRatios db_esr = new EventSplitRatios();
				db_esr.setEvents(db_event);
				db_esr.setInLinkId(getDBLinkId(sr.getLinkIn()));
				db_esr.setOutLinkId(getDBLinkId(sr.getLinkOut()));
				db_esr.setVehicleTypes(db_vt[i]);
				db_esr.setSplitRatio(data[i]);
				db_esr.save(conn);
			}
		}
	}

	/**
	 * Imports destination networks
	 * @param destnets destination networks
	 * @param db_scenario an imported scenario
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.DestinationNetworks destnets, Scenarios db_scenario) throws TorqueException {
		if (null == destnets) return;
		for (com.relteq.sirius.jaxb.DestinationNetwork destnet : destnets.getDestinationNetwork()) {
			DestinationNetworkSets db_destnetset = new DestinationNetworkSets();
			db_destnetset.setScenarios(db_scenario);
			db_destnetset.setDestinationNetworks(save(destnet));
			db_destnetset.save(conn);
		}
	}

	/**
	 * Imports a destination network
	 * @param destnet a destination network
	 * @return an imported destination network
	 * @throws TorqueException
	 */
	private DestinationNetworks save(com.relteq.sirius.jaxb.DestinationNetwork destnet) throws TorqueException {
		DestinationNetworks db_destnet = new DestinationNetworks();
		db_destnet.setDestinationLinkId(getDBLinkId(destnet.getLinkIdDestination()));
		db_destnet.setProjectId(getProjectId());
		db_destnet.save(conn);
		for (com.relteq.sirius.jaxb.LinkReference linkref : destnet.getLinkReferences().getLinkReference())
			save(linkref, db_destnet);
		return db_destnet;
	}

	/**
	 * Imports destination network's link reference
	 * @param linkref a link reference
	 * @param db_destnet an imported destination network
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.LinkReference linkref, DestinationNetworks db_destnet) throws TorqueException {
		DestinationNetworkLinks db_dnl = new DestinationNetworkLinks();
		db_dnl.setLinkId(getDBLinkId(linkref.getId()));
		db_dnl.setDestinationNetworks(db_destnet);
		db_dnl.save(conn);
	}

	/**
	 * Imports routes
	 * @param routes
	 * @param db_scenario an imported scenario
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Routes routes, Scenarios db_scenario) throws TorqueException {
		if (null == routes) return;
		for (com.relteq.sirius.jaxb.Route route : routes.getRoute()) {
			RouteSets db_rs = new RouteSets();
			db_rs.setScenarios(db_scenario);
			db_rs.setRoutes(save(route));
			db_rs.save(conn);
		}
	}

	/**
	 * Imports a route
	 * @param route
	 * @return an imported route
	 * @throws TorqueException
	 */
	private Routes save(com.relteq.sirius.jaxb.Route route) throws TorqueException {
		Routes db_route = new Routes();
		db_route.setProjectId(getProjectId());
		db_route.setName(route.getName());
		int ordinal = 0;
		for (com.relteq.sirius.jaxb.LinkReference lr : route.getLinkReferences().getLinkReference()) {
			RouteLinks db_rl = new RouteLinks();
			db_rl.setLinkId(getDBLinkId(lr.getId()));
			db_rl.setOrdinal(Integer.valueOf(ordinal++));
			db_route.addRouteLinks(db_rl);
		}
		db_route.save(conn);
		return db_route;
	}

	/**
	 * Imports parameters
	 * @param params
	 * @param db_obj an imported parent element
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Parameters params, com.relteq.sirius.db.BaseObject db_obj) throws TorqueException {
		if (null == params) return;
		String element_type = db_obj.getElementType();
		for (com.relteq.sirius.jaxb.Parameter param : params.getParameter()) {
			Parameters db_param = new Parameters();
			db_param.setScenarioElementId(db_obj.getId());
			db_param.setScenarioElementType(element_type);
			db_param.setName(param.getName());
			db_param.setValue(param.getValue());
			db_param.save(conn);
		}
	}

	/**
	 * Imports a table
	 * @param table
	 * @param db_obj an imported parent element
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.Table table, com.relteq.sirius.db.BaseObject db_obj) throws TorqueException {
		if (null == table) return;
		Tables db_table = new Tables();
		db_table.setName(table.getName());
		db_table.setParentElementId(db_obj.getId());
		db_table.setParentElementType(db_obj.getElementType());
		db_table.save(conn);

		int colnum = 0;
		for (com.relteq.sirius.jaxb.ColumnName colname : table.getColumnNames().getColumnName()) {
			TabularDataKeys db_tdk = new TabularDataKeys();
			db_tdk.setTables(db_table);
			db_tdk.setColumnName(colname.getValue());
			db_tdk.setColumnNumber(Integer.valueOf(colnum++));
			db_tdk.setIsKey(colname.isKey());
			db_tdk.save(conn);
		}
		int rownum = 0;
		for (com.relteq.sirius.jaxb.Row row : table.getRow()) {
			java.util.Iterator<com.relteq.sirius.jaxb.ColumnName> citer = table.getColumnNames().getColumnName().iterator();
			for (String elem : row.getColumn()) {
				TabularData db_td = new TabularData();
				db_td.setTables(db_table);
				db_td.setColumnName(citer.next().getValue());
				db_td.setRowNumber(Integer.valueOf(rownum));
				db_td.setValue(elem);
				db_td.save(conn);
			}
			++rownum;
		}
	}

	/**
	 * Imports target elements
	 * @param elems
	 * @param db_obj an imported parent element
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.TargetElements elems, com.relteq.sirius.db.BaseObject db_obj) throws TorqueException {
		if (null == elems) return;
		for (com.relteq.sirius.jaxb.ScenarioElement elem : elems.getScenarioElement())
			save(elem, "target", db_obj);
	}

	/**
	 * Imports feedback elements
	 * @param elems
	 * @param db_obj an imported parent element
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.FeedbackElements elems, com.relteq.sirius.db.BaseObject db_obj) throws TorqueException {
		if (null == elems) return;
		for (com.relteq.sirius.jaxb.ScenarioElement elem : elems.getScenarioElement())
			save(elem, "feedback", db_obj);
	}

	/**
	 * Imports a referenced scenario element
	 * @param elem scenario element
	 * @param type "target" or "feedback"
	 * @param db_parent an imported parent element
	 * @throws TorqueException
	 */
	private void save(com.relteq.sirius.jaxb.ScenarioElement elem, String type, com.relteq.sirius.db.BaseObject db_parent) throws TorqueException {
		ReferencedScenarioElements db_elems = new ReferencedScenarioElements();
		db_elems.setParentElementId(db_parent.getId());
		db_elems.setParentElementType(db_parent.getElementType());
		db_elems.setType(type);
		db_elems.setUsage(elem.getUsage());
		com.relteq.sirius.db.BaseObject db_ref = null;
		if (elem.getType().equals("link")) {
			if (null != links) db_ref = links.get(elem.getId());
		} else if (elem.getType().equals("node")) {
			if (null != nodes) db_ref = nodes.get(elem.getId());
		} else if (elem.getType().equals("controller")) {
			if (null != controllers) db_ref = controllers.get(elem.getId());
		} else if (elem.getType().equals("sensor")) {
			if (null != sensors) db_ref = sensors.get(elem.getId());
		} else if (elem.getType().equals("event")) {
			if (null != events) db_ref = events.get(elem.getId());
		} else if (elem.getType().equals("signal")) {
			if (null != signals) db_ref = signals.get(elem.getId());
		} else
			logger.error("Reference to a " + elem.getType() + " is not implemented");
		if (null != db_ref) {
			db_elems.setScenarioElementId(db_ref.getId());
			db_elems.setScenarioElementType(db_ref.getElementType());
			db_elems.save(conn);
		} else
			logger.error("Object " + elem.getType() + " [id=" + elem.getId() + "] not found");
	}

	private static String pos2str(com.relteq.sirius.jaxb.Position position) {
		if (null == position) return null;
		// TODO method stub
		return "";
	}

}
