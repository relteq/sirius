package com.relteq.sirius.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.sql.*;

/**
 * Administers the Sirius Database
 */
public class Admin {
	/**
	 * Initializes the database.
	 * If the database exists, it is dropped and recreated.
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void init() throws SQLException, IOException {
		SQLExec exec = new SQLExec();
		Parameters params = Parameters.get();
		exec.setSrc("sql" + File.separator + //
				params.getDriver() + File.separator + "sirius-db-schema.sql");
		exec.setUrl(params.getUrl());
		System.out.println("Connection URL: " + exec.getUrl());
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

		/**
		 * Sets the resource name of the SQL script to be run
		 * @param name the resource name
		 * @throws IOException
		 */
		public void setSrc(String name) throws IOException {
			int ind[] = {-1, -1};
			ind[0] = name.lastIndexOf(File.separator);
			ind[1] = name.lastIndexOf('.');
			if (-1 == ind[1] || ind[1] <= ind[0]) ind[1] = name.length();
			File file = File.createTempFile(name.substring(ind[0] + 1, ind[1]),//
					ind[1] >= name.length() ? ".sql" : name.substring(ind[1]));
			file.deleteOnExit();
			InputStream is = Admin.class.getClassLoader().getResourceAsStream(name);
			OutputStream os = new FileOutputStream(file);
			copy(is, os);
			is.close();
			os.close();
			super.setSrc(file);
		}

		/**
		 * Copies an input stream to an output stream
		 * @param is the input stream
		 * @param os the output stream
		 * @throws IOException
		 */
		private static void copy(InputStream is, OutputStream os) throws IOException {
			ReadableByteChannel ich = Channels.newChannel(is);
			WritableByteChannel och = Channels.newChannel(os);
			final ByteBuffer buf = ByteBuffer.allocateDirect(16384);
			while (ich.read(buf) != -1) {
				buf.flip();
				och.write(buf);
				buf.compact();
			}
			buf.flip();
			while (buf.hasRemaining()) och.write(buf);
			ich.close();
			och.close();
		}
	}

}
