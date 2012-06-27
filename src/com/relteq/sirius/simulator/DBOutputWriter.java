package com.relteq.sirius.simulator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Calendar;

import org.apache.torque.TorqueException;
import org.apache.torque.util.Transaction;

import com.relteq.sirius.om.*;

/**
 * Database output writer
 */
public class DBOutputWriter extends OutputWriterBase {
	private Connection conn = null;

	public DBOutputWriter(Scenario scenario) {
		super(scenario);
	}

	@Override
	public void open() throws SiriusException {
		try {
			conn = Transaction.begin();
		} catch (TorqueException exc) {
			exc.printStackTrace();
			throw new SiriusException(exc.getMessage());
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
				data.setLinkId(link.getId());
				data.setNetworkId(network.getId());
				data.setScenarioId(scenario.getId());
				data.setRunId(getRunId());
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
		try {
			Transaction.commit(conn);
		} catch (TorqueException exc) {
			exc.printStackTrace();
			Transaction.safeRollback(conn);
		}
		conn = null;
	}

}
