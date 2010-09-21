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

import thredds.catalog.InvDatasetImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Interface for plugging in Viewers.
 * Generally, these are implemented with jnlp files in /content/thredds/view/views/*.jnlp
 * You can customizing by adding parameters to the jnlp file, eg parm=subst&name=value.
 * Then all instances of "{param}" will be replaced by subst, and
 * all instances of "{name}" will be replaced by value, etc.
 *
 * @see thredds.servlet.ViewServlet#registerViewer
 */
public interface ViewerLinkProvider extends Viewer
{

  /**
   * Get an HTML fragment link to the viewer JNLP file, for this dataset.
   * Example:
   *   return "<a href='" + req.getContextPath() + "/view/idv.jnlp?url="+dataURI.toString()+"'>Integrated Data Viewer (IDV) (webstart)</a>";
   *
   * @param ds the dataset to view
   * @param req the request
   * @return HTML fragment string
   */
   public List<ViewerLink> getViewerLinks( InvDatasetImpl ds, HttpServletRequest req);

  public class ViewerLink
  {
    private String title;
    private String url;

    public ViewerLink( String title, String url) {
      this.title = title;
      this.url = url;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
  }
}
