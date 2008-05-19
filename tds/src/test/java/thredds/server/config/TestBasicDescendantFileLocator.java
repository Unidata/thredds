package thredds.server.config;

import junit.framework.*;
import org.springframework.util.StringUtils;
import thredds.TestAll;

import java.io.File;
import java.io.IOException;

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
    File tmpDir = new File( TestAll.temporaryDataDir );
    File notDirFile = null;
    try
    {
      notDirFile = File.createTempFile( "TestBasicDescendantFileLocator", "tmp", tmpDir );
    }
    catch ( IOException e )
    {
      fail( "Could not create temporary file.");
    }

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
    File tmpDir = new File( TestAll.temporaryDataDir );

    DescendantFileSource bfl = new BasicDescendantFileSource( tmpDir);
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath()),
                  bfl.getRootDirectoryPath());

    File tmp2 = new File( tmpDir, "../fred/./../julie/marge/../franky");
    bfl = new BasicDescendantFileSource( tmp2 );
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath() ),
                  bfl.getRootDirectoryPath() );

  }
}
