package thredds.servlet;

import thredds.catalog.InvDatasetImpl;

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
   * @return HTML fragment string
   */
   public String getViewerLinkHtml( InvDatasetImpl ds);

}
