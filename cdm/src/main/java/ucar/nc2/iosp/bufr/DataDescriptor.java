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
package ucar.nc2.iosp.bufr;

import ucar.nc2.iosp.bufr.tables.TableC;
import ucar.nc2.iosp.bufr.tables.TableB;

import java.util.List;

/**
 * Essentially a TableB entry, modified by any relevent TableC operators.
 * TableD has been expanded.
 * Replication gets made into nested DataDesccriptors, which we map to Structures (fixed replication) or
 * Sequences (deferred replication).
 * Most of the processing is done by DataDescriptorTreeConstructor.convert().
 * Here we encapsolate the final result, ready to map to the CDM.
 *
 * @author caron
 * @since Apr 5, 2008
 */
public class DataDescriptor {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataDescriptor.class);

  ////////////////////////////////

  // from the TableB.Descriptor
  short fxy;
  int f, x, y;
  String name, units;
  boolean bad;  // no descriptor found
  boolean localOverride;

  // may get modified by TableC operators
  int scale;
  int refVal;
  int bitWidth;
  int type; // 0 = isNumeric, 1 = isString, 2 = isEnum, 3 = compound;

  // replication info
  List<DataDescriptor> subKeys;
  int replication = 1; // number of replications, essentially dk.y when sk.f == 1
  int replicationCountSize; // for delayed replication : size of count in bits
  int repetitionCountSize; // for delayed repetition

  AssociatedField assField; // associated field == 02 04 Y, Y number of extra bits
  Object refersTo; // temporary place to put a sequence object
  DataDescriptorTreeConstructor.DataPresentIndicator dpi;

  DataDescriptor() {
  }

  public DataDescriptor(short fxy, TableLookup lookup) {
    this.fxy = fxy;
    this.f = (fxy & 0xC000) >> 14;
    this.x = (fxy & 0x3F00) >> 8;
    this.y = fxy & 0xFF;

    TableB.Descriptor db = null;
    if (f == 0) {
      db = lookup.getDescriptorTableB(fxy);
      if (db != null)
        setDescriptor(db);
      else {
        bad = true;
        if (f != 1) this.name = "*NOT FOUND";
      }
    }

    else if (f == 1) // replication
      this.type = 3; // compound

    else if (f == 2) {
      this.name = TableC.getOperatorName(x);
    }
  }

  private void setDescriptor(TableB.Descriptor d) {
    this.name = d.getName().trim();
    this.units = d.getUnits().trim();
    this.refVal = d.getRefVal();
    this.scale = d.getScale();
    this.bitWidth = d.getDataWidth();
    this.localOverride = d.getLocalOverride();

    if (units.equalsIgnoreCase("CCITT IA5") || units.equalsIgnoreCase("CCITT_IA5")) {
      this.type = 1; // String
      //this.bitWidth *= 8;
    }

    // LOOK what about flag table ??
    if (units.equalsIgnoreCase("Code Table") || units.equalsIgnoreCase("Code_Table")) {
      this.type = 2; // enum
    }
  }

  /* for dpi fields
  DataDescriptor makeStatField(String statType) {
    DataDescriptor statDD = new DataDescriptor();
    statDD.name = name + "_" + statType;
    statDD.units = units;
    statDD.refVal = 0;

    return statDD;
  } */

  // for associated fields
  DataDescriptor makeAssociatedField(int bitWidth) {
    DataDescriptor assDD = new DataDescriptor();
    assDD.name = name + "_associated_field";
    assDD.units = "";
    assDD.refVal = 0;
    assDD.scale = 0;
    assDD.bitWidth = bitWidth;
    assDD.type = 0;

    assDD.f = 0;
    assDD.x = 31;
    assDD.y = 22;
    assDD.fxy =  (short) ((f << 14) + (x << 8) + (y));

    return assDD;
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
    return (f == 0) || (f == 1) || ((f == 2) && (x == 5) || ((f == 2) && (x == 24) && (y == 255)));
  }

  public boolean isLocal() {
    if ((f == 0) || (f == 3)) {
      if ((x >= 48) || (y >= 192))
        return true;
    }
    return false;
  }

  public boolean isLocalOverride() {
    return localOverride;
  }

  public String getFxyName() {
    return f +"-"+x+"-"+y;
  }


  public short getFxy() {
    return fxy;
  }

  public String getName() {
    return name;
  }

  public int getType() {
    return type;
  }

  public int getScale() {
    return scale;
  }

  public int getRefVal() {
    return refVal;
  }

  public String getUnits() {
    return units;
  }

  public float convert( long raw) {
    if ( BufrNumbers.isMissing(raw, bitWidth)) return Float.NaN;

    // bpacked = (value * 10^scale - refVal)
    // value = (bpacked + refVal) / 10^scale
    float fscale = (float) Math.pow(10.0, -scale); // LOOK precompute ??
    float fval = (raw + refVal);
    return fscale * fval;
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

  ///////////////////////////////////////////////////////////////////////////////////////////////
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
    if (type == 1) // string
      return bitWidth / 8;

    if (type == 3) // compound
      return total_nbytesCDM;

    // numeric or enum
    if (bitWidth < 9) return 1;
    if (bitWidth < 17) return 2;
    if (bitWidth < 33) return 4;
    return 8;
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
