package com.relteq.sirius.simulator;
import java.util.ArrayList;
import java.util.HashSet;

public class Table {
	
	
	/** List of Column names*/	
	protected ArrayList<String> ColumnNames;
	
	/** List of Rows. Each row contains a list of strings denoting different columns */	
	protected ArrayList<ArrayList<String>> Rows;
	
	
	/** Checks that each row has the same number of columns as the column_names. Also checks for unique column names*/  
	public boolean checkTable(){
		if (ColumnNames.size() !=(new HashSet<String>(ColumnNames)).size())
			return false;
		for (ArrayList<String> r: Rows){
			if (r.size()!=ColumnNames.size())
				return false;
		}
		return true;
	}
	
	
	/** Returns number of rows in the table*/
	public int getNoRows(){
		return Rows.size();
	}
	
	/** Returns the column number corresponding to the given column_name*/ 
	public int getColumnNo(String cname){		
		return 	ColumnNames.indexOf((Object) cname);
	}
	
	/** Returns an element in the table, indexed by row and column numbers*/
	public String getTableElement(int RowNo,int ColumnNo){
		return (Rows.get(RowNo)).get(ColumnNo);
	}
	
	/** Returns an element in the table, indexed by row number and column name*/
	public String getTableElement(int RowNo,String cname){
		return (Rows.get(RowNo)).get(this.getColumnNo(cname));
	}
	
	
	/** Constructors*/
	public Table(com.relteq.sirius.jaxb.Table T1){	
		 Rows = new ArrayList<ArrayList<String>>();
		 ColumnNames=new ArrayList<String>();
		 for (Object o: T1.getContent()){
			 if (o instanceof  com.relteq.sirius.jaxb.Row){
				Rows.add((ArrayList<String>) ((com.relteq.sirius.jaxb.Row) o).getColumn()); 
			 }
			 if (o instanceof  com.relteq.sirius.jaxb.ColumnNames){
				ColumnNames.addAll(((com.relteq.sirius.jaxb.ColumnNames) o).getColumnName()); 				
			 }
		 }		
	}


	public Table(ArrayList<String> columnNames,	ArrayList<ArrayList<String>> rows) {
		super();
		ColumnNames = columnNames;
		Rows = rows;
	}


	
	
}


