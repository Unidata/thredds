package thredds.crawlabledataset.filter;

import junit.framework.*;

import java.io.File;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since Jan 22, 2007 10:04:56 PM
 */
public class TestLogicalCompFilterFactory extends TestCase
{
  public TestLogicalCompFilterFactory( String name )
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
    File tmpFile = null;
    try
    {
      tmpFile = File.createTempFile( "temp", "file");
    }
    catch ( IOException e )
    {
      assertTrue( "Failed to create temp file in default temp directory: " + e.getMessage(),
                  false );
      return;
    }

    File tmpDir = new File( tmpFile.getParentFile(), "thredds.testLogicalComFilterFactory");

    File file1 = new File( tmpDir, "new.grib1");
    File file2 = new File( tmpDir, "old.grib1");
    File file3 = new File( tmpDir, "new.nc" );
    File file4 = new File( tmpDir, "old.nc" );

    if ( !tmpDir.mkdir() )
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


    if ( ! file2.setLastModified( System.currentTimeMillis() - 360000 )
         && ! file4.setLastModified( System.currentTimeMillis() - 360000 ))
    {
      assertTrue( "Failed to set last modified time <" + file2.getPath() + "> <" + file4.getPath() + ">.",
                  false);
      return;
    }


    // ******** DO TEST STUFF **********

    if ( ! file1.delete() && ! file2.delete()
         && ! file3.delete() && ! file4.delete() )
    {
      System.out.println( "Failed to delete at least one temp file <dir=" + tmpDir.getAbsolutePath() + ">.");
      return;
    }
    if ( ! tmpDir.delete())
    {
      System.out.println( "Failed to remove temp dir <" + tmpDir.getAbsolutePath() + ">." );
      return;
    }
    if ( ! tmpFile.delete() )
    {
      System.out.println( "Failed to remove temp file <" + tmpFile.getAbsolutePath() + ">." );
      return;
    }
  }
}
