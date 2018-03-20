/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util.filesource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ucar.unidata.util.test.TestFileDirUtils;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static org.junit.Assert.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestBasicDescendantFileSource
{
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
  private File tmpDir;

  @Before
  public void setUp() throws IOException {
    // Create a data directory and some data files.
    tmpDir = tempFolder.newFolder();

    File dir1 = TestFileDirUtils.addDirectory( tmpDir, "dir1" );
    TestFileDirUtils.addFile( dir1, "file1_1" );
    TestFileDirUtils.addDirectory( dir1, "dir1_1" );
    File dir1_2 = TestFileDirUtils.addDirectory( dir1, "dir1_2" );
    File dir2 = TestFileDirUtils.addDirectory( tmpDir, "dir2" );
    TestFileDirUtils.addDirectory( dir2, "dir2_1" );
    File dir2_2 = TestFileDirUtils.addDirectory( dir2, "dir2_2" );
    TestFileDirUtils.addFile( dir2_2, "file2_2_1" );
    TestFileDirUtils.addDirectory( dir1_2, "dir1_2_1" );
  }

  @Test
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

  @Test
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

  @Test
  public void testNewGivenNondirectoryFile()
  {
    File notDirFile = null;
    try
    {
      notDirFile = File.createTempFile( "TestBasicDescendantFileSource", "tmp", tmpDir );
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

  @Test
  public void testNormalizedPath()
  {
    DescendantFileSource bfl = new BasicDescendantFileSource( tmpDir );
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath() ),
                  bfl.getRootDirectoryPath() );


    File tmp2 = new File( tmpDir, "./dir1/./dir1_2/../dir1_2/../../dir1");
    bfl = new BasicDescendantFileSource( tmp2 );
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath() ),
                  bfl.getRootDirectoryPath() );

    bfl = new BasicDescendantFileSource( tmp2.getPath() );
    assertEquals( "Root directory path not clean.",
                  StringUtils.cleanPath( bfl.getRootDirectoryPath() ),
                  bfl.getRootDirectoryPath() );
  }

  @Test
  public void testBasics()
  {
    DescendantFileSource bfl = new BasicDescendantFileSource( tmpDir );
    assertNotNull( bfl);

    // Test getFile() with null.
    assertNull( "Did not get null from getFile(null).",
                bfl.getFile( null ) );
    // Test getFile() with path starting with "../".
    assertNull( bfl.getFile( "../tmp/dir1"));
    // Test getFile() with absolute path.
    assertNull( bfl.getFile( tmpDir.getAbsolutePath()));
    // Test getFile()
    assertNotNull( bfl.getFile( ""));
    assertTrue( bfl.getFile( "").equals( tmpDir.getAbsoluteFile()));
    assertNotNull( bfl.getFile( "dir1"));
    assertNull( bfl.getFile( "dir1/noFile"));
    assertNotNull( bfl.getFile( "dir1/file1_1"));
    assertNotNull( bfl.getFile( "dir1/./file1_1"));

    // Test getDescendant()
    DescendantFileSource bfl2 = bfl.getDescendant( "dir1" );
    assertNotNull( bfl2);
    assertEquals( bfl.getFile( "dir1/file1_1"), bfl2.getFile( "file1_1"));
    assertTrue( bfl2.getRootDirectoryPath().startsWith( bfl.getRootDirectoryPath() ));
    assertNull( bfl2.getDescendant( "file1_1" ));
    assertNotNull( bfl2.getDescendant( "dir1_1" ));
    assertNotNull( bfl.getDescendant( "dir1/./dir1_1" ));

    // Test isDescendant()
    assertTrue( bfl2.isDescendant( bfl.getFile( "dir1/file1_1" )) );
    assertTrue( bfl2.isDescendant( new File( tmpDir, "dir1/file1_1" ).getPath()) );
    assertTrue( bfl2.isDescendant( new File( tmpDir, "dir1/./file1_1" ).getPath()) );

    // Test getRelativePath()
    String filePath1_1 = "dir1/file1_1";
    assertEquals( filePath1_1,
                  bfl.getRelativePath( bfl.getFile( filePath1_1 ) ) );
    assertEquals( filePath1_1,
                  bfl.getRelativePath( new File( tmpDir, filePath1_1 ).getPath() ) );
    assertEquals( filePath1_1,
                  bfl.getRelativePath( new File( tmpDir, "dir1/./file1_1" ).getPath() ) );
  }
}
