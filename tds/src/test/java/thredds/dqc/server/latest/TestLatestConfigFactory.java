/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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