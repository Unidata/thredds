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
// $Id: TestAllCatGen.java,v 1.4 2006/01/20 20:42:06 caron Exp $
package thredds.cataloggen;

import junit.framework.*;

/**
 * A description
 *
 * @author edavis
 * @since Mar 30, 2005T4:26:37 PM
 */
public class TestAllCatGen extends TestCase
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TestAllCatGen.class );

  public TestAllCatGen( String name )
  {
    super( name );
  }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTestSuite( thredds.cataloggen.config.TestResultService.class );
    suite.addTestSuite( thredds.cataloggen.config.TestDatasetFilterType.class );
    suite.addTestSuite( thredds.cataloggen.config.TestDatasetFilter.class );
    suite.addTestSuite( thredds.cataloggen.config.TestDatasetNamerType.class );
    suite.addTestSuite( thredds.cataloggen.config.TestDatasetNamer.class );
    suite.addTestSuite( thredds.cataloggen.config.TestCatGenConfigMetadataFactory.class );
    suite.addTestSuite( thredds.cataloggen.config.TestCatalogRefExpander.class );
    suite.addTestSuite( thredds.cataloggen.config.TestDatasetSource.class );

    suite.addTestSuite( thredds.cataloggen.datasetenhancer.TestRegExpAndDurationTimeCoverageEnhancer.class );

    suite.addTestSuite( thredds.cataloggen.TestDatasetEnhancer1.class );
    suite.addTestSuite( thredds.cataloggen.TestDirectoryScanner.class );
    suite.addTestSuite( thredds.cataloggen.TestCollectionLevelScanner.class );
    suite.addTestSuite( thredds.cataloggen.TestSimpleCatalogBuilder.class );
    suite.addTestSuite( thredds.cataloggen.TestCatalogGen.class );

    return suite;
  }
}

/*
 * $Log: TestAllCatGen.java,v $
 * Revision 1.4  2006/01/20 20:42:06  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.3  2005/12/16 23:19:38  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.2  2005/08/16 21:47:51  edavis
 * Switch from Log4j to commons logging.
 *
 * Revision 1.1  2005/03/30 23:56:07  edavis
 * Fix tests.
 *
 */