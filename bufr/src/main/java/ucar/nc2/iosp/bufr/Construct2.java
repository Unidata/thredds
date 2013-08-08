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

import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.bufr.tables.CodeFlagTables;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

/**
 * BufrIosp2 delegates the construction of the Netcdf objects to Construct2.
 *
 * @author caron
 */

class Construct2 {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Construct2.class);
  static private final boolean warnUnits = false;

  private ucar.nc2.NetcdfFile ncfile;

  Sequence recordStructure;
  private Message proto;

  Construct2(Message proto, BufrConfig bufrConfig, ucar.nc2.NetcdfFile nc) throws IOException {
    this.proto = proto;
    this.ncfile = nc;

    //dkeyRoot = dds.getDescriptorRoot();
    //int nbits = dds.getTotalBits();
    //int inputBytes = (nbits % 8 == 0) ? nbits / 8 : nbits / 8 + 1;
    //int outputBytes = dds.getTotalBytes();

    // the category
    int cat = proto.ids.getCategory();
    int subcat = proto.ids.getSubCategory();

    // global Attributes
    ncfile.addAttribute(null, new Attribute(CDM.HISTORY, "Direct read of BUFR data by CDM"));
    if (nc.getLocation() != null)
      ncfile.addAttribute(null, new Attribute("location", nc.getLocation()));
    ncfile.addAttribute(null, new Attribute("BUFR:edition", proto.is.getBufrEdition()));
    ncfile.addAttribute(null, new Attribute("BUFR:categoryName", proto.getCategoryName()));
    ncfile.addAttribute(null, new Attribute("BUFR:category", cat));
    ncfile.addAttribute(null, new Attribute("BUFR:subCategory", subcat));
    ncfile.addAttribute(null, new Attribute("BUFR:localSubCategory", proto.ids.getLocalSubCategory()));
    ncfile.addAttribute(null, new Attribute("BUFR:centerName", proto.getCenterName()));
    ncfile.addAttribute(null, new Attribute("BUFR:center", proto.ids.getCenterId()));
    ncfile.addAttribute(null, new Attribute("BUFR:subCenter", proto.ids.getSubCenterId()));
    //ncfile.addAttribute(null, new Attribute("BUFR:tableName", proto.ids.getMasterTableFilename()));
    ncfile.addAttribute(null, new Attribute("BUFR:table", proto.ids.getMasterTableId()));
    ncfile.addAttribute(null, new Attribute("BUFR:tableVersion", proto.ids.getMasterTableVersion()));
    ncfile.addAttribute(null, new Attribute("BUFR:localTableVersion", proto.ids.getLocalTableVersion()));

    String header = proto.getHeader();
    if (header != null)
      ncfile.addAttribute(null, new Attribute("WMO Header", header));
    ncfile.addAttribute(null, new Attribute("Conventions", "BUFR/CDM"));

    makeObsRecord(bufrConfig);

    ncfile.finish();
  }

   private void makeObsRecord(BufrConfig bufrConfig) throws IOException {
    recordStructure = new Sequence(ncfile, null, null, BufrIosp.obsRecord);
    ncfile.addVariable(null, recordStructure);

    BufrConfig.FieldConverter root = bufrConfig.getRootConverter();
    for (BufrConfig.FieldConverter fld : root.flds) {
      if (fld.getAction() == BufrConfig.Action.remove) continue;

      DataDescriptor dkey = fld.dds;
      if (!dkey.isOkForVariable()) continue;

      if (fld.isSeq) {
        addSequence(recordStructure, fld);

      } else if (dkey.replication > 1) {

        List<BufrConfig.FieldConverter>
        List<DataDescriptor> subKeys = dkey.subKeys;
        if (subKeys.size() == 1) {  // only one member
          DataDescriptor sub = dkey.subKeys.get(0);
          if (sub.dpi != null) {
            addDpiStructure(recordStructure, fld, sub);

          } else if (sub.replication == 1) { // one member not a replication
            Variable v = addVariable(recordStructure, sub, dkey.replication);
            v.setSPobject(dkey); // set the replicating dkey as SPI object

          } else { // one member is a replication (two replications in a row)
            addStructure(recordStructure, fld, dkey.replication);
          }
        } else if (subKeys.size() > 1) {
          addStructure(recordStructure, fld, dkey.replication);
        }

      } else { // replication == 1
        addVariable(recordStructure, fld, dkey.replication);
      }
    }
  }

  private int structNum = 1;
  private void addStructure(Structure parent, BufrConfig.FieldConverter fld, int count) {
    String uname = findUniqueName(parent, fld.getName(), "struct");
    dataDesc.name = uname; // name may need to be changed for uniqueness

    //String structName = dataDesc.name != null ? dataDesc.name : "struct" + structNum++;
    Structure struct = new Structure(ncfile, null, parent, uname);
    try {
      struct.setDimensionsAnonymous(new int[]{count}); // anon vector
    } catch (InvalidRangeException e) {
      log.error("illegal count= " + count + " for " + fld);
    }

    for (BufrConfig.FieldConverter subKey : fld.flds)
      addMember(struct, subKey);

    parent.addMemberVariable(struct);
    struct.setSPobject(fld);

    dataDesc.refersTo = struct;
  }

  private int seqNum = 1;
  private void addSequence(Structure parent, BufrConfig.FieldConverter fld) {
    if (fld.getAction() == BufrConfig.Action.asMissing) {
      // add members
      addVariable(recordStructure, fld, dkey.replication);
    }

    String uname = findUniqueName(parent, fld.getName(), "seq");
    dataDesc.name = uname; // name may need to be changed for uniqueness

    //String seqName = ftype == (FeatureType.STATION_PROFILE) ? "profile" : "seq";
    //String seqName = dataDesc.name != null ? dataDesc.name : "seq" + seqNum++;

    Sequence seq = new Sequence(ncfile, null, parent, uname);
    seq.setDimensions(""); // scalar

    for (BufrConfig.FieldConverter subKey : fld.flds)
      addMember(seq, subKey);

    parent.addMemberVariable(seq);
    seq.setSPobject(fld);

    dataDesc.refersTo = seq;
  }

  private void addMember(Structure parent, BufrConfig.FieldConverter fld) {
    if (fld.isSeq)
      addSequence(parent, fld);

    else if (dkey.replication > 1) {
      List<DataDescriptor> subKeys = dkey.subKeys;
      if (subKeys.size() == 1) {
        DataDescriptor sub = dkey.subKeys.get(0);
        Variable v = addVariable(parent, sub, dkey.replication);
        v.setSPobject(dkey); // set the replicating dkey as SPI object

      } else {
        addStructure(parent, dkey, dkey.replication);
      }

    } else {
      addVariable(parent, dkey, dkey.replication);
    }
  }

  private void addDpiStructure(Structure parent, BufrConfig.FieldConverter parentFld, BufrConfig.FieldConverter dpiField) {
    String uname = findUniqueName(parent, dpiField.getName(), "struct");
    dpiField.name = uname; // name may need to be changed for uniqueness

    //String structName = findUnique(parent, dpiField.name);
    Structure struct = new Structure(ncfile, null, parent, uname);
    int n = parentDD.replication;
    try {
      struct.setDimensionsAnonymous(new int[]{n}); // anon vector
    } catch (InvalidRangeException e) {
      log.error("illegal count= " + 1 + " for " + dpiField);
    }

    Variable v = new Variable(ncfile, null, struct, "name");
    v.setDataType(DataType.STRING); // scalar
    v.setDimensions(""); // scalar
    struct.addMemberVariable(v);

    v = new Variable(ncfile, null, struct, "data");
    v.setDataType(DataType.FLOAT); // scalar
    v.setDimensions(""); // scalar
    struct.addMemberVariable(v);

    parent.addMemberVariable(struct);
    struct.setSPobject(dpiField);  // ??

    dpiField.refersTo = struct;

    // add some fake dkeys corresponding to above
    // DataDescriptor nameDD = new DataDescriptor();
  }

  private void addDpiSequence(Structure parent, BufrConfig.FieldConverter fld) {
    Structure struct = new Structure(ncfile, null, parent, "statistics");
    try {
      struct.setDimensionsAnonymous(new int[] {dataDesc.replication}); // scalar
    } catch (InvalidRangeException e) {
      e.printStackTrace();  
    }
    Variable v = new Variable(ncfile, null, struct, "name");
    v.setDataType(DataType.STRING); // scalar
    v.setDimensions(""); // scalar
    struct.addMemberVariable(v);

    v = new Variable(ncfile, null, struct, "data");
    v.setDataType(DataType.FLOAT); // scalar
    v.setDimensions(""); // scalar
    struct.addMemberVariable(v);

    parent.addMemberVariable(struct);    
  }

  private Variable addVariable(Structure struct, BufrConfig.FieldConverter fld, int count) {
    String uname = findUniqueName(struct, fld.getName(), "unknown");
    dataDesc.name = uname; // name may need to be changed for uniqueness

    Variable v = new Variable(ncfile, null, struct, uname);
    try {
      if (count > 1)
        v.setDimensionsAnonymous(new int[]{count}); // anon vector
      else
        v.setDimensions(""); // scalar
    } catch (InvalidRangeException e) {
      log.error("illegal count= " + count + " for " + fld);
    }

    if (fld.getDesc() != null)
        v.addAttribute(new Attribute("long_name", fld.getDesc()));

    if (fld.getUnits() == null) {
      if (warnUnits) log.warn("dataDesc.units == null for " + uname);
    } else {
      String units = fld.getUnits();
      if (units.equalsIgnoreCase("Code_Table") || units.equalsIgnoreCase("Code Table"))
        v.addAttribute(new Attribute(CDM.UNITS, "CodeTable " + fld.dds.getFxyName()));
      else if (units.equalsIgnoreCase("Flag_Table") || units.equalsIgnoreCase("Flag Table"))
        v.addAttribute(new Attribute(CDM.UNITS, "FlagTable " + fld.dds.getFxyName()));
      else if (!units.startsWith("CCITT") && !units.startsWith("Numeric"))
        v.addAttribute(new Attribute(CDM.UNITS, units));
    }

    DataDescriptor dataDesc = fld.dds;
    if (dataDesc.type == 1) {
      v.setDataType(DataType.CHAR);
      int size = dataDesc.bitWidth / 8;
      try {
        v.setDimensionsAnonymous(new int[]{size});
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }

    } else if ((dataDesc.type == 2) && CodeFlagTables.hasTable(dataDesc.fxy)) {  // enum
      int nbits = dataDesc.bitWidth;
      int nbytes = (nbits % 8 == 0) ? nbits / 8 : nbits / 8 + 1;

      CodeFlagTables ct = CodeFlagTables.getTable(dataDesc.fxy);
      if (nbytes == 1)
        v.setDataType(DataType.ENUM1);
      else if (nbytes == 2)
        v.setDataType(DataType.ENUM2);
      else if (nbytes == 4)
        v.setDataType(DataType.ENUM4);

      //v.removeAttribute(CDM.UNITS);
      v.addAttribute(new Attribute("BUFR:CodeTable", ct.getName() + " (" + dataDesc.getFxyName() + ")"));

      Group g = struct.getParentGroup();
      EnumTypedef enumTypedef = g.findEnumeration(ct.getName());
      if (enumTypedef == null) {
        enumTypedef = new EnumTypedef(ct.getName(), ct.getMap());
        g.addEnumeration(enumTypedef);
      }
      v.setEnumTypedef(enumTypedef);

    } else {
      int nbits = dataDesc.bitWidth;
      // use of unsigend seems fishy, since only ime it uses high bit is for missing
      // not necessarily true, just when they "add one bit" to deal with missing case
      if (nbits < 9) {
        v.setDataType(DataType.BYTE);
        if (nbits == 8) {
          v.addAttribute(new Attribute("_Unsigned", "true"));
          v.addAttribute(new Attribute("missing_value", (short) BufrNumbers.missingValue(nbits)));
        } else
          v.addAttribute(new Attribute("missing_value", (byte) BufrNumbers.missingValue(nbits)));

      } else if (nbits < 17) {
        v.setDataType(DataType.SHORT);
        if (nbits == 16) {
          v.addAttribute(new Attribute("_Unsigned", "true"));
          v.addAttribute(new Attribute("missing_value", BufrNumbers.missingValue(nbits)));
        } else
          v.addAttribute(new Attribute("missing_value", (short) BufrNumbers.missingValue(nbits)));

      } else if (nbits < 33) {
        v.setDataType(DataType.INT);
        if (nbits == 32) {
          v.addAttribute(new Attribute("_Unsigned", "true"));
          v.addAttribute(new Attribute("missing_value", (int) BufrNumbers.missingValue(nbits)));
        } else
          v.addAttribute(new Attribute("missing_value", BufrNumbers.missingValue(nbits)));

      } else  {
        v.setDataType(DataType.LONG);
        v.addAttribute(new Attribute("missing_value", BufrNumbers.missingValue(nbits)));
      }

      // value = scale_factor * packed + add_offset
      // bpacked = (value * 10^scale - refVal)
      // (bpacked + refVal) / 10^scale = value
      // value = bpacked * 10^-scale + refVal * 10^-scale
      // scale_factor = 10^-scale
      // add_ofset =  refVal * 10^-scale
      int scale10 = dataDesc.scale;
      double scale = (scale10 == 0) ? 1.0 : Math.pow(10.0, -scale10);
      if (scale10 != 0)
        v.addAttribute(new Attribute("scale_factor", (float) scale));
      if (dataDesc.refVal != 0)
        v.addAttribute(new Attribute("add_offset", (float) scale * dataDesc.refVal));

    }

    annotate(v, fld);
    v.addAttribute(new Attribute("BUFR:TableB_descriptor", dataDesc.getFxyName()));
    v.addAttribute(new Attribute("BUFR:bitWidth", dataDesc.bitWidth));
    struct.addMemberVariable(v);

    v.setSPobject(fld);
    return v;
  }


  private int tempNo = 1;
  private String findUniqueName(Structure struct, String want, String def) {
    if (want == null) return def + tempNo++;

    String vwant = NetcdfFile.makeValidCdmObjectName(want);
    Variable oldV = struct.findVariable(vwant);
    if (oldV == null) return vwant;

    int seq = 2;
    while (true) {
      String wantSeq = vwant + "-" + seq;
      oldV = struct.findVariable(wantSeq);
      if (oldV == null) return wantSeq;
      seq++;
    }
  }


  private void annotate(Variable v, BufrConfig.FieldConverter fld) {
    if (fld.type == null) return;

    switch (fld.type) {
      case lat:
        v.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
        break;

      case lon:
        v.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
        break;

      case height:
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
        break;

      case heightAboveStation:
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));  // LOOK wrong
        break;

      case stationId:
        v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.STATION_ID));
        break;

      case wmoId:
        v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.STATION_WMOID));
        break;
    }

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // LOOK where does this go ??

  boolean isTimeOk() {
    return isTimeOk;
  }

  private boolean isTimeOk;
  private String yearName, monthName, dayName, hourName, minName, secName, doyName;
  private CalendarDateUnit dateUnit;

  // determine if the fields exist to create time coordinate
  private boolean hasTime() throws IOException {

    DataDescriptor root = proto.getRootDataDescriptor();
    for (DataDescriptor dkey : root.subKeys) {
      if (!dkey.isOkForVariable()) continue;

      String key = dkey.getFxyName();
      if (key.equals("0-4-1") && (yearName == null))
        yearName = dkey.name;
      if (key.equals("0-4-2") && (monthName == null))
        monthName = dkey.name;
      if (key.equals("0-4-3") && (dayName == null))
        dayName = dkey.name;
      if (key.equals("0-4-43") && (doyName == null))
        doyName = dkey.name;
      if (key.equals("0-4-4") && (hourName == null))
        hourName = dkey.name;
      if (key.equals("0-4-5") && (minName == null))
        minName = dkey.name;
      if ((key.equals("0-4-6") || key.equals("0-4-7")) && (secName == null))
        secName = dkey.name;
    }

    boolean hasTime = (yearName != null) && (((monthName != null) && (dayName != null)) || (doyName != null)) && (hourName != null);

    if (hasTime) {
      String u;
      if (secName != null)
        u = "secs";
      else if (minName != null)
        u = "minutes";
      else
        u = "hours";
      try {
        // dateUnit = CalendarDateUnit.of(null, u + " since " +proto.getReferenceTime());
        dateUnit = CalendarDateUnit.of(null, "msecs since 1970-01-01T00:00:00");
      } catch (Exception e) {
        log.error("BufrIosp failed to create date unit", e);
        hasTime = false;
      }
    }

    return hasTime;
  }

  /* double makeObsTimeValue(ArrayStructure abb) {
    int year = abb.convertScalarInt(0, abb.findMember(yearName));
    int hour = abb.convertScalarInt(0, abb.findMember(hourName));
    int min = (minName == null) ? 0 : abb.convertScalarInt(0, abb.findMember(minName));
    int sec = (secName == null) ? 0 : abb.convertScalarInt(0, abb.findMember(secName));

    if (dayName != null) {
      int day = abb.convertScalarInt(0, abb.findMember(dayName));
      int month = abb.convertScalarInt(0, abb.findMember(monthName));
      cal.set(year, month-1, day, hour, min, sec);
    } else {
      int doy = abb.convertScalarInt(0, abb.findMember(doyName));
      cal.set(Calendar.YEAR, year);
      cal.set(Calendar.DAY_OF_YEAR, doy);
      cal.set(Calendar.HOUR_OF_DAY, hour);
      cal.set(Calendar.MINUTE, min);
      cal.set(Calendar.SECOND, sec);
    }
    Date d = cal.getTime();
    return dateUnit.makeValue(d);
  } */

  CalendarDate makeObsTimeValue(StructureData sdata) {

    int year = sdata.convertScalarInt(yearName);
    int hour = sdata.convertScalarInt(hourName);
    int min = (minName == null) ? 0 : sdata.convertScalarInt(minName);
    int sec = (secName == null) ? 0 : sdata.convertScalarInt(secName);
    if (sec < 0)  {
      System.out.println("HEY");
      sdata.convertScalarInt(secName);
      sec = 0;
    }

    if (dayName != null) {
      int day = sdata.convertScalarInt(dayName);
      int month = sdata.convertScalarInt(monthName);
      try {
        return CalendarDate.of(null, year, month, day, hour, min, sec);
      } catch(RuntimeException t) {
        log.error("Illegal Date fields", t);
        return CalendarDate.present(); // LOOK FAKE
      }

    } else {
      int doy = sdata.convertScalarInt(doyName);
      return CalendarDate.withDoy(null, year, doy, hour, min, sec);
    }
  }

}
