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
package ucar.nc2.iosp.grib;

import ucar.grib.*;
import ucar.grib.grib1.Grib1Lookup;
import ucar.nc2.*;
import ucar.nc2.dt.fmr.FmrcCoordSys;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.StringUtil;
import ucar.grid.GridParameter;

import java.io.*;
import java.util.*;

/**
 * Create a Netcdf File from a ucar.grib.Index
 *
 * @author caron
 * @deprecated
 */
public class Index2NC  {

    /// hack - move into ucar.grib.TableLookup
  static public boolean isLayer(Index.GribRecord gr, TableLookup lookup) {
    if (lookup instanceof Grib1Lookup) {
      if (gr.levelType1 == 101) return true;
      if (gr.levelType1 == 104) return true;
      if (gr.levelType1 == 106) return true;
      if (gr.levelType1 == 108) return true;
      if (gr.levelType1 == 110) return true;
      if (gr.levelType1 == 112) return true;
      if (gr.levelType1 == 114) return true;
      if (gr.levelType1 == 116) return true;
      if (gr.levelType1 == 120) return true;
      if (gr.levelType1 == 121) return true;
      if (gr.levelType1 == 128) return true;
      if (gr.levelType1 == 141) return true;
      return false;
    } else {
      if (gr.levelType1 == 0) return false;
      if (gr.levelType2 == 255) return false;
      return true;      
      // return (gr.levelType2 != 255);
    }
  }

  static public String makeLevelName(Index.GribRecord gr, TableLookup lookup) {
    String vname = lookup.getLevelName( gr);
    boolean isGrib1 = (lookup instanceof Grib1Lookup);
    if (isGrib1) return vname;

    // for grib2, we need to add the layer to disambiguate
    return isLayer(gr, lookup) ? vname + "_layer" : vname;
  }

  static public String makeVariableName(Index.GribRecord gr, TableLookup lookup) {
    GridParameter param = lookup.getParameter(gr);
    if (param == null) return null;
    String levelName = makeLevelName( gr, lookup);
    return ((levelName == null) || (levelName.length() == 0)) ? param.getDescription() : param.getDescription() + "_" + levelName;
  }

  static public String makeLongName(Index.GribRecord gr, TableLookup lookup) {
    GridParameter param = lookup.getParameter(gr);
    String levelName = makeLevelName( gr, lookup);
    return (levelName.length() == 0) ? param.getDescription() : param.getDescription() + " @ " + makeLevelName( gr, lookup);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Index2NC.class);

  private Map<String, GribHorizCoordSys> hcsHash = new HashMap<String, GribHorizCoordSys>( 10); // GribHorizCoordSys
  private DateFormatter formatter = new DateFormatter();
  private boolean debug = false;

  void open(Index index, TableLookup lookup, int version, NetcdfFile ncfile, FmrcCoordSys fmrcCoordSys, CancelTask cancelTask) throws IOException {

    // create the HorizCoord Systems : one for each gds
    List<Index.GdsRecord> hcsList = index.getHorizCoordSys();
    boolean needGroups = (hcsList.size() > 1);
    for (int i = 0; i < hcsList.size(); i++) {
      Index.GdsRecord gdsIndex = hcsList.get(i);
      Group g = null;
      if (needGroups) {
        g = new Group(ncfile, null, "proj"+i);
        ncfile.addGroup( null, g);
      }

      // (Index.GdsRecord gdsIndex, String grid_name, String shape_name, Group g)
      GribHorizCoordSys hcs = new GribHorizCoordSys( gdsIndex, lookup, g);

      hcsHash.put(gdsIndex.gdsKey, hcs);
     }

    // run through each record
    Index.GribRecord firstRecord = null;
    List<Index.GribRecord> records = index.getGribRecords();
    if (GribServiceProvider.debugOpen) System.out.println(" number of products = "+records.size());
    for (Index.GribRecord gribRecord : records) {
      if (firstRecord == null) firstRecord = gribRecord;

      GribHorizCoordSys hcs = hcsHash.get(gribRecord.gdsKey);
      String name = makeVariableName(gribRecord, lookup);
      if (name == null) continue; // LOOK: message should get logged from grib layer

      GribVariable pv = hcs.varHash.get(name); // combo gds, param name and level name
      if (null == pv) {
        String pname = lookup.getParameter(gribRecord).getDescription();
        pv = new GribVariable(name, pname, hcs, lookup);
        hcs.varHash.put(name, pv);

        // keep track of all products with same parameter name
        List<GribVariable> plist = hcs.productHash.get(pname);
        if (null == plist) {
          plist = new ArrayList<GribVariable>();
          hcs.productHash.put(pname, plist);
        }
        plist.add(pv);
      }
      pv.addProduct(gribRecord);
    }

    // global stuff
    ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.0"));

    String creator = lookup.getFirstCenterName() + " subcenter = "+lookup.getFirstSubcenterId();
    if (creator != null)
      ncfile.addAttribute(null, new Attribute("Originating_center", creator) );
    String genType = lookup.getTypeGenProcessName( firstRecord);
    if (genType != null)
      ncfile.addAttribute(null, new Attribute("Generating_Process_or_Model", genType) );
    if (null != lookup.getFirstProductStatusName())
      ncfile.addAttribute(null, new Attribute("Product_Status", lookup.getFirstProductStatusName()) );
    ncfile.addAttribute(null, new Attribute("Product_Type", lookup.getFirstProductTypeName()) );

    // dataset discovery
    ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.GRID.toString()));
    ncfile.addAttribute(null, new Attribute("creator_name", creator));
    ncfile.addAttribute(null, new Attribute("file_format", "GRIB-"+version));
    ncfile.addAttribute(null, new Attribute("location", ncfile.getLocation()));
    //ncfile.addAttribute(null, new Attribute("history", "Direct read of GRIB into NetCDF-Java 2.2 API"));
    ncfile.addAttribute(null, new Attribute(
            "history", "Direct read of GRIB-" + version + " into NetCDF-Java 4.0 API"));
    ncfile.addAttribute(null, new Attribute(_Coordinate.ModelRunDate, formatter.toDateTimeStringISO(lookup.getFirstBaseTime())));

    if (fmrcCoordSys != null)
      makeDefinedCoordSys(ncfile, lookup, fmrcCoordSys);
    /* else if (GribServiceProvider.useMaximalCoordSys)
      makeMaximalCoordSys(ncfile, lookup, cancelTask); */
    else
      makeDenseCoordSys(ncfile, lookup, cancelTask);

    if (GribServiceProvider.debugMissing) {
      int count = 0;
      Collection<GribHorizCoordSys> hcset = hcsHash.values();
      for (GribHorizCoordSys hcs : hcset) {
        List<GribVariable> gribvars = new ArrayList<GribVariable>(hcs.varHash.values());
        for (GribVariable gv : gribvars) {
          count += gv.dumpMissingSummary();
        }
      }
      System.out.println(" total missing= "+count);
    }

    if (GribServiceProvider.debugMissingDetails) {
      Collection<GribHorizCoordSys> hcset = hcsHash.values();
      for (GribHorizCoordSys hcs : hcset) {
        System.out.println("******** Horiz Coordinate= " + hcs.getGridName());

        String lastVertDesc = null;
        List<GribVariable> gribvars = new ArrayList<GribVariable>(hcs.varHash.values());
        Collections.sort(gribvars, new CompareGribVariableByVertName());

        for (GribVariable gv : gribvars) {
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
    for (GribHorizCoordSys ghcs : hcsHash.values()) {
      ghcs.empty();
    }
  }

  // make coordinate system without missing data - means that we have to make a coordinate axis for each unique set
  // of time or vertical levels.
  private void makeDenseCoordSys(NetcdfFile ncfile, TableLookup lookup, CancelTask cancelTask) throws IOException {
    List<GribTimeCoord> timeCoords = new ArrayList<GribTimeCoord>();
    List<GribVertCoord> vertCoords = new ArrayList<GribVertCoord>();

    // loop over HorizCoordSys
    Collection<GribHorizCoordSys> hcset = hcsHash.values();
    for (GribHorizCoordSys hcs : hcset) {
      if ((cancelTask != null) && cancelTask.isCancel()) break;

      // loop over GribVariables in the HorizCoordSys
      // create the time and vertical coordinates
      List<GribVariable> gribvars = new ArrayList<GribVariable>(hcs.varHash.values());
      for (GribVariable pv : gribvars) {
        if ((cancelTask != null) && cancelTask.isCancel()) break;

        List<Index.GribRecord> recordList = pv.getRecords();
        Index.GribRecord record = recordList.get(0);
        String vname = makeLevelName( record, lookup);

        // look to see if vertical already exists
        GribVertCoord useVertCoord = null;
        for (GribVertCoord gvcs : vertCoords) {
          if (vname.equals(gvcs.getLevelName())) {
            if (gvcs.matchLevels(recordList)) // must have the same levels
              useVertCoord = gvcs;
          }
        }
        if (useVertCoord == null) { // nope, got to create it
          useVertCoord = new GribVertCoord(recordList, vname, lookup);
          vertCoords.add( useVertCoord);
        }
        pv.setVertCoord( useVertCoord);

        // look to see if time coord already exists
        GribTimeCoord useTimeCoord = null;
        for (GribTimeCoord gtc : timeCoords) {
          if (gtc.matchLevels(recordList)) // must have the same levels
            useTimeCoord = gtc;
        }
        if (useTimeCoord == null) { // nope, got to create it
          useTimeCoord = new GribTimeCoord(recordList, lookup);
          timeCoords.add( useTimeCoord);
        }
        pv.setTimeCoord( useTimeCoord);
      }

      //// assign time coordinate names
      // find time dimensions with largest length
      GribTimeCoord tcs0 = null;
      int maxTimes = 0;
      for (GribTimeCoord tcs : timeCoords) {
        if (tcs.getNTimes() > maxTimes) {
          tcs0 = tcs;
          maxTimes = tcs.getNTimes();
        }
      }

      // add time dimensions, give time dimensions unique names
      int seqno = 1;
      for (GribTimeCoord tcs : timeCoords) {
        if (tcs != tcs0)
          tcs.setSequence(seqno++);
        tcs.addDimensionsToNetcdfFile(ncfile, hcs.getGroup());
      }

      // add x, y dimensions
      hcs.addDimensionsToNetcdfFile( ncfile);

      // add vertical dimensions, give them unique names
      Collections.sort( vertCoords);
      int vcIndex;
      String listName = null;
      int start = 0;
      for (vcIndex = 0; vcIndex < vertCoords.size(); vcIndex++) {
        GribVertCoord gvcs = vertCoords.get(vcIndex);
        String vname = gvcs.getLevelName();
        if (listName == null) listName = vname; // initial

        if (!vname.equals(listName)) {
          makeVerticalDimensions( vertCoords.subList(start, vcIndex), ncfile, hcs.getGroup());
          listName = vname;
          start = vcIndex;
        }
      }
      makeVerticalDimensions( vertCoords.subList(start, vcIndex), ncfile, hcs.getGroup());

      // create a variable for each entry, but check for other products with same desc
      // to disambiguate by vertical coord
      List<List<GribVariable>> products = new ArrayList<List<GribVariable>>(hcs.productHash.values());
      Collections.sort( products, new CompareGribVariableListByName());
      for (List<GribVariable> plist : products) {
        if ((cancelTask != null) && cancelTask.isCancel()) break;

        if (plist.size() == 1) {
          GribVariable pv = plist.get(0);
          Variable v = pv.makeVariable(ncfile, hcs.getGroup(), true);
          ncfile.addVariable(hcs.getGroup(), v);

        } else {

          Collections.sort(plist, new CompareGribVariableByNumberVertLevels());
          /* find the one with the most vertical levels
          int maxLevels = 0;
          GribVariable maxV = null;
          for (int j = 0; j < plist.size(); j++) {
            GribVariable pv = (GribVariable) plist.get(j);
            if (pv.getVertCoord().getNLevels() > maxLevels) {
              maxLevels = pv.getVertCoord().getNLevels();
              maxV = pv;
            }
          } */
          // finally, add the variables
          for (int k = 0; k < plist.size(); k++) {
            GribVariable pv = plist.get(k);
            //int nlevels = pv.getVertNlevels();
            //boolean useDesc = (k == 0) && (nlevels > 1); // keep using the level name if theres only one level
            ncfile.addVariable(hcs.getGroup(), pv.makeVariable(ncfile, hcs.getGroup(), (k == 0)));
          }
        } // multipe vertical levels

      } // create variable

      // add coordinate variables at the end
      for (GribTimeCoord tcs : timeCoords) {
        tcs.addToNetcdfFile(ncfile, hcs.getGroup());
      }
      hcs.addToNetcdfFile( ncfile);

      for (GribVertCoord gvcs : vertCoords) {
        gvcs.addToNetcdfFile(ncfile, hcs.getGroup());
      }

    } // loop over hcs

    for (GribVertCoord gvcs : vertCoords) {
      gvcs.empty();
    }

  }

  // vertCoords all with the same name
  private void makeVerticalDimensions(List<GribVertCoord> vertCoordList, NetcdfFile ncfile, Group group) {
    // find biggest vert coord
    GribVertCoord gvcs0 = null;
    int maxLevels = 0;
    for (GribVertCoord gvcs : vertCoordList) {
      if (gvcs.getNLevels() > maxLevels) {
        gvcs0 = gvcs;
        maxLevels = gvcs.getNLevels();
      }
    }

    int seqno = 1;
    for (GribVertCoord gvcs : vertCoordList) {
      if (gvcs != gvcs0)
        gvcs.setSequence(seqno++);
      gvcs.addDimensionsToNetcdfFile(ncfile, group);
    }
  }

  /* make coordinate system by taking the union of all time steps, vertical levels
  // means that there can be a fair amount of missing data
  // probably should be deprecated
  private void makeMaximalCoordSys(NetcdfFile ncfile, TableLookup lookup, CancelTask cancelTask) throws IOException {

    // loop over HorizCoordSys
    Iterator iterHcs = hcsHash.values().iterator();
    while (iterHcs.hasNext()) {
      GribHorizCoordSys hcs =  (GribHorizCoordSys) iterHcs.next();

      // create the time coordinate : just union on all the times
      // LOOK: only one tcs per GribHorizCoordSys ?
      GribTimeCoord tcs = new GribTimeCoord();
      Iterator iter = hcs.varHash.values().iterator();
      while (iter.hasNext()) {
        GribVariable pv =  (GribVariable) iter.next();
        List products = pv.getRecords();
        tcs.addTimes( products);
      }

      // create the coordinate systems
      iter = hcs.varHash.values().iterator();
      while (iter.hasNext()) {
        GribVariable pv =  (GribVariable) iter.next();
        List recordList = pv.getRecords();
        Index.GribRecord record = (Index.GribRecord) recordList.get(0);
        String vname = lookup.getLevelName( record);

        GribCoordSys gvcs = (GribCoordSys) hcs.vcsHash.get(vname);
        if (gvcs == null) {
          gvcs = new GribCoordSys(hcs, record, vname, lookup);
          hcs.vcsHash.put( vname, gvcs);
        }

        gvcs.addLevels(  recordList);
        pv.setVertCoordSys( gvcs);
      }

      // add needed dimensions
      tcs.addDimensionsToNetcdfFile( ncfile, hcs.getGroup());
      hcs.addDimensionsToNetcdfFile( ncfile);
      iter = hcs.vcsHash.values().iterator();
      while (iter.hasNext()) {
        GribCoordSys gvcs =  (GribCoordSys) iter.next();
        gvcs.addDimensionsToNetcdfFile( ncfile, hcs.getGroup());
      }

      // create a variable for each entry, but check for other products with same desc
      // to disambiguate by vertical coord
      ArrayList products = new ArrayList(hcs.productHash.values());
      Collections.sort( products, new CompareGribVariableListByName());
      for (int i = 0; i < products.size(); i++) {
        ArrayList plist = (ArrayList) products.get(i);

        if (plist.size() == 1) {
          GribVariable pv = (GribVariable) plist.get(0);
          pv.setTimeCoord( tcs);
          ncfile.addVariable( hcs.getGroup(), pv.makeVariable(ncfile, hcs.getGroup(), true));
        } else {

          // find the one with the most vertical levels
          int maxLevels = 0;
          GribVariable maxV = null;
          for (int j = 0; j < plist.size(); j++) {
            GribVariable pv = (GribVariable) plist.get(j);
            if (pv.getVertCoordSys().getNLevels() > maxLevels) {
              maxLevels = pv.getVertCoordSys().getNLevels();
              maxV = pv;
            }
          }
          // finally, add the variables
          for (int k = 0; k < plist.size(); k++) {
            GribVariable pv = (GribVariable) plist.get(k);
            pv.setTimeCoord( tcs);
            ncfile.addVariable( hcs.getGroup(), pv.makeVariable(ncfile, hcs.getGroup(), (pv == maxV)));
          }
        } // multipe vertical levels

      } // create variable

      // add coordinate systems at end
      tcs.addToNetcdfFile( ncfile, hcs.getGroup() );
      hcs.addToNetcdfFile( ncfile);
      iter = hcs.vcsHash.values().iterator();
      while (iter.hasNext()) {
        GribCoordSys gvcs =  (GribCoordSys) iter.next();
        gvcs.addToNetcdfFile( ncfile, hcs.getGroup());
      }

    } // loop over hcs

  } */



  // make coordinate system from a Definition object
  private void makeDefinedCoordSys(NetcdfFile ncfile, TableLookup lookup, FmrcCoordSys fmr) throws IOException {
    List<GribTimeCoord> timeCoords = new ArrayList<GribTimeCoord>();
    List<GribVertCoord> vertCoords = new ArrayList<GribVertCoord>();

    List<String> removeVariables = new ArrayList<String>();

    // loop over HorizCoordSys
    Collection<GribHorizCoordSys> hcset = hcsHash.values();
    for (GribHorizCoordSys hcs : hcset) {

      // loop over GribVariables in the HorizCoordSys
      // create the time and vertical coordinates
      Set<String> keys = hcs.varHash.keySet();
      for (String key : keys) {
        GribVariable pv = hcs.varHash.get(key);
        Index.GribRecord record = pv.getFirstRecord();

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

          // look to see if GribVertCoord already made
          GribVertCoord useVertCoord = null;
          for (GribVertCoord gvcs : vertCoords) {
            if (vc_name.equals(gvcs.getLevelName()))
              useVertCoord = gvcs;
          }
          if (useVertCoord == null) { // nope, got to create it
            useVertCoord = new GribVertCoord(record, vc_name, lookup, vc_def.getValues1(), vc_def.getValues2());
            useVertCoord.addDimensionsToNetcdfFile( ncfile, hcs.getGroup());
            vertCoords.add( useVertCoord);
          }
          pv.setVertCoord( useVertCoord);

        } else {
          pv.setVertCoord( new GribVertCoord(searchName)); // fake
        }

        // get the time coordinate for this variable
        FmrcCoordSys.TimeCoord tc_def = fmr.findTimeCoordForVariable( searchName, lookup.getFirstBaseTime());
        String tc_name = tc_def.getName();

        // look to see if GribTimeCoord already made
        GribTimeCoord useTimeCoord = null;
        for (GribTimeCoord gtc : timeCoords) {
          if (tc_name.equals(gtc.getName()))
            useTimeCoord = gtc;
        }
        if (useTimeCoord == null) { // nope, got to create it
          useTimeCoord = new GribTimeCoord(tc_name, tc_def.getOffsetHours(), lookup);
          useTimeCoord.addDimensionsToNetcdfFile( ncfile, hcs.getGroup());
          timeCoords.add( useTimeCoord);
        }
        pv.setTimeCoord( useTimeCoord);
      }

      // any need to be removed?
      for (String key : removeVariables) {
        hcs.varHash.remove(key);
      }

      // add x, y dimensions
      hcs.addDimensionsToNetcdfFile( ncfile);

      // create a variable for each entry
      Collection<GribVariable> vars = hcs.varHash.values();
      for (GribVariable pv : vars) {
        Group g = hcs.getGroup() == null ? ncfile.getRootGroup() : hcs.getGroup();
        Variable v = pv.makeVariable(ncfile, g, true);
        if (g.findVariable( v.getShortName()) != null) { // already got. can happen when a new vert level is added
          logger.warn("GribServiceProvider.Index2NC: FmrcCoordSys has 2 variables mapped to ="+v.getShortName()+
                  " for file "+ncfile.getLocation());
        } else
          g.addVariable( v);
      }

      // add coordinate variables at the end
      for (GribTimeCoord tcs : timeCoords) {
        tcs.addToNetcdfFile(ncfile, hcs.getGroup());
      }

      hcs.addToNetcdfFile( ncfile);
      for (GribVertCoord gvcs : vertCoords) {
        gvcs.addToNetcdfFile(ncfile, hcs.getGroup());
      }

    } // loop over hcs

    if (debug) System.out.println("Index2NC.makeDefinedCoordSys for "+ncfile.getLocation());
  }

  private String findVariableName(NetcdfFile ncfile, Index.GribRecord gr, TableLookup lookup, FmrcCoordSys fmr) {
    // first lookup with name & vert name
    String name = makeVariableName(gr, lookup);
    if (fmr.hasVariable( name))
      return name;

    // now try just the name
    String pname = lookup.getParameter(gr).getDescription();
    if (fmr.hasVariable( pname))
      return pname;

    // try replacing the blanks
    String nameWunder = StringUtil.replace(name, ' ', "_");
    if (fmr.hasVariable( nameWunder))
      return nameWunder;

    String pnameWunder = StringUtil.replace(pname, ' ', "_");
    if (fmr.hasVariable( pnameWunder))
      return pnameWunder;

    logger.warn("GribServiceProvider.Index2NC: FmrcCoordSys does not have the variable named ="+name+" or "+pname+" or "+
            nameWunder+" or "+pnameWunder+" for file "+ncfile.getLocation());

    return null;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  private class CompareGribVariableListByName implements Comparator<List<GribVariable>> {

    public int compare(List<GribVariable> list1, List<GribVariable> list2) {
      GribVariable gv1 =  list1.get(0);
      GribVariable gv2 =  list2.get(0);
      return gv1.getName().compareToIgnoreCase( gv2.getName());
    }
  }

  private class CompareGribVariableByVertName implements Comparator<GribVariable> {

    public int compare(GribVariable gv1, GribVariable gv2) {
      return gv1.getVertName().compareToIgnoreCase( gv2.getVertName());
    }
  }

  private class CompareGribVariableByNumberVertLevels implements Comparator<GribVariable> {

    public int compare(GribVariable gv1, GribVariable gv2) {
      int n1 = gv1.getVertCoord().getNLevels();
      int n2 = gv2.getVertCoord().getNLevels();

      if (n1 == n2) // break ties for consistency
        return gv1.getVertCoord().getLevelName().compareTo(  gv2.getVertCoord().getLevelName());
      else
        return n2 - n1; // highest number first
    }
  }



}
