/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.viewer.dataservice;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import thredds.catalog.InvAccess;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvProperty;
import thredds.catalog.ServiceType;
import thredds.server.wms.Godiva2Viewer;
import thredds.servlet.Viewer;
import thredds.servlet.ViewerLinkProvider;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.IO;
import ucar.unidata.util.StringUtil2;

@Service
public class ViewerServiceImpl implements ViewerService {
  private static Logger logger = LoggerFactory.getLogger(ViewerServiceImpl.class);

  public static ViewerLinkProvider getStaticView() {
    return new StaticView();
  }

  private List<Viewer> viewers = new ArrayList<>();
  private HashMap<String, String> templates = new HashMap<>();

  @Override
  public List<Viewer> getViewers() {

    return null;
  }

  @Override
  public Viewer getViewer(String viewer) {

    return null;
  }

  @Override
  public boolean registerViewer(Viewer v) {

    return viewers.add(v);
  }

  @Override
  public String getViewerTemplate(String path) {

    String template = templates.get(path);
    if (template != null)
      return template;

    try {
      template = IO.readFile(path);
    } catch (IOException ioe) {
      return null;
    }

    templates.put(path, template);
    return template;

  }

  @Override
  public void showViewers(StringBuilder sbuff, InvDatasetImpl dataset, HttpServletRequest req) {

    int count = 0;
    for (Viewer viewer : viewers) {
      if (viewer.isViewable(dataset)) count++;
    }
    if (count == 0) return;

    sbuff.append("<h3>Viewers:</h3><ul>\r\n");

    for (Viewer viewer : viewers) {
      if (viewer.isViewable(dataset)) {
        if (viewer instanceof ViewerLinkProvider) {
          List<ViewerLinkProvider.ViewerLink> sp = ((ViewerLinkProvider) viewer).getViewerLinks(dataset, req);
          for (ViewerLinkProvider.ViewerLink vl : sp)
            if (vl.getUrl() != null && !vl.getUrl().equals(""))
              sbuff.append("<li><a href='").append(vl.getUrl())
                      .append("'>").append(vl.getTitle() != null ? vl.getTitle() : vl.getUrl())
                      .append("</a></li>\n");

        } else {
          String viewerLinkHtml = viewer.getViewerLinkHtml(dataset, req);
          if (viewerLinkHtml != null) {
            sbuff.append("  <li> ");
            sbuff.append(viewerLinkHtml);
            sbuff.append("</li>\n");
          }
        }
      }
    }
    sbuff.append("</ul>\r\n");
  }

  @SuppressWarnings("unused")
  @PostConstruct
  private void registerViewers() {
    registerViewer(new Godiva2Viewer());
    registerViewer(new ToolsUI());
    registerViewer(new IDV());
    registerViewer(new StaticView());

  }

  // Viewers...
  // ToolsUI
  private static class ToolsUI implements Viewer {

    public boolean isViewable(InvDatasetImpl ds) {
      String id = ds.getID();
      return ((id != null) && ds.hasAccess());
    }

    public String getViewerLinkHtml(InvDatasetImpl ds, HttpServletRequest req) {
      String base = ds.getParentCatalog().getUriString();
      if (base.endsWith(".html"))
        base = base.substring(0, base.length() - 5) + ".xml";
      Formatter query = new Formatter();
      query.format("<a href='%s/view/ToolsUI.jnlp?", req.getContextPath());
      query.format("catalog=%s&amp;dataset=%s'>NetCDF-Java ToolsUI (webstart)</a>", base, ds.getID());
      return query.toString();
    }
  }

  // IDV
  private static class IDV implements Viewer {

    public boolean isViewable(InvDatasetImpl ds) {
      InvAccess access = getOpendapAccess(ds);
      if (access == null)
        return false;

      FeatureType dt = ds.getDataType();
      return dt == FeatureType.GRID;
    }

    public String getViewerLinkHtml(InvDatasetImpl ds, HttpServletRequest req) {
      InvAccess access = getOpendapAccess(ds);
      if (access == null)
        return null;

      URI dataURI = access.getStandardUri();
      if (dataURI == null) {
        logger.warn("IDVViewer access URL failed on {}", ds.getName());
        return null;
      }
      if (!dataURI.isAbsolute()) {
        try {
          URI base = new URI(req.getRequestURL().toString());
          dataURI = base.resolve(dataURI);
          // System.out.println("Resolve URL with "+req.getRequestURL()+" got= "+dataURI.toString());
        } catch (URISyntaxException e) {
          logger.error("Resolve URL with " + req.getRequestURL(), e);
        }
      }

      return "<a href='" + req.getContextPath() + "/view/idv.jnlp?url="
              + dataURI.toString()
              + "'>Integrated Data Viewer (IDV) (webstart)</a>";
    }

    private InvAccess getOpendapAccess(InvDatasetImpl ds) {
      InvAccess access = ds.getAccess(ServiceType.DODS);
      if (access == null)
        access = ds.getAccess(ServiceType.OPENDAP);
      return access;
    }
  }

  private static final String propertyNamePrefix = "viewer";


  // LOOK whats this for ??
  private static class StaticView implements ViewerLinkProvider {


    public boolean isViewable(InvDatasetImpl ds) {
      return hasViewerProperties(ds);
    }

    public String getViewerLinkHtml(InvDatasetImpl ds, HttpServletRequest req) {
      List<ViewerLink> viewerLinks = getViewerLinks(ds, req);
      if (viewerLinks.isEmpty())
        return null;
      ViewerLink firstLink = viewerLinks.get(0);
      return "<a href='" + firstLink.getUrl() + "'>" + firstLink.getTitle() + "</a>";
    }

    @Override
    public List<ViewerLink> getViewerLinks(InvDatasetImpl ds, HttpServletRequest req) {
      List<InvProperty> viewerProperties = findViewerProperties(ds);
      if (viewerProperties.isEmpty())
        return Collections.emptyList();
      List<ViewerLink> result = new ArrayList<>();
      for (InvProperty p : viewerProperties) {
        ViewerLink viewerLink = parseViewerPropertyValue(p.getName(), p.getValue(), ds);
        if (viewerLink != null)
          result.add(viewerLink);
      }
      return result;
    }

    private ViewerLink parseViewerPropertyValue(String viewerName, String viewerValue, InvDatasetImpl ds) {
      String viewerUrl;
      String viewerTitle;

      int lastCommaLocation = viewerValue.lastIndexOf(",");
      if (lastCommaLocation != -1) {
        viewerUrl = viewerValue.substring(0, lastCommaLocation);
        viewerTitle = viewerValue.substring(lastCommaLocation + 1);
        if (viewerUrl.equals(""))
          return null;
        if (viewerTitle.equals(""))
          viewerTitle = viewerName;
      } else {
        viewerUrl = viewerValue;
        viewerTitle = viewerName;
      }
      viewerUrl = StringUtil2.quoteHtmlContent(sub(viewerUrl, ds));

      return new ViewerLink(viewerTitle, viewerUrl);
    }

    private boolean hasViewerProperties(InvDatasetImpl ds) {
      for (InvProperty p : ds.getProperties())
        if (p.getName().startsWith(propertyNamePrefix))
          return true;

      return false;
    }

    private List<InvProperty> findViewerProperties(InvDatasetImpl ds) {
      List<InvProperty> result = new ArrayList<>();
      for (InvProperty p : ds.getProperties())
        if (p.getName().startsWith(propertyNamePrefix))
          result.add(p);

      return result;
    }

    private String sub(String org, InvDatasetImpl ds) {
      List<InvAccess> access = ds.getAccess();
      if (access.size() == 0)
        return org;

      // look through all access for {serviceName}
      for (InvAccess acc : access) {
        String sname = "{" + acc.getService().getServiceType() + "}";
        if (org.contains(sname)) {
          URI uri = acc.getStandardUri();
          if (uri != null)
            return StringUtil2.substitute(org, sname, uri.toString());
        }
      }

      String sname = "{url}";
      if ((org.contains(sname)) && (access.size() > 0)) {
        InvAccess acc = access.get(0); // just use the first one
        URI uri = acc.getStandardUri();
        if (uri != null)
          return StringUtil2.substitute(org, sname, uri.toString());
      }

      return org;
    }
  }

}
