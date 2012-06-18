package com.relteq.sirius.db.exporter;
import java.util.Iterator;
import java.util.List;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

import com.relteq.sirius.om.InitialDensities;
import com.relteq.sirius.om.InitialDensitiesPeer;
import com.relteq.sirius.om.InitialDensitySets;
import com.relteq.sirius.om.Links;
import com.relteq.sirius.om.LinksPeer;
import com.relteq.sirius.om.NetworkLists;
import com.relteq.sirius.om.Networks;
import com.relteq.sirius.om.Nodes;
import com.relteq.sirius.om.NodesPeer;
import com.relteq.sirius.om.Scenarios;
import com.relteq.sirius.om.ScenariosPeer;
import com.relteq.sirius.om.VehicleTypeLists;
import com.relteq.sirius.om.VehicleTypes;
import com.relteq.sirius.om.VehicleTypesPeer;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;

/**
 * Loads a scenario from the database
 */
public class ScenarioRestorer {
	/**
	 * Load a scenario from the database
	 * @param id a scenario id
	 * @return the scenario
	 * @throws SiriusException
	 */
	public static com.relteq.sirius.simulator.Scenario getScenario(String id) throws SiriusException {
		if (!com.relteq.sirius.db.Service.isInit()) com.relteq.sirius.db.Service.init();
		return new ScenarioRestorer().restore(id);
	}

	com.relteq.sirius.simulator.JaxbObjectFactory factory = null;

	private ScenarioRestorer() {
		factory = new com.relteq.sirius.simulator.JaxbObjectFactory();
	}

	private com.relteq.sirius.simulator.Scenario restore(String id) throws SiriusException {
		Criteria crit = new Criteria();
		crit.add(ScenariosPeer.ID, id);
		try {
			@SuppressWarnings("unchecked")
			List<Scenarios> db_scenarios = ScenariosPeer.doSelect(crit);
			if (0 == db_scenarios.size()) throw new SiriusException("Scenario '" + id + "' does not exist");
			else if (1 < db_scenarios.size()) throw new SiriusException(db_scenarios.size() + " scenarios matched");
			Scenarios db_scenario = db_scenarios.get(0);
			com.relteq.sirius.simulator.Scenario scenario = (com.relteq.sirius.simulator.Scenario) factory.createScenario();
			scenario.setId(db_scenario.getId());
			scenario.setName(db_scenario.getName());
			scenario.setDescription(db_scenario.getDescription());
			scenario.setSettings(restoreSettings(db_scenario));
			scenario.setNetworkList(restoreNetworkList(db_scenario));
			scenario.setInitialDensityProfile(restoreInitialDensityProfile(db_scenario));
			return scenario;
		} catch (TorqueException exc) {
			throw new SiriusException(exc.getMessage(), exc.getCause());
		}
	}

	private com.relteq.sirius.jaxb.Settings restoreSettings(Scenarios db_scenario) {
		com.relteq.sirius.jaxb.Settings settings = (com.relteq.sirius.jaxb.Settings) factory.createSettings();
		try {
			VehicleTypeLists db_vtlists = db_scenario.getVehicleTypeLists();
			if (null != db_vtlists) {
				com.relteq.sirius.jaxb.VehicleTypes vts = factory.createVehicleTypes();
				List<com.relteq.sirius.jaxb.VehicleType> vtl = vts.getVehicleType();
				Criteria crit = new Criteria();
				crit.addAscendingOrderByColumn(VehicleTypesPeer.VEHICLE_TYPE_LIST_ID);
				@SuppressWarnings("unchecked")
				List<VehicleTypes> db_vtl = db_vtlists.getVehicleTypess(crit);
				for (Iterator<VehicleTypes> iter = db_vtl.iterator(); iter.hasNext();) {
					VehicleTypes db_vt = iter.next();
					com.relteq.sirius.jaxb.VehicleType vt = factory.createVehicleType();
					vt.setName(db_vt.getName());
					vt.setWeight(db_vt.getWeight());
					vtl.add(vt);
				}
				settings.setVehicleTypes(vts);
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return settings;
	}

	private com.relteq.sirius.jaxb.NetworkList restoreNetworkList(Scenarios db_scenario) {
		try {
			@SuppressWarnings("unchecked")
			List<NetworkLists> db_netll = db_scenario.getNetworkListss();
			if (0 < db_netll.size()) {
				com.relteq.sirius.jaxb.NetworkList nets = factory.createNetworkList();
				for (NetworkLists db_netl : db_netll)
					nets.getNetwork().add(restoreNetwork(db_netl.getNetworks()));
				return nets;
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return null;
	}

	private com.relteq.sirius.jaxb.Network restoreNetwork(Networks db_net) {
		com.relteq.sirius.jaxb.Network net = factory.createNetwork();
		net.setId(db_net.getId());
		net.setDescription(db_net.getDescription());
		net.setNodeList(restoreNodeList(db_net));
		net.setLinkList(restoreLinkList(db_net));
		return net;
	}

	private com.relteq.sirius.jaxb.NodeList restoreNodeList(Networks db_net) {
		Criteria crit = new Criteria();
		crit.add(NodesPeer.NETWORK_ID, db_net.getId());
		try {
			@SuppressWarnings("unchecked")
			List<Nodes> db_nl = NodesPeer.doSelect(crit);
			if (0 < db_nl.size()) {
				com.relteq.sirius.jaxb.NodeList nl = factory.createNodeList();
				for (Nodes db_node : db_nl)
					nl.getNode().add(restoreNode(db_node));
				return nl;
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return null;
	}

	private com.relteq.sirius.jaxb.Node restoreNode(Nodes db_node) {
		com.relteq.sirius.jaxb.Node node = factory.createNode();
		node.setId(db_node.getId());
		node.setName(db_node.getName());
		node.setDescription(db_node.getDescription());
		node.setType(db_node.getType());
		com.relteq.sirius.jaxb.Point point = factory.createPoint();
		point.setElevation(db_node.getElevation());
		point.setLat(db_node.getLatitude());
		point.setLng(db_node.getLongitude());
		com.relteq.sirius.jaxb.Position pos = factory.createPosition();
		pos.getPoint().add(point);
		node.setPosition(pos);
		node.setPostmile(db_node.getPostmile());
		return node;
	}

	private com.relteq.sirius.jaxb.LinkList restoreLinkList(Networks db_net) {
		Criteria crit = new Criteria();
		crit.add(LinksPeer.NETWORK_ID, db_net.getId());
		try {
			@SuppressWarnings("unchecked")
			List<Links> db_ll = LinksPeer.doSelect(crit);
			if (0 < db_ll.size()) {
				com.relteq.sirius.jaxb.LinkList ll = factory.createLinkList();
				for (Links db_link : db_ll) {
					ll.getLink().add(restoreLink(db_link));
				}
				return ll;
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return null;
	}

	private com.relteq.sirius.jaxb.Link restoreLink(Links db_link) {
		com.relteq.sirius.jaxb.Link link = factory.createLink();
		link.setId(db_link.getId());
		link.setName(db_link.getName());
		link.setRoadName(db_link.getRoadName());
		link.setDescription(db_link.getDescription());
		link.setType(db_link.getType());
		link.setLanes(db_link.getLanes());
		link.setLength(db_link.getLength());
		com.relteq.sirius.jaxb.Dynamics dynamics = factory.createDynamics();
		dynamics.setType(db_link.getModel());
		link.setDynamics(dynamics);
		link.setLaneOffset(db_link.getDisplayLaneOffset());
		return link;
	}

	private com.relteq.sirius.jaxb.InitialDensityProfile restoreInitialDensityProfile(Scenarios db_scenario) throws TorqueException {
		InitialDensitySets db_idset = db_scenario.getInitialDensitySets();
		if (null == db_idset) return null;
		com.relteq.sirius.jaxb.InitialDensityProfile idprofile = factory.createInitialDensityProfile();
		idprofile.setId(db_idset.getId());
		idprofile.setName(db_idset.getName());
		idprofile.setDescription(db_idset.getDescription());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(InitialDensitiesPeer.LINK_ID);
		crit.addAscendingOrderByColumn(InitialDensitiesPeer.VEHICLE_TYPE_ID);
		@SuppressWarnings("unchecked")
		List<InitialDensities> db_idl = db_idset.getInitialDensitiess(crit);
		String link_id = null;
		StringBuilder sb = null;
		for (InitialDensities db_id : db_idl) {
			String density = db_id.getDensity().toPlainString();
			if (null == link_id) { // first iteration
				link_id = db_id.getLinkId();
				sb = new StringBuilder(density);
			} else if (link_id != db_id.getLinkId()) { // new link started
				idprofile.getDensity().add(createDensity(link_id, sb.toString()));
				link_id = db_id.getLinkId();
				sb.setLength(0);
				sb.append(density);
			} else { // same link, different vehicle type
				sb.append(":").append(density);
			}
		}
		// last link
		if (null != link_id) idprofile.getDensity().add(createDensity(link_id, sb.toString()));
		return idprofile;
	}

	private com.relteq.sirius.jaxb.Density createDensity(String link_id, String content) {
		com.relteq.sirius.jaxb.Density density = factory.createDensity();
		density.setLinkId(link_id);
		// TODO density.setNetworkId();
		density.setContent(content);
		return density;
	}

}
