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

import java.io.File;

/**
 * Represent a root directory and act as a source for existing java.io.File
 * descendants of the root directory.
 *
 * NOTE: Perhaps should instead be using org.springframework.core.io.Resource.
 *
 * @author edavis
 * @since 4.0
 */
public interface DescendantFileSource extends FileSource
{
  /**
   * Return a DescendantFileSource representing the descendant directory
   * specified by the relative path.
   *
   * @param relativePath the relative path to the descendant directory.
   * @return a FileLocator representing the descendant root directory or null if the path is null or the descendant directory doesn't exist or isn't a directory (or isn't a descendant).
   */
  public DescendantFileSource getDescendant( String relativePath);

//  /**
//   * Return the descendant File, if one exists, specified by the given relative path.
//   *
//   * @param relativePath the relative path to the descendant file.
//   * @return a File descendant of the root directory or null if the path is null or doesn't represent a descendant or the File does not exist.
//   */
//  public File getDescendantFile( String relativePath);

  /**
   * Return the root directory represented by this FileLocator as a java.io.File.
   *
   * @return the root directory represented by this FileLocator.
   */
  public File getRootDirectory();

  /**
   * Return the path of the root directory represented by this FileLocator.
   *
   * @return the path of the root directory represented by this FileLocator.
   */
  public String getRootDirectoryPath();

  /**
   * Return true if the given File is an existing descendant of the root directory, false otherwise.
   *
   * @param file the File to be checked if it is a descendant.
   * @return true if the given file is an existing descendant, false otherwise.
   */
  public boolean isDescendant( File file);

  /**
   * Return true if the given file path is an existing descendant of the root directory, false otherwise.
   *
   * @param filePath the file path to be checked if it is a descendant.
   * @return true if the given file path represents an existing descendant, false otherwise.
   */
  public boolean isDescendant( String filePath);

  /**
   * Return the path relative to the root directory that represents the given descendant file.
   *
   * @param file the File for which the relative path is to be determined.
   * @return the path relative to the root directory that represents the given descendant file or null if the given file is null or not a descendant.
   */
  public String getRelativePath( File file);

  /**
   * Return the path relative to the root directory that represents the given descendant file path.
   *
   * @param filePath the file path for which the relative path is to be determined.
   * @return the path relative to the root directory that represents the given descendant file path or null if the given file path is null or not a descendant.
   */
  public String getRelativePath( String filePath);
}
