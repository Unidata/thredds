/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.viewer;

import java.io.File;
import java.io.FileFilter;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.client.catalog.*;
import thredds.core.AllowedServices;
import thredds.core.StandardService;
import thredds.server.config.TdsContext;
import thredds.server.notebook.JupyterNotebookServiceCache;
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

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private JupyterNotebookServiceCache  jupyterNotebooks;

  @Autowired
  private AllowedServices allowedServices;

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

  @Override
  public List<ViewerLinkProvider.ViewerLink> getViewerLinks(Dataset dataset, HttpServletRequest req) {
    List<ViewerLinkProvider.ViewerLink> viewerLinks = new ArrayList<>();

    for (Viewer viewer : viewers) {
      if (viewer.isViewable(dataset)) {
        if (viewer instanceof ViewerLinkProvider) {
          viewerLinks.addAll(((ViewerLinkProvider) viewer).getViewerLinks(dataset, req));
        } else {
          viewerLinks.add(viewer.getViewerLink(dataset, req));
        }
      }
    }
    return viewerLinks;
  }

  @SuppressWarnings("unused")
  @PostConstruct
  private void registerViewers() {
    registerViewer(new Godiva3Viewer());
    registerViewer(new StaticView());
    registerViewer(new JupyterNotebookViewer(jupyterNotebooks, allowedServices, tdsContext.getContentRootPathProperty()));
  }

  // Viewers...

  private static class JupyterNotebookViewer implements Viewer {
    private static final String title = "Jupyter Notebook viewer";

    private JupyterNotebookServiceCache jupyterNotebooks;

    private AllowedServices allowedServices;

    private String contentDir;

    public JupyterNotebookViewer (JupyterNotebookServiceCache jupyterNotebooks, AllowedServices allowedServices, String contentDir) {
      this.jupyterNotebooks = jupyterNotebooks;
      this.allowedServices = allowedServices;
      this.contentDir = contentDir;
    }

    public boolean isViewable(Dataset ds) {
      return this.allowedServices.isAllowed(StandardService.jupyterNotebook)
        && jupyterNotebooks.getNotebookFilename(ds) != null;
    }

    public String getViewerLinkHtml( Dataset ds, HttpServletRequest req) {
      ViewerLinkProvider.ViewerLink viewerLink = this.getViewerLink(ds, req);
      return "<a href='" + viewerLink.getUrl() +  "'>" + viewerLink.getTitle() + "</a>";
    }

    public ViewerLinkProvider.ViewerLink getViewerLink(Dataset ds, HttpServletRequest req) {
      String catUrl = ds.getCatalogUrl();
      if (catUrl.indexOf('#') > 0)
        catUrl = catUrl.substring(0, catUrl.lastIndexOf('#'));
      if (catUrl.indexOf(contentDir) > -1) {
        catUrl = catUrl.substring(catUrl.indexOf(contentDir) + contentDir.length());
      }
      String catalogServiceBase = StandardService.catalogRemote.getBase();
      catUrl = catUrl.substring(catUrl.indexOf(catalogServiceBase) + catalogServiceBase.length()).replace("html", "xml");

      String url = req.getContextPath() + StandardService.jupyterNotebook.getBase() + ds.getID() + "?catalog=" + catUrl;
      return new ViewerLinkProvider.ViewerLink(JupyterNotebookViewer.title, url);
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
    public ViewerLink getViewerLink(Dataset ds, HttpServletRequest req) {
      List<ViewerLink> viewerLinks = getViewerLinks(ds, req);
      if (viewerLinks.isEmpty())
        return null;
      return viewerLinks.get(0);
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
