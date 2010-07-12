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


package ucar.nc2.iosp.grid;


import ucar.nc2.*;
import ucar.nc2.iosp.mcidas.McIDASLookup;
import ucar.nc2.iosp.gempak.GempakLookup;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.fmr.FmrcCoordSys;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;
import ucar.grib.grib2.Grib2GridTableLookup;
import ucar.grib.grib1.Grib1GridTableLookup;
import ucar.unidata.util.StringUtil;
import ucar.grid.*;

import java.io.*;

import java.util.*;


/**
 * Create a Netcdf File from a GridIndex
 *
 * @author caron
 */
public class GridIndexToNC {

  /**
   * logger
   */
  static private org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(GridIndexToNC.class);

  /**
   * map of horizontal coordinate systems
   */
  private Map<String,GridHorizCoordSys> hcsHash = new HashMap<String,GridHorizCoordSys>(10);  // GridHorizCoordSys

  /**
   * date formattter
   */
  private DateFormatter formatter = new DateFormatter();

  /**
   * debug flag
   */
  private boolean debug = false;

  /**
   * flag for using GridParameter description for variable names
   */
  //private boolean useDescriptionForVariableName = true;

  /**
   * Make the level name
   *
   * @param gr     grid record
   * @param lookup lookup table
   * @return name for the level
   */
  static String makeLevelName(GridRecord gr, GridTableLookup lookup) {
    // for grib2, we need to add the layer to disambiguate
    if ( lookup instanceof Grib2GridTableLookup ) {
      String vname = lookup.getLevelName(gr);
      return lookup.isLayer(gr) ? vname + "_layer" : vname;
    } else {
      return lookup.getLevelName(gr);  // GEMPAK, GRIB1
    }
  }

  /**
   * Get suffix name of variable if it exists
   *
   * @param gr     grid record
   * @param lookup lookup table
   * @return name of the suffix, ensemble, probability, error, etc
   */
  static String makeSuffixName(GridRecord gr, GridTableLookup lookup) {
    if ( ! (lookup instanceof Grib2GridTableLookup) )
      return "";
    Grib2GridTableLookup g2lookup = (Grib2GridTableLookup) lookup;
    return g2lookup.makeSuffix( gr );
  }

  /**
   * Make the variable name with suffix and level if present
   *
   * @param gr     grid record
   * @param lookup lookup table
   * @param addLevel add level name if there is one
   * @param addEnsemble  add ensemble name if there is one
   * @return variable name
   */
  public static String makeVariableName(GridRecord gr, GridTableLookup lookup, boolean addLevel, boolean addEnsemble) {
    GridParameter param = lookup.getParameter(gr);
    String paramName = param.getDescription();

    if (addEnsemble) {
      String suffixName = makeSuffixName(gr, lookup);
      paramName = (suffixName.length() == 0) ? paramName : paramName + "_" + suffixName;
    }

    if (addLevel) {
      String levelName = makeLevelName(gr, lookup);
      if (levelName.length() != 0) {
        paramName += "_" + levelName;
      }
    }

    return paramName;
  }

  /**
   * Fill in the netCDF file
   *
   * @param index        grid index
   * @param lookup       lookup table
   * @param version      version of data
   * @param ncfile       netCDF file to fill in
   * @param fmrcCoordSys forecast model run CS
   * @param cancelTask   cancel task
   * @throws IOException Problem reading from the file
   */
  public void open(GridIndex index, GridTableLookup lookup, int version,
                   NetcdfFile ncfile, FmrcCoordSys fmrcCoordSys, CancelTask cancelTask) throws IOException {

    // create the HorizCoord Systems : one for each gds
    List<GridDefRecord> hcsList = index.getHorizCoordSys();
    boolean needGroups = (hcsList.size() > 1);
    for (GridDefRecord gds : hcsList) {
      Group g = null;
      if (needGroups) {
        g = new Group(ncfile, null, gds.getGroupName());
        ncfile.addGroup(null, g);
      }
      // (GridDefRecord gdsIndex, String grid_name, String shape_name, Group g)
      GridHorizCoordSys hcs = new GridHorizCoordSys(gds, lookup, g);
      hcsHash.put(gds.getParam(GridDefRecord.GDS_KEY), hcs);
    }

    // run through each record
    GridRecord firstRecord = null;
    List<GridRecord> records = index.getGridRecords();
    if (GridServiceProvider.debugOpen) {
      System.out.println(" number of products = " + records.size());
    }
    for (GridRecord gribRecord : records) {
      if (firstRecord == null)
        firstRecord = gribRecord;

      GridHorizCoordSys hcs =  hcsHash.get(gribRecord.getGridDefRecordId());
      String name = makeVariableName(gribRecord, lookup, true, true);
      // combo gds, param name and level name
      GridVariable pv = (GridVariable) hcs.varHash.get(name);
      if (null == pv) {
        pv = new GridVariable(name, hcs, lookup);
        hcs.varHash.put(name, pv);
        //System.out.printf("Add name=%s pname=%s%n", name, pname);

        // keep track of all products with same parameter name + suffix == "simple name"
        // String pname = lookup.getParameter(gribRecord).getDescription(); // dont use plain old parameter name anymore 6/3/2010 jc
        String simpleName = makeVariableName(gribRecord, lookup, false, true); // LOOK may not be a good idea
        List<GridVariable> plist = hcs.productHash.get(simpleName);
        if (null == plist) {
          plist = new ArrayList<GridVariable>();
          hcs.productHash.put(simpleName, plist);
        }
        plist.add(pv);
      } else if ( lookup instanceof Grib2GridTableLookup ) {
        Grib2GridTableLookup g2lookup = (Grib2GridTableLookup) lookup;
        // check for non interval pv and interval record which needs a interval pv
        if( ! pv.isInterval() && g2lookup.isInterval( gribRecord ) ) {
          // make a interval variable
          String interval = name +"_interval";
          pv = (GridVariable) hcs.varHash.get(interval);
          if (null == pv) {
            pv = new GridVariable(interval, hcs, lookup);
            hcs.varHash.put(interval, pv);
            String simpleName = makeVariableName(gribRecord, lookup, false, true); // LOOK may not be a good idea
            List<GridVariable> plist = hcs.productHash.get(simpleName);
            if (null == plist) {
              plist = new ArrayList<GridVariable>();
              hcs.productHash.put(simpleName, plist);
            }
            plist.add(pv);
          }
        } else if ( pv.isInterval() && !g2lookup.isInterval( gribRecord )  ) {
          // make a non-interval variable
          logger.info( "Non-Interval records for %s%n", pv.getName());
            continue;
        }
      }

      pv.addProduct(gribRecord);
    }

    // global CF Conventions
    // Conventions attribute change must be in sync with CDM code
    ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.4"));

    String center = null;
    String subcenter = null;
    if ( lookup instanceof Grib2GridTableLookup ) {
      Grib2GridTableLookup g2lookup = (Grib2GridTableLookup) lookup;
      center = g2lookup.getFirstCenterName();
      subcenter = g2lookup.getFirstSubcenterName();
      ncfile.addAttribute(null, new Attribute("Originating_center", center));
      //if (subcenter != null)
      //  ncfile.addAttribute(null, new Attribute("Originating_subcenter", subcenter));

      String genType = g2lookup.getTypeGenProcessName(firstRecord);
      if (genType != null)
        ncfile.addAttribute(null, new Attribute("Generating_Process_or_Model", genType));
      if (null != g2lookup.getFirstProductStatusName())
        ncfile.addAttribute(null, new Attribute("Product_Status", g2lookup.getFirstProductStatusName()));
      ncfile.addAttribute(null, new Attribute("Product_Type", g2lookup.getFirstProductTypeName()));

    } else if ( lookup instanceof Grib1GridTableLookup ) {
      Grib1GridTableLookup g1lookup = (Grib1GridTableLookup) lookup;
      center = g1lookup.getFirstCenterName();
      subcenter = g1lookup.getFirstSubcenterName();
      ncfile.addAttribute(null, new Attribute("Originating_center", center));
      if (subcenter != null)
      ncfile.addAttribute(null, new Attribute("Originating_subcenter", subcenter));

      String genType = g1lookup.getTypeGenProcessName(firstRecord);
      if (genType != null)
        ncfile.addAttribute(null, new Attribute("Generating_Process_or_Model", genType));
      if (null != g1lookup.getFirstProductStatusName())
        ncfile.addAttribute(null, new Attribute("Product_Status", g1lookup.getFirstProductStatusName()));
      ncfile.addAttribute(null, new Attribute("Product_Type", g1lookup.getFirstProductTypeName()));
    }

    // CF Global attributes
    ncfile.addAttribute(null, new Attribute("title", lookup.getTitle() +" at "+
    formatter.toDateTimeStringISO(lookup.getFirstBaseTime()) ));
    if ( lookup.getInstitution() != null)
      ncfile.addAttribute(null, new Attribute("institution", lookup.getInstitution()));
    String source = lookup.getSource();
    if ( source != null && ! source.startsWith( "Unknown"))
      ncfile.addAttribute(null, new Attribute("source", source));
    String now = formatter.toDateTimeStringISO( Calendar.getInstance().getTime());
    ncfile.addAttribute(null, new Attribute("history", now +" Direct read of "+
        lookup.getGridType() +" into NetCDF-Java 4 API"));
    if ( lookup.getComment() != null)
      ncfile.addAttribute(null, new Attribute("comment", lookup.getComment()));

    // dataset discovery
    //if ( center != null)
    //  ncfile.addAttribute(null, new Attribute("center_name", center));

    // CDM attributes
    ncfile.addAttribute(null, new Attribute("CF:feature_type", FeatureType.GRID.toString()));
    ncfile.addAttribute(null, new Attribute("file_format", lookup.getGridType()));
    ncfile.addAttribute(null,
        new Attribute("location", ncfile.getLocation()));
    ncfile.addAttribute(null, new Attribute(_Coordinate.ModelRunDate,
            formatter.toDateTimeStringISO(lookup.getFirstBaseTime())));

    if (fmrcCoordSys != null) {
      makeDefinedCoordSys(ncfile, lookup, fmrcCoordSys);
    } else {
      makeDenseCoordSys(ncfile, lookup, cancelTask);
    }

    if (GridServiceProvider.debugMissing) {
      int count = 0;
      Collection<GridHorizCoordSys> hcset = hcsHash.values();
      for (GridHorizCoordSys hcs : hcset) {
        List<GridVariable> gribvars = new ArrayList<GridVariable>(hcs.varHash.values());
        for (GridVariable gv : gribvars) {
          count += gv.dumpMissingSummary();
        }
      }
      System.out.println(" total missing= " + count);
    }

    if (GridServiceProvider.debugMissingDetails) {
      Collection<GridHorizCoordSys> hcset = hcsHash.values();
      for (GridHorizCoordSys hcs : hcset) {
        System.out.println("******** Horiz Coordinate= " + hcs.getGridName());

        String lastVertDesc = null;
        List<GridVariable> gribvars = new ArrayList<GridVariable>(hcs.varHash.values());
        Collections.sort(gribvars, new CompareGridVariableByVertName());

        for (GridVariable gv : gribvars) {
          String vertDesc = gv.getVertName();
          if (!vertDesc.equals(lastVertDesc)) {
            System.out.println("---Vertical Coordinate= " + vertDesc);
            lastVertDesc = vertDesc;
          }
          gv.dumpMissing();
        }
      }
    }

    // clean out stuff we dont need anymore
    //for (GridHorizCoordSys ghcs : hcsHash.values()) {
    //  ghcs.empty();
    //}
  }

  // debugging
  public GridHorizCoordSys getHorizCoordSys(GridRecord gribRecord) {
    return hcsHash.get(gribRecord.getGridDefRecordId());
  }

  public Map<String,GridHorizCoordSys> getHorizCoordSystems() {
    return hcsHash;
  }

  /**
   * Make coordinate system without missing data - means that we
   * have to make a coordinate axis for each unique set
   * of time or vertical levels.
   *
   * @param ncfile     netCDF file
   * @param lookup     lookup  table
   * @param cancelTask cancel task
   * @throws IOException problem reading file
   */
  private void makeDenseCoordSys(NetcdfFile ncfile, GridTableLookup lookup, CancelTask cancelTask) throws IOException {
    List<GridTimeCoord> timeCoords = new ArrayList<GridTimeCoord>();
    List<GridVertCoord> vertCoords = new ArrayList<GridVertCoord>();
    List<GridEnsembleCoord> ensembleCoords = new ArrayList<GridEnsembleCoord>();

    // loop over HorizCoordSys
    Collection<GridHorizCoordSys> hcset = hcsHash.values();
    for (GridHorizCoordSys hcs : hcset) {
      if ((cancelTask != null) && cancelTask.isCancel()) break;

      // loop over GridVariables in the HorizCoordSys
      // create the time and vertical coordinates
      List<GridVariable> gribvars = new ArrayList<GridVariable>(hcs.varHash.values());
      for (GridVariable pv : gribvars) {
        if ((cancelTask != null) && cancelTask.isCancel()) break;

        List<GridRecord> recordList = pv.getRecords();
        GridRecord record = recordList.get(0);
        String vname = makeLevelName(record, lookup);

        // look to see if vertical already exists
        GridVertCoord useVertCoord = null;
        for (GridVertCoord gvcs : vertCoords) {
          if (vname.equals(gvcs.getLevelName())) {
            if (gvcs.matchLevels(recordList)) {  // must have the same levels
              useVertCoord = gvcs;
            }
          }
        }
        if (useVertCoord == null) {  // nope, got to create it
          useVertCoord = new GridVertCoord(recordList, vname, lookup, hcs);
          vertCoords.add(useVertCoord);
        }
        pv.setVertCoord(useVertCoord);
        // look to see if time coord already exists
        GridTimeCoord useTimeCoord = null;
        for (GridTimeCoord gtc : timeCoords) {
          if (gtc.matchTimes(recordList)) {  // must have the same time coords
            useTimeCoord = gtc;
            break;
          }
        }
        if (useTimeCoord == null) {  // nope, got to create it
          useTimeCoord = new GridTimeCoord(recordList, lookup);
          timeCoords.add(useTimeCoord);
        }
        pv.setTimeCoord(useTimeCoord);

        // check for ensemble members
        //System.out.println( pv.getName() +"  "+ pv.getParamName() );
        GridEnsembleCoord useEnsembleCoord = null;
        GridEnsembleCoord  ensembleCoord = new GridEnsembleCoord(recordList, lookup);
        for (GridEnsembleCoord gec : ensembleCoords) {
          if (ensembleCoord.getNEnsembles() == gec.getNEnsembles()) {
            useEnsembleCoord = gec;
            break;
          }
        }

        if (useEnsembleCoord == null) {
          //useEnsembleCoord = new GridEnsembleCoord(recordList, lookup);
          useEnsembleCoord = ensembleCoord;
          ensembleCoords.add(useEnsembleCoord);
        }
        // only add ensemble dimensions
        if (useEnsembleCoord.getNEnsembles() > 1)
          pv.setEnsembleCoord(useEnsembleCoord);
      }

      //// assign time coordinate names
      // find time dimensions with largest length
      GridTimeCoord tcs0 = null;
      int maxTimes = 0;
      for (GridTimeCoord tcs : timeCoords) {
        if (tcs.getNTimes() > maxTimes) {
          tcs0 = tcs;
          maxTimes = tcs.getNTimes();
        }
      }

      // add time dimensions, give time dimensions unique names
      int seqno = 1;
      for (GridTimeCoord tcs : timeCoords) {
        if (tcs != tcs0) {
          tcs.setSequence(seqno++);
        }
        tcs.addDimensionsToNetcdfFile(ncfile, hcs.getGroup());
      }

      // add Ensemble dimensions, give Ensemble dimensions unique names
      seqno = 0;
      for (GridEnsembleCoord gec : ensembleCoords) {
        gec.setSequence(seqno++);
        if (gec.getNEnsembles() > 1)
          gec.addDimensionsToNetcdfFile(ncfile, hcs.getGroup());
      }
      // add x, y dimensions
      hcs.addDimensionsToNetcdfFile(ncfile);

      // add vertical dimensions, give them unique names
      Collections.sort(vertCoords);
      int vcIndex = 0;
      String listName = null;
      int start = 0;
      for (vcIndex = 0; vcIndex < vertCoords.size(); vcIndex++) {
        GridVertCoord gvcs = (GridVertCoord) vertCoords.get(vcIndex);
        String vname = gvcs.getLevelName();
        if (listName == null) {
          listName = vname;  // initial
        }

        if (!vname.equals(listName)) {
          makeVerticalDimensions(vertCoords.subList(start, vcIndex), ncfile, hcs.getGroup());
          listName = vname;
          start = vcIndex;
        }
      }
      makeVerticalDimensions(vertCoords.subList(start, vcIndex), ncfile, hcs.getGroup());

      // create a variable for each entry, but check for other products with same desc
      // to disambiguate by vertical coord
      List<List<GridVariable>> products = new ArrayList<List<GridVariable>>(hcs.productHash.values());
      Collections.sort( products, new CompareGridVariableListByName());
      for (List<GridVariable> plist : products) {
        if ((cancelTask != null) && cancelTask.isCancel()) break;

        if (plist.size() == 1) {
          GridVariable pv = plist.get(0);
          boolean useNameWithoutLevel = !((lookup instanceof GempakLookup) || (lookup instanceof McIDASLookup ));

          Variable v = pv.makeVariable(ncfile, hcs.getGroup(), useNameWithoutLevel );
          ncfile.addVariable(hcs.getGroup(), v);

        } else {

          Collections.sort(plist, new CompareGridVariableByNumberVertLevels());
          boolean isGrib2 = false;
          Grib2GridTableLookup g2lookup = null;
          if ( (lookup instanceof Grib2GridTableLookup) ) {
            g2lookup = (Grib2GridTableLookup) lookup;
            isGrib2 = true;
          }
          // finally, add the variables
          for (int k = 0; k < plist.size(); k++) {
            GridVariable pv = plist.get(k);

            if (isGrib2 &&
                (g2lookup.isEnsemble( pv.getFirstRecord()) || g2lookup.isProbability( pv.getFirstRecord() ) ) ||
                (lookup instanceof GempakLookup) ||
                (lookup instanceof McIDASLookup ) ) {

              ncfile.addVariable(hcs.getGroup(), pv.makeVariable(ncfile, hcs.getGroup(), false));

            } else {
              ncfile.addVariable(hcs.getGroup(), pv.makeVariable(ncfile, hcs.getGroup(), (k == 0)));
            }
          }
        } // multiple vertical levels

      } // create variable

      // add coordinate variables at the end
      for (GridTimeCoord tcs : timeCoords) {
        tcs.addToNetcdfFile(ncfile, hcs.getGroup());
      }
      for (GridEnsembleCoord gec : ensembleCoords) {
        if (gec.getNEnsembles() > 1)
          gec.addToNetcdfFile(ncfile, hcs.getGroup());
      }
      hcs.addToNetcdfFile(ncfile);

      for (GridVertCoord gvcs : vertCoords) {
        gvcs.addToNetcdfFile(ncfile, hcs.getGroup());
      }

    } // loop over hcs
    // TODO: check this,  in ToolsUI it caused problems
    //for (GridVertCoord gvcs : vertCoords) {
    //  gvcs.empty();
    //}

  }

  /**
   * Make a vertical dimensions
   *
   * @param vertCoordList vertCoords all with the same name
   * @param ncfile        netCDF file to add to
   * @param group         group in ncfile
   */
  private void makeVerticalDimensions(List<GridVertCoord> vertCoordList, NetcdfFile ncfile, Group group) {
    // find biggest vert coord
    GridVertCoord gvcs0 = null;
    int maxLevels = 0;
    for (GridVertCoord gvcs : vertCoordList) {
      if (gvcs.getNLevels() > maxLevels) {
        gvcs0 = gvcs;
        maxLevels = gvcs.getNLevels();
      }
    }

    int seqno = 1;
    for (GridVertCoord gvcs : vertCoordList) {
      if (gvcs != gvcs0) {
        gvcs.setSequence(seqno++);
      }
      gvcs.addDimensionsToNetcdfFile(ncfile, group);
    }
  }

  /**
   * Make coordinate system from a Definition object
   *
   * @param ncfile netCDF file to add to
   * @param lookup lookup table
   * @param fmr    FmrcCoordSys
   * @throws IOException problem reading from file
   */
  private void makeDefinedCoordSys(NetcdfFile ncfile,
    GridTableLookup lookup, FmrcCoordSys fmr) throws IOException {

    List<GridTimeCoord> timeCoords = new ArrayList<GridTimeCoord>();
    List<GridVertCoord> vertCoords = new ArrayList<GridVertCoord>();
    List<GridEnsembleCoord> ensembleCoords = new ArrayList<GridEnsembleCoord>();

    List<String> removeVariables = new ArrayList<String>();

    // loop over HorizCoordSys
    Collection<GridHorizCoordSys> hcset = hcsHash.values();
    for (GridHorizCoordSys hcs : hcset) {

      // loop over GridVariables in the HorizCoordSys
      // create the time and vertical coordinates
      Set<String> keys = hcs.varHash.keySet();
      for (String key : keys) {
        GridVariable pv = hcs.varHash.get(key);
        GridRecord record = pv.getFirstRecord();

        // we dont know the name for sure yet, so have to try several
        String searchName = findVariableName(ncfile, record, lookup, fmr);
        if (searchName == null) { // cant find - just remove
          removeVariables.add(key); // cant remove (concurrentModException) so save for later
          continue;
        }
        pv.setVarName( searchName);

        // get the vertical coordinate for this variable, if it exists
        FmrcCoordSys.VertCoord vc_def = fmr.findVertCoordForVariable( searchName);
        if (vc_def != null) {
          String vc_name = vc_def.getName();

          // look to see if GridVertCoord already made
          GridVertCoord useVertCoord = null;
          for (GridVertCoord gvcs : vertCoords) {
            if (vc_name.equals(gvcs.getLevelName()))
              useVertCoord = gvcs;
          }
          if (useVertCoord == null) { // nope, got to create it
            useVertCoord = new GridVertCoord(record, vc_name, lookup, vc_def.getValues1(), vc_def.getValues2());
            useVertCoord.addDimensionsToNetcdfFile( ncfile, hcs.getGroup());
            vertCoords.add( useVertCoord);
          }
          pv.setVertCoord( useVertCoord);

        } else {
          pv.setVertCoord( new GridVertCoord(searchName)); // fake
        }

        // get the time coordinate for this variable
        FmrcCoordSys.TimeCoord tc_def = fmr.findTimeCoordForVariable( searchName, lookup.getFirstBaseTime());
        String tc_name = tc_def.getName();

        // look to see if GridTimeCoord already made
        GridTimeCoord useTimeCoord = null;
        for (GridTimeCoord gtc : timeCoords) {
          if (tc_name.equals(gtc.getName()))
            useTimeCoord = gtc;
        }
        if (useTimeCoord == null) { // nope, got to create it
          useTimeCoord = new GridTimeCoord(tc_name, tc_def.getOffsetHours(), lookup);
          useTimeCoord.addDimensionsToNetcdfFile( ncfile, hcs.getGroup());
          timeCoords.add( useTimeCoord);
        }
        pv.setTimeCoord( useTimeCoord);

        // check for ensemble members
        //System.out.println( pv.getName() +"  "+ pv.getParamName() );
        GridEnsembleCoord useEnsembleCoord = null;
        GridEnsembleCoord  ensembleCoord = new GridEnsembleCoord(record, lookup);
        for (GridEnsembleCoord gec : ensembleCoords) {
          if (ensembleCoord.getNEnsembles() == gec.getNEnsembles()) {
            useEnsembleCoord = gec;
            break;
          }
        }

        if (useEnsembleCoord == null) {
          useEnsembleCoord = ensembleCoord;
          ensembleCoords.add(useEnsembleCoord);
        }
        // only add ensemble dimensions
        if (useEnsembleCoord.getNEnsembles() > 1)
          pv.setEnsembleCoord(useEnsembleCoord);

      }

      // any need to be removed?
      for (String key : removeVariables) {
        hcs.varHash.remove(key);
      }

      // add x, y dimensions
      hcs.addDimensionsToNetcdfFile( ncfile);

      // create a variable for each entry
      Collection<GridVariable> vars = hcs.varHash.values();
      for (GridVariable pv : vars) {
        Group g = hcs.getGroup() == null ? ncfile.getRootGroup() : hcs.getGroup();
        Variable v = pv.makeVariable(ncfile, g, true);
        if (g.findVariable( v.getShortName()) != null) { // already got. can happen when a new vert level is added
          logger.warn("GribGridServiceProvider.GridIndexToNC: FmrcCoordSys has 2 variables mapped to ="+v.getShortName()+
                  " for file "+ncfile.getLocation());
        } else
          g.addVariable( v);
      }

      // add coordinate variables at the end
      for (GridTimeCoord tcs : timeCoords) {
        tcs.addToNetcdfFile(ncfile, hcs.getGroup());
      }

      for (GridEnsembleCoord gec : ensembleCoords) {
        if (gec.getNEnsembles() > 1)
          gec.addToNetcdfFile(ncfile, hcs.getGroup());
      }

      hcs.addToNetcdfFile( ncfile);
      for (GridVertCoord gvcs : vertCoords) {
        gvcs.addToNetcdfFile(ncfile, hcs.getGroup());
      }

    } // loop over hcs

    if (debug) System.out.println("GridIndexToNC.makeDefinedCoordSys for "+ncfile.getLocation());
  }

  /**
   * Find the variable name for the grid
   *
   * @param ncfile netCDF file
   * @param gr     grid record
   * @param lookup lookup table
   * @param fmr    FmrcCoordSys
   * @return name for the grid
   *
  private String findVariableName(NetcdfFile ncfile, GridRecord gr, GridTableLookup lookup, FmrcCoordSys fmr) {
    // first lookup with name & vert name
    String name = AbstractIOServiceProvider.createValidNetcdfObjectName(makeVariableName(gr, lookup));
    if (fmr.hasVariable(name)) {
      return name;
    }

    // now try just the name
    String pname = AbstractIOServiceProvider.createValidNetcdfObjectName( lookup.getParameter(gr).getDescription());
    if (fmr.hasVariable(pname)) {
      return pname;
    }

    logger.warn( "GridIndexToNC: FmrcCoordSys does not have the variable named ="
            + name + " or " + pname + " for file " + ncfile.getLocation());

    return null;
  } */

  private String findVariableName(NetcdfFile ncfile, GridRecord gr, GridTableLookup lookup, FmrcCoordSys fmr) {

    // first lookup with name & vert name
    String name = makeVariableName(gr, lookup, true, true);
    if (debug)
      System.out.println( "name ="+ name );
    if (fmr.hasVariable( name))
      return name;

    // now try just the name
    String pname = lookup.getParameter(gr).getDescription();
    if (debug)
      System.out.println( "pname ="+ pname );
    if (fmr.hasVariable( pname))
      return pname;

    // try replacing the blanks
    String nameWunder = StringUtil.replace(name, ' ', "_");
    if (debug)
      System.out.println( "nameWunder ="+ nameWunder );
    if (fmr.hasVariable( nameWunder))
      return nameWunder;

    String pnameWunder = StringUtil.replace(pname, ' ', "_");
    if (debug)
      System.out.println( "pnameWunder ="+ pnameWunder );
    if (fmr.hasVariable( pnameWunder))
      return pnameWunder;

    logger.warn("GridServiceProvider.GridIndexToNC: FmrcCoordSys does not have the variable named ="+name+" or "+pname+" or "+
            nameWunder+" or "+pnameWunder+" for file "+ncfile.getLocation());

    return null;
  }


  /**
   * Comparable object for grid variable
   *
   * @author IDV Development Team
   * @version $Revision: 1.3 $
   */
  private class CompareGridVariableListByName implements Comparator {

    /**
     * Compare the two lists of names
     *
     * @param o1 first list
     * @param o2 second list
     * @return comparison
     */
    public int compare(Object o1, Object o2) {
      ArrayList list1 = (ArrayList) o1;
      ArrayList list2 = (ArrayList) o2;

      GridVariable gv1 = (GridVariable) list1.get(0);
      GridVariable gv2 = (GridVariable) list2.get(0);

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
     * @param o1 first list
     * @param o2 second list
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
     * @param o1 first list
     * @param o2 second list
     * @return comparison
     */
    public int compare(Object o1, Object o2) {
      GridVariable gv1 = (GridVariable) o1;
      GridVariable gv2 = (GridVariable) o2;

      int n1 = gv1.getVertCoord().getNLevels();
      int n2 = gv2.getVertCoord().getNLevels();

      if (n1 == n2) {  // break ties for consistency
        return gv1.getVertCoord().getLevelName().compareTo(
            gv2.getVertCoord().getLevelName());
      } else {
        return n2 - n1;  // highest number first
      }
    }
  }

  /*
   * Should use the description for the variable name.
   *
   * @param value false to use name instead of description
   *
  public void setUseDescriptionForVariableName(boolean value) {
    useDescriptionForVariableName = value;
  } */

}
