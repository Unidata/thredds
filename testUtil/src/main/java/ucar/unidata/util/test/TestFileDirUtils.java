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
}
