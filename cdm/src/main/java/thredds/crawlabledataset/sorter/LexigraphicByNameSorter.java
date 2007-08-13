// $Id: LexigraphicByNameSorter.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset.sorter;

import thredds.crawlabledataset.CrawlableDatasetSorter;
import thredds.crawlabledataset.CrawlableDataset;

import java.util.List;
import java.util.Collections;
import java.util.Comparator;

/**
 * Sort CrawlableDataset lists lexigraphically on the dataset name.
 *
 * @author edavis
 * @since Nov 18, 2005 4:26:38 PM
 */
public class LexigraphicByNameSorter implements CrawlableDatasetSorter
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( LexigraphicByNameSorter.class );

  private boolean increasingOrder = false;
  private Comparator<CrawlableDataset> comparator;

  public LexigraphicByNameSorter( boolean increasingOrder )
  {
    this.increasingOrder = increasingOrder;
    this.comparator = new java.util.Comparator<CrawlableDataset>()
    {
      public int compare( CrawlableDataset crDs1, CrawlableDataset crDs2 )
      {
        int compareVal = crDs1.getName().compareTo( crDs2.getName() );

        return ( LexigraphicByNameSorter.this.increasingOrder ? compareVal : -compareVal );
      }
    };
  }

  public Object getConfigObject() { return null; }
  
  public boolean isIncreasing() { return increasingOrder; }

  public void sort( List<CrawlableDataset> datasetList )
  {
    Collections.sort( datasetList, this.comparator );
  }
}
