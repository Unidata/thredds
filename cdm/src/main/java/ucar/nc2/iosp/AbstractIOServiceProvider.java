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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.iosp;

import ucar.ma2.*;
import ucar.nc2.ParsedSectionSpec;

import java.io.IOException;
import java.io.DataOutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;

public abstract class AbstractIOServiceProvider implements IOServiceProvider {

  // a no-op but leave it in in case we change our minds
  static public String createValidNetcdfObjectName(String name) {
    return name;
  }

  // default implementation, reads into an Array, then writes to WritableByteChannel
  // subclasses should override if possible
  public long readToByteChannel(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    Array result = readData(v2, section);

    // LOOK should we buffer ?? 
    DataOutputStream outStream = new DataOutputStream( Channels.newOutputStream( channel));

    IndexIterator iterA = result.getIndexIterator();
    Class classType = result.getElementType();

    if (classType == double.class) {
      while (iterA.hasNext())
        outStream.writeDouble(iterA.getDoubleNext());

    } else if (classType == float.class) {
      while (iterA.hasNext())
        outStream.writeFloat(iterA.getFloatNext());

    } else if (classType == long.class) {
      while (iterA.hasNext())
        outStream.writeLong(iterA.getLongNext());

    } else if (classType == int.class) {
      while (iterA.hasNext())
        outStream.writeInt(iterA.getIntNext());

    } else if (classType == short.class) {
      while (iterA.hasNext())
        outStream.writeShort(iterA.getShortNext());

    } else if (classType == char.class) {
      while (iterA.hasNext())
        outStream.writeChar(iterA.getCharNext());

    } else if (classType == byte.class) {
      while (iterA.hasNext())
        outStream.writeByte(iterA.getByteNext());

    } else if (classType == boolean.class) {
      while (iterA.hasNext())
        outStream.writeBoolean(iterA.getBooleanNext());

    } else
      throw new UnsupportedOperationException("Class type = " + classType.getName());

    return 0;
  }

  public ucar.ma2.Array readSection(ParsedSectionSpec cer) throws IOException, InvalidRangeException {
    return IospHelper.readSection(cer);  //  IOSPs can optimize by overriding
  }

  public Object sendIospMessage(Object message) {
    return null;
  }

  public boolean syncExtend() throws IOException {
    return false;
  }

  public boolean sync() throws IOException {
    return false;
  }

  public String toStringDebug(Object o) {
    return "";
  }

  public String getDetailInfo() {
    return "";
  }

}
