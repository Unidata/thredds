// $Id$
package thredds.cataloggen;

/**
 * _more_
 *
 * @author edavis
 * @since Dec 6, 2005 2:48:11 PM
 */
public interface CatalogRefExpander
{
  public boolean expandCatalogRef( InvCrawlablePair catRefInfo );
}
/*
 * $Log: CatalogRefExpander.java,v $
 * Revision 1.1  2005/12/16 23:19:35  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 */