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
import ucar.nc2.util.CancelTask;
import ucar.ma2.DataType;

import java.util.*;

/**
 * Header reading BUFR file format.
 *
 * @author rkambic
 */

class Index2NC {

  private Index index;
  private ucar.nc2.NetcdfFile ncfile;

  private Structure recordStructure, levelStructure;
  private Map<String, Dimension> dimensions = new HashMap<String, Dimension>();
  private List<Index.Parameter> parameters;

  private boolean setTime = true, setLat = true, setLon = true, setHgt = true;

  void open(Index index, ucar.nc2.NetcdfFile ncf, CancelTask cancelTask) {
    this.index = index;
    this.ncfile = ncf;

    Map<String, String> atts = index.getGlobalAttributes();

    // find out what type of dataset, so proper ncfile structure can be created
    //boolean pointDS = false, stationDS = false, trajectoryDS = false, satelliteDS = false;
    FeatureType ftype;
    String category = atts.get("category");
    if (category.startsWith("2 Vertical")) {
      ftype = FeatureType.STATION_PROFILE;
    } else if (category.startsWith("3 Vertical")) {
      ftype = FeatureType.PROFILE;
    } else if (category.startsWith("4 Single level upper-air data")) {
      ftype = FeatureType.TRAJECTORY;
    } else {
      throw new UnsupportedOperationException("category="+category);
    }

    // parameters in this DS
    parameters = index.getParameters();

    recordStructure = new Structure(ncfile, ncfile.getRootGroup(), null, "record");

    // global Attributes
    ncfile.addAttribute(null, new Attribute("history", "direct read of BUFR data by CDM version 4.0"));
    addGlobalAttribute(atts, "location", "original_location");
    addGlobalAttribute(atts, "bufr_edition", "BUFR_edition");
    addGlobalAttribute(atts, "category", null);
    addGlobalAttribute(atts, "center_id", null);
    addGlobalAttribute(atts, "sub_center_id", null);
    addGlobalAttribute(atts, "table", null);
    addGlobalAttribute(atts, "header", null);

    ncfile.addAttribute(null, new Attribute("Conventions", "Unidata"));
    /* ncfile.addAttribute(null, new Attribute("observationDimension", "obs"));
    if (stationDS) {
      ncfile.addAttribute(null, new Attribute("stationDimension", "stns"));
      ncfile.addAttribute(null, new Attribute("parent_index_coordinate", "record.parent_index"));
    } else if (trajectoryDS) {
      ncfile.addAttribute(null, new Attribute("trajectoryDimension", "trajectories"));
    } */

    //ncfile.addAttribute( null, new Attribute( "time_nominal_coordinate", "record.time_nominal" ));
    ncfile.addAttribute(null, new Attribute("time_coverage_start", (String) index.getObsTimes().get(0)));
    ncfile.addAttribute(null, new Attribute("time_coverage_end", (String) index.getObsTimes().get(index.getObsTimes().size() - 1)));
    //ncfile.addAttribute(null, new Attribute("geospatial_lat_max", "90"));
    //ncfile.addAttribute(null, new Attribute("geospatial_lat_min", "-90"));
    //ncfile.addAttribute(null, new Attribute("geospatial_lon_max", "360"));
    //ncfile.addAttribute(null, new Attribute("geospatial_lon_min", "0"));

    ncfile.addAttribute(null, new Attribute("cdm_datatype", ftype.toString()));

    /* global Dimensions / Attributes
    Dimension stnsDim = null, levelsDim = null, trajsDim = null;
    if (stationDS) {
      // stnsDim = new Dimension("stns", index.getLocations().size(), true, false, false);
      ncfile.addDimension(null, stnsDim);
      //levelsDim = new Dimension("levels", Dimension.UNKNOWN.getLength(), true, false, true);
      //ncfile.addDimension( null, levelsDim );

    } else if (trajectoryDS) {
      trajsDim = new Dimension("trajectories", index.getLocations().size(), true, false, false);
      ncfile.addDimension(null, trajsDim);

    } else if (satelliteDS) {
      levelsDim = new Dimension("levels", 41, true, false, false);
      ncfile.addDimension(null, levelsDim);
    }

    Dimension obsDim = new Dimension("obs", index.getNumberObs(), true, true, false);
    ncfile.addDimension(null, obsDim);
    recordStructure.setDimensions(obsDim.getName());

    // create/add variable dimensions
    for (Index.Parameter p : parameters) {
      //System.out.println( "p.dim ="+ p.dim );
      if (p.dimension != 1) { // 1 = obsDim
        String dim = "dim" + Integer.toString(p.dimension);
        if (!dimensions.containsKey(dim)) {
          if (p.dimension > 10) {
            dimensions.put(dim, levelsDim);
          } else {
            Dimension d = new Dimension(dim, p.dimension, true, false, false);
            ncfile.addDimension(null, d);
            dimensions.put(dim, d);
          }
        }
      }
    } */

    Dimension levelsDim = null, trajsDim = null;

    // create variables
      makeStationProfile(ftype);
    /*
    } else if (satelliteDS) {
      createSatVertNC(levelsDim);

    } /* else {
      // Dimension for variables
      //System.out.println("parameters.size() =" + parameters.size() );
      for (Index.Parameter p : parameters) {
        List<Dimension> dl = new ArrayList<Dimension>();

        if (p.dimension != 1) {
          String dimName = "levels" + p.dimension;
          Dimension d = dimensions.get(dimName);
          if (d == null) {
            d = new Dimension(dimName, p.dimension);
            dimensions.put(dimName, d);
            ncfile.addDimension(null, d);
          }
          dl.add(d);

          addVariable(recordStructure, dl, p);

        } else {
          addVariable(recordStructure, dl, p);
        }
      }
      ncfile.addVariable(null, recordStructure);
    } */
/*
    if( levelStructure != null ) {
       levelStructure.setDimensions( levelsDim.getName());
       ncfile.addVariable(null, levelStructure);
    }
*/
    // finish
    ncfile.finish();

  }

  private void makeStationProfile(FeatureType ftype) {
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

      if (parm.key.equals("0-5-1") || parm.key.equals("0-5-2") ||
              parm.key.equals("0-27-1") || parm.key.equals("0-27-2")) {
        Variable v = addVariable(isStation ? stnStructure : recordStructure, parm);
        v.addAttribute(new Attribute("units", "degrees_north"));
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
        continue;
      }

      if (parm.key.equals("0-6-1") || parm.key.equals("0-6-2") ||
              parm.key.equals("0-28-1") || parm.key.equals("0-28-2")) {
        Variable v = addVariable(isStation ? stnStructure : recordStructure, parm);
        v.addAttribute(new Attribute("units", "degrees_east"));
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
        continue;
      }

      if (parm.key.equals("0-7-1") || parm.key.equals("0-7-2")) {
        Variable v = addVariable(isStation ? stnStructure : recordStructure, parm);
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
        continue;
      }

      if (parm.key.equals("0-1-18")) {   // stn name
        Variable v = addVariable(stnStructure, parm);
        v.addAttribute(new Attribute("standard_name", "station_id"));
        continue;
      }

      if (parm.key.equals("0-1-1") || parm.key.equals("0-1-2") || parm.key.equals("0-1-18") || parm.key.equals("0-2-1") 
              || parm.key.equals("0-2-3")) {   // stn info
        addVariable(stnStructure, parm);
        continue;
      }

      if (parm.key.equals("0-4-250")) {   // time
        Variable v = addVariable(recordStructure, parm);
        v.setDataType(DataType.INT);
        v.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));
        continue;
      }

      if (parm.key.equals("0-7-6")) {
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
    ncfile.finish();    
  }

  private Variable addVariable(Structure struct, Index.Parameter parm) {
    Variable v = new Variable(ncfile, null, struct, parm.name);
    v.setDimensions(""); // scalar
    v.addAttribute(new Attribute("units", parm.units));

    if (parm.isNumeric) {
      v.setDataType(DataType.FLOAT);
      v.addAttribute(new Attribute("missing_value", "-9999"));

    } else {
      v.setDataType(DataType.STRING);
    }

    v.addAttribute(new Attribute("BUFR_TableB_desc", parm.key));
    struct.addMemberVariable(v);
    return v;
  }

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
      if (attName == null) attName = "BUFR_" + key;
      ncfile.addAttribute(null, new Attribute(attName, value));
    }
  }


}
