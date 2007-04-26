package thredds.servlet;

import thredds.catalog.*;
import thredds.datatype.DateType;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Format;

import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;

/**
 * Provide methods to write HTML representations of a catalog, directory, or CDM dataset to an HTTP response.
 *
 * HtmlWriter is implemented as a singleton. Before HtmlWriter can be used it
 * must be initialized with init(...). The singleton instance can then be
 * obtained with getInstance().
 *
 * @author edavis
 * @since Feb 24, 2006 3:18:50 PM
 */
public class HtmlWriter
{
  static private org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( HtmlWriter.class );

  private static HtmlWriter singleton;

  private String contextPath;
  private String contextName;
  private String contextVersion;
  private String userCssPath; // relative to context path
  private String contextLogoPath;   // relative to context path
  private String contextLogoAlt;    // Alternate text for logo
  private String instituteLogoPath; // relative to context path
  private String instituteLogoAlt;  // Alternate text for logo
  private String docsPath;    // relative to context path
  private String folderIconPath; // relative to context path
  private String folderIconAlt; // alternate text for folder icon

  private ucar.nc2.units.DateFormatter formatter = new ucar.nc2.units.DateFormatter();

  /*
   * <li>Context path: "/thredds"</li>
 * <li>Servlet name: "THREDDS Data Server"</li>
 * <li>Documentation location: "/thredds/docs/"</li>
 * <li>Version information: ThreddsDefault.version</li>
 * <li>Catalog reference URL: "/thredds/catalogServices?catalog="</li>

  */

  /**
   * Initialize the HtmlWriter singleton instance.
   *
   * Note: All paths must be relative to the context path.
   *
   * @param contextPath the context path for this web app (e.g., "/thredds")
   * @param contextName the name of the web app (e.g., "THREDDS Data Server")
   * @param contextVersion the version of the web app (e.g., "3.14.00")
   * @param docsPath the path for the main documentation page (e.g., "docs/")
   * @param userCssPath the path for the CSS document (e.g., "upc.css")
   * @param contextLogoPath the path for the context logo (e.g., "thredds.jpg")
   * @param contextLogoAlt alternate text for the context logo (e.g., "thredds")
   * @param instituteLogoPath the path for the institute logo (e.g., "unidataLogo.jpg")
   * @param instituteLogoAlt alternate text for the institute logo (e.g., "Unidata")
   * @param folderIconPath the path for the folder icon (e.g., "folder.gif"), try to keep small, ours is 20x22 pixels
   * @param folderIconAlt alternate text for the folder icon (e.g., "folder")
   */
  public static void init( String contextPath, String contextName, String contextVersion,
                           String docsPath, String userCssPath,
                           String contextLogoPath, String contextLogoAlt,
                           String instituteLogoPath, String instituteLogoAlt,
                           String folderIconPath, String folderIconAlt )
  {
    if ( singleton != null )
    {
      log.warn( "init(): this method has already been called; it should only be called once." );
      return;
      //throw new IllegalStateException( "HtmlWriter.init() has already been called.");
    }
    singleton = new HtmlWriter( contextPath, contextName, contextVersion,
                                docsPath, userCssPath,
                                contextLogoPath, contextLogoAlt,
                                instituteLogoPath, instituteLogoAlt,
                                folderIconPath, folderIconAlt );
  }

  public static HtmlWriter getInstance()
  {
    if ( singleton == null )
    {
      log.warn( "getInstance(): init() has not been called.");
      return null;
      //throw new IllegalStateException( "HtmlWriter.init() has not been called." );
    }
    return singleton;
  }

  /** @noinspection UNUSED_SYMBOL*/
  private HtmlWriter() {}

  private HtmlWriter( String contextPath, String contextName, String contextVersion,
                      String docsPath, String userCssPath,
                      String contextLogoPath, String contextLogoAlt,
                      String instituteLogoPath, String instituteLogoAlt,
                      String folderIconPath, String folderIconAlt )
  {
    this.contextPath = contextPath;
    this.contextName = contextName;
    this.contextVersion = contextVersion;
    this.docsPath = docsPath;
    this.userCssPath = userCssPath;
    this.contextLogoPath = contextLogoPath;
    this.contextLogoAlt = contextLogoAlt;
    this.instituteLogoPath = instituteLogoPath;
    this.instituteLogoAlt = instituteLogoAlt;
    this.folderIconPath = folderIconPath;
    this.folderIconAlt = folderIconAlt;
  }

  public String getContextPath() { return contextPath; }
  public String getContextName() { return contextName; }
  public String getContextVersion() { return contextVersion; }
  public String getContextLogoPath() { return contextLogoPath; }
  //public String getUserCssPath() { return userCssPath; }
  //public String getInstituteLogoPath() { return instituteLogoPath; }
  public String getDocsPath() { return docsPath; }

  public String getHtmlDoctypeAndOpenTag()
  {
    return new StringBuffer()
            .append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n")
            .append( "        \"http://www.w3.org/TR/html4/loose.dtd\">\n")
            .append( "<html>\n")
            .toString();
  }
  public String getXHtmlDoctypeAndOpenTag()
  {
    return new StringBuffer()
            // .append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            .append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n")
            .append( "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n")
            .append( "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">")
            .toString();
  }

//  public static final String UNIDATA_CSS
  public String getUserCSS()
  {
    return new StringBuffer()
            .append( "<link rel='stylesheet' href='")
            .append( this.contextPath)
            .append( "/").append( userCssPath).append("' type='text/css' >").toString();
  }


//  public static final String UNIDATA_HEAD
  public String getUserHead()
  {
    return new StringBuffer()
            .append( "<table width=\"100%\"><tr><td>\n")
            .append( "  <img src=\"").append( contextPath).append("/").append( instituteLogoPath ).append("\"\n")
            .append( "       alt=\"").append( instituteLogoAlt).append("\"\n")
            .append( "       align=\"left\" valign=\"top\"\n")
            .append( "       hspace=\"10\" vspace=\"2\">\n")
            .append( "  <h3><strong>").append( contextName).append("</strong></h3>\n" )
            .append( "</td></tr></table>\n")
            .toString();
  }


//  private static final String TOMCAT_CSS
  private String getTomcatCSS()
  {
    return new StringBuffer()
            .append( "H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " )
            .append( "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} " )
            .append( "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} " )
            .append( "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} " )
            .append( "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} " )
            .append( "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}" )
            .append( "A {color : black;}" )
            .append( "A.name {color : black;}" )
            .append( "HR {color : #525D76;}")
            .toString();
  }

  /**
   * Write a file directory.
   *
   * @param dir  directory
   * @param path the URL path reletive to the base
   */
  public void writeDirectory( HttpServletResponse res, File dir, String path )
          throws IOException
  {
    // error checking
    if ( dir == null )
    {
      res.sendError( HttpServletResponse.SC_NOT_FOUND );
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      return;
    }

    if ( !dir.exists() || !dir.isDirectory() )
    {
      res.sendError( HttpServletResponse.SC_NOT_FOUND );
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      return;
    }

    // Get directory as HTML
    String dirHtmlString = getDirectory( path, dir );

    res.setContentLength( dirHtmlString.length() );
    res.setContentType( "text/html; charset=iso-8859-1" );

    // LOOK faster to use PrintStream instead of PrintWriter
    // Return an input stream to the underlying bytes
    // Prepare a writer
    OutputStreamWriter osWriter;
    try
    {
      osWriter = new OutputStreamWriter( res.getOutputStream(), "UTF8" );
    }
    catch ( java.io.UnsupportedEncodingException e )
    {
      // Should never happen
      osWriter = new OutputStreamWriter( res.getOutputStream() );
    }
    PrintWriter writer = new PrintWriter( osWriter );
    writer.write( dirHtmlString );
    writer.flush();

    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, dirHtmlString.length() );
  }

  private String getDirectory( String path, File dir )
  {
    StringBuffer sb = new StringBuffer();

    // Render the page header
    sb.append( getHtmlDoctypeAndOpenTag() ); // "<html>\n" );
    sb.append( "<head>\r\n" );
    sb.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" );
    sb.append( "<title>" );
    sb.append( "Directory listing for " ).append( path );
    sb.append( "</title>\r\n" );
    sb.append( "<STYLE type='text/css'><!--" );
    sb.append( this.getTomcatCSS() );
    sb.append( "--></STYLE>\r\n" );
    sb.append( "</head>\r\n" );
    sb.append( "<body>\r\n" );
    sb.append( "<h1>" );
    sb.append( "Directory listing for " ).append( path );

    // Render the link to our parent (if required)
    String parentDirectory = path;
    if ( parentDirectory.endsWith( "/" ) )
    {
      parentDirectory =
              parentDirectory.substring( 0, parentDirectory.length() - 1 );
    }
    int slash = parentDirectory.lastIndexOf( '/' );
    if ( slash >= 0 )
    {
      String parent = parentDirectory.substring( 0, slash );
      sb.append( " - <a href=\"" );
      if ( parent.equals( "" ) )
      {
        parent = "/";
      }
      sb.append( "../" ); // sb.append(encode(parent));
      //if (!parent.endsWith("/"))
      //  sb.append("/");
      sb.append( "\">" );
      sb.append( "<b>" );
      sb.append( "Up to " ).append( parent );
      sb.append( "</b>" );
      sb.append( "</a>" );
    }

    sb.append( "</h1>\r\n" );
    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<table width=\"100%\" cellspacing=\"0\"" +
               " cellpadding=\"5\" align=\"center\">\r\n" );

    // Render the column headings
    sb.append( "<tr>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Filename" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"center\"><font size=\"+1\"><strong>" );
    sb.append( "Size" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"right\"><font size=\"+1\"><strong>" );
    sb.append( "Last Modified" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "</tr>" );

    // Render the directory entries within this directory
    boolean shade = false;
    File[] children = dir.listFiles();
    List fileList = Arrays.asList( children );
    Collections.sort( fileList );
    for ( int i = 0; i < fileList.size(); i++ )
    {
      File child = (File) fileList.get( i );

      String childname = child.getName();
      if ( childname.equalsIgnoreCase( "WEB-INF" ) ||
           childname.equalsIgnoreCase( "META-INF" ) )
      {
        continue;
      }

      if ( child.isDirectory() ) childname = childname + "/";
      //if (!endsWithSlash) childname = path + "/" + childname; // client removes last path if no slash

      sb.append( "<tr" );
      if ( shade )
      {
        sb.append( " bgcolor=\"#eeeeee\"" );
      }
      sb.append( ">\r\n" );
      shade = !shade;

      sb.append( "<td align=\"left\">&nbsp;&nbsp;\r\n" );
      sb.append( "<a href=\"" );
      //sb.append( encode(contextPath));
      // resourceName = encode(path + resourceName);
      sb.append( childname );
      sb.append( "\"><tt>" );
      sb.append( childname );
      sb.append( "</tt></a></td>\r\n" );

      sb.append( "<td align=\"right\"><tt>" );
      if ( child.isDirectory() )
      {
        sb.append( "&nbsp;" );
      }
      else
      {
        sb.append( renderSize( child.length() ) );
      }
      sb.append( "</tt></td>\r\n" );

      sb.append( "<td align=\"right\"><tt>" );
      sb.append( formatter.toDateTimeString( new Date( child.lastModified() ) ) );
      sb.append( "</tt></td>\r\n" );

      sb.append( "</tr>\r\n" );
    }

    // Render the page footer
    sb.append( "</table>\r\n" );
    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<h3>" ).append( this.contextVersion );
    sb.append( " <a href='").append(this.contextPath).append(this.docsPath).append("'> Documentation</a></h3>\r\n" );
    sb.append( "</body>\r\n" );
    sb.append( "</html>\r\n" );

    return sb.toString();
  }

  private String renderSize( long size )
  {

    long leftSide = size / 1024;
    long rightSide = ( size % 1024 ) / 103;   // Makes 1 digit
    if ( ( leftSide == 0 ) && ( rightSide == 0 ) && ( size > 0 ) )
    {
      rightSide = 1;
    }

    return ( "" + leftSide + "." + rightSide + " kb" );
  }

  public void writeCatalog( HttpServletResponse res, InvCatalogImpl cat, boolean isLocalCatalog )
          throws IOException
  {
    String catHtmlAsString = convertCatalogToHtml( cat, isLocalCatalog );

    res.setContentLength( catHtmlAsString.length() );
    res.setContentType( "text/html; charset=iso-8859-1" );

    // Write it out
    OutputStreamWriter osWriter;
    try
    {
      osWriter = new OutputStreamWriter( res.getOutputStream(), "UTF8" );
    }
    catch ( java.io.UnsupportedEncodingException e )
    {
      // Should never happen
      osWriter = new OutputStreamWriter( res.getOutputStream() );
    }
    PrintWriter writer = new PrintWriter( osWriter );
    writer.write( catHtmlAsString );
    writer.flush();

    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, catHtmlAsString.length() );
  }

  /**
   * Write a catalog in HTML, make it look like a file directory.
   *
   * @param cat catalog to write
   */
  private String convertCatalogToHtml( InvCatalogImpl cat, boolean isLocalCatalog )
  {
    StringBuffer sb = new StringBuffer( 10000 );

    String catname = StringUtil.quoteHtmlContent( cat.getUriString() );

    // Render the page header
    sb.append( getHtmlDoctypeAndOpenTag() ); // "<html>\n" );
    sb.append( "<head>\r\n" );
    sb.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" );
    sb.append( "<title>" );
    sb.append( "Catalog " ).append( catname );
    sb.append( "</title>\r\n" );
    sb.append( "<STYLE type='text/css'><!--" );
    sb.append( this.getTomcatCSS() );
    sb.append( "--></STYLE> " );
    sb.append( "</head>\r\n" );
    sb.append( "<body>" );
    sb.append( "<h1>" );
    sb.append( "Catalog " ).append( catname );
    sb.append( "</h1>" );
    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<table width=\"100%\" cellspacing=\"0\"" +
               " cellpadding=\"5\" align=\"center\">\r\n" );

    // Render the column headings
    sb.append( "<tr>\r\n" );
    sb.append( "<th align=\"left\"><font size=\"+1\">" );
    sb.append( "Dataset" );
    sb.append( "</font></th>\r\n" );
    sb.append( "<th align=\"center\"><font size=\"+1\">" );
    sb.append( "Size" );
    sb.append( "</font></th>\r\n" );
    sb.append( "<th align=\"right\"><font size=\"+1\">" );
    sb.append( "Last Modified" );
    sb.append( "</font></th>\r\n" );
    sb.append( "</tr>" );

    // Recursively render the datasets
    boolean shade = false;
    shade = doDatasets( cat, cat.getDatasets(), sb, shade, 0, isLocalCatalog );

    // Render the page footer
    sb.append( "</table>\r\n" );

    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<h3>" ).append( this.contextVersion );
    sb.append( " <a href='" ).append( contextPath ).append( "/").append(this.docsPath).append("'> Documentation</a></h3>\r\n" );
    sb.append( "</body>\r\n" );
    sb.append( "</html>\r\n" );

    return ( sb.toString() );
  }

  private boolean doDatasets( InvCatalogImpl cat, List datasets, StringBuffer sb, boolean shade, int level, boolean isLocalCatalog )
  {
    URI catURI = cat.getBaseURI();
    String catHtml;
    if ( !isLocalCatalog )
    {
      // Setup HREF url to link to HTML dataset page (more below).
      catHtml = contextPath + "/catalog.html?cmd=subset&catalog=" + cat.getUriString() + "&";
      // Can't be "/catalogServices?..." because subset decides on xml or html by trailing ".html" on URL path 
    }
    else
    { // replace xml with html
      catHtml = cat.getUriString();
      int pos = catHtml.lastIndexOf( '.' );
      if (pos < 0)
        catHtml = catHtml + "catalog.html?";
      else
        catHtml = catHtml.substring( 0, pos ) + ".html?";
    }

    for ( int i = 0; i < datasets.size(); i++ )
    {
      InvDatasetImpl ds = (InvDatasetImpl) datasets.get( i );
      String name = StringUtil.quoteHtmlContent( ds.getName() );

      sb.append( "<tr" );
      if ( shade )
      {
        sb.append( " bgcolor=\"#eeeeee\"" );
      }
      sb.append( ">\r\n" );
      shade = !shade;

      sb.append( "<td align=\"left\">" );
      for ( int j = 0; j <= level; j++ )
      {
        sb.append( "&nbsp;&nbsp;&nbsp;&nbsp;" );
      }
      sb.append( "\r\n" );

      if ( ds instanceof InvCatalogRef )
      {
        InvCatalogRef catref = (InvCatalogRef) ds;
        String href = catref.getXlinkHref();
        if ( ! isLocalCatalog )
        {
          URI hrefUri = cat.getBaseURI().resolve( href);
          href = hrefUri.toString();
        }
        try {
          URI uri = new URI(href);
          if (uri.isAbsolute()) {
            href = contextPath + "/catalogServices?catalog=" + href;
          } else {
            int pos = href.lastIndexOf('.');
            href = href.substring(0, pos) + ".html";
          }

        } catch (URISyntaxException e) {
          log.error(href, e);
        }

        sb.append( "<img src='").append( contextPath ).append( "/" ).append( this.folderIconPath )
                   .append("' alt='").append(this.folderIconAlt).append("'> &nbsp;");
        sb.append( "<a href=\"" );
        sb.append( StringUtil.quoteHtmlContent( href ) );
        sb.append( "\"><tt>" );
        sb.append( name );
        sb.append( "/</tt></a></td>\r\n" );
      }
      else // Not an InvCatalogRef
      {
        if (ds.hasNestedDatasets())
          sb.append( "<img src='").append( contextPath ).append( "/" ).append( this.folderIconPath )
                  .append("' alt='").append( this.folderIconAlt ).append("'> &nbsp;");

        // Check if dataset has single resolver service.
        if ( ds.getAccess().size() == 1 &&
             ( (InvAccess) ds.getAccess().get( 0)).getService().getServiceType().equals( ServiceType.RESOLVER ) )
        {
          InvAccess access = (InvAccess) ds.getAccess().get( 0);
          String accessUrlName = access.getUnresolvedUrlName();
          int pos = accessUrlName.lastIndexOf( ".xml");
          if ( pos != -1 )
            accessUrlName = accessUrlName.substring( 0, pos ) + ".html";
          sb.append( "<a href=\"" );
          sb.append( StringUtil.quoteHtmlContent( accessUrlName ) );
          sb.append( "\"><tt>" );
          String tmpName = name;
          if ( tmpName.endsWith( ".xml"))
          {
            tmpName = tmpName.substring( 0, tmpName.lastIndexOf( '.' ) );
          }
          sb.append( tmpName );
          sb.append( "</tt></a></td>\r\n" );
        }
        // Dataset with an ID.
        else if ( ds.getID() != null )
        {
          // Write link to HTML dataset page.
          sb.append( "<a href=\"" );
          // sb.append("catalog.html?cmd=subset&catalog=");
          sb.append( StringUtil.quoteHtmlContent( catHtml ) );
          sb.append( "dataset=" );
          sb.append( StringUtil.quoteHtmlContent( ds.getID() ) );
          sb.append( "\"><tt>" );
          sb.append( name );
          sb.append( "</tt></a></td>\r\n" );
        }
        // Dataset without an ID.
        else
        {
          sb.append( "<tt>" );
          sb.append( name );
          sb.append( "</tt></td>\r\n" );
        }
      }

      sb.append( "<td align=\"right\"><tt>" );
      double size = ds.getDataSize();
      if ( ( size != 0.0 ) && !Double.isNaN( size ) )
      {
        sb.append( Format.formatByteSize( size ) );
      }
      else
      {
        sb.append( "&nbsp;" );
      }
      sb.append( "</tt></td>\r\n" );

      sb.append( "<td align=\"right\"><tt>" );

      // Get last modified time.
      DateType lastModDateType = ds.getLastModifiedDate();
      if ( lastModDateType == null )
      {
        if ( ! ds.hasAccess())
          sb.append( "--");// "");
        else
          sb.append( "--");// "Unknown");
      }
      else
      {
        if ( lastModDateType.isPresent() )
          sb.append( formatter.toDateTimeString( new Date() ) );
        if ( lastModDateType.getDate() != null )
          sb.append( formatter.toDateTimeString( lastModDateType.getDate() ) );
      }

      sb.append( "</tt></td>\r\n" );

      sb.append( "</tr>\r\n" );

      if ( !( ds instanceof InvCatalogRef ) )
      {
        shade = doDatasets( cat, ds.getDatasets(), sb, shade, level + 1, isLocalCatalog );
      }
    }

    return shade;
  }

  /**
   * Show CDM compliance (ccordinate systems, etc) of a NetcdfDataset.
   *
   * @param ds  dataset to write
   */
  public void showCDM( HttpServletResponse res , NetcdfDataset ds )
          throws IOException
  {
    String cdmAsString = getCDM( ds);

    res.setContentLength( cdmAsString.length() );
    res.setContentType( "text/html; charset=iso-8859-1" );

    // Write it out
    OutputStreamWriter osWriter;
    try
    {
      osWriter = new OutputStreamWriter( res.getOutputStream(), "UTF8" );
    }
    catch ( UnsupportedEncodingException e )
    {
      // Should never happen
      osWriter = new OutputStreamWriter( res.getOutputStream() );
    }
    PrintWriter writer = new PrintWriter( osWriter );
    writer.write( cdmAsString );
    writer.flush();

    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, cdmAsString.length() );

  }

  private String getCDM( NetcdfDataset ds )
  {
    StringBuffer sb = new StringBuffer( 10000 );

    String name = StringUtil.quoteHtmlContent( ds.getLocation() );

    // Render the page header
    sb.append( getHtmlDoctypeAndOpenTag() ); // "<html>\n" );
    sb.append( "<head>\r\n" );
    sb.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" );
    sb.append( "<title>" );
    sb.append( "Common Data Model" );
    sb.append( "</title>\r\n" );
    sb.append( "<STYLE type='text/css'><!--" );
    sb.append( this.getTomcatCSS() );
    sb.append( "--></STYLE> " );
    sb.append( "</head>\r\n" );
    sb.append( "<body>" );
    sb.append( "<h1>" );
    sb.append( "Dataset ").append( name );
    sb.append( "</h1>" );
    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<table width=\"100%\" cellspacing=\"0\"" +
               " cellpadding=\"5\" align=\"center\">\r\n" );

    //////// Axis
    sb.append( "<tr>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Axis" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Type" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Units" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "</tr>" );

    // Show the coordinate axes
    boolean shade = false;
    List axes = ds.getCoordinateAxes();
    for ( int i = 0; i < axes.size(); i++ )
    {
      CoordinateAxis axis = (CoordinateAxis) axes.get( i );
      showAxis( axis, sb, shade );
      shade = !shade;
    }

    ///////////// Grid
    GridDataset gds = new ucar.nc2.dt.grid.GridDataset( ds );

    // look for projections
    //List gridsets = gds.getGridsets();

    sb.append( "<tr>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "GeoGrid" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Description" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "<td align=\"left\"><font size=\"+1\"><strong>" );
    sb.append( "Units" );
    sb.append( "</strong></font></td>\r\n" );
    sb.append( "</tr>" );

    // Show the grids
    shade = false;
    List grids = gds.getGrids();
    for ( int i = 0; i < grids.size(); i++ )
    {
      GridDatatype grid = (GridDatatype) grids.get( i );
      showGrid( grid, sb, shade );
      shade = !shade;
    }

    // Render the page footer
    sb.append( "</table>\r\n" );

    sb.append( "<HR size=\"1\" noshade=\"noshade\">" );

    sb.append( "<h3>" ).append( this.contextVersion );
    sb.append( " <a href='" ).append( contextPath ).append( "/" ).append( this.docsPath ).append( "'> Documentation</a></h3>\r\n" );
    sb.append( "</body>\r\n" );
    sb.append( "</html>\r\n" );

    return( sb.toString());
  }

  private void showAxis( CoordinateAxis axis, StringBuffer sb, boolean shade )
  {

    sb.append( "<tr" );
    if ( shade )
    {
      sb.append( " bgcolor=\"#eeeeee\"" );
    }
    sb.append( ">\r\n" );
    shade = !shade;

    sb.append( "<td align=\"left\">" );
    sb.append( "\r\n" );

    StringBuffer sbuff = new StringBuffer();
    axis.getNameAndDimensions( sbuff, false, true );
    String name = StringUtil.quoteHtmlContent( sbuff.toString() );
    sb.append( "&nbsp;" );
    sb.append( name );
    sb.append( "</tt></a></td>\r\n" );

    sb.append( "<td align=\"left\"><tt>" );
    AxisType type = axis.getAxisType();
    String stype = ( type == null ) ? "" : StringUtil.quoteHtmlContent( type.toString() );
    sb.append( stype );
    sb.append( "</tt></td>\r\n" );

    sb.append( "<td align=\"left\"><tt>" );
    String units = axis.getUnitsString();
    String sunits = ( units == null ) ? "" : units;
    sb.append( sunits );
    sb.append( "</tt></td>\r\n" );

    sb.append( "</tr>\r\n" );
  }

  private void showGrid( GridDatatype grid, StringBuffer sb, boolean shade )
  {

    sb.append( "<tr" );
    if ( shade )
    {
      sb.append( " bgcolor=\"#eeeeee\"" );
    }
    sb.append( ">\r\n" );
    shade = !shade;

    sb.append( "<td align=\"left\">" );
    sb.append( "\r\n" );

    VariableEnhanced ve = grid.getVariable();
    StringBuffer sbuff = new StringBuffer();
    ve.getNameAndDimensions( sbuff, false, true );
    String name = StringUtil.quoteHtmlContent( sbuff.toString() );
    sb.append( "&nbsp;" );
    sb.append( name );
    sb.append( "</tt></a></td>\r\n" );

    sb.append( "<td align=\"left\"><tt>" );
    String desc = ve.getDescription();
    String sdesc = ( desc == null ) ? "" : StringUtil.quoteHtmlContent( desc );
    sb.append( sdesc );
    sb.append( "</tt></td>\r\n" );

    sb.append( "<td align=\"left\"><tt>" );
    String units = ve.getUnitsString();
    String sunits = ( units == null ) ? "" : units;
    sb.append( sunits );
    sb.append( "</tt></td>\r\n" );

    sb.append( "</tr>\r\n" );
  }

}
