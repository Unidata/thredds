// $Id: TestRegExpMatchOnNameFilter.java 51 2006-07-12 17:13:13Z caron $
package thredds.crawlabledataset.filter;

import junit.framework.*;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFactory;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 5, 2005 1:26:20 PM
 */
public class TestRegExpMatchOnNameFilter extends TestCase
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( TestWildcardMatchOnNameFilter.class );

  public TestRegExpMatchOnNameFilter( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testRegExpMatch()
  {
    checkRegExpMatch( "build\\.xml", "build.xml" );
    checkRegExpMatch( "build\\.xm.?", "build.xml" );
    checkRegExpMatch( "build\\..*", "build.xml" );

    checkRegExpMatch( "visad\\.jar", "lib/visad.jar" );
    checkRegExpMatch( "visad\\.j.?r", "lib/visad.jar" );
    checkRegExpMatch( ".*ad\\.jar", "lib/visad.jar" );
  }

  private void checkRegExpMatch( String regExpString, String dsPath )
  {
    RegExpMatchOnNameFilter me = new RegExpMatchOnNameFilter( regExpString );

    CrawlableDataset ds = null;
    try
    {
      ds = CrawlableDatasetFactory.createCrawlableDataset( dsPath, null, null );
    }
    catch ( Exception e )
    {
      assertTrue( "Failed to create CrawlableDataset <" + dsPath + ">: " + e.getMessage(),
                  false );
    }
    assertTrue( "Failed to construct RegExpMatchOnNameFilter <" + regExpString + ">.",
                me != null );

    assertTrue( "RegExp string <" + regExpString + "> did not match dataset <" + ds.getName() + ">.",
                me.accept( ds) );
  }
}
