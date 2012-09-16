package com.relteq.sirius.simulator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.torque.NoRowsException;
import org.apache.torque.TooManyRowsException;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;
import org.apache.torque.util.Transaction;

import com.relteq.sirius.om.*;
import com.workingdogs.village.DataSetException;

/**
 * Database output writer
 */
public class DBOutputWriter extends OutputWriterBase {

	public DBOutputWriter(Scenario scenario) {
		super(scenario);
		try {
			db_scenario = ScenariosPeer.retrieveByPK(str2id(scenario.getId()));
		} catch (NoRowsException exc) {
			logger.error("Scenario " + str2id(scenario.getId()) + " was not found in the database");
		} catch (TooManyRowsException exc) {
			logger.error("Data integrity violation", exc);
		} catch (TorqueException exc) {
			logger.error("Could not load scenario " + str2id(scenario.getId()), exc);
		}
		db_vehicle_type = new VehicleTypes[scenario.getNumVehicleTypes()];
		if (null != db_scenario) {
			logger.info("Loading vehicle types");
			Criteria crit = new Criteria();
			crit.addJoin(VehicleTypesPeer.VEHICLE_TYPE_ID, VehicleTypesInSetsPeer.VEHICLE_TYPE_ID);
			crit.add(VehicleTypesInSetsPeer.VEHICLE_TYPE_SET_ID, db_scenario.getVehicleTypeSetId());
			try {
				@SuppressWarnings("unchecked")
				List<VehicleTypes> db_vt_l = VehicleTypesPeer.doSelect(crit);
				for (VehicleTypes db_vt : db_vt_l)
					for (int i = 0; i < scenario.getNumVehicleTypes(); ++i)
						if (db_vt.getName().equals(scenario.getVehicleTypeNames()[i]))
							db_vehicle_type[i] = db_vt;
			} catch (TorqueException exc) {
				logger.error("Failed to load vehicle types for scenario " + db_scenario.getId(), exc);
			}
		}
	}

	private static Logger logger = Logger.getLogger(DBOutputWriter.class);

	private Scenarios db_scenario = null;
	VehicleTypes[] db_vehicle_type;
	private SimulationRuns db_simulation_run = null;

	private Long str2id(String id) {
		return Long.parseLong(id, 10);
	}

	boolean success = false;

	private Calendar ts = null;

	@Override
	public void open(int run_id) throws SiriusException {
		success = false;
		if (1 != scenario.numEnsemble)
			logger.warn("scenario.numEnsembles != 1");
		if (null == db_scenario)
			throw new SiriusException("Scenario was not loaded from the database");

		logger.info("Initializing simulation run");
		Connection conn = null;
		try {
			conn = Transaction.begin();

			DataSources db_ds = new DataSources();
			db_ds.setId(DataSourcesPeer.nextId(DataSourcesPeer.ID, conn));
			db_ds.save(conn);

			Criteria crit = new Criteria();
			crit.add(ScenariosPeer.ID, db_scenario.getId());
			com.workingdogs.village.Value max_runnum = SimulationRunsPeer.maxColumnValue(SimulationRunsPeer.RUN_NUMBER, crit, conn);
			final long run_number = null == max_runnum ? 1 : max_runnum.asLong() + 1;
			logger.info("Run number: " + run_number);

			db_simulation_run = new com.relteq.sirius.om.SimulationRuns();
			db_simulation_run.setDataSources(db_ds);
			db_simulation_run.setScenarios(db_scenario);
			db_simulation_run.setRunNumber(run_number);
			db_simulation_run.setVersion(com.relteq.sirius.Version.get().getEngineVersion());
			db_simulation_run.setBuild("");
			db_simulation_run.setSimulationStartTime(BigDecimal.valueOf(scenario.getTimeStart()));
			db_simulation_run.setSimulationDuration(BigDecimal.valueOf(scenario.getTimeEnd() - scenario.getTimeStart()));
			db_simulation_run.setSimulationDt(BigDecimal.valueOf(scenario.getSimDtInSeconds()));
			db_simulation_run.setOutputDt(BigDecimal.valueOf(scenario.getOutputDt()));
			db_simulation_run.setExecutionStartTime(Calendar.getInstance().getTime());
			db_simulation_run.setStatus(-1);
			db_simulation_run.save(conn);

			Transaction.commit(conn);
			conn = null;
			success = true;
		} catch (TorqueException exc) {
			throw new SiriusException(exc);
		} catch (DataSetException exc) {
			throw new SiriusException(exc);
		} finally {
			if (null != conn) {
				Transaction.safeRollback(conn);
				db_simulation_run = null;
			}
		}
		ts = Calendar.getInstance();
		ts.set(Calendar.MILLISECOND, 0);
	}

	@Override
	public void recordstate(double time, boolean exportflows, int outsteps) throws SiriusException {
		success = false;
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
				} catch (Exception exc) {
					throw new SiriusException(exc);
				} finally {
					_link.reset_cumulative();
				}
			}
		}
		success = true;
	}

	/**
	 * Fills link_data_total table
	 * @param link
	 * @param exportflows
	 * @param invsteps
	 * @param dt
	 * @return the stored row
	 * @throws Exception
	 */
	private LinkDataTotal fill_total(Link link, boolean exportflows, double invsteps, double dt) throws Exception {
		LinkDataTotal db_ldt = new LinkDataTotal();
		db_ldt.setLinkId(str2id(link.getId()));
		db_ldt.setNetworkId(str2id(link.myNetwork.getId()));
		db_ldt.setDataSources(db_simulation_run.getDataSources());
		db_ldt.setTs(ts.getTime());
		db_ldt.setAggregation("raw");
		db_ldt.setType("mean");
		db_ldt.setCellNumber(Integer.valueOf(0));
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
		// congestion wave speed, mph
		double cwspeed = link.getWInMPH(0);
		if (!Double.isNaN(cwspeed)) db_ldt.setCongestionWaveSpeed(new BigDecimal(cwspeed));
		// maximum flow, vph
		double capacity = link.getCapacityInVPH(0);
		if (!Double.isNaN(capacity)) db_ldt.setCapacity(new BigDecimal(capacity));
		// jam density, vehicle per mile per lane
		double jamdens = link.getDensityJamInVPMPL(0);
		if (!Double.isNaN(jamdens)) db_ldt.setJamDensity(new BigDecimal(jamdens));
		// capacity drop, vehicle per hour per lane
		double capdrop = link.getCapacityDropInVPHPL(0);
		if (!Double.isNaN(capdrop)) db_ldt.setCapacityDrop(new BigDecimal(capdrop));
		FundamentalDiagram fd = link.currentFD(0);
		if (null != fd) db_ldt.setCriticalSpeed(fd.getCriticalSpeed());
		db_ldt.save();
		return db_ldt;
	}

	/**
	 * Fills link_data_detailed table
	 * @param link
	 * @param exportflows
	 * @param invsteps
	 * @param dt
	 * @param total_speed
	 * @throws Exception
	 */
	private void fill_detailed(Link link, boolean exportflows, double invsteps, double dt, BigDecimal total_speed) throws Exception {
		for (int vt_ind = 0; vt_ind < db_vehicle_type.length; ++vt_ind) {
			LinkDataDetailed db_ldd = new LinkDataDetailed();
			db_ldd.setLinkId(str2id(link.getId()));
			db_ldd.setNetworkId(str2id(link.myNetwork.getId()));
			db_ldd.setDataSources(db_simulation_run.getDataSources());
			db_ldd.setVehicleTypes(db_vehicle_type[vt_ind]);
			db_ldd.setTs(ts.getTime());
			db_ldd.setAggregation("raw");
			db_ldd.setType("mean");
			db_ldd.setCellNumber(Integer.valueOf(0));
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
			db_ldd.save();
		}
	}

	@Override
	public void close() {
		ts = null;
		if (null != db_simulation_run) {
			db_simulation_run.setExecutionEndTime(Calendar.getInstance().getTime());
			db_simulation_run.setStatus(success ? 0 : 1);
			try {
				db_simulation_run.save();
			} catch (Exception exc) {
				logger.error("Failed to update simulation run status", exc);
			} finally {
				db_simulation_run = null;
			}
		}
	}

}
