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
package thredds.crawlabledataset;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

import thredds.crawlabledataset.filter.*;
import ucar.unidata.test.util.TestDir;
import ucar.unidata.test.util.TestFileDirUtils;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 4, 2005 10:09:07 PM
 */
public class TestCrawlableDatasetFilter
{
  private static File tmpTestDataDir;
  private static CrawlableDataset tmpTestDataCrDs;
  private static List<String> dataFiles_FullPathNames;
  private static List<String> allFiles_FullPathNames;

  @BeforeClass
  public static void setupTestDataDir() {
    File tmpLocalRootDataDir = new File( TestDir.temporaryLocalDataDir);
    assertTrue( tmpLocalRootDataDir.exists());
    assertTrue( tmpLocalRootDataDir.canRead());
    assertTrue( tmpLocalRootDataDir.canWrite() );
    tmpTestDataDir = TestFileDirUtils.createTempDirectory( "TestCrawlableDatasetFilter", tmpLocalRootDataDir );
    assertNotNull(tmpTestDataDir);
    assertTrue( tmpTestDataDir.exists());
    assertTrue( tmpTestDataDir.canRead());
    assertTrue( tmpTestDataDir.canWrite());

  }

  public void createEtaDirWithCvsAndDotGitDirs( File targetDir) {

    tmpTestDataCrDs = createCrawlableDataset( targetDir.getPath(), targetDir.getName());

    List<String> dirNamesToIgnore = new ArrayList<String>();
    dirNamesToIgnore.add("CVS");
    dirNamesToIgnore.add(".git");

    List<String> dataFileNames = new ArrayList<String>();
    dataFileNames.add("2004050300_eta_211.nc");
    dataFileNames.add("2004050312_eta_211.nc");
    dataFileNames.add("2004050400_eta_211.nc");
    dataFileNames.add("2004050412_eta_211.nc");

    for ( String dirName : dirNamesToIgnore )
      TestFileDirUtils.addDirectory( targetDir, dirName);

    for ( String fileName : dataFileNames )
      TestFileDirUtils.addFile( targetDir, fileName );

    allFiles_FullPathNames = new ArrayList<String>();
    dataFiles_FullPathNames = new ArrayList<String>();

    for ( String fileName : dirNamesToIgnore )
      allFiles_FullPathNames.add( String.format( "%s/%s", tmpTestDataCrDs.getPath(), fileName));

    for ( String fileName : dataFileNames ) {
      String path = String.format("%s/%s", tmpTestDataCrDs.getPath(), fileName);
      allFiles_FullPathNames.add( path);
      dataFiles_FullPathNames.add( path);
    }

  }

  @Test
  public void testDatafilesInDateTimeNestedDirs() {
    File tmpTestDir = TestFileDirUtils.addDirectory( tmpTestDataDir, "testDatafilesInDateTimeNestedDirs" );
    assertNotNull(tmpTestDir);
    assertTrue( tmpTestDir.exists());
    assertTrue( tmpTestDir.canRead());
    assertTrue( tmpTestDir.canWrite());

    File profilesDir = TestFileDirUtils.addDirectory( tmpTestDir, "profiles");
    File firstDayDir = TestFileDirUtils.addDirectory( profilesDir, "20131106");

    TestFileDirUtils.addFile( firstDayDir, "PROFILER_wind_06min_20131106_2341.nc");
    TestFileDirUtils.addFile( firstDayDir, "PROFILER_wind_06min_20131106_2348.nc");
    TestFileDirUtils.addFile( firstDayDir, "PROFILER_wind_06min_20131106_2354.nc");

    File secondDayDir = TestFileDirUtils.addDirectory( profilesDir, "20131107");
    TestFileDirUtils.addFile( secondDayDir, "PROFILER_wind_06min_20131107_0001.nc");
    TestFileDirUtils.addFile( secondDayDir, "PROFILER_wind_06min_20131107_0008.nc");
    TestFileDirUtils.addFile( secondDayDir, "PROFILER_wind_06min_20131107_0014.nc");

    // Construct filter
    List<MultiSelectorFilter.Selector> selectors = new ArrayList<MultiSelectorFilter.Selector>();
    selectors.add( new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( ".*"), true, true, false ) );
    CrawlableDatasetFilter me = new MultiSelectorFilter( selectors );
    assertTrue( me != null );

    CrawlableDataset profilesCrDs = createCrawlableDataset( profilesDir.getPath(), profilesDir.getName());

    // Get filtered list of datasets.
    List<CrawlableDataset> list = null;
    try {
      list = profilesCrDs.listDatasets( me);
    } catch ( IOException e ) {
      fail(String.format( "IOException listing children of the CrDs [%s]: %s", profilesCrDs.getName(), e.getMessage()));
      return;
    }

    int expectedNumDs = 2;
    assertEquals( "Number of datasets", expectedNumDs, list.size());

    List<CrawlableDataset> subChildren = null;
    String atomicChildrenMatchPattern = "PROFILER_wind_06min_2013110[67]_[0-9]{4}\\.nc";

    for ( CrawlableDataset curCrDs : list ) {
      String crDsName = curCrDs.getName();
      assertTrue( String.format( "CrDs name [%s] not as expected [%s or %s].", crDsName, "20131106", "20131107"),
                  crDsName.equals( "20131106" ) || crDsName.equals( "20131107" ));
      try {
        subChildren = curCrDs.listDatasets( me );
      } catch ( IOException e ) {
        fail(String.format( "IOException listing children of the CrDs [%s]: %s", curCrDs.getName(), e.getMessage()));
        return;
      }
      assertEquals( "Number of CrDs children", 3, subChildren.size());
      for ( CrawlableDataset curSubCrDs: subChildren ) {
        assertTrue( String.format( "CrDs [%s] does not match pattern [%s]", curSubCrDs.getName(), atomicChildrenMatchPattern),
                    curSubCrDs.getName().matches( atomicChildrenMatchPattern ));
      }
    }
  }

  @Test
  public void testRegExpIncludeAll()
  {
    File tmpTestDir = TestFileDirUtils.addDirectory( tmpTestDataDir, "testRegExpIncludeAll" );
    assertNotNull(tmpTestDir);
    assertTrue( tmpTestDir.exists());
    assertTrue( tmpTestDir.canRead());
    assertTrue( tmpTestDir.canWrite());

    createEtaDirWithCvsAndDotGitDirs( tmpTestDir );

    // Construct filter
    List<MultiSelectorFilter.Selector> selectors = new ArrayList<MultiSelectorFilter.Selector>();
    selectors.add( new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( ".*"), true, true, false ) );
    CrawlableDatasetFilter me = new MultiSelectorFilter( selectors );
    assertTrue( me != null );

    // Get filtered list of datasets.
    List<CrawlableDataset> list = null;
    try {
      list = tmpTestDataCrDs.listDatasets( me);
    } catch ( IOException e ) {
      fail(String.format( "IOException getting children datasets [%s]: %s", tmpTestDataCrDs.getName(), e.getMessage()));
      return;
    }

    int expectedNumDs = 6;
    assertEquals( "Number of datasets", expectedNumDs, list.size());
    for ( CrawlableDataset curCd: list ) {
      assertTrue("Result path [" + curCd.getPath() + "] not as expected [" + allFiles_FullPathNames + "].",
          allFiles_FullPathNames.contains(curCd.getPath()));
    }

  }

  @Test
  public void testRegExpIncludeNcExcludeCvsExcludeDotGit()
  {
    File tmpTestDir = TestFileDirUtils.addDirectory( tmpTestDataDir, "testRegExpIncludeNcExcludeCVS" );
    assertNotNull(tmpTestDir);
    assertTrue( tmpTestDir.exists());
    assertTrue( tmpTestDir.canRead());
    assertTrue( tmpTestDir.canWrite());

    createEtaDirWithCvsAndDotGitDirs( tmpTestDir );

    // Construct filter
    List<MultiSelectorFilter.Selector> selectors = new ArrayList<MultiSelectorFilter.Selector>();
    selectors.add( new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( ".*nc$"), true, true, false ) );
    selectors.add( new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( "CVS"), false, false, true ) );
    selectors.add( new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( "\\.git"), false, false, true ) );
    CrawlableDatasetFilter me = new MultiSelectorFilter( selectors );
    assertTrue( me != null );

    // Get filtered list of datasets.
    List<CrawlableDataset> list = null;
    try {
      list = tmpTestDataCrDs.listDatasets( me );
    } catch ( IOException e ) {
      fail(String.format( "IOException getting children datasets [%s]: %s", tmpTestDataCrDs.getName(), e.getMessage()));
    }
    assertEquals( "Number of datasets", 4, list.size());
    for ( CrawlableDataset curCd: list ) {
      assertTrue("Result path [" + curCd.getPath() + "] not as expected ]" + dataFiles_FullPathNames + "].",
          dataFiles_FullPathNames.contains(curCd.getPath()));
    }
  }

  @Test
  public void testWildcardIncludeNcExcludeCvsExcludeDotGit()
  {
    File tmpTestDir = TestFileDirUtils.addDirectory( tmpTestDataDir, "testWildcardIncludeNcExcludeCVS" );
    assertNotNull(tmpTestDir);
    assertTrue( tmpTestDir.exists());
    assertTrue( tmpTestDir.canRead());
    assertTrue( tmpTestDir.canWrite());

    createEtaDirWithCvsAndDotGitDirs( tmpTestDir );

    // Construct filter
    List<MultiSelectorFilter.Selector> selectors = new ArrayList<MultiSelectorFilter.Selector>();
    selectors.add( new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( "*.nc"), true, true, false ) );
    selectors.add( new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( "CVS"), false, false, true ) );
    selectors.add( new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( ".git"), false, false, true ) );
    CrawlableDatasetFilter me = new MultiSelectorFilter( selectors );
    assertTrue( me != null );

    // Get filtered list of datasets.
    List<CrawlableDataset> list = null;
    try {
      list = tmpTestDataCrDs.listDatasets( me );
    } catch ( IOException e ) {
      fail(String.format( "IOException getting children datasets [%s]: %s", tmpTestDataCrDs.getName(), e.getMessage()));
      return;
    }

    assertEquals( "Number of datasets", 4, list.size());
    for ( CrawlableDataset curCd: list ) {
      assertTrue("Result path [" + curCd.getPath() + "] not as expected [" + dataFiles_FullPathNames + "].",
          dataFiles_FullPathNames.contains(curCd.getPath()));
    }
  }

  private static CrawlableDataset createCrawlableDataset( String path, String name )
  {
    try {
      return CrawlableDatasetFactory.createCrawlableDataset( path, null, null );
    } catch ( Exception e ) {
      fail( String.format( "Failed to create CrawlableDataset [%s]: %s", path, e.getMessage()));
      return null;
    }
  }
}