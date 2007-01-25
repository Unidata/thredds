// $Id: WildcardMatchOnNameFilter.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDataset;

/**
 * CrawlableDatasetFilter implementation that accepts datasets whose
 * names are matched by the given wildcard string. The wildcard string
 * can contain astrisks ("*") which match 0 or more characters and
 * question marks ("?") which match 0 or 1 character.
 *
 * @author edavis
 * @since Nov 5, 2005 12:51:56 PM
 */
public class WildcardMatchOnNameFilter implements CrawlableDatasetFilter
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( WildcardMatchOnNameFilter.class );

  protected String wildcardString;
  protected java.util.regex.Pattern pattern;

  public WildcardMatchOnNameFilter( String wildcardString )
  {
    // Replace "." with "\.".
    this.wildcardString = wildcardString.replaceAll( "\\.", "\\\\." );

    // Replace "*" with ".*".
    this.wildcardString = this.wildcardString.replaceAll( "\\*", ".*");

    // Replace "?" with ".?".
    this.wildcardString = this.wildcardString.replaceAll( "\\?", ".?");

    this.pattern = java.util.regex.Pattern.compile( this.wildcardString );
  }

  public Object getConfigObject() { return wildcardString; }
  public String getWildcardString() { return wildcardString; }

  public boolean accept( CrawlableDataset dataset )
  {
    java.util.regex.Matcher matcher = this.pattern.matcher( dataset.getName() );
    if ( matcher.matches() ) return true;
    return false;
  }
}
