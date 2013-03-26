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
import java.util.List;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
            createDatabase(wmotablesHome, databaseSelected);
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
    public void createDatabase(String wmotablesHome, String databaseSelected)  {
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
	    		    createTables(derbyDriver, derbyUrl + ";create=true", null, null);
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
    private static void createTables(String driver, String url, String username, String password) throws SQLException { 
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String createUsersTableSQL = "CREATE TABLE users" +
                                     "(" +
                                     "userId INTEGER primary key not null GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                                     "userName VARCHAR(100) not null, " +
                                     "emailAddress VARCHAR(100) not null, " +
                                     "fullName VARCHAR(100) not null, " +
                                     "affiliation VARCHAR(255) not null, " +
                                     "dateCreated TIMESTAMP not null, " +
                                     "dateModified TIMESTAMP not null" +
                                     ")";

        String createTablesTableSQL = "CREATE TABLE tables" +
                                      "(" +
                                      "tableId INTEGER primary key not null GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
                                      "title VARCHAR(100) not null, " +
                                      "description VARCHAR(255) not null, " +
                                      "originalName VARCHAR(100) not null, " +
                                      "version VARCHAR(100) not null, " + 
                                      "md5 CHAR(32) not null, " +
                                      "visibility SMALLINT not null, " +
                                      "userId INTEGER not null, " +
                                      "dateCreated TIMESTAMP not null, " +
                                      "dateModified TIMESTAMP not null" +
                                      ")";
 
        try {
            connection = getDatabaseConnection(driver, url, username, password);
            preparedStatement = connection.prepareStatement(createUsersTableSQL);
            preparedStatement.executeUpdate();
            preparedStatement = connection.prepareStatement(createTablesTableSQL);
			preparedStatement.executeUpdate();
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
}
