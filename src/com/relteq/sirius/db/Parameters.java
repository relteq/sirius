package com.relteq.sirius.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Parameters {
	/** The database type: <code>derby</code>, <code>postgresql</code>, etc */
	private String driver;
	private String host;
	private String port;
	private String user;
	private String password;

	/** The Sirius database name */
	private static final String db_name = "sirius";

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
		params.setHost(System.getenv("SIRIUS_DB_HOST"));
		params.setPort(System.getenv("SIRIUS_DB_PORT"));
		if (null == params.getHost() && null != params.getPort())
			params.setHost("localhost");
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

}
