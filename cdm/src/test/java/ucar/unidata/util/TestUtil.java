package ucar.unidata.util;

import junit.framework.*;

import java.io.File;

/**
 * _more_
 *
 * @author edavis
 * @since Apr 19, 2007 10:11:24 PM
 */
public class TestUtil
{
  public static File createDirectory( String dirPath )
  {
    File dirFile = new File( dirPath );
    if ( dirFile.exists() )
    {
      System.out.println( "**WARN: Deleting temporary directory <" + dirPath + "> from previous run." );
      if ( !deleteDirectoryAndContent( dirFile ) )
      {
        System.out.println( "**ERROR: Unable to delete already existing temporary directory <" + dirFile.getAbsolutePath() + ">." );
        return null;
      }
    }

    if ( !dirFile.mkdirs() )
    {
      System.out.println( "**ERROR: Failed to make directory <" + dirFile.getAbsolutePath() + ">." );
      return null;
    }
    return dirFile;
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
