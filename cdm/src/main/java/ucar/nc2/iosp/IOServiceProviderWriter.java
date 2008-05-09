/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp;

import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Attribute;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * This is really just an interface to Netcdf-3 file writing.
 * However, we will probably add Netcf-4 writing, exen if its only through a JNI interface.
 * For now, other parties are discourages from using this, as it will likely be refactored in 4.1.
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
