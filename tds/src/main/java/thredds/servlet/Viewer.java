/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.servlet;

import thredds.catalog.InvDatasetImpl;

import javax.servlet.http.HttpServletRequest;

public interface Viewer {

  /**
   * Interface for plugging in Viewers.
   * Generally, these are implemented with jnlp files in /content/thredds/view/views/*.jnlp
   * You can customizing by adding parameters to the jnlp file, eg parm=subst&name=value.
   * Then all instances of "{param}" will be replaced by subst, and
   *  all instances of "{name}" will be replaced by value, etc.
   *
   * @see ViewServlet#registerViewer
   */

  /**
   * Is this dataset vieweable by me?
   * @param ds the dataset
   * @return  true if viewable
   */
   public boolean isViewable( InvDatasetImpl ds);

  /**
   * Get an HTML fragment link to the viewer JNLP file, for this dataset.
   * Example: "<a href='idv.jnlp?url="+access.getStandardUrlName()+"'>Integrated Data Viewer (IDV) (webstart)</a>"
   * 
   * @param ds the dataset to view
   * @param req the request
   * @return HTML fragment string
   */
   public String getViewerLinkHtml( InvDatasetImpl ds, HttpServletRequest req);

}
