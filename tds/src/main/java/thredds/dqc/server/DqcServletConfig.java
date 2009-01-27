/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
// $Id: DqcServletConfig.java 51 2006-07-12 17:13:13Z caron $

package thredds.dqc.server;

import thredds.catalog.*;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The DqcServletConfig class keeps track of the configuration
 * of a DQC Servlet. The configuration is made up of all the
 * dataset collections being handled by this DQC Servlet.
 *
 */
public class DqcServletConfig
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( DqcServletConfig.class);

  /** The path to the config files. */
  private String configPath = null;

  /** The name of the config document. */
  private String configDocName = null;

  /** The XMLStore for the config file. */
  private XMLStore configStore = null;

  /** The preferences stored in the config file. */
  private PreferencesExt configPrefs = null;

  /** The collection of configuration items. */
  private Collection configItems = null;
  private HashMap configHash = new HashMap();

  private String dqcServletTitle = null;

  /**
   * <p> Create a new DqcServletConfig instance using the given configuration file.
   *
   * <p>The configuration file is a ucar.util.prefs.XMLStore that contains
   * one bean collection named "config". Each bean is a DqcServletConfigItem
   * containing information about the setup of a DQC handler. Here is an
   * example configuration document:</p>
   * <pre>
   * <?xml version='1.0' encoding='UTF-8'?>
   * <preferences EXTERNAL_XML_VERSION='1.0'>
   *   <root type='user'>
   *     <map>
   *       <beanCollection key='config' class='thredds.dqc.server.DqcServletConfigItem'>
   *         <bean name='example1' description='An example (1) DqcServletConfigItem'
   *               handlerName='an.example.dqc.Handler1' handlerConfigFileName='exampleConfig1.xml'
   *               dqcFileName='exampleDqc1.xml' />
   *         <bean name='example2' description='An example (2) DqcServletConfigItem'
   *               handlerName='an.example.dqc.Handler2' handlerConfigFileName='exampleConfig2.xml'
   *               dqcFileName='exampleDqc2.xml'
   *           />
   *       </beanCollection>
   *     </map>
   *   </root>
   * </preferences>
   * </pre>
   * <p>Several other examples are distributed as part of the DqcServlet
   * distribution.</p>
   *
   * @param configPath - the path to the DqcServlet configuration files.
   * @param configFileName - the name of the DqcServlet configuration file.
   *
   * @throws IOException if can't create XMLStore from given file.
   * @throws NullPointerException if either the path or the filename parameters are null.
   * @throws RuntimeException if ucar.util.prefs has problems (not clear when these situations might occur).
   */
  public DqcServletConfig( File configPath, String configFileName )
          throws IOException
  {
    if ( configPath == null || configFileName == null )
    {
      log.error( "DqcServletConfig(): Config path or filename arguments were null." );
      throw( new java.lang.NullPointerException( "Config path or filename arguments were null." ) );
    }
    this.configPath = configPath.getAbsolutePath();
    this.configDocName = configFileName;

    // Check that given config file exists and is readable
    File configFile = new File( configPath, configFileName );
    if ( ! configFile.exists() )
    {
      log.error( "DqcServletConfig(File): Config file <" + configFile.getAbsolutePath() + "> does not exist.");
      throw new IOException( "Config file <" + configFileName + "> does not exist.");
    }
    if ( ! configFile.canRead() )
    {
      log.error( "DqcServletConfig(File): Can't read config file <" + configFile.getAbsolutePath() + ">." );
      throw new IOException( "Can't read config file <" + configFileName + ">." );
    }

    // Open XMLStore.
    log.debug( "DqcServletConfig(File): Opening XMLStore with given file <" + configFile.getAbsolutePath() + ">." );
    this.configStore = XMLStore.createFromFile( configFile.getAbsolutePath(), null);

    if ( this.configStore == null )
    {
      // This doesn't ever seem to happen, even for non-XMLStore documents.
      log.error( "DqcServletConfig(): Config XMLStore is null after createFromFile(<" + configFile.getAbsolutePath() + ">)." );
      throw( new RuntimeException( "Config XMLStore is null after createFromFile(<" + configFileName + ">)." ) );
      // @todo Decide on a more appropriate/specific RuntimeException than a plain RuntimeException.
    }

    this.setup();
  }


  /**
   * Used for testing. Same functionality as DqcServletConfig( File, String).
   *
   * @param configDocResourcePath - the path to the resource containing the configuration document.
   * @param configDocResourceName - the name of the resource containing the configuration document.
   * @throws IOException if can't create XMLStore from resource.
   * @throws NullPointerException if the resource name parameter is null.
   * @throws RuntimeException if ucar.util.prefs has problems (not clear when these situations might occur).
   */
  protected DqcServletConfig( String configDocResourcePath, String configDocResourceName )
          throws IOException
  {
    if ( configDocResourcePath == null || configDocResourceName == null )
    {
      log.error( "DqcServletConfig(): Config document argument was null." );
      throw( new java.lang.NullPointerException( "Config document argument was null." ) );
    }
    this.configPath = configDocResourcePath;
    this.configDocName = configDocResourceName;

    String resourceName = this.configPath + "/" + this.configDocName;

    // Open XMLStore using the given configuration file.
    log.debug( "DqcServletConfig(String): Opening XMLStore with given resource <" + resourceName + ">." );
    this.configStore = XMLStore.createFromResource( resourceName, null );

    if ( this.configStore == null )
    {
      // This doesn't ever seem to happen, even for non-XMLStore documents.
      String tmpString = "Config XMLStore is null after createFromResource(\"" + resourceName + "\", null).";
      log.error( "DqcServletConfig(): " + tmpString );
      // @todo Decide on a more appropriate/specific RuntimeException than a plain RuntimeException.
      throw( new RuntimeException( tmpString ) );
    }

    this.setup();
  }

  private void setup()
  {
    // Get preferences from the XMLStore.
    log.debug( "setup(): get root preferences node from XMLStore.");
    this.configPrefs = this.configStore.getPreferences();
    if ( this.configPrefs == null )
    {
      // This doesn't seem to ever happen, even with a non-XMLStore document.
      String tmpString = "Config XMLStore's root preferences is null.";
      log.error( "setup(): " + tmpString );
      // @todo Decide on a more appropriate/specific RuntimeException than a plain RuntimeException.
      throw( new RuntimeException( tmpString ) );
    }

    // Get preference items, i.e., the collection of Beans named "config".
    log.debug( "setup(): get \"config\" preference items.");
    this.configItems = (Collection) this.configPrefs.getBean( "config", new ArrayList());

    if ( this.configItems.isEmpty() )
    {
      log.debug( "setup(): Config doc has no DqcServletConfigItem entries.");
      // @todo Might want to throw an exception here.
      return;
    }

    // If configItems is not empty, initialize and schedule each task.
    log.debug( "setup(): Config doc has one or more DqcServletConfigItem entries.");

    DqcServletConfigItem curItem = null;
    java.util.Iterator iter = this.configItems.iterator();
    while ( iter.hasNext())
    {
      curItem =  (DqcServletConfigItem) iter.next();

      log.debug( "Add config item <" + curItem.getName() + ">.");
      this.configHash.put( curItem.getName(), curItem);
    }
  }

  public String getDqcServletTitle() { return( "dqcServletTitle unknown (TODO: add title to config"); }
  // @todo Add DqcServlet title information to the configuration.

  /** Return an iterator of the config items in this config. */
  public java.util.Iterator getIterator()
  { return( this.configItems.iterator()); }

  /** Find an item with the given name. */
  public DqcServletConfigItem findItem( String itemName)
  {
    return( (DqcServletConfigItem) configHash.get( itemName));
  }

  public boolean addItem( DqcServletConfigItem item)
  {
    log.debug( "addItem( item): start.");
    if ( item.getName() == null )
    {
      log.debug( "addItem( item): item name is null.");
      return( false);
    }
    if ( this.findItem( item.getName()) != null)
    {
      log.debug( "addItem( item): item with same name alread exists (" + item.getName() + ").");
      return( false);
    }

    this.configItems.add( item);
    this.configHash.put( item.getName(), item);

    log.debug( "addItem( item): item added (" + item.getName() + ").");
    return( true );
  }

  public boolean addItem( String name, String description,
                          String handlerName, String handlerConfigFileName )
  {
    log.debug( "addItem( name, ...): start.");
    DqcServletConfigItem item = null;
    item = new DqcServletConfigItem( name, description, handlerName,
                                     handlerConfigFileName);
    log.debug( "addItem( name, ...): passing to addItem( item).");
    return( this.addItem( item) );
  }

  /* Write the configuration to the XMLStore. */
  public void writeConfig( OutputStream os)
    throws java.io.IOException
  {
    log.debug( "writeConfig(): Create \"config\" preferences.");
    this.configPrefs.putBeanCollection( "config", configItems);

    log.debug( "writeConfig() Write \"config\" preferences to XMLStore (OutputStream).");
    this.configStore.save( os);
  }

  /**
   * A convenience routine for writing a DqcServlet config file.
   *
   * @param fileName
   * @param name
   * @param description
   * @param handlerName
   * @param handlerConfigFileName
   * @throws java.io.IOException
   */
  public static void writeExampleConfig( String fileName, String name, String description,
                                         String handlerName, String handlerConfigFileName )
    throws java.io.IOException
  {
    log.debug( "Write example \"config\" preferences to (new) XMLStore.");

    // Open the preferences file.
    XMLStore configStore = XMLStore.createFromFile( fileName, null);

    // Get preferences.
    PreferencesExt configPrefs = configStore.getPreferences();

    // Setup a DqcServletConfigItem.
    DqcServletConfigItem item = null;
    Collection itemList = new ArrayList();;

    item = new DqcServletConfigItem( name, description, handlerName,
                                     handlerConfigFileName );
    itemList.add( item);

    // Add collection of DqcServletConfigItems to prefs.
    configPrefs.putBeanCollection( "config", itemList);

    // Write to the XMLStore.
    configStore.save();

    return;
  }

  public InvCatalog createCatalogRepresentation( String servletURL)
  {
    String serviceName = "myDqcServlet";

    // Create the catalog, service, and top-level dataset.
    InvCatalogImpl catalog = new InvCatalogImpl( null, null, null );
    InvService myService = new InvService( serviceName, ServiceType.RESOLVER.toString(), servletURL, null, null );
    InvDatasetImpl topDs = new InvDatasetImpl( null, "DqcServlet Available Datasets" );
                                       // OR ( null, this.mainConfig.getDqcServletTitle() );
    // Add service and top-level dataset to the catalog.
    catalog.addService( myService );
    catalog.addDataset( topDs );

    // Add a dataset to the catalog for each dataset handled by this server.
    DqcServletConfigItem curItem = null;
    Iterator it = this.getIterator();
    InvDatasetImpl curDs = null;
    InvDocumentation curDoc = null;
    StringBuffer docContent = null;
    while ( it.hasNext() )
    {
      curItem = (DqcServletConfigItem) it.next();
      curDs = new InvDatasetImpl( topDs, curItem.getDescription(), null, serviceName, curItem.getName() + ".xml");
      docContent = new StringBuffer();
      docContent.append( curItem.getDescription() + "\n")
                .append( "Using the DqcHandler " + curItem.getHandlerClassName())
                .append( " with config file " + curItem.getHandlerConfigFileName() + ".");
      curDoc = new InvDocumentation( null,null, null, null, docContent.toString() );
      curDs.addDocumentation( curDoc);
      topDs.addDataset( curDs );
    }

    // Tie up any loose ends in catalog with finish().
    ( (InvCatalogImpl) catalog ).finish();

    return( catalog );
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer();

    buf.append( "DqcServletConfig (" );

    DqcServletConfigItem curItem = null;
    Iterator it = this.getIterator();
    String prefix = "";
    while ( it.hasNext() )
    {
      curItem = (DqcServletConfigItem) it.next();
      buf.append( prefix );
      buf.append( curItem.toString() );
      prefix = ", ";
    }
    buf.append( ")");

    return ( buf.toString() );
  }
}
/*
 * $Log: DqcServletConfig.java,v $
 * Revision 1.8  2006/01/20 20:42:04  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.7  2005/07/13 22:48:07  edavis
 * Improve server logging, includes adding a final log message
 * containing the response time for each request.
 *
 * Revision 1.6  2005/04/05 22:37:03  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.5  2004/08/23 16:45:20  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.4  2004/04/03 00:44:58  edavis
 * DqcServlet:
 * - Start adding a service that returns a catalog listing all the DQC docs
 *   available from a particular DqcServlet installation (i.e., DqcServlet
 *   config to catalog)
 * JplQuikSCAT:
 * - fix how the modulo nature of longitude selection is handled
 * - improve some log messages, remove some that drastically increase
 *   the size of the log file; fix some 
 * - fix some template strings
 *
 * Revision 1.3  2003/12/11 01:15:51  edavis
 * Add logging, general cleanup, add the addItem() method.
 *
 * Revision 1.2  2003/05/06 22:14:07  edavis
 * Add writeExampleConfig() and toString().
 *
 * Revision 1.1  2003/04/28 17:57:45  edavis
 * Initial checkin of THREDDS DqcServlet.
 *
 */

