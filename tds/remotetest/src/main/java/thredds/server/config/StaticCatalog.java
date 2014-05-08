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
package thredds.server.config;

import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogImpl;

import java.io.File;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StaticCatalog
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( StaticCatalog.class );

  private final String path;
  private final File catalogFile;
  private final InvCatalog catalog;

  private final StaticCatalog parent;
  private final List<StaticCatalog> children;

  public StaticCatalog( String path, File catalogFile, InvCatalog catalog,
                        StaticCatalog parent, List<StaticCatalog> children )
  {
    if ( path == null || path.equals( "") )
      throw new IllegalArgumentException( "Path may not be null or empty.");
    //if ( catalogFile == null || ! catalogFile.exists())
      //((InvCatalogImpl)catalog).

    this.path = path;
    this.catalogFile = catalogFile;
    this.catalog = catalog;
    this.parent = parent;
    this.children = children;
  }
}
