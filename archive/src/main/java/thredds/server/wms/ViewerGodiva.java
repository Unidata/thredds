/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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

package thredds.server.wms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.*;
import thredds.servlet.ServletUtil;
import thredds.servlet.ThreddsConfig;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A Viewer for viewing datasets using the built-in Godiva2 client.  The viewer
 * must be configured in {@code ${tomcat_home}/content/thredds/threddsConfig.xml}, as per
 * instructions <a href="http://www.unidata.ucar.edu/projects/THREDDS/tech/tds4.2/reference/Viewers.html">here</a>.
 * @author Jon
 */
public class ViewerGodiva implements thredds.servlet.Viewer {
      static private final Logger logger = LoggerFactory.getLogger(ViewerGodiva.class);
  
      /**
       * Returns true if this is a gridded dataset that is accessible via WMS.
       */
      @Override
      public boolean isViewable(Dataset ds)
      {
          Access access = ds.getAccess(ServiceType.WMS);
          return access != null && (ThreddsConfig.getBoolean("WMS.allow", false));
      }
  
      @Override
      public String getViewerLinkHtml(Dataset ds, HttpServletRequest req)
      {
        Access access = ds.getAccess(ServiceType.WMS);
        if (access == null) return null;
  
        URI dataURI = access.getStandardUri();
        if (dataURI == null) {
          logger.warn("Godiva2Viewer access URL failed on {}", ds.getName());
          return null;
        }
  
        try {
          URI base = new URI( req.getRequestURL().toString());
          dataURI = base.resolve( dataURI);
  
        } catch (URISyntaxException e) {
          logger.warn("Godiva2Viewer URL=" + req.getRequestURL().toString(), e);
          return null;
        }
  
        // ToDo Switch to use TdsContext.getContextPath()
        return "<a href='" + ServletUtil.getContextPath() + "/godiva2/godiva2.html?server="+dataURI.toString()+"'>Godiva2 (browser-based)</a>";
      }
}
