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

import ucar.bufr.*;
import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;

import java.util.*;
import java.io.IOException;

/**
 * Header reading BUFR file format.
 *
 * @author rkambic
 * @author caron
 */

class Index2NC {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Index2NC.class);

  private Index index;
  private ucar.nc2.NetcdfFile ncfile;
  private FeatureType ftype;

  BufrRecord record;
  private BufrTables table;
  private BufrDataDescriptionSection dds;
  List<DataDescriptor> dkeys;

  Index2NC(BufrRecord record, Index index, ucar.nc2.NetcdfFile ncf) throws IOException {
    this.record = record;


    BufrTables table = new BufrTables(record.ids.getMasterTableFilename());

    this.dds = record.dds;
    this.dds.getRoot(table);
    dkeys = dds.getDescriptorKeys();
    int nbits = dds.getTotalBits();
    int inputBytes = (nbits % 8 == 0) ? nbits / 8 : nbits / 8 + 1;
    int outputBytes = dds.getTotalBytes();

    int cat = record.ids.getCategory();

    this.index = index;
    this.ncfile = ncf;

    Map<String, String> atts = index.getGlobalAttributes();

    // find out what type of dataset, so proper ncfile structure can be created
    //boolean pointDS = false, stationDS = false, trajectoryDS = false, satelliteDS = false;
    String category = atts.get("category");
    if (cat == 0) {
      ftype = FeatureType.STATION;
    } else if (cat == 2) {
      ftype = FeatureType.STATION_PROFILE;
    } else if (cat == 3) {
      ftype = FeatureType.PROFILE;
    } else if (cat == 4) {
      ftype = FeatureType.TRAJECTORY;
    } else {
      log.warn("unknown category=" + category);
    }

    // parameters in this DS
    //parameters = index.getParameters();

    // global Attributes
    ncfile.addAttribute(null, new Attribute("history", "direct read of BUFR data by CDM version 4.0"));
    addGlobalAttribute(atts, "location", "original_location");
    addGlobalAttribute(atts, "bufr_edition", "BUFR:edition");
    addGlobalAttribute(atts, "category", null);
    addGlobalAttribute(atts, "center_id", null);
    addGlobalAttribute(atts, "sub_center_id", null);
    addGlobalAttribute(atts, "table", null);
    addGlobalAttribute(atts, "header", null);

    ncfile.addAttribute(null, new Attribute("Conventions", "Unidata Point Feature v1.0"));

    ncfile.addAttribute(null, new Attribute("time_coverage_start", index.getObsTimes().get(0)));
    ncfile.addAttribute(null, new Attribute("time_coverage_end", index.getObsTimes().get(index.getObsTimes().size() - 1)));

    if (ftype != null)
      ncfile.addAttribute(null, new Attribute("cdm_datatype", ftype.toString()));

    makeObsRecord();
    makeReportIndexStructure();

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


  private void makeObsRecord() {
    Dimension obsDim = new Dimension("record", index.getNumberObs());
    ncfile.addDimension(null, obsDim);

    Structure recordStructure = new Structure(ncfile, null, null, BufrIosp.obsRecord);
    ncfile.addVariable(null, recordStructure);
    recordStructure.setDimensions("record");

    Variable timev = recordStructure.addMemberVariable(new Variable(ncfile, null, recordStructure, "time", DataType.LONG, ""));
    timev.addAttribute(new Attribute("units", "msecs since 1970-01-01 00:00"));
    timev.addAttribute(new Attribute("long_name", "observation time"));
    timev.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));

    for (DataDescriptor dkey : dkeys) {
      if (!dkey.isOkForVariable()) continue;

      if (dkey.replication == 0)
        addSequence(recordStructure, dkey);

      else if (dkey.replication > 1) {
        List<DataDescriptor> subKeys = dkey.subKeys;
        if (subKeys.size() == 1) {
          DataDescriptor sub = dkey.subKeys.get(0);
          Variable v = addVariable(recordStructure, sub, dkey.replication);
          v.setSPobject(dkey); // set the replicating dkey as SPI object
        } else {
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

  private Variable addVariable(Structure struct, DataDescriptor dataDesc, int count) {
    Variable v = new Variable(ncfile, null, struct, dataDesc.name);
    try {
      if (count > 1)
        v.setDimensionsAnonymous(new int[]{count}); // anon vector
      else
        v.setDimensions(""); // scalar
    } catch (InvalidRangeException e) {
      log.error("illegal count= " + count + " for " + dataDesc);
    }
    if (dataDesc.units == null)
      System.out.println("HEY dataDesc.units == null");
    else
      v.addAttribute(new Attribute("units", dataDesc.units));

    if (dataDesc.type != 1) {
      int nbits = dataDesc.bitWidth;
      int nbytes = (nbits % 8 == 0) ? nbits / 8 : nbits / 8 + 1;
      if (nbytes == 1) {
        v.setDataType(DataType.BYTE);
        if (nbits == 8) {
          v.addAttribute(new Attribute("_unsigned", "true"));
          v.addAttribute(new Attribute("missing_value", (short) BufrDataSection.missing_value[nbits]));
        } else
          v.addAttribute(new Attribute("missing_value", (byte) BufrDataSection.missing_value[nbits]));

      } else if (nbytes == 2) {
        v.setDataType(DataType.SHORT);
        if (nbits == 16) {
          v.addAttribute(new Attribute("_unsigned", "true"));
          v.addAttribute(new Attribute("missing_value", BufrDataSection.missing_value[nbits]));
        } else
          v.addAttribute(new Attribute("missing_value", (short) BufrDataSection.missing_value[nbits]));

      } else {
        v.setDataType(DataType.INT);
        v.addAttribute(new Attribute("missing_value", BufrDataSection.missing_value[nbits]));
      }

      int scale10 = dataDesc.scale;
      double scale = (scale10 == 0) ? 1.0 : Math.pow(10.0, -scale10);
      if (scale10 != 0)
        v.addAttribute(new Attribute("scale_factor", (float) scale));
      if (dataDesc.refVal != 0)
        v.addAttribute(new Attribute("add_offset", (float) scale * dataDesc.refVal));

    } else {
      v.setDataType(DataType.CHAR);
      int size = dataDesc.bitWidth / 8;
      try {
        v.setDimensionsAnonymous(new int[]{size});
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
    }

    annotate(v, dataDesc);
    v.addAttribute(new Attribute("BUFR:TableB_descriptor", dataDesc.id));
    v.addAttribute(new Attribute("BUFR:bitWidth", dataDesc.bitWidth));
    struct.addMemberVariable(v);
    v.setSPobject(dataDesc);
    return v;
  }

  private void annotate(Variable v, DataDescriptor dkey) {
    if (dkey.id.equals("0-5-1") || dkey.id.equals("0-5-2") || dkey.id.equals("0-27-1") || dkey.id.equals("0-27-2")) {
      v.addAttribute(new Attribute("units", "degrees_north"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    }

    if (dkey.id.equals("0-6-1") || dkey.id.equals("0-6-2") ||
            dkey.id.equals("0-28-1") || dkey.id.equals("0-28-2")) {
      v.addAttribute(new Attribute("units", "degrees_east"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    }

    if (dkey.id.equals("0-7-1") || dkey.id.equals("0-7-2")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
    }

    if (dkey.id.equals("0-4-250")) {   // time
      v.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));
    }

    if (dkey.id.equals("0-7-6")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
    }

    if (dkey.id.equals("0-1-18")) {
      v.addAttribute(new Attribute("standard_name", "station_name"));
    }

  }

  /* private void makeProfile(FeatureType ftype) {
  boolean isStation = ftype == FeatureType.STATION_PROFILE;

  Structure stnStructure = null;
  if (isStation) {
    Dimension stnsDim = new Dimension("station", index.getLocations().size());
    ncfile.addDimension(null, stnsDim);

    stnStructure = new Structure(ncfile, null, null, "station");
    stnStructure.setDimensions("station");
    ncfile.addVariable(null, stnStructure);

    // Dimensions
    List<Dimension> dl = new ArrayList<Dimension>();
    List<Dimension> ds = new ArrayList<Dimension>();
    ds.add(stnsDim);

  }

  Dimension obsDim = new Dimension("record", index.getNumberObs());
  ncfile.addDimension(null, obsDim);

  Structure recordStructure = new Structure(ncfile, null, null, "obsRecord");
  ncfile.addVariable(null, recordStructure);
  recordStructure.setDimensions("record");

  Sequence levelStructure = new Sequence(ncfile, null, recordStructure, "level");

  boolean levelIncrement = false;
  Dimension d;
  for (Index.Parameter parm : parameters) {
    System.out.println("Parameter= " + parm);

    if (dkey.id.equals("0-5-1") || dkey.id.equals("0-5-2") ||
            dkey.id.equals("0-27-1") || dkey.id.equals("0-27-2")) {
      Variable v = addVariable(isStation ? stnStructure : recordStructure, parm);
      v.addAttribute(new Attribute("units", "degrees_north"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
      continue;
    }

    if (dkey.id.equals("0-6-1") || dkey.id.equals("0-6-2") ||
            dkey.id.equals("0-28-1") || dkey.id.equals("0-28-2")) {
      Variable v = addVariable(isStation ? stnStructure : recordStructure, parm);
      v.addAttribute(new Attribute("units", "degrees_east"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
      continue;
    }

    if (dkey.id.equals("0-7-1") || dkey.id.equals("0-7-2")) {
      Variable v = addVariable(isStation ? stnStructure : recordStructure, parm);
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
      continue;
    }

    if (dkey.id.equals("0-1-18")) {   // stn name
      Variable v = addVariable(stnStructure, parm);
      v.addAttribute(new Attribute("standard_name", "station_id"));
      continue;
    }

    if (dkey.id.equals("0-1-1") || dkey.id.equals("0-1-2") || dkey.id.equals("0-1-18") || dkey.id.equals("0-2-1")
            || dkey.id.equals("0-2-3")) {   // stn info
      addVariable(stnStructure, parm);
      continue;
    }

    if (dkey.id.equals("0-4-250")) {   // time
      Variable v = addVariable(recordStructure, parm);
      v.setDataType(DataType.INT);
      v.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));
      continue;
    }

    if (dkey.id.equals("0-7-6")) {
      Variable v = addVariable(levelStructure, parm);
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
      continue;
    }

    if (parm.dimension == 1) {
      addVariable(recordStructure, parm);

    } else {
      addVariable(levelStructure, parm);
    }
  }

  recordStructure.addMemberVariable(levelStructure);
}  */

  /* if (p.key.equals("0-7-5")) {
levelIncrement = true;
d = new Dimension("dim43", 43, true, false, false);
ncfile.addDimension(null, d);
dimensions.put("dim43", d);
d = new Dimension("dim86", 86, true, false, false);
ncfile.addDimension(null, d);
dimensions.put("dim86", d);
}

if (p.key.equals("0-2-134") || p.key.equals("0-2-135") ||
  p.key.equals("0-7-5")) {
d = dimensions.get("dim3");
dl.add(d);
addVariable(recordStructure, dl, p);
dl.remove(0);

} else if (levelIncrement && p.dimension != 1) {
if (p.key.equals("0-8-22")) {
  d = dimensions.get("dim86");
} else {
  d = dimensions.get("dim43");
}
dl.add(d);
//addVariable( levelStructure, dl, p );
addVariable(recordStructure, dl, p);
dl.remove(0);

} else if (p.dimension != 1) {

if (levelStructure == null) {
  levelsDim = new Dimension("levels", Dimension.VLEN.getLength(), true, false, true);
  ncfile.addDimension(null, levelsDim);
  levelStructure = new Structure(ncfile, ncfile.getRootGroup(), recordStructure, "level");
  levelStructure.setDimensions(levelsDim.getName());
}
//String dim = "dim"+ Integer.toString( p.dimension );
//Dimension d = (Dimension) dimensions.get( dim );
//dl.add( d );
addVariable(levelStructure, dl, p);
//addVariable( recordStructure, dl, p );
//dl.remove( 0 );
//break;
} else {
addVariable(recordStructure, dl, p);
}    */

//if (levelStructure != null)
//  recordStructure.addMemberVariable(levelStructure);
//}

/* private void createTrajNC(Dimension trajsDim) {
// Dimensions
List<Dimension> dl = new ArrayList<Dimension>();
List<Dimension> dt = new ArrayList<Dimension>();
dt.add(trajsDim);

// create variables
Variable v;
v = new Variable(ncfile, ncfile.getRootGroup(), null, "number_trajectories");
v.addAttribute(new Attribute("long_name", "number of trajectories"));
v.setDimensions(dl);
v.setDataType(DataType.INT);
ncfile.addVariable(null, v);
v = new Variable(ncfile, ncfile.getRootGroup(), null, "trajectory_id");
v.addAttribute(new Attribute("long_name", "Trajectory Identification"));
v.setDimensions(dt);
v.setDataType(DataType.STRING);
ncfile.addVariable(null, v);
v = new Variable(ncfile, ncfile.getRootGroup(), null, "firstChild");
v.addAttribute(new Attribute("long_name", "firstChild for this trajectory"));
v.setDimensions(dt);
v.setDataType(DataType.INT);
ncfile.addVariable(null, v);
v = new Variable(ncfile, ncfile.getRootGroup(), null, "numChildren");
v.addAttribute(new Attribute("long_name", "number of obs in this trajectory"));
v.setDimensions(dt);
v.setDataType(DataType.INT);
ncfile.addVariable(null, v);

v = new Variable(ncfile, ncfile.getRootGroup(), recordStructure, "parent_index");
v.addAttribute(new Attribute("long_name", "index of this trajectory for the record"));
v.setDimensions(dl);
v.setDataType(DataType.INT);
recordStructure.addMemberVariable(v);

//System.out.println("parameters.size() =" + parameters.size() );
for (Index.Parameter p : parameters) {

  if (p.dimension != 1) {
    String dim = "dim" + Integer.toString(p.dimension);
    Dimension d = dimensions.get(dim);
    dl.add(d);
    addVariable(recordStructure, dl, p);
    dl.remove(0);
  } else {
    addVariable(recordStructure, dl, p);
  }
}
}  */

/* private void createSatVertNC(Dimension levelsDim) {
 // Dimensions
 Dimension dim35 = new Dimension("dim35", 35, true, false, false);
 ncfile.addDimension(null, dim35);
 dimensions.put("dim35", dim35);
 Dimension dim40 = new Dimension("dim40", 40, true, false, false);
 ncfile.addDimension(null, dim40);
 dimensions.put("dim40", dim40);
 List<Dimension> dl = new ArrayList<Dimension>();
 //ArrayList dt = new ArrayList();
 //dt.add( levelsDim );
 // create variables
 // Variable v;

 //System.out.println("parameters.size() =" + parameters.size() );
 Dimension d;
 for (Index.Parameter p : parameters) {

   if (p.dimension != 1) {
     if (p.key.equals("0-5-42") || p.key.equals("0-12-63")) {
       d = dimensions.get("dim35");
     } else if (p.key.equals("0-13-2")) {
       d = dimensions.get("dim40");
     } else {
       String dim = "dim" + Integer.toString(p.dimension);
       d = dimensions.get(dim);
     }
     dl.add(d);
     addVariable(recordStructure, dl, p);
     dl.remove(0);

   } else {
     addVariable(recordStructure, dl, p);
   }
 }
} */


  private void addGlobalAttribute(Map<String, String> atts, String key, String attName) {
    String value = atts.get(key);
    if (value != null) {
      if (attName == null) attName = "BUFR:" + key;
      ncfile.addAttribute(null, new Attribute(attName, value));
    }
  }


}
