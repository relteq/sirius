package com.relteq.sirius.db.exporter;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.SchemaFactory;

import org.apache.torque.NoRowsException;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;
import org.xml.sax.SAXException;

import com.relteq.sirius.om.*;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;

/**
 * Loads a scenario from the database
 */
public class ScenarioRestorer {
	public static void export(long id, String filename) throws SiriusException, JAXBException, SAXException {
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
	public static com.relteq.sirius.simulator.Scenario getScenario(long id) throws SiriusException {
		com.relteq.sirius.simulator.Scenario scenario = com.relteq.sirius.simulator.ObjectFactory.process(new ScenarioRestorer().restore(id));
		if (null == scenario)
			throw new SiriusException("Could not load scenario " + id + " from the database. See error log for details.");
		return scenario;
	}

	com.relteq.sirius.simulator.JaxbObjectFactory factory = null;

	private ScenarioRestorer() {
		factory = new com.relteq.sirius.simulator.JaxbObjectFactory();
	}

	private com.relteq.sirius.simulator.Scenario restore(long id) throws SiriusException {
		com.relteq.sirius.db.Service.ensureInit();
		Scenarios db_scenario = null;
		try {
			db_scenario = ScenariosPeer.retrieveByPK(id);
		} catch (NoRowsException exc) {
			throw new SiriusException("Scenario " + id + " does not exist", exc);
		} catch (TorqueException exc) {
			throw new SiriusException(exc);
		}
		return (com.relteq.sirius.simulator.Scenario) restoreScenario(db_scenario);
	}

	/**
	 * Converts a numeric ID to a string
	 * @param id
	 * @return String
	 */
	private static String id2str(long id) {
		return Long.toString(id);
	}

	private com.relteq.sirius.jaxb.Scenario restoreScenario(Scenarios db_scenario) throws SiriusException {
		if (null == db_scenario) return null;
		com.relteq.sirius.jaxb.Scenario scenario = factory.createScenario();
		scenario.setId(id2str(db_scenario.getId()));
		scenario.setName(db_scenario.getName());
		scenario.setDescription(db_scenario.getDescription());
		try{
			scenario.setSettings(restoreSettings(db_scenario));
			scenario.setNetworkList(restoreNetworkList(db_scenario));
			scenario.setNetworkConnections(restoreNetworkConnections(db_scenario.getNetworkConnectionSets()));
			scenario.setSignalList(restoreSignalList(db_scenario.getSignalSets()));
			scenario.setSensorList(restoreSensorList(db_scenario.getSensorSets()));
			scenario.setSplitRatioProfileSet(restoreSplitRatioProfileSet(db_scenario.getSplitRatioProfileSets()));
			scenario.setWeavingFactorSet(restoreWeavingFactorSet(db_scenario.getWeavingFactorSets()));
			scenario.setInitialDensitySet(restoreInitialDensitySet(db_scenario.getInitialDensitySets()));
			scenario.setFundamentalDiagramProfileSet(restoreFundamentalDiagramProfileSet(db_scenario.getFundamentalDiagramProfileSets()));
			scenario.setDemandProfileSet(restoreDemandProfileSet(db_scenario.getDemandProfileSets()));
			scenario.setDownstreamBoundaryCapacityProfileSet(restoreDownstreamBoundaryCapacity(db_scenario.getDownstreamBoundaryCapacityProfileSets()));
			scenario.setControllerSet(restoreControllerSet(db_scenario.getControllerSets()));
			scenario.setEventSet(restoreEventSet(db_scenario.getEventSets()));
		} catch (TorqueException exc) {
			throw new SiriusException(exc);
		}
		return scenario;
	}

	private com.relteq.sirius.jaxb.Settings restoreSettings(Scenarios db_scenario) throws TorqueException {
		com.relteq.sirius.jaxb.Settings settings = factory.createSettings();
		settings.setUnits("US");
		settings.setVehicleTypes(restoreVehicleTypes(db_scenario.getVehicleTypeSets()));
		return settings;
	}

	private com.relteq.sirius.jaxb.VehicleTypes restoreVehicleTypes(VehicleTypeSets db_vtsets) {
		if (null == db_vtsets) return null;
		com.relteq.sirius.jaxb.VehicleTypes vts = factory.createVehicleTypes();
		Criteria crit = new Criteria();
		crit.addJoin(VehicleTypesInSetsPeer.VEHICLE_TYPE_ID, VehicleTypesPeer.VEHICLE_TYPE_ID);
		crit.add(VehicleTypesInSetsPeer.VEHICLE_TYPE_SET_ID, db_vtsets.getId());
		crit.add(VehicleTypesPeer.PROJECT_ID, db_vtsets.getProjectId());
		crit.addAscendingOrderByColumn(VehicleTypesPeer.VEHICLE_TYPE_ID);
		try {
			@SuppressWarnings("unchecked")
			List<VehicleTypes> db_vt_l = VehicleTypesPeer.doSelect(crit);
			for (VehicleTypes db_vt : db_vt_l)
				vts.getVehicleType().add(restoreVehicleType(db_vt));
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return vts;
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
			List<NetworkSets> db_nets_l = db_scenario.getNetworkSetss();
			if (0 < db_nets_l.size()) {
				com.relteq.sirius.jaxb.NetworkList nets = factory.createNetworkList();
				for (NetworkSets db_nets : db_nets_l)
					nets.getNetwork().add(restoreNetwork(db_nets.getNetworks()));
				return nets;
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return null;
	}

	private com.relteq.sirius.jaxb.Network restoreNetwork(Networks db_net) {
		com.relteq.sirius.jaxb.Network net = factory.createNetwork();
		net.setId(id2str(db_net.getId()));
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
			SiriusErrorLog.addError(exc.getMessage());
		}
		return null;
	}

	private com.relteq.sirius.jaxb.Node restoreNode(Nodes db_node) {
		com.relteq.sirius.jaxb.Node node = factory.createNode();
		node.setId(id2str(db_node.getNodeId()));
		try {
			@SuppressWarnings("unchecked")
			List<NodeName> db_nname_l = db_node.getNodeNames();
			if (0 < db_nname_l.size()) {
				node.setName(db_nname_l.get(0).getName());
				if (1 < db_nname_l.size())
					SiriusErrorLog.addWarning("Node " + db_node.getNodeId() + " has " + db_nname_l.size() + " values for @name");
			}
			@SuppressWarnings("unchecked")
			List<NodeType> db_ntype_l = db_node.getNodeTypes();
			if (0 < db_ntype_l.size()) {
				node.setType(db_ntype_l.get(0).getType());
				if (1 < db_ntype_l.size())
					SiriusErrorLog.addWarning("Node " + db_node.getNodeId() + " has " + db_ntype_l.size() + " values for @type");

			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		// TODO node.setDescription();
		// TODO node.setType();
		// TODO db_node.getGeometry() -> node.setPosition();
		// TODO node.setPostmile();
		node.setInputs(restoreInputs(db_node));
		node.setOutputs(restoreOutputs(db_node));
		return node;
	}

	private com.relteq.sirius.jaxb.Inputs restoreInputs(Nodes db_node) {
		com.relteq.sirius.jaxb.Inputs inputs = factory.createInputs();
		Criteria crit = new Criteria();
		crit.add(LinksPeer.NETWORK_ID, db_node.getNetworkId());
		crit.add(LinksPeer.END_NODE_ID, db_node.getNodeId());
		try {
			@SuppressWarnings("unchecked")
			List<Links> db_link_l = LinksPeer.doSelect(crit);
			for (Links db_link : db_link_l)
				inputs.getInput().add(restoreInput(db_link));
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return inputs;
	}

	private com.relteq.sirius.jaxb.Input restoreInput(Links db_link) {
		com.relteq.sirius.jaxb.Input input = factory.createInput();
		input.setLinkId(id2str(db_link.getLinkId()));
		return input;
	}

	private com.relteq.sirius.jaxb.Outputs restoreOutputs(Nodes db_node) {
		com.relteq.sirius.jaxb.Outputs outputs = factory.createOutputs();
		Criteria crit = new Criteria();
		crit.add(LinksPeer.NETWORK_ID, db_node.getNetworkId());
		crit.add(LinksPeer.BEGIN_NODE_ID, db_node.getNodeId());
		try {
			@SuppressWarnings("unchecked")
			List<Links> db_link_l = LinksPeer.doSelect(crit);
			for (Links db_link : db_link_l)
				outputs.getOutput().add(restoreOutput(db_link));
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return outputs;
	}

	private com.relteq.sirius.jaxb.Output restoreOutput(Links db_link) {
		com.relteq.sirius.jaxb.Output output = factory.createOutput();
		output.setLinkId(id2str(db_link.getLinkId()));
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
			SiriusErrorLog.addError(exc.getMessage());
		}
		return null;
	}

	private com.relteq.sirius.jaxb.Link restoreLink(Links db_link) {
		com.relteq.sirius.jaxb.Link link = factory.createLink();
		link.setId(id2str(db_link.getLinkId()));
		// TODO link.setName();
		// TODO link.setRoadName();
		// TODO link.setDescription();
		// TODO link.setType();
		// TODO revise: geometry -> shape
		link.setShape(db_link.getGeometry());
		try {
			@SuppressWarnings("unchecked")
			List<LinkLanes> db_llanes_l = db_link.getLinkLaness();
			if (0 < db_llanes_l.size()) {
				link.setLanes(db_llanes_l.get(0).getLanes());
				if (1 < db_llanes_l.size())
					SiriusErrorLog.addWarning("Link " + db_link.getLinkId() + " has " + db_llanes_l.size() + " values for @lanes");
			}
			@SuppressWarnings("unchecked")
			List<LinkType> db_ltype_l = db_link.getLinkTypes();
			if (0 < db_ltype_l.size()) {
				link.setType(db_ltype_l.get(0).getType());
				if (1 < db_ltype_l.size())
					SiriusErrorLog.addWarning("Link " + db_link.getLinkId() + " has " + db_ltype_l.size() + " values for @type");
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		link.setLength(db_link.getLength());
		// TODO link.setDynamics();
		// TODO link.setLaneOffset();
		if (null != db_link.getBeginNodeId()) {
			com.relteq.sirius.jaxb.Begin begin = factory.createBegin();
			begin.setNodeId(id2str(db_link.getBeginNodeId()));
			link.setBegin(begin);
		}
		if (null != db_link.getEndNodeId()) {
			com.relteq.sirius.jaxb.End end = factory.createEnd();
			end.setNodeId(id2str(db_link.getEndNodeId()));
			link.setEnd(end);
		}
		return link;
	}

	private com.relteq.sirius.jaxb.InitialDensitySet restoreInitialDensitySet(InitialDensitySets db_idset) {
		if (null == db_idset) return null;
		com.relteq.sirius.jaxb.InitialDensitySet idset = factory.createInitialDensitySet();
		idset.setId(id2str(db_idset.getId()));
		idset.setName(db_idset.getName());
		idset.setDescription(db_idset.getDescription());
		// TODO idset.setTstamp();
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(InitialDensitiesPeer.LINK_ID);
		crit.addAscendingOrderByColumn(InitialDensitiesPeer.VEHICLE_TYPE_ID);
		try {
			@SuppressWarnings("unchecked")
			List<InitialDensities> db_idl = db_idset.getInitialDensitiess(crit);
			com.relteq.sirius.jaxb.Density density = null;
			StringBuilder sb = new StringBuilder();
			for (InitialDensities db_id : db_idl) {
				if (null != density && !density.getLinkId().equals(id2str(db_id.getLinkId()))) {
					density.setContent(sb.toString());
					idset.getDensity().add(density);
					density = null;
				}
				if (null == density) { // new link
					density = factory.createDensity();
					density.setLinkId(id2str(db_id.getLinkId()));
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
			SiriusErrorLog.addError(exc.getMessage());
		}
		return idset;
	}

	private com.relteq.sirius.jaxb.WeavingFactorSet restoreWeavingFactorSet(WeavingFactorSets db_wfset) {
		if (null == db_wfset) return null;
		com.relteq.sirius.jaxb.WeavingFactorSet wfset = factory.createWeavingFactorSet();
		wfset.setId(id2str(db_wfset.getId()));
		wfset.setName(db_wfset.getName());
		wfset.setDescription(db_wfset.getDescription());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(WeavingFactorsPeer.IN_LINK_ID);
		crit.addAscendingOrderByColumn(WeavingFactorsPeer.OUT_LINK_ID);
		crit.addAscendingOrderByColumn(WeavingFactorsPeer.VEHICLE_TYPE_ID);
		try {
			@SuppressWarnings("unchecked")
			List<WeavingFactors> db_wf_l = db_wfset.getWeavingFactorss();
			com.relteq.sirius.jaxb.Weavingfactors wf = null;
			StringBuilder sb = new StringBuilder();
			for (WeavingFactors db_wf : db_wf_l) {
				if (null != wf && !(wf.getLinkIn().equals(id2str(db_wf.getInLinkId())) && wf.getLinkOut().equals(id2str(db_wf.getOutLinkId())))) {
					wf.setContent(sb.toString());
					wfset.getWeavingfactors().add(wf);
					wf = null;
				}
				if (null == wf) { // new weaving factor
					wf = factory.createWeavingfactors();
					wf.setLinkIn(id2str(db_wf.getInLinkId()));
					wf.setLinkOut(id2str(db_wf.getOutLinkId()));
					sb.setLength(0);
				} else { // same weaving factor, different vehicle type
					sb.append(':');
				}
				sb.append(db_wf.getFactor().toPlainString());
			}
			if (null != wf) {
				wf.setContent(sb.toString());
				wfset.getWeavingfactors().add(wf);
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return wfset;
	}

	private com.relteq.sirius.jaxb.SplitRatioProfileSet restoreSplitRatioProfileSet(SplitRatioProfileSets db_srps) {
		if (null == db_srps) return null;
		com.relteq.sirius.jaxb.SplitRatioProfileSet srps = factory.createSplitRatioProfileSet();
		srps.setId(id2str(db_srps.getId()));
		srps.setName(db_srps.getName());
		srps.setDescription(db_srps.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<SplitRatioProfiles> db_srp_l = db_srps.getSplitRatioProfiless();
			for (SplitRatioProfiles db_srp : db_srp_l)
				srps.getSplitratioProfile().add(restoreSplitRatioProfile(db_srp));
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return srps;
	}

	private com.relteq.sirius.jaxb.SplitratioProfile restoreSplitRatioProfile(SplitRatioProfiles db_srp) {
		com.relteq.sirius.jaxb.SplitratioProfile srp = factory.createSplitratioProfile();
		srp.setNodeId(id2str(db_srp.getNodeId()));
		srp.setDt(db_srp.getDt());
		srp.setStartTime(db_srp.getStartTime());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(SplitRatiosPeer.IN_LINK_ID);
		crit.addAscendingOrderByColumn(SplitRatiosPeer.OUT_LINK_ID);
		crit.addAscendingOrderByColumn(SplitRatiosPeer.ORDINAL);
		crit.addAscendingOrderByColumn(SplitRatiosPeer.VEHICLE_TYPE_ID);
		try {
			@SuppressWarnings("unchecked")
			List<SplitRatios> db_sr_l = db_srp.getSplitRatioss(crit);
			com.relteq.sirius.jaxb.Splitratio sr = null;
			Integer ordinal = null;
			StringBuilder sb = new StringBuilder();
			for (SplitRatios db_sr : db_sr_l) {
				if (null != sr && !(sr.getLinkIn().equals(id2str(db_sr.getInLinkId())) && sr.getLinkOut().equals(id2str(db_sr.getOutLinkId())))) {
					sr.setContent(sb.toString());
					srp.getSplitratio().add(sr);
					sr = null;
				}
				if (null == sr) { // new split ratio
					sr = factory.createSplitratio();
					sr.setLinkIn(id2str(db_sr.getInLinkId()));
					sr.setLinkOut(id2str(db_sr.getOutLinkId()));
					sb.setLength(0);
				} else { // same split ratio, different time stamp (',') or vehicle type (':')
					sb.append(db_sr.getOrdinal().equals(ordinal) ? ':' : ',');
				}
				ordinal = db_sr.getOrdinal();
				sb.append(db_sr.getSplitRatio().toPlainString());
			}
			if (null != sr) {
				sr.setContent(sb.toString());
				srp.getSplitratio().add(sr);
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return srp;
	}

	com.relteq.sirius.jaxb.FundamentalDiagramProfileSet restoreFundamentalDiagramProfileSet(FundamentalDiagramProfileSets db_fdps) {
		if (null == db_fdps) return null;
		com.relteq.sirius.jaxb.FundamentalDiagramProfileSet fdps = factory.createFundamentalDiagramProfileSet();
		fdps.setId(id2str(db_fdps.getId()));
		fdps.setName(db_fdps.getName());
		fdps.setDescription(db_fdps.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<FundamentalDiagramProfiles> db_fdprofile_l = db_fdps.getFundamentalDiagramProfiless();
			for (FundamentalDiagramProfiles db_fdprofile : db_fdprofile_l)
				fdps.getFundamentalDiagramProfile().add(restoreFundamentalDiagramProfile(db_fdprofile));
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return fdps;
	}

	com.relteq.sirius.jaxb.FundamentalDiagramProfile restoreFundamentalDiagramProfile(FundamentalDiagramProfiles db_fdprofile) {
		com.relteq.sirius.jaxb.FundamentalDiagramProfile fdprofile = factory.createFundamentalDiagramProfile();
		fdprofile.setLinkId(id2str(db_fdprofile.getLinkId()));
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
			SiriusErrorLog.addError(exc.getMessage());
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
		fd.setStdDevCapacity(db_fd.getCapacityStd());
		return fd;
	}

	private com.relteq.sirius.jaxb.DemandProfileSet restoreDemandProfileSet(DemandProfileSets db_dpset) {
		if (null == db_dpset) return null;
		com.relteq.sirius.jaxb.DemandProfileSet dpset = factory.createDemandProfileSet();
		dpset.setId(id2str(db_dpset.getId()));
		dpset.setName(db_dpset.getName());
		dpset.setDescription(db_dpset.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<DemandProfiles> db_dp_l = db_dpset.getDemandProfiless();
			for (DemandProfiles db_dp : db_dp_l)
				dpset.getDemandProfile().add(restoreDemandProfile(db_dp));
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return dpset;
	}

	private com.relteq.sirius.jaxb.DemandProfile restoreDemandProfile(DemandProfiles db_dp) {
		com.relteq.sirius.jaxb.DemandProfile dp = factory.createDemandProfile();
		dp.setLinkIdOrigin(id2str(db_dp.getOriginLinkId()));
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
			Integer number = null;
			for (Demands db_demand : db_demand_l) {
				if (null == sb) sb = new StringBuilder();
				else sb.append(db_demand.getNumber().equals(number) ? ':' : ',');
				number = db_demand.getNumber();
				sb.append(db_demand.getDemand().toPlainString());
			}
			if (null != sb) dp.setContent(sb.toString());
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return dp;
	}

	private com.relteq.sirius.jaxb.NetworkConnections restoreNetworkConnections(NetworkConnectionSets db_ncs) {
		if (null == db_ncs) return null;
		com.relteq.sirius.jaxb.NetworkConnections nc = factory.createNetworkConnections();
		nc.setId(id2str(db_ncs.getId()));
		nc.setName(db_ncs.getName());
		nc.setDescription(db_ncs.getDescription());
		Criteria crit = new Criteria();
		crit.addAscendingOrderByColumn(NetworkConnectionsPeer.FROM_NETWORK_ID);
		crit.addAscendingOrderByColumn(NetworkConnectionsPeer.TO_NETWORK_ID);
		try {
			@SuppressWarnings("unchecked")
			List<NetworkConnections> db_nc_l = db_ncs.getNetworkConnectionss(crit);
			com.relteq.sirius.jaxb.Networkpair np = null;
			for (NetworkConnections db_nc : db_nc_l) {
				if (null != np && (!np.getNetworkA().equals(db_nc.getFromNetworkId()) || !np.getNetworkB().equals(db_nc.getToNetworkId()))) {
					nc.getNetworkpair().add(np);
					np = null;
				}
				if (null == np) {
					np = factory.createNetworkpair();
					np.setNetworkA(id2str(db_nc.getFromNetworkId()));
					np.setNetworkB(id2str(db_nc.getToNetworkId()));
				}
				com.relteq.sirius.jaxb.Linkpair lp = factory.createLinkpair();
				lp.setLinkA(id2str(db_nc.getFromLinkId()));
				lp.setLinkB(id2str(db_nc.getToLinkId()));
				np.getLinkpair().add(lp);
			}
			if (null != np) nc.getNetworkpair().add(np);
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return nc;
	}

	private com.relteq.sirius.jaxb.SignalList restoreSignalList(SignalSets db_ss) {
		if (null == db_ss) return null;
		com.relteq.sirius.jaxb.SignalList sl = factory.createSignalList();
		// TODO sl.setName(db_sl.getName());
		// TODO sl.setDescription(db_sl.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<Signals> db_signal_l = db_ss.getSignalss();
			for (Signals db_signal : db_signal_l)
				sl.getSignal().add(restoreSignal(db_signal));
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return sl;
	}

	private com.relteq.sirius.jaxb.Signal restoreSignal(Signals db_signal) {
		com.relteq.sirius.jaxb.Signal signal = factory.createSignal();
		signal.setId(id2str(db_signal.getId()));
		signal.setNodeId(id2str(db_signal.getNodeId()));
		try {
			@SuppressWarnings("unchecked")
			List<Phases> db_ph_l = db_signal.getPhasess();
			for (Phases db_ph : db_ph_l)
				signal.getPhase().add(restorePhase(db_ph));
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return signal;
	}

	private com.relteq.sirius.jaxb.Phase restorePhase(Phases db_ph) {
		com.relteq.sirius.jaxb.Phase phase = factory.createPhase();
		phase.setNema(BigInteger.valueOf(db_ph.getNema()));
		phase.setProtected(db_ph.getIsProtected());
		phase.setPermissive(db_ph.getPermissive());
		phase.setLag(db_ph.getLag());
		phase.setRecall(db_ph.getRecall());
		phase.setMinGreenTime(db_ph.getMinGreenTime());
		phase.setYellowTime(db_ph.getYellowTime());
		phase.setRedClearTime(db_ph.getRedClearTime());
		try {
			@SuppressWarnings("unchecked")
			List<PhaseLinks> db_phl_l = db_ph.getPhaseLinkss();
			com.relteq.sirius.jaxb.LinkReferences linkrefs = factory.createLinkReferences();
			for (PhaseLinks db_phl : db_phl_l)
				linkrefs.getLinkReference().add(restorePhaseLink(db_phl));
			phase.setLinkReferences(linkrefs);
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return phase;
	}

	private com.relteq.sirius.jaxb.LinkReference restorePhaseLink(PhaseLinks db_phl) {
		com.relteq.sirius.jaxb.LinkReference lr = factory.createLinkReference();
		lr.setId(id2str(db_phl.getLinkId()));
		return lr;
	}

	private com.relteq.sirius.jaxb.SensorList restoreSensorList(SensorSets db_ss) {
		if (null == db_ss) return null;
		com.relteq.sirius.jaxb.SensorList sl = factory.createSensorList();
		// TODO sl.getSensor().add();
		return sl;
	}

	private com.relteq.sirius.jaxb.DownstreamBoundaryCapacityProfileSet restoreDownstreamBoundaryCapacity(DownstreamBoundaryCapacityProfileSets db_dbcps) {
		if (null == db_dbcps) return null;
		com.relteq.sirius.jaxb.DownstreamBoundaryCapacityProfileSet dbcps = factory.createDownstreamBoundaryCapacityProfileSet();
		dbcps.setId(id2str(db_dbcps.getId()));
		dbcps.setName(db_dbcps.getName());
		dbcps.setDescription(db_dbcps.getDescription());
		try {
			@SuppressWarnings("unchecked")
			List<DownstreamBoundaryCapacityProfiles> db_dbcp_l = db_dbcps.getDownstreamBoundaryCapacityProfiless();
			for (DownstreamBoundaryCapacityProfiles db_dbcp : db_dbcp_l)
				dbcps.getCapacityProfile().add(restoreCapacityProfile(db_dbcp));
		} catch (TorqueException exc) {
			SiriusErrorLog.addError(exc.getMessage());
		}
		return dbcps;
	}

	private com.relteq.sirius.jaxb.CapacityProfile restoreCapacityProfile(DownstreamBoundaryCapacityProfiles db_dbcp) {
		com.relteq.sirius.jaxb.CapacityProfile cprofile = factory.createCapacityProfile();
		cprofile.setLinkId(id2str(db_dbcp.getLinkId()));
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
			SiriusErrorLog.addError(exc.getMessage());
		}
		return cprofile;
	}

	private com.relteq.sirius.jaxb.ControllerSet restoreControllerSet(ControllerSets db_cs) {
		if (null == db_cs) return null;
		com.relteq.sirius.jaxb.ControllerSet cset = factory.createControllerSet();
		cset.setId(id2str(db_cs.getId()));
		// TODO cset.setName();
		// TODO cset.setDescription();
		// TODO cset.getController().add();
		return cset;
	}

	private com.relteq.sirius.jaxb.EventSet restoreEventSet(EventSets db_es) {
		if (null == db_es) return null;
		com.relteq.sirius.jaxb.EventSet eset = factory.createEventSet();
		eset.setId(id2str(db_es.getId()));
		// TODO eset.setName();
		// TODO eset.setDescription();
		// TODO eset.getEvent().add();
		return eset;
	}
}
