package com.relteq.sirius.db.exporter;
import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.SchemaFactory;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;
import org.xml.sax.SAXException;

import com.relteq.sirius.om.DemandProfileSets;
import com.relteq.sirius.om.DemandProfiles;
import com.relteq.sirius.om.Demands;
import com.relteq.sirius.om.DemandsPeer;
import com.relteq.sirius.om.FundamentalDiagramProfileSets;
import com.relteq.sirius.om.FundamentalDiagramProfiles;
import com.relteq.sirius.om.FundamentalDiagrams;
import com.relteq.sirius.om.FundamentalDiagramsPeer;
import com.relteq.sirius.om.InitialDensities;
import com.relteq.sirius.om.InitialDensitiesPeer;
import com.relteq.sirius.om.InitialDensitySets;
import com.relteq.sirius.om.Links;
import com.relteq.sirius.om.LinksPeer;
import com.relteq.sirius.om.NetworkConnectionLists;
import com.relteq.sirius.om.NetworkConnections;
import com.relteq.sirius.om.NetworkConnectionsPeer;
import com.relteq.sirius.om.NetworkLists;
import com.relteq.sirius.om.Networks;
import com.relteq.sirius.om.Nodes;
import com.relteq.sirius.om.OdLists;
import com.relteq.sirius.om.Ods;
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
	public static void export(String id, String filename) throws SiriusException, JAXBException, SAXException {
		com.relteq.sirius.simulator.Scenario scenario = ScenarioRestorer.getScenario(id);
		scenario.setSchemaVersion(com.relteq.sirius.Version.get().getSchemaVersion());
		JAXBContext jaxbc = JAXBContext.newInstance("com.relteq.sirius.jaxb");
		Marshaller mrsh = jaxbc.createMarshaller();
		SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		mrsh.setSchema(sf.newSchema(ScenarioRestorer.class.getClassLoader().getResource("sirius.xsd")));
		mrsh.marshal(scenario, new File(filename));
	}

	/**
	 * Load a scenario from the database
	 * @param id a scenario id
	 * @return the scenario
	 * @throws SiriusException
	 */
	public static com.relteq.sirius.simulator.Scenario getScenario(String id) throws SiriusException {
		if (!com.relteq.sirius.db.Service.isInit()) com.relteq.sirius.db.Service.init();
		com.relteq.sirius.simulator.Scenario scenario = com.relteq.sirius.simulator.ObjectFactory.process(new ScenarioRestorer().restore(id));
		if (null == scenario) {
			if (SiriusErrorLog.haserror()) {
				SiriusErrorLog.printErrorMessage();
				SiriusErrorLog.clearErrorMessage();
			}
			throw new SiriusException("Could not load the scenario");
		}
		return scenario;
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
			scenario.setNetworkConnections(restoreNetworkConnections(db_scenario.getNetworkConnectionLists()));
			scenario.setODList(restoreODList(db_scenario.getOdLists()));
			scenario.setInitialDensitySet(restoreInitialDensitySet(db_scenario.getInitialDensitySets()));
			scenario.setWeavingFactorSet(restoreWeavingFactorSet(db_scenario.getWeavingFactorSets()));
			scenario.setSplitRatioProfileSet(restoreSplitRatioProfileSet(db_scenario.getSplitRatioProfileSets()));
			scenario.setFundamentalDiagramProfileSet(restoreFundamentalDiagramProfileSet(db_scenario.getFundamentalDiagramProfileSets()));
			scenario.setDemandProfileSet(restoreDemandProfileSet(db_scenario.getDemandProfileSets()));
			return scenario;
		} catch (TorqueException exc) {
			throw new SiriusException(exc.getMessage(), exc.getCause());
		}
	}

	private com.relteq.sirius.jaxb.Settings restoreSettings(Scenarios db_scenario) {
		com.relteq.sirius.jaxb.Settings settings = factory.createSettings();
		settings.setUnits("US");
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
		net.setName(db_net.getName());
		net.setDescription(db_net.getDescription());
		net.setDt(new BigDecimal(1)); // TODO change this when the DB schema is updated
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
		node.setInputs(restoreInputs(db_node));
		node.setOutputs(restoreOutputs(db_node));
		return node;
	}

	private com.relteq.sirius.jaxb.Inputs restoreInputs(Nodes db_node) {
		com.relteq.sirius.jaxb.Inputs inputs = factory.createInputs();
		Criteria crit = new Criteria();
		crit.add(LinksPeer.NETWORK_ID, db_node.getNetworkId());
		crit.add(LinksPeer.END_NODE_ID, db_node.getId());
		try {
			@SuppressWarnings("unchecked")
			List<Links> db_link_l = LinksPeer.doSelect(crit);
			for (Links db_link : db_link_l)
				inputs.getInput().add(restoreInput(db_link));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return inputs;
	}

	private com.relteq.sirius.jaxb.Input restoreInput(Links db_link) {
		com.relteq.sirius.jaxb.Input input = factory.createInput();
		input.setLinkId(db_link.getId());
		return input;
	}

	private com.relteq.sirius.jaxb.Outputs restoreOutputs(Nodes db_node) {
		com.relteq.sirius.jaxb.Outputs outputs = factory.createOutputs();
		Criteria crit = new Criteria();
		crit.add(LinksPeer.NETWORK_ID, db_node.getNetworkId());
		crit.add(LinksPeer.BEGIN_NODE_ID, db_node.getId());
		try {
			@SuppressWarnings("unchecked")
			List<Links> db_link_l = LinksPeer.doSelect(crit);
			for (Links db_link : db_link_l)
				outputs.getOutput().add(restoreOutput(db_link));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return outputs;
	}

	private com.relteq.sirius.jaxb.Output restoreOutput(Links db_link) {
		com.relteq.sirius.jaxb.Output output = factory.createOutput();
		output.setLinkId(db_link.getId());
		return output;
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
		if (null != db_link.getBeginNodeId()) {
			com.relteq.sirius.jaxb.Begin begin = factory.createBegin();
			begin.setNodeId(db_link.getBeginNodeId());
			link.setBegin(begin);
		}
		if (null != db_link.getEndNodeId()) {
			com.relteq.sirius.jaxb.End end = factory.createEnd();
			end.setNodeId(db_link.getEndNodeId());
			link.setEnd(end);
		}
		return link;
	}

	private com.relteq.sirius.jaxb.InitialDensitySet restoreInitialDensitySet(InitialDensitySets db_idset) throws TorqueException {
		if (null == db_idset) return null;
		com.relteq.sirius.jaxb.InitialDensitySet idset = factory.createInitialDensitySet();
		idset.setId(db_idset.getId());
		idset.setName(db_idset.getName());
		idset.setDescription(db_idset.getDescription());
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
				idset.getDensity().add(density);
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
			idset.getDensity().add(density);
		}
		return idset;
	}

	private com.relteq.sirius.jaxb.WeavingFactorSet restoreWeavingFactorSet(WeavingFactorSets db_wfset) throws TorqueException {
		if (null == db_wfset) return null;
		com.relteq.sirius.jaxb.WeavingFactorSet wfset = factory.createWeavingFactorSet();
		wfset.setId(db_wfset.getId());
		wfset.setName(db_wfset.getName());
		wfset.setDescription(db_wfset.getDescription());
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
		return wfset;
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

	com.relteq.sirius.jaxb.FundamentalDiagramProfileSet restoreFundamentalDiagramProfileSet(FundamentalDiagramProfileSets db_fdps) {
		if (null == db_fdps) return null;
		com.relteq.sirius.jaxb.FundamentalDiagramProfileSet fdps = factory.createFundamentalDiagramProfileSet();
		fdps.setId(db_fdps.getId());
		fdps.setName(db_fdps.getName());
		fdps.setDescription(db_fdps.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<FundamentalDiagramProfiles> db_fdprofile_l = db_fdps.getFundamentalDiagramProfiless();
			for (FundamentalDiagramProfiles db_fdprofile : db_fdprofile_l)
				fdps.getFundamentalDiagramProfile().add(restoreFundamentalDiagramProfile(db_fdprofile));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return fdps;
	}

	com.relteq.sirius.jaxb.FundamentalDiagramProfile restoreFundamentalDiagramProfile(FundamentalDiagramProfiles db_fdprofile) throws TorqueException {
		com.relteq.sirius.jaxb.FundamentalDiagramProfile fdprofile = factory.createFundamentalDiagramProfile();
		fdprofile.setLinkId(db_fdprofile.getLinkId());
		fdprofile.setDt(db_fdprofile.getDt());
		fdprofile.setStartTime(db_fdprofile.getStartTime());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(FundamentalDiagramsPeer.TS);
		@SuppressWarnings("unchecked")
		List<FundamentalDiagrams> db_fd_l = db_fdprofile.getFundamentalDiagramss(crit);
		for (FundamentalDiagrams db_fd : db_fd_l)
			fdprofile.getFundamentalDiagram().add(restoreFundamentalDiagram(db_fd));
		return fdprofile;
	}

	com.relteq.sirius.jaxb.FundamentalDiagram restoreFundamentalDiagram(FundamentalDiagrams db_fd) {
		com.relteq.sirius.jaxb.FundamentalDiagram fd = factory.createFundamentalDiagram();
		fd.setFreeFlowSpeed(db_fd.getFreeFlowSpeed());
		fd.setCongestionSpeed(db_fd.getCongestionWaveSpeed());
		fd.setCapacity(db_fd.getCapacity());
		fd.setJamDensity(db_fd.getJamDensity());
		fd.setCapacityDrop(db_fd.getCapacityDrop());
		fd.setStdDevCapacity(db_fd.getStdDeviationCapacity());
		return fd;
	}

	private com.relteq.sirius.jaxb.DemandProfileSet restoreDemandProfileSet(DemandProfileSets db_dpset) throws TorqueException {
		if (null == db_dpset) return null;
		com.relteq.sirius.jaxb.DemandProfileSet dpset = factory.createDemandProfileSet();
		dpset.setId(db_dpset.getId());
		dpset.setName(db_dpset.getName());
		dpset.setDescription(db_dpset.getDescription());
		@SuppressWarnings("unchecked")
		List<DemandProfiles> db_dp_l = db_dpset.getDemandProfiless();
		for (DemandProfiles db_dp : db_dp_l)
			dpset.getDemandProfile().add(restoreDemandProfile(db_dp));
		return dpset;
	}

	private com.relteq.sirius.jaxb.DemandProfile restoreDemandProfile(DemandProfiles db_dp) throws TorqueException {
		com.relteq.sirius.jaxb.DemandProfile dp = factory.createDemandProfile();
		dp.setLinkIdOrigin(db_dp.getLinkId());
		dp.setDt(db_dp.getDt());
		dp.setStartTime(db_dp.getStartTime());
		dp.setKnob(db_dp.getKnob());
		dp.setStdDevAdd(db_dp.getStdDeviationAdditive());
		dp.setStdDevMult(db_dp.getStdDeviationMultiplicative());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(DemandsPeer.TS);
		crit.addAscendingOrderByColumn(DemandsPeer.VEHICLE_TYPE_ID);
		@SuppressWarnings("unchecked")
		List<Demands> db_demand_l = db_dp.getDemandss(crit);
		if (0 < db_demand_l.size()) {
			StringBuilder sb = new StringBuilder();
			Date ts = null;
			for (Demands db_demand : db_demand_l) {
				if (null != ts) sb.append(ts.equals(db_demand.getTs()) ? ':' : ',');
				ts = db_demand.getTs();
				sb.append(db_demand.getDemand().toPlainString());
			}
		}
		return dp;
	}

	private com.relteq.sirius.jaxb.NetworkConnections restoreNetworkConnections(NetworkConnectionLists db_ncl) {
		if (null == db_ncl) return null;
		com.relteq.sirius.jaxb.NetworkConnections nc = factory.createNetworkConnections();
		nc.setId(db_ncl.getId());
		nc.setName(db_ncl.getName());
		nc.setDescription(db_ncl.getDescription());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(NetworkConnectionsPeer.FROM_NETWORK_ID);
		crit.addAscendingOrderByColumn(NetworkConnectionsPeer.TO_NETWORK_ID);
		try {
			@SuppressWarnings("unchecked")
			List<NetworkConnections> db_nc_l = db_ncl.getNetworkConnectionss(crit);
			com.relteq.sirius.jaxb.Networkpair np = null;
			for (NetworkConnections db_nc : db_nc_l) {
				if (null != np && (!np.getNetworkA().equals(db_nc.getFromNetworkId()) || !np.getNetworkB().equals(db_nc.getToNetworkId()))) {
					nc.getNetworkpair().add(np);
					np = null;
				}
				if (null == np) {
					np = factory.createNetworkpair();
					np.setNetworkA(db_nc.getFromNetworkId());
					np.setNetworkB(db_nc.getToNetworkId());
				}
				com.relteq.sirius.jaxb.Linkpair lp = factory.createLinkpair();
				lp.setLinkA(db_nc.getFromLinkId());
				lp.setLinkB(db_nc.getToLinkId());
				np.getLinkpair().add(lp);
			}
			if (null != np) nc.getNetworkpair().add(np);
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return nc;
	}

	private com.relteq.sirius.jaxb.ODList restoreODList(OdLists db_odl) {
		if (null == db_odl) return null;
		com.relteq.sirius.jaxb.ODList odlist = factory.createODList();
		odlist.setId(db_odl.getId());
		odlist.setName(db_odl.getName());
		// TODO odlist.setDescription(db_odl.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<Ods> db_od_l = db_odl.getOdss();
			for (Ods db_od : db_od_l)
				odlist.getOd().add(restoreOD(db_od));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return odlist;
	}

	private com.relteq.sirius.jaxb.Od restoreOD(Ods db_od) {
		com.relteq.sirius.jaxb.Od od = factory.createOd();
		od.setId(db_od.getId());
		od.setLinkIdOrigin(db_od.getOriginLinkId());
		od.setLinkIdDestination(db_od.getDestinationLinkId());
		// TODO od.setRouteSegments();
		// TODO od.setDecisionPoints();
		return od;
	}
}
