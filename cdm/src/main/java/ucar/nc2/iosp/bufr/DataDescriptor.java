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
package ucar.nc2.iosp.bufr;

import ucar.nc2.iosp.bufr.tables.TableC;
import ucar.nc2.iosp.bufr.tables.TableB;

import java.util.List;

/**
 * Essentially a TableB entry, modified by any relevent TableC operators.
 * TableD has been expanded.
 * Replication gets made into nested DataDesccriptors, which we map to Structures (fixed replication) or
 * Sequences (deferred replication).
 * Most of the processing is done by BufrDataDescriptionSection.convert().
 * Here we encapsolate the final result, ready to map to the CDM.
 *
 * @author caron
 * @since Apr 5, 2008
 */
public class DataDescriptor {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataDescriptor.class);

  ////////////////////////////////

  // from the TableB.Descriptor
  public short fxy;
  public int f, x, y;
  public String name, units;
  public boolean bad;  // no descriptor found

  // may get modified by TableC operators
  public int scale;
  public int refVal;
  public int bitWidth;
  public int type; // 0 = isNumeric, 1 = isString, 2 = isEnum, 3 = compound;

  // replication info
  public List<DataDescriptor> subKeys;
  public int replication = 1; // number of replications, essentially dk.y when sk.f == 1
  public int replicationCountSize; // for delayed replication : size of count in bits
  int repetitionCountSize; // for delayed repetition

  AssociatedField assField; // associated field == 02 04 Y, Y number of extra bits
  public Object refersTo; // temporary place to put a sequence object

  DataDescriptor() {
  }

  DataDescriptor(short fxy, TableLookup lookup) {
    this.fxy = fxy;
    this.f = (fxy & 0xC000) >> 14;
    this.x = (fxy & 0x3F00) >> 8;
    this.y = fxy & 0xFF;

    TableB.Descriptor db = null;
    if (f == 0) {
      db = lookup.getDescriptorTableB( fxy);
      bad = (db == null);
    }
    if (db != null)
      setDescriptor(db);

    if (f == 1) // replication
      this.type = 3; // compound
  }

  private void setDescriptor(TableB.Descriptor d) {
    this.name = d.getName().trim();
    this.units = d.getUnits().trim();
    this.refVal = d.getRefVal();
    this.scale = d.getScale();
    this.bitWidth = d.getWidth();

    if (units.equalsIgnoreCase("CCITT IA5") || units.equalsIgnoreCase("CCITT_IA5")) {
      this.type = 1; // String
    }

    // LOOK what about flag table ??
    if (units.equalsIgnoreCase("Code Table") || units.equalsIgnoreCase("Code_Table")) {
      this.type = 2; // enum
    }
  }

  // for associated fields
  DataDescriptor(DataDescriptor d, int bitWidth) {
    this.name = d.name + "_associated_field";
    this.units = "";
    this.refVal = 0;
    this.scale = 0;
    this.bitWidth = bitWidth;
    this.type = 0;

    this.f = 0;
    this.x = 31;
    this.y = 22;
    this.fxy =  (short) ((f << 14) + (x << 8) + (y));
  }

  static class AssociatedField {
    int nbits;
    int nfields;
    String dataFldName;

    AssociatedField(int nbits) {
      this.nbits = nbits;
    }
  }

  public List<DataDescriptor> getSubKeys() {
    return subKeys;
  }

  public boolean isOkForVariable() {
    return (f == 0) || (f == 1);
  }

  public boolean isLocal() {
    if ((f == 0) || (f == 3)) {
      if ((x >= 48) || (y >= 192))
        return true;
    }
    return false;
  }

  public String getFxyName() {
    return f +"-"+x+"-"+y;
  }

  /**
   * Transfer info from the "proto message" to another message with the exact same structure.
   * @param fromList transfer from here
   * @param toList to here
   */
  static public void transferInfo(List<DataDescriptor> fromList, List<DataDescriptor> toList) { // get info from proto message
    if (fromList.size() != toList.size())
      throw new IllegalArgumentException("list sizes dont match "+fromList.size()+" != "+toList.size());

    for (int i=0; i<fromList.size(); i++) {
      DataDescriptor from = fromList.get(i);
      DataDescriptor to = toList.get(i);
      to.refersTo = from.refersTo;
      to.name = from.name;

      if (from.getSubKeys() != null)
        transferInfo(from.getSubKeys(), to.getSubKeys());
    }
  }


  private int total_nbytesCDM = 0;

  /**
   * count the bits used by the data in this dd and its children
   * only accurate for not compressed, and not variable length
   *
   * @return bits used by the data in the file
   */
  int countBits() {
    int total_nbits = 0;
    total_nbytesCDM = 0;

    for (DataDescriptor dd : subKeys) {
      if (dd.subKeys != null) {
        total_nbits += dd.countBits();
        total_nbytesCDM += dd.total_nbytesCDM;

      } else if (dd.f == 0) {
        total_nbits += dd.bitWidth;
        total_nbytesCDM += dd.getByteWidthCDM();
      }
    }

    // replication
    if (replication > 1) {
      total_nbits *= replication;
      total_nbytesCDM *= replication;
    }

    return total_nbits;
  }

  public int getBitWidth() {
    return bitWidth;
  }

  /**
   * Get the number of bytes the CDM datatype will take
   *
   * @return the number of bytes the CDM datatype will take
   */
  public int getByteWidthCDM() {
    if (type == 1)
      return bitWidth / 8;

    if (type == 3)
      return total_nbytesCDM;

    // numeric or enum
    if (bitWidth < 9) return 1;
    if (bitWidth < 17) return 2;
    return 4;
  }

  public String toString() {
    String id = getFxyName();
    StringBuilder sbuff = new StringBuilder();
    if (f == 0) {
      sbuff.append(getFxyName()).append(": ");
      sbuff.append(name).append(" units=").append(units);
      if (type == 0) {
        sbuff.append(" scale=").append(scale).append(" refVal=").append(refVal);
        sbuff.append(" nbits=").append(bitWidth);
      } else if (type == 1) {
        sbuff.append(" nchars=").append(bitWidth / 8);
      } else {
        sbuff.append(" enum nbits=").append(bitWidth);
      }

    } else if (f == 1) {
      sbuff.append(id).append(": ").append( "Replication");
      if (replication != 1)
        sbuff.append(" count=").append(replication);
      if (replicationCountSize != 0)
        sbuff.append(" replicationCountSize=").append(replicationCountSize);
      if (repetitionCountSize != 0)
        sbuff.append(" repetitionCountSize=").append(repetitionCountSize);

    } else if (f == 2) {
      String desc = TableC.getOperatorName(x);
      if (desc == null) desc = "Operator";
      sbuff.append(id).append(": ").append(desc);

    } else
      sbuff.append(id).append(": ").append( name);

    return sbuff.toString();
  }

  /////////////////////////////////
  // stuff for the root
  boolean isVarLength;
  boolean isBad;
  int total_nbits;

  public int getTotalBits() { return total_nbits; }
  public boolean isVarLength() { return isVarLength; }

}
