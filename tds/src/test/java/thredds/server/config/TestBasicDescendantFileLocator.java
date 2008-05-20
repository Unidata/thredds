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
  private File tmpDir;

  public TestBasicDescendantFileLocator( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    // Create a data directory and some data files.
    tmpDir = TestUtil.addDirectory( new File( TestAll.temporaryDataDir ), "TestBasicDescendantFileLocator" );
  }

  protected void tearDown()
  {
    // Delete temp directory.
    TestUtil.deleteDirectoryAndContent( tmpDir );
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
    File nonExistDir = new File( tmpDir, "nonExistDir");
    try
    {
      new BasicDescendantFileSource( nonExistDir.getPath() );
    }
    catch ( IllegalArgumentException e )
    {
      try
      {
        new BasicDescendantFileSource( nonExistDir);
      }
      catch ( IllegalArgumentException e2 )
      {
        return;
      }
    }
    fail( "Did not throw IllegalArgumentException for non-existent directory." );
  }

  public void testNewGivenNondirectoryFile()
  {
    File notDirFile = null;
    try
    {
      notDirFile = File.createTempFile( "TestBasicDescendantFileLocator", "tmp", tmpDir );
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
    DescendantFileSource bfl = new BasicDescendantFileSource( tmpDir );
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath() ),
                  bfl.getRootDirectoryPath() );

    File newDir1 = TestUtil.addDirectory( tmpDir, "fred1" );
    File newDir2 = TestUtil.addDirectory( newDir1, "fred2" );
    TestUtil.addDirectory( newDir2, "fred3" );


    File tmp2 = new File( tmpDir, "./fred1/./fred2/../fred2/../../fred1");
    bfl = new BasicDescendantFileSource( tmp2 );
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath() ),
                  bfl.getRootDirectoryPath() );
  }
}
