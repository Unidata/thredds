package edu.ucar.unidata.wmotables.service;

import org.apache.log4j.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
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
    protected static Logger logger = Logger.getLogger(TableManagerImpl.class);
    @Value("${wmotables.home}")
    private String wmotablesHome;

    /**
     * Sets the data access object which will acquire and persist the data 
     * passed to it via the methods of this TableManager. 
     * 
     * @param tableDao  The service mechanism data access object representing a Table. 
     */
    public void setTableDao(TableDao tableDao) {
        this.tableDao = tableDao;
    }

    /**
     * Looks up and retrieves a table from the persistence mechanism using the table id.
     * 
     * @param tableId  The id of the table we are trying to locate (will be unique for each table).
     * @return  The table represented as a Table object.   
     */
    public Table lookupTable(int tableId){
        return tableDao.lookupTable(tableId);  
    }

    /**
     * Looks up and retrieves a table from the persistence mechanism using the checkSum value.
     * 
     * @param checkSum  The checkSum check sum of the table we are trying to locate (will be unique for each table). 
     * @return  The table represented as a Table object.   
     */
    public Table lookupTable(String checkSum) {
        return tableDao.lookupTable(checkSum);  
    }

    /**
     * Requests a List of ALL tables from the persistence mechanism.
     * 
     * @return  A List of tables.   
     */
    public List<Table> getTableList() {
        return tableDao.getTableList();
    }
   
    /**
     * Requests a List of tables owned by a particular user from the persistence mechanism.
     * 
     * @param userId  The id of the user what owns the tables.
     * @return  A List of tables. 
     */
    public List<Table> getTableList(int userId) {
        return tableDao.getTableList(userId);
    }

    /**
     * Queries the persistence mechanism and returns the number of tables.
     * 
     * @return  The total number of tables as an int.   
     */
    public int getTableCount() {
        return tableDao.getTableCount();
    }

    /**
     * Queries the persistence mechanism and returns the number of tables owned by a user.
     * 
     * @param userId  The id of the user that owns the tables.
     * @return  The total number of tables as an int.  
     */
    public int getTableCount(int userId) {
        return tableDao.getTableCount(userId);
    }

    /**
     * Toggles the table's visiblity attribute to in the persistence mechanism.
     * 
     * @param table  The table in the persistence mechanism. 
     */
    public void toggleTableVisibility(Table table) {
        tableDao.toggleTableVisibility(table);     
    }

    /**
     * Creates a new table. A WMO table is uploaded as a CommonsMultipartFile, 
     * an Checksum (MD5) is generated from its contents, and the Table is 
     * stored on disk using the checksum as a name). 
     * 
     * @param table  The table to be created. 
     * @throws IOException  If an IO error occurs when writing the table to the file system.
     */
    public void createTable(Table table) throws IOException {
        File tableStashDir = new File(wmotablesHome + "/tables/");
        if (!tableStashDir.exists()) {
            if (!tableStashDir.mkdirs()) {
                throw new IOException("Unable to create the following directory: " + tableStashDir);
            }                    
        }  
        byte[] fileData = table.getFile().getFileItem().get();
        String checksum = DigestUtils.md5Hex(fileData);

        table.setChecksum(checksum);
        table.setDateCreated(new Date(System.currentTimeMillis()));
        table.setDateModified(new Date(System.currentTimeMillis()));
        tableDao.createTable(table);
        FileOutputStream outputStream = null; 
        try {
            File file = new File(tableStashDir + "/" + table.getChecksum());
            outputStream = new FileOutputStream(file); 
            outputStream.write(fileData);
            outputStream.flush();
        } finally {
            outputStream.close();
        }
   
    }

    /**
     * Saves changes made to an existing table (meaning changes made to table
     * attributes in the persistence mechanism and NOT the creation/deletion of tables
     * contained within the table). 
     * 
     * @param table   The existing table with changes that needs to be saved. 
     */
    public void updateTable(Table table) {
        table.setDateModified(new Date(System.currentTimeMillis()));
        tableDao.updateTable(table);
    }

    /**
     * Access the table file on disk and streams it to the response object.
     * 
     * @param action  Whether we want to view the file or download the file (influences the mimetype).
     * @param table  The Table object representing the file to download.
     * @param response  The current HttpServletRequest response.
     * @throws RuntimeException  If unable to stream the file to the response object.
     */
    public void downloadTableFile(String action, Table table, HttpServletResponse response) throws RuntimeException {
        File tableFile = new File(wmotablesHome + "/tables/" + table.getChecksum());
        FileInputStream inputStream = null; 
		try {
			inputStream = new FileInputStream(tableFile);
            if (action.equals("view")) {
                response.setContentType(table.getMimeType());
            } else {
                response.setContentType("application/wmotables");
            }
            // copy it to response's OutputStream
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
		} catch (IOException e) {
			logger.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
                }
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}     
    }
}
