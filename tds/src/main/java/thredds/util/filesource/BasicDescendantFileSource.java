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

import org.springframework.util.StringUtils;

import java.io.File;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BasicDescendantFileSource
        implements DescendantFileSource
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( BasicDescendantFileSource.class );

  private final String rootDirectoryPath;
  private final File rootDirectory;

  public BasicDescendantFileSource( File rootDirectory)
  {
    if ( rootDirectory == null )
      throw new IllegalArgumentException( "Root directory must not be null." );
    if ( ! rootDirectory.exists() )
      throw new IllegalArgumentException( "Root directory must exist: " + rootDirectory.getAbsolutePath() + "." );
    if ( ! rootDirectory.isDirectory() )
      throw new IllegalArgumentException( "Root directory must be a directory: " + rootDirectory.getAbsolutePath() + "." );
    this.rootDirectoryPath = StringUtils.cleanPath( rootDirectory.getAbsolutePath() );
    this.rootDirectory = new File( this.rootDirectoryPath );
  }

  public BasicDescendantFileSource( String rootDirectoryPath )
  {
    if ( rootDirectoryPath == null )
      throw new IllegalArgumentException( "Root directory must not be null.");
    File file = new File( rootDirectoryPath );
    if ( ! file.exists() )
      throw new IllegalArgumentException( "Root directory must exist: " + rootDirectoryPath + "." );
    if ( ! file.isDirectory() )
      throw new IllegalArgumentException( "Root directory must be a directory: " + rootDirectoryPath + "." );

    this.rootDirectoryPath = StringUtils.cleanPath( file.getAbsolutePath() );
    this.rootDirectory = new File( this.rootDirectoryPath);
  }

  /**
   * This implementation requires the path to be relative to the root
   * directory and a descendant of the root directory. It also requires the
   * relative path at each path segment to be a descendant of the root
   * directory, i.e., it cannot start with "../" or contain "../" path
   * segments such that once "normalized" it would start with "../"
   * (e.g., "dir1/../../dir2" once normalized would be "../dir2").
   *
   * @param path the relative path to the descendant File.
   * @return the descendant File represented by the given relative path or null if the path is null, not relative to the root, not a descendant, or the File doesn't exist.
   */
  public File getFile( String path )
  {
    if ( path == null )
      return null;
    String workPath = StringUtils.cleanPath( path );
    if ( workPath.startsWith( "../" ) )
      return null;
    if ( new File( workPath ).isAbsolute() )
      return null;

    File file = new File( this.rootDirectory, workPath );
    if ( file.exists())
      return file;
    else
      return null;
  }

  public DescendantFileSource getDescendant( String relativePath )
  {
    File descendantFile = getFile( relativePath );
    if ( descendantFile == null || ! descendantFile.isDirectory() )
      return null;
    return new BasicDescendantFileSource( descendantFile );
  }

  public File getRootDirectory()
  {
    return this.rootDirectory;
  }

  public String getRootDirectoryPath()
  {
    return this.rootDirectoryPath;
  }

  public boolean isDescendant( File file )
  {
    if ( file == null )
      return false;
    String relPath = getRelativePath( file );
    if ( relPath == null || relPath.equals( "" ))
      return false;
    else
      return true;
  }

  public boolean isDescendant( String filePath )
  {
    return filePath == null ? false : isDescendant( new File( filePath ) );
  }

  public String getRelativePath( File file )
  {
    if ( file == null )
      return null;
    String cleanPath = StringUtils.cleanPath( file.getAbsolutePath() ).trim();
    if ( cleanPath.startsWith( this.rootDirectoryPath + "/" )
         && cleanPath.length() > this.rootDirectoryPath.length() + 1 )
    {
      String relativePath = cleanPath.substring( this.rootDirectoryPath.length() + 1 );
      return relativePath.trim();
    }
    return null;
  }

  public String getRelativePath( String filePath )
  {
    if ( filePath == null )
      return null;
    return getRelativePath( new File( filePath) );
  }
}
