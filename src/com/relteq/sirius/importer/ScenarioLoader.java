package com.relteq.sirius.importer;

import java.math.BigDecimal;
import java.sql.Connection;
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
import com.relteq.sirius.om.VehicleTypeFamilies;
import com.relteq.sirius.om.VehicleTypeLists;
import com.relteq.sirius.om.VehicleTypes;
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

	protected void save(Scenario scenario) throws TorqueException {
		Scenarios db_scenario = new Scenarios();
		db_scenario.setId(scenario_id = uuid());
		db_scenario.setName(scenario.getName());
		db_scenario.setDescription(scenario.getDescription());
		db_scenario.save(conn);
		db_scenario.setVehicleTypeLists(save(scenario.getSettings().getVehicleTypes()));
		save(scenario.getNetworkList());
		db_scenario.setInitialDensitySets(save(scenario.getInitialDensityProfile()));
		db_scenario.save(conn);
	}

	protected VehicleTypeLists save(com.relteq.sirius.jaxb.VehicleTypes vtypes) throws TorqueException {
		if (null == vtypes) return null;
		VehicleTypeLists db_vtl = new VehicleTypeLists();
		db_vtl.setId(uuid());
		db_vtl.save(conn);
		List<com.relteq.sirius.jaxb.VehicleType> vtlist = vtypes.getVehicleType();
		vehicle_type_id = new String[vtlist.size()];
		int ind = 0;
		for (com.relteq.sirius.jaxb.VehicleType vt : vtypes.getVehicleType()) {
			VehicleTypeFamilies db_vtf = new VehicleTypeFamilies();
			db_vtf.setId(vehicle_type_id[ind++] = uuid());
			db_vtf.save(conn);
			VehicleTypes db_vtype = new VehicleTypes();
			db_vtype.setVehicleTypeFamilies(db_vtf);
			db_vtype.setVehicleTypeLists(db_vtl);
			db_vtype.setWeight(vt.getWeight());
			db_vtype.save(conn);
		}
		return db_vtl;
	}

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

	protected Networks save(com.relteq.sirius.jaxb.Network network) throws TorqueException {
		Networks db_network = new Networks();
		String id = uuid();
		network_id.put(network.getId(), id);
		db_network.setId(id);
		db_network.setName(network.getName());
		db_network.setDescription(network.getDescription());
		db_network.save(conn);
		for (com.relteq.sirius.jaxb.Link link : network.getLinkList().getLink()) {
			Links db_link = new Links();
			db_link.setId(getLinkFamily(link.getId()));
			db_link.setNetworks(db_network);
			db_link.setName(link.getName());
			db_link.setRoadName(link.getRoadName());
			db_link.setDescription(link.getDescription());
			db_link.setType(link.getType());
			if (null != link.getLinkGeometry()) db_link.setShape(link.getLinkGeometry().toString());
			db_link.setLanes(link.getLanes());
			db_link.setLength(link.getLength());
			db_link.setModel(link.getDynamics().getType());
			db_link.save(conn);
		}
		for (com.relteq.sirius.jaxb.Node node : network.getNodeList().getNode()) {
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
		return db_network;
	}

	protected InitialDensitySets save(com.relteq.sirius.jaxb.InitialDensityProfile idprofile) throws TorqueException {
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
}