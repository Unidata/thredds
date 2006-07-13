// $Id: TestLatestConfigFactory.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server.latest;

import junit.framework.*;

import java.io.InputStream;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since Sep 21, 2005 1:54:53 PM
 */
public class TestLatestConfigFactory extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( TestLatestConfigFactory.class );

  //private LatestConfigFactory me;

  private String resourcePath = "/thredds/dqc/server/latest";

  private String testLatestConfig_exampleConfig_resource = "testLatestConfig.exampleConfig.xml";
  private String testLatestConfig_exampleConfigOld_resource = "testLatestConfig.exampleConfigOld.xml";

  public TestLatestConfigFactory( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testParseConfig()
  {
    String resource = resourcePath + "/" + testLatestConfig_exampleConfig_resource;

    String resourceID = "resource:" + resource;

    InputStream resourceInStream = this.getClass().getResourceAsStream( resource );
    LatestConfig config;
    try
    {
      config = LatestConfigFactory.parseXML( resourceInStream, resourceID );
    }
    catch ( IOException e )
    {
      String tmpMsg = "IOException reading Latest config document from resource <" + resourceID + ">: " + e.getMessage();
      log.error( tmpMsg );
      assertTrue( tmpMsg, false );
      return;
    }

    assertTrue( "No items in config.",
                ! config.isEmpty() );

    System.out.println( config.toString() );
  }

  public void testParseOldConfig()
  {
    String resource = resourcePath + "/" + testLatestConfig_exampleConfigOld_resource;

    String resourceID = "resource:" + resource;

    InputStream resourceInStream = this.getClass().getResourceAsStream( resource );
    LatestConfig config;
    try
    {
      config = LatestConfigFactory.parseXML( resourceInStream, resourceID );
    }
    catch ( IOException e )
    {
      String tmpMsg = "IOException reading Latest config document from resource <" + resourceID + ">: " + e.getMessage();
      log.error( tmpMsg );
      assertTrue( tmpMsg, false );
      return;
    }

    assertTrue( "No items in config.",
                ! config.isEmpty() );

    System.out.println( config.toString() );
  }

  public void testWriteConfig()
  {
    assertTrue( "Haven't implemented LatestConfigFactory.writeXML() yet.",
                false);
  }
}
/*
 * $Log: TestLatestConfigFactory.java,v $
 * Revision 1.2  2006/01/23 18:51:07  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.1  2005/09/30 21:51:38  edavis
 * Improve "Latest" DqcHandler so it can deal with new IDD naming conventions:
 * new configuration file format; add LatestDqcHandler which handles new and old
 * config file formats; use LatestDqcHandler as a proxy for LatestModel.
 *
 */