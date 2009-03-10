package thredds.server.cdmvalidator;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.slf4j.MDC;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import thredds.servlet.UsageLog;
import thredds.servlet.ThreddsConfig;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.DiskCache;
import ucar.nc2.dataset.NetcdfDatasetInfo;
import ucar.unidata.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CdmValidatorController extends AbstractController
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( CdmValidatorController.class );

  private CdmValidatorContext cdmValidatorContext;

  private DiskCache2 cdmValidateCache = null;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1 );


  private DiskFileItemFactory factory;
  private File cacheDir;
  private long maxFileUploadSize;
  private boolean deleteImmediately = true;

  public void setCdmValidatorContext( CdmValidatorContext cdmValidatorContext )
  {
    this.cdmValidatorContext = cdmValidatorContext;
  }

  public void init()
  {
    log.info( "init(): start - " + UsageLog.setupNonRequestContext() );

    if ( this.cdmValidatorContext == null )
    {
      String msg = "Null CdmValidatorContext not allowed.";
      log.error( "init(): " + msg + " - " + UsageLog.closingMessageNonRequestContext());
      throw new IllegalStateException( msg );
    }

    // Configure CdmValidator: max upload size; cache dir and scheme.
    maxFileUploadSize = ThreddsConfig.getBytes( "CdmValidatorService.maxFileUploadSize", (long) 1000 * 1000 * 1000 );

    String cacheDirPath = ThreddsConfig.get( "CdmValidatorService.cache.dir", new File( this.cdmValidatorContext.getContentDirectory(), "cache").getPath() );

    int scourSecs = ThreddsConfig.getSeconds( "CdmValidatorService.cache.scour", -1 );
    int maxAgeSecs = ThreddsConfig.getSeconds( "CdmValidatorService.cache.maxAge", -1 );
    final long maxSize = ThreddsConfig.getBytes( "CdmValidatorService.cache.maxSize",
                                                 (long) 1000 * 1000 * 1000 ); // 1 Gbyte
    if ( maxAgeSecs > 0 )
    {
      // Setup cache used by CDM stack (uses DiskCache which is an older static disk cache impl).
      DiskCache.setRootDirectory( cacheDirPath );
      DiskCache.setCachePolicy( true );
      if ( ! scheduler.isShutdown() )
      {
        Runnable command = new Runnable()
        {
          public void run()
          {
            StringBuilder sb = new StringBuilder();
            DiskCache.cleanCache( maxSize, sb ); // 1 Gbyte
            sb.append( "----------------------\n" );
            log.debug( "init():Runnable:run(): Scour on ucar.nc2.util.DiskCache:\n" + sb );
          }
        };
        scheduler.scheduleAtFixedRate( command, scourSecs / 2, scourSecs, TimeUnit.SECONDS );
      }

      // Setup cache for file upload (uses DiskCache2 which is a newer disk cache impl).
      deleteImmediately = false;
      cdmValidateCache = new DiskCache2( cacheDirPath, false, maxAgeSecs / 60, scourSecs / 60 );
    }

    // Setup file upload factory.
    cacheDir = new File( cacheDirPath );
    if ( ! cacheDir.exists() && ! cacheDir.mkdirs() )
    {
      String msg = "File upload cache directory [" + cacheDir + "] doesn't exist and couldn't be created.";
      log.error( "init(): " + msg + " - " + UsageLog.closingMessageNonRequestContext() );
      throw new IllegalStateException( msg );
    }
    factory = new DiskFileItemFactory( 0, cacheDir ); // LOOK can also do in-memory

    log.info( "init(): done - " + UsageLog.closingMessageNonRequestContext() );
  }

  public void destroy()
  {
    if ( cdmValidateCache != null )
      cdmValidateCache.exit();

    if ( this.scheduler != null )
      this.scheduler.shutdown();
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest request,
                                                HttpServletResponse response )
          throws Exception
  {
    log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( request ) );

    // Get the request path.
    String reqPath = request.getPathInfo();
    if ( reqPath == null )
    {
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0 ) );
      response.sendError( HttpServletResponse.SC_NOT_FOUND );
      return null;
    }
    
    if ( request.getMethod().equalsIgnoreCase( "GET" ))
    {
      if ( reqPath.equals( "/cdmValidate.html" ))
      {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put( "contextPath", request.getContextPath() );
        model.put( "servletPath", request.getServletPath() );

        this.cdmValidatorContext.getHtmlConfig().addHtmlConfigInfoToModel( model );

        log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, -1 ) );
        return new ModelAndView( "/thredds/server/cdmvalidator/cdmValidate", model );
      }
      else if ( reqPath.equals( "/cdmValidateHelp.html" ))
      {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put( "contextPath", request.getContextPath() );
        model.put( "servletPath", request.getServletPath() );

        this.cdmValidatorContext.getHtmlConfig().addHtmlConfigInfoToModel( model );

        log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, -1 ) );
        return new ModelAndView( "/thredds/server/cdmvalidator/cdmValidate", model );
      }
      else
      {
        this.doGet( request, response );
      }
    }
    else if ( request.getMethod().equalsIgnoreCase( "POST" ))
      this.doPost( request, response );

    return null;
  }

  /**
   * GET handles the case where its a remote URL (dods or http)
   *
   * @param req request
   * @param res response
   * @throws javax.servlet.ServletException
   * @throws java.io.IOException
   */
  public void doGet( HttpServletRequest req, HttpServletResponse res )
          throws ServletException, IOException
  {

    log.info( "doGet(): " + UsageLog.setupRequestContext( req ) );

    String urlString = req.getParameter( "URL" );
    if ( urlString == null )
    {
      log.info( "doGet(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ) );
      res.sendError( HttpServletResponse.SC_BAD_REQUEST, "Must have a URL parameter" );
      return;
    }

    // validate the uri String
    try
    {
      URI uri = new URI( urlString );
      urlString = uri.toASCIIString(); // LOOK do we want just toString() ? Is this useful "input validation" ?
    }
    catch ( URISyntaxException e )
    {
      log.info( "doGet(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ) );
      res.sendError( HttpServletResponse.SC_BAD_REQUEST, "URISyntaxException on URU parameter" );
      return;
    }

    String xml = req.getParameter( "xml" );
    boolean wantXml = ( xml != null ) && xml.equals( "true" );

    try
    {
      int len = showValidatorResults( res, urlString, wantXml );
      log.info( "doGet(): URL = " + urlString );
      log.info( "doGet(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, len ) );

    }
    catch ( Exception e )
    {
      log.error( "doGet(): Validator internal error", e );
      log.info( "doGet(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ) );
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Validator internal error" );
    }

  }

  /**
   * POST handles uploaded files
   *
   * @param req request
   * @param res response
   * @throws ServletException
   * @throws IOException
   */
  public void doPost( HttpServletRequest req, HttpServletResponse res )
          throws ServletException, IOException
  {

    log.info( "doPost(): " + UsageLog.setupRequestContext( req ) );

    // Check that we have a file upload request
    boolean isMultipart = ServletFileUpload.isMultipartContent( req );
    if ( !isMultipart )
    {
      log.info( "doPost(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ) );
      res.sendError( HttpServletResponse.SC_BAD_REQUEST );
      return;
    }

    //Create a new file upload handler
    ServletFileUpload upload = new ServletFileUpload( factory );
    upload.setSizeMax( maxFileUploadSize );  // maximum bytes before a FileUploadException will be thrown

    List<FileItem> fileItems;
    try
    {
      fileItems = (List<FileItem>) upload.parseRequest( req );
    }
    catch ( FileUploadException e )
    {
      log.info( "doPost(): Validator FileUploadException", e );
      log.info( "doPost(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ) );
      res.sendError( HttpServletResponse.SC_BAD_REQUEST );
      return;
    }

    //Process the uploaded items
    String username = null;
    boolean wantXml = false;
    for ( FileItem item : fileItems )
    {
      if ( item.isFormField() )
      {
        if ( "username".equals( item.getFieldName() ) )
          username = item.getString();
        if ( "xml".equals( item.getFieldName() ) )
          wantXml = item.getString().equals( "true" );
      }
    }

    for ( FileItem item : fileItems )
    {
      if ( !item.isFormField() )
      {
        try
        {
          processUploadedFile( req, res, (DiskFileItem) item, username, wantXml );
          return;
        }
        catch ( Exception e )
        {
          log.info( "doPost(): Validator processUploadedFile", e );
          log.info( "doPost(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ) );
          res.sendError( HttpServletResponse.SC_BAD_REQUEST, e.getMessage() );
        }
      }
    }

  }

  private void processUploadedFile( HttpServletRequest req, HttpServletResponse res, DiskFileItem item,
                                    String username, boolean wantXml ) throws Exception
  {

    if ( ( username == null ) || ( username.length() == 0 ) )
      username = "none";
    username = StringUtil.filter( username, "_" );
    String filename = item.getName();
    filename = StringUtil.replace( filename, "/", "-" );
    filename = StringUtil.filter( filename, ".-_" );

    File uploadedFile = new File( cacheDir + "/" + username + "/" + filename );
    uploadedFile.getParentFile().mkdirs();
    item.write( uploadedFile );

    int len = showValidatorResults( res, uploadedFile.getPath(), wantXml );

    if ( deleteImmediately )
    {
      try
      {
        uploadedFile.delete();
      }
      catch ( Exception e )
      {
        log.error( "processUploadedFile(): Uploaded File = " + uploadedFile.getPath() + " delete failed = " + e.getMessage() );
      }
    }

    if ( req.getRemoteUser() == null )
    {
      if ( username != null )
        MDC.put( "userid", username );
    }

    log.info( "processUploadedFile(): Uploaded File = " + item.getName() + " sent to " + uploadedFile.getPath() + " size= " + uploadedFile.length() );
    log.info( "processUploadedFile(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, len ) );
  }

  private int showValidatorResults( HttpServletResponse res, String location, boolean wantXml ) throws Exception
  {

    NetcdfDatasetInfo info = null;
    try
    {
      info = new NetcdfDatasetInfo( location );

      String infoString;

      if ( wantXml )
      {
        infoString = info.writeXML();
        res.setContentLength( infoString.length() );
        res.setContentType( "text/xml; charset=iso-8859-1" );

      }
      else
      {
        Document xml = info.makeDocument();
        InputStream is = getXSLT();
        XSLTransformer transformer = new XSLTransformer( is );

        Document html = transformer.transform( xml );
        XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat() );
        infoString = fmt.outputString( html );

        res.setContentType( "text/html; charset=iso-8859-1" );
      }

      res.setContentLength( infoString.length() );

      OutputStream out = res.getOutputStream();
      out.write( infoString.getBytes() );
      out.flush();

      return infoString.length();

    }
    finally
    {
      if ( null != info )
        try
        {
          info.close();
        }
        catch ( IOException ioe )
        {
          log.error( "showValidatorResults(): Failed to close = " + location );
        }
    }
  }

  private InputStream getXSLT()
  {
    Class c = CdmValidatorController.class;
    String resource = "/WEB-INF/classes/resources/xsl/cdmValidation.xsl";
    InputStream is = c.getResourceAsStream( resource );
    if ( null == is )
      log.error( "getXSLT(): Cant load XSLT resource = " + resource );

    return is;
  }
}
