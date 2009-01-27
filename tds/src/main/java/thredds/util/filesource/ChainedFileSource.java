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
import java.util.List;

/**
 * Implements the FileSource interface using a chain of DescendantFileSource
 * objects. This allows a relative path to be given and located in the first
 * DescendantFileSource that contains a matching File.
 *
 * @author edavis
 * @since 4.0
 */
public class ChainedFileSource implements FileSource
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( ChainedFileSource.class );

  private final List<DescendantFileSource> chain;

  public ChainedFileSource( List<DescendantFileSource> chain )
  {
    if ( chain == null || chain.isEmpty() )
      throw new IllegalArgumentException( "Locator chain must not be null or empty.");

    for ( DescendantFileSource dfs : chain)
      if ( dfs == null )
        throw new IllegalArgumentException( "Locator chain must not contain null items.");
    this.chain = chain;
  }

  /**
   * This implementation requires a relative path. The relative path may
   * not start with "../" or once "normalized" start with "../". Here
   * "normalized" means that "./" and "path/.." segments are removed,
   * e.g., "dir1/../../dir2" once normalized would be "../dir2".
   *
   * @param path the relative path to the desired File.
   * @return the File represented by the given relative path or null if the path is null or the File it represents does not exist.
   */
  public File getFile( String path )
  {
    File file;
    for ( DescendantFileSource curLocator : chain )
    {
      file = curLocator.getFile( path );
      if ( file != null )
        return file;
    }
    return null;
  }

}
