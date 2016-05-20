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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.TestFileDirUtils;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestCrawlableDatasetFile
{

  // ToDo Not sure what this test is trying to test. Perhaps things that are no longer the case.
  //@Test
  public void testUnreadableDir_Level2()
  {
    File tmpDataRootDir = new File( TestDir.temporaryLocalDataDir);
    assertTrue( tmpDataRootDir.exists() );
    assertTrue( tmpDataRootDir.canRead() );
    assertTrue( tmpDataRootDir.canWrite() );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "TestCrawlableDatasetFile.testUnreadableDir_Level2", tmpDataRootDir );
    assertNotNull( tmpDataDir );

    CrawlableDatasetFile tmpDataDirCrDs = new CrawlableDatasetFile( tmpDataDir );
    assertTrue( tmpDataDirCrDs.exists() );
    assertTrue( tmpDataDirCrDs.isCollection() );

    File testDir = new File( tmpDataDir, "testUnreadableDir");
    CrawlableDatasetFile crDs = new CrawlableDatasetFile( testDir);
    assertTrue( crDs.exists());
    assertTrue( crDs.isCollection());

    List<CrawlableDataset> crDsChildren = null;
    try
    {
      crDsChildren = crDs.listDatasets();
    }
    catch ( IOException e )
    {
      fail( "Couldn't list children: " + e.getMessage());
    }
    assertTrue( crDsChildren.size() == 1);
    crDs = (CrawlableDatasetFile) crDsChildren.get( 0 );
    try
    {
      crDsChildren = crDs.listDatasets();
    }
    catch ( IOException e )
    {
      fail( "Couldn't list children: " + e.getMessage() );
    }

    for ( CrawlableDataset curCrDs : crDsChildren )
    {
      if ( curCrDs.getName().equals( "aSubSubdir"))
        assertTrue( curCrDs.exists() );
      else if ( curCrDs.getName().equals( "anUnreadableSubSubdir"))
      {
        if ( curCrDs.exists() )
        {
          System.out.println( "DANG! File.canRead()==true when it shouldn't." );
          System.out.println( "      OS Name        : " + System.getProperty( "os.name" ));
          System.out.println( "      OS Architecture: " + System.getProperty( "os.arch" ));
          System.out.println( "      OS Version     : " + System.getProperty( "os.version" ));
          List<CrawlableDataset> kids = null;
          try
          {
            kids = curCrDs.listDatasets();
          }
          catch ( IOException e )
          {
            fail( "Listing children failed: " + e.getMessage());
          }
          assertTrue( kids.isEmpty() );
        }
        else
        {
          System.out.println( "YAY! File.canRead() returned false?" );
        }
      }
      else
        fail( "Unexpected sub-subdirectory [" + curCrDs.getPath() + "].");
    }
  }
}
