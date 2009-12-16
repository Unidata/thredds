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
import ucar.nc2.iosp.bufr.tables.CodeFlagTables;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

/**
 * BufrIosp delegates the construction of the Netcdf objects to ConstructNC.
 *
 * @author caron
 */

class ConstructNC {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConstructNC.class);
  static final String TIME_NAME = "time";
  static private final boolean warnUnits = false;

  private ucar.nc2.NetcdfFile ncfile;

  Sequence recordStructure;
  private Message proto;

  ConstructNC(Message proto, int nobs, ucar.nc2.NetcdfFile nc) throws IOException {
    this.proto = proto;
    this.ncfile = nc;

    //dkeyRoot = dds.getDescriptorRoot();
    //int nbits = dds.getTotalBits();
    //int inputBytes = (nbits % 8 == 0) ? nbits / 8 : nbits / 8 + 1;
    //int outputBytes = dds.getTotalBytes();

    // the category
    int cat = proto.ids.getCategory();
    int subcat = proto.ids.getSubCategory();
    /* if ((cat == 0) || (cat == 12 && subcat == 0)) {
      ftype = FeatureType.STATION;
    } else if (cat == 2) {
      ftype = FeatureType.STATION_PROFILE;
    } else if (cat == 3) {
      ftype = FeatureType.PROFILE;
    } else if (cat == 4) {
      ftype = FeatureType.TRAJECTORY;
    } else {
      // log.warn("unknown category=" + category);
    }  */

    // global Attributes
    ncfile.addAttribute(null, new Attribute("history", "direct read of BUFR data by CDM version 4.1"));
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

    // cant tell what the ttype is - defere to BufrCdm plugin
    /* if (ftype != null) {
      CF.FeatureType cf = CF.FeatureType.convert( ftype);
      if (ftype != null)
        ncfile.addAttribute(null, new Attribute(CF.featureTypeAtt, cf.toString()));
    } */

    makeObsRecord();
    //makeReportIndexStructure();

    ncfile.finish();
  }

  private Structure makeReportIndexStructure() throws IOException {
    Structure reportIndex = new Structure(ncfile, null, null, BufrIosp.obsIndex);
    ncfile.addVariable(null, reportIndex);
    reportIndex.setDimensions("record");

    reportIndex.addAttribute(new Attribute("long_name", "index on report"));

    Variable v = reportIndex.addMemberVariable(new Variable(ncfile, null, reportIndex, "name", DataType.STRING, ""));
    v.addAttribute(new Attribute("long_name", "name of station"));
    v.addAttribute(new Attribute("standard_name", "station_name"));

    v = reportIndex.addMemberVariable(new Variable(ncfile, null, reportIndex, "time", DataType.LONG, ""));
    v.addAttribute(new Attribute("units", "msecs since 1970-01-01 00:00"));
    v.addAttribute(new Attribute("long_name", "observation time"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));

    return reportIndex;
  }


  private void makeObsRecord() throws IOException {
    recordStructure = new Sequence(ncfile, null, null, BufrIosp.obsRecord);
    ncfile.addVariable(null, recordStructure);
    //recordStructure.setDimensions("record");

    DataDescriptor root = proto.getRootDataDescriptor();
    if (hasTime()) {
      Variable timev = recordStructure.addMemberVariable(new Variable(ncfile, null, recordStructure, TIME_NAME, DataType.INT, ""));
      timev.addAttribute(new Attribute("units", dateUnit.getUnitsString()));
      timev.addAttribute(new Attribute("long_name", "time of observation"));
      timev.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));
    }

    for (DataDescriptor dkey : root.subKeys) {
      if (!dkey.isOkForVariable()) continue;

      if (dkey.replication == 0)
        addSequence(recordStructure, dkey);

      else if (dkey.replication > 1) {
        List<DataDescriptor> subKeys = dkey.subKeys;
        if (subKeys.size() == 1) {
          DataDescriptor sub = dkey.subKeys.get(0);
          if (sub.dpi != null) {
            addDpiStructure(recordStructure, dkey, sub);

          } else {
            Variable v = addVariable(recordStructure, sub, dkey.replication);
            v.setSPobject(dkey); // set the replicating dkey as SPI object
          }
        } else if (subKeys.size() > 1) {
          addStructure(recordStructure, dkey, dkey.replication);
        }

      } else {
        addVariable(recordStructure, dkey, dkey.replication);
      }
    }
  }

  private int structNum = 1;
  private void addStructure(Structure parent, DataDescriptor dataDesc, int count) {
    String structName = "struct" + structNum;
    structNum++;
    Structure struct = new Structure(ncfile, null, parent, structName);
    try {
      struct.setDimensionsAnonymous(new int[]{count}); // anon vector
    } catch (InvalidRangeException e) {
      log.error("illegal count= " + count + " for " + dataDesc);
    }

    for (DataDescriptor subKey : dataDesc.getSubKeys())
      addMember(struct, subKey);

    parent.addMemberVariable(struct);
    struct.setSPobject(dataDesc);

    dataDesc.name = structName;
    dataDesc.refersTo = struct;
  }

  private int seqNum = 1;

  private void addSequence(Structure parent, DataDescriptor dataDesc) {
    //String seqName = ftype == (FeatureType.STATION_PROFILE) ? "profile" : "seq";
    String seqName = "seq" + seqNum;
    seqNum++;

    Sequence seq = new Sequence(ncfile, null, parent, seqName);
    seq.setDimensions(""); // scalar

    for (DataDescriptor dkey : dataDesc.getSubKeys())
      addMember(seq, dkey);

    parent.addMemberVariable(seq);
    seq.setSPobject(dataDesc);

    dataDesc.name = seqName;
    dataDesc.refersTo = seq;
  }

  private void addMember(Structure parent, DataDescriptor dkey) {
    if (dkey.replication == 0)
      addSequence(parent, dkey);

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

  private void addDpiStructure(Structure parent, DataDescriptor parentDD, DataDescriptor dpiField) {
    String structName = findUnique(parent, dpiField.name);
    Structure struct = new Structure(ncfile, null, parent, structName);
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

    dpiField.name = structName;
    dpiField.refersTo = struct;

    // add some fake dkeys corresponding to above
    // DataDescriptor nameDD = new DataDescriptor();
  }

  private void addDpiSequence(Structure parent, DataDescriptor dataDesc) {
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

  private Variable addVariable(Structure struct, DataDescriptor dataDesc, int count) {
    String name = findUnique(struct, dataDesc.name);
    dataDesc.name = name; // name may need to be changed for uniqueness

    Variable v = new Variable(ncfile, null, struct, name);
    try {
      if (count > 1)
        v.setDimensionsAnonymous(new int[]{count}); // anon vector
      else
        v.setDimensions(""); // scalar
    } catch (InvalidRangeException e) {
      log.error("illegal count= " + count + " for " + dataDesc);
    }

    if (dataDesc.units == null) {
      if (warnUnits) log.warn("dataDesc.units == null for " + name);
    } else {
        if ((dataDesc == null) || (dataDesc.units == null))
            System.out.println("HEY");
      if (dataDesc.units.equalsIgnoreCase("Code_Table") || dataDesc.units.equalsIgnoreCase("Code Table"))
        v.addAttribute(new Attribute("units", "CodeTable " + dataDesc.getFxyName()));
      else if (dataDesc.units.equalsIgnoreCase("Flag_Table") || dataDesc.units.equalsIgnoreCase("Flag Table"))
        v.addAttribute(new Attribute("units", "FlagTable " + dataDesc.getFxyName()));
      else if (!dataDesc.units.startsWith("CCITT") && !dataDesc.units.startsWith("Numeric"))
        v.addAttribute(new Attribute("units", dataDesc.units));
    }

    if (dataDesc.type == 1) {
      v.setDataType(DataType.CHAR);
      int size = dataDesc.bitWidth / 8;
      try {
        v.setDimensionsAnonymous(new int[]{size});
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }

    } else if ((dataDesc.type == 2) && CodeFlagTables.hasTable(dataDesc.fxy)) {
      int nbits = dataDesc.bitWidth;
      int nbytes = (nbits % 8 == 0) ? nbits / 8 : nbits / 8 + 1;

      CodeFlagTables ct = CodeFlagTables.getTable(dataDesc.fxy);
      if (nbytes == 1)
        v.setDataType(DataType.ENUM1);
      else if (nbytes == 2)
        v.setDataType(DataType.ENUM2);
      else if (nbytes == 4)
        v.setDataType(DataType.ENUM4);

      //v.removeAttribute("units");
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

    annotate(v, dataDesc);
    v.addAttribute(new Attribute("BUFR:TableB_descriptor", dataDesc.getFxyName()));
    v.addAttribute(new Attribute("BUFR:bitWidth", dataDesc.bitWidth));
    struct.addMemberVariable(v);
    v.setSPobject(dataDesc);
    return v;
  }

  private String findUnique(Structure struct, String want) {
    Variable oldV = struct.findVariable(want);
    if (oldV == null) return want;

    int seq = 1;
    while (true) {
      String wantSeq = want + "-" + seq;
      oldV = struct.findVariable(wantSeq);
      if (oldV == null) return wantSeq;
      seq++;
    }
  }


  private void annotate(Variable v, DataDescriptor dkey) {
    String id = dkey.getFxyName();
    if (id.equals("0-5-1") || id.equals("0-5-2")) {
      v.addAttribute(new Attribute("units", "degrees_north"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    }

    if (id.equals("0-6-1") || id.equals("0-6-2")) {
      v.addAttribute(new Attribute("units", "degrees_east"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    }

    if (id.equals("0-7-1") || id.equals("0-7-2")|| id.equals("0-7-10")|| id.equals("0-7-30")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
    }

    if (id.equals("0-7-6") || id.equals("0-7-7")) { // LOOK : height above station ??
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
    }

    if (id.equals("0-1-7") || id.equals("0-1-11") || id.equals("0-1-18") || (id.equals("0-1-194") && (proto.ids.getCenterId() == 59))) {
      v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.STATION_ID));
    }

    if (id.equals("0-1-2")) {
      v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.STATION_WMOID));
    }

  }

  boolean hasTime;
  private String yearName, monthName, dayName, hourName, minName, secName, doyName;
  private DateUnit dateUnit;
  private Calendar cal;

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

    hasTime = (yearName != null) && (((monthName != null) && (dayName != null)) || (doyName != null)) && (hourName != null);

    if (hasTime) {
      String u;
      if (secName != null)
        u = "secs";
      else if (minName != null)
        u = "minutes";
      else
        u = "hours";
      try {
        DateFormatter format = new DateFormatter();
        dateUnit = new DateUnit(u + " since " +format.toDateTimeStringISO(proto.getReferenceTime()));
      } catch (Exception e) {
        log.error("BufrIosp failed to create date unit", e);
        hasTime = false;
      }
    }

    if (hasTime) {
      cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
      cal.clear();
    }

    return hasTime;
  }

  double makeObsTimeValue(ArrayStructure abb) {
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
  }

  double makeObsTimeValue(StructureData sdata) {

    int year = sdata.convertScalarInt(yearName);
    int hour = sdata.convertScalarInt(hourName);
    int min = (minName == null) ? 0 : sdata.convertScalarInt(minName);
    int sec = (secName == null) ? 0 : sdata.convertScalarInt(secName);

    if (dayName != null) {
      int day = sdata.convertScalarInt(dayName);
      int month = sdata.convertScalarInt(monthName);
      cal.set(year, month-1, day, hour, min, sec);
    } else {
      int doy = sdata.convertScalarInt(doyName);
      cal.set(Calendar.YEAR, year);
      cal.set(Calendar.DAY_OF_YEAR, doy);
      cal.set(Calendar.HOUR_OF_DAY, hour);
      cal.set(Calendar.MINUTE, min);
      cal.set(Calendar.SECOND, sec);
    }
    Date d = cal.getTime();
    return dateUnit.makeValue(d);
  }

  // not currently used
  static private String identifyCoords(String val) {
    if (val.equals("0-5-1") || val.equals("0-5-2"))
      return AxisType.Lat.toString();

    if (val.equals("0-6-1") || val.equals("0-6-2"))
      return AxisType.Lon.toString();

    if (val.equals("0-7-30") || val.equals("0-7-1") || val.equals("0-7-2") || val.equals("0-7-10"))
      return AxisType.Height.toString();

    if (val.equals("0-4-1"))
      return "year";
    if (val.equals("0-4-2"))
      return "month";
    if (val.equals("0-4-3"))
      return "day";
    if (val.equals("0-4-4"))
      return "hour";
    if (val.equals("0-4-5"))
      return "minute";
    if (val.equals("0-4-6") || val.equals("0-4-7"))
      return "sec";

    if (val.equals("0-1-1"))
      return "wmo_block";
    if (val.equals("0-1-2"))
      return "wmo_id";

    if ((val.equals("0-1-7") || val.equals("0-1-194") || val.equals("0-1-11") || val.equals("0-1-18")))
      return "station_id";

    return null;
  }
}
