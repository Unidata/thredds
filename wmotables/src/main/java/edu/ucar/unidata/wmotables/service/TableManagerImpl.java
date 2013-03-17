package edu.ucar.unidata.wmotables.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.ucar.unidata.wmotables.domain.Table;
import edu.ucar.unidata.wmotables.domain.User;
import edu.ucar.unidata.wmotables.repository.TableDao;


/**
 * Service for processing Table objects. 
 */
public class TableManagerImpl implements TableManager {

    private TableDao tableDao;

    @Value("${wmotables.home}")
    private String wmotablesHome;

    /*
     * Sets the data access object which will acquire and persist the data 
     * passed to it via the methods of this TableManager. 
     * 
     * @param tableDao  The service mechanism data access object representing a Table. 
     */
    public void setTableDao(TableDao tableDao) {
        this.tableDao = tableDao;
    }

    /*
     * Looks up and retrieves a table from the persistence mechanism using the table id.
     * 
     * @param tableId  The id of the table we are trying to locate (will be unique for each table).
     * @return  The table represented as a Table object.   
     */
    public Table lookupTable(int tableId){
        return tableDao.lookupTable(tableId);  
    }

    /*
     * Requests a List of ALL tables from the persistence mechanism.
     * 
     * @return  A List of tables.   
     */
    public List<Table> getTableList() {
        return tableDao.getTableList();
    }
   
    /*
     * Requests a List of tables owned by a particular user from the persistence mechanism.
     * 
     * @param userId  The id of the user what owns the tables.
     * @return  A List of tables. 
     */
    public List<Table> getTableList(int userId) {
        return tableDao.getTableList(userId);
    }

    /*
     * Requests a List of tables owned by a particular user from the persistence mechanism.
     * 
     * @param user  The User what owns the tables.
     * @return  A List of tables.   
     */
    public List<Table> getTableList(User user) {
        return tableDao.getTableList(user);
    }

    /*
     * Queries the persistence mechanism and returns the number of tables.
     * 
     * @return  The total number of tables as an int.   
     */
    public int getTableCount() {
        return tableDao.getTableCount();
    }

    /*
     * Queries the persistence mechanism and returns the number of tables owned by a user.
     * 
     * @param userId  The id of the user that owns the tables.
     * @return  The total number of tables as an int.  
     */
    public int getTableCount(int userId) {
        return tableDao.getTableCount(userId);
    }

    /*
     * Queries the persistence mechanism and returns the number of tables owned by a user.
     * 
     * @param user  The User that owns the tables.
     * @return  The total number of tables as an int.   
     */
    public int getTableCount(User user) {
        return tableDao.getTableCount(user);
    }

    /*
     * Toggles the table's visiblity attribute to in the persistence mechanism.
     * 
     * @param table  The table in the persistence mechanism. 
     */
    public void toggleTableVisibility(Table table) {
        tableDao.toggleTableVisibility(table);     
    }

    /*
     * Creates a new table. A WMO table is uploaded as a CommonsMultipartFile, 
     * an MD5 Checksum is generated from its contents, and the Table is 
     * stored on disk using the MD5 as a name). 
     * 
     * @param table  The table to be created. 
     * @throws IOException  If an IO error occurs when writing the table to the file system.
     */
    public void createTable(Table table) throws IOException {
        File tableStashDir = new File(wmotablesHome + "/tables");
        if (!tableStashDir.exists()) {
            if (!tableStashDir.mkdirs()) {
                throw new IOException("Unable to create the following directory: " + tableStashDir);
            }                    
        }  
        byte[] fileData = table.getFile().getFileItem().get();
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fileData);

        table.setMd5(md5);
        table.setDateCreated(new Date(System.currentTimeMillis()));
        table.setDateModified(new Date(System.currentTimeMillis()));
        tableDao.createTable(table);
        FileOutputStream outputStream = null; 
        try {
            outputStream = new FileOutputStream(new File(tableStashDir + "/" + md5)); 
            outputStream.write(fileData);
            outputStream.flush();
        } finally {
            outputStream.close();
        }
     
    }

    /*
     * Saves changes made to an existing table (meaning changes made to table
     * attributes in the persistence mechanism and NOT the creation/deletion of tables
     * contained within the table). 
     * 
     * @param table   The existing table with changes that needs to be saved. 
     * @return  The updated Table object.
     */
    public void updateTable(Table table) {
        table.setDateModified(new Date(System.currentTimeMillis()));
        tableDao.updateTable(table);
    }
}
