package com.relteq.sirius.db;

import java.io.File;
import java.sql.*;

import com.relteq.sirius.simulator.SiriusException;

/**
 * Administers the Sirius Database
 */
public class Admin {
	/**
	 * Initializes the database.
	 * If the database exists, it is dropped and recreated.
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 * @throws SQLException
	 * @throws SiriusException
	 * @throws Exception
	 */
	public static void init() throws SQLException {
		SQLExec exec = new SQLExec();
		Parameters params = Parameters.get();
		// TODO: add SQL scripts to the JAR
		exec.setSrc(new File("torque" + File.separator + "sql" + File.separator + //
				params.getDriver() + File.separator + "sirius-db-schema.sql"));
		exec.setUrl(params.getUrl());
		exec.setDriver(DriverManager.getDriver(exec.getUrl()).getClass().getName());
		exec.setUserid(null == params.getUser() ? "" : params.getUser());
		exec.setPassword(null == params.getPassword() ? "" : params.getPassword());
		SQLExec.OnError onerror = new SQLExec.OnError();
		onerror.setValue("continue");
		exec.setOnerror(onerror);
		exec.setPrint(true);
		exec.execute();
	}

	/**
	 * Executes SQL statements
	 */
	public static class SQLExec extends org.apache.tools.ant.taskdefs.SQLExec {
		public SQLExec() {
			org.apache.tools.ant.Project project = new org.apache.tools.ant.Project();
			project.init();
			setProject(project);
			setTaskType("sql");
			setTaskName("sql");
		}
	}

}
