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
package thredds.server.views;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.util.Map;
import java.io.OutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import thredds.servlet.Debug;
import thredds.servlet.UsageLog;
import thredds.servlet.ServletUtil;
import ucar.nc2.util.cache.FileCacheRaf;
import ucar.nc2.util.IO;

/**
 *  Render the response to a request for a local file including byte range requests.
 *
 * <p>
 * This view supports the following model elements:
 * <pre>
 *  KEY                  OBJECT             Required
 *  ===                  ======             ========
 * "file"               java.io.File         yes
 * "contentType"        java.lang.String     no
 * "characterEncoding"  java.lang.Stringb    no
 * </pre>
 *
 * NOTE: If the content type is determined to be text, the character encoding
 * is assumed to be UTF-8 unless
 *
 * @author edavis
 * @since 4.0
 */
public class FileView extends AbstractView
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( FileView.class );

  private FileCacheRaf fileCacheRaf;
  public void setFileCacheRaf( FileCacheRaf fileCacheRaf) { this.fileCacheRaf = fileCacheRaf; }

  public void init()
  {
    if ( this.fileCacheRaf == null )
      this.fileCacheRaf = ServletUtil.getFileCache();
    if ( this.fileCacheRaf == null )
      throw new IllegalStateException( "FileCacheRaf not configured.");
  }

  protected void renderMergedOutputModel( Map model, HttpServletRequest req, HttpServletResponse res )
          throws Exception
  {
    if ( model == null || model.isEmpty() )
      throw new IllegalArgumentException( "Model must not be null or empty." );
    if ( ! model.containsKey( "file" ) )
      throw new IllegalArgumentException( "Model must contain \"file\" key." );
    Object o = model.get( "file" );
    if ( ! ( o instanceof File ) )
      throw new IllegalArgumentException( "Object mapped by \"file\" key  must be a File." );
    File file = (File) o;

    // Check that file exists and is not a directory.
    if ( ! file.isFile() )
    {
      // ToDo Send error or throw exception to be handled by Spring exception handling stuff.
//      throw new IllegalArgumentException( "File must exist and not be a directory." );
      log.info( "renderMergedOutputModel(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, 0 ) );
      res.sendError( HttpServletResponse.SC_BAD_REQUEST );
      return;
    }

    // Check if content type is specified.
    String contentType = null;
    if ( model.containsKey( "contentType"))
    {
      o = model.get( "contentType");
      if ( o instanceof String )
        contentType = (String) o;
    }

    // Check if content type is specified.
    String characterEncoding = null;
    if ( model.containsKey( "characterEncoding"))
    {
      o = model.get( "characterEncoding");
      if ( o instanceof String )
        characterEncoding = (String) o;
    }

    // Set the type of the file
    String filename = file.getPath();
    if ( null == contentType )
    {
      if ( filename.endsWith( ".html" ) )
        contentType = "text/html; charset=utf-8";
      else if ( filename.endsWith( ".xml" ) )
        contentType = "application/xml; charset=utf-8";
      else if ( filename.endsWith( ".txt" ) || filename.endsWith( ".log" ) || filename.endsWith( ".out" ) )
        contentType = "text/plain; charset=utf-8";
      else if ( filename.indexOf( ".log." ) > 0 )
        contentType = "text/plain; charset=utf-8";
      else if ( filename.endsWith( ".nc" ) )
        contentType = "application/x-netcdf";
      else
        contentType = this.getServletContext().getMimeType( filename );

      if ( contentType == null )
        contentType = "application/octet-stream";
    }

    // ToDo Do I need/want to do this?
    if ( characterEncoding == null )
    {
      if ( ( ! contentType.contains( "charset=") ) &&
           ( contentType.startsWith( "text/" ) ||
             contentType.startsWith( "application/xml")))
      {
        characterEncoding = "utf-8";
      }
    }

    // Set content type and character encoding as given/determined.
    res.setContentType( contentType );
    if ( characterEncoding != null )
      res.setCharacterEncoding( characterEncoding );

    // The rest of this is from John's thredds.servlet.ServletUtil.returnFile(...)
    // see if its a Range Request
    boolean isRangeRequest = false;
    long startPos = 0, endPos = Long.MAX_VALUE;
    String rangeRequest = req.getHeader( "Range" );
    if ( rangeRequest != null )
    { // bytes=12-34 or bytes=12-
      int pos = rangeRequest.indexOf( "=" );
      if ( pos > 0 )
      {
        int pos2 = rangeRequest.indexOf( "-" );
        if ( pos2 > 0 )
        {
          String startString = rangeRequest.substring( pos + 1, pos2 );
          String endString = rangeRequest.substring( pos2 + 1 );
          startPos = Long.parseLong( startString );
          if ( endString.length() > 0 )
            endPos = Long.parseLong( endString ) + 1;
          isRangeRequest = true;
        }
      }
    }

    // set content length
    long fileSize = file.length();
    long contentLength = fileSize;
    if ( isRangeRequest )
    {
      endPos = Math.min( endPos, fileSize );
      contentLength = endPos - startPos;
    }
    res.setContentLength( (int) contentLength );

    boolean debugRequest = Debug.isSet( "returnFile" );
    if ( debugRequest )
      log.debug( "renderMergedOutputModel(): filename = " + filename +
                 " contentType = " + contentType + " contentLength = " + contentLength );

    // indicate we allow Range Requests
    if ( ! isRangeRequest )
      res.addHeader( "Accept-Ranges", "bytes" );

    if ( req.getMethod().equals( "HEAD" ) )
    {
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, 0 ) );
      return;
    }

    try
    {
      if ( isRangeRequest )
      {
        // set before content is sent
        res.addHeader( "Content-Range", "bytes " + startPos + "-" + ( endPos - 1 ) + "/" + fileSize );
        res.setStatus( HttpServletResponse.SC_PARTIAL_CONTENT );

        FileCacheRaf.Raf craf = null;
        try
        {
          craf = fileCacheRaf.acquire( filename );
          IO.copyRafB( craf.getRaf(), startPos, contentLength, res.getOutputStream(), new byte[60000] );
          log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_PARTIAL_CONTENT, contentLength ));
          return;
        }
        finally
        {
          if ( craf != null ) fileCacheRaf.release( craf );
        }
      }

      // Return the file
      ServletOutputStream out = res.getOutputStream();
      IO.copyFileB( file, out, 60000 );
      res.flushBuffer();
      out.close();
      if ( debugRequest )
        log.debug( "renderMergedOutputModel(): file response ok = " + filename );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, contentLength ) );
    }
    // @todo Split up this exception handling: those from file access vs those from dealing with response
    //       File access: catch and res.sendError()
    //       response: don't catch (let bubble up out of doGet() etc)
    catch ( FileNotFoundException e )
    {
      log.error( "returnFile(): FileNotFoundException= " + filename );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0 ) );
      if ( ! res.isCommitted() ) res.sendError( HttpServletResponse.SC_NOT_FOUND );
    }
    catch ( java.net.SocketException e )
    {
      log.info( "returnFile(): SocketException sending file: " + filename + " " + e.getMessage() );
      log.info( UsageLog.closingMessageForRequestContext( ServletUtil.STATUS_CLIENT_ABORT, 0 ) ); 
    }
    catch ( IOException e )
    {
      String eName = e.getClass().getName(); // dont want compile time dependency on ClientAbortException
      if ( eName.equals( "org.apache.catalina.connector.ClientAbortException" ) )
      {
        log.info( "returnFile(): ClientAbortException while sending file: " + filename + " " + e.getMessage() );
        log.info( UsageLog.closingMessageForRequestContext( ServletUtil.STATUS_CLIENT_ABORT, 0 ) );
        return;
      }

      log.error( "returnFile(): IOException (" + e.getClass().getName() + ") sending file ", e );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ) );
      res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Problem sending file" );
    }
  }
}