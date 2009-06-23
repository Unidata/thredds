/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.util;

import thredds.servlet.UsageLog;
import thredds.servlet.ServletUtil;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class RequestForwardUtils
{
  private static org.slf4j.Logger log
          = org.slf4j.LoggerFactory.getLogger( RequestForwardUtils.class );

  private RequestForwardUtils(){}

  public static void forwardRequestRelativeToCurrentContext( String fwdPath,
                                                             HttpServletRequest request,
                                                             HttpServletResponse response )
          throws IOException, ServletException
  {
    if ( fwdPath == null || request == null || response == null )
    {
      String msg = "Path, request, and response may not be null";
      log.error( "forwardRequestRelativeToCurrentContext(): " + msg + ( fwdPath == null ? ": " : "[" + fwdPath + "]: " )
                 + UsageLog.closingMessageForRequestContext( ServletUtil.STATUS_FORWARD_FAILURE, -1 ) );
      throw new IllegalArgumentException( msg + ".");
    }

    RequestDispatcher dispatcher = request.getRequestDispatcher( fwdPath );

    if ( dispatcherWasFound( fwdPath, dispatcher, response ) )
      forwardRequest( fwdPath, dispatcher, request, response );
  }
  public static void forwardRequestRelativeToGivenContext( String fwdPath,
                                                           ServletContext targetContext,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response )
          throws IOException, ServletException
  {
    if ( fwdPath == null || targetContext == null || request == null || response == null )
    {
      String msg = "Path, context, request, and response may not be null";
      log.error( "forwardRequestRelativeToGivenContext(): " + msg + (fwdPath == null ? ": " : "[" + fwdPath + "]: ")
                 + UsageLog.closingMessageForRequestContext( ServletUtil.STATUS_FORWARD_FAILURE, -1 ) );
      throw new IllegalArgumentException( msg + "." );
    }

    RequestDispatcher dispatcher = targetContext.getRequestDispatcher( fwdPath );

    if ( dispatcherWasFound( fwdPath, dispatcher, response ))
      forwardRequest( fwdPath, dispatcher, request, response );
  }

  public static void forwardRequest( String fwdPath,
                                     RequestDispatcher dispatcher,
                                     HttpServletRequest request,
                                     HttpServletResponse response )
          throws IOException, ServletException
  {
    if ( fwdPath == null || dispatcher == null || request == null || response == null )
    {
      String msg = "Path, dispatcher, request, and response may not be null";
      log.error( "forwardRequestRelativeToGivenContext(): " + msg + ( fwdPath == null ? ": " : "[" + fwdPath + "]: " )
                 + UsageLog.closingMessageForRequestContext( ServletUtil.STATUS_FORWARD_FAILURE, -1 ) );
      throw new IllegalArgumentException( msg + "." );
    }
    
    log.info( "forwardRequest() Forwarding request to \"" + fwdPath + "\": "
              + UsageLog.closingMessageForRequestContext( ServletUtil.STATUS_FORWARDED, -1 ) );
    dispatcher.forward( request, response );
  }

  private static boolean dispatcherWasFound( String fwdPath,
                                             RequestDispatcher dispatcher,
                                             HttpServletResponse response )
          throws IOException
  {
    if ( fwdPath == null || response == null )
    {
      String msg = "Path and response may not be null";
      log.error( "forwardRequestRelativeToGivenContext(): " + msg + ( fwdPath == null ? ": " : "[" + fwdPath + "]: " )
                 + UsageLog.closingMessageForRequestContext( ServletUtil.STATUS_FORWARD_FAILURE, -1 ) );
      throw new IllegalArgumentException( msg + "." );
    }

    if ( dispatcher == null )
    {
      log.error( "dispatcherWasFound(): Dispatcher for forwarding [" + fwdPath + "] not found:"
                 + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, -1 ) );
      response.sendError( HttpServletResponse.SC_NOT_FOUND );
      return false;
    }
    return true;
  }
}
