package com.relteq.sirius.db;

import java.sql.Connection;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

import com.workingdogs.village.DataSetException;

@SuppressWarnings("serial")
public class BasePeer extends org.apache.torque.util.BasePeer {

	/**
	 * Returns a maximum column value
	 * @param colname column name
	 * @return null if the table is empty
	 * @throws TorqueException
	 * @throws DataSetException
	 */
	public static com.workingdogs.village.Value maxColumnValue(String colname, Criteria crit, Connection conn) throws TorqueException, DataSetException {
		if (null == crit) crit = new Criteria();
		crit.addSelectColumn("COUNT(" + colname + ")");
		crit.addSelectColumn("MAX(" + colname + ")");
		@SuppressWarnings("unchecked")
		List<com.workingdogs.village.Record> record_l = doSelect(crit, conn);
		com.workingdogs.village.Record record = record_l.get(0);
		int count = record.getValue(1).asInt();
		return 0 == count ? null : record.getValue(2);
	}

	private static Logger logger = Logger.getLogger(BasePeer.class);

	/**
	 * Generates an ID
	 * @param colname column name (table.column)
	 * @param conn DB connection on NULL for autocommit
	 * @return a new ID or NULL if an error occurred
	 * @throws TorqueException
	 */
	public static Long nextId(String colname, Connection conn) throws TorqueException {
		try {
			com.workingdogs.village.Value maxval = maxColumnValue(colname, null, conn);
			return Long.valueOf(null == maxval ? 0 : maxval.asLong() + 1);
		} catch (DataSetException exc) {
			logger.error(exc.getMessage(), exc);
			return null;
		}
	}

}
