// $Id:IOServiceProvider.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import java.io.IOException;
import java.util.List;

/**
 * This is the service provider interface for the low-level I/O access classes (read only).
 * This is only used by service implementors.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
public interface IOServiceProvider {

   /**
    * Check if this is a valid file for this IOServiceProvider.
    * @param raf RandomAccessFile
    * @return true if valid.
    */
  public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) throws IOException;

  /**
   * Open existing file, and populate ncfile with it.
   * @param raf the file to work on, it has already passed the isValidFile() test.
   * @param ncfile add objects to this NetcdfFile
   * @param cancelTask used to monito user cancellation; may be null.
   * @throws IOException
   */
  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException;

  /**
   * Read data from a top level Variable and return a memory resident Array.
   * This Array has the same element type as the Variable, and the requested shape.
   *
   * @param v2 a top-level Variable
   * @param section List of type Range specifying the section of data to read.
   *   There must be a Range for each Dimension in the variable, in order.
   *   Note: no nulls.
   * @return the requested data in a memory-resident Array
   * @throws java.io.IOException
   * @throws ucar.ma2.InvalidRangeException
   * @see ucar.ma2.Range
   */
  public ucar.ma2.Array readData(ucar.nc2.Variable v2, java.util.List section)
         throws java.io.IOException, ucar.ma2.InvalidRangeException;

  /**
   * Read data from a Variable that is nested in one or more Structures. If there are no Structures in the file,
   *   this will never be called. Return an Array of the same type as the Variable and the requested shape. The shape
   *   must be an accumulation of all the shapes of the Structures containing the variable.
   *
   * <p> v2.getParent() is called to get the containing Structures.
   *
   * @param v2 a nested Variable.
   * @param section List of type Range specifying the section of data to read. There must be a Range for each
   *  Dimension in each parent, as well as in the Variable itself. Must be in order from outer to inner.
   *   Note: no nulls.
   * @return the requested data in a memory-resident Array
   */
  public ucar.ma2.Array readNestedData(ucar.nc2.Variable v2, java.util.List section)
         throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * Close the file.
   * It is the IOServiceProvider's job to close the file (even though it didnt open it),
   * and to free any other resources it has used.
   * @throws IOException
   */
  public void close() throws IOException;

  /** Extend the file if needed in a way that is compatible with the current metadata.
   *  For example, if the unlimited dimension has grown.
   * @return true if file was exteneed.
   * @throws IOException
   */
  public boolean syncExtend() throws IOException;

  /** Check if file has changed, and reread metadata if needed.
   * @return true if file was changed.
   * @throws IOException
   */
  public boolean sync() throws IOException;

  /**
   * A way to communicate arbitrary information to an iosp.
   * Typically this is set before open() is called.
   * @param special opaque special settings.
   */
  public void setSpecial( Object special);

  /** Debug info for this object. */
  public String toStringDebug(Object o);

  /** Show debug / underlying implementation details */
  public String getDetailInfo();

}