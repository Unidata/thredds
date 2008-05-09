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
import ucar.nc2.CEresult;
import ucar.nc2.Variable;
import ucar.nc2.Structure;

import java.io.IOException;
import java.io.DataOutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.util.List;
import java.util.ArrayList;

public abstract class AbstractIOServiceProvider implements IOServiceProvider {

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

  public ucar.ma2.Array readSection(CEresult cer) throws IOException, InvalidRangeException {    
    if (cer.child == null)
      return cer.v.read(cer.section);

    Variable inner = null;
    List<Range> totalRanges = new ArrayList<Range>();
    CEresult current = cer;
    while (current != null) {
      totalRanges.addAll( current.section.getRanges());
      inner = current.v;
      current = current.child;
    }

    Section total = new Section( totalRanges);
    Array result = Array.factory(inner.getDataType(), total.getShape());

    // must be a Structure
    Structure outer = (Structure) cer.v;
    Structure outerSubset = outer.select( cer.child.v); // allows IOSPs to optimize for  this case
    ArrayStructure outerData = (ArrayStructure) outerSubset.read(cer.section);
    extractSection( cer.child, outerData, result.getIndexIterator());
    return result;
  }

  private void extractSection(CEresult child, ArrayStructure outerData, IndexIterator to) throws IOException, InvalidRangeException {
    long wantNelems = child.section.computeSize();

    StructureMembers.Member m = outerData.findMember( child.v.getShortName());
    for (int recno = 0; recno < outerData.getSize(); recno++) {
      Array innerData = outerData.getArray(recno, m);

      if (child.child == null) {  // inner variable

        if (wantNelems != innerData.getSize())
          innerData = innerData.section(child.section.getRanges());
        MAMath.copy(child.v.getDataType(), innerData.getIndexIterator(), to);

      } else {                   // not an inner variable - must be an ArrayStructure
        if (wantNelems != innerData.getSize())
          innerData = sectionArrayStructure(child, (ArrayStructure) innerData, m);
        extractSection(child.child, (ArrayStructure) innerData, to);
      }
    }
  }

  // LOOK could be used in createView ??
  private ArrayStructure sectionArrayStructure(CEresult child, ArrayStructure innerData, StructureMembers.Member m) throws IOException, InvalidRangeException {
    StructureMembers membersw = new StructureMembers(m.getStructureMembers()); // no data arrays get propagated
    ArrayStructureW result = new ArrayStructureW(membersw, child.section.getShape());

    int count =0;
    Section.Iterator iter = child.section.getIterator( child.v.getShape());
    while (iter.hasNext()) {
      int recno = iter.next();
      StructureData sd = innerData.getStructureData(recno);
      result.setStructureData(sd, count++);
    }

    return result;
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
