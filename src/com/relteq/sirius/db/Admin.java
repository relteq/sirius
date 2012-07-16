package com.relteq.sirius.db;

import java.io.File;
import java.io.IOException;
import java.sql.*;

import org.apache.torque.TorqueException;
import org.apache.torque.util.BasePeer;

import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;

/**
 * Administers the Sirius Database
 */
public class Admin {
	/**
	 * Initializes the database.
	 * If the database exists, it is dropped and recreated.
	 * @throws SQLException
	 * @throws IOException
	 * @throws SiriusException
	 */
	public static void init() throws SQLException, IOException, SiriusException {
		init(Parameters.fromEnvironment());
	}

	/**
	 * Initializes the database
	 * @param params the database connection parameters
	 * @throws SQLException
	 * @throws IOException
	 * @throws SiriusException
	 */
	public static void init(Parameters params) throws SQLException, IOException, SiriusException {
		SQLExec exec = new SQLExec();
		drop(params);
		if (params.getDriver().equals("derby")) params.setCreate(true);
		Service.init(params);
		exec.runStatements(new java.io.InputStreamReader(Admin.class.getClassLoader().getResourceAsStream(
				"sql" + File.separator + params.getDriver() + File.separator + "sirius-db-schema.sql")),
				System.err);
	}

	/**
	 * Executes SQL statements
	 */
	public static class SQLExec extends org.apache.torque.task.TorqueSQLExec {
		public SQLExec() {
			org.apache.tools.ant.Project project = new org.apache.tools.ant.Project();
			project.init();
			setProject(project);
		}

		@Override
		public void runStatements(java.io.Reader reader, java.io.PrintStream out) throws IOException, SQLException {
			super.runStatements(reader, out);
		}

		@Override
		protected void execSQL(String sql, java.io.PrintStream out) {
			try {
				BasePeer.executeStatement(sql);
			} catch (TorqueException exc) {
				SiriusErrorLog.addErrorMessage(exc.getMessage());
			}
		}

	}

	/**
	 * Drops the database
	 * @param params DB connection parameters
	 */
	public static void drop(Parameters params) {
		if (params.getDriver().equals("derby"))
			try {
				org.apache.commons.io.FileUtils.deleteDirectory(new File(params.getDBName()));
			} catch (IOException exc) {
				SiriusErrorLog.addErrorMessage(exc.getMessage());
			}
	}
}
