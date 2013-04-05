package edu.ucar.unidata.wmotables;

import org.apache.log4j.Logger;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;


/**
 * Done at application initialization
 */
public class ApplicationInitialization implements ServletContextListener {

    protected static Logger logger = Logger.getLogger(ApplicationInitialization.class);

    private static final String DEFAULT_HOME = System.getProperty("catalina.base") + "/content/wmotables";
    private static final String DEFAULT_DATABASE = "derby";

    private String wmotablesHome = null;
    private String databaseSelected = null;

    /**
     * Find the application home (wmotables.home) and make sure it exists.  if not, create it.
     * Find out what database was selected for use and create the database if it doesn't exist.
     * 
     * @param sce  The event class.
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent)  {
        ServletContext servletContext = servletContextEvent.getServletContext();

        try {
            File configFile = new File(servletContext.getRealPath("") + "/WEB-INF/classes/wmotables.properties");
            if (!configFile.exists()) {
                logger.info("Configuration file not provided.");  
                logger.info("Using wmotables.home default: " + DEFAULT_HOME);    
                wmotablesHome = DEFAULT_HOME; 
                logger.info("Using wmotables.db default: " + DEFAULT_DATABASE);    
                databaseSelected = DEFAULT_DATABASE;
            } else {
                logger.info("Reading configuration file.");  
                String currentLine;
                BufferedReader reader = new BufferedReader(new FileReader(configFile));
                while ((currentLine = reader.readLine()) != null) {
                    String lineData;
                    if ((lineData = StringUtils.stripToNull(currentLine)) != null) {
                        if (lineData.startsWith("wmotables.home")) {
                            wmotablesHome = StringUtils.removeStart(lineData, "wmotables.home=");
                            logger.info("wmotables.home set to: " + wmotablesHome);  
                        }
                        if (lineData.startsWith("wmotables.db")) {
                            databaseSelected = StringUtils.removeStart(lineData, "wmotables.db=");
                            logger.info("wmotables.db set to: " + databaseSelected);  
                        }
                    }
                }
                if (wmotablesHome == null) {
                    logger.info("Configuration file does not contain wmotables.home information.");  
                    logger.info("Using wmotables.home default: " + DEFAULT_HOME);  
                    wmotablesHome = DEFAULT_HOME;      
                }
                if (databaseSelected == null) {
                    logger.info("Configuration file does not contain wmotables.db information.");  
                    logger.info("Using wmotables.db default: " + DEFAULT_DATABASE);  
                    databaseSelected = DEFAULT_DATABASE;      
                }
            }
            createDirectory(new File(wmotablesHome));
        } catch (Exception e) {            
            logger.error(e.getMessage());   
            throw new RuntimeException(e.getMessage());  
        }

        try {
            createDatabase(wmotablesHome, databaseSelected, servletContext);
        } catch (Exception e) {            
            logger.error(e.getMessage());   
            throw new RuntimeException(e.getMessage());  
        }
    }  

    /**
     * Shutdown the database if it hasn't already been shutdown.
     * 
     * @param sce  The event class.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (databaseSelected.equals("derby")) {
            String derbyUrl = "jdbc:derby:" + wmotablesHome + "/db/wmotables";
            try { 
                DriverManager.getConnection( derbyUrl + ";shutdown=true");
            } catch (SQLException e) {
                logger.error(e.getMessage()); 
            }  
        }
    }

    /**
     * Creates a directory (and parent directories as needed) using the provided file.
     * 
     * @param file  The directory to create.
     * @throws RuntimeException  If we are unable to create the directory.
     */
    public void createDirectory(File file) throws RuntimeException {
        if (!file.exists()) {
            logger.info("Creating wmotables.home...");
            if (!file.mkdirs()) {
                throw new RuntimeException("Unable to create the following directory: " + file);
            }                   
        } 
    }

    /**
     * Creates a directory (and parent directories as needed) using the provided file.
     * 
     * @param wmotablesHome  The value of wmotables.home.
     * @param databaseSelected  The value of wmotables.db.
     */
    public void createDatabase(String wmotablesHome, String databaseSelected, ServletContext servletContext)  {
        if (databaseSelected.equals("derby")) {
            String derbyDriver = "org.apache.derby.jdbc.EmbeddedDriver";
            String derbyUrl = "jdbc:derby:" + wmotablesHome + "/db/wmotables";
            if (!new File(wmotablesHome + "/db/wmotables").exists()) {
                logger.info("Database does not exist yet.  Creating...");
                try { 
                    createDirectory(new File(wmotablesHome + "/db")); 
                } catch (Exception e) {
                    logger.error(e.getMessage()); 
                }  
                try { 
	    		    createTables(derbyDriver, derbyUrl + ";create=true", null, null, servletContext);
                    DriverManager.getConnection( derbyUrl + ";shutdown=true");
                } catch (SQLException e) {
                    logger.error(e.getMessage()); 
                }        
            } else {
                logger.info("Database already exists.");
                logger.info("Our work here is done.");
            }
        } else {
            // mySQL
        }
    }


    /**
     * Creates the tables in the databases.
     *
     * @param driver  The jdbc driver to load.
     * @param url  The database url with which to make the connection.
     * @param username  The database username (null if not used).
     * @param password  The database password (null if not used).
     * @throws SQLException  If an SQL error occurs when trying to close the preparedStatement or conenction.
     */
    private static void createTables(String driver, String url, String username, String password, ServletContext servletContext) throws SQLException { 
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String createUsersTableSQL = "CREATE TABLE users" +
                                     "(" +
                                     "userId INTEGER primary key not null GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                                     "userName VARCHAR(50) not null, " +
                                     "password CHAR(32) not null, " +
                                     "accessLevel INTEGER not null, " +
                                     "emailAddress VARCHAR(75) not null, " +
                                     "fullName VARCHAR(75) not null, " +
                                     "center INTEGER not null, " +
                                     "subCenter INTEGER not null, " +
                                     "dateCreated TIMESTAMP not null, " +
                                     "dateModified TIMESTAMP not null" +
                                     ")";

        String createTablesTableSQL = "CREATE TABLE tables" +
                                      "(" +
                                      "tableId INTEGER primary key not null GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                                      "title VARCHAR(75) not null, " +
                                      "description VARCHAR(255) not null, " +
                                      "originalName VARCHAR(100) not null, " +
                                      "masterVersion INTEGER, " +
                                      "localVersion INTEGER not null, " +
                                      "center INTEGER not null, " +
                                      "subCenter INTEGER not null, " +
                                      "mimeType VARCHAR(100) not null, " +
                                      "tableType VARCHAR(100) not null, " +
                                      "checkSum CHAR(32) not null, " +
                                      "visibility INTEGER not null, " +
                                      "userId INTEGER not null, " +
                                      "dateCreated TIMESTAMP not null, " +
                                      "dateModified TIMESTAMP not null" +
                                      ")";

        String createCentersTableSQL = "CREATE TABLE centers" +
                                       "(" +
                                       "centerId INTEGER primary key not null, " +
                                       "name VARCHAR(255) not null " +
                                       ")";

        String createSubCentersTableSQL = "CREATE TABLE subCenters" +
                                          "(" +
                                          "subCenterId INTEGER primary key not null, " +
                                          "centerId INTEGER not null, " +
                                          "name VARCHAR(255) not null " +
                                          ")";

        String insertAdminUserSQL = "INSERT INTO users " +
                                    "(userName, password, accessLevel, emailAddress, fullName, center, subCenter, dateCreated, dateModified) VALUES " +
                                    "(?,?,?,?,?,?,?,?,?)"; 

        String insertCentersSQL = "INSERT INTO centers " +
                                  "(centerId, name) VALUES " +
                                  "(?,?)"; 

        String insertSubCentersSQL = "INSERT INTO subCenters " +
                                     "(subCenterId, centerId, name) VALUES " +
                                     "(?,?,?)"; 
        try {
            connection = getDatabaseConnection(driver, url, username, password);
            preparedStatement = connection.prepareStatement(createUsersTableSQL);
            preparedStatement.executeUpdate();
            preparedStatement = connection.prepareStatement(createTablesTableSQL);
			preparedStatement.executeUpdate();
            preparedStatement = connection.prepareStatement(insertAdminUserSQL);
            preparedStatement.setString(1, "admin");
            preparedStatement.setString(2, "4cb9c8a8048fd02294477fcb1a41191a");
            preparedStatement.setInt(3, 2);
            preparedStatement.setString(4, "plaza@unidata.ucar.edu");
            preparedStatement.setString(5, "WMO Tables Admin");
            preparedStatement.setInt(6, 1);
            preparedStatement.setInt(7, 1);
            preparedStatement.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            preparedStatement.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
			preparedStatement.executeUpdate();
            // centers & subcenters
            preparedStatement = connection.prepareStatement(createCentersTableSQL);
            preparedStatement.executeUpdate();
            preparedStatement = connection.prepareStatement(insertCentersSQL);
            Map<String, String> centers = getCenters(servletContext);
            Iterator centerIterator = centers.entrySet().iterator();
	        while (centerIterator.hasNext()) {
		        Map.Entry mapEntry = (Map.Entry) centerIterator.next();
                preparedStatement.setInt(1, new Integer((String) mapEntry.getKey()).intValue());
                preparedStatement.setString(2, (String) mapEntry.getValue());
                preparedStatement.executeUpdate();
	        }

            preparedStatement = connection.prepareStatement(createSubCentersTableSQL);
            preparedStatement.executeUpdate();
            preparedStatement = connection.prepareStatement(insertSubCentersSQL);
            Map<String, Map> subCenters = getSubCenters(servletContext);
            Iterator subCenterIterator = subCenters.entrySet().iterator();
	        while (subCenterIterator.hasNext()) {
		        Map.Entry mapEntry = (Map.Entry) subCenterIterator.next();
                preparedStatement.setInt(1, new Integer((String) mapEntry.getKey()).intValue());
                Map<String, String> map = (Map<String, String>) mapEntry.getValue();
                Iterator sIterator = map.entrySet().iterator();
	            while (sIterator.hasNext()) {
                    Map.Entry mEntry = (Map.Entry) sIterator.next();
                    preparedStatement.setInt(2, new Integer((String) mEntry.getKey()).intValue());
                    preparedStatement.setString(3, (String) mEntry.getValue());
	            }
                preparedStatement.executeUpdate();
            }

        } catch (SQLException e) { 
            logger.error(e.getMessage()); 
        } finally { 
			if (preparedStatement != null) {
				preparedStatement.close();
			} 
			if (connection != null) {
				connection.close();
			} 
		} 
	}


    /**
     * Loads the appropriate JDBC driver and makes the database connection.
     *
     * @param driver  The jdbc driver to load.
     * @param url  The database url with which to make the connection.
     * @param username  The database username (null if not used).
     * @param password  The database password (null if not used).
     * @return  The the database connection.
     */
	private static Connection getDatabaseConnection(String driver, String url, String username, String password) { 
        Connection connection = null;
        try {
            Class.forName(driver); 
        } catch (ClassNotFoundException e) { 
            logger.error(e.getMessage()); 
        }

        try { 
            if ((username != null) && (password != null)){
                connection = DriverManager.getConnection(url, username, password);
            } else {
                connection = DriverManager.getConnection(url);
            }
            return connection; 
        } catch (SQLException e) {
            logger.error(e.getMessage()); ;
        } 
        return connection; 
    }


    private static Map<String, String> getCenters(ServletContext servletContext) {
        Map<String, String> map = new HashMap<String, String>();
        try {
            File centers = new File(servletContext.getRealPath("") + "/WEB-INF/classes/resources/centers.csv");
            if (centers.exists()) {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(centers ));
                String currentLine; 	     		
 			    while ((currentLine = bufferedReader.readLine()) != null) {
                    String[] lineComponents = StringUtils.normalizeSpace(currentLine).split("; ");
                    map.put(lineComponents[0], lineComponents[1]);
			    }
                bufferedReader.close();
            }
        } catch (Exception e) {            
            logger.error(e.getMessage());   
            throw new RuntimeException(e.getMessage());  
        }
        return map;
    }


    private static Map<String, Map> getSubCenters(ServletContext servletContext) {
        Map<String, Map> map = new HashMap<String, Map>();
        try {
            File subcenters = new File(servletContext.getRealPath("") + "/WEB-INF/classes/resources/subcenters.csv");
            if (subcenters.exists()) {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(subcenters));
                String currentLine; 	     		
 			    while ((currentLine = bufferedReader.readLine()) != null) {
                    String[] lineComponents = StringUtils.normalizeSpace(currentLine).split("; ");
                    Map<String, String> m = new HashMap<String, String>();
                    m.put(lineComponents[0], lineComponents[2]);
                    map.put(lineComponents[1], m);
			    }
                bufferedReader.close();
            }
        } catch (Exception e) {            
            logger.error(e.getMessage());   
            throw new RuntimeException(e.getMessage());                    
        }
        return map;
    }


}
