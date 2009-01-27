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
// $Id: TestLatestDqcHandler.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server.latest;

import junit.framework.*;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import thredds.dqc.server.DqcHandler;
import thredds.dqc.server.DqcServletConfigItem;
import thredds.dqc.server.DqcHandlerInstantiationException;

/**
 * _more_
 *
 * @author edavis
 * @since Oct 3, 2005 8:23:06 AM
 */
public class TestLatestDqcHandler extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( TestLatestDqcHandler.class );

  private String configPath = "test/data/thredds/dqc/server/latest";

  private String testLatestConfig_exampleConfig_filename = "testLatestConfig.exampleConfig.xml";
  private String testLatestConfig_exampleConfigOld_filename = "testLatestConfig.exampleConfigOld.xml";

  public TestLatestDqcHandler( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  public void testDatasetDateStringFullMatch()
  {
    LatestDqcHandler me = new LatestDqcHandler();
    assertTrue( me != null );


    String fileName = "GFS_CONUS_80km_20050930_1200.nc";
    String dsNameMatchPattern = ".*([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).nc";
    Date fileNameDate = new Date( 1128081600000L );
    LatestConfig.Item item = new LatestConfig.Item( "gfs_211", "GFS CONUS 80km", "",
                                                    dsNameMatchPattern, "$1-$2-$3T$4:$5:00",
                                                    "/thredds/dodsC/model", "1.0", "0.3" );
    Date dsDate = me.getDatasetDate( new File( fileName ), item );

    assertTrue( "The date determined from the dataset name <" + dsDate.getTime() + " - " + dsDate.toString() +
                "> not as expected <" + fileNameDate.getTime() + " - " + fileNameDate.toString() + ">.",
                dsDate.equals( fileNameDate ));

  }

  public void testDatasetDateStringEndMatch()
  {
    LatestDqcHandler me = new LatestDqcHandler();
    assertTrue( me != null );


    String fileName = "GFS_CONUS_80km_20050930_1200.nc";
    String dsNameMatchPattern = "([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).nc";
    Date fileNameDate = new Date( 1128081600000L );
    LatestConfig.Item item = new LatestConfig.Item( "gfs_211", "GFS CONUS 80km", "",
                                                    dsNameMatchPattern, "$1-$2-$3T$4:$5:00",
                                                    "/thredds/dodsC/model", "1.0", "0.3" );
    Date dsDate = me.getDatasetDate( new File( fileName ), item );

    assertTrue( "The date determined from the dataset name <" + dsDate.getTime() + " - " + dsDate.toString() +
                "> not as expected <" + fileNameDate.getTime() + " - " + fileNameDate.toString() + ">.",
                dsDate.equals( fileNameDate ));

  }

  public void testDatasetDateStringMiddleMatch()
  {
    LatestDqcHandler me = new LatestDqcHandler();
    assertTrue( me != null );


    String fileName = "GFS_CONUS_80km_20050930_1200.nc";
    String dsNameMatchPattern = "([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2})";
    Date fileNameDate = new Date( 1128081600000L );
    LatestConfig.Item item = new LatestConfig.Item( "gfs_211", "GFS CONUS 80km", "",
                                                    dsNameMatchPattern, "$1-$2-$3T$4:$5:00",
                                                    "/thredds/dodsC/model", "1.0", "0.3" );
    Date dsDate = me.getDatasetDate( new File( fileName ), item );

    assertTrue( "The date determined from the dataset name <" + dsDate.getTime() + " - " + dsDate.toString() +
                "> not as expected <" + fileNameDate.getTime() + " - " + fileNameDate.toString() + ">.",
                dsDate.equals( fileNameDate ));

  }

  /**
   * Test ...
   */
  public void testReadConfig()
  {
    LatestDqcHandler me;
    try
    {
      me = (LatestDqcHandler) DqcHandler.factory(
              new DqcServletConfigItem( "latest", "the latest",
                                        "thredds.dqc.server.latest.LatestDqcHandler",
                                        testLatestConfig_exampleConfig_filename ),
              configPath );
    }
    catch ( DqcHandlerInstantiationException e )
    {
      assertTrue( "Couldn't instantiate DqcHandler: " + e.getMessage(),
                  false );
      return;
    }
    catch ( IOException e )
    {
      assertTrue( "Trouble reading config document: " + e.getMessage(),
                  false );
      return;
    }

    assertTrue( "LatestDqcHandler <" + testLatestConfig_exampleConfig_filename + "> is null.",
                me != null );
  }

  public void testReadOldConfig()
  {
    LatestDqcHandler me;
    try
    {
      me = (LatestDqcHandler) DqcHandler.factory(
              new DqcServletConfigItem( "latest", "the latest",
                                        "thredds.dqc.server.latest.LatestDqcHandler",
                                        testLatestConfig_exampleConfigOld_filename ),
              configPath );
    }
    catch ( DqcHandlerInstantiationException e )
    {
      assertTrue( "Couldn't instantiate DqcHandler: " + e.getMessage(),
                  false );
      return;
    }
    catch ( IOException e )
    {
      assertTrue( "Trouble reading config document: " + e.getMessage(),
                  false );
      return;
    }

    assertTrue( "LatestDqcHandler <" + testLatestConfig_exampleConfigOld_filename + "> is null.",
                me != null );
  }

}
/*
 * $Log: TestLatestDqcHandler.java,v $
 * Revision 1.2  2006/01/23 18:51:07  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.1  2005/10/03 22:35:41  edavis
 * Minor fixes for LatestDqcHandler.
 *
 */