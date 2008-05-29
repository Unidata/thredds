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


package ucar.nc2.iosp.grid;


import ucar.ma2.Array;
import ucar.ma2.DataType;

import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;

import java.util.ArrayList;
import java.util.List;


/**
 * A Variable for a Grid dataset.
 * @author caron
 */
public class GridVariable {

    /** logger */
    static private org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(GridVariable.class);

    /** parameter name */
    private String name;

    /** parameter description */
    private String desc;

    /** variable name */
    private String vname;

    /** first grid record */
    private GridRecord firstRecord;

    /** lookup table */
    private GridTableLookup lookup;

    /** flag for grib1*/
    private boolean isGrib1;

    /** horizontal coord system */
    private GridHorizCoordSys hcs;

    /** vertical coord system */
    private GridCoordSys vcs;  // maximal strategy (old way)

    /** time coord system */
    private GridTimeCoord tcs;

    /** vertical coordinate */
    private GridVertCoord vc;

    /** list of records that make up this variable */
    private ArrayList records = new ArrayList();  // GridRecord

    /** number of levels */
    private int nlevels;

    /** number of times */
    private int ntimes;

    /** record tracker */
    private GridRecord[] recordTracker;

    /** decimal scale */
    private int decimalScale = 0;

    /** flag for having a vertical coordinate */
    private boolean hasVert = false;

    /** debug flag */
    private boolean showRecords = false;

    /** debug flag */
    private boolean showGen = false;

    /**
     * Create a new GridVariable
     *
     * @param name  name
     * @param desc  description
     * @param hcs   horizontal coordinate system
     * @param lookup  lookup table
     */
    GridVariable(String name, String desc, GridHorizCoordSys hcs,
                 GridTableLookup lookup) {
        this.name   = name;  // used to get unique grouping of products
        this.desc   = desc;
        this.hcs    = hcs;
        this.lookup = lookup;
        // TODO: what to do?
        //isGrib1 = (lookup instanceof Grib1Lookup);
        isGrib1 = false;
    }

    /**
     * Add in a new product
     *
     * @param record  grid  to add
     */
    void addProduct(GridRecord record) {
        records.add(record);
        if (firstRecord == null) {
            firstRecord = record;
        }
    }

    /**
     * Get the list of grids
     *
     * @return grid records
     */
    List getRecords() {
        return records;
    }

    /**
     * get the first grid record
     *
     * @return  the first in the list
     */
    GridRecord getFirstRecord() {
        return (GridRecord) records.get(0);
    }

    /**
     * Get the horizontal coordinate system
     *
     * @return the horizontal coordinate system
     */
    GridHorizCoordSys getHorizCoordSys() {
        return hcs;
    }

    /**
     * Get the vertical coordinate system
     *
     * @return the vertical coordinate system
     */
    GridCoordSys getVertCoordSys() {
        return vcs;
    }

    /**
     * Get the vertical coordinate
     *
     * @return  the vertical coordinate
     */
    GridVertCoord getVertCoord() {
        return vc;
    }

    /**
     * Does this have a vertical dimension
     *
     * @return true if has a vertical dimension
     */
    boolean hasVert() {
        return hasVert;
    }

    /**
     * Set the variable name
     *
     * @param vname the variable name
     */
    void setVarName(String vname) {
        this.vname = vname;
    }

    /**
     * Set the vertical coordinate system
     *
     * @param vcs  the vertical coordinate system
     */
    void setVertCoordSys(GridCoordSys vcs) {
        this.vcs = vcs;
    }

    /**
     * Set the vertical coordinate
     *
     * @param vc  the vertical coordinate
     */
    void setVertCoord(GridVertCoord vc) {
        this.vc = vc;
    }

    /**
     * Set the time coordinate
     *
     * @param tcs the time coordinate
     */
    void setTimeCoord(GridTimeCoord tcs) {
        this.tcs = tcs;
    }

    /**
     * Get the number of vertical levels
     *
     * @return the number of vertical levels
     */
    int getVertNlevels() {
        return (vcs == null)
               ? vc.getNLevels()
               : vcs.getNLevels();
    }

    /**
     * Get the name of the vertical dimension
     *
     * @return the name of the vertical dimension
     */
    String getVertName() {
        return (vcs == null)
               ? vc.getVariableName()
               : vcs.getVerticalName();
    }

    /**
     * Get the name of the vertical level
     *
     * @return the name of the vertical level
     */
    String getVertLevelName() {
        return (vcs == null)
               ? vc.getLevelName()
               : vcs.getVerticalName();
    }

    /**
     * Is vertical used?
     *
     * @return true if vertical used
     */
    boolean getVertIsUsed() {
        return (vcs == null)
               ? !vc.dontUseVertical
               : !vcs.dontUseVertical;
    }

    /**
     * Get the index in the vertical for the particular grid
     *
     * @param p   grid to check
     *
     * @return the index
     */
    int getVertIndex(GridRecord p) {
        return (vcs == null)
               ? vc.getIndex(p)
               : vcs.getIndex(p);
    }

    /**
     * Get the number of times
     *
     * @return  the number of times
     */
    int getNTimes() {
        return (tcs == null)
               ? 1
               : tcs.getNTimes();
    }

    /* String getSearchName() {
      Parameter param = lookup.getParameter( firstRecord);
      String vname = lookup.getLevelName( firstRecord);
      return param.getDescription() + " @ " + vname;
    } */

    /**
     * Make the variable
     *
     * @param ncfile   netCDF file
     * @param g        group
     * @param useDesc  true use the description
     *
     * @return  the variable
     */
    Variable makeVariable(NetcdfFile ncfile, Group g, boolean useDesc) {

        nlevels = getVertNlevels();
        ntimes  = tcs.getNTimes();
        // TODO:  this is not used ?
        decimalScale = firstRecord.getDecimalScale();

        if (vname == null) {
            vname = NetcdfFile.createValidNetcdfObjectName(useDesc
                    ? desc
                    : name);
        }

        //vname = StringUtil.replace(vname, '-', "_"); // Done in dods server now
        Variable v = new Variable(ncfile, g, null, vname);
        v.setDataType(DataType.FLOAT);

        String dims = tcs.getName();
        if (getVertIsUsed()) {
            dims    = dims + " " + getVertName();
            hasVert = true;
        }

        if (hcs.isLatLon()) {
            dims = dims + " lat lon";
        } else {
            dims = dims + " y x";
        }

        v.setDimensions(dims);
        GridParameter param = lookup.getParameter(firstRecord);

        //TODO:  handle null units?
        //v.addAttribute(new Attribute("units", param.getUnit()));
        String unit = param.getUnit();
        if (unit == null) {
            unit = "";
        }
        v.addAttribute(new Attribute("units", unit));
        v.addAttribute(new Attribute("long_name",
                                     makeLongName(firstRecord, lookup)));
        v.addAttribute(
            new Attribute(
                "missing_value", new Float(lookup.getFirstMissingValue())));
        if ( !hcs.isLatLon()) {
            if (ucar.nc2.iosp.grib.GribServiceProvider.addLatLon) {
                v.addAttribute(new Attribute("coordinates", "lat lon"));
            }
            v.addAttribute(new Attribute("grid_mapping", hcs.getGridName()));
        }

        /*
         * TODO: figure out what to do with this - perhaps have a subclass
         * GribVariable that adds in the specific attributes.
         *
         if (lookup instanceof Grib1Lookup) {
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
          v.addAttribute(new Attribute("GRIB_level_type", new Integer(firstRecord.getLevelType1())));
         }
         */

        //if (pds.getTypeSecondFixedSurface() != 255 )
        //  v.addAttribute( new Attribute("GRIB2_type_of_second_fixed_surface", pds.getTypeSecondFixedSurfaceName()));

        /* String coordSysName = getVertIsUsed() ? getVertName() :
            (hcs.isLatLon() ? "latLonCoordSys" : "projectionCoordSys");
        v.addAttribute( new Attribute(_Coordinate.Systems", coordSysName)); */

        v.setSPobject(this);

        if (showRecords) {
            System.out.println("Variable " + getName());
        }

        recordTracker = new GridRecord[ntimes * nlevels];
        for (int i = 0; i < records.size(); i++) {
            GridRecord p = (GridRecord) records.get(i);
            if (showRecords) {
                System.out.println(" " + vc.getVariableName() + " (type="
                                   + p.getLevelType1() + ","
                                   + p.getLevelType2() + ")  value="
                                   + p.getLevelType1() + ","
                                   + p.getLevelType2()
                //+" # genProcess="+p.typeGenProcess);
                );
            }
            /* TODO:  figure out what to do here
            if (showGen && !isGrib1 && !p.typeGenProcess.equals("2"))
              System.out.println(" "+getName()+ " genProcess="+p.typeGenProcess);
            */

            int level = getVertIndex(p);
            if ( !getVertIsUsed() && (level > 0)) {
                log.warn("inconsistent level encoding=" + level);
                level = 0;  // inconsistent level encoding ??
            }
            int time = tcs.getIndex(p);
            // System.out.println("time="+time+" level="+level);
            if (level < 0) {
                log.warn("NOT FOUND record; level=" + level + " time= "
                         + time + " for " + getName() + " file="
                         + ncfile.getLocation() + "\n" + "   "
                         + getVertLevelName() + " (type=" + p.getLevelType1()
                         + "," + p.getLevelType2() + ")  value="
                         + p.getLevel1() + "," + p.getLevel2() + "\n");

                getVertIndex(p);  // allow breakpoint
                continue;
            }

            if (time < 0) {
                log.warn("NOT FOUND record; level=" + level + " time= "
                         + time + " for " + getName() + " file="
                         + ncfile.getLocation() + "\n" + " forecastTime= "
                         + p.getValidTimeOffset() + " date= "
                         + tcs.getValidTime(p) + "\n");

                tcs.getIndex(p);  // allow breakpoint
                continue;
            }

            int recno = time * nlevels + level;
            if (recordTracker[recno] == null) {
                recordTracker[recno] = p;
            } else {
                GridRecord q = recordTracker[recno];
                /* TODO:  huh?
                if (!p.typeGenProcess.equals(q.typeGenProcess)) {
                  log.warn("Duplicate record; level="+level+" time= "+time+" for "+getName()+" file="+ncfile.getLocation()+"\n"
                        +"   "+getVertLevelName()+" (type="+p.getLevelType1() + ","+p.getLevelType2()+")  value="+p.getLevel1() + ","+p.getLevel2()+"\n"
                        +"   already got (type="+q.getLevelType1() + ","+q.getLevelType2()+")  value="+q.getLevel1() + ","+q.getLevel2()+"\n"
                        //+"   gen="+p.typeGenProcess+"   "+q.typeGenProcess);
                        );
                }
                */
                recordTracker[recno] = p;  // replace it with latest one
                // System.out.println("   gen="+p.typeGenProcess+" "+q.typeGenProcess+"=="+lookup.getTypeGenProcessName(p));
            }
        }

        return v;

    }

    /**
     * Dump out the missing data
     */
    void dumpMissing() {
        //System.out.println("  " +name+" ntimes (across)= "+ ntimes+" nlevs (down)= "+ nlevels+":");
        System.out.println("  " + name);
        for (int j = 0; j < nlevels; j++) {
            System.out.print("   ");
            for (int i = 0; i < ntimes; i++) {
                boolean missing = recordTracker[i * nlevels + j] == null;
                System.out.print(missing
                                 ? "-"
                                 : "X");
            }
            System.out.println();
        }
    }

    /**
     * Dump out the missing data as a summary
     *
     * @return number of missing levels
     */
    int dumpMissingSummary() {
        if (nlevels == 1) {
            return 0;
        }

        int count = 0;
        int total = nlevels * ntimes;

        for (int i = 0; i < total; i++) {
            if (recordTracker[i] == null) {
                count++;
            }
        }

        System.out.println("  MISSING= " + count + "/" + total + " " + name);
        return count;
    }

    /**
     * Find the grid record for the time and level indices
     *
     * @param time   time index
     * @param level  level index
     *
     * @return  the record or null
     */
    public GridRecord findRecord(int time, int level) {
        return recordTracker[time * nlevels + level];
    }

    /**
     * Check for equality
     *
     * @param oo   object in question
     *
     * @return  true if they are equal
     */
    public boolean equals(Object oo) {
        if (this == oo) {
            return true;
        }
        if ( !(oo instanceof GridVariable)) {
            return false;
        }
        return hashCode() == oo.hashCode();
    }

    /**
     * Get the name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the parameter name
     *
     * @return description
     */
    public String getParamName() {
        return desc;
    }

    /**
     * Get the decimal scale
     *
     * @return  decimal scale
     */
    public int getDecimalScale() {
        return decimalScale;
    }

    /**
     * Override Object.hashCode() to implement equals.
     *
     * @return equals;
     */
    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result   = 37 * result + name.hashCode();
            result   += 37 * result + firstRecord.getLevelType1();
            result   += 37 * result + hcs.getID().hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    /** hash code */
    private volatile int hashCode = 0;


    /**
     * Dump this variable
     *
     * @return the variable
     */
    public String dump() {
        DateFormatter formatter = new DateFormatter();
        StringBuilder  sbuff     = new StringBuilder();
        sbuff.append(name + " " + records.size() + "\n");
        for (int i = 0; i < records.size(); i++) {
            GridRecord record = (GridRecord) records.get(i);
            sbuff.append(" level = " + record.getLevelType1() + " "
                         + record.getLevel1());
            if (null != record.getValidTime()) {
                sbuff.append(
                    " time = "
                    + formatter.toDateTimeString(record.getValidTime()));
            }
            sbuff.append("\n");
        }
        return sbuff.toString();
    }

    /**
     * Make a long name for the variable
     *
     * @param gr   grid record
     * @param lookup  lookup table
     *
     * @return long variable name
     */
    private String makeLongName(GridRecord gr, GridTableLookup lookup) {
        GridParameter param = lookup.getParameter(gr);
        //String        levelName = GridIndexToNC.makeLevelName(gr, lookup);
        String levelName = lookup.getLevelDescription(gr);
        return (levelName.length() == 0)
               ? param.getDescription()
               : param.getDescription() + " @ " + levelName;
    }

}

