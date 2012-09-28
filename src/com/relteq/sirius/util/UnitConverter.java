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

	private Scenario scenario = null;

	private UnitConverter(Scenario scenario) {
		this.scenario = scenario;
	}

	/** Abstract unit converter */
	protected static abstract class Converter {
		public enum UnitSystem {UNKNOWN, US, METRIC};

		protected static final BigDecimal MILE = BigDecimal.valueOf(1609.344);
		protected static final BigDecimal KILOMETER = BigDecimal.valueOf(1000);
		protected static final BigDecimal HOUR = BigDecimal.valueOf(3600);

		protected static final MathContext mathctx = new MathContext(6, RoundingMode.HALF_UP);

		public static UnitSystem getUnitSystem(String units) {
			if (units.equalsIgnoreCase("US"))
				return UnitSystem.US;
			else if (units.equalsIgnoreCase("Metric"))
				return UnitSystem.METRIC;
			else
				return UnitSystem.UNKNOWN;
		}

		protected UnitSystem usystem;

		public Converter(UnitSystem usystem) {
			this.usystem = usystem;
		}

		public abstract BigDecimal convert(BigDecimal value);
	}

	protected static class LengthConverter extends Converter {
		public LengthConverter(UnitSystem usystem) {
			super(usystem);
		}

		@Override
		public BigDecimal convert(BigDecimal length) {
			if (null == length) return null;
			switch (usystem) {
			case US: // miles -> meters
				return length.multiply(MILE);
			case METRIC: // kilometers -> meters
				return length.multiply(KILOMETER);
			default:
				return length;
			}
		}
	}

	protected static class DensityConverter extends Converter {
		public DensityConverter(UnitSystem usystem) {
			super(usystem);
		}

		@Override
		public BigDecimal convert(BigDecimal density) {
			if (null == density) return null;
			switch (usystem) {
			case US: // vehicles/mile -> vehicles/meter
				return density.divide(MILE, mathctx);
			case METRIC: // vehicles/kilometer -> vehicles/meter
				return density.divide(KILOMETER);
			default:
				return density;
			}
		}
	}

	protected static class FlowConverter extends Converter {
		public FlowConverter(UnitSystem usystem) {
			super(usystem);
		}

		@Override
		public BigDecimal convert(BigDecimal flow) {
			if (null == flow) return null;
			switch (usystem) {
			case US: // vehicles/hour -> vehicles/second
			case METRIC:
				return flow.divide(HOUR, mathctx);
			default:
				return flow;
			}
		}
	}

	protected static class SpeedConverter extends Converter {
		public SpeedConverter(UnitSystem usystem) {
			super(usystem);
		}

		@Override
		public BigDecimal convert(BigDecimal speed) {
			if (null == speed) return null;
			switch (usystem) {
			case US: // miles/hour -> meters/second
				return speed.multiply(MILE).divide(HOUR, mathctx);
			case METRIC: // kilometers/hour -> meters/second
				return speed.multiply(KILOMETER).divide(HOUR);
			default:
				return speed;
			}
		}
	}

	private LengthConverter lconv = null; // length converter
	private DensityConverter dconv = null; // density converter
	private FlowConverter fconv = null; // flow converter
	private SpeedConverter sconv = null; // speed converter

	private void process() throws SiriusException {
		String units = null;
		if (null != scenario.getSettings())
			units = scenario.getSettings().getUnits();
		if (null == units)
			throw new SiriusException("no units");
		Converter.UnitSystem usystem = Converter.getUnitSystem(units);
		if (Converter.UnitSystem.UNKNOWN == usystem)
			throw new SiriusException("Invalid units '" + units + "'. Should be either 'US' or 'Metric'");
		lconv = new LengthConverter(usystem);
		dconv = new DensityConverter(usystem);
		fconv = new FlowConverter(usystem);
		sconv = new SpeedConverter(usystem);

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
					link.setLength(lconv.convert(link.getLength()));
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
					sb.append(dconv.convert(val).toPlainString());
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
					sb.append(fconv.convert(val).toPlainString());
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
		Converter converter = null;
		if (name.equals("capacity") || name.equals("capacity_drop") || name.endsWith("Flow"))
			converter = fconv;
		else if (name.equals("free_flow_speed") || name.equals("congestion_speed") || name.startsWith("gain"))
			converter = sconv;
		else if (name.equals("jam_density") || name.equals("targetDensity"))
			converter = dconv;
		// TODO check if all the parameters are processed correctly
		if (null != converter)
			param.setValue(converter.convert(new BigDecimal(param.getValue())).toPlainString());
	}

	private void process(Table table) {
		if (null == table) return;
		Converter conv[] = new Converter[table.getColumnNames().getColumnName().size()];
		int colnum = 0;
		for (ColumnName colname : table.getColumnNames().getColumnName()) {
			if (colname.getValue().equals("MeteringRates") || colname.getValue().equals("FlowThresholds"))
				conv[colnum] = fconv;
			else if (colname.getValue().equals("SpeedThresholds"))
				conv[colnum] = sconv;
			else
				conv[colnum] = null;
			++colnum;
		}
		for (Row row : table.getRow()) {
			java.util.ListIterator<String> citer = row.getColumn().listIterator();
			for (colnum = 0; citer.hasNext(); ++colnum) {
				String value = citer.next();
				if (null != conv[colnum])
					citer.set(conv[colnum].convert(new BigDecimal(value)).toPlainString());
			}
		}
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
						sb.append(fconv.convert(data[t][vtn]).toPlainString());
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
			process(controller.getTable());
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
		fd.setFreeFlowSpeed(sconv.convert(fd.getFreeFlowSpeed()));
		fd.setCriticalSpeed(sconv.convert(fd.getCriticalSpeed()));
		fd.setCongestionSpeed(sconv.convert(fd.getCongestionSpeed()));
		fd.setCapacity(fconv.convert(fd.getCapacity()));
		if (null != fd.getJamDensity())
			fd.setJamDensity(dconv.convert(fd.getJamDensity()));
		if (null != fd.getCapacityDrop())
			fd.setCapacityDrop(fconv.convert(fd.getCapacityDrop()));
		if (null != fd.getStdDevCapacity())
			fd.setStdDevCapacity(fconv.convert(fd.getStdDevCapacity()));
		if (null != fd.getStdDevFreeFlowSpeed())
			fd.setStdDevFreeFlowSpeed(sconv.convert(fd.getStdDevFreeFlowSpeed()));
		if (null != fd.getStdDevCongestionSpeed())
			fd.setStdDevCongestionSpeed(sconv.convert(fd.getStdDevCongestionSpeed()));
	}

}
