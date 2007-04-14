// $Id:IOServiceProviderWriter.java 51 2006-07-12 17:13:13Z caron $
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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import java.io.IOException;

/**
 * This is the service provider interface for the low-level I/O writing.
 * This is only used by service implementors.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
public interface IOServiceProviderWriter extends IOServiceProvider {

    /**
   * Create new file, populate it from the objects in ncfile.
   * @param filename name of file to create.
   * @param ncfile get dimensions, attributes, and variables from here.
   * @param fill if true, write fill value into all variables.
   * @throws java.io.IOException
   */
  public void create(String filename, ucar.nc2.NetcdfFile ncfile, boolean fill) throws IOException;

  /**
   * Write data into a variable.
   * @param v2 variable to write; must already exist.
   * @param section List of type Range specifying the section of data to write.
   * There must be a Range for each Dimension in the variable, in order.
   * The shape must match the shape of values.
   * @param values data to write. The shape must match the shape of Range list.
   * @throws IOException
   */
  public void writeData(ucar.nc2.Variable v2, java.util.List section, ucar.ma2.Array values)
      throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * Update the value of an existing attribute. Attribute is found by name, which must match exactly.
   * You cannot make an attribute longer, or change the number of values.
   * For strings: truncate if longer, zero fill if shorter.  Strings are padded to 4 byte boundaries, ok to use padding if it exists.
   * For numerics: must have same number of values.
   *
   * @param v2 variable, or null for global attribute
   * @param att replace with this value
   * @throws IOException
   */
  public void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException;

  /**
   * Flush all data buffers to disk.
   * @throws IOException
   */
  public void flush() throws IOException;

}
