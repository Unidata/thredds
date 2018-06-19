/*
 * Copyright (c) 2010 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package thredds.server.wms;

import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.Access;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.ServiceType;
import thredds.server.config.ThreddsConfig;
import thredds.server.viewer.Viewer;
import thredds.server.viewer.ViewerLinkProvider;

/**
 * A Viewer for viewing datasets using the built-in Godiva3 client.  The viewer
 * must be configured in {@code ${tomcat_home}/content/thredds/threddsConfig.xml}, as per
 * instructions <a href="http://www.unidata.ucar.edu/projects/THREDDS/tech/tds4.2/reference/Viewers.html">here</a>(needs updated). *
 * @author Jon
 */
public class Godiva3Viewer implements Viewer {
  static private final Logger logger = LoggerFactory.getLogger(Godiva3Viewer.class);

  static private final String title = "Godiva3 (browser-based)";

  /**
   * Returns true if this is a gridded dataset that is accessible via WMS.
   */
  @Override
  public boolean isViewable(Dataset ds) {
    Access access = ds.getAccess(ServiceType.WMS);
    return access != null && (ThreddsConfig.getBoolean("WMS.allow", false));
  }

  @Override
  public String getViewerLinkHtml(Dataset ds, HttpServletRequest req) {
    ViewerLinkProvider.ViewerLink viewerLink = getViewerLink(ds, req);
    return "<a href='" + viewerLink.getUrl() + "'>" + viewerLink.getTitle() + "</a>";
  }

  @Override
  public ViewerLinkProvider.ViewerLink getViewerLink(Dataset ds, HttpServletRequest req) {
    Access access = ds.getAccess(ServiceType.WMS);
    if (access == null) return null;

    URI dataURI = access.getStandardUri();
    if (dataURI == null) {
      logger.warn("Godiva3Viewer access URL failed on {}", ds.getName());
      return null;
    }

    try {
      URI base = new URI(req.getRequestURL().toString());
      dataURI = base.resolve(dataURI);

    } catch (URISyntaxException e) {
      logger.warn("Godiva3Viewer URL=" + req.getRequestURL().toString(), e);
      return null;
    }
    String url = req.getContextPath() + "/Godiva.html?server=" + dataURI.toString();
    return new ViewerLinkProvider.ViewerLink(Godiva3Viewer.title, url);
  }
}
