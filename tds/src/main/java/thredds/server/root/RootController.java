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
package thredds.server.root;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.LastModified;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import thredds.servlet.ServletUtil;
import thredds.server.config.TdsContext;

import java.io.File;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class RootController extends AbstractController implements LastModified
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( RootController.class );

  private TdsContext tdsContext;

  public void setTdsContext( TdsContext tdsContext )
  {
    this.tdsContext = tdsContext;
  }

  public void init()
  {
    WebApplicationContext webAppContext = this.getWebApplicationContext();
    ServletContext sc = webAppContext.getServletContext();
    initContent();
  }

  private void initContent()
          //throws javax.servlet.ServletException
  {

    // first time, create content directory
    File initialContentDirectory = tdsContext.getStartupContentDirectory();
    if ( initialContentDirectory.exists() )
    {
      try
      {
        if ( ServletUtil.copyDir( initialContentDirectory.toString(),
                                  tdsContext.getContentDirectory().toString() ) );
          //log.info( "copyDir " + initialContentPath + " to " + contentPath );
      }
      catch ( IOException ioe )
      {
        //log.error( "failed to copyDir " + initialContentPath + " to " + contentPath, ioe );
      }
    }

  }

  public void destroy()
  {
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest req, HttpServletResponse res )
          throws Exception
  {
    String path = getPath( req );
    if ( path.equals( "/" ))
    {
      String newPath = tdsContext.getContextPath() + "/catalog.html";
      res.sendRedirect( newPath );
      return null;
    }

    File file = tdsContext.getPublicDocFileSource().getFile( path );
    if ( file == null )
    {
      tdsContext.getDefaultRequestDispatcher().forward( req, res );
      return null;
    }
    return new ModelAndView( "threddsFileView", "file", file );
  }

  public long getLastModified( HttpServletRequest req )
  {
    String path = getPath( req );
    File file = tdsContext.getPublicDocFileSource().getFile( path );
    if ( file == null )
      return -1;
    long lastModTime = file.lastModified();
    if ( lastModTime == 0L )
      return -1;
    return lastModTime;
  }

  private String getPath( HttpServletRequest req )
  {
    String path = req.getPathInfo();
    if ( path == null || path.equals( "" ) )
      path = req.getServletPath();

    if ( path == null )
      return null;

    if ( ! path.equals( "/" ) )
      path = StringUtils.trimLeadingCharacter( path, '/' );

    return path;
  }
}
