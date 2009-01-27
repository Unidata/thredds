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
package thredds.crawlabledataset.filter;

import junit.framework.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;

/**
 * _more_
 *
 * @author edavis
 * @since Jan 22, 2007 10:04:56 PM
 */
public class TestLogicalFilterComposer extends TestCase
{
  private File tmpFile;
  private File tmpDir;
  private File file1;
  private File file2;
  private File file3;
  private File file4;

  public TestLogicalFilterComposer( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testOne()
  {
    createFiles();

    // ******** DO TEST STUFF **********
    /*
      <filter logicalComp="OR">
        <!-- Only grib1 files that are older than 60 seconds -->
        <filter logicalComp="AND">
          <filter>
            <include wildcard="*.grib1"/>
          </filter>
          <filter lastModifiedLimit="60000"  />
        </filter>
        <!-- Only nc files that are less then 60 seconds old -->
        <filter logicalComp="AND">
          <filter>
            <include wildcard="*.nc"/>
          </filter>
          <filter logicalComp="NOT">
            <filter lastModifiedLimit="60000"/>
          </filter>
        </filter>
      </filter>
     */
    CrawlableDatasetFilter includeGribFilter =
            new MultiSelectorFilter( new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( "*.grib1"), true, true, false ) );
    CrawlableDatasetFilter lastModAtLeast4MinPastFilter = new LastModifiedLimitFilter( 240000 );
    CrawlableDatasetFilter oldGribFilter =
            LogicalFilterComposer.getAndFilter( includeGribFilter, lastModAtLeast4MinPastFilter);

    CrawlableDatasetFilter includeNcFilter =
            new MultiSelectorFilter( new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( "*.nc"), true, true, false ) );
    CrawlableDatasetFilter newNcFilter =
            LogicalFilterComposer.getAndFilter( includeNcFilter,
            LogicalFilterComposer.getNotFilter( lastModAtLeast4MinPastFilter ) );

    CrawlableDatasetFilter oldGribOrNewNcFilter = LogicalFilterComposer.getOrFilter( oldGribFilter, newNcFilter);

    CrawlableDataset tmpDirCrDs = new CrawlableDatasetFile( tmpDir);
    List crDsList = null;
    try
    {
      crDsList = tmpDirCrDs.listDatasets();
    }
    catch ( IOException e )
    {
      assertTrue( "I/O problem getting contained dataset list.",
                  false );
      deleteFiles();
      return;
    }
    for ( Iterator it = crDsList.iterator(); it.hasNext(); )
    {
      CrawlableDataset curCrDs = (CrawlableDataset) it.next();
      if ( oldGribOrNewNcFilter.accept( curCrDs) )
      {
        if ( ! curCrDs.getName().equals( "old.grib1")
             && ! curCrDs.getName().equals( "new.nc"))
        {
          assertTrue( "Matched wrong file <" + curCrDs.getPath() + ">.",
                      false);
          deleteFiles();
          return;
        }
      }
    }

    // ******** DO TEST STUFF - END **********
    deleteFiles();
  }

  private void createFiles()
  {
    try
    {
      tmpFile = File.createTempFile( "thredds.testLogicalCompFilterFactory", "file" );
    }
    catch ( IOException e )
    {
      assertTrue( "Failed to create temp file in default temp directory: " + e.getMessage(),
                  false );
      return;
    }

    tmpDir = new File( tmpFile.getParentFile(), "thredds.testLogicalCompFilterFactory" );

    file1 = new File( tmpDir, "new.grib1" );
    file2 = new File( tmpDir, "old.grib1" );
    file3 = new File( tmpDir, "new.nc" );
    file4 = new File( tmpDir, "old.nc" );

    if ( ! tmpDir.mkdir() )
    {
      assertTrue( "Failed to create test dir <" + tmpDir.getAbsolutePath() + ">.",
                  false );
      return;
    }
    try
    {
      file1.createNewFile();
      file2.createNewFile();
      file3.createNewFile();
      file4.createNewFile();
    }
    catch ( IOException e )
    {
      assertTrue( "Failed to create test file: " + e.getMessage(),
                  false );
      return;
    }


    if ( !file2.setLastModified( System.currentTimeMillis() - 360000 ) )
    {
      assertTrue( "Failed to set last modified time <" + file2.getPath() + ">.",
                  false );
      deleteFiles();
      return;
    }
    if ( !file4.setLastModified( System.currentTimeMillis() - 360000 ) )
    {
      assertTrue( "Failed to set last modified time <" + file4.getPath() + ">.",
                  false );
      deleteFiles();
      return;
    }

  }

  private void deleteFiles()
  {
    if ( !file1.delete() && !file2.delete()
         && !file3.delete() && !file4.delete() )
    {
      System.out.println( "Failed to delete at least one temp file <dir=" + tmpDir.getAbsolutePath() + ">." );
      return;
    }
    if ( !tmpDir.delete() )
    {
      System.out.println( "Failed to remove temp dir <" + tmpDir.getAbsolutePath() + ">." );
      return;
    }
    if ( !tmpFile.delete() )
    {
      System.out.println( "Failed to remove temp file <" + tmpFile.getAbsolutePath() + ">." );
      return;
    }
  }
}
