// $Id: BooleanCatalogRefExpander.java,v 1.1 2005/12/16 23:19:34 edavis Exp $
package thredds.cataloggen.catalogrefexpander;

import thredds.cataloggen.CatalogRefExpander;
import thredds.cataloggen.InvCrawlablePair;

/**
 * _more_
 *
 * @author edavis
 * @since Dec 6, 2005 3:42:55 PM
 */
public class BooleanCatalogRefExpander implements CatalogRefExpander
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( BooleanCatalogRefExpander.class );

  private boolean expandAll = false;

  public BooleanCatalogRefExpander( boolean expandAll )
  {
    this.expandAll = expandAll;
  }

  public boolean expandCatalogRef( InvCrawlablePair catRefInfo )
  {
    return expandAll;  
  }
}
/*
 * $Log: BooleanCatalogRefExpander.java,v $
 * Revision 1.1  2005/12/16 23:19:34  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 */