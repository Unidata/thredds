/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
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



package ucar.nc2.iosp.grid;


import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.fmr.FmrcCoordSys;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;

import java.io.*;

import java.util.*;


/**
 * Create a Netcdf File from a GridIndex
 *
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class GridIndexToNC {

    /** logger */
    static private org.slf4j.Logger logger =
        org.slf4j.LoggerFactory.getLogger(GridIndexToNC.class);

    /** map of horizontal coordinate systems */
    private HashMap hcsHash = new HashMap(10);  // GridHorizCoordSys

    /** date formattter */
    private DateFormatter formatter = new DateFormatter();

    /** debug flag */
    private boolean debug = false;

    /** flag for using GridParameter description for variable names */
    private boolean useDescriptionForVariableName = true;

    //TODO:  how to make format specific names if these are static
    /**
     * Make the level name
     *
     * @param gr   grid record
     * @param lookup  lookup table
     *
     * @return name for the level
     */
    public static String makeLevelName(GridRecord gr, GridTableLookup lookup) {
        String  vname   = lookup.getLevelName(gr);
        boolean isGrib1 = true;   // same for GEMPAK
        if (isGrib1) {
            return vname;
        }

        // for grib2, we need to add the layer to disambiguate
        return lookup.isLayer(gr)
               ? vname + "_layer"
               : vname;
    }

    /**
     * Make the variable name
     *
     * @param gr   grid record
     * @param lookup  lookup table
     *
     * @return variable name
     */
    public String makeVariableName(GridRecord gr, GridTableLookup lookup) {
        GridParameter param     = lookup.getParameter(gr);
        String        levelName = makeLevelName(gr, lookup);
        String        paramName = (useDescriptionForVariableName)
            ? param.getDescription()
            : param.getName();
        return (levelName.length() == 0)
               ? paramName
               : paramName + "_" + levelName;
    }

    /**
     * TODO:  moved to GridVariable = made sense since it knows what it is
     * Make a long name for the variable
     *
     * @param gr   grid record
     * @param lookup  lookup table
     *
     * @return long variable name
    public static String makeLongName(GridRecord gr, GridTableLookup lookup) {
        GridParameter param     = lookup.getParameter(gr);
        String        levelName = makeLevelName(gr, lookup);
        return (levelName.length() == 0)
               ? param.getDescription()
               : param.getDescription() + " @ " + makeLevelName(gr, lookup);
    }
     */

    /**
     * Fill in the netCDF file
     *
     * @param index  grid index
     * @param lookup lookup table
     * @param version  version of data
     * @param ncfile   netCDF file to fill in
     * @param fmrcCoordSys  forecast model run CS
     * @param cancelTask    cancel task
     *
     * @throws IOException Problem reading from the file
     */
    public void open(GridIndex index, GridTableLookup lookup, int version,
              NetcdfFile ncfile, FmrcCoordSys fmrcCoordSys,
              CancelTask cancelTask)
            throws IOException {

        // create the HorizCoord Systems : one for each gds
        List    hcsList    = index.getHorizCoordSys();
        boolean needGroups = (hcsList.size() > 1);
        for (int i = 0; i < hcsList.size(); i++) {
            GridDefRecord gdsIndex = (GridDefRecord) hcsList.get(i);

            Group         g        = null;
            if (needGroups) {
                //g = new Group(ncfile, null, "proj" + i);
                g = new Group(ncfile, null, gdsIndex.getGroupName());
                ncfile.addGroup(null, g);
            }

            // (GridDefRecord gdsIndex, String grid_name, String shape_name, Group g)
            GridHorizCoordSys hcs = new GridHorizCoordSys(gdsIndex, lookup,
                                        g);

            hcsHash.put(gdsIndex.getParam(gdsIndex.GDS_KEY), hcs);
        }

        // run through each record
        GridRecord firstRecord = null;
        List       records     = index.getGridRecords();
        if (GridServiceProvider.debugOpen) {
            System.out.println(" number of products = " + records.size());
        }
        for (int i = 0; i < records.size(); i++) {
            GridRecord gribRecord = (GridRecord) records.get(i);
            if (firstRecord == null) {
                firstRecord = gribRecord;
            }

            GridHorizCoordSys hcs = (GridHorizCoordSys) hcsHash.get(
                                        gribRecord.getGridDefRecordId());
            String name = makeVariableName(gribRecord, lookup);
            GridVariable pv = (GridVariable) hcs.varHash.get(name);  // combo gds, param name and level name
            if (null == pv) {
                String pname =
                    lookup.getParameter(gribRecord).getDescription();
                pv = new GridVariable(name, pname, hcs, lookup);
                hcs.varHash.put(name, pv);

                // keep track of all products with same parameter name
                ArrayList plist = (ArrayList) hcs.productHash.get(pname);
                if (null == plist) {
                    plist = new ArrayList();
                    hcs.productHash.put(pname, plist);
                }
                plist.add(pv);
            }
            pv.addProduct(gribRecord);
        }

        // global stuff
        ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.0"));

        /* TODO:  figure out what to do with these
        String creator = lookup.getFirstCenterName() + " subcenter = "+lookup.getFirstSubcenterId();
        if (creator != null)
          ncfile.addAttribute(null, new Attribute("Originating_center", creator) );
        String genType = lookup.getTypeGenProcessName( firstRecord);
        if (genType != null)
          ncfile.addAttribute(null, new Attribute("Generating_Process_or_Model", genType) );
        if (null != lookup.getFirstProductStatusName())
          ncfile.addAttribute(null, new Attribute("Product_Status", lookup.getFirstProductStatusName()) );
        ncfile.addAttribute(null, new Attribute("Product_Type", lookup.getFirstProductTypeName()) );
        */

        // dataset discovery
        ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.GRID.toString()));
        String gridType = lookup.getGridType();
        //ncfile.addAttribute(null, new Attribute("creator_name", creator));
        ncfile.addAttribute(null, new Attribute("file_format", gridType+"-"+version));
        ncfile.addAttribute(null,
                            new Attribute("location", ncfile.getLocation()));
        ncfile.addAttribute(
            null,
            new Attribute(
                "history", "Direct read of "+gridType+" into NetCDF-Java 2.2 API"));

        ncfile.addAttribute(
            null,
            new Attribute(
                _Coordinate.ModelRunDate,
                formatter.toDateTimeStringISO(lookup.getFirstBaseTime())));

        if (fmrcCoordSys != null) {
            makeDefinedCoordSys(ncfile, lookup, fmrcCoordSys);
            /* else if (GribServiceProvider.useMaximalCoordSys)
              makeMaximalCoordSys(ncfile, lookup, cancelTask); */
        } else {
            makeDenseCoordSys(ncfile, lookup, cancelTask);
        }

        if (GridServiceProvider.debugMissing) {
            int      count   = 0;
            Iterator iterHcs = hcsHash.values().iterator();
            while (iterHcs.hasNext()) {
                GridHorizCoordSys hcs = (GridHorizCoordSys) iterHcs.next();
                ArrayList gribvars    = new ArrayList(hcs.varHash.values());
                for (int i = 0; i < gribvars.size(); i++) {
                    GridVariable gv = (GridVariable) gribvars.get(i);
                    count += gv.dumpMissingSummary();
                }
            }
            System.out.println(" total missing= " + count);
        }

        if (GridServiceProvider.debugMissingDetails) {
            Iterator iterHcs = hcsHash.values().iterator();
            while (iterHcs.hasNext()) {  // loop over HorizCoordSys
                GridHorizCoordSys hcs = (GridHorizCoordSys) iterHcs.next();
                System.out.println("******** Horiz Coordinate= "
                                   + hcs.getGridName());

                String    lastVertDesc = null;
                ArrayList gribvars     = new ArrayList(hcs.varHash.values());
                Collections.sort(gribvars,
                                 new CompareGridVariableByVertName());

                for (int i = 0; i < gribvars.size(); i++) {
                    GridVariable gv       = (GridVariable) gribvars.get(i);
                    String       vertDesc = gv.getVertName();
                    if ( !vertDesc.equals(lastVertDesc)) {
                        System.out.println("---Vertical Coordinate= "
                                           + vertDesc);
                        lastVertDesc = vertDesc;
                    }
                    gv.dumpMissing();
                }
            }
        }

    }

    /**
     * Make coordinate system without missing data - means that we
     * have to make a coordinate axis for each unique set
     * of time or vertical levels.
     *
     * @param ncfile   netCDF file
     * @param lookup   lookup  table
     * @param cancelTask  cancel task
     *
     * @throws IOException problem reading file
     */
    private void makeDenseCoordSys(NetcdfFile ncfile, GridTableLookup lookup,
                                   CancelTask cancelTask)
            throws IOException {

        ArrayList timeCoords = new ArrayList();
        ArrayList vertCoords = new ArrayList();

        // loop over HorizCoordSys
        Iterator iterHcs = hcsHash.values().iterator();
        while (iterHcs.hasNext()) {
            GridHorizCoordSys hcs = (GridHorizCoordSys) iterHcs.next();

            // loop over GridVariables in the HorizCoordSys
            // create the time and vertical coordinates
            Iterator iter = hcs.varHash.values().iterator();
            while (iter.hasNext()) {
                GridVariable pv         = (GridVariable) iter.next();
                List         recordList = pv.getRecords();
                GridRecord   record     = (GridRecord) recordList.get(0);
                String       vname      = makeLevelName(record, lookup);

                // look to see if vertical already exists
                GridVertCoord useVertCoord = null;
                for (int i = 0; i < vertCoords.size(); i++) {
                    GridVertCoord gvcs = (GridVertCoord) vertCoords.get(i);
                    if (vname.equals(gvcs.getLevelName())) {
                        if (gvcs.matchLevels(recordList)) {  // must have the same levels
                            useVertCoord = gvcs;
                        }
                    }
                }
                if (useVertCoord == null) {  // nope, got to create it
                    useVertCoord = new GridVertCoord(recordList, vname,
                            lookup);
                    vertCoords.add(useVertCoord);
                }
                pv.setVertCoord(useVertCoord);

                // look to see if time coord already exists
                GridTimeCoord useTimeCoord = null;
                for (int i = 0; i < timeCoords.size(); i++) {
                    GridTimeCoord gtc = (GridTimeCoord) timeCoords.get(i);
                    if (gtc.matchLevels(recordList)) {  // must have the same levels
                        useTimeCoord = gtc;
                    }
                }
                if (useTimeCoord == null) {  // nope, got to create it
                    useTimeCoord = new GridTimeCoord(recordList, lookup);
                    timeCoords.add(useTimeCoord);
                }
                pv.setTimeCoord(useTimeCoord);
            }

            //// assign time coordinate names
            // find time dimensions with largest length
            GridTimeCoord tcs0     = null;
            int           maxTimes = 0;
            for (int i = 0; i < timeCoords.size(); i++) {
                GridTimeCoord tcs = (GridTimeCoord) timeCoords.get(i);
                if (tcs.getNTimes() > maxTimes) {
                    tcs0     = tcs;
                    maxTimes = tcs.getNTimes();
                }
            }

            // add time dimensions, give time dimensions unique names
            int seqno = 1;
            for (int i = 0; i < timeCoords.size(); i++) {
                GridTimeCoord tcs = (GridTimeCoord) timeCoords.get(i);
                if (tcs != tcs0) {
                    tcs.setSequence(seqno++);
                }
                tcs.addDimensionsToNetcdfFile(ncfile, hcs.getGroup());
            }

            // add x, y dimensions
            hcs.addDimensionsToNetcdfFile(ncfile);

            // add vertical dimensions, give them unique names
            Collections.sort(vertCoords);
            int    vcIndex  = 0;
            String listName = null;
            int    start    = 0;
            for (vcIndex = 0; vcIndex < vertCoords.size(); vcIndex++) {
                GridVertCoord gvcs  = (GridVertCoord) vertCoords.get(vcIndex);
                String        vname = gvcs.getLevelName();
                if (listName == null) {
                    listName = vname;  // initial
                }

                if ( !vname.equals(listName)) {
                    makeVerticalDimensions(vertCoords.subList(start,
                            vcIndex), ncfile, hcs.getGroup());
                    listName = vname;
                    start    = vcIndex;
                }
            }
            makeVerticalDimensions(vertCoords.subList(start, vcIndex),
                                   ncfile, hcs.getGroup());

            // create a variable for each entry, but check for other products with same desc
            // to disambiguate by vertical coord
            ArrayList products = new ArrayList(hcs.productHash.values());
            Collections.sort(products, new CompareGridVariableListByName());
            for (int i = 0; i < products.size(); i++) {
                ArrayList plist = (ArrayList) products.get(i);  // all the products with the same name

                if (plist.size() == 1) {
                    GridVariable pv = (GridVariable) plist.get(0);
                    Variable v = pv.makeVariable(ncfile, hcs.getGroup(),
                                     useDescriptionForVariableName);
                    ncfile.addVariable(hcs.getGroup(), v);

                } else {

                    Collections.sort(
                        plist, new CompareGridVariableByNumberVertLevels());
                    /* find the one with the most vertical levels
                    int maxLevels = 0;
                    GridVariable maxV = null;
                    for (int j = 0; j < plist.size(); j++) {
                      GridVariable pv = (GridVariable) plist.get(j);
                      if (pv.getVertCoord().getNLevels() > maxLevels) {
                        maxLevels = pv.getVertCoord().getNLevels();
                        maxV = pv;
                      }
                    } */
                    // finally, add the variables
                    for (int k = 0; k < plist.size(); k++) {
                        GridVariable pv = (GridVariable) plist.get(k);
                        //int nlevels = pv.getVertNlevels();
                        //boolean useDesc = (k == 0) && (nlevels > 1); // keep using the level name if theres only one level
                        // TODO: is there a better way to do this?
                        boolean useDesc = (k==0 && useDescriptionForVariableName);
                        ncfile.addVariable(hcs.getGroup(),
                                           pv.makeVariable(ncfile,
                                               hcs.getGroup(), useDesc));
                    }
                }  // multipe vertical levels

            }      // create variable

            // add coordinate variables at the end
            for (int i = 0; i < timeCoords.size(); i++) {
                GridTimeCoord tcs = (GridTimeCoord) timeCoords.get(i);
                tcs.addToNetcdfFile(ncfile, hcs.getGroup());
            }
            hcs.addToNetcdfFile(ncfile);
            for (int i = 0; i < vertCoords.size(); i++) {
                GridVertCoord gvcs = (GridVertCoord) vertCoords.get(i);
                gvcs.addToNetcdfFile(ncfile, hcs.getGroup());
            }

        }          // loop over hcs


    }


    /**
     * Make a vertical dimensions
     *
     * @param vertCoordList vertCoords all with the same name
     * @param ncfile  netCDF file to add to
     * @param group   group in ncfile
     */
    private void makeVerticalDimensions(List vertCoordList,
                                        NetcdfFile ncfile, Group group) {
        // find biggest vert coord
        GridVertCoord gvcs0     = null;
        int           maxLevels = 0;
        for (int i = 0; i < vertCoordList.size(); i++) {
            GridVertCoord gvcs = (GridVertCoord) vertCoordList.get(i);
            if (gvcs.getNLevels() > maxLevels) {
                gvcs0     = gvcs;
                maxLevels = gvcs.getNLevels();
            }
        }

        int seqno = 1;
        for (int i = 0; i < vertCoordList.size(); i++) {
            GridVertCoord gvcs = (GridVertCoord) vertCoordList.get(i);
            if (gvcs != gvcs0) {
                gvcs.setSequence(seqno++);
            }
            gvcs.addDimensionsToNetcdfFile(ncfile, group);
        }
    }


    /**
     * Make coordinate system from a Definition object
     *
     * @param ncfile  netCDF file to add to
     * @param lookup  lookup table
     * @param fmr     FmrcCoordSys
     *
     * @throws IOException problem reading from file
     */
    private void makeDefinedCoordSys(NetcdfFile ncfile,
                                     GridTableLookup lookup, FmrcCoordSys fmr)
            throws IOException {

        ArrayList timeCoords      = new ArrayList();
        ArrayList vertCoords      = new ArrayList();
        ArrayList removeVariables = new ArrayList();

        // loop over HorizCoordSys
        Iterator iterHcs = hcsHash.values().iterator();
        while (iterHcs.hasNext()) {
            GridHorizCoordSys hcs = (GridHorizCoordSys) iterHcs.next();

            // loop over GridVariables in the HorizCoordSys
            // create the time and vertical coordinates
            Iterator iterKey = hcs.varHash.keySet().iterator();
            while (iterKey.hasNext()) {
                String       key    = (String) iterKey.next();
                GridVariable pv     = (GridVariable) hcs.varHash.get(key);
                GridRecord   record = pv.getFirstRecord();

                // we dont know the name for sure yet, so have to try several
                String searchName = findVariableName(ncfile, record, lookup,
                                        fmr);
                System.out.println("Search name = " + searchName);
                if (searchName == null) {  // cant find - just remove
                    System.out.println("removing " + searchName);
                    removeVariables.add(key);  // cant remove (concurrentModException) so save for later
                    continue;
                }
                pv.setVarName(searchName);

                // get the vertical coordinate for this variable, if it exists
                FmrcCoordSys.VertCoord vc_def =
                    fmr.findVertCoordForVariable(searchName);
                if (vc_def != null) {
                    String vc_name = vc_def.getName();

                    // look to see if GridVertCoord already made
                    GridVertCoord useVertCoord = null;
                    for (int i = 0; i < vertCoords.size(); i++) {
                        GridVertCoord gvcs =
                            (GridVertCoord) vertCoords.get(i);
                        if (vc_name.equals(gvcs.getLevelName())) {
                            useVertCoord = gvcs;
                        }
                    }
                    if (useVertCoord == null) {  // nope, got to create it
                        useVertCoord = new GridVertCoord(record, vc_name,
                                lookup, vc_def.getValues1(),
                                vc_def.getValues2());
                        useVertCoord.addDimensionsToNetcdfFile(ncfile,
                                hcs.getGroup());
                        vertCoords.add(useVertCoord);
                    }
                    pv.setVertCoord(useVertCoord);

                } else {
                    pv.setVertCoord(new GridVertCoord(searchName));  // fake
                }

                // get the time coordinate for this variable
                FmrcCoordSys.TimeCoord tc_def =
                    fmr.findTimeCoordForVariable(searchName,
                        lookup.getFirstBaseTime());
                String tc_name = tc_def.getName();

                // look to see if GridTimeCoord already made
                GridTimeCoord useTimeCoord = null;
                for (int i = 0; i < timeCoords.size(); i++) {
                    GridTimeCoord gtc = (GridTimeCoord) timeCoords.get(i);
                    if (tc_name.equals(gtc.getName())) {
                        useTimeCoord = gtc;
                    }
                }
                if (useTimeCoord == null) {  // nope, got to create it
                    useTimeCoord = new GridTimeCoord(tc_name,
                            tc_def.getOffsetHours(), lookup);
                    useTimeCoord.addDimensionsToNetcdfFile(ncfile,
                            hcs.getGroup());
                    timeCoords.add(useTimeCoord);
                }
                pv.setTimeCoord(useTimeCoord);
            }

            // any need to be removed?
            for (int i = 0; i < removeVariables.size(); i++) {
                String key = (String) removeVariables.get(i);
                hcs.varHash.remove(key);
            }

            // add x, y dimensions
            hcs.addDimensionsToNetcdfFile(ncfile);

            // create a variable for each entry
            Iterator iter2 = hcs.varHash.values().iterator();
            while (iter2.hasNext()) {
                GridVariable pv = (GridVariable) iter2.next();
                Variable     v = pv.makeVariable(ncfile, hcs.getGroup(),
                                     true);
                ncfile.addVariable(hcs.getGroup(), v);
            }

            // add coordinate variables at the end
            for (int i = 0; i < timeCoords.size(); i++) {
                GridTimeCoord tcs = (GridTimeCoord) timeCoords.get(i);
                tcs.addToNetcdfFile(ncfile, hcs.getGroup());
            }
            hcs.addToNetcdfFile(ncfile);
            for (int i = 0; i < vertCoords.size(); i++) {
                GridVertCoord gvcs = (GridVertCoord) vertCoords.get(i);
                gvcs.addToNetcdfFile(ncfile, hcs.getGroup());
            }

        }  // loop over hcs

        if (debug) {
            System.out.println("GridIndexToNC.makeDefinedCoordSys for "
                               + ncfile.getLocation());
        }

    }

    /**
     * Find the variable name for the grid
     *
     * @param ncfile   netCDF file
     * @param gr       grid record
     * @param lookup   lookup table
     * @param fmr      FmrcCoordSys
     *
     * @return  name for the grid
     */
    private String findVariableName(NetcdfFile ncfile, GridRecord gr,
                                    GridTableLookup lookup,
                                    FmrcCoordSys fmr) {
        // first lookup with name & vert name
        String name =
            AbstractIOServiceProvider.createValidNetcdfObjectName(makeVariableName(gr,
                lookup));
        if (fmr.hasVariable(name)) {
            return name;
        }

        // now try just the name
        String pname = AbstractIOServiceProvider.createValidNetcdfObjectName(
                           lookup.getParameter(gr).getDescription());
        if (fmr.hasVariable(pname)) {
            return pname;
        }

        logger.warn(
            "GridIndexToNC: FmrcCoordSys does not have the variable named ="
            + name + " or " + pname + " for file " + ncfile.getLocation());

        return null;
    }

    /**
     * Comparable object for grid variable
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class CompareGridVariableListByName implements Comparator {

        /**
         * Compare the two lists of names
         *
         * @param o1  first list
         * @param o2  second list
         *
         * @return comparison
         */
        public int compare(Object o1, Object o2) {
            ArrayList    list1 = (ArrayList) o1;
            ArrayList    list2 = (ArrayList) o2;

            GridVariable gv1   = (GridVariable) list1.get(0);
            GridVariable gv2   = (GridVariable) list2.get(0);

            return gv1.getName().compareToIgnoreCase(gv2.getName());
        }
    }

    /**
     * Comparator for grids by vertical variable name
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class CompareGridVariableByVertName implements Comparator {

        /**
         * Compare the two lists of names
         *
         * @param o1  first list
         * @param o2  second list
         *
         * @return comparison
         */
        public int compare(Object o1, Object o2) {
            GridVariable gv1 = (GridVariable) o1;
            GridVariable gv2 = (GridVariable) o2;

            return gv1.getVertName().compareToIgnoreCase(gv2.getVertName());
        }
    }

    /**
     * Comparator for grid variables by number of vertical levels
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class CompareGridVariableByNumberVertLevels implements Comparator {

        /**
         * Compare the two lists of names
         *
         * @param o1  first list
         * @param o2  second list
         *
         * @return comparison
         */
        public int compare(Object o1, Object o2) {
            GridVariable gv1 = (GridVariable) o1;
            GridVariable gv2 = (GridVariable) o2;

            int          n1  = gv1.getVertCoord().getNLevels();
            int          n2  = gv2.getVertCoord().getNLevels();

            if (n1 == n2) {  // break ties for consistency
                return gv1.getVertCoord().getLevelName().compareTo(
                    gv2.getVertCoord().getLevelName());
            } else {
                return n2 - n1;  // highest number first
            }
        }
    }

    /**
     * Should use the description for the variable name.
     * @param value  false to use name instead of description
     */
    public void setUseDescriptionForVariableName(boolean value) {
        useDescriptionForVariableName = value;
    }

}

