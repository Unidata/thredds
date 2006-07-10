package ucar.nc2.iosp.grib;

import ucar.grib.*;
import ucar.nc2.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;

import java.io.*;
import java.util.*;

/**
 * Create a Netcdf File from a ucar.grib.Index
 *
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
 */
public class Index2NC  {

  private HashMap hcsHash = new HashMap( 10); // GribHorizCoordSys
  private DateFormatter formatter = new DateFormatter();

  public void open(Index index, TableLookup lookup, int version, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {

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
      // hcs.addToNetcdfFile( ncfile);

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
      Parameter param = lookup.getParameter(gribRecord);
      String pname = param.getDescription();
      String vname = lookup.getLevelName( gribRecord);
      String name = pname +"_"+vname;
      GribVariable pv = (GribVariable) hcs.varHash.get(name); // combo gds, param name and level name
      if (null == pv) {
        pv = new GribVariable( name, pname, hcs);
        hcs.varHash.put(name, pv);

         // keep track of all products with same desc
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

    // LOOK : might want to put in groups?
    ncfile.addAttribute(null, new Attribute("Originating_center", lookup.getFirstCenterName() +
        " subcenter = "+lookup.getFirstSubcenterId()) );
    ncfile.addAttribute(null, new Attribute("Product_Status", lookup.getFirstProductStatusName()) );
    ncfile.addAttribute(null, new Attribute("Product_Type", lookup.getFirstProductTypeName()) );
    ncfile.addAttribute(null, new Attribute("FileFormat", "GRIB-"+version));
    ncfile.addAttribute(null, new Attribute("DataType", "GRID"));
    ncfile.addAttribute(null, new Attribute("DatasetLocation", ncfile.getLocation()));
    ncfile.addAttribute(null, new Attribute("Processing", "direct read of GRIB into NetCDF-Java 2.2 API"));
    ncfile.addAttribute(null, new Attribute("_CoordinateAxisModelRunDate", formatter.toDateTimeString(lookup.getFirstBaseTime())));

    if (GribServiceProvider.useMaximalCoordSys)
      makeMaximalCoordSys(ncfile, lookup, cancelTask);
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
        String vname = lookup.getLevelName( record);

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
          Variable v = pv.makeVariable(ncfile, hcs.getGroup(), lookup, true);
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
            ncfile.addVariable( hcs.getGroup(), pv.makeVariable(ncfile, hcs.getGroup(), lookup, k == 0));
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

  // make coordinate system by taking the union of all time steps, vertical levels
  // means that there can be a fair amount of missing data
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
          ncfile.addVariable( hcs.getGroup(), pv.makeVariable(ncfile, hcs.getGroup(), lookup, true));
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
            ncfile.addVariable( hcs.getGroup(), pv.makeVariable(ncfile, hcs.getGroup(), lookup, (pv == maxV)));
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

  }


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
