package com.relteq.sirius.db.exporter;
import java.util.Iterator;
import java.util.List;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

import com.relteq.sirius.om.NetworkListsPeer;
import com.relteq.sirius.om.Networks;
import com.relteq.sirius.om.NetworksPeer;
import com.relteq.sirius.om.Scenarios;
import com.relteq.sirius.om.ScenariosPeer;
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
			return scenario;
		} catch (TorqueException exc) {
			throw new SiriusException(exc.getMessage(), exc.getCause());
		}
	}

	private com.relteq.sirius.jaxb.Settings restoreSettings(Scenarios db_scenario) {
		com.relteq.sirius.jaxb.Settings settings = (com.relteq.sirius.jaxb.Settings) factory.createSettings();
		if (null != db_scenario.getVehicleTypeListId()) {
			com.relteq.sirius.jaxb.VehicleTypes vts = factory.createVehicleTypes();
			List<com.relteq.sirius.jaxb.VehicleType> vtl = vts.getVehicleType();
			Criteria crit = new Criteria();
			crit.add(VehicleTypesPeer.VEHICLE_TYPE_LIST_ID, db_scenario.getVehicleTypeListId());
			crit.addAscendingOrderByColumn(VehicleTypesPeer.VEHICLE_TYPE_LIST_ID);
			try {
				@SuppressWarnings("unchecked")
				List<VehicleTypes> db_vtl = VehicleTypesPeer.doSelect(crit);
				for (Iterator<VehicleTypes> iter = db_vtl.iterator(); iter.hasNext();) {
					VehicleTypes db_vt = iter.next();
					com.relteq.sirius.jaxb.VehicleType vt = factory.createVehicleType();
					vt.setName(db_vt.getName());
					vt.setWeight(db_vt.getWeight());
					vtl.add(vt);
				}
			} catch (TorqueException exc) {
				SiriusErrorLog.addErrorMessage(exc.getMessage());
			}
			settings.setVehicleTypes(vts);
		}
		return settings;
	}

	private com.relteq.sirius.jaxb.NetworkList restoreNetworkList(Scenarios db_scenario) {
		Criteria crit = new Criteria();
		crit.add(NetworkListsPeer.SCENARIO_ID, db_scenario.getId());
		crit.addJoin(NetworkListsPeer.NETWORK_ID, NetworksPeer.ID);
		try {
			@SuppressWarnings("unchecked")
			List<Networks> db_netl = NetworksPeer.doSelect(crit);
			if (0 < db_netl.size()) {
				com.relteq.sirius.jaxb.NetworkList nets = factory.createNetworkList();
				List<com.relteq.sirius.jaxb.Network> netl = nets.getNetwork();
				for (Iterator<Networks> iter = db_netl.iterator(); iter.hasNext();) {
					Networks db_net = iter.next();
					com.relteq.sirius.jaxb.Network net = factory.createNetwork();
					net.setId(db_net.getId());
					net.setDescription(db_net.getDescription());
					netl.add(net);
				}
				return nets;
			}
		} catch (TorqueException exc) {
			SiriusErrorLog.addErrorMessage(exc.getMessage());
		}
		return null;
	}

}
