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
package ucar.nc2.iosp;

import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Attribute;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * This is really just an interface to Netcdf-3 file writing.
 * However, we will probably add Netcdf-4 writing, even if its only through a JNI interface.
 * For now, other parties are discouraged from using this, as it will likely be refactored in 4.x.
 *
 * @author caron
 */
public interface IOServiceProviderWriter extends IOServiceProvider {

  /**
   * Create new file, populate it from the objects in ncfile.
   *
   * @param filename name of file to create.
   * @param ncfile get dimensions, attributes, and variables from here.
   * @param extra  if > 0, pad header with extra bytes
   * @param preallocateSize if > 0, set length of file to this upon creation - this (usually) pre-allocates contiguous storage.
   * @param largeFile  if want large file format
   * @throws java.io.IOException if I/O error
   */
  public void create(String filename, ucar.nc2.NetcdfFile ncfile, int extra, long preallocateSize, boolean largeFile) throws IOException;

  /**
   * Set the fill flag.
   * For new files, set in the create() method. This method is to set fill for existing files that you want to write.
   * If true, the data is first written with fill values.
   * Leave false if you expect to write all data values, set to true if you want to be
   * sure that unwritten data values have the fill value in it.
   *
   * @param fill set fill mode true or false
   */
  public void setFill(boolean fill);

  /**
   * Write data into a variable.
   * @param v2 variable to write; must already exist.
   * @param section the section of data to write.
   *  There must be a Range for each Dimension in the variable, in order.
   *  The shape must match the shape of values.
   *  The origin and stride indicate where the data is placed into the stored Variable array.
   * @param values data to write. The shape must match section.getShape().
   * @throws IOException if I/O error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */
  public void writeData(ucar.nc2.Variable v2, Section section, ucar.ma2.Array values)
      throws IOException, ucar.ma2.InvalidRangeException;

  public boolean rewriteHeader(boolean largeFile) throws IOException;  

  /**
   * Update the value of an existing attribute. Attribute is found by name, which must match exactly.
   * You cannot make an attribute longer, or change the number of values.
   * For strings: truncate if longer, zero fill if shorter.  Strings are padded to 4 byte boundaries, ok to use padding if it exists.
   * For numerics: must have same number of values.
   *
   * @param v2 variable, or null for global attribute
   * @param att replace with this value
   * @throws IOException if I/O error
   */
  public void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException;

  /**
   * Flush all data buffers to disk.
   * @throws IOException if I/O error
   */
  public void flush() throws IOException;

}
