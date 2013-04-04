package edu.ucar.unidata.wmotables.repository;

import java.util.List;
import java.util.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.dao.RecoverableDataAccessException;

import edu.ucar.unidata.wmotables.domain.Table;
import edu.ucar.unidata.wmotables.domain.User;


/**
 * The TableDao implementation.  Persistence mechanism is a database.
 */

public class JdbcTableDao extends JdbcDaoSupport implements TableDao {

     private SimpleJdbcInsert insertActor;

    /**
     * Looks up and retrieves a table from the database using the table id.
     * 
     * @param tableId  The id of the table we are trying to locate (will be unique for each table). 
     * @return  The table represented as a Table object.   
     * @throws RecoverableDataAccessException  If unable to lookup table with the given table id. 
     */
    public Table lookupTable(int tableId) {
        String sql = "SELECT * FROM tables WHERE tableId = ?";
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper(), tableId); 
        if (tables.isEmpty()) {
            throw new RecoverableDataAccessException("Unable to find table with Id: " + new Integer(tableId).toString());
        }         
        return tables.get(0);
    }

    /**
     * Looks up and retrieves a table from the persistence mechanism using the checksum value.
     * 
     * @param checksum  The checksum of the table we are trying to locate (will be unique for each table). 
     * @return  The table represented as a Table object.   
     * @throws RecoverableDataAccessException  If unable to lookup table with the given table checksum.
     */
    public Table lookupTable(String checksum) {
        String sql = "SELECT * FROM tables WHERE checksum = ?";
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper(), checksum); 
        if (tables.isEmpty()) {
            throw new RecoverableDataAccessException("Unable to find table with checksum: " + checksum);
        }         
        return tables.get(0);
    }

    /**
     * Requests a List of ALL tables from the persistence mechanism.
     * 
     * @return  A List of tables.   
     */
    public List<Table> getTableList() {
        String sql = "SELECT * FROM tables ORDER BY dateCreated DESC";               
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper());
        return tables;
    }

    /**
     * Requests a List of tables owned by a particular user from the persistence mechanism.
     * 
     * @param userId  The id of the user what owns the tables.
     * @return  A List of tables.   
     */
    public List<Table> getTableList(int userId) {
        String sql = "SELECT * FROM tables WHERE userId = ? ORDER BY dateCreated DESC";               
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper(), userId);
        return tables;
    }

    /**
     * Queries the persistence mechanism and returns the number of tables.
     * 
     * @return  The total number of tables as an int.   
     */
    public int getTableCount() {
        String sql = "SELECT count(*) FROM tables";
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper());
        return tables.size();
    }

    /**
     * Queries the persistence mechanism and returns the number of tables owned by a user.
     * 
     * @param userId  The id of the user that owns the tables.
     * @return  The total number of tables as an int.  
     */
    public int getTableCount(int userId) {
        String sql = "SELECT count(*) FROM tables WHERE userId = ?";
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper(), userId);
        return tables.size();
    }

    /**
     * Toggles the table's visiblity attribute to in the persistence mechanism.
     * 
     * @param tableId  The ID of the table in the persistence mechanism. 
     * @throws RecoverableDataAccessException  If unable to find the table to toggle. 
     */
    public void toggleTableVisibility(Table table) {
        String sql = "UPDATE tables SET visibility = ? WHERE tableId = ?";
        int rowsAffected = getJdbcTemplate().update(sql, new Object[] {
            // order matters here
            table.getVisibility(),
            table.getTableId()
        });
        if (rowsAffected <= 0) {
            throw new RecoverableDataAccessException("Unable to toggle table visibility. No table found with checksum: " + table.getChecksum());
        }
    }

    /**
     * Creates a new table.
     * 
     * @param table  The table to be created.  
     * @throws RecoverableDataAccessException  If the table we are trying to create already exists.
     */
    public void createTable(Table table) {        
        String sql = "SELECT * FROM tables WHERE checksum = ?";
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper(), table.getChecksum());        
        if (!tables.isEmpty()) {
            throw new RecoverableDataAccessException("An identical table has already been uploaded.");
        } else {
            this.insertActor = new SimpleJdbcInsert(getDataSource()).withTableName("tables").usingGeneratedKeyColumns("tableId");
            SqlParameterSource parameters = new BeanPropertySqlParameterSource(table);
            Number newTableId = insertActor.executeAndReturnKey(parameters);
            table.setTableId(newTableId.intValue());
        }                
    }

    /**
     * Saves changes made to an existing table. 
     * 
     * @param table   The existing table with changes that needs to be saved. 
     * @throws RecoverableDataAccessException  If unable to find the table to update. 
     */
    public void updateTable(Table table) {
        String sql = "UPDATE tables SET title = ?, description = ?, localVersion = ?, center= ?, subCenter= ?, tableType = ?, dateModified = ? WHERE tableId = ?";
        int rowsAffected = getJdbcTemplate().update(sql, new Object[] {
            // order matters here
            table.getTitle(),
            table.getDescription(), 
            table.getLocalVersion(), 
            table.getCenter(), 
            table.getSubCenter(), 
            table.getTableType(), 
            table.getDateModified(),
            table.getTableId()
        });
        if (rowsAffected <= 0) {
            throw new RecoverableDataAccessException("Unable to update table.  No table found with checksum: " + table.getChecksum());
        }
    } 


    /***
     * Maps each row of the ResultSet to a Table object.
     */
    private static class TableMapper implements RowMapper<Table> {
        /**
         * Maps each row of data in the ResultSet to the Table object.
         * 
         * @param rs  The ResultSet to be mapped.
         * @param rowNum  The number of the current row.
         * @return  The populated Table object.
         * @throws SQLException  If a SQLException is encountered getting column values.
         */
        public Table mapRow(ResultSet rs, int rowNum) throws SQLException {
            Table table = new Table();
            table.setTableId(rs.getInt("tableId"));
            table.setTitle(rs.getString("title"));
            table.setDescription(rs.getString("description"));
            table.setOriginalName(rs.getString("originalName"));
            table.setLocalVersion(rs.getInt("localVersion"));
            table.setCenter(rs.getInt("center"));
            table.setSubCenter(rs.getInt("subCenter"));
            table.setTableType(rs.getString("tableType"));
            table.setMimeType(rs.getString("mimeType"));
            table.setChecksum(rs.getString("checksum"));
            table.setUserId(rs.getInt("userId"));
            table.setVisibility(rs.getInt("visibility"));
            table.setDateCreated(rs.getTimestamp("dateCreated"));
            table.setDateModified(rs.getTimestamp("dateModified"));
            return table;
        }
    }


}
