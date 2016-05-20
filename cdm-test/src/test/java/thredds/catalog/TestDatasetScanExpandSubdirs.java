/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.catalog;

import org.junit.Before;
import org.junit.Test;
import thredds.cataloggen.catalogrefexpander.BooleanCatalogRefExpander;
import thredds.crawlabledataset.sorter.LexigraphicByNameSorter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.TestFileDirUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * test dataset scan with subdirs
 *
 * @author edavis
 * @since 4.0
 */
public class TestDatasetScanExpandSubdirs
{
  private String scanPath;
  private String scanLocation;

  @Before
  public void setupScanLocation()
  {
    File tmpDataDir = new File( TestDir.temporaryLocalDataDir);
    File testDir = TestFileDirUtils.createTempDirectory("dsScanExpandSubdirs", tmpDataDir);
    TestFileDirUtils.addFile( testDir, "file1.nc" );
    TestFileDirUtils.addFile( testDir, "file2.nc" );
    TestFileDirUtils.addFile( testDir, "file3.nc" );
    File subdir1 = TestFileDirUtils.addDirectory( testDir, "subdir1" );
    TestFileDirUtils.addFile( subdir1, "file4.nc" );
    TestFileDirUtils.addFile( subdir1, "file5.nc" );
    File subdir2 = TestFileDirUtils.addDirectory( testDir, "subdir2" );
    TestFileDirUtils.addFile( subdir2, "file6.nc" );
    TestFileDirUtils.addFile( subdir2, "file7.nc" );
    File subdir3 = TestFileDirUtils.addDirectory( subdir2, "subdir2_1" );
    TestFileDirUtils.addFile( subdir3, "file8.nc" );
    TestFileDirUtils.addFile( subdir3, "file9.nc" );

    this.scanPath = "test/path";
    this.scanLocation = testDir.getPath();
  }

  @Test
  public void checkExpandSubdirs() throws URISyntaxException, IOException
  {
    InvCatalogImpl configCat = new InvCatalogImpl( "configCat", "1.0.2", new URI( "http://server/thredds/catalog.xml"));
    configCat.addService( new InvService( "odap", "OPENDAP", "/thredds/dodsC/", null, null ) );
    InvDatasetImpl configRootDs = new InvDatasetImpl( null, "root ds" );
    configCat.addDataset( configRootDs );
    InvDatasetScan scan = new InvDatasetScan( configRootDs, "test", this.scanPath, this.scanLocation,
                                              null, null, null, null, null, true,
                                              new LexigraphicByNameSorter( true),
                                              null, null,
                                              new BooleanCatalogRefExpander( true) );

    scan.setServiceName( "odap" );
    configRootDs.addDataset( scan );
    assertTrue( configCat.finish());

    System.out.printf("%s%n", InvCatalogFactory.getDefaultFactory(false).writeXML(configCat));

    StringBuilder sb = new StringBuilder();
    boolean good = scan.check( sb, true );
    assertTrue( sb.toString(), good);
    assertTrue( sb.toString(), sb.length() == 0 );

    assertTrue( scan.isValid());

    InvCatalogImpl cat = scan.makeCatalogForDirectory( this.scanPath + "/catalog.xml", new URI( "http://server/thredds/catalogs/test/path/catalog.xml") );

    List<InvDataset> dsList = cat.getDatasets();
    assertFalse( dsList.isEmpty());
    assertEquals( 1, dsList.size());
    InvDataset rootDs = dsList.get( 0 );
    assertTrue( rootDs.hasNestedDatasets());

    dsList= rootDs.getDatasets();
    assertFalse( dsList.isEmpty());
    assertEquals( 5, dsList.size());

    assertEquals( "","file1.nc", dsList.get( 0).getName());
    assertEquals( "file2.nc", dsList.get( 1).getName());
    assertEquals( "file3.nc", dsList.get( 2).getName());

    InvDataset invDsSubdir1 = dsList.get( 3 );
    InvDataset invDsSubdir2 = dsList.get( 4 );

    assertEquals( "subdir1", invDsSubdir1.getName());
    dsList = invDsSubdir1.getDatasets();
    assertEquals( 2, dsList.size());
    assertEquals( "file4.nc", dsList.get( 0 ).getName() );
    assertEquals( "file5.nc", dsList.get( 1 ).getName() );

    assertEquals( "subdir2", invDsSubdir2.getName());
    dsList = invDsSubdir2.getDatasets();
    assertEquals( 3, dsList.size() );
    assertEquals( "file6.nc", dsList.get( 0 ).getName() );
    assertEquals( "file7.nc", dsList.get( 1 ).getName() );

    InvDataset invDsSubdir2_1 = dsList.get( 2 );
    assertEquals( "subdir2_1", invDsSubdir2_1.getName());
    dsList = invDsSubdir2_1.getDatasets();
    assertEquals( 2, dsList.size() );
    assertEquals( "file8.nc", dsList.get( 0 ).getName() );
    assertEquals( "file9.nc", dsList.get( 1 ).getName() );
  }
}
