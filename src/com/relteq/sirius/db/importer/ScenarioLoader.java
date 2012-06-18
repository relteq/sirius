package com.relteq.sirius.db.importer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.torque.TorqueException;
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
import com.relteq.sirius.om.Scenarios;
import com.relteq.sirius.om.SplitRatioProfileSets;
import com.relteq.sirius.om.SplitRatioProfiles;
import com.relteq.sirius.om.SplitRatios;
import com.relteq.sirius.om.VehicleTypeFamilies;
import com.relteq.sirius.om.VehicleTypeLists;
import com.relteq.sirius.om.VehicleTypes;
import com.relteq.sirius.om.WeavingFactorSets;
import com.relteq.sirius.simulator.Double2DMatrix;
import com.relteq.sirius.simulator.InitialDensityProfile;
import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.SiriusException;

/**
 *
 */
public class ScenarioLoader {
	Connection conn = null;

	/**
	 * @return a universally unique random string identifier
	 */
	protected String uuid() {
		return UUID.randomUUID().toString();
	}

	private String scenario_id = null;
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

	protected void load(Scenario scenario) throws TorqueException {
		try {
			conn = Transaction.begin();
			save(scenario);
			Transaction.commit(conn);
			conn = null;
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
	protected void save(Scenario scenario) throws TorqueException {
		Scenarios db_scenario = new Scenarios();
		db_scenario.setId(scenario_id = uuid());
		db_scenario.setName(scenario.getName());
		db_scenario.setDescription(scenario.getDescription());
		db_scenario.save(conn);
		db_scenario.setVehicleTypeLists(save(scenario.getSettings().getVehicleTypes()));
		save(scenario.getNetworkList());
		db_scenario.setInitialDensitySets(save(scenario.getInitialDensityProfile()));
		db_scenario.setWeavingFactorSets(save(scenario.getWeavingFactorsProfile()));
		db_scenario.setSplitRatioProfileSets(save(scenario.getSplitRatioProfileSet()));
		db_scenario.save(conn);
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
		for (com.relteq.sirius.jaxb.VehicleType vt : vtlist) {
			VehicleTypeFamilies db_vtf = new VehicleTypeFamilies();
			db_vtf.setId(vehicle_type_id[ind++] = uuid());
			db_vtf.save(conn);
			VehicleTypes db_vtype = new VehicleTypes();
			db_vtype.setVehicleTypeFamilies(db_vtf);
			db_vtype.setVehicleTypeLists(db_vtl);
			db_vtype.setName(vt.getName());
			db_vtype.setWeight(vt.getWeight());
			db_vtype.save(conn);
		}
		return db_vtl;
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
		db_network.setName(network.getName());
		db_network.setDescription(network.getDescription());
		db_network.save(conn);
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
	 * Imports initial densities
	 * @param idprofile
	 * @return the imported initial density set
	 * @throws TorqueException
	 */
	protected InitialDensitySets save(com.relteq.sirius.jaxb.InitialDensityProfile idprofile) throws TorqueException {
		if (null == idprofile) return null;
		InitialDensitySets db_idsets = new InitialDensitySets();
		db_idsets.setId(uuid());
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
		return db_wfset;
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
