package thredds.server.config;

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
