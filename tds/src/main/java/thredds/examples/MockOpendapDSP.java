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
// $Id: MockOpendapDSP.java 51 2006-07-12 17:13:13Z caron $
package thredds.examples;

import thredds.servlet.DataServiceProvider;
import thredds.servlet.ServletUtil;
import thredds.servlet.UsageLog;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;

/**
 * _more_
 *
 * @author edavis
 * @since Mar 24, 2006 2:22:23 PM
 */
public class MockOpendapDSP implements DataServiceProvider
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( MockOpendapDSP.class );

  public MockOpendapDSP() { }

  public DatasetRequest getRecognizedDatasetRequest( String path, HttpServletRequest req )
  {
    DatasetRequest dsReq;
    if ( path.endsWith( ".dds" ) )
      dsReq = new MyDsReq( path.substring( 0, path.lastIndexOf( ".dds" ) ),
                           ".dds", req.getQueryString() );
    else if ( path.endsWith( ".das" ) )
      dsReq = new MyDsReq( path.substring( 0, path.lastIndexOf( ".das" ) ),
                           ".das", req.getQueryString() );
    else if ( path.endsWith( ".dods" ) )
      dsReq = new MyDsReq( path.substring( 0, path.lastIndexOf( ".dods" ) ),
                           ".dods", req.getQueryString() );
    else if ( path.endsWith( ".ddx" ) )
      dsReq = new MyDsReq( path.substring( 0, path.lastIndexOf( ".ddx" ) ),
                           ".ddx", req.getQueryString() );
    else if ( path.endsWith( ".info" ) )
      dsReq = new MyDsReq( path.substring( 0, path.lastIndexOf( ".info" ) ),
                           ".info", req.getQueryString() );
    else if ( path.endsWith( ".html" ) )
      dsReq = new MyDsReq( path.substring( 0, path.lastIndexOf( ".html" ) ),
                           ".html", req.getQueryString() );
    else if ( path.endsWith( ".ver" ) )
      dsReq = new MyDsReq( path.substring( 0, path.lastIndexOf( ".ver" ) ),
                           ".ver", req.getQueryString() );
    else if ( path.endsWith( "/version" ) )
      dsReq = new MyDsReq( path.substring( 0, path.lastIndexOf( "/version" ) ),
                           "/version", req.getQueryString() );
    else if ( path.endsWith( "/version/" ) )
      dsReq = new MyDsReq( path.substring( 0, path.lastIndexOf( "/version/" ) ),
                           "/version/", req.getQueryString() );
    else
      dsReq = null;

    return dsReq;
  }

  public void handleRequestForDataset( DatasetRequest dsReq, CrawlableDataset crDs, HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    if ( dsReq == null ) throw new IllegalArgumentException( "DatasetRequest must not be null." );
    if ( crDs == null ) throw new IllegalArgumentException( "CrawlableDataset must not be null." );

    String path = dsReq.getDatasetPath();
    String ext = ( (MyDsReq) dsReq ).getDatasetExt();
    String ce = ( (MyDsReq) dsReq ).getDatasetCe();

    // Check if know how to handle the data request.
    if ( crDs instanceof CrawlableDatasetFile )
    {
      File crDsFile = ( (CrawlableDatasetFile) crDs ).getFile();
      if ( crDsFile.exists() )
      {
        PrintWriter out = res.getWriter();
        res.setContentType( "text/html" );
        res.setStatus( HttpServletResponse.SC_OK );
        StringBuffer responseString = new StringBuffer();
        responseString
                .append( getHtmlDoctypeAndOpenTag() )
                .append( "<head><title>Test Response to Data Request</title></head><body>\n" )
                .append( "<h1>Test Response to Data Request</h1>\n" )
                .append( "<ul>\n" )
                .append( "<li>Dataset requested: " ).append( path )
                .append( "</li>\n" )
                .append( "<li>File to serve: " ).append( crDsFile.getPath() )
                .append( "</li>\n" )
                .append( "<li>OPeNDAP request: " ).append( ext )
                .append( "</li>\n" )
                .append( "<li>OPeNDAP ce: " ).append( ce )
                .append( "</li>\n" )
                .append( "</p>\n" )
                .append( "</body></html>" );
        out.print( responseString.toString() );
        out.flush();

        log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, responseString.length() ));
        return;
      }
      else
      {
        log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1 ));
        res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Valid CrawlableDatasetFile but File returned by getFile() does not exist." );
        return;
      }
    }
//      else if ( crDs instanceof S4CrawlableDataset )
//      {
//        // serve data
//        return;
//      }
    // Don't know how to serve this type of CrawlableDataset
    else
    {
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Type of CrawlableDataset <> not undersood." );
      return;
    }
  }

  public void handleUnrecognizedRequestForCollection( CrawlableDataset crDs, HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    String newPath = req.getRequestURL().append( "/catalog.html" ).toString();
    ServletUtil.sendPermanentRedirect( newPath, req, res );
    return;
  }

  public void handleUnrecognizedRequest( CrawlableDataset crDs, HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    // Request not recognized as an OPeNDAP request.
    StringBuffer responseString = new StringBuffer();
    if ( false )
    {
      responseString.append( "Not an OPeNDAP request [URL must end in one of the following \".dds\", \".das\", \".dods\", \".html\", ... ." );

      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, responseString.length() ));
      res.sendError( HttpServletResponse.SC_BAD_REQUEST, responseString.toString() ); // 400
      // @todo Can set an error-page declaration in web.xml to specify HTML page for this error.
      return;
    }
    else
    {
      responseString
              .append( getHtmlDoctypeAndOpenTag() )
              .append( "<head><title>Not an OPeNDAP Request</title></head><body>\n" )
              .append( "<h1>Not an OPeNDAP Request</h1>\n" )
              .append( "<p>Expected URL to end in \".dds\", \".das\", \".dods\", \".html\",etc.\n" )
              .append( "</p>\n" )
              .append( "</body></html>" );

      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, responseString.length() ));
      PrintWriter out = res.getWriter();
      res.setContentType( "text/html" );
      res.setStatus( HttpServletResponse.SC_BAD_REQUEST );
      out.print( responseString.toString() );
      out.flush();

      return;
    }
  }

  private String getHtmlDoctypeAndOpenTag()
  {
    return new StringBuffer()
            .append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" )
            .append( "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" )
            .append( "<html>\n" )
            .toString();
  }

  private String getXHtmlDoctypeAndOpenTag()
  {
    return new StringBuffer()
            // .append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            .append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n" )
            .append( "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" )
            .append( "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">" )
            .toString();
  }

  private class MyDsReq implements DatasetRequest
  {
    private String _path;
    private String _ext;
    private String _ce;

    private MyDsReq( String path, String ext, String ce )
    {
      _path = path;
      _ext = ext;
      _ce = ce;
    }

    public String getDatasetPath()
    {
      return _path;
    }

    public String getDatasetExt()
    {
      return _ext;
    }

    public String getDatasetCe()
    {
      return _ce;
    }
  }
}

/*
 * $Log: MockOpendapDSP.java,v $
 * Revision 1.2  2006/04/18 20:39:54  edavis
 * Change context parameter "ContextPath" to "ContentPath".
 *
 * Revision 1.1  2006/03/30 23:22:10  edavis
 * Refactor THREDDS servlet framework, especially CatalogRootHandler and ServletUtil.
 *
 */