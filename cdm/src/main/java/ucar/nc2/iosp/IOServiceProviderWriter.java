/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.Structure;

import java.io.IOException;

/**
 * This is really just an interface to Netcdf-3 and Netcdf-4 file writing.
 * For now, other parties are discouraged from using this.
 *
 * @author caron
 */
public interface IOServiceProviderWriter extends IOServiceProvider {

  /**
   * Create new file, populate it from the objects in ncfile.
   *
   * @param filename name of file to create.
   * @param ncfile get everything but data from here
   * @param extra  if > 0, pad header with extra bytes
   * @param preallocateSize if > 0, set length of file to this upon creation - this (usually) pre-allocates contiguous storage.
   * @param largeFile  if want large file format
   * @throws java.io.IOException if I/O error
   */
  void create(String filename, ucar.nc2.NetcdfFile ncfile, int extra, long preallocateSize, boolean largeFile) throws IOException;

  /**
   * Open existing file and allow writing.
   *
   * @param raf the file to work on.
   * @param ncfile add objects to this empty NetcdfFile
   * @param cancelTask used to monitor user cancellation; may be null.
   * @throws IOException if I/O error
   */
  void openForWriting(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException;

  /**
   * Set the fill flag.
   * For new files, set in the create() method. This method is to set fill for existing files that you want to write.
   * If true, the data is first written with fill values.
   * Set to false if you expect to write all data values, set to true if you want to be
   * sure that unwritten data values have the fill value in it.
   *
   * @param fill set fill mode true or false
   */
  void setFill(boolean fill);

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
  void writeData(ucar.nc2.Variable v2, Section section, ucar.ma2.Array values)
      throws IOException, ucar.ma2.InvalidRangeException;

  /**
   * Append a structureData along the unlimited dimension
   *
   * @param s     belongs to this structure
   * @param sdata the stuctureData to append
   * @return      the recnum where it was written
   * @throws IOException
   * @throws InvalidRangeException
   */
  int appendStructureData(Structure s, StructureData sdata) throws IOException, InvalidRangeException;


  /**
   * if theres room before data, rewrite header without moving the data. netcdf3 only
   * @return true if it worked
   * @throws IOException
   */
  boolean rewriteHeader(boolean largeFile) throws IOException;

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
  void updateAttribute(ucar.nc2.Variable v2, Attribute att) throws IOException;

  /**
   * Flush all data buffers to disk.
   * @throws IOException if I/O error
   */
  void flush() throws IOException;

}
