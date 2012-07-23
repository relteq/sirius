package com.relteq.sirius.db;

import java.sql.Connection;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.torque.NoRowsException;
import org.apache.torque.TorqueException;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Transaction;

import com.relteq.sirius.om.LinkDataDetailedPeer;
import com.relteq.sirius.om.LinkDataTotalPeer;
import com.relteq.sirius.om.LinkPerformanceDetailedPeer;
import com.relteq.sirius.om.LinkPerformanceTotalPeer;
import com.relteq.sirius.om.ScenariosPeer;
import com.relteq.sirius.om.SimulationRuns;
import com.relteq.sirius.om.SimulationRunsPeer;
import com.relteq.sirius.simulator.SiriusException;

public class Cleaner {

	/**
	 * Initialize the DB service if it hasn't been initialized yet
	 * @throws SiriusException
	 */
	private static void initDB() throws SiriusException {
		if (!com.relteq.sirius.db.Service.isInit()) com.relteq.sirius.db.Service.init();
	}

	private static Logger logger = Logger.getLogger(Cleaner.class);

	private static String select2delete(String query) {
		if (query.startsWith("SELECT")) return query.replaceFirst("SELECT", "DELETE");
		else {
			logger.warn("No SELECT in '" + query + "'");
			return query;
		}
	}

	private static void executeStatement(String statement, Connection conn) throws TorqueException {
		logger.debug(statement);
		BasePeer.executeStatement(statement, conn);
	}

	public static void clearProcessed(int scenario_id) throws SiriusException {
		initDB();
		Connection conn = null;
		try {
			conn = Transaction.begin();
			try {
				ScenariosPeer.retrieveByPK(scenario_id, conn);
			} catch (NoRowsException exc) {
				throw new SiriusException("Scenario '" + scenario_id + "\' does not exist", exc);
			}

			Criteria crit = new Criteria();
			crit.add(SimulationRunsPeer.SCENARIO_ID, scenario_id);
			crit.addAscendingOrderByColumn(SimulationRunsPeer.RUN_NUMBER);
			@SuppressWarnings("unchecked")
			List<SimulationRuns> db_sr_l = SimulationRunsPeer.doSelect(crit, conn);

			for (SimulationRuns db_sr : db_sr_l) {
				logger.info("Run number: " + db_sr.getRunNumber());

				crit.clear();
				crit.add(LinkDataTotalPeer.DATA_SOURCE_ID, db_sr.getDataSourceId());
				crit.add(LinkDataTotalPeer.AGGREGATION, (Object) "raw", Criteria.NOT_EQUAL);
				executeStatement(select2delete(LinkDataTotalPeer.createQueryString(crit)), conn);

				crit.clear();
				crit.add(LinkDataDetailedPeer.DATA_SOURCE_ID, db_sr.getDataSourceId());
				crit.add(LinkDataDetailedPeer.AGGREGATION, (Object) "raw", Criteria.NOT_EQUAL);
				executeStatement(select2delete(LinkDataDetailedPeer.createQueryString(crit)), conn);

				crit.clear();
				crit.add(LinkPerformanceTotalPeer.DATA_SOURCE_ID, db_sr.getDataSourceId());
				executeStatement(select2delete(LinkPerformanceTotalPeer.createQueryString(crit)), conn);

				crit.clear();
				crit.add(LinkPerformanceDetailedPeer.DATA_SOURCE_ID, db_sr.getDataSourceId());
				executeStatement(select2delete(LinkPerformanceDetailedPeer.createQueryString(crit)), conn);
			}

			Transaction.commit(conn);
			conn = null;
		} catch (TorqueException exc) {
			throw new SiriusException(exc);
		} finally {
			if (null != conn) {
				Transaction.safeRollback(conn);
				conn = null;
			}
		}
	}

}
