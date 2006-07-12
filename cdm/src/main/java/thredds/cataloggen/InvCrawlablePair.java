// $Id$
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

/*
 * $Log: InvCrawlablePair.java,v $
 * Revision 1.2  2006/05/01 14:29:23  edavis
 * Add access to all Inv/Crawlable pairs.
 *
 * Revision 1.1  2005/12/16 23:19:36  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.2  2005/11/15 18:40:45  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.1  2005/08/22 17:40:23  edavis
 * Another round on CrawlableDataset: make CrawlableDatasetAlias a subclass
 * of CrawlableDataset; start generating catalogs (still not using in
 * InvDatasetScan or CatalogGen, yet).
 *
 */