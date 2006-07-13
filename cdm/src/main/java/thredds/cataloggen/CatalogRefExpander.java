// $Id: CatalogRefExpander.java 63 2006-07-12 21:50:51Z edavis $
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
