package com.relteq.sirius.db;

import java.text.DateFormat;
import java.util.List;

import org.apache.torque.NoRowsException;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

import com.relteq.sirius.om.Scenarios;
import com.relteq.sirius.om.ScenariosPeer;
import com.relteq.sirius.om.SimulationRuns;
import com.relteq.sirius.simulator.SiriusException;

/**
 * Implements "list" commands
 */
public class Lister {
	/**
	 * Initialize the DB service if it hasn't been initialized yet
	 * @throws SiriusException
	 */
	private static void initDB() throws SiriusException {
		if (!com.relteq.sirius.db.Service.isInit()) com.relteq.sirius.db.Service.init();
	}

	public static void listScenarios() throws SiriusException {
		initDB();
		try {
			@SuppressWarnings("unchecked")
			List<Scenarios> db_scenarios = ScenariosPeer.doSelect(new Criteria());
			for (Scenarios db_scenario : db_scenarios) {
				StringBuilder sb = new StringBuilder(db_scenario.getId());
				if (null != db_scenario.getName())
					sb.append(" ").append(db_scenario.getName());
				if (null != db_scenario.getDescription())
					sb.append(" ").append(db_scenario.getDescription());
				System.out.println(sb.toString());
			}
		} catch (TorqueException exc) {
			throw new SiriusException(exc.getMessage(), exc);
		}
	}

	public static void listRuns(String scenario_id) throws SiriusException {
		initDB();
		try {
			Scenarios db_scenario = ScenariosPeer.retrieveByPK(scenario_id);
			DateFormat date_format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
			@SuppressWarnings("unchecked")
			List<SimulationRuns> db_run_l = db_scenario.getSimulationRunss();
			for (SimulationRuns db_sr : db_run_l) {
				StringBuilder sb = new StringBuilder(String.format("%2d", db_sr.getRunNumber()));
				if (null != db_sr.getStartTime()) {
					sb.append("\t" + date_format.format(db_sr.getStartTime()));
					if (null != db_sr.getEndTime())
						sb.append(" -- " + date_format.format(db_sr.getEndTime()));
				}
				System.out.println(sb.toString());
			}
		} catch (NoRowsException exc) {
			throw new SiriusException("Scenario '" + scenario_id + "\' does not exist");
		} catch (TorqueException exc) {
			throw new SiriusException(exc.getMessage(), exc);
		}
	}

}
