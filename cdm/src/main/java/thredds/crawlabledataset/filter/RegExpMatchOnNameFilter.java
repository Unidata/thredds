// $Id$
package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDataset;

/**
 * CrawlableDatasetFilter implementation that accepts datasets whose
 * names are matched by the given regular expression.
 *
 * @author edavis
 * @since Nov 5, 2005 12:51:56 PM
 */
public class RegExpMatchOnNameFilter implements CrawlableDatasetFilter
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( RegExpMatchOnNameFilter.class );

  private String regExp;
  private java.util.regex.Pattern pattern;

  public RegExpMatchOnNameFilter( String regExp )
  {
    this.regExp = regExp;
    this.pattern = java.util.regex.Pattern.compile( regExp );
  }

  public Object getConfigObject() { return regExp; }

  public boolean accept( CrawlableDataset dataset )
  {
    java.util.regex.Matcher matcher = this.pattern.matcher( dataset.getName() );
    if ( matcher.matches() ) return true;
    return false;
  }
}
