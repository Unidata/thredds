// $Id: GribVariable.java,v 1.26 2006/06/26 23:33:21 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.grib;

import ucar.grib.Index;
import ucar.grib.Parameter;
import ucar.grib.TableLookup;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.DataType;
import ucar.ma2.Array;

import java.util.ArrayList;
import java.util.List;

/**
 * A Variable for a Grib dataset.
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
 */
public class GribVariable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribVariable.class);

  private String name, desc;
  private Index.GribRecord firstRecord;

  private GribHorizCoordSys hcs;
  private GribCoordSys vcs; // maximal strategy (old way)
  private GribTimeCoord tcs;
  private GribVertCoord vc;
  private ArrayList records = new ArrayList(); // Index.GribRecord

  private int nlevels, ntimes;
  private Index.GribRecord[] recordTracker;
  private int decimalScale = 0;
  private boolean hasVert = false;

  GribVariable (String name, String desc,  GribHorizCoordSys hcs) {
    this.name = name; // used to get unique grouping of products
    this.desc = desc;
    this.hcs = hcs;
  }

  void addProduct( Index.GribRecord record) {
    records.add( record);
    if (firstRecord == null) firstRecord = record;
  }

  List getRecords() { return records; }

  GribHorizCoordSys getHorizCoordSys() { return hcs; }
  GribCoordSys getVertCoordSys() { return vcs; }
  GribVertCoord getVertCoord() { return vc; }
  boolean hasVert() { return hasVert; }

  void setVertCoordSys(GribCoordSys vcs) { this.vcs = vcs; }
  void setVertCoord( GribVertCoord vc) {  this.vc = vc; }
  void setTimeCoord( GribTimeCoord tcs) {  this.tcs = tcs; }

  int getVertNlevels() {
    return (vcs == null) ? vc.getNLevels() : vcs.getNLevels();
  }

  String getVertName() {
    return (vcs == null) ? vc.getVariableName() : vcs.getVerticalName();
  }

  boolean getVertIsUsed() {
    return (vcs == null) ? !vc.dontUseVertical : !vcs.dontUseVertical;
  }

   int getVertIndex(Index.GribRecord p) {
    return (vcs == null) ? vc.getIndex( p) : vcs.getIndex( p);
  }

  int getNTimes() {
    return (tcs == null) ? 1 : tcs.getNTimes();
  }

  Variable makeVariable(NetcdfFile ncfile, Group g, TableLookup lookup, boolean useDesc) {
    nlevels = getVertNlevels();
    ntimes = tcs.getNTimes();
    decimalScale = firstRecord.decimalScale;

    String vname = NetcdfFile.createValidNetcdfObjectName(useDesc ? desc : name);
    //vname = StringUtil.replace(vname, '-', "_"); // Done in dods server now
    Variable v = new Variable( ncfile, g, null, vname);
    v.setDataType( DataType.FLOAT);

    String dims = tcs.getVariableName();
    if (getVertIsUsed()) {
      dims = dims + " " + getVertName();
      hasVert = true;
    }

    if (hcs.isLatLon())
      dims = dims + " lat lon";
    else
      dims = dims + " y x";

    v.setDimensions( dims);
    Parameter param = lookup.getParameter( firstRecord);

    v.addAttribute(new Attribute("units", param.getUnit()));
    v.addAttribute(new Attribute("long_name", param.getDescription() + " @ " + getVertName()));
    v.addAttribute(new Attribute("missing_value", new Float(lookup.getFirstMissingValue())));
    if (!hcs.isLatLon()) {
      v.addAttribute(new Attribute("coordinates", "lat lon"));
      v.addAttribute(new Attribute("grid_mapping", hcs.getGridName()));
    }

    int[] paramId = lookup.getParameterId(firstRecord);
    if (paramId[0] == 1) {
      v.addAttribute(new Attribute("GRIB_param_name", param.getDescription()));
      v.addAttribute(new Attribute("GRIB_center_id", new Integer(paramId[1])));
      v.addAttribute(new Attribute("GRIB_table_id", new Integer(paramId[2])));
      v.addAttribute(new Attribute("GRIB_param_number", new Integer(paramId[3])));
    } else {
      v.addAttribute(new Attribute("GRIB_param_discipline", lookup.getDisciplineName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_param_category", lookup.getCategoryName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_param_name", param.getName()));
    }
    v.addAttribute(new Attribute("GRIB_param_id", Array.factory(int.class, new int[]{paramId.length}, paramId)));
    v.addAttribute(new Attribute("GRIB_product_definition_type", lookup.getProductDefinitionName(firstRecord)));
    v.addAttribute(new Attribute("GRIB_level_type", new Integer(firstRecord.levelType1)));

    //if (pds.getTypeSecondFixedSurface() != 255 )
   //  v.addAttribute( new Attribute("GRIB2_type_of_second_fixed_surface", pds.getTypeSecondFixedSurfaceName()));

    /* String coordSysName = getVertIsUsed() ? getVertName() :
        (hcs.isLatLon() ? "latLonCoordSys" : "projectionCoordSys");
    v.addAttribute( new Attribute("_CoordinateSystems", coordSysName)); */

    v.setSPobject( this);

    recordTracker = new Index.GribRecord[ntimes * nlevels];
    for (int i = 0; i < records.size(); i++) {
      Index.GribRecord p = (Index.GribRecord) records.get(i);
      int level = getVertIndex( p);
      if (!getVertIsUsed() && level > 0) {
        level = 0; // inconsistent level encoding ??
      }
      int time = tcs.getIndex( p);
      int recno = time*nlevels + level;
      if (recordTracker[recno] == null)
        recordTracker[recno] = p;
      else
        log.warn("Duplicate record; level="+level+" time= "+time+" for "+getName());
    }

    return v;
  }



  void dumpMissing() {
    //System.out.println("  " +name+" ntimes (across)= "+ ntimes+" nlevs (down)= "+ nlevels+":");
    System.out.println("  " +name);
    for (int j = 0; j < nlevels; j++) {
      System.out.print("   ");
      for (int i = 0; i < ntimes; i++) {
        boolean missing = recordTracker[i * nlevels + j] == null;
        System.out.print(missing ? "-" : "X");
      }
      System.out.println();
    }
  }

  int dumpMissingSummary() {
    if (nlevels == 1) return 0;

    int count = 0;
    int total = nlevels*ntimes;

    for (int i = 0; i < total; i++)
      if (recordTracker[i] == null) count++;

    System.out.println("  MISSING= "+count+"/"+total+" "+name);
    return count;
  }

  public Index.GribRecord findRecord(int time, int level) {
    return recordTracker[time*nlevels + level];
  }

  public boolean equals(Object oo) {
   if (this == oo) return true;
   if ( !(oo instanceof GribVariable)) return false;
   return hashCode() == oo.hashCode();
  }

  public String getName() { return name; }
  public int getDecimalScale() { return decimalScale; }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
   if (hashCode == 0) {
     int result = 17;
     result = 37*result + name.hashCode();
     result += 37*result + firstRecord.levelType1;
     result += 37*result + hcs.getID().hashCode();
     hashCode = result;
   }
   return hashCode;
  }
  private volatile int hashCode = 0;


  public String dump() {
    DateFormatter formatter = new DateFormatter();
   StringBuffer sbuff = new StringBuffer();
   sbuff.append(name+" "+records.size()+"\n");
   for (int i = 0; i < records.size(); i++) {
     Index.GribRecord record = (Index.GribRecord) records.get(i);
     sbuff.append(" level = "+record.levelType1+ " "+ record.levelValue1);
     if (null != record.getValidTime())
       sbuff.append(" time = "+ formatter.toDateTimeString( record.getValidTime()));
     sbuff.append("\n");
   }
   return sbuff.toString();
  }
}