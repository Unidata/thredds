package thredds.crawlabledataset;

import junit.framework.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import ucar.nc2.TestAll;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestCrawlableDatasetFile extends TestCase
{
  public TestCrawlableDatasetFile( String name )
  {
    super( name );
  }

  public void testUnreadableDir_Level2()
  {
    File topDir = new File( new File( TestAll.upcShareThreddsDataDir ), "TestCrawlableDatasetFile");
    File testDir = new File( topDir, "testUnreadableDir"); 
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
