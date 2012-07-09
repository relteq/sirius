package com.relteq.sirius.db.exporter;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.SchemaFactory;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;
import org.xml.sax.SAXException;

import com.relteq.sirius.om.ControllerSets;
import com.relteq.sirius.om.DecisionPointSplitProfileSets;
import com.relteq.sirius.om.DecisionPointSplitProfiles;
import com.relteq.sirius.om.DecisionPointSplits;
import com.relteq.sirius.om.DecisionPointSplitsPeer;
import com.relteq.sirius.om.DemandProfileSets;
import com.relteq.sirius.om.DemandProfiles;
import com.relteq.sirius.om.Demands;
import com.relteq.sirius.om.DemandsPeer;
import com.relteq.sirius.om.DownstreamBoundaryCapacities;
import com.relteq.sirius.om.DownstreamBoundaryCapacitiesPeer;
import com.relteq.sirius.om.DownstreamBoundaryCapacityProfileSets;
import com.relteq.sirius.om.DownstreamBoundaryCapacityProfiles;
import com.relteq.sirius.om.EventSets;
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
import com.relteq.sirius.om.OdDemandProfileSets;
import com.relteq.sirius.om.OdDemandProfiles;
import com.relteq.sirius.om.OdDemands;
import com.relteq.sirius.om.OdDemandsPeer;
import com.relteq.sirius.om.OdLists;
import com.relteq.sirius.om.Ods;
import com.relteq.sirius.om.PhaseLinks;
import com.relteq.sirius.om.PhaseLinksPeer;
import com.relteq.sirius.om.Phases;
import com.relteq.sirius.om.Scenarios;
import com.relteq.sirius.om.ScenariosPeer;
import com.relteq.sirius.om.SensorLists;
import com.relteq.sirius.om.SignalLists;
import com.relteq.sirius.om.Signals;
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
			// TODO scenario.setRouteSegments();
			scenario.setDecisionPoints(restoreDecisionPoints(db_scenario.getDecisionPointSplitProfileSets()));
			scenario.setSignalList(restoreSignalList(db_scenario.getSignalLists()));
			scenario.setSensorList(restoreSensorList(db_scenario.getSensorLists()));
			scenario.setSplitRatioProfileSet(restoreSplitRatioProfileSet(db_scenario.getSplitRatioProfileSets()));
			scenario.setWeavingFactorSet(restoreWeavingFactorSet(db_scenario.getWeavingFactorSets()));
			scenario.setInitialDensitySet(restoreInitialDensitySet(db_scenario.getInitialDensitySets()));
			scenario.setFundamentalDiagramProfileSet(restoreFundamentalDiagramProfileSet(db_scenario.getFundamentalDiagramProfileSets()));
			scenario.setDemandProfileSet(restoreDemandProfileSet(db_scenario.getDemandProfileSets()));
			scenario.setODDemandProfileSet(restoreODDemandProfileSet(db_scenario.getOdDemandProfileSets()));
			scenario.setDownstreamBoundaryCapacityProfileSet(restoreDownstreamBoundaryCapacity(db_scenario.getDownstreamBoundaryCapacityProfileSets()));
			scenario.setControllerSet(restoreControllerSet(db_scenario.getControllerSets()));
			scenario.setEventSet(restoreEventSet(db_scenario.getEventSets()));
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

	private com.relteq.sirius.jaxb.InitialDensitySet restoreInitialDensitySet(InitialDensitySets db_idset) {
		if (null == db_idset) return null;
		com.relteq.sirius.jaxb.InitialDensitySet idset = factory.createInitialDensitySet();
		idset.setId(db_idset.getId());
		idset.setName(db_idset.getName());
		idset.setDescription(db_idset.getDescription());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(InitialDensitiesPeer.LINK_ID);
		crit.addAscendingOrderByColumn(InitialDensitiesPeer.VEHICLE_TYPE_ID);
		try {
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
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return idset;
	}

	private com.relteq.sirius.jaxb.WeavingFactorSet restoreWeavingFactorSet(WeavingFactorSets db_wfset) {
		if (null == db_wfset) return null;
		com.relteq.sirius.jaxb.WeavingFactorSet wfset = factory.createWeavingFactorSet();
		wfset.setId(db_wfset.getId());
		wfset.setName(db_wfset.getName());
		wfset.setDescription(db_wfset.getDescription());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(WeavingFactorsPeer.IN_LINK_ID);
		crit.addAscendingOrderByColumn(WeavingFactorsPeer.OUT_LINK_ID);
		crit.addAscendingOrderByColumn(WeavingFactorsPeer.VEHICLE_TYPE_ID);
		try {
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
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return wfset;
	}

	private com.relteq.sirius.jaxb.SplitRatioProfileSet restoreSplitRatioProfileSet(SplitRatioProfileSets db_srps) {
		if (null == db_srps) return null;
		com.relteq.sirius.jaxb.SplitRatioProfileSet srps = factory.createSplitRatioProfileSet();
		srps.setId(db_srps.getId());
		srps.setName(db_srps.getName());
		srps.setDescription(db_srps.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<SplitRatioProfiles> db_srp_l = db_srps.getSplitRatioProfiless();
			for (SplitRatioProfiles db_srp : db_srp_l)
				srps.getSplitratioProfile().add(restoreSplitRatioProfile(db_srp));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return srps;
	}

	private com.relteq.sirius.jaxb.SplitratioProfile restoreSplitRatioProfile(SplitRatioProfiles db_srp) {
		com.relteq.sirius.jaxb.SplitratioProfile srp = factory.createSplitratioProfile();
		srp.setNodeId(db_srp.getNodeId());
		srp.setDt(db_srp.getDt());
		srp.setStartTime(db_srp.getStartTime());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(SplitRatiosPeer.IN_LINK_ID);
		crit.addAscendingOrderByColumn(SplitRatiosPeer.OUT_LINK_ID);
		crit.addAscendingOrderByColumn(SplitRatiosPeer.NUMBER);
		crit.addAscendingOrderByColumn(SplitRatiosPeer.VEHICLE_TYPE_ID);
		try {
			@SuppressWarnings("unchecked")
			List<SplitRatios> db_sr_l = db_srp.getSplitRatioss(crit);
			com.relteq.sirius.jaxb.Splitratio sr = null;
			int number = -1;
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
					sb.append(db_sr.getNumber() == number ? ':' : ',');
				}
				number = db_sr.getNumber();
				sb.append(db_sr.getSplitRatio().toPlainString());
			}
			if (null != sr) {
				sr.setContent(sb.toString());
				srp.getSplitratio().add(sr);
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
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

	com.relteq.sirius.jaxb.FundamentalDiagramProfile restoreFundamentalDiagramProfile(FundamentalDiagramProfiles db_fdprofile) {
		com.relteq.sirius.jaxb.FundamentalDiagramProfile fdprofile = factory.createFundamentalDiagramProfile();
		fdprofile.setLinkId(db_fdprofile.getLinkId());
		fdprofile.setDt(db_fdprofile.getDt());
		fdprofile.setStartTime(db_fdprofile.getStartTime());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(FundamentalDiagramsPeer.NUMBER);
		try {
			@SuppressWarnings("unchecked")
			List<FundamentalDiagrams> db_fd_l = db_fdprofile.getFundamentalDiagramss(crit);
			for (FundamentalDiagrams db_fd : db_fd_l)
				fdprofile.getFundamentalDiagram().add(restoreFundamentalDiagram(db_fd));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
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

	private com.relteq.sirius.jaxb.DemandProfileSet restoreDemandProfileSet(DemandProfileSets db_dpset) {
		if (null == db_dpset) return null;
		com.relteq.sirius.jaxb.DemandProfileSet dpset = factory.createDemandProfileSet();
		dpset.setId(db_dpset.getId());
		dpset.setName(db_dpset.getName());
		dpset.setDescription(db_dpset.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<DemandProfiles> db_dp_l = db_dpset.getDemandProfiless();
			for (DemandProfiles db_dp : db_dp_l)
				dpset.getDemandProfile().add(restoreDemandProfile(db_dp));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return dpset;
	}

	private com.relteq.sirius.jaxb.DemandProfile restoreDemandProfile(DemandProfiles db_dp) {
		com.relteq.sirius.jaxb.DemandProfile dp = factory.createDemandProfile();
		dp.setLinkIdOrigin(db_dp.getLinkId());
		dp.setDt(db_dp.getDt());
		dp.setStartTime(db_dp.getStartTime());
		dp.setKnob(db_dp.getKnob());
		dp.setStdDevAdd(db_dp.getStdDeviationAdditive());
		dp.setStdDevMult(db_dp.getStdDeviationMultiplicative());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(DemandsPeer.NUMBER);
		crit.addAscendingOrderByColumn(DemandsPeer.VEHICLE_TYPE_ID);
		try {
			@SuppressWarnings("unchecked")
			List<Demands> db_demand_l = db_dp.getDemandss(crit);
			StringBuilder sb = null;
			int number = -1;
			for (Demands db_demand : db_demand_l) {
				if (null == sb) sb = new StringBuilder();
				else sb.append(db_demand.getNumber() == number ? ':' : ',');
				number = db_demand.getNumber();
				sb.append(db_demand.getDemand().toPlainString());
			}
			if (null != sb) dp.setContent(sb.toString());
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
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

	private com.relteq.sirius.jaxb.DecisionPoints restoreDecisionPoints(DecisionPointSplitProfileSets db_dpsps) {
		if (null == db_dpsps) return null;
		com.relteq.sirius.jaxb.DecisionPoints dpoints = factory.createDecisionPoints();
		// TODO dpoints.setName(db_dpsps.getName());
		// TODO dpoints.setDescription(db_dpsps.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<DecisionPointSplitProfiles> db_dpsp_l = db_dpsps.getDecisionPointSplitProfiless();
			for (DecisionPointSplitProfiles db_dpsp : db_dpsp_l)
				dpoints.getDecisionPoint().add(restoreDecisionPoint(db_dpsp));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return dpoints;
	}

	private com.relteq.sirius.jaxb.DecisionPoint restoreDecisionPoint(DecisionPointSplitProfiles db_dpsp) {
		com.relteq.sirius.jaxb.DecisionPoint dpoint = factory.createDecisionPoint();
		dpoint.setId(db_dpsp.getId());
		dpoint.setNodeId(db_dpsp.getNodeId());
		dpoint.setDt(db_dpsp.getDt());
		dpoint.setStartTime(db_dpsp.getStartTime());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(DecisionPointSplitsPeer.IN_ROUTE_SEGMENT_ID);
		crit.addAscendingOrderByColumn(DecisionPointSplitsPeer.OUT_ROUTE_SEGMENT_ID);
		crit.addAscendingOrderByColumn(DecisionPointSplitsPeer.VEHICLE_TYPE_ID);
		try {
			@SuppressWarnings("unchecked")
			List<DecisionPointSplits> db_dps_l = db_dpsp.getDecisionPointSplitss(crit);
			com.relteq.sirius.jaxb.DecisionPointSplit dps = null;
			StringBuilder sb = new StringBuilder();
			for (DecisionPointSplits db_dps : db_dps_l) {
				if (null != dps && (!dps.getRouteSegmentIn().equals(db_dps.getInRouteSegmentId()) || !dps.getRouteSegmentOut().equals(db_dps.getOutRouteSegmentId()))) {
					dps.setContent(sb.toString());
					dpoint.getDecisionPointSplit().add(dps);
					dps = null;
					sb.setLength(0);
				}
				if (null == dps) {
					dps = factory.createDecisionPointSplit();
					dps.setRouteSegmentIn(db_dps.getInRouteSegmentId());
					dps.setRouteSegmentOut(db_dps.getOutRouteSegmentId());
				} else
					sb.append(':');
				sb.append(db_dps.getSplit().toPlainString());
			}
			if (null != dps) {
				dps.setContent(sb.toString());
				dpoint.getDecisionPointSplit().add(dps);
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return dpoint;
	}

	private com.relteq.sirius.jaxb.SignalList restoreSignalList(SignalLists db_sl) {
		if (null == db_sl) return null;
		com.relteq.sirius.jaxb.SignalList sl = factory.createSignalList();
		// TODO sl.setName(db_sl.getName());
		// TODO sl.setDescription(db_sl.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<Signals> db_signal_l = db_sl.getSignalss();
			for (Signals db_signal : db_signal_l)
				sl.getSignal().add(restoreSignal(db_signal));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return sl;
	}

	private com.relteq.sirius.jaxb.Signal restoreSignal(Signals db_signal) {
		com.relteq.sirius.jaxb.Signal signal = factory.createSignal();
		signal.setId(db_signal.getId());
		signal.setNodeId(db_signal.getNodeId());
		try {
			@SuppressWarnings("unchecked")
			List<Phases> db_ph_l = db_signal.getPhasess();
			for (Phases db_ph : db_ph_l)
				signal.getPhase().add(restorePhase(db_ph));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return signal;
	}

	private com.relteq.sirius.jaxb.Phase restorePhase(Phases db_ph) {
		com.relteq.sirius.jaxb.Phase phase = factory.createPhase();
		phase.setNema(BigInteger.valueOf(db_ph.getPhase()));
		phase.setProtected(db_ph.getIs_protected());
		phase.setPermissive(db_ph.getPermissive());
		phase.setLag(db_ph.getLag());
		phase.setRecall(db_ph.getRecall());
		phase.setMinGreenTime(db_ph.getMinGreenTime());
		phase.setYellowTime(db_ph.getYellowTime());
		phase.setRedClearTime(db_ph.getRedClearTime());
		Criteria crit = new Criteria();
		crit.add(PhaseLinksPeer.PHASE, db_ph.getPhase());
		try {
			@SuppressWarnings("unchecked")
			List<PhaseLinks> db_phl_l = db_ph.getSignals(null).getPhaseLinkss(crit);
			com.relteq.sirius.jaxb.Links links = factory.createLinks();
			for (PhaseLinks db_phl : db_phl_l)
				links.getLinkReference().add(restorePhaseLink(db_phl));
			phase.setLinks(links);
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return phase;
	}

	private com.relteq.sirius.jaxb.LinkReference restorePhaseLink(PhaseLinks db_phl) {
		com.relteq.sirius.jaxb.LinkReference lr = factory.createLinkReference();
		lr.setId(db_phl.getLinkId());
		return lr;
	}

	private com.relteq.sirius.jaxb.SensorList restoreSensorList(SensorLists db_sl) {
		if (null == db_sl) return null;
		com.relteq.sirius.jaxb.SensorList sl = factory.createSensorList();
		// TODO sl.getSensor().add();
		return sl;
	}

	private com.relteq.sirius.jaxb.ODDemandProfileSet restoreODDemandProfileSet(OdDemandProfileSets db_oddps) {
		if (null == db_oddps) return null;
		com.relteq.sirius.jaxb.ODDemandProfileSet oddps = factory.createODDemandProfileSet();
		oddps.setId(db_oddps.getId());
		oddps.setName(db_oddps.getName());
		oddps.setDescription(db_oddps.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<OdDemandProfiles> db_oddp_l = db_oddps.getOdDemandProfiless();
			for (OdDemandProfiles db_oddp : db_oddp_l)
				oddps.getOdDemandProfile().add(restoreODDemandProfile(db_oddp));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return oddps;
	}

	private com.relteq.sirius.jaxb.OdDemandProfile restoreODDemandProfile(OdDemandProfiles db_oddp) {
		com.relteq.sirius.jaxb.OdDemandProfile oddp = factory.createOdDemandProfile();
		oddp.setOdId(db_oddp.getOdId());
		oddp.setDt(db_oddp.getDt());
		oddp.setStartTime(db_oddp.getStartTime());
		oddp.setKnob(db_oddp.getKnob());
		oddp.setStdDevAdd(db_oddp.getStdDeviationAdditive());
		oddp.setStdDevMult(db_oddp.getStdDeviationMultiplicative());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(OdDemandsPeer.VEHICLE_TYPE_ID);
		try {
			@SuppressWarnings("unchecked")
			List<OdDemands> db_odd_l = db_oddp.getOdDemandss(crit);
			StringBuilder sb = null;
			for (OdDemands db_odd : db_odd_l) {
				if (null == sb) sb = new StringBuilder();
				else sb.append(':');
				sb.append(db_odd.getOdDemand().toPlainString());
			}
			if (null != sb) oddp.setContent(sb.toString());
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return oddp;
	}

	private com.relteq.sirius.jaxb.DownstreamBoundaryCapacityProfileSet restoreDownstreamBoundaryCapacity(DownstreamBoundaryCapacityProfileSets db_dbcps) {
		if (null == db_dbcps) return null;
		com.relteq.sirius.jaxb.DownstreamBoundaryCapacityProfileSet dbcps = factory.createDownstreamBoundaryCapacityProfileSet();
		dbcps.setId(db_dbcps.getId());
		dbcps.setName(db_dbcps.getName());
		dbcps.setDescription(db_dbcps.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<DownstreamBoundaryCapacityProfiles> db_dbcp_l = db_dbcps.getDownstreamBoundaryCapacityProfiless();
			for (DownstreamBoundaryCapacityProfiles db_dbcp : db_dbcp_l)
				dbcps.getCapacityProfile().add(restoreCapacityProfile(db_dbcp));
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return dbcps;
	}

	private com.relteq.sirius.jaxb.CapacityProfile restoreCapacityProfile(DownstreamBoundaryCapacityProfiles db_dbcp) {
		com.relteq.sirius.jaxb.CapacityProfile cprofile = factory.createCapacityProfile();
		cprofile.setLinkId(db_dbcp.getLinkId());
		cprofile.setDt(db_dbcp.getDt());
		cprofile.setStartTime(db_dbcp.getStartTime());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(DownstreamBoundaryCapacitiesPeer.NUMBER);
		try {
			@SuppressWarnings("unchecked")
			List<DownstreamBoundaryCapacities> db_dbc_l = db_dbcp.getDownstreamBoundaryCapacitiess(crit);
			StringBuilder sb = null;
			for (DownstreamBoundaryCapacities db_dbc : db_dbc_l) {
				// TODO delimiter = ',' or ':'?
				if (null == sb) sb = new StringBuilder();
				else sb.append(',');
				sb.append(db_dbc.getDownstreamBoundaryCapacity().toPlainString());
			}
			if (null != sb) cprofile.setContent(sb.toString());
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return cprofile;
	}

	private com.relteq.sirius.jaxb.ControllerSet restoreControllerSet(ControllerSets db_cs) {
		if (null == db_cs) return null;
		com.relteq.sirius.jaxb.ControllerSet cset = factory.createControllerSet();
		cset.setId(db_cs.getId());
		// TODO cset.setName();
		// TODO cset.setDescription();
		// TODO cset.getController().add();
		return cset;
	}

	private com.relteq.sirius.jaxb.EventSet restoreEventSet(EventSets db_es) {
		if (null == db_es) return null;
		com.relteq.sirius.jaxb.EventSet eset = factory.createEventSet();
		eset.setId(db_es.getId());
		// TODO eset.setName();
		// TODO eset.setDescription();
		// TODO eset.getEvent().add();
		return eset;
	}
}
