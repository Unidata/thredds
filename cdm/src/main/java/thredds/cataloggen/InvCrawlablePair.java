// $Id: InvCrawlablePair.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen;

import thredds.catalog.InvDataset;
import thredds.crawlabledataset.CrawlableDataset;

/**
 * Helper class to contain an InvDataset and its corresponding CrawlableDataset.
 *
 * Used by CollectionLevelScanner to provide access to:
 * 1) All generated collection dataset objects (InvCatalogRef) and their corresponding CrawlableDataset objects;
 * 2) All generated atomic dataset objects (InvDataset) and their corresponding CrawlableDataset objects.
 *
 */
public class InvCrawlablePair
{
  private CrawlableDataset crawlableDataset;
  private InvDataset invDataset;

  public InvCrawlablePair( CrawlableDataset crawlableDataset, InvDataset invDataset )
  {
    this.crawlableDataset = crawlableDataset;
    this.invDataset = invDataset;
  }

  public CrawlableDataset getCrawlableDataset()
  {
    return crawlableDataset;
  }

  public InvDataset getInvDataset()
  {
    return invDataset;
  }
}
