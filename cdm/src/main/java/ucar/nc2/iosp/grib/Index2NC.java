package ucar.nc2.iosp.grib;

import ucar.grib.*;
import ucar.grib.grib1.Grib1Lookup;
import ucar.nc2.*;
import ucar.nc2.dt.fmr.FmrcCoordSys;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.util.*;

/**
 * Create a Netcdf File from a ucar.grib.Index
 *
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
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
      return (gr.levelType2 != 255);
    }
  }

  static public String makeLevelName(Index.GribRecord gr, TableLookup lookup) {
    String vname = lookup.getLevelName( gr);
    boolean isGrib1 = (lookup instanceof Grib1Lookup); // for grib2, we need to add the layer to disambiguate
    return (!isGrib1 && isLayer(gr, lookup)) ? vname + "_layer" : vname;
  }

  static public String makeVariableName(Index.GribRecord gr, TableLookup lookup) {
    Parameter param = lookup.getParameter(gr);
    return param.getDescription() + "_" + makeLevelName( gr, lookup);
  }

  static public String makeLongName(Index.GribRecord gr, TableLookup lookup) {
    Parameter param = lookup.getParameter(gr);
    return param.getDescription() + " @ " + makeLevelName( gr, lookup);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribServiceProvider.class);

  private HashMap hcsHash = new HashMap( 10); // GribHorizCoordSys
  private DateFormatter formatter = new DateFormatter();
  private boolean debug = false;

  void open(Index index, TableLookup lookup, int version, NetcdfFile ncfile, FmrcCoordSys fmrcCoordSys, CancelTask cancelTask) throws IOException {

    // create the HorizCoord Systems : one for each gds
    List hcsList = index.getHorizCoordSys();
    boolean needGroups = (hcsList.size() > 1);
    for (int i = 0; i < hcsList.size(); i++) {
      Index.GdsRecord gdsIndex =  (Index.GdsRecord) hcsList.get(i);

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
    ArrayList records = index.getGribRecords();
    if (GribServiceProvider.debugOpen) System.out.println(" number of products = "+records.size());
    for( int i = 0; i < records.size(); i++ ) {
      Index.GribRecord gribRecord = (Index.GribRecord) records.get( i );
      if (firstRecord == null) firstRecord = gribRecord;

      GribHorizCoordSys hcs = (GribHorizCoordSys) hcsHash.get(gribRecord.gdsKey);
      String name = makeVariableName( gribRecord, lookup);
      GribVariable pv = (GribVariable) hcs.varHash.get(name); // combo gds, param name and level name
      if (null == pv) {
        String pname = lookup.getParameter(gribRecord).getDescription();
        pv = new GribVariable( name, pname, hcs, lookup);
        hcs.varHash.put(name, pv);

         // keep track of all products with same parameter name
        ArrayList plist = (ArrayList) hcs.productHash.get(pname);
        if (null == plist) {
          plist = new ArrayList();
          hcs.productHash.put(pname, plist);
        }
        plist.add( pv);
      }
      pv.addProduct( gribRecord);
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
    ncfile.addAttribute(null, new Attribute("cdm_data_type", thredds.catalog.DataType.GRID.toString()));
    ncfile.addAttribute(null, new Attribute("creator_name", creator));
    ncfile.addAttribute(null, new Attribute("file_format", "GRIB-"+version));
    ncfile.addAttribute(null, new Attribute("location", ncfile.getLocation()));
    ncfile.addAttribute(null, new Attribute("history", "Direct read of GRIB into NetCDF-Java 2.2 API"));

    ncfile.addAttribute(null, new Attribute(_Coordinate.ModelRunDate, formatter.toDateTimeStringISO(lookup.getFirstBaseTime())));

    if (fmrcCoordSys != null)
      makeDefinedCoordSys(ncfile, lookup, fmrcCoordSys);
    /* else if (GribServiceProvider.useMaximalCoordSys)
      makeMaximalCoordSys(ncfile, lookup, cancelTask); */
    else
      makeDenseCoordSys(ncfile, lookup, cancelTask);

    if (GribServiceProvider.debugMissing) {
      int count = 0;
      Iterator iterHcs = hcsHash.values().iterator();
      while (iterHcs.hasNext()) {
        GribHorizCoordSys hcs = (GribHorizCoordSys) iterHcs.next();
        ArrayList gribvars = new ArrayList(hcs.varHash.values());
        for (int i = 0; i < gribvars.size(); i++) {
          GribVariable gv = (GribVariable) gribvars.get(i);
          count += gv.dumpMissingSummary();
        }
      }
      System.out.println(" total missing= "+count);
    }

    if (GribServiceProvider.debugMissingDetails) {
      Iterator iterHcs = hcsHash.values().iterator();
      while (iterHcs.hasNext()) { // loop over HorizCoordSys
        GribHorizCoordSys hcs = (GribHorizCoordSys) iterHcs.next();
        System.out.println("******** Horiz Coordinate= " + hcs.getGridName());

        String lastVertDesc = null;
        ArrayList gribvars = new ArrayList(hcs.varHash.values());
        Collections.sort(gribvars, new CompareGribVariableByVertName());

        for (int i = 0; i < gribvars.size(); i++) {
          GribVariable gv = (GribVariable) gribvars.get(i);
          String vertDesc = gv.getVertName();
          if (!vertDesc.equals(lastVertDesc)) {
            System.out.println("---Vertical Coordinate= " + vertDesc);
            lastVertDesc = vertDesc;
          }
          gv.dumpMissing();
        }
      }
    }

  }

  // make coordinate system without missing data - means that we have to make a coordinate axis for each unique set
  // of time or vertical levels.
  private void makeDenseCoordSys(NetcdfFile ncfile, TableLookup lookup, CancelTask cancelTask) throws IOException {
    ArrayList timeCoords = new ArrayList();
    ArrayList vertCoords = new ArrayList();

    // loop over HorizCoordSys
    Iterator iterHcs = hcsHash.values().iterator();
    while (iterHcs.hasNext()) {
      GribHorizCoordSys hcs =  (GribHorizCoordSys) iterHcs.next();

      // loop over GribVariables in the HorizCoordSys
      // create the time and vertical coordinates
      Iterator iter = hcs.varHash.values().iterator();
      while (iter.hasNext()) {
        GribVariable pv =  (GribVariable) iter.next();
        List recordList = pv.getRecords();
        Index.GribRecord record = (Index.GribRecord) recordList.get(0);
        String vname = makeLevelName( record, lookup);

        // look to see if vertical already exists
        GribVertCoord useVertCoord = null;
        for (int i = 0; i < vertCoords.size(); i++) {
          GribVertCoord gvcs = (GribVertCoord) vertCoords.get(i);
          if (vname.equals( gvcs.getLevelName())) {
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
        for (int i = 0; i < timeCoords.size(); i++) {
          GribTimeCoord gtc = (GribTimeCoord) timeCoords.get(i);
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
      for (int i = 0; i < timeCoords.size(); i++) {
        GribTimeCoord tcs = (GribTimeCoord) timeCoords.get(i);
        if (tcs.getNTimes() > maxTimes) {
          tcs0 = tcs;
          maxTimes = tcs.getNTimes();
        }
      }

      // add time dimensions, give time dimensions unique names
      int seqno = 1;
      for (int i = 0; i < timeCoords.size(); i++) {
        GribTimeCoord tcs = (GribTimeCoord) timeCoords.get(i);
        if (tcs != tcs0)
          tcs.setSequence(seqno++);
        tcs.addDimensionsToNetcdfFile( ncfile, hcs.getGroup());
      }

      // add x, y dimensions
      hcs.addDimensionsToNetcdfFile( ncfile);

      // add vertical dimensions, give them unique names
      Collections.sort( vertCoords);
      int vcIndex = 0;
      String listName = null;
      int start = 0;
      for (vcIndex = 0; vcIndex < vertCoords.size(); vcIndex++) {
        GribVertCoord gvcs = (GribVertCoord) vertCoords.get(vcIndex);
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
      ArrayList products = new ArrayList(hcs.productHash.values());
      Collections.sort( products, new CompareGribVariableListByName());
      for (int i = 0; i < products.size(); i++) {
        ArrayList plist = (ArrayList) products.get(i); // all the products with the same name

        if (plist.size() == 1) {
          GribVariable pv = (GribVariable) plist.get(0);
          Variable v = pv.makeVariable(ncfile, hcs.getGroup(),  true);
          ncfile.addVariable( hcs.getGroup(), v);

        } else {

          Collections.sort( plist, new CompareGribVariableByNumberVertLevels());
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
            GribVariable pv = (GribVariable) plist.get(k);
            //int nlevels = pv.getVertNlevels();
            //boolean useDesc = (k == 0) && (nlevels > 1); // keep using the level name if theres only one level
            ncfile.addVariable( hcs.getGroup(), pv.makeVariable(ncfile, hcs.getGroup(), (k == 0)));
          }
        } // multipe vertical levels

      } // create variable

      // add coordinate variables at the end
      for (int i = 0; i < timeCoords.size(); i++) {
        GribTimeCoord tcs = (GribTimeCoord) timeCoords.get(i);
        tcs.addToNetcdfFile( ncfile, hcs.getGroup());
      }
      hcs.addToNetcdfFile( ncfile);
      for (int i = 0; i < vertCoords.size(); i++) {
        GribVertCoord gvcs = (GribVertCoord) vertCoords.get(i);
        gvcs.addToNetcdfFile( ncfile, hcs.getGroup());
      }

    } // loop over hcs

  }

  // vertCoords all with the same name
  private void makeVerticalDimensions(List vertCoordList, NetcdfFile ncfile, Group group) {
    // find biggest vert coord
    GribVertCoord gvcs0 = null;
    int maxLevels = 0;
    for (int i = 0; i < vertCoordList.size(); i++) {
      GribVertCoord gvcs = (GribVertCoord) vertCoordList.get(i);
      if (gvcs.getNLevels() > maxLevels) {
        gvcs0 = gvcs;
        maxLevels = gvcs.getNLevels();
      }
    }

    int seqno = 1;
    for (int i = 0; i < vertCoordList.size(); i++) {
      GribVertCoord gvcs = (GribVertCoord) vertCoordList.get(i);
      if (gvcs != gvcs0)
        gvcs.setSequence(seqno++);
      gvcs.addDimensionsToNetcdfFile( ncfile, group);
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



  // make coordinate system from a Definitioon object
  private void makeDefinedCoordSys(NetcdfFile ncfile, TableLookup lookup, FmrcCoordSys fmr) throws IOException {
    ArrayList timeCoords = new ArrayList();
    ArrayList vertCoords = new ArrayList();

    // loop over HorizCoordSys
    Iterator iterHcs = hcsHash.values().iterator();
    while (iterHcs.hasNext()) {
      GribHorizCoordSys hcs =  (GribHorizCoordSys) iterHcs.next();

      // loop over GribVariables in the HorizCoordSys
      // create the time and vertical coordinates
      Iterator iter = hcs.varHash.values().iterator();
      while (iter.hasNext()) {
        GribVariable pv =  (GribVariable) iter.next();
        Index.GribRecord record = pv.getFirstRecord();

        // we dont know the name for sure yet, so have to try several
        String searchName = findVariableName(record, lookup, fmr);
        if (searchName == null) {
          continue;
        }
        pv.setVarName( searchName);

        // get the vertical coordinate for this variable, if it exists
        FmrcCoordSys.VertCoord vc_def = fmr.findVertCoordForVariable( searchName);
        if (vc_def != null) {
          String vc_name = vc_def.getName();

          // look to see if GribVertCoord already made
          GribVertCoord useVertCoord = null;
          for (int i = 0; i < vertCoords.size(); i++) {
            GribVertCoord gvcs = (GribVertCoord) vertCoords.get(i);
            if (vc_name.equals( gvcs.getLevelName()))
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
        for (int i = 0; i < timeCoords.size(); i++) {
          GribTimeCoord gtc = (GribTimeCoord) timeCoords.get(i);
          if (tc_name.equals( gtc.getName()))
              useTimeCoord = gtc;
        }
        if (useTimeCoord == null) { // nope, got to create it
          useTimeCoord = new GribTimeCoord(tc_name, tc_def.getOffsetHours(), lookup);
          useTimeCoord.addDimensionsToNetcdfFile( ncfile, hcs.getGroup());
          timeCoords.add( useTimeCoord);
        }
        pv.setTimeCoord( useTimeCoord);
      }

      // add x, y dimensions
      hcs.addDimensionsToNetcdfFile( ncfile);

      // create a variable for each entry
      Iterator iter2 = hcs.varHash.values().iterator();
      while (iter2.hasNext()) {
        GribVariable pv =  (GribVariable) iter2.next();
        Variable v = pv.makeVariable(ncfile, hcs.getGroup(), true);
        ncfile.addVariable( hcs.getGroup(), v);
      }

      // add coordinate variables at the end
      for (int i = 0; i < timeCoords.size(); i++) {
        GribTimeCoord tcs = (GribTimeCoord) timeCoords.get(i);
        tcs.addToNetcdfFile( ncfile, hcs.getGroup());
      }
      hcs.addToNetcdfFile( ncfile);
      for (int i = 0; i < vertCoords.size(); i++) {
        GribVertCoord gvcs = (GribVertCoord) vertCoords.get(i);
        gvcs.addToNetcdfFile( ncfile, hcs.getGroup());
      }

    } // loop over hcs

    if (debug) System.out.println("Index2NC.makeDefinedCoordSys for "+ncfile.getLocation());
  }

  private String findVariableName(Index.GribRecord gr, TableLookup lookup, FmrcCoordSys fmr) {
    // first lookup with name & vert name
    String name = NetcdfFile.createValidNetcdfObjectName( makeVariableName(gr, lookup));
    if (fmr.hasVariable( name))
      return name;

    // now try just the name
    String pname = NetcdfFile.createValidNetcdfObjectName( lookup.getParameter(gr).getDescription());
    if (fmr.hasVariable( pname))
      return pname;

    System.out.println("GribServiceProvider.Index2NC: FmrcCoordSys does not have the variable named ="+name+" or "+pname);
    logger.warn("GribServiceProvider.Index2NC: FmrcCoordSys does not have the variable named ="+name+" or "+pname);

    return null;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  private class CompareGribVariableListByName implements Comparator {

    public int compare(Object o1, Object o2) {
      ArrayList list1 = (ArrayList) o1;
      ArrayList list2 = (ArrayList) o2;

      GribVariable gv1 = (GribVariable) list1.get(0);
      GribVariable gv2 = (GribVariable) list2.get(0);

      return gv1.getName().compareToIgnoreCase( gv2.getName());
    }
  }

  private class CompareGribVariableByVertName implements Comparator {

    public int compare(Object o1, Object o2) {
      GribVariable gv1 = (GribVariable) o1;
      GribVariable gv2 = (GribVariable) o2;

      return gv1.getVertName().compareToIgnoreCase( gv2.getVertName());
    }
  }

  private class CompareGribVariableByNumberVertLevels implements Comparator {

    public int compare(Object o1, Object o2) {
      GribVariable gv1 = (GribVariable) o1;
      GribVariable gv2 = (GribVariable) o2;

      int n1 = gv1.getVertCoord().getNLevels();
      int n2 = gv2.getVertCoord().getNLevels();

      if (n1 == n2) // break ties for consistency
        return gv1.getVertCoord().getLevelName().compareTo(  gv2.getVertCoord().getLevelName());
      else
        return n2 - n1; // highest number first
    }
  }



}
