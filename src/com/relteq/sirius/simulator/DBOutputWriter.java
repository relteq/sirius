package com.relteq.sirius.simulator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Calendar;
import java.util.List;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Transaction;

import com.relteq.sirius.om.*;

/**
 * Database output writer
 */
public class DBOutputWriter extends OutputWriterBase {
	private Connection conn = null;
	private String data_source_id = null;

	public DBOutputWriter(Scenario scenario) {
		super(scenario);
	}

	private void createDataSource() throws TorqueException {
		Connection conn = null;
		try {
			conn = Transaction.begin();

			com.relteq.sirius.om.DataSources db_ds = new com.relteq.sirius.om.DataSources();
			db_ds.setId(data_source_id = com.relteq.sirius.db.util.UUID.generate());
			db_ds.save(conn);

			Criteria crit = new Criteria();
			crit.add(com.relteq.sirius.om.SimulationRunsPeer.SCENARIO_ID, getScenario().getId());
			crit.addDescendingOrderByColumn(com.relteq.sirius.om.SimulationRunsPeer.RUN_NUMBER);
			// TODO limit the number of rows
			@SuppressWarnings("unchecked")
			List<com.relteq.sirius.om.SimulationRuns> db_sr_l = com.relteq.sirius.om.SimulationRunsPeer.doSelect(crit);
			final int run_number = db_sr_l.isEmpty() ? 1 : db_sr_l.get(0).getRunNumber() + 1;

			com.relteq.sirius.om.SimulationRuns db_sr = new com.relteq.sirius.om.SimulationRuns();
			db_sr.setDataSources(db_ds);
			db_sr.setScenarioId(getScenario().getId());
			db_sr.setRunNumber(run_number);
			db_sr.setStartTime(Calendar.getInstance().getTime());
			db_sr.setStatus(-1);
			db_sr.save(conn);

			Transaction.commit(conn);
			conn = null;
		} finally {
			if (null != conn) {
				Transaction.safeRollback(conn);
				data_source_id = null;
			}
		}
	}

	@Override
	public void open() throws SiriusException {
		try {
			createDataSource();
			conn = Transaction.begin();
		} catch (TorqueException exc) {
			throw new SiriusException(exc.getMessage(), exc.getCause());
		}
	}

	@Override
	public void recordstate(double time, boolean exportflows, int outsteps) throws SiriusException {
		boolean firststep = 0 == scenario.clock.getCurrentstep();
		double invsteps = firststep ? 1.0d : 1.0d / outsteps;
		Calendar ts = Calendar.getInstance();
		double min = Math.floor(time / 60);
		double hrs = Math.floor(min / 60);
		ts.set(Calendar.HOUR_OF_DAY, (int) hrs);
		ts.set(Calendar.MINUTE, (int) (min - hrs * 60));
		ts.set(Calendar.SECOND, (int) (time - min * 60));
		for (com.relteq.sirius.jaxb.Network network : scenario.getNetworkList().getNetwork()) {
			for (com.relteq.sirius.jaxb.Link link : network.getLinkList().getLink()) {
				LinkDataTotal data = new LinkDataTotal();
				try {
					data.setLinkId(link.getId());
					data.setNetworkId(network.getId());
					data.setDataSourceId(data_source_id);
				} catch (TorqueException exc) {
					throw new SiriusException(exc.getMessage(), exc.getCause());
				}
				data.setTs(ts.getTime());
				data.setAggregation("raw");
				Link _link = (Link) link;
				if (exportflows) {
					data.setInFlow(new BigDecimal(SiriusMath.sum(_link.cumulative_inflow[0])));
					data.setOutFlow(new BigDecimal(SiriusMath.sum(_link.cumulative_outflow[0])));
				}
				data.setDensity(new BigDecimal(SiriusMath.sum(_link.cumulative_density[0]) * invsteps));
				double ffspeed = _link.getVfInMPH(0);
				if (!Double.isNaN(ffspeed)) data.setFreeFlowSpeed(new BigDecimal(ffspeed));
				double capacity = _link.getCapacityInVPH(0);
				if (!Double.isNaN(capacity)) data.setCapacity(new BigDecimal(capacity));
				_link.reset_cumulative();
				try {
					data.save(conn);
				} catch (TorqueException exc) {
					throw new SiriusException(exc.getMessage(), exc.getCause());
				}
			}
		}
	}

	@Override
	public void close() {
		boolean success = false;
		try {
			Transaction.commit(conn);
			conn = null;
			success = true;
		} catch (TorqueException exc) {
			exc.printStackTrace();
		} finally {
			if (null != conn) {
				Transaction.safeRollback(conn);
				conn = null;
			}
		}
		if (null != data_source_id)
			try {
				com.relteq.sirius.om.SimulationRuns db_sr = com.relteq.sirius.om.SimulationRunsPeer.retrieveByPK(data_source_id);
				db_sr.setEndTime(Calendar.getInstance().getTime());
				db_sr.setStatus(success ? 0 : 1);
				db_sr.save();
			} catch (Exception exc) {
				exc.printStackTrace();
			} finally {
				data_source_id = null;
			}
	}

}
