package com.relteq.sirius.simulator;

public class Table extends com.relteq.sirius.jaxb.Table {

	/**
	 * @return column names
	 */
	public com.relteq.sirius.jaxb.ColumnNames getColumnNames() {
		for (Object item : getContent()) {
			if (item instanceof com.relteq.sirius.jaxb.ColumnNames)
				return (com.relteq.sirius.jaxb.ColumnNames) item;
		}
		return null;
	}

	/**
	 * @return table rows
	 */
	public java.util.List<com.relteq.sirius.jaxb.Row> getRowList() {
		java.util.List<com.relteq.sirius.jaxb.Row> row_l= new java.util.ArrayList<com.relteq.sirius.jaxb.Row>();
		for (Object item : getContent()) {
			if (item instanceof com.relteq.sirius.jaxb.Row)
				row_l.add((com.relteq.sirius.jaxb.Row) item);
		}
		return row_l;
	}

	public java.util.Map<String, Integer> getColumnNameToIndexMap() {
		com.relteq.sirius.jaxb.ColumnNames colnames = getColumnNames();
		if (null == colnames) return null;
		else {
			java.util.Map<String, Integer> map = new java.util.TreeMap<String, Integer>();
			int index = 0;
			for (String colname : colnames.getColumnName())
				map.put(colname, index++);
			return map;
		}
	}
}
