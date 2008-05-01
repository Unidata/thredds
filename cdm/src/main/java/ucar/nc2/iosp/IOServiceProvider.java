/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp;

import ucar.ma2.Section;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.SequenceDataCursor;
import ucar.nc2.Structure;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * This is the service provider interface for the low-level I/O access classes (read only).
 * This is only used by service implementors.
 *
 * The NetcdfFile class manages all registered IOServiceProvider classes.
 * When NetcdfFile.open() is called:
 * <ol>
 * <li> the file is opened as a ucar.unidata.io.RandomAccessFile;</li>
 * <li> the file is handed to the isValidFile() method of each registered
 * IOServiceProvider class (until one returns true, which means it can read the file).</li>
 * <li> the open() method on the resulting IOServiceProvider class is handed the file.</li>
 *
 * @see ucar.nc2.NetcdfFile#registerIOProvider(Class) ;
 * @see ucar.nc2.iosp.IOServiceProviderWriter;
 *
 * @author caron
 */
public interface IOServiceProvider {

   /**
    * Check if this is a valid file for this IOServiceProvider.
    * You must make this method thread safe, ie dont keep any state.
    * 
    * @param raf RandomAccessFile
    * @return true if valid.
    * @throws java.io.IOException if read error
    */
  public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) throws IOException;

  /**
   * Open existing file, and populate ncfile with it. This method is only called by the
   * NetcdfFile constructor on itself. The provided NetcdfFile object will be empty
   * except for the location String and the IOServiceProvider associated with this
   * NetcdfFile object.
   *
   * @param raf the file to work on, it has already passed the isValidFile() test.
   * @param ncfile add objects to this empty NetcdfFile 
   * @param cancelTask used to monitor user cancellation; may be null.
   * @throws IOException if read error
   */
  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException;

  /**
   * Read data from a top level Variable and return a memory resident Array.
   * This Array has the same element type as the Variable, and the requested shape.
   *
   * @param v2 a top-level Variable
   * @param section the section of data to read.
   *   There must be a Range for each Dimension in the variable, in order.
   *   Note: no nulls allowed. IOSP may not modify.
   * @return the requested data in a memory-resident Array
   * @throws java.io.IOException if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   * @see ucar.ma2.Range
   */
  public ucar.ma2.Array readData(ucar.nc2.Variable v2, Section section)
         throws java.io.IOException, ucar.ma2.InvalidRangeException;

  /**
   * Read data from a top level Variable and send data to a WritableByteChannel.
   * Must be in big-endian order.
   *
   * @param v2 a top-level Variable
   * @param section the section of data to read.
   *   There must be a Range for each Dimension in the variable, in order.
   *   Note: no nulls allowed. IOSP may not modify.
   * @param channel write data to this WritableByteChannel
   * @return the number of bytes written to the channel
   * @throws java.io.IOException if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */
  public long readData(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
         throws java.io.IOException, ucar.ma2.InvalidRangeException;
  
  /*
   * LOOK Should we allow reading on member variables ??
   * Read data from a Variable that is nested in one or more Structures.
   * Return an Array of the same type as the Variable and the requested shape. The shape
   * must be an accumulation of all the shapes of the Structures containing the variable.
   * If there are no Structures in the NetcdfFile, this will never be called, and may return UnsupportdOperationException 
   *
   * <p> v2.getParent() is called to get the containing Structures.
   *
   * @param v2 a nested Variable.
   * @param section the section of data to read. There must be a Range for each
   *  Dimension in each parent, as well as in the Variable itself. Must be in order from outer to inner.
   *   Note: no nulls. IOSP may not modify.
   * @return the requested data in a memory-resident Array
   * @throws java.io.IOException if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   *
  public ucar.ma2.Array readNestedData(ucar.nc2.Variable v2, Section section)
         throws IOException, ucar.ma2.InvalidRangeException; */

  /**
   * Close the file.
   * It is the IOServiceProvider's job to close the file (even though it didnt open it),
   * and to free any other resources it has used.
   * @throws IOException if read error
   */
  public void close() throws IOException;

  /** Extend the file if needed in a way that is compatible with the current metadata.
   *  For example, if the unlimited dimension has grown.
   * @return true if file was extended.
   * @throws IOException if read error
   */
  public boolean syncExtend() throws IOException;

  /** Check if file has changed, and reread metadata if needed.
   * @return true if file was changed.
   * @throws IOException if read error
   */
  public boolean sync() throws IOException;

  /**
   * A way to communicate arbitrary information to an iosp.
   * @param message opaque message.
   * @return opaque return, may be null.
   */
  public Object sendIospMessage( Object message);

  /** Debug info for this object.
   * @param o which object
   * @return debug info for this object
   */
  public String toStringDebug(Object o);

  /** Show debug / underlying implementation details
   * @return debug info
   */
  public String getDetailInfo();

}