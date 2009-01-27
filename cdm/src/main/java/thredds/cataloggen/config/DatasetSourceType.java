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
// $Id: DatasetSourceType.java 63 2006-07-12 21:50:51Z edavis $

package thredds.cataloggen.config;

/**
 * Type-safe enumeration of CatalogGen DatasetSource types.
 *
 * @author Ethan Davis (from John Caron's thredds.catalog.ServiceType)
 * @version $Revision: 63 $
 */
public final class DatasetSourceType
{
  private static java.util.HashMap hash = new java.util.HashMap(20);

  public final static DatasetSourceType LOCAL = new DatasetSourceType( "Local");
  public final static DatasetSourceType DODS_FILE_SERVER = new DatasetSourceType( "DodsFileServer");
  public final static DatasetSourceType DODS_DIR = new DatasetSourceType( "DodsDir");
  public final static DatasetSourceType GRADS_DATA_SERVER = new DatasetSourceType( "GrADSDataServer");

  private String typeName;
  private DatasetSourceType( String name)
  {
    this.typeName = name;
    hash.put( name, this);
  }

  /**
   * Find the DatasetSourceType that matches this name.
   * @param name
   * @return DatasetSourceType or null if no match.
   */
  public static DatasetSourceType getType( String name)
  {
    if ( name == null) return null;
    return ( (DatasetSourceType) hash.get( name));
  }

  /**
   * Return the string name.
   */
  public String toString()
  {
    return typeName;
  }

}
