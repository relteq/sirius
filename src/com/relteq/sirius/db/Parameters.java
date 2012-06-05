package com.relteq.sirius.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.configuration.BaseConfiguration;

public class Parameters {
	/** The database type: <code>derby</code>, <code>postgresql</code>, etc */
	private String driver;
	private String db_name;
	private String host;
	private String port;
	private String user;
	private String password;

	private static final String default_db_name = "sirius";
	private static final String default_host = "localhost";

	/**
	 * @return the driver
	 */
	public String getDriver() {
		return driver;
	}

	/**
	 * @param driver the driver to set
	 */
	public void setDriver(String driver) {
		this.driver = driver;
	}

	/**
	 * @return the database name
	 */
	public String getDb_name() {
		return db_name;
	}

	/**
	 * @param db_name the database name to set
	 */
	public void setDb_name(String db_name) {
		this.db_name = db_name;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	private Parameters() {}

	/**
	 * Initializes the DB parameters from the environment
	 * @return the parameters
	 */
	public static Parameters get() {
		Parameters params = new Parameters();
		String driver = System.getenv("SIRIUS_DB");
		if (null == driver) driver = "derby";
		params.setDriver(driver);
		String db_name = System.getenv("SIRIUS_DB_NAME");
		if (null == db_name) db_name = default_db_name;
		params.setDb_name(db_name);
		params.setHost(System.getenv("SIRIUS_DB_HOST"));
		params.setPort(System.getenv("SIRIUS_DB_PORT"));
		if (null == params.getHost() && null != params.getPort())
			params.setHost(default_host);
		params.setUser(System.getenv("SIRIUS_DB_USER"));
		params.setPassword(System.getenv("SIRIUS_DB_PASSWORD"));
		return params;
	}

	/**
	 * @return the database connection URL
	 */
	public String getUrl() {
		StringBuilder url = new StringBuilder("jdbc:");
		url.append(driver).append(":");
		if (driver.equals("postgresql") && (null != host)) {
			url.append("//").append(host);
			if (null != port) url.append(":").append(port);
			url.append("/");
		}
		url.append(db_name);
		if (driver.equals("derby")) url.append(";create=true");
		return url.toString();
	}

	/**
	 * @return a list of connection arguments, such as "user" and "password"
	 */
	public Properties getConnectionProperties() {
		Properties props = new Properties();
		if (null != getUser()) props.put("user", getUser());
		if (null != getPassword()) props.put("password", getPassword());
		return props;
	}

	/**
	 * Establishes a database connection
	 * @return a database connection
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(getUrl(), getConnectionProperties());
	}

	public org.apache.commons.configuration.Configuration toConfiguration() {
		BaseConfiguration conf = new BaseConfiguration();
		conf.addProperty("torque.database.default", "sirius");
		conf.addProperty("torque.database.sirius.adapter", getDriver());
		conf.addProperty("torque.dsfactory.sirius.factory", "org.apache.torque.dsfactory.SharedPoolDataSourceFactory");
		conf.addProperty("torque.dsfactory.sirius.connection.url", getUrl());
		if (null != getUser()) conf.addProperty("torque.dsfactory.sirius.connection.user", getUser());
		if (null != getPassword()) conf.addProperty("torque.dsfactory.sirius.connection.password", getPassword());
		return conf;
	}

}
