// $Id: DqcHandler.java,v 1.12 2006/01/20 20:42:04 caron Exp $

package thredds.dqc.server;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.*;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;

/**
 * Super-class that each DQC handler must extend.
 *
 * <p>Only two methods need to be implemented:</p>
 * <ul>
 *   <li>DqcHandler.initWithHandlerConfigDoc( InputStream, InputStream), and</li>
 *   <li>DqcHandler.handleRequest( HttpServletRequest, HttpServletResponse)</li>
 * </ul>
 * <p>The initWithHandlerConfigDoc() method does not need to do anything.
 * It is just there in case your handler needs some setup information.
 * </p>
 *
 * @author Ethan Davis
 * @version $Id: DqcHandler.java,v 1.12 2006/01/20 20:42:04 caron Exp $
 */
abstract public class DqcHandler
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( DqcHandler.class );

  protected DqcServletConfigItem getHandlerInfo() { return( this.handlerInfo ); }
  protected void setHandlerInfo( DqcServletConfigItem info) { this.handlerInfo = info; }
  private DqcServletConfigItem handlerInfo = null;

  /**
   * Return a DqcHandler using the handler information in the given DqcServletConfigItem.
   *
   * @param handlerInfo - the information about the requested DQC handler.
   * @param configPath - the file or resource path to the DqcServlet config files.
   * @return a DqcHandler as specified in the given DqcServletConfigItem.
   *
   *
   * @throws DqcHandlerInstantiationException if the requested DqcHandler cannot be instatiated.
   * @throws IOException if the config document could not be read.
   * @throws SecurityException (runtime)
   *
   */
  public static final DqcHandler factory( DqcServletConfigItem handlerInfo, String configPath )
          throws DqcHandlerInstantiationException, IOException
  {
    String tmpMsg = null;

    Class handlerClass = null;
    DqcHandler newHandler = null;

    // Get the Class instance for desired handler.
    try
    {
      handlerClass = Class.forName( handlerInfo.getHandlerClassName() );
    }
    catch ( ClassNotFoundException e )
    {
      tmpMsg = "Requested DqcHandler class not found <" + handlerInfo.getHandlerClassName() + ">: " + e.getMessage();
      throw( new DqcHandlerInstantiationException( tmpMsg, e));
    }
    log.debug( "factory(): loaded class for DqcHandler <" + handlerInfo.getHandlerClassName() + "> with given config doc." );

    // Instantiate the desired handler.
    try
    {
      newHandler = (DqcHandler) handlerClass.newInstance();
    }
    catch ( InstantiationException e )
    {
      tmpMsg = "Could not instantiate requested DqcHandler <" + handlerInfo.getHandlerClassName() + ">: " + e.getMessage();
      throw( new DqcHandlerInstantiationException( tmpMsg, e ) );
    }
    catch ( IllegalAccessException e )
    {
      tmpMsg = "Could not instantiate requested DqcHandler <" + handlerInfo.getHandlerClassName() + ">: " + e.getMessage();
      throw( new DqcHandlerInstantiationException( tmpMsg, e ) );
    }
    log.debug( "Instantiated DqcHandler <" + handlerInfo.getHandlerClassName() + ">.");

    // Provide the handler the setup information.
    newHandler.setHandlerInfo( handlerInfo );

    // Initialize the handler with a config document.
    URL configDocURL = null;
    if ( ! ( configPath == null || handlerInfo.getHandlerConfigFileName() == null))
    {
      File configFile = new File( configPath, handlerInfo.getHandlerConfigFileName() );
      if ( !configFile.canRead() )
      {
        configDocURL = handlerClass.getResource( configPath + "/" + handlerInfo.getHandlerConfigFileName() );
      }
      else
      {
        URI configDocURI = configFile.toURI();
        try
        {
          configDocURL = configDocURI.toURL();
        }
        catch ( MalformedURLException e )
        {
          tmpMsg = "Config file URL malformed <" + configDocURL.toString() + ">: " + e.getMessage();
          throw( new DqcHandlerInstantiationException( tmpMsg, e ) );
        }
      }
    }
    // If any of the information on the config document was null, then
    // initWithHandlerConfigDoc() is passed a null argument. This might
    // occur when a DqcHandler subclass does not require a config document.
    newHandler.initWithHandlerConfigDoc( configDocURL );
    log.debug( "Initialized DqcHandler with config documents.");

    return (newHandler);
  }

  /**
   * This abstract method initializes the DqcHandler from configuration information.
   *
   * This method must be implemented in your DqcHandler. However, if your handler
   * does not require and config information, this method does not have to do
   * anything (e.g., "{ return; }").
   *
   * @param handlerConfigDocURL - a URL to the DQC handler config document.
   * @throws IOException if trouble reading config file.
   * @throws IllegalArgumentException if config document does not contain the needed information.
   */
  abstract public void initWithHandlerConfigDoc( URL handlerConfigDocURL )
          throws IOException;

  /**
   * This abstract method is used by the DqcServlet to generate an HttpServletResponse
   * from the given HttpServletRequest.
   *
   * @param req - HttpServletRequest to a concrete DqcHandler.
   * @param res - HttpServletResponse generated for the given request.
   * @throws IOException if an I/O error is detected (when communicating with client not for servlet internal IO problems).
   * @throws ServletException if the request could not be handled for some reason.
   */
  abstract public void handleRequest( HttpServletRequest req, HttpServletResponse res )
          throws IOException, ServletException;
}

/*
 * $Log: DqcHandler.java,v $
 * Revision 1.12  2006/01/20 20:42:04  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.11  2005/09/30 21:51:37  edavis
 * Improve "Latest" DqcHandler so it can deal with new IDD naming conventions:
 * new configuration file format; add LatestDqcHandler which handles new and old
 * config file formats; use LatestDqcHandler as a proxy for LatestModel.
 *
 * Revision 1.10  2005/04/05 22:37:03  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.9  2004/08/23 16:45:20  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.8  2004/04/03 00:44:58  edavis
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
 * Revision 1.7  2004/03/05 06:32:45  edavis
 * Update the DqcHandler interface.
 *
 * Revision 1.6  2004/01/15 19:45:31  edavis
 * Changes made while adding support for the JPL QuikSCAT
 * DODS File Server catalog.
 *
 * Revision 1.5  2003/12/24 06:25:21  edavis
 * - Change factory() to use InputStream instead of File for config doc.
 * - Change from get/setHandlerConfig() to initWithHandlerConfigDoc().
 *
 * Revision 1.4  2003/12/11 01:31:47  edavis
 * Added logging. Added more error handling.
 *
 * Revision 1.3  2003/06/23 22:30:33  caron
 * test newline
 *
 * Revision 1.2  2003/05/06 22:13:22  edavis
 * Add some comments.
 *
 * Revision 1.1  2003/04/28 17:57:45  edavis
 * Initial checkin of THREDDS DqcServlet.
 *
 *
 */
