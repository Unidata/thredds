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
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestBasicWithExclusionsDescendantFileSource
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
    File dir1_1 = TestFileDirUtils.addDirectory( dir1, "dir1_1" );
    File dir1_2 = TestFileDirUtils.addDirectory( dir1, "dir1_2" );
    File dir2 = TestFileDirUtils.addDirectory( tmpDir, "dir2" );
    File dir2_1 = TestFileDirUtils.addDirectory( dir2, "dir2_1" );
    File dir2_2 = TestFileDirUtils.addDirectory( dir2, "dir2_2" );
    TestFileDirUtils.addFile( dir2_2, "file2_2_1" );
    TestFileDirUtils.addDirectory( dir1_2, "dir1_2_1" );

  }

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
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
