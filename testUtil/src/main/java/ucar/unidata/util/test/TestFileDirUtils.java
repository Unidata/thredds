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
package ucar.unidata.util.test;

import org.junit.Assert;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Static utilities for testing
 *
 * @author edavis
 * @since Apr 19, 2007 10:11:24 PM
 */
public class TestFileDirUtils
{
  private TestFileDirUtils() {}
  
  /**
   * Creates a new temporary directory in the specified directory. The name
   * of the new directory is generated with the given prefix string followed
   * by a random string. Returns null if the given directory does not exist
   * or a temporary directory cannot be created.
   *
   * The temporary directory and any content will be deleted on termination
   * of the VM (i.e., deleteOnExit() has been called on the directory).
   *
   * @param prefix a prefix for the name of the temporary file.
   * @param directory the directory in which to create the temporary directory.
   * @return the File representing the newly created temporary directory.
   *
   * @throws IllegalArgumentException if the prefix is null or shorter than three characters or if the directory is null, does not exist, or is not a directory.
   */
  public static File createTempDirectory( String prefix, File directory ) {
    return createTempDirectory( prefix, directory, -1 );
  }

  public static File createTempDirectory( String prefix, File directory, long lastModTime )
  {
    if ( prefix == null )
      throw new IllegalArgumentException( "Prefix may not be null.");
    if ( prefix.length() < 3 )
      throw new IllegalArgumentException( "Prefix must be at least three characters.");
    if ( directory == null || ! directory.exists() || ! directory.isDirectory() )
      throw new IllegalArgumentException( "Given directory [" + directory.getPath() + "] must exist and be a directory.");

    File newDir = null;
    Random rand = new Random();
    boolean success = false;
    int numTries = 0;
    while ( numTries < 5 )
    {
      newDir = new File( directory, prefix + "." + rand.nextInt( 1000000) );
      if ( newDir.mkdir()) {
        success = true;
        break;
      }
      numTries++;
    }
    if ( ! success )
      return null;

// ToDo Should use exceptions here rather than depend on org.junit
//    if ( lastModTime > 0 && ! newDir.setLastModified( lastModTime ))
//      throw new IOException( String.format( "Failed to set lastModified time on directory [%s].", newDir.getPath()) );

    if ( lastModTime > 0 )
      Assert.assertTrue( "Failed to set lastModified time on directory [" + newDir.getPath() + "].",
                  newDir.setLastModified( lastModTime ));

    newDir.deleteOnExit();
    return newDir;
  }

  /**
   * Create a new directory for the given path. Fails if the directory already
   * exists or can't be created.
   *
   * @param dirPath the path of the directory to create.
   * @return the java.io.File which represents the newly created directory.
   */
  public static File createDirectory( String dirPath ) {
    return createDirectory( dirPath, -1 );
  }

  public static File createDirectory( String dirPath, long lastModTime )
  {
    File dirFile = new File( dirPath );
    Assert.assertFalse( "Directory [" + dirFile.getAbsolutePath() + "] already exists.",
                 dirFile.exists());

    Assert.assertTrue( "Failed to make directory [" + dirFile.getAbsolutePath() + "].",
                dirFile.mkdirs() );

    if ( lastModTime > 0 )
      Assert.assertTrue( "Failed to set lastModified time on directory [" + dirFile.getPath() + "].",
                  dirFile.setLastModified( lastModTime ));
    return dirFile;
  }

  /**
   * In the given parent directory, add a subdirectory. Fail if the parent
   * directory doesn't exist or isn't a directory or the new directory
   * already exists.
   *
   * @param parentDir the already existing parent directory.
   * @param dirName the directory path to create
   * @return the java.io.File that represents the created directory.
   */
  public static File addDirectory( File parentDir, String dirName ) {
    return addDirectory( parentDir, dirName, -1 );
  }

  public static File addDirectory( File parentDir, String dirName, long lastModTime )
  {
    // Check that the parent directory already exists and is a directory.
    Assert.assertTrue( "Parent file does not exist <" + parentDir.getPath() + ">.",
                parentDir.exists() );
    Assert.assertTrue( "Parent file not a directory <" + parentDir.getPath() + ">.",
                parentDir.isDirectory() );

    File newDir = new File( parentDir, dirName );

    Assert.assertFalse( "New directory already exists [" + newDir.getPath() + "].",
                 newDir.exists() );

    // Create the new directory (including any necessary but nonexistent parent directories).
    Assert.assertTrue( "Failed to create the new directory [" + newDir.getAbsolutePath() + "].",
                newDir.mkdirs() );

    if ( lastModTime > 0 )
      Assert.assertTrue( "Failed to set lastModified time on directory [" + newDir.getPath() + "].",
                  newDir.setLastModified( lastModTime ));

    return newDir;
  }

  /**
   * Add a file directly in the given parent directory. Fail if the parent
   * directory doesn't exist or isn't a directory or if the new file
   * already exists.
   *
   * @param parentDir the already existing parent directory.
   * @param fileName the name of the file to create (may not contain multiple path segments).
   * @return the java.io.File that represents the created file.
   */
  public static File addFile( File parentDir, String fileName ) {
    return addFile( parentDir, fileName, -1 );
  }

  public static File addFile( File parentDir, String fileName, long lastModTime )
  {
    // Check that the parent directory already exists and is a directory.
    Assert.assertTrue( "Parent file does not exist <" + parentDir.getPath() + ">.",
                parentDir.exists() );
    Assert.assertTrue( "Parent file not a directory <" + parentDir.getPath() + ">.",
                parentDir.isDirectory() );

    File newFile = new File( parentDir, fileName );

    Assert.assertFalse( "New file [" + newFile.getAbsolutePath() + "] already exists.",
                 newFile.exists());

    // Make sure the new file is directly contained in the parent directory, not a subdirectory.
    Assert.assertTrue( "Multiple levels not allowed in file name <" + fileName + ">.",
                newFile.getParentFile().equals( parentDir ) );

    try {
      Assert.assertTrue( "Failed to create new file [" + newFile.getAbsolutePath() + "].",
                  newFile.createNewFile());
    } catch ( IOException e ) {
      Assert.fail( "Failed to create new file <" + newFile.getAbsolutePath() + ">: " + e.getMessage() );
    }
    if ( lastModTime > 0 )
      Assert.assertTrue( "Failed to set lastModified time on file [" + newFile.getPath() + "].",
                  newFile.setLastModified( lastModTime ));

    return newFile;
  }

  /**
   * Delete the given directory including any files or directories contained in the directory.
   *
   * Note: this method is called recursively to delete all subdirectories.
   *
   * @param directory the directory to remove
   * @return true if and only if the file or directory is successfully deleted; false otherwise.
   */
  public static boolean deleteDirectoryAndContent( File directory )
  {
    if ( !directory.exists() ) return false;
    if ( !directory.isDirectory() ) return false;

    boolean removeAll = true;

    File[] files = directory.listFiles();
    for ( int i = 0; i < files.length; i++ )
    {
      File curFile = files[i];
      if ( curFile.isDirectory() ) {
        removeAll &= deleteDirectoryAndContent( curFile );
      } else {
        if ( !curFile.delete() ) {
          System.out.println( "**ERROR: Failed to delete file <" + curFile.getAbsolutePath() + ">" );
          removeAll = false;
        }
      }
    }

    if ( !directory.delete() ) {
      System.out.println( "**ERROR: Failed to delete directory <" + directory.getAbsolutePath() + ">" );
      removeAll = false;
    }

    return removeAll;
  }
}
