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
