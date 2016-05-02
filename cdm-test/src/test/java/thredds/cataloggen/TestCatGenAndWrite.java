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
package thredds.cataloggen;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.catalog.InvService;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.TestFileDirUtils;

/**
 * _more_
 *
 * @author edavis
 * @since Feb 1, 2007 10:20:23 PM
 */
public class TestCatGenAndWrite
{
  private File tmpDir;

  @Before
  public void setUp()
  {
    // Create a data directory and some data files.
    tmpDir = TestFileDirUtils.addDirectory( new File( TestDir.temporaryLocalDataDir), "TestCatGenAndWrite" );
  }

  @After
  public void tearDown()
  {
    // Delete temp directory.
    TestFileDirUtils.deleteDirectoryAndContent( tmpDir );
  }

  // ToDo Get working or remove.
  //@Test
  public void testLocalDataFiles()
  {
    String startPath = "dataDir";
    File dataDir = TestFileDirUtils.addDirectory( tmpDir, startPath );

    File eta211Dir = TestFileDirUtils.addDirectory( dataDir, "eta_211" );
    TestFileDirUtils.addFile( eta211Dir, "2004050300_eta_211.nc" );
    TestFileDirUtils.addFile( eta211Dir, "2004050312_eta_211.nc" );
    TestFileDirUtils.addFile( eta211Dir, "2004050400_eta_211.nc" );
    TestFileDirUtils.addFile( eta211Dir, "2004050412_eta_211.nc" );

    File gfs211Dir = TestFileDirUtils.addDirectory( dataDir, "gfs_211" );
    TestFileDirUtils.addFile( gfs211Dir, "2004050300_gfs_211.nc" );
    TestFileDirUtils.addFile( gfs211Dir, "2004050306_gfs_211.nc" );
    TestFileDirUtils.addFile( gfs211Dir, "2004050312_gfs_211.nc" );
    TestFileDirUtils.addFile( gfs211Dir, "2004050318_gfs_211.nc" );


    File catWriteDir = new File( tmpDir, "catWriteDir");
    CrawlableDataset collectionCrDs = new CrawlableDatasetFile( tmpDir );
    InvService service = new InvService( "myServer", "File", collectionCrDs.getPath() + "/", null, null );
    CrawlableDatasetFilter filter = null;
    CrawlableDataset topCatCrDs = collectionCrDs.getDescendant( startPath );

    CatGenAndWrite cgaw = null;
    try
    {
      cgaw = new CatGenAndWrite( "DATA", "My data", "", service,
                                 collectionCrDs, topCatCrDs, filter, null, catWriteDir );
    }
    catch ( IllegalArgumentException e )
    {
      fail( "Bad argument: " + e.getMessage() );
      return;
    }

    try
    {
      cgaw.genAndWriteCatalogTree();
    }
    catch ( IOException e )
    {
      fail( "I/O error generating and writing catalogs at and under \"" + topCatCrDs.getPath() + "\": " + e.getMessage() );
      return;
    }

    crawlCatalogs( new File( new File( catWriteDir, startPath), "catalog.xml") );
  }

  // ToDo Get working or remove.
  @Test
  public void testLocalDataFilesOnTds()
  {
    String startPath = "dataDir";
    File dataDir = TestFileDirUtils.addDirectory( tmpDir, startPath );

    File eta211Dir = TestFileDirUtils.addDirectory( dataDir, "eta_211" );
    TestFileDirUtils.addFile( eta211Dir, "2004050300_eta_211.nc" );
    TestFileDirUtils.addFile( eta211Dir, "2004050312_eta_211.nc" );
    TestFileDirUtils.addFile( eta211Dir, "2004050400_eta_211.nc" );
    TestFileDirUtils.addFile( eta211Dir, "2004050412_eta_211.nc" );

    File gfs211Dir = TestFileDirUtils.addDirectory( dataDir, "gfs_211" );
    TestFileDirUtils.addFile( gfs211Dir, "2004050300_gfs_211.nc" );
    TestFileDirUtils.addFile( gfs211Dir, "2004050306_gfs_211.nc" );
    TestFileDirUtils.addFile( gfs211Dir, "2004050312_gfs_211.nc" );
    TestFileDirUtils.addFile( gfs211Dir, "2004050318_gfs_211.nc" );


    File catWriteDir = new File( tmpDir, "catWriteDir" );
    CrawlableDataset collectionCrDs = new CrawlableDatasetFile( tmpDir );

    InvService service = new InvService( "myServer", "OPeNDAP", "/thredds/dodsC/", null, null );
    CrawlableDatasetFilter filter = null;
    CrawlableDataset topCatCrDs = collectionCrDs.getDescendant( startPath );

    CatGenAndWrite cgaw = null;
    try
    {
      cgaw = new CatGenAndWrite( "DATA", "My data", "tdr", service,
                                 collectionCrDs, topCatCrDs, filter, null, catWriteDir );
    }
    catch ( IllegalArgumentException e )
    {
      fail( "Bad argument: " + e.getMessage() );
      return;
    }

    try
    {
      cgaw.genAndWriteCatalogTree();
    }
    catch ( IOException e )
    {
      fail( "I/O error generating and writing catalogs at and under \"" + topCatCrDs.getPath() + "\": " + e.getMessage() );
      return;
    }

    crawlCatalogs( new File( new File( catWriteDir, startPath), "catalog.xml") );
  }

  private void crawlCatalogs( File topCatalogFile)
  {
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false);
    InvCatalogImpl topCatalog = fac.readXML( topCatalogFile.toURI() );

    //topCatalog.g
    // TODO actually test something
  }
}
