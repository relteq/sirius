package com.relteq.sirius;

import com.relteq.sirius.simulator.SiriusErrorLog;

/**
 * Implements "Sirius: Concept of Operations"
 */
public class Runner {

	/**
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		try {
			if (0 == args.length) throw new InvalidUsageException();
			String cmd = args[0];
			String[] arguments = new String[args.length - 1];
			System.arraycopy(args, 1, arguments, 0, args.length - 1);
			if (cmd.equals("import") || cmd.equals("i")) {
				if (arguments.length != 1) throw new InvalidUsageException("Usage: import|i scenario_file_name");
				com.relteq.sirius.db.importer.ScenarioLoader.load(arguments[0]);
			} else if (cmd.equals("update") || cmd.equals("u")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("export") || cmd.equals("e")) {
				if (0 == arguments.length || 2 < arguments.length)
					throw new InvalidUsageException("Usage: export|e scenario_id [output_file_name]");
				else {
					String filename = 1 == arguments.length ? arguments[0] + ".xml" : arguments[1];
					com.relteq.sirius.db.exporter.ScenarioRestorer.export(arguments[0], filename);
				}
			} else if (cmd.equals("calibrate") || cmd.equals("c")) {
				com.relteq.sirius.calibrator.FDCalibrator.main(arguments);
			} else if (cmd.equals("simulate") || cmd.equals("s")) {
				com.relteq.sirius.simulator.Runner.run_db(arguments);
			} else if (cmd.equals("simulate_output") || cmd.equals("so")) {
				com.relteq.sirius.simulator.Runner.main(arguments);
			} else if (cmd.equals("debug")) {
				com.relteq.sirius.simulator.Runner.debug(arguments);
			} else if (cmd.equals("simulate_process") || cmd.equals("sp")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("list_scenarios") || cmd.equals("ls")) {
				com.relteq.sirius.db.Lister.listScenarios();
			} else if (cmd.equals("list_runs") || cmd.equals("lr")) {
				if (1 == arguments.length)
					com.relteq.sirius.db.Lister.listRuns(arguments[0]);
				else
					throw new InvalidUsageException("Usage: list_runs|lr scenario_id");
			} else if (cmd.equals("load") || cmd.equals("l")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("process") || cmd.equals("p")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("output") || cmd.equals("o")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("list_aggregations") || cmd.equals("la")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("link_data") || cmd.equals("ld")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("route_data") || cmd.equals("rd")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("node_data") || cmd.equals("nd")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("signal_data") || cmd.equals("sd")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("detection_data") || cmd.equals("dd")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("probe_data") || cmd.equals("pd")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("controller_data") || cmd.equals("cd")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("report") || cmd.equals("r")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("init")) {
				com.relteq.sirius.db.Admin.init();
			} else if (cmd.equals("clear_data") || cmd.equals("cld")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("clear_processed") || cmd.equals("clp")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("clear_scenario") || cmd.equals("cls")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("clear_all") || cmd.equals("cla")) {
				throw new NotImplementedException(cmd);
			} else if (cmd.equals("version") || cmd.equals("v")) {
				printVersion();
			} else throw new InvalidCommandException(cmd);
			if (SiriusErrorLog.haserror()) SiriusErrorLog.print();
		} catch (InvalidUsageException exc) {
			String msg = exc.getMessage();
			if (null == msg) msg = "Usage: command [parameters]";
			System.err.println(msg);
			System.exit(1);
		} catch (NotImplementedException exc) {
			System.err.println(exc.getMessage());
			System.exit(1);
		} catch (InvalidCommandException exc) {
			System.err.println(exc.getMessage());
			System.exit(1);
		} catch (Exception exc) {
			exc.printStackTrace();
			System.exit(2);
		} finally {
			if (com.relteq.sirius.db.Service.isInit())
				com.relteq.sirius.db.Service.shutdown();
		}
	}

	@SuppressWarnings("serial")
	public static class NotImplementedException extends Exception {
		/**
		 * Constructs a <code>NotImplementedException</code> for the specified command
		 * @param cmd name of the command
		 */
		NotImplementedException(String cmd) {
			super("Command '" + cmd + "' is not implemented");
		}
	}

	@SuppressWarnings("serial")
	public static class InvalidCommandException extends Exception {
		/**
		 * Constructs an <code>InvalidCommandException</code> for the specified command
		 * @param cmd name of the command
		 */
		public InvalidCommandException(String cmd) {
			super("Invalid command '" + cmd + "'");
		}
	}

	@SuppressWarnings("serial")
	public static class InvalidUsageException extends Exception {
		public InvalidUsageException() {
			super();
		}
		public InvalidUsageException(String message) {
			super(message);
		}
	}

	private static void printVersion() {
		System.out.println(Version.get());
	}

}
