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
package ucar.nc2.iosp.hdf4;

import ucar.nc2.iosp.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.*;

import java.io.IOException;

/**
 * @author caron
 * @since Dec 17, 2007
 */
public class H4iosp extends AbstractIOServiceProvider {
  private RandomAccessFile raf;
  private H4header header = new H4header();

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    return H4header.isValidFile(raf);
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    header.read(raf, ncfile);
  }

  public Array readData(Variable v, Section section) throws IOException, InvalidRangeException {
    if (v instanceof Structure)
      return readStructureData((Structure) v, section);

    H4header.Vinfo vinfo = (H4header.Vinfo) v.getSPobject();
    DataType dataType = v.getDataType();

    if (!vinfo.isLinked && !vinfo.isCompressed) {
      Indexer index = RegularLayout.factory(vinfo.start, v.getElementSize(), -1, v.getShape(), section);
      Object data = readData(raf, index, dataType);
      return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), data);

    } else if (vinfo.isLinked && !vinfo.isCompressed) {
      Indexer index = new SegmentedLayout(vinfo.segPos, vinfo.segSize, v.getElementSize(), v.getShape(), section);
      Object data = readData(raf, index, dataType);
      return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), data);

    } else if (vinfo.isLinked && vinfo.isCompressed) {
      Indexer index = new SegmentedLayout(vinfo.segPos, vinfo.segSize, v.getElementSize(), v.getShape(), section);
      Object data = readData(raf, index, dataType);
      return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), data);
    }

    throw new IllegalStateException();
  }

  /**
   * Read data from record structure. For N3, this is the only possible structure, and there can be no nesting.
   * Read all variables for each record, for efficiency.
   *
   * @param s       the record structure
   * @param section the record range to read
   * @return an ArrayStructure, with all the data read in.
   * @throws IOException on error
   */
  private ucar.ma2.Array readStructureData(ucar.nc2.Structure s, Section section) throws java.io.IOException {
    H4header.Vinfo vinfo = (H4header.Vinfo) s.getSPobject();
    int recsize = vinfo.recsize;
    int start = vinfo.start;

    // may be a scalar or 1D
    Range recordRange = (s.getRank() > 0) ? section.getRange(0) : new Range(1);

    // create the ArrayStructure
    StructureMembers members = s.makeStructureMembers();
    for (StructureMembers.Member m : members.getMembers()) {
      Variable v2 = s.findVariable(m.getName());
      H4header.Minfo minfo = (H4header.Minfo) v2.getSPobject();
      m.setDataParam((int) (minfo.offset));
    }

    members.setStructureSize(recsize);
    ArrayStructureBB structureArray = new ArrayStructureBB(members, new int[]{recordRange.length()});

    // loop over records
    byte[] result = (byte[]) structureArray.getStorage();
    int count = 0;
    for (int recnum = recordRange.first(); recnum <= recordRange.last(); recnum += recordRange.stride()) {
      raf.seek(start + recnum * recsize); // where the record starts
      raf.readFully(result, count * recsize, recsize);
      count++;
    }

    return structureArray;
  }

  public void close() throws IOException {
    raf.close();
  }

  public String toStringDebug(Object o) {
    if (o instanceof Variable) {
      Variable v = (Variable) o;
      H4header.Vinfo vinfo = (H4header.Vinfo) v.getSPobject();
      return vinfo.toString();
    }
    return null;
  }
}
