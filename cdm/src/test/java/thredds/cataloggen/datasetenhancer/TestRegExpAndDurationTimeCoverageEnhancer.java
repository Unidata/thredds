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
package thredds.cataloggen.datasetenhancer;

import junit.framework.*;
import thredds.catalog.InvDatasetImpl;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.mock.MockCrawlableDataset;

/**
 * _more_
 *
 * @author edavis
 * @since Mar 27, 2006 1:21:49 PM
 */
public class TestRegExpAndDurationTimeCoverageEnhancer extends TestCase
{
//  static private org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( TestRegExpAndDurationTimeCoverageEnhancer.class );


  public TestRegExpAndDurationTimeCoverageEnhancer( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testDatasetNameMatchSuccess()
  {
    String matchPattern = "NDFD_CONUS_5km_([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).grib2";
    String substitutionPattern = "$1-$2-$3T$4:$5:00";
    String duration = "96 hours";

    String dsName = "NDFD_CONUS_5km_20060325_1200.grib2";

    RegExpAndDurationTimeCoverageEnhancer timeCoverageEnhancer =
            RegExpAndDurationTimeCoverageEnhancer
                    .getInstanceToMatchOnDatasetName(
                            matchPattern, substitutionPattern, duration );
    assertTrue( timeCoverageEnhancer != null );

    InvDatasetImpl ds = new InvDatasetImpl( null, dsName );
    CrawlableDataset crDs = new MockCrawlableDataset( dsName, false );

    assertTrue( "Failed to add metadata.",
                timeCoverageEnhancer.addMetadata( ds, crDs ));
  }

  public void testDatasetNameMatchFail()
  {
    String matchPattern = "NDFD_CONUS_5km_([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).grib2";
    String substitutionPattern = "$1-$2-$3T$4:$5:00";
    String duration = "96 hours";

    String dsName = "NDFD_CONUS_5km_200600325_1200.grib2";

    RegExpAndDurationTimeCoverageEnhancer timeCoverageEnhancer =
            RegExpAndDurationTimeCoverageEnhancer
                    .getInstanceToMatchOnDatasetName(
                            matchPattern, substitutionPattern, duration );
    assertTrue( timeCoverageEnhancer != null );

    InvDatasetImpl ds = new InvDatasetImpl( null, dsName );
    CrawlableDataset crDs = new MockCrawlableDataset( dsName, false );

    assertTrue( "Unexpected success adding metadata.",
                ! timeCoverageEnhancer.addMetadata( ds, crDs ));
  }

  public void testDatasetPathMatchSuccess()
  {
    String matchPattern = "prod/sref.([0-9]{4})([0-9]{2})([0-9]{2})/([0-9]{2})/pgrb_biasc/sref_([^.]*)\\.t\\4z\\.pgrb([0-9]{3})\\.(.*)\\.grib2$";
    String substitutionPattern = "$1-$2-$3T$4";
//    String substitutionPattern = "$1-$2-$3T$4 Grid $6 member $5-$7";
    String duration = "96 hours";

    String dsName = "/data/nccf/com/sref/prod/sref.20090603/03/pgrb_biasc/sref_eta.t03z.pgrb212.n2.grib2";

    RegExpAndDurationTimeCoverageEnhancer timeCoverageEnhancer =
            RegExpAndDurationTimeCoverageEnhancer
                    .getInstanceToMatchOnDatasetPath(
                            matchPattern, substitutionPattern, duration );
    assertTrue( timeCoverageEnhancer != null );

    InvDatasetImpl ds = new InvDatasetImpl( null, dsName );
    CrawlableDataset crDs = new MockCrawlableDataset( dsName, false );

    assertTrue( "Failed to add metadata.",
                timeCoverageEnhancer.addMetadata( ds, crDs ) );

    // This dataset hasn't been finalized so ds.getTimeCoverage() doesn't work.
    String startDateString = ds.getLocalMetadata().getTimeCoverage().getStart().getText();
    String expectedStartDateString = "2009-06-03T03";
    assertTrue( "TimeCoverage start date [" + startDateString + "] not as expected [" + expectedStartDateString + "].",
                startDateString.equals( expectedStartDateString ));
  }


}