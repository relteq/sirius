package com.relteq.sirius.db.exporter;
import java.util.Date;
import java.util.List;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

import com.relteq.sirius.om.InitialDensities;
import com.relteq.sirius.om.InitialDensitiesPeer;
import com.relteq.sirius.om.InitialDensitySets;
import com.relteq.sirius.om.Links;
import com.relteq.sirius.om.NetworkLists;
import com.relteq.sirius.om.Networks;
import com.relteq.sirius.om.Nodes;
import com.relteq.sirius.om.Scenarios;
import com.relteq.sirius.om.ScenariosPeer;
import com.relteq.sirius.om.SplitRatioProfileSets;
import com.relteq.sirius.om.SplitRatioProfiles;
import com.relteq.sirius.om.SplitRatios;
import com.relteq.sirius.om.SplitRatiosPeer;
import com.relteq.sirius.om.VehicleTypeLists;
import com.relteq.sirius.om.VehicleTypes;
import com.relteq.sirius.om.VehicleTypesInListsPeer;
import com.relteq.sirius.om.VehicleTypesPeer;
import com.relteq.sirius.om.WeavingFactorSets;
import com.relteq.sirius.om.WeavingFactors;
import com.relteq.sirius.om.WeavingFactorsPeer;
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
		try {
			Scenarios db_scenario = ScenariosPeer.retrieveByPK(id);
			com.relteq.sirius.simulator.Scenario scenario = (com.relteq.sirius.simulator.Scenario) factory.createScenario();
			scenario.setId(db_scenario.getId());
			scenario.setName(db_scenario.getName());
			scenario.setDescription(db_scenario.getDescription());
			scenario.setSettings(restoreSettings(db_scenario));
			scenario.setNetworkList(restoreNetworkList(db_scenario));
			scenario.setInitialDensityProfile(restoreInitialDensityProfile(db_scenario.getInitialDensitySets()));
			scenario.setWeavingFactorsProfile(restoreWeavingFactorsProfile(db_scenario.getWeavingFactorSets()));
			scenario.setSplitRatioProfileSet(restoreSplitRatioProfileSet(db_scenario.getSplitRatioProfileSets()));
			return scenario;
		} catch (TorqueException exc) {
			throw new SiriusException(exc.getMessage(), exc.getCause());
		}
	}

	private com.relteq.sirius.jaxb.Settings restoreSettings(Scenarios db_scenario) {
		com.relteq.sirius.jaxb.Settings settings = factory.createSettings();
		try {
			VehicleTypeLists db_vtlists = db_scenario.getVehicleTypeLists();
			if (null != db_vtlists) {
				com.relteq.sirius.jaxb.VehicleTypes vts = factory.createVehicleTypes();
				List<com.relteq.sirius.jaxb.VehicleType> vtl = vts.getVehicleType();
				Criteria crit = new Criteria();
				crit.addJoin(VehicleTypesInListsPeer.VEHICLE_TYPE_ID, VehicleTypesPeer.ID);
				crit.add(VehicleTypesInListsPeer.VEHICLE_TYPE_LIST_ID, db_vtlists.getId());
				crit.add(VehicleTypesPeer.PROJECT_ID, db_vtlists.getProjectId());
				crit.addAscendingOrderByColumn(VehicleTypesPeer.ID);
				@SuppressWarnings("unchecked")
				List<VehicleTypes> db_vt_l = VehicleTypesPeer.doSelect(crit);
				for (VehicleTypes db_vt : db_vt_l)
					vtl.add(restoreVehicleType(db_vt));
				settings.setVehicleTypes(vts);
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return settings;
	}

	private com.relteq.sirius.jaxb.VehicleType restoreVehicleType(VehicleTypes db_vt) {
		com.relteq.sirius.jaxb.VehicleType vt = factory.createVehicleType();
		vt.setName(db_vt.getName());
		vt.setWeight(db_vt.getWeight());
		return vt;
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
		try {
			@SuppressWarnings("unchecked")
			List<Nodes> db_nl = db_net.getNodess();
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
		try {
			@SuppressWarnings("unchecked")
			List<Links> db_ll = db_net.getLinkss();
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

	private com.relteq.sirius.jaxb.InitialDensityProfile restoreInitialDensityProfile(InitialDensitySets db_idset) throws TorqueException {
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
		com.relteq.sirius.jaxb.Density density = null;
		StringBuilder sb = new StringBuilder();
		for (InitialDensities db_id : db_idl) {
			if (null != density && !density.getLinkId().equals(db_id.getLinkId())) {
				density.setContent(sb.toString());
				idprofile.getDensity().add(density);
				density = null;
			}
			if (null == density) { // new link
				density = factory.createDensity();
				density.setLinkId(db_id.getLinkId());
				// TODO density.setNetworkId();
				sb.setLength(0);
			} else { // same link, different vehicle type
				sb.append(":");
			}
			sb.append(db_id.getDensity().toPlainString());
		}
		// last link
		if (null != density) {
			density.setContent(sb.toString());
			idprofile.getDensity().add(density);
		}
		return idprofile;
	}

	private com.relteq.sirius.jaxb.WeavingFactorsProfile restoreWeavingFactorsProfile(WeavingFactorSets db_wfset) throws TorqueException {
		if (null == db_wfset) return null;
		com.relteq.sirius.jaxb.WeavingFactorsProfile wfp = factory.createWeavingFactorsProfile();
		wfp.setId(db_wfset.getId());
		wfp.setName(db_wfset.getName());
		wfp.setDescription(db_wfset.getDescription());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(WeavingFactorsPeer.IN_LINK_ID);
		crit.addAscendingOrderByColumn(WeavingFactorsPeer.OUT_LINK_ID);
		crit.addAscendingOrderByColumn(WeavingFactorsPeer.VEHICLE_TYPE_ID);
		@SuppressWarnings("unchecked")
		List<WeavingFactors> db_wf_l = db_wfset.getWeavingFactorss();
		// TODO uncomment when the XSD schema is updated
		/*
		com.relteq.sirius.jaxb.Weavingfactors wf = null;
		StringBuilder sb = new StringBuilder();
		for (WeavingFactors db_wf : db_wf_l) {
			if (null != wf && !(wf.getLinkIn().equals(db_wf.getInLinkId()) && wf.getLinkOut().equals(db_wf.getOutLinkId()))) {
				wf.setContent(sb.toString());
				wfp.getWeavingfactors().add(wf);
				wf = null;
			}
			if (null == wf) { // new weaving factor
				wf = factory.createWeavingfactors();
				wf.setLinkIn(db_wf.getInLinkId());
				wf.setLinkOut(db_wf.getOutLinkId());
				// TODO wf.setNetworkId();
				sb.setLength(0);
			} else { // same weaving factor, different vehicle type
				sb.append(':');
			}
			sb.append(db_wf.getFactor().toPlainString());
		}
		if (null != wf) {
			wf.setContent(sb.toString());
			wfp.getWeavingfactors().add(wf);
		}
		*/
		return wfp;
	}

	private com.relteq.sirius.jaxb.SplitRatioProfileSet restoreSplitRatioProfileSet(SplitRatioProfileSets db_srps) throws TorqueException {
		if (null == db_srps) return null;
		com.relteq.sirius.jaxb.SplitRatioProfileSet srps = factory.createSplitRatioProfileSet();
		srps.setId(db_srps.getId());
		srps.setName(db_srps.getName());
		srps.setDescription(db_srps.getDescription());
		@SuppressWarnings("unchecked")
		List<SplitRatioProfiles> db_srp_l = db_srps.getSplitRatioProfiless();
		for (SplitRatioProfiles db_srp : db_srp_l)
			srps.getSplitratioProfile().add(restoreSplitRatioProfile(db_srp));
		return srps;
	}

	private com.relteq.sirius.jaxb.SplitratioProfile restoreSplitRatioProfile(SplitRatioProfiles db_srp) throws TorqueException {
		com.relteq.sirius.jaxb.SplitratioProfile srp = factory.createSplitratioProfile();
		srp.setNodeId(db_srp.getNodeId());
		srp.setDt(db_srp.getDt());
		srp.setStartTime(db_srp.getStartTime());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(SplitRatiosPeer.IN_LINK_ID);
		crit.addAscendingOrderByColumn(SplitRatiosPeer.OUT_LINK_ID);
		crit.addAscendingOrderByColumn(SplitRatiosPeer.TS);
		crit.addAscendingOrderByColumn(SplitRatiosPeer.VEHICLE_TYPE_ID);
		@SuppressWarnings("unchecked")
		List<SplitRatios> db_sr_l = db_srp.getSplitRatioss(crit);
		com.relteq.sirius.jaxb.Splitratio sr = null;
		Date ts = null;
		StringBuilder sb = new StringBuilder();
		for (SplitRatios db_sr : db_sr_l) {
			if (null != sr && !(sr.getLinkIn().equals(db_sr.getInLinkId()) && sr.getLinkOut().equals(db_sr.getOutLinkId()))) {
				sr.setContent(sb.toString());
				srp.getSplitratio().add(sr);
				sr = null;
			}
			if (null == sr) { // new split ratio
				sr = factory.createSplitratio();
				sr.setLinkIn(db_sr.getInLinkId());
				sr.setLinkOut(db_sr.getOutLinkId());
				sb.setLength(0);
			} else { // same split ratio, different time stamp (',') or vehicle type (':')
				sb.append(ts.equals(db_sr.getTs()) ? ':' : ',');
			}
			ts = db_sr.getTs();
			sb.append(db_sr.getSplitRatio().toPlainString());
		}
		if (null != sr) {
			sr.setContent(sb.toString());
			srp.getSplitratio().add(sr);
		}
		return srp;
	}

}
