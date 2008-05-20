package ucar.unidata.util;

import junit.framework.*;

import java.io.File;
import java.io.IOException;

/**
 * Static utililities for testing
 *
 * @author edavis
 * @since Apr 19, 2007 10:11:24 PM
 */
public class TestUtils
{
  /**
   * Create a new directory for the given path. Fails if the directory already
   * exists or can't be created.
   *
   * @param dirPath the path of the directory to create.
   * @return the java.io.File which represents the newly created directory.
   */
  public static File createDirectory( String dirPath )
  {
    File dirFile = new File( dirPath );
    Assert.assertFalse( "Directory [" + dirFile.getAbsolutePath() + "] already exists.",
                        dirFile.exists());

    Assert.assertTrue( "Failed to make directory [" + dirFile.getAbsolutePath() + "].",
                       dirFile.mkdirs() );

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
  public static File addDirectory( File parentDir, String dirName )
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
  public static File addFile( File parentDir, String fileName )
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

    try
    {
      Assert.assertTrue( "Failed to create new file [" + newFile.getAbsolutePath() + "].",
                         newFile.createNewFile());
    }
    catch ( IOException e )
    {
      Assert.assertTrue( "Failed to create new file <" + newFile.getAbsolutePath() + ">: " + e.getMessage(),
                         false );
    }

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
      if ( curFile.isDirectory() )
      {
        removeAll &= deleteDirectoryAndContent( curFile );
      }
      else
      {
        if ( !curFile.delete() )
        {
          System.out.println( "**ERROR: Failed to delete file <" + curFile.getAbsolutePath() + ">" );
          removeAll = false;
        }
      }
    }

    if ( !directory.delete() )
    {
      System.out.println( "**ERROR: Failed to delete directory <" + directory.getAbsolutePath() + ">" );
      removeAll = false;
    }

    return removeAll;
  }

}
