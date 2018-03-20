/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.viewer;

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

import com.coverity.security.Escape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import thredds.client.catalog.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.IO;
import ucar.unidata.util.StringUtil2;
import thredds.server.wms.Godiva3Viewer;

@Component
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
  public void showViewers(Formatter out, Dataset dataset, HttpServletRequest req) {

    int count = 0;
    for (Viewer viewer : viewers) {
      if (viewer.isViewable(dataset)) count++;
    }
    if (count == 0) return;

    out.format("<h3>Viewers:</h3><ul>\r\n");

    for (Viewer viewer : viewers) {
      if (viewer.isViewable(dataset)) {
        if (viewer instanceof ViewerLinkProvider) {
          List<ViewerLinkProvider.ViewerLink> sp = ((ViewerLinkProvider) viewer).getViewerLinks(dataset, req);
          for (ViewerLinkProvider.ViewerLink vl : sp) {
            if (vl.getUrl() != null && !vl.getUrl().equals(""))
              out.format("<li><a href='%s'>%s</a></li>\r\n", vl.getUrl(), vl.getTitle() != null ? vl.getTitle() : vl.getUrl());
          }

        } else {
          String viewerLinkHtml = viewer.getViewerLinkHtml(dataset, req);
          if (viewerLinkHtml != null) {
            out.format("  <li> %s</li>\r\n", viewerLinkHtml);
          }
        }
      }
    }
    out.format("</ul>\r\n");
  }

  @SuppressWarnings("unused")
  @PostConstruct
  private void registerViewers() {
    registerViewer(new Godiva3Viewer());
    registerViewer(new ToolsUI());
    registerViewer(new IDV());
    registerViewer(new StaticView());
  }

  // Viewers...
  // ToolsUI
  private static class ToolsUI implements Viewer {

    public boolean isViewable(Dataset ds) {
      String id = ds.getID();
      return ((id != null) && ds.hasAccess());
    }

    public String getViewerLinkHtml(Dataset ds, HttpServletRequest req) {
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

    public boolean isViewable(Dataset ds) {
      Access access = getOpendapAccess(ds);
      if (access == null)
        return false;

      FeatureType dt = ds.getFeatureType();
      return dt == FeatureType.GRID;
    }

    public String getViewerLinkHtml(Dataset ds, HttpServletRequest req) {
      Access access = getOpendapAccess(ds);
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

    private Access getOpendapAccess(Dataset ds) {
      Access access = ds.getAccess(ServiceType.DODS);
      if (access == null)
        access = ds.getAccess(ServiceType.OPENDAP);
      return access;
    }
  }

  // LOOK whats this for ??
  private static final String propertyNamePrefix = "viewer";
  private static class StaticView implements ViewerLinkProvider {

    public boolean isViewable(Dataset ds) {
      return hasViewerProperties(ds);
    }

    public String getViewerLinkHtml(Dataset ds, HttpServletRequest req) {
      List<ViewerLink> viewerLinks = getViewerLinks(ds, req);
      if (viewerLinks.isEmpty())
        return null;
      ViewerLink firstLink = viewerLinks.get(0);
      return "<a href='" + firstLink.getUrl() + "'>" + firstLink.getTitle() + "</a>";
    }

    @Override
    public List<ViewerLink> getViewerLinks(Dataset ds, HttpServletRequest req) {
      List<Property> viewerProperties = findViewerProperties(ds);
      if (viewerProperties.isEmpty())
        return Collections.emptyList();
      List<ViewerLink> result = new ArrayList<>();
      for (Property p : viewerProperties) {
        ViewerLink viewerLink = parseViewerPropertyValue(p.getName(), p.getValue(), ds);
        if (viewerLink != null)
          result.add(viewerLink);
      }
      return result;
    }

    private ViewerLink parseViewerPropertyValue(String viewerName, String viewerValue, Dataset ds) {
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
      viewerUrl = sub(viewerUrl, ds);

      return new ViewerLink(viewerTitle, viewerUrl);
    }

    private boolean hasViewerProperties(Dataset ds) {
      for (Property p : ds.getProperties())
        if (p.getName().startsWith(propertyNamePrefix))
          return true;

      return false;
    }

    private List<Property> findViewerProperties(Dataset ds) {
      List<Property> result = new ArrayList<>();
      for (Property p : ds.getProperties())
        if (p.getName().startsWith(propertyNamePrefix))
          result.add(p);

      return result;
    }

    private String sub(String org, Dataset ds) {
      List<Access> access = ds.getAccess();
      if (access.size() == 0)
        return org;

      // look through all access for {serviceName}
      for (Access acc : access) {
        String sname = "{" + acc.getService().getServiceTypeName() + "}";
        if (org.contains(sname)) {
          URI uri = acc.getStandardUri();
          if (uri != null)
            return StringUtil2.substitute(org, sname, uri.toString());
        }
      }

      String sname = "{url}";
      if ((org.contains(sname)) && (access.size() > 0)) {
        Access acc = access.get(0); // just use the first one
        URI uri = acc.getStandardUri();
        if (uri != null)
          return StringUtil2.substitute(org, sname, uri.toString());
      }

      return org;
    }
  }

}
