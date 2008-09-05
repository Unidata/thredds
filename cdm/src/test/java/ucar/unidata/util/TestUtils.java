package ucar.unidata.util;

import junit.framework.*;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Static utililities for testing
 *
 * @author edavis
 * @since Apr 19, 2007 10:11:24 PM
 */
public class TestUtils
{
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
  public static File createTempDirectory( String prefix, File directory )
  {
    if ( prefix == null )
      throw new IllegalArgumentException( "Prefix may not be null.");
    if ( prefix.length() < 3 )
      throw new IllegalArgumentException( "Prefix must be at least three characters.");
    if ( directory == null || ! directory.exists() || ! directory.isDirectory() )
      throw new IllegalArgumentException( "Given directory must exist and be a directory.");

    File newDir = null;
    Random rand = new Random();
    boolean success = false;
    int numTries = 0;
    while ( numTries < 5 )
    {
      newDir = new File( directory, prefix + "." + rand.nextInt( 1000000) );
      if ( newDir.mkdir())
      {
        success = true;
        break;
      }
      numTries++;
    }
    if ( ! success )
      return null;

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
