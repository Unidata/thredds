/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.viewer;

import thredds.client.catalog.Dataset;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface for plugging in Viewers.
 * Generally, these are implemented with jnlp files in /content/thredds/view/views/*.jnlp
 * You can customize by adding parameters to the jnlp file, eg parm=subst&name=value.
 * Then all instances of "{param}" will be replaced by subst, and
 * all instances of "{name}" will be replaced by value, etc.
 *
 */
public interface Viewer {
  /**
   * Is this dataset vieweable by me?
   * @param ds the dataset
   * @return  true if viewable
   */
  boolean isViewable( Dataset ds);

  /**
   * Get an HTML fragment link to the viewer JNLP file, for this dataset.
   * Example:
   *   return "<a href='" + req.getContextPath() + "/view/idv.jnlp?url="+dataURI.toString()+"'>Integrated Data Viewer (IDV) (webstart)</a>";
   *
   * @param ds the dataset to view
   * @param req the request
   * @return HTML fragment string
   */
  String getViewerLinkHtml( Dataset ds, HttpServletRequest req);

  /**
   ** @param ds the dataset to view
   * @param req the request
   * @return HTML fragment string
   */
  ViewerLinkProvider.ViewerLink getViewerLink(Dataset ds, HttpServletRequest req);

}
