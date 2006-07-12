// $Id$
package thredds.cataloggen.config;

import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDatasetScan;

/**
 * A description
 *
 * @author edavis
 * @since Apr 20, 2005 17:02:08 PM
 */
public class DatasetSorter
{
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetSorter.class);

  // Location(s) in catalog heirarchy wher this sorter should be applied.
  // E.g., "NCEP models/Eta 211", "models/eta_211", "*", "models/*/A" -- must be dataset names
  private String targetLevel;
  private boolean increasingOrder = true; // Default order is increasing.
  private String type = "lexigraphic"; // only lex for now
  private String attributeName;

  private java.util.Comparator invDatasetComparator;

  /**
   * Sort datasets into lexigraphic order by dataset name in either increasing
   * (increasingOrder=true) or decreasing (increasingOrder=false) order.
   *
   * @param increasingOrder if true, datasets are sorted into increasing order, otherwise decreasing order.
   */
  public DatasetSorter( boolean increasingOrder )
  {
    this.increasingOrder = increasingOrder;
    this.invDatasetComparator = new java.util.Comparator()
    {
      public int compare( Object obj1, Object obj2 )
      {
        InvDataset ds1 = (InvDataset) obj1;
        InvDataset ds2 = (InvDataset) obj2;

        int compareVal = ds1.getName().compareTo( ds2.getName() );

        return ( DatasetSorter.this.increasingOrder ? compareVal : -compareVal );
      }
    };
  }

  /**
   * Sort datasets according to the given java.util.Comparator.
   *
   * @param invDatasetComparator a java.util.Comparator that compares InvDataset objects.
   */
  public DatasetSorter( java.util.Comparator invDatasetComparator)
  {
    this.invDatasetComparator = invDatasetComparator;
  }

  public void sortDatasets( InvDataset collectionDs )
  {
    this.sortDatasets( collectionDs.getDatasets() );
  }

  public void sortDatasets( java.util.List datasets)
  {
    java.util.Collections.sort( datasets, invDatasetComparator );
  }

  public void sortNestedDatasets( InvDataset collectionDs )
  {
    // Sort grand children datasets.
    InvDataset curDs = null;
    for ( java.util.Iterator dsIter = collectionDs.getDatasets().iterator(); dsIter.hasNext(); )
    {
      curDs = (InvDataset) dsIter.next();
      // Do not dereference catalogRef or datasetScan.
      if ( ! curDs.getClass().equals( InvCatalogRef.class )
           && ! curDs.getClass().equals( InvDatasetScan.class))
      // @todo Move this test into abstract InvDataset.remoteDataset()?? 
      {
        this.sortDatasets( curDs);
      }
    }

    // Sort child datasets.
    logger.debug( "sortDatasets(): sort the datasets contained by dataset ({})", collectionDs.getName() );
    this.sortDatasets( collectionDs.getDatasets());

    return;
  }

}

/*
 * $Log: DatasetSorter.java,v $
 * Revision 1.4  2006/01/20 02:08:23  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.3  2005/12/06 19:39:19  edavis
 * Last CatalogBuilder/CrawlableDataset changes before start using in InvDatasetScan.
 *
 * Revision 1.2  2005/06/24 22:00:56  edavis
 * Write DatasetEnhancer1 to allow adding metadata to datasets.
 * Implement DatasetEnhancers for adding timeCoverage and for
 * adding ID to datasets. Also fix DatasetFilter so that 1) if
 * no filter is applicable for collection datasets, allow all
 * collection datasets and 2) if no filter is applicable for
 * atomic datasets, allow all atomic datasets.
 *
 * Revision 1.1  2005/04/27 23:05:40  edavis
 * Move sorting capabilities into new DatasetSorter class.
 * Fix a bunch of tests and such.
 *
 */