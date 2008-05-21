package thredds.server.config;

import junit.framework.*;
import org.springframework.util.StringUtils;
import thredds.TestAll;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import ucar.unidata.util.TestUtils;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestBasicWithExclusionsDescendantFileSource extends TestCase
{
  private File tmpDir;

  public TestBasicWithExclusionsDescendantFileSource( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    // Create a data directory and some data files.
    tmpDir = TestUtils.addDirectory( new File( TestAll.temporaryDataDir ), "TestBasicWithExclusionsDescendantFileSource" );

    File dir1 = TestUtils.addDirectory( tmpDir, "dir1" );
    TestUtils.addFile( dir1, "file1_1" );
    File dir1_1 = TestUtils.addDirectory( dir1, "dir1_1" );
    File dir1_2 = TestUtils.addDirectory( dir1, "dir1_2" );
    File dir2 = TestUtils.addDirectory( tmpDir, "dir2" );
    File dir2_1 = TestUtils.addDirectory( dir2, "dir2_1" );
    File dir2_2 = TestUtils.addDirectory( dir2, "dir2_2" );
    TestUtils.addFile( dir2_2, "file2_2_1" );
    TestUtils.addDirectory( dir1_2, "dir1_2_1" );

  }

  protected void tearDown()
  {
    // Delete temp directory.
    TestUtils.deleteDirectoryAndContent( tmpDir );
  }

  /**
   * Test ...
   */
  public void testNewWithNullPath()
  {
    List<String> exclusions = Collections.singletonList( "dir2" );
    try
    {
      String path = null;
      new BasicWithExclusionsDescendantFileSource( path, exclusions);
    }
    catch( IllegalArgumentException e )
    {
      try
      {
        File file = null;
        new BasicWithExclusionsDescendantFileSource( file, exclusions);
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
    File nonExistDir = new File( tmpDir, "nonExistDir" );
    List<String> exclusions = Collections.singletonList( "dir2" );

    try
    {
      new BasicWithExclusionsDescendantFileSource( nonExistDir.getPath(), exclusions );
    }
    catch ( IllegalArgumentException e )
    {
      try
      {
        new BasicWithExclusionsDescendantFileSource( nonExistDir, exclusions );
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
    List<String> exclusions = Collections.singletonList( "dir2" );
    try
    {
      notDirFile = File.createTempFile( "TestBasicWithExclusionsDescendantFileSource", "tmp", tmpDir );
    }
    catch ( IOException e )
    {
      fail( "Could not create temporary file." );
    }

    assertTrue( "The temporary non-directory file does not exist.",
                notDirFile.exists() );
    assertFalse( "The temporary non-directory file is a directory.",
                 notDirFile.isDirectory() );
    try
    {
      new BasicWithExclusionsDescendantFileSource( notDirFile, exclusions );
    }
    catch ( IllegalArgumentException e )
    {
      try
      {
        new BasicWithExclusionsDescendantFileSource( notDirFile.getAbsolutePath(), exclusions );
      }
      catch ( IllegalArgumentException e2 )
      {
        return;
      }
    }
    fail( "Did not throw IllegalArgumentException for non-directory root path." );
  }

  public void testNewWithNullExclusions()
  {
    try
    {
      new BasicWithExclusionsDescendantFileSource( tmpDir, null);
    }
    catch( IllegalArgumentException e )
    {
      try
      {
        new BasicWithExclusionsDescendantFileSource( tmpDir.getPath(), null );
      }
      catch ( IllegalArgumentException e2 )
      {
        return;
      }
    }
    fail( "Did not throw IllegalArgumentException for null exclusions.");
  }

  public void testNewWithEmptyExclusions()
  {
    List<String> exclusions = Collections.emptyList();
    try
    {
      new BasicWithExclusionsDescendantFileSource( tmpDir, exclusions);
    }
    catch( IllegalArgumentException e )
    {
      try
      {
        new BasicWithExclusionsDescendantFileSource( tmpDir.getPath(), exclusions );
      }
      catch ( IllegalArgumentException e2 )
      {
        return;
      }
    }
    fail( "Did not throw IllegalArgumentException for empty exclusions.");
  }

  public void testNewWithNonexistentExclusionsDir()
  {
    List<String> exclusions = Collections.singletonList( "dir5" );
    try
    {
      new BasicWithExclusionsDescendantFileSource( tmpDir, exclusions);
    }
    catch( IllegalArgumentException e )
    {
      try
      {
        new BasicWithExclusionsDescendantFileSource( tmpDir.getPath(), exclusions );
      }
      catch ( IllegalArgumentException e2 )
      {
        return;
      }
    }
    fail( "Did not throw IllegalArgumentException for non-existent exclusion directory.");
  }

  public void testNormalizedPath()
  {
    List<String> exclusions = Collections.singletonList( "dir2" );
    DescendantFileSource bfl = new BasicWithExclusionsDescendantFileSource( tmpDir, exclusions );
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath() ),
                  bfl.getRootDirectoryPath() );


    File tmp2 = new File( tmpDir, "./dir1/./dir1_2/../dir1_2/../../dir1");
    exclusions = Collections.singletonList( "dir1_2" );
    bfl = new BasicWithExclusionsDescendantFileSource( tmp2, exclusions );
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath() ),
                  bfl.getRootDirectoryPath() );
  }

  public void testBasics()
  {
    List<String> exclusions = Collections.singletonList( "dir2" );
    DescendantFileSource bfl = new BasicWithExclusionsDescendantFileSource( tmpDir, exclusions );

    // Test getFile()
    assertNull( "Did not get null from getFile(null).",
                bfl.getFile( null ) );
    assertNull( bfl.getFile( "../tmp/dir1"));
    assertNull( bfl.getFile( tmpDir.getAbsolutePath()));
    assertNotNull( bfl.getFile( "dir1"));
    assertNull( bfl.getFile( "dir1/noFile"));
    assertNotNull( bfl.getFile( "dir1/file1_1"));

    assertNull( bfl.getFile( "dir2" )); // excluded
    assertNull( bfl.getFile( "dir2/dir2_1" )); // excluded

    // Test getDescendant()
    DescendantFileSource bfl2 = bfl.getDescendant( "dir1" );
    assertNotNull( bfl2);
    assertEquals( bfl.getFile( "dir1/file1_1"), bfl2.getFile( "file1_1"));
    assertTrue( bfl2.getRootDirectoryPath().startsWith( bfl.getRootDirectoryPath() ));
    assertNull( bfl2.getDescendant( "file1_1" ));
    assertNotNull( bfl2.getDescendant( "dir1_1" ));

    assertNull( bfl.getDescendant( "dir2" )); // excluded
    assertNull( bfl.getDescendant( "dir2/dir2_1" )); // excluded

    // Test isDescendant()
    assertTrue( bfl2.isDescendant( bfl.getFile( "dir1/file1_1" )) );

    assertFalse( bfl.isDescendant( new File( tmpDir, "dir2") )); // excluded
    assertFalse( bfl.isDescendant( new File( tmpDir, "dir2/dir2_1") )); // excluded
    assertFalse( bfl.isDescendant( new File( tmpDir, "dir2/./dir2_1") )); // excluded

    // Test getRelativePath()
    String filePath1_1 = "dir1/file1_1";
    assertEquals( filePath1_1,
                  bfl.getRelativePath( bfl.getFile( filePath1_1 ) ) );
    assertEquals( filePath1_1,
                  bfl.getRelativePath( new File( tmpDir, filePath1_1 ).getPath() ) );
    assertEquals( filePath1_1,
                  bfl.getRelativePath( new File( tmpDir, "dir1/./file1_1" ).getPath() ) );

    assertNull( bfl.getRelativePath( new File( tmpDir, "dir2") )); // excluded
    assertNull( bfl.getRelativePath( new File( tmpDir, "dir2/dir2_1") )); // excluded
    assertNull( bfl.getRelativePath( new File( tmpDir, "dir2/./dir2_1") )); // excluded
    assertNull( bfl.getRelativePath( new File( tmpDir, "dir2/./dir2_1").getPath() )); // excluded

  }
}