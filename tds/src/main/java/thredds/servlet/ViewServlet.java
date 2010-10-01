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

package thredds.servlet;

import thredds.catalog.InvProperty;
import thredds.server.wms.Godiva2Viewer;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.IO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;

import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvAccess;
import thredds.catalog.ServiceType;
import ucar.unidata.util.StringUtil;

/**
 * Catalog Serving
 *
 * handles /view/*
 */
public class ViewServlet extends AbstractServlet {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViewServlet.class);
  static private HashMap<String,String> templates = new HashMap<String,String>();
  static private ArrayList<Viewer> viewerList;
  static {
    viewerList = new ArrayList<Viewer>();
    registerViewer( new IDV());
    registerViewer( new ToolsUI());
    registerViewer( new Godiva2Viewer() );
    registerViewer( new StaticView());
  }

 static public void registerViewer( String className) {
   Class vClass;
   try {
     vClass = ViewServlet.class.getClassLoader().loadClass(className);
   } catch (ClassNotFoundException e) {
     log.error("Attempt to load Viewer class "+className+" not found");
     return;
   }

   if (!(Viewer.class.isAssignableFrom( vClass))) {
     log.error("Attempt to load class "+className+" does not implement "+Viewer.class.getName());
     return;
   }

   // create instance of the class
   Object instance;
   try {
     instance = vClass.newInstance();
   } catch (InstantiationException e) {
     log.error("Attempt to load Viewer class "+className+" cannot instantiate, probably need default Constructor.");
     return;
   } catch (IllegalAccessException e) {
     log.error("Attempt to load Viewer class "+className+" is not accessible.");
     return;
   }

    registerViewer( (Viewer) instance);
  }

  static public void registerViewer(Viewer v) {
    viewerList.add( v);
  }

  static private String getTemplate( String path) {
    String template = templates.get( path);
    if (template != null) return template;

    try {
      template = IO.readFile(path);
    } catch (IOException ioe) {
      return null;
    }

    templates.put( path, template);
    return template;
  }

  public void init() throws ServletException
  {
    super.init();
    logServerStartup.info( getClass().getName() + " initialization done -  " + UsageLog.closingMessageNonRequestContext() );
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    log.info( UsageLog.setupRequestContext( req ));

    String path = req.getPathInfo();
    int pos = path.lastIndexOf("/");
    String filename = "views/" + path.substring(pos + 1);
    log.debug("**ViewManager req= "+path+" look for "+ServletUtil.getRootPath() + "/" + filename);

    String template = getTemplate( ServletUtil.getRootPath() + "/WEB-INF/" +filename);
    if (template == null)
      template = getTemplate( contentPath  + "/" +filename);
    if (template == null) {
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0 ));
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    StringBuffer sbuff = new StringBuffer( template);

    Enumeration params = req.getParameterNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      String values[] = req.getParameterValues(name);
      if (values != null) {
        String sname = "{"+name+"}";
        for (String value : values) {
          StringUtil.substitute(sbuff, sname, value); // multiple ok
        }
      }
    }

    try {
      res.setContentType("application/x-java-jnlp-file");
      ServletUtil.returnString(sbuff.toString(), res);
      // System.out.println(" jnlp="+sbuff.toString());

    } catch (Throwable t) {
      log.error(" jnlp="+sbuff.toString(), t);
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
      if ( ! res.isCommitted() ) res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  static public void showViewers( StringBuilder sbuff, InvDatasetImpl dataset, HttpServletRequest req) {
    int count = 0;
    for (Viewer viewer : viewerList) {
      if (viewer.isViewable(dataset)) count++;
    }
    if (count == 0) return;

    sbuff.append("<h3>Viewers:</h3><ul>\r\n");

    for (Viewer viewer : viewerList)
    {
      if (viewer.isViewable(dataset))
      {
        if ( viewer instanceof ViewerLinkProvider )
        {
          List<ViewerLinkProvider.ViewerLink> sp = ( (ViewerLinkProvider) viewer ).getViewerLinks( dataset, req );
          for ( ViewerLinkProvider.ViewerLink vl : sp )
            if ( vl.getUrl() != null & !vl.getUrl().equals( "" ) )
              sbuff.append( "<li><a href='" ).append( vl.getUrl() )
                      .append( "'>" ).append( vl.getTitle() != null ? vl.getTitle() : vl.getUrl() )
                      .append( "</a></li>\n" );

        } else {
          String viewerLinkHtml = viewer.getViewerLinkHtml( dataset, req );
          if ( viewerLinkHtml != null )
          {
            sbuff.append( "  <li> " );
            sbuff.append( viewerLinkHtml );
            sbuff.append( "</li>\n" );
          }
        }
      }
    }
    sbuff.append("</ul>\r\n");
  }

  /* static private void showViews( StringBuffer sbuff, InvDatasetImpl dataset, String viewer) {
    List list = View.getViews(); // findViews( dataset.getParentCatalog().getUriString(), dataset.getID(), viewer);
    if (list.size() == 0) return;
    //sbuff.append("<h4>Contributed Views:</h4>\n<ol>\n");
    sbuff.append("<ul>\n");
    for (int i=0; i<list.size(); i++) {
      View v = (View) list.get(i);
      v.writeHtml( sbuff);
    }
    sbuff.append("\n</ul><p>\n");
  } */

  // must end with "/"
  protected String getPath() { return "view/";  }
  protected void makeDebugActions() { }

  private static class ToolsUI implements Viewer {

    public  boolean isViewable( InvDatasetImpl ds) {
      String id = ds.getID();
      return ((id != null) && ds.hasAccess());
    }

    public String  getViewerLinkHtml( InvDatasetImpl ds, HttpServletRequest req) {
      String base = ds.getParentCatalog().getUriString();
      if (base.endsWith(".html"))
        base = base.substring(0, base.length()-5)+".xml";
      Formatter query = new Formatter();
      query.format("<a href='%s/view/ToolsUI.jnlp?", req.getContextPath());
      query.format("catalog=%s&amp;dataset=%s'>NetCDF-Java ToolsUI (webstart)</a>",  base, ds.getID());
      return query.toString();
    }
  }

  private static class IDV implements Viewer {

    public boolean isViewable( InvDatasetImpl ds) {
      InvAccess access = getOpendapAccess( ds );
      if (access == null) return false;

      FeatureType dt = ds.getDataType();
      if (dt != FeatureType.GRID) return false;
      return true;
    }

    public String getViewerLinkHtml( InvDatasetImpl ds, HttpServletRequest req) {
      InvAccess access = getOpendapAccess( ds );

      URI dataURI = access.getStandardUri();
      if (!dataURI.isAbsolute()) {
        try {
          URI base = new URI( req.getRequestURL().toString());
          dataURI = base.resolve( dataURI);
          // System.out.println("Resolve URL with "+req.getRequestURL()+" got= "+dataURI.toString());
        } catch (URISyntaxException e) {
          log.error("Resolve URL with "+req.getRequestURL(),e);
        }
      }

      return "<a href='" + req.getContextPath() + "/view/idv.jnlp?url="+dataURI.toString()+"'>Integrated Data Viewer (IDV) (webstart)</a>";
    }

    private InvAccess getOpendapAccess( InvDatasetImpl ds )
    {
      InvAccess access = ds.getAccess( ServiceType.DODS);
      if (access == null) access = ds.getAccess(ServiceType.OPENDAP);
      return access;
    }
  }

  protected static class StaticView implements ViewerLinkProvider {

    private final String propertyNamePrefix = "viewer";

    public  boolean isViewable( InvDatasetImpl ds) {
      return hasViewerProperties( ds );
    }

    public String  getViewerLinkHtml( InvDatasetImpl ds, HttpServletRequest req)
    {
      List<ViewerLink> viewerLinks = getViewerLinks( ds, req );
      if ( viewerLinks.isEmpty())
        return null;
      ViewerLink firstLink = viewerLinks.get( 0 );
      return "<a href='" + firstLink.getUrl() + "'>" + firstLink.getTitle() + "</a>";
    }

    @Override
    public List<ViewerLink> getViewerLinks( InvDatasetImpl ds, HttpServletRequest req )
    {
      List<InvProperty> viewerProperties = findViewerProperties( ds );
      if ( viewerProperties.isEmpty() )
        return Collections.emptyList();
      List<ViewerLink> result = new ArrayList<ViewerLink>();
      for ( InvProperty p : viewerProperties )
      {
        ViewerLink viewerLink = parseViewerPropertyValue( p.getName(), p.getValue(), ds, req );
        if ( viewerLink != null )
          result.add( viewerLink );
      }
      return result;
    }

    private ViewerLink parseViewerPropertyValue( String viewerName, String viewerValue, InvDatasetImpl ds, HttpServletRequest req )
    {
      String viewerUrl;
      String viewerTitle;

      int lastCommaLocation = viewerValue.lastIndexOf( "," );
      if ( lastCommaLocation != -1 )
      {
        viewerUrl = viewerValue.substring( 0, lastCommaLocation );
        viewerTitle = viewerValue.substring( lastCommaLocation + 1 );
        if ( viewerUrl.equals( "" ) )
          return null;
        if ( viewerTitle.equals( "" ) )
          viewerTitle = viewerName;
      } else {
        viewerUrl = viewerValue;
        viewerTitle = viewerName;
      }
      viewerUrl = StringUtil.quoteHtmlContent( sub( viewerUrl, ds, req ) );

      ViewerLink viewerLink = new ViewerLink( viewerTitle, viewerUrl );
      return viewerLink;
    }

    private boolean hasViewerProperties( InvDatasetImpl ds)
    {
      for ( InvProperty p : ds.getProperties() )
        if ( p.getName().startsWith( propertyNamePrefix))
          return true;

      return false;
    }
    private List<InvProperty> findViewerProperties( InvDatasetImpl ds )
    {
      List<InvProperty> result = new ArrayList<InvProperty>();
      for ( InvProperty p : ds.getProperties() )
        if ( p.getName().startsWith( propertyNamePrefix ) )
          result.add( p);

      return result;
    }


  private String sub( String org, InvDatasetImpl ds, HttpServletRequest req )
  {
    List<InvAccess> access = ds.getAccess();
    if ( access.size() == 0 ) return org;

    // look through all access for {serviceName}
    for ( InvAccess acc : access )
    {
      String sname = "{" + acc.getService().getServiceType() + "}";
      if ( org.indexOf( sname ) >= 0 )
        return StringUtil.substitute( org, sname, acc.getStandardUri().toString() );
    }

    String sname = "{url}";
    if ( ( org.indexOf( sname ) >= 0 ) && ( access.size() > 0 ) )
    {
      InvAccess acc = access.get( 0 ); // just use the first one
      return StringUtil.substitute( org, sname, acc.getStandardUri().toString() );
    }

    return org;
  }
}

}
