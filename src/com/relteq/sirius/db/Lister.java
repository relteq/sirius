package com.relteq.sirius.db;

import java.util.Iterator;
import java.util.List;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

import com.relteq.sirius.om.Scenarios;
import com.relteq.sirius.om.ScenariosPeer;
import com.relteq.sirius.simulator.SiriusException;

/**
 * Implements "list" commands
 */
public class Lister {

	public static void listScenarios() throws SiriusException {
		try {
			com.relteq.sirius.db.Service.init();
			@SuppressWarnings("unchecked")
			List<Scenarios> db_scenarios = ScenariosPeer.doSelect(new Criteria());
			for (Iterator<Scenarios> iter = db_scenarios.iterator(); iter.hasNext();) {
				Scenarios db_scenario = iter.next();
				StringBuilder sb = new StringBuilder(db_scenario.getId());
				if (null != db_scenario.getName())
					sb.append(" ").append(db_scenario.getName());
				if (null != db_scenario.getDescription())
					sb.append(" ").append(db_scenario.getDescription());
				System.out.println(sb.toString());
			}
			com.relteq.sirius.db.Service.shutdown();
		} catch (TorqueException exc) {
			throw new SiriusException(exc.getMessage(), exc);
		}
	}

}
