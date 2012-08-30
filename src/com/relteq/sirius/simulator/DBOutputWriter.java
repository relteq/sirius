package com.relteq.sirius.simulator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
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

	private static Logger logger = Logger.getLogger(DBOutputWriter.class);

	private Scenarios db_scenario = null;

	private void createDataSource() throws TorqueException {
		Connection conn = null;
		try {
			conn = Transaction.begin();

			if (null == db_scenario)
				db_scenario = ScenariosPeer.retrieveByPK(Integer.parseInt(getScenario().getId()), conn);

			com.relteq.sirius.om.DataSources db_ds = new com.relteq.sirius.om.DataSources();
			db_ds.setId(data_source_id = com.relteq.sirius.db.util.UUID.generate());
			db_ds.save(conn);

			Criteria crit = new Criteria();
			crit.addDescendingOrderByColumn(com.relteq.sirius.om.SimulationRunsPeer.RUN_NUMBER);
			crit.setLimit(1);
			@SuppressWarnings("unchecked")
			List<com.relteq.sirius.om.SimulationRuns> db_sr_l = db_scenario.getSimulationRunss(crit);
			final int run_number = db_sr_l.isEmpty() ? 1 : db_sr_l.get(0).getRunNumber() + 1;
			logger.info("Run number: " + run_number);

			com.relteq.sirius.om.SimulationRuns db_sr = new com.relteq.sirius.om.SimulationRuns();
			db_sr.setDataSources(db_ds);
			db_sr.setScenarios(db_scenario);
			db_sr.setRunNumber(run_number);
			db_sr.setExecutionStartTime(Calendar.getInstance().getTime());
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

	java.util.Map<String, String> vt_name2id = null;

	private String getVehicleTypeId(String name, Connection conn) throws SiriusException {
		if (null == vt_name2id) {
			vt_name2id = new java.util.TreeMap<String, String>();
			logger.info("Loading vehicle type IDs");
			Criteria crit = new Criteria();
			crit.addJoin(VehicleTypesPeer.VEHICLE_TYPE_ID, VehicleTypesInListsPeer.VEHICLE_TYPE_ID);
			crit.add(VehicleTypesInListsPeer.VEHICLE_TYPE_LIST_ID, db_scenario.getVehicleTypeListId());
			try {
				@SuppressWarnings("unchecked")
				List<VehicleTypes> db_vt_l = VehicleTypesPeer.doSelect(crit, conn);
				for (VehicleTypes db_vt : db_vt_l)
					vt_name2id.put(db_vt.getName(), db_vt.getVehicleTypeId());
			} catch (TorqueException exc) {
				throw new SiriusException(exc);
			}
		}
		return vt_name2id.get(name);
	}

	@Override
	public void open(int run_id) throws SiriusException {
		if (1 != scenario.numEnsemble)
			logger.warn("scenario.numEnsembles != 1");
		try {
			createDataSource();
			conn = Transaction.begin();
		} catch (TorqueException exc) {
			throw new SiriusException(exc);
		}
		ts = Calendar.getInstance();
		ts.set(Calendar.MILLISECOND, 0);
	}

	private Calendar ts = null;

	@Override
	public void recordstate(double time, boolean exportflows, int outsteps) throws SiriusException {
		boolean firststep = 0 == scenario.clock.getCurrentstep();
		double invsteps = firststep ? 1.0d : 1.0d / outsteps;
		double min = Math.floor(time / 60);
		double hrs = Math.floor(min / 60);
		ts.set(Calendar.HOUR_OF_DAY, (int) hrs);
		ts.set(Calendar.MINUTE, (int) (min - hrs * 60));
		ts.set(Calendar.SECOND, (int) (time - min * 60));
		// output period, hr
		double dt = scenario.getSimDtInSeconds() * outsteps / 3600.0d;

		for (com.relteq.sirius.jaxb.Network network : scenario.getNetworkList().getNetwork()) {
			for (com.relteq.sirius.jaxb.Link link : network.getLinkList().getLink()) {
				Link _link = (Link) link;
				try {
					LinkDataTotal db_ldt = fill_total(_link, exportflows, invsteps, dt);
					fill_detailed(_link, exportflows, invsteps, dt, db_ldt.getSpeed());
				} catch (TorqueException exc) {
					throw new SiriusException(exc);
				} finally {
					_link.reset_cumulative();
				}
			}
		}
	}

	/**
	 * Fills link_data_total table
	 * @param link
	 * @param exportflows
	 * @param invsteps
	 * @param dt
	 * @return the stored row
	 * @throws TorqueException
	 */
	private LinkDataTotal fill_total(Link link, boolean exportflows, double invsteps, double dt) throws TorqueException {
		LinkDataTotal db_ldt = new LinkDataTotal();
		db_ldt.setLinkId(link.getId());
		db_ldt.setNetworkId(link.myNetwork.getId());
		db_ldt.setDataSourceId(data_source_id);
		db_ldt.setTs(ts.getTime());
		db_ldt.setAggregation("raw");
		// mean density, veh
		double density = SiriusMath.sum(link.cumulative_density[0]) * invsteps;
		db_ldt.setDensity(new BigDecimal(density));
		// free flow speed, mph
		double ffspeed = link.getVfInMPH(0);
		if (!Double.isNaN(ffspeed)) db_ldt.setFreeFlowSpeed(new BigDecimal(ffspeed));
		if (exportflows) {
			db_ldt.setInFlow(new BigDecimal(SiriusMath.sum(link.cumulative_inflow[0])));
			// output flow, veh
			double outflow = SiriusMath.sum(link.cumulative_outflow[0]);
			db_ldt.setOutFlow(new BigDecimal(outflow));
			// speed, mph
			double speed = Double.NaN;
			if (density <= 0)
				speed = ffspeed;
			else {
				speed = outflow * link.getLengthInMiles() / (dt * density);
				if (!Double.isNaN(ffspeed) && speed > ffspeed) speed = ffspeed;
			}
			if (!Double.isNaN(speed)) db_ldt.setSpeed(new BigDecimal(speed));
		}
		// maximum flow, vph
		double capacity = link.getCapacityInVPH(0);
		if (!Double.isNaN(capacity)) db_ldt.setCapacity(new BigDecimal(capacity));
		db_ldt.save(conn);
		return db_ldt;
	}

	/**
	 * Fills link_data_detailed table
	 * @param link
	 * @param exportflows
	 * @param invsteps
	 * @param dt
	 * @param total_speed
	 * @throws TorqueException
	 * @throws SiriusException
	 */
	private void fill_detailed(Link link, boolean exportflows, double invsteps, double dt, BigDecimal total_speed) throws TorqueException, SiriusException {
		for (int vt_ind = 0; vt_ind < scenario.getNumVehicleTypes(); ++vt_ind) {
			LinkDataDetailed db_ldd = new LinkDataDetailed();
			db_ldd.setLinkId(link.getId());
			db_ldd.setNetworkId(link.myNetwork.getId());
			db_ldd.setDataSourceId(data_source_id);
			db_ldd.setVehicleTypeId(getVehicleTypeId(scenario.getVehicleTypeNames()[vt_ind], conn));
			db_ldd.setTs(ts.getTime());
			db_ldd.setAggregation("raw");
			double density = link.cumulative_density[0][vt_ind] * invsteps;
			db_ldd.setDensity(new BigDecimal(density));
			if (exportflows) {
				db_ldd.setInFlow(new BigDecimal(link.cumulative_inflow[0][vt_ind]));
				double outflow = link.cumulative_outflow[0][vt_ind];
				db_ldd.setOutFlow(new BigDecimal(outflow));
				if (density <= 0)
					db_ldd.setSpeed(total_speed);
				else {
					// speed, mph
					double speed = outflow * link.getLengthInMiles() / (dt * density);
					// free flow speed, mph
					double ffspeed = link.getVfInMPH(0);
					if (!Double.isNaN(ffspeed) && speed > ffspeed) speed = ffspeed;
					db_ldd.setSpeed(new BigDecimal(speed));
				}
			}
			db_ldd.save(conn);
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
		ts = null;
		if (null != data_source_id)
			try {
				com.relteq.sirius.om.SimulationRuns db_sr = com.relteq.sirius.om.SimulationRunsPeer.retrieveByPK(data_source_id);
				db_sr.setExecutionEndTime(Calendar.getInstance().getTime());
				db_sr.setStatus(success ? 0 : 1);
				db_sr.save();
			} catch (Exception exc) {
				exc.printStackTrace();
			} finally {
				data_source_id = null;
			}
	}

}
