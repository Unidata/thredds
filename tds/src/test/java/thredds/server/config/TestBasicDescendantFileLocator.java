package thredds.server.config;

import junit.framework.*;
import org.springframework.util.StringUtils;
import thredds.TestAll;

import java.io.File;
import java.io.IOException;

import ucar.unidata.util.TestUtil;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestBasicDescendantFileLocator extends TestCase
{
  public TestBasicDescendantFileLocator( String name )
  {
    super( name );
  }

  /**
   * Test ...
   */
  public void testNewGivenNullPath()
  {
    String path = null;
    try
    {
      new BasicDescendantFileSource( path );
    }
    catch( IllegalArgumentException e )
    {
      File file = null;
      try
      {
        new BasicDescendantFileSource( file);
      }
      catch( IllegalArgumentException e2 )
      {
        return;
      }
    }
    fail( "Did not throw IllegalArgumentException for null path.");
  }

  public void testNewGivenNonexistentDirectory()
  {
    String path = TestAll.temporaryDataDir + "nonExistDir";
    try
    {
      new BasicDescendantFileSource( path );
    }
    catch ( IllegalArgumentException e )
    {
      File file = new File( path);
      try
      {
        new BasicDescendantFileSource( file);
      }
      catch ( IllegalArgumentException e2 )
      {
        return;
      }
    }
    fail( "Did not throw IllegalArgumentException for null path." );
  }

  public void testNewGivenNondirectoryFile()
  {
    File dir = new File( TestAll.temporaryDataDir );
    File notDirFile = null;
    try
    {
      notDirFile = File.createTempFile( "TestBasicDescendantFileLocator", "tmp", dir );
    }
    catch ( IOException e )
    {
      fail( "Could not create temporary file.");
    }

    assertTrue( "The temporary non-directory file does not exist.",
                notDirFile.exists());
    assertFalse( "The temporary non-directory file is a directory.",
                 notDirFile.isDirectory());
    try
    {
      new BasicDescendantFileSource( notDirFile );
    }
    catch ( IllegalArgumentException e )
    {
      try
      {
        new BasicDescendantFileSource( notDirFile.getAbsolutePath() );
      }
      catch ( IllegalArgumentException e2 )
      {
        return;
      }
    }
    fail( "Did not throw IllegalArgumentException for non-directory root path." );
  }

  public void testNormalizedPath()
  {
    // Create a data directory and some data files.
    File tmpDir = TestUtil.addDirectory( new File( TestAll.temporaryDataDir ), "TestBasicDescendantFileLocator" );

    String startPath = "dataDir";
    File dataDir = TestUtil.addDirectory( tmpDir, startPath );

    File eta211Dir = TestUtil.addDirectory( dataDir, "eta_211" );
    TestUtil.addFile( eta211Dir, "2004050300_eta_211.nc" );
    TestUtil.addFile( eta211Dir, "2004050312_eta_211.nc" );
    TestUtil.addFile( eta211Dir, "2004050400_eta_211.nc" );
    TestUtil.addFile( eta211Dir, "2004050412_eta_211.nc" );

    File gfs211Dir = TestUtil.addDirectory( dataDir, "gfs_211" );
    TestUtil.addFile( gfs211Dir, "2004050300_gfs_211.nc" );
    TestUtil.addFile( gfs211Dir, "2004050306_gfs_211.nc" );
    TestUtil.addFile( gfs211Dir, "2004050312_gfs_211.nc" );
    TestUtil.addFile( gfs211Dir, "2004050318_gfs_211.nc" );

//    File tmpDir = new File( TestAll.temporaryDataDir );
//    TestAll.

    DescendantFileSource bfl = new BasicDescendantFileSource( tmpDir);
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath()),
                  bfl.getRootDirectoryPath());

    File tmp2 = new File( tmpDir, "../fred/./../julie/marge/../franky");
    bfl = new BasicDescendantFileSource( tmp2 );
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath() ),
                  bfl.getRootDirectoryPath() );

    // Delete temp directory.
    TestUtil.deleteDirectoryAndContent( tmpDir );

  }
}
