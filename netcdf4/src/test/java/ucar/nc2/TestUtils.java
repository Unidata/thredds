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
package ucar.nc2;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.CDM;

/**
 * Static utililities for testing
 *
 * @author Russ Rew
 */

public class TestUtils {

  /**
   * Clone metadata from the given netcdf to the given netcdf file writer.
   * 
   * @param fromNetcdf the cloned netcdf
   * @param toWriter the netcdf file writer
   */
  public static void cloneMetadata(NetcdfFile fromNetcdf, NetcdfFileWriter toWriter) {

    final Group rootFrom = fromNetcdf.getRootGroup();
    final Group rootTo = toWriter.addGroup(null, null);

    cloneMetadata(toWriter, rootFrom, rootTo);
  }

  /**
   * Clone data from the given netcdf to the given netcdf file writer.
   * 
   * @param fromNetcdf the cloned netcdf
   * @param toWriter the netcdf file writer
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static void cloneData(NetcdfFile fromNetcdf, NetcdfFileWriter toWriter)
      throws IOException, InvalidRangeException {

    final Group rootFrom = fromNetcdf.getRootGroup();
    final Group rootTo = toWriter.getNetcdfFile().getRootGroup();

    cloneData(toWriter, rootFrom, rootTo);

  }

  /**
   * Clone metadata from the given group to the given group of the netcdf file writer.
   * 
   * @param to the netcdf file writer
   * @param groupFrom the cloned metadata group
   * @param groupTo the written group
   */
  private static void cloneMetadata(final NetcdfFileWriter to, final Group groupFrom, final Group groupTo) {

    // dimension
    cloneMetadataDimension(to, groupFrom, groupTo);

    // attribute
    cloneMetadataAttribute(to, groupFrom, groupTo);

    // variable
    cloneMetadataVariable(to, groupFrom, groupTo);

    // enum
    cloneMetadataEnum(to, groupFrom, groupTo);

    for (final Group gFrom : groupFrom.getGroups()) {
      // create new Group
      final Group g = to.addGroup(groupTo, gFrom.getShortName());
      cloneMetadata(to, gFrom, g);

    }
  }

  /**
   * Clone metadata dimension.
   *
   * @param to the netcdf file writer
   * @param groupFrom the cloned metadata dimension group
   * @param groupTo the written group
   */
  private static void cloneMetadataDimension(final NetcdfFileWriter to, final Group groupFrom, final Group groupTo) {
    for (final Dimension dimension : groupFrom.getDimensions()) {
      if (!dimension.isVariableLength()) {
        to.addDimension(groupTo, dimension.getShortName(), dimension.getLength(), dimension.isShared(),
            dimension.isUnlimited(), dimension.isVariableLength());
      }
    }
  }

  /**
   * Clone metadata attribute.
   *
   * @param to the netcdf file writer
   * @param groupFrom the cloned metadata attribute group
   * @param groupTo the written group
   */
  private static void cloneMetadataAttribute(final NetcdfFileWriter to, final Group groupFrom, final Group groupTo) {
    for (final Attribute attribute : groupFrom.getAttributes()) {
      final Attribute att = new Attribute(attribute.getShortName(), attribute);
      to.addGroupAttribute(groupTo, att);
    }
  }

  /**
   * Clone metadata variable.
   *
   * @param to the netcdf file writer
   * @param groupFrom the cloned metadata variable group
   * @param groupTo the written group
   */
  private static void cloneMetadataVariable(final NetcdfFileWriter to, final Group groupFrom, final Group groupTo) {
    for (final Variable variable : groupFrom.getVariables()) {

      final String parsedDim = variable.getDimensionsString();
      final Variable v = to.addVariable(groupTo, variable.getShortName(), variable.getDataType(), parsedDim);

      for (final Attribute attribute : variable.getAttributes()) {
        final Attribute att = new Attribute(attribute.getShortName(), attribute);
        v.addAttribute(att);
      }
    }

  }

  /**
   * Clone metadata enum.
   *
   * @param to the netcdf file writer
   * @param groupFrom the cloned metadata enum group
   * @param groupTo the written group
   */
  private static void cloneMetadataEnum(final NetcdfFileWriter to, final Group groupFrom, final Group groupTo) {
    for (final EnumTypedef enumTypedef : groupFrom.getEnumTypedefs()) {
      final EnumTypedef e =
          new EnumTypedef(enumTypedef.getShortName(), new HashMap<>(enumTypedef.getMap()), enumTypedef.getBaseType());
      to.addTypedef(groupTo, e);
    }
  }

  /**
   * Clone data.
   * 
   * @param to the netcdf file writer
   * @param groupFrom the cloned metadata enum group
   * @param groupTo the written group
   * @throws IOException
   * @throws InvalidRangeException
   */
  private static void cloneData(final NetcdfFileWriter to, final Group groupFrom, final Group groupTo)
      throws IOException, InvalidRangeException {
    if (null != groupTo) {
      for (final Variable variableFrom : groupFrom.getVariables()) {

        final Variable variableTo = groupTo.findVariable(variableFrom.getShortName());
        if (null != variableTo) {
          to.write(variableTo, variableFrom.read().copy());
        }
      }

      for (final Group gFrom : groupFrom.getGroups()) {
        final Group gTo = groupTo.findGroup(gFrom.getShortName());
        cloneData(to, gFrom, gTo);
      }
    }
  }
}
