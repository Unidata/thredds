// $Id$
package thredds.crawlabledataset.filter;

import java.util.List;
import java.util.Iterator;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

/**
 * The standard CrawlableDatasetFilter which uses a list of
 * CrawlableDatasetSelectors to determine if a dataset is accepted. 
 *
 * @author edavis
 * @since Jul 25, 2005 11:15:43 -0600
 */
public class MultiSelectorFilter implements CrawlableDatasetFilter
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( MultiSelectorFilter.class );

  private List selectorGroup;

  public MultiSelectorFilter( Object selectorGroup )
  {
    if ( selectorGroup == null ) throw new IllegalArgumentException( "Selector group parameter must not be null." );
    if ( selectorGroup instanceof List )
      this.selectorGroup = (List) selectorGroup;
    else
      throw new IllegalArgumentException( "Config object not a list <" + selectorGroup.getClass().getName() + ">.");
  }

  public Object getConfigObject() { return selectorGroup; }

  public boolean accept( CrawlableDataset dataset )
  {
    if ( dataset == null ) throw new IllegalArgumentException( "Dataset parameter must not be null." );

    // If no selectors, accept all datasets.
    if ( selectorGroup.isEmpty() )
    {
      return ( true );
    }

    // Loop through selector group to check if current dataset should be accepted.
    boolean accept = false;
    boolean doAnyIncludersApply = false;
    boolean doAnySelectorsApply = false;
    for ( Iterator it = selectorGroup.iterator(); it.hasNext(); )
    {
      Selector curSelector = (Selector) it.next();

      if ( curSelector.isApplicable( dataset ) )
      {
        doAnySelectorsApply = true;
        if ( curSelector.isIncluder() )
        {
          doAnyIncludersApply = true;
          if ( curSelector.match( dataset ) )
          {
            accept = true; // Dataset accepted by current DatasetFilter.
          }
        }
        else // isExcluder()
        {
          if ( curSelector.match( dataset ) )
          {
            return ( false ); // Exclusion takes precedence over inclusion.
          }
        }
      }
    }

    // If at least one filter accepted (and none rejected), accept.
    if ( accept ) return ( true );

    // If no selectors apply to this dataset, accept it.
    if ( ! doAnySelectorsApply ) return ( true );

    // If no includers apply to this dataset (and no excluders
    // rejected it), accept. [Allows exclusion only.]
    if ( ! doAnyIncludersApply ) return ( true );

    // Dataset not accepted or rejected by any DatasetFilter (so reject).
    return ( false );
  }

}

/*
 * $Log: MultiSelectorFilter.java,v $
 * Revision 1.4  2006/03/01 23:08:35  edavis
 * Minor fix.
 *
 * Revision 1.3  2006/01/10 23:21:14  edavis
 * Document changes to datasetScan. Also, make a few simplifying and clarifying changes to code and XSD that came up while documenting.
 *
 * Revision 1.2  2005/12/30 00:18:53  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.1  2005/11/15 18:40:47  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.1  2005/08/31 17:10:55  edavis
 * Update DqcServletRedirect for release as dqcServlet.war. It forwards
 * /dqcServlet/*, /dqcServlet/dqc/*, and /dqcServlet/dqcServlet/* requests
 * to /thredds/dqc/*. It also provides some URLs for testing various HTTP
 *  redirections (301, 302, 305) and forwarding (i.e.,
 * javax.servlet.RequestDispatcher.forward()) at /dqcServlet/redirect-test/.
 *
 */