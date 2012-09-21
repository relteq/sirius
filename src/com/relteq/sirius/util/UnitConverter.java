package com.relteq.sirius.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.relteq.sirius.jaxb.*;
import com.relteq.sirius.simulator.SiriusException;

/**
 * Converts scenario units
 * from US (mile, hour) or Metric (kilometer, hour)
 * to SI (meter, second).
 */
public class UnitConverter {
	/**
	 * Loads a scenario, performs unit conversion, saves the resulting scenario
	 * @param iconfig input scenario file
	 * @param oconfig output file
	 * @throws SiriusException
	 */
	public static void convertUnits(String iconfig, String oconfig) throws SiriusException {
		com.relteq.sirius.simulator.Scenario scenario = com.relteq.sirius.simulator.ObjectFactory.createAndLoadScenario(iconfig);
		process(scenario);
		scenario.saveToXML(oconfig);
	}

	/**
	 * Performs in-line unit conversion
	 * @param scenario
	 * @throws SiriusException
	 */
	public static void process(Scenario scenario) throws SiriusException {
		new UnitConverter(scenario).process();
	}

	private enum UnitSystem {UNKNOWN, US, METRIC};

	private Scenario scenario = null;
	private UnitSystem units;

	private UnitConverter(Scenario scenario) {
		this.scenario = scenario;
	}

	protected static final BigDecimal MILE = BigDecimal.valueOf(1609.344);
	protected static final BigDecimal KILOMETER = BigDecimal.valueOf(1000);
	protected static final BigDecimal HOUR = BigDecimal.valueOf(3600);

	private static final MathContext mathctx = new MathContext(6, RoundingMode.HALF_UP);

	private BigDecimal convertLength(BigDecimal length) {
		if (null == length) return null;
		switch (units) {
		case US: // miles -> meters
			return length.multiply(MILE);
		case METRIC: // kilometers -> meters
			return length.multiply(KILOMETER);
		default:
			return length;
		}
	}

	private BigDecimal convertDensity(BigDecimal density) {
		if (null == density) return null;
		switch (units) {
		case US: // vehicles/mile -> vehicles/meter
			return density.divide(MILE, mathctx);
		case METRIC: // vehicles/kilometer -> vehicles/meter
			return density.divide(KILOMETER);
		default:
			return density;
		}
	}

	private BigDecimal convertFlow(BigDecimal flow) {
		if (null == flow) return null;
		switch (units) {
		case US: // vehicles/hour -> vehicles/second
		case METRIC:
			return flow.divide(HOUR, mathctx);
		default:
			return flow;
		}
	}

	private BigDecimal convertSpeed(BigDecimal speed) {
		if (null == speed) return null;
		switch (units) {
		case US: // miles/hour -> meters/second
			return speed.multiply(MILE).divide(HOUR, mathctx);
		case METRIC: // kilometers/hour -> meters/second
			return speed.multiply(KILOMETER).divide(HOUR);
		default:
			return speed;
		}
	}

	private void process() throws SiriusException {
		String units = null;
		if (null != scenario.getSettings())
			units = scenario.getSettings().getUnits();
		if (null == units)
			throw new SiriusException("no units");
		else if (units.equalsIgnoreCase("US"))
			this.units = UnitSystem.US;
		else if (units.equalsIgnoreCase("Metric"))
			this.units = UnitSystem.METRIC;
		else {
			this.units = UnitSystem.UNKNOWN;
			throw new SiriusException("Invalid units '" + units + "'. Should be either 'US' or 'Metric'");
		}

		// settings: nothing to process
		process(scenario.getNetworkList());
		// signal list, sensor list: nothing to process
		process(scenario.getInitialDensitySet());
		// weaving factors, split ratios: nothing to process
		process(scenario.getDownstreamBoundaryCapacityProfileSet());
		process(scenario.getEventSet());
		process(scenario.getDemandProfileSet());
		process(scenario.getControllerSet());
		process(scenario.getFundamentalDiagramProfileSet());
		// network connections, destination networks, routes: nothing to process
	}

	private void process(NetworkList netlist) {
		if (null == netlist) return;
		for (Network network : netlist.getNetwork()) {
			// node list: nothing to process
			if (null != network.getLinkList())
				for (Link link : network.getLinkList().getLink()) {
					link.setLength(convertLength(link.getLength()));
				}
		}
	}

	private void process(InitialDensitySet idset) {
		if (null == idset) return;
		for (com.relteq.sirius.jaxb.Density density : idset.getDensity()) {
			Data1D data1d = new Data1D(density.getContent(), ":");
			if (!data1d.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (BigDecimal val : data1d.getData()) {
					if (0 < sb.length()) sb.append(':');
					sb.append(convertDensity(val).toPlainString());
				}
				density.setContent(sb.toString());
			}
		}
	}

	private void process(DownstreamBoundaryCapacityProfileSet dbcpset) {
		if (null == dbcpset) return;
		for (CapacityProfile cp : dbcpset.getCapacityProfile()) {
			// TODO delimiter = ':' or ','?
			Data1D data1d = new Data1D(cp.getContent(), ",");
			if (!data1d.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (BigDecimal val : data1d.getData()) {
					if (0 < sb.length()) sb.append(',');
					sb.append(convertFlow(val).toPlainString());
				}
				cp.setContent(sb.toString());
			}
		}
	}

	private void process(EventSet eset) {
		if (null == eset) return;
		for (Event event : eset.getEvent())
			process(event.getParameters());
	}

	private void process(Parameters params) {
		if (null == params) return;
		for (Parameter param : params.getParameter())
			process(param);
	}

	private void process(Parameter param) {
		if (null == param.getName() || null == param.getValue()) return;
		final String name = param.getName();
		String value = param.getValue();
		BigDecimal converted = null;
		if (name.equals("capacity") || name.equals("capacity_drop"))
			converted = convertFlow(new BigDecimal(value));
		else if (name.equals("free_flow_speed") || name.equals("congestion_speed"))
			converted = convertSpeed(new BigDecimal(value));
		else if (name.equals("jam_density"))
			converted = convertDensity(new BigDecimal(value));
		else if (name.startsWith("gain"))
			converted = convertSpeed(new BigDecimal(value));
		else return;
		// TODO check if all the parameters are processed correctly
		if (null != converted)
			param.setValue(converted.toPlainString());
	}

	private void process(DemandProfileSet dpset) {
		if (null == dpset) return;
		for (DemandProfile dp : dpset.getDemandProfile()) {
			Data2D data2d = new Data2D(dp.getContent(), new String[] {",", ":"});
			if (!data2d.isEmpty()) {
				BigDecimal[][] data = data2d.getData();
				StringBuilder sb = new StringBuilder();
				for (int t = 0; t < data.length; ++t) {
					if (0 < t) sb.append(',');
					for (int vtn = 0; vtn < data[t].length; ++vtn) {
						if (0 < vtn) sb.append(':');
						sb.append(convertFlow(data[t][vtn]).toPlainString());
					}
				}
				dp.setContent(sb.toString());
			}
		}
	}

	private void process(ControllerSet cset) {
		if (null == cset) return;
		for (Controller controller : cset.getController()) {
			process(controller.getParameters());
			process(controller.getQueueController());
			// TODO process controller table
		}
	}

	private void process(QueueController qcontroller) {
		if (null == qcontroller) return;
		process(qcontroller.getParameters());
	}

	private void process(FundamentalDiagramProfileSet fdpset) {
		if (null == fdpset) return;
		for (FundamentalDiagramProfile fdprofile : fdpset.getFundamentalDiagramProfile())
			for (FundamentalDiagram fd : fdprofile.getFundamentalDiagram())
				process(fd);
	}

	private void process(FundamentalDiagram fd) {
		fd.setFreeFlowSpeed(convertSpeed(fd.getFreeFlowSpeed()));
		fd.setCriticalSpeed(convertSpeed(fd.getCriticalSpeed()));
		fd.setCongestionSpeed(convertSpeed(fd.getCongestionSpeed()));
		fd.setCapacity(convertFlow(fd.getCapacity()));
		if (null != fd.getJamDensity())
			fd.setJamDensity(convertDensity(fd.getJamDensity()));
		if (null != fd.getCapacityDrop())
			fd.setCapacityDrop(convertFlow(fd.getCapacityDrop()));
		if (null != fd.getStdDevCapacity())
			fd.setStdDevCapacity(convertFlow(fd.getStdDevCapacity()));
		if (null != fd.getStdDevFreeFlowSpeed())
			fd.setStdDevFreeFlowSpeed(convertSpeed(fd.getStdDevFreeFlowSpeed()));
		if (null != fd.getStdDevCongestionSpeed())
			fd.setStdDevCongestionSpeed(convertSpeed(fd.getStdDevCongestionSpeed()));
	}
}
