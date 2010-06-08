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
// $Id: CrawlableDatasetFactory.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;

/**
 * A description
 *
 * @author edavis
 * @since Jun 23, 2005T10:29:26 PM
 */
public class CrawlableDatasetFactory
{


  private static String defaultClassName = "thredds.crawlabledataset.CrawlableDatasetFile";

  /**
   * Construct a CrawlableDataset for the given path using the
   * CrawlableDataset implementation indicated by the given class name.
   *
   * The class given by className is used to instantiate the requested
   * CrawlableDataset and so must be an implementation of
   * thredds.crawlabledataset.CrawlableDataset. The given class must also
   * supply a public constructor with a single String argument. The String
   * argument is the path for the CrawlableDataset being constructed.
   *  
   * If className is null, thredds.crawlabledataset.CrawlableDatasetFile will
   * be used.
   *
   * If the path is an alias containing wildcard characters ("*"),
   * the class thredds.crawlabledataset.CrawlableDatasetAlias will be used to
   * expand the alias into a collection of CrawlableDatasets of the type given
   * by className.
   *
   * @param path the path of the CrawlableDataset.
   * @param className the class name of the CrawlableDataset implementation to instantiate.
   * @param configObj
   *
   * @return a CrawlableDataset for the given path and of the type given by the class name.
   *
   * @throws IOException if a CrawlableDataset cannot be created due to IO problems.
   * @throws ClassNotFoundException if the given CrawlableDataset implementation was not found.
   * @throws NoSuchMethodException if the given CrawlableDataset implementation does not have a constructor with a single String parameter which is required.
   * @throws IllegalAccessException if the constructor is inaccessible due to Java language access control.
   * @throws InvocationTargetException if the constructor throws an exception.
   * @throws InstantiationException if the given CrawlableDataset implementation is an abstract class.
   *
   * @throws NullPointerException if the given path is null.
   * @throws IllegalArgumentException if the given class name is not an implementation of CrawlableDataset.
   */
  public static CrawlableDataset createCrawlableDataset( String path, String className, Object configObj )
          throws IOException,
                 ClassNotFoundException, NoSuchMethodException,
                 IllegalAccessException, InvocationTargetException,
                 InstantiationException, IllegalArgumentException, NullPointerException
          // throws CrDsException, IllegalArgumentException
  {
    if ( path == null ) throw new NullPointerException( "Given path must not be null.");
    String tmpClassName = ( className == null
                            ? defaultClassName
                            : className );

    // @todo Remove alias until sure how to handle things like ".scour*" being a regular file.
//    if ( CrawlableDatasetAlias.isAlias( tmpPath) )
//        return new CrawlableDatasetAlias( tmpPath, tmpClassName, configObj );

    // Get the Class instance for desired CrawlableDataset implementation.
    Class crDsClass = Class.forName( tmpClassName );

    // Check that the Class is a CrawlableDataset.
    if ( ! CrawlableDataset.class.isAssignableFrom( crDsClass ) )
    {
      throw new IllegalArgumentException( "Requested class <" + className + "> not an implementation of thredds.crawlabledataset.CrawlableDataset.");
    }

    // Instantiate the desired CrawlableDataset.
    Class [] argTypes = { String.class, Object.class };
    Object [] args = { path, configObj };
    Constructor constructor = crDsClass.getDeclaredConstructor( argTypes );

    try
    {
      return (CrawlableDataset) constructor.newInstance( args );
    }
    catch ( InvocationTargetException e )
    {
      if ( IOException.class.isAssignableFrom( e.getCause().getClass()) )
        throw (IOException) e.getCause();
      else throw e;
    }
  }

  /**
   * Normalize the given path so that it can be used in the creation of a CrawlableDataset.
   * This method can be used on absolute or relative paths.
   *
   * Normal uses slashes ("/") as path seperator, not backslashes ("\"), and does
   * not use trailing slashes. This function allows users to specify Windows
   * pathnames and UNC pathnames in there normal manner.
   *
   * @param path the path to be normalized.
   * @return the normalized path.
   * @throws NullPointerException if path is null.
   */
  public static String normalizePath( String path )
  {
    // Replace any occurance of a backslash ("\") with a slash ("/").
    // NOTE: Both String and Pattern escape backslash, so need four backslashes to find one.
    // NOTE: No longer replace multiple backslashes with one slash, which allows for UNC pathnames (Windows LAN addresses).
    //       Was path.replaceAll( "\\\\+", "/");
    String newPath = path.replaceAll( "\\\\", "/");

    // Remove trailing slashes.
    while ( newPath.endsWith( "/") && ! newPath.equals("/") )
      newPath = newPath.substring( 0, newPath.length() - 1 );

    return newPath;
  }
}
