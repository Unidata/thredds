package edu.ucar.unidata.wmotables.repository;

import java.util.List;

import edu.ucar.unidata.wmotables.domain.Table;
import edu.ucar.unidata.wmotables.domain.User;

/**
 * The data access object representing a Table.  
 */

public interface TableDao {

    /**
     * Looks up and retrieves a table from the persistence mechanism using the table id.
     * 
     * @param tableId  The id of the table we are trying to locate (will be unique for each table). 
     * @return  The table represented as a Table object.   
     */
    public Table lookupTable(int tableId);

    /**
     * Looks up and retrieves a table from the persistence mechanism using the checksum value.
     * 
     * @param checksum  The checksum of the table we are trying to locate (will be unique for each table). 
     * @return  The table represented as a Table object.   
     */
    public Table lookupTable(String checksum);

    /**
     * Requests a List of ALL tables from the persistence mechanism.
     * 
     * @return  A List of tables.   
     */
    public List<Table> getTableList();

    /**
     * Requests a List of tables owned by a particular user from the persistence mechanism.
     * 
     * @param userId  The id of the user what owns the tables.
     * @return  A List of tables. 
     */
    public List<Table> getTableList(int userId);

    /**
     * Queries the persistence mechanism and returns the number of tables.
     * 
     * @return  The total number of tables as an int.   
     */
    public int getTableCount();

    /**
     * Queries the persistence mechanism and returns the number of tables owned by a user.
     * 
     * @param userId  The id of the user that owns the tables.
     * @return  The total number of tables as an int.  
     */
    public int getTableCount(int userId);

    /**
     * Toggles the table's visiblity attribute to in the persistence mechanism.
     * 
     * @param table  The table in the persistence mechanism. 
     */
    public void toggleTableVisibility(Table table);

    /**
     * Creates a new table.
     * 
     * @param table  The table to be created. 
     */
    public void createTable(Table table);

    /**
     * Saves changes made to an existing table. 
     * 
     * @param table   The existing table with changes that needs to be saved. 
     */
    public void updateTable(Table table);

}
