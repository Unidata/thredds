/*
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

package thredds.server.ncSubset;

import ucar.ma2.*;
import ucar.nc2.dt.*;
import ucar.nc2.dt.point.WriterStationObsDataset;
import ucar.nc2.dt.point.WriterProfileObsDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.units.DateFormatter;

import java.io.*;
import java.util.*;
import java.text.ParseException;

import thredds.datatype.DateType;

public class GridPointWriter {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridPointWriter.class);

  private static boolean debug = false;
  private static long timeToScan = 0;


  private DateFormatter format = new DateFormatter();
  private GridDataset gds;

  public GridPointWriter(GridDataset gds) {
    this.gds = gds;
  }

  private class Limit {
    int count;
    int limit = Integer.MAX_VALUE;
    int matches;
  }

  public File write(QueryParams qp, java.io.PrintWriter pw) throws IOException, InvalidRangeException {
    long starting = System.currentTimeMillis();
    Limit counter = new Limit();

    //construct the grid list
    List<GridDatatype> grids = new ArrayList<GridDatatype>();
    for (String gridName : qp.vars) {
      GridDatatype grid = gds.findGridDatatype(gridName);
      if (grid == null) continue;
      grids.add(grid);
    }
    GridAsPointDataset gap = new GridAsPointDataset(grids);
    List<Date> dates = gap.getDates(); // list of all possible dates

    // vertical coordinate : use the first grid that has a z coordinate
    double[] zValues = null;
    CoordinateAxis1D zAxis = null;
    GridDatatype useForZ = null; // use this grid to set the z coordinates in the data
    for (GridDatatype grid : grids) {
      zAxis = grid.getCoordinateSystem().getVerticalAxis();
      useForZ = grid;
      if (zAxis != null) break;
    }
    boolean hasZ = (zAxis != null);

    // decide on the subset of time wanted
    List<Date> wantDates = new ArrayList<Date>();
    if (qp.hasTimePoint) {
      long want = qp.time.getDate().getTime();
      int best_index = 0;
      long best_diff = Long.MAX_VALUE;
      for (int i = 0; i < dates.size(); i++) {
        Date date =  dates.get(i);
        long diff = Math.abs(date.getTime() - want);
        if (diff < best_diff) {
          best_index = i;
          best_diff = diff;
        }
      }
      wantDates.add(dates.get(best_index));

    } else if (qp.hasDateRange) {
      Date start = qp.getDateRange().getStart().getDate();
      Date end = qp.getDateRange().getEnd().getDate();
      for (Date date : dates) {
        if (date.before(start) || date.after(end)) continue;
        wantDates.add(date);
      }
    } else { // all
      wantDates = dates;
    }
    boolean hasMultipleTimes = wantDates.size() > 1;

    // construct the StructureData
    StructureMembers members = new StructureMembers("");
    int[] scalarShape = new int[0];

    //StructureMembers.Member stnMember = members.addMember("station", null, null, DataType.STRING, scalarShape);
    ArrayObject.D0 stnData = new ArrayObject.D0(String.class);
    StructureMembers.Member timeMember = members.addMember("date", null, null, DataType.STRING, scalarShape);
    ArrayObject.D0 timeData = new ArrayObject.D0(String.class);
    StructureMembers.Member latMember = members.addMember("lat", null, "degrees_north", DataType.DOUBLE, scalarShape);
    ArrayDouble.D0 latData = new ArrayDouble.D0();
    StructureMembers.Member lonMember = members.addMember("lon", null, "degrees_east", DataType.DOUBLE, scalarShape);
    ArrayDouble.D0 lonData = new ArrayDouble.D0();

    StructureDataW sdata = new StructureDataW(members);
    //sdata.setMemberData(stnMember, stnData);
    sdata.setMemberData(timeMember, timeData);
    sdata.setMemberData(latMember, latData);
    sdata.setMemberData(lonMember, lonData);

    // add vertical if needed
    ArrayDouble.D0 zData = new ArrayDouble.D0();
    if (hasZ) {
      StructureMembers.Member zMember = members.addMember("vertCoord", null, zAxis.getUnitsString(), DataType.DOUBLE, scalarShape);
      sdata.setMemberData(zMember, zData);

      if (qp.hasVerticalCoord) {
        zValues = new double[] {qp.vertCoord};
      } else {
        zValues = zAxis.getCoordValues();
      }
    }

    // add the grid members
    for (GridDatatype grid : grids) {
      StructureMembers.Member m = members.addMember(grid.getName(), null, grid.getUnitsString(), grid.getDataType(), scalarShape);
      Array data = Array.factory(grid.getDataType(), scalarShape);
      sdata.setMemberData(m, data);
    }

    // for now, we only have one point = one station
    String stnName = "GridPoint";
    Station s = new StationImpl( stnName, "Grid Point at lat/lon="+qp.lat+","+qp.lon, qp.lat, qp.lon, Double.NaN);
    List<Station> stnList  = new ArrayList<Station>();
    stnList.add(s);
    stnData.set(stnName);

    Writer w;
    if (qp.acceptType.equals(QueryParams.XML)) {
      w = new WriterXML(qp, qp.vars, pw);

    } else if (qp.acceptType.equals(QueryParams.CSV)) {
      w = new WriterCSV(qp, qp.vars, pw);

    } else if (qp.acceptType.equals(QueryParams.NETCDF)) {
      if (hasZ)
        w = new WriterNetcdfProfiler(qp, qp.vars, wantDates.size(), zAxis.getName(), pw);
      else
        w = new WriterNetcdfStation(qp, qp.vars, pw);

    } else {
      log.error("Unknown writer type = " + qp.acceptType);
      return null;
    }

    // write header
    w.header(members, stnList);

    // loop over data
    if (hasZ) {

      // loop over each time
      for (Date date : wantDates) {
        timeData.set(format.toDateTimeStringISO(date));

        // loop over z
        for (int i = 0; i < zValues.length; i++) {
          double zCoord = zValues[i];

          // loop over each grid
          for (GridDatatype grid : grids) {
            Array mdata = sdata.getArray(grid.getName());

            // set missing value if not available at this time, vertCoord
            if (!gap.hasVert(grid, zCoord)) {
              mdata.setDouble(mdata.getIndex(), gap.getMissingValue(grid));
              continue;
            }

            if (!gap.hasTime(grid, date)) {
              mdata.setDouble(mdata.getIndex(), gap.getMissingValue(grid));
              continue;
            }

            GridAsPointDataset.Point p = gap.readData(grid, date, zCoord, qp.lat, qp.lon);
            latData.set(p.lat);
            lonData.set(p.lon);
            if (grid == useForZ) zData.set(p.z);
            mdata.setDouble(mdata.getIndex(), p.dataValue);
          }
          w.write(stnName, date, sdata); // one structure per time step per vertical level
        }

      }

    } else {

      // loop over each time
      for (Date date : wantDates) {
        timeData.set(format.toDateTimeStringISO(date));

        // loop over each grid
        for (GridDatatype grid : grids) {
          Array mdata = sdata.getArray(grid.getName());

          // set missing value if not available at this time
          if (!gap.hasTime(grid, date)) {
            mdata.setDouble(mdata.getIndex(), gap.getMissingValue(grid));
            continue;
          }

          GridAsPointDataset.Point p = gap.readData(grid, date, qp.lat, qp.lon);
          latData.set(p.lat);
          lonData.set(p.lon);
          mdata.setDouble(mdata.getIndex(), p.dataValue);
        }
        w.write(stnName, date, sdata); // one structure per time step
      }
    }

    // finish up
    w.trailer();
    if (pw != null) pw.flush();

    if (debug) {
      long took = System.currentTimeMillis() - starting;
      System.out.println("\nread " + counter.count + " records; match and write " + counter.matches + " raw records");
      System.out.println("that took = " + took + " msecs");

      if (timeToScan > 0) {
        long writeTime = took - timeToScan;
        double mps = 1000 * counter.matches / writeTime;
        System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
      }
    }

    return w.getNetcdfFile();
  }

  abstract class Writer {
    abstract void header(StructureMembers members, List<Station> stnList);

    abstract void write(String stnName, Date obsDate, StructureData sdata) throws IOException;

    abstract void trailer();

    public File getNetcdfFile() { return null; }

    QueryParams qp;
    List<String> varNames;
    java.io.PrintWriter writer;
    DateFormatter format = new DateFormatter();
    int count = 0;

    Writer(QueryParams qp, List<String> varNames, final java.io.PrintWriter writer) {
      this.qp = qp;
      this.varNames = varNames;
      this.writer = writer;
    }

    List<VariableSimpleIF> getVars(List<String> varNames, List<VariableSimpleIF> dataVariables) {
      List<VariableSimpleIF> result = new ArrayList<VariableSimpleIF>();
      for (VariableSimpleIF v : dataVariables) {
        if ((varNames == null) || varNames.contains(v.getName()))
          result.add(v);
      }
      return result;
    }
  }

  class WriterXML extends Writer {

    WriterXML(QueryParams qp, List<String> vars, final java.io.PrintWriter writer) {
      super(qp, vars, writer);
    }

    public void header(StructureMembers members, List<Station> stnList) {
      writer.println("<?xml version='1.0' encoding='UTF-8'?>");
      writer.println("<grid dataset='" + gds.getLocationURI() + "'>");
    }

    public void trailer() {
      writer.println("</grid>");
    }

    public void write(String stnName, Date obsDate, StructureData sdata) throws IOException {
      writer.println("  <point>");
      List<StructureMembers.Member> members = (List<StructureMembers.Member>) sdata.getStructureMembers().getMembers();
      for (StructureMembers.Member m : members) {
        /* writer.print("    <" + m.getName());
        if (null != m.getUnitsString())
          writer.print(" unit=\"" + m.getUnitsString() + "\"");
        writer.print(">");
        writer.print(sdata.getScalarObject(m));
        writer.println("</" + m.getName() + ">"); */

        writer.print("    <data name='" + m.getName());
        if (m.getUnitsString() != null)
          writer.print("' units='" + m.getUnitsString());
        writer.print("'>");
        writer.print(sdata.getScalarObject(m));
        writer.println("</data>");
      }
      writer.println("  </point>");
      count++;
    }
  }


  class WriterCSV extends Writer {
    boolean headerWritten = false;
    List<VariableSimpleIF> validVars;

    WriterCSV(QueryParams qp, List<String> stns, final java.io.PrintWriter writer) {
      super(qp, stns, writer);
    }

    public void header(StructureMembers sm, List<Station> stnList) {
      boolean first = true;
      List<StructureMembers.Member> members = (List<StructureMembers.Member>) sm.getMembers();
      for (StructureMembers.Member m : members) {
        if (!first)
          writer.print(",");
        writer.print(m.getName());
        if (null != m.getUnitsString())
          writer.print("[unit=\"" + m.getUnitsString() + "\"]");
        first = false;
      }
      writer.println();
    }

    public void trailer() {
    }

    public void write(String stnName, Date obsDate, StructureData sdata) throws IOException {
      boolean first = true;
      List<StructureMembers.Member> members = (List<StructureMembers.Member>) sdata.getStructureMembers().getMembers();
      for (StructureMembers.Member m : members) {
        if (!first)
          writer.print(",");
        writer.print(sdata.getScalarObject(m));
        first = false;
      }
      writer.println();
      count++;
    }
  }

  class WriterNetcdfStation extends Writer {
    File netcdfResult;
    WriterStationObsDataset sobsWriter;
    // List<Station> stnList;
    List<VariableSimpleIF> varList;

    WriterNetcdfStation(QueryParams qp, List<String> varNames, final java.io.PrintWriter writer) throws IOException {
      super(qp, varNames, writer);

      netcdfResult = File.createTempFile("ncss", ".nc");

      sobsWriter = new WriterStationObsDataset(netcdfResult.getAbsolutePath(),
              "Extract Points data from Grid file "+ gds.getLocationURI());

      NetcdfDataset ncfile = (NetcdfDataset) gds.getNetcdfFile(); // fake-arino
      System.out.println("write to  "+netcdfResult.getPath());

      // need VariableSimpleIF for each variable
      varList = new ArrayList<VariableSimpleIF>(varNames.size());
      List<GridDatatype> grids = gds.getGrids();
      for (GridDatatype grid : grids ) {
        if (varNames.contains(grid.getName())) {
          VariableEnhanced ve = grid.getVariable();
          String dims = ""; // always scalar ????
          VariableSimpleIF want = new VariableDS( ncfile, null, null, ve.getShortName(),
                  ve.getDataType(), dims, ve.getUnitsString(), ve.getDescription());
          varList.add( want);
        }
      }

      // need list of stations
    }

    public File getNetcdfFile() { return netcdfResult; }

    public void header(StructureMembers sm, List<Station> stnList) {
      try {
        sobsWriter.writeHeader(stnList, varList);
      } catch (IOException e) {
        log.error("GridPointWriter.NetcdfWriter.header", e);
      }
    }

    public void trailer() {
      try {
        sobsWriter.finish();
      } catch (IOException e) {
        log.error("GridPointWriter.WriterNetcdf.trailer", e);
      }
    }

    public void write(String stnName, Date obsDate, StructureData sdata) throws IOException {
      sobsWriter.writeRecord(stnName, obsDate, sdata);
      count++;
    }
  }

  class WriterNetcdfProfiler extends Writer {
    File netcdfResult;
    WriterProfileObsDataset pobsWriter;
    List<VariableSimpleIF> varList;
    int nprofilers;
    String altVarName;

    WriterNetcdfProfiler(QueryParams qp, List<String> varNames, int nprofilers, String altVarName, final java.io.PrintWriter writer) throws IOException {
      super(qp, varNames, writer);
      this.nprofilers = nprofilers;
      this.altVarName = altVarName;

      netcdfResult = File.createTempFile("ncss", ".nc");

      pobsWriter = new WriterProfileObsDataset(netcdfResult.getAbsolutePath(),
              "Extract Profiler data from Grid file "+ gds.getLocationURI());

      NetcdfDataset ncfile = (NetcdfDataset) gds.getNetcdfFile(); // fake-arino
      System.out.println("write to  "+netcdfResult.getPath());

      // need VariableSimpleIF for each variable
      varList = new ArrayList<VariableSimpleIF>(varNames.size());
      List<GridDatatype> grids = gds.getGrids();
      for (GridDatatype grid : grids ) {
        if (varNames.contains(grid.getName())) {
          VariableEnhanced ve = grid.getVariable();
          String dims = ""; // always scalar ????
          VariableSimpleIF want = new VariableDS( ncfile, null, null, ve.getShortName(),
                  ve.getDataType(), dims, ve.getUnitsString(), ve.getDescription());
          varList.add( want);
        }
      }

      // add vertical coordinate
      VariableDS vertCoord = (VariableDS) ncfile.findVariable(altVarName);
      if (vertCoord != null) {
        VariableSimpleIF want = new VariableDS( ncfile, null, null, vertCoord.getShortName(),
               vertCoord.getDataType(), "", vertCoord.getUnitsString(), vertCoord.getDescription());
        varList.add( want);
      }

    }

    public File getNetcdfFile() {
      return netcdfResult; 
    }

    public void header(StructureMembers sm, List<Station> stnList) {
      try {
        pobsWriter.writeHeader(stnList, varList, nprofilers, altVarName);
      } catch (IOException e) {
        log.error("GridPointWriter.NetcdfWriter.header", e);
      }
    }

    public void trailer() {
      try {
        pobsWriter.finish();
      } catch (IOException e) {
        log.error("GridPointWriter.WriterNetcdf.trailer", e);
      }
    }

    public void write(String stnName, Date obsDate, StructureData sdata) throws IOException {
      pobsWriter.writeRecord(stnName, obsDate, sdata);
      count++;
    }
  }

  static public void main(String args[]) throws IOException, InvalidRangeException, ParseException {
    String fileIn = "C:/data/grib/nam/conus80/NAM_CONUS_80km_20060812_0000.grib1";
    ucar.nc2.dt.GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(fileIn);

    GridPointWriter writer = new GridPointWriter(gds);
    QueryParams qp = new QueryParams();
    qp.acceptType = QueryParams.NETCDF;

    qp.vars = new ArrayList<String>();
    qp.vars.add("Temperature");
    qp.vars.add("Absolute_vorticity");

    qp.hasLatlonPoint = true;
    qp.lat = 40.0;
    qp.lon = -105;

    qp.hasTimePoint = false;
    DateFormatter format = new DateFormatter();
    qp.time = new DateType(false, format.isoDateTimeFormat("2005-12-07T06:00:00Z"));

    qp.hasVerticalCoord = true;
    qp.vertCoord = 223.0;
    PrintWriter pw = new PrintWriter(System.out);

    writer.write(qp, pw);

    qp.acceptType = QueryParams.CSV;
    writer.write(qp, pw);
  }

}
