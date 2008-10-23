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

package ucar.nc2.ft.point.writer;

import ucar.nc2.dt.*;
import ucar.nc2.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.iosp.netcdf3.N3outputStreamWriter;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.*;
import ucar.ma2.DataType;

import java.util.*;
import java.io.*;

/**
 * Write PointObsDataset in "CF" proposed point convention.
 * Also experiment with streaming netcdf.
 *
 * @author caron
 */
public class WriterCFPointObsDataset {
  private static final String recordDimName = "record";
  private static final String latName = "latitude";
  private static final String lonName = "longitude";
  private static final String altName = "altitude";
  private static final String timeName = "time";

  private NetcdfFileStream ncfile;
  private List<Attribute> globalAtts;
  private String altUnits;

  private Set<Dimension> dimSet = new HashSet<Dimension>();
  private List<Variable> recordVars = new ArrayList<Variable>();

  private boolean useAlt = false;

  private boolean debug = false;

  public WriterCFPointObsDataset(DataOutputStream stream, List<Attribute> globalAtts, String altUnits) {
    ncfile = new NetcdfFileStream(stream);
    this.globalAtts = globalAtts;
    this.altUnits = altUnits;
    useAlt = (altUnits != null);
  }

  public void writeHeader(List<VariableSimpleIF> vars) throws IOException {
    createGlobalAttributes();
    createRecordVariables(vars);

    ncfile.finish(); // done with define mode
    ncfile.writeHeader();
  }

  private void createGlobalAttributes() {
    if (globalAtts != null) {
      for (Attribute att : globalAtts)
        ncfile.addAttribute(null, att);
    }
    ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.4"));
    ncfile.addAttribute(null, new Attribute("CF:featureType", "point"));
  }

  // private ArrayInt.D1 timeArray = new ArrayInt.D1(1);
  //private ArrayInt.D1 parentArray = new ArrayInt.D1(1);

  private void createRecordVariables(List<VariableSimpleIF> simpleVars) {

    ncfile.addDimension(null, new Dimension(recordDimName, 0, true, true, false));

    // time variable
    Variable timeVar = ncfile.addVariable(null, timeName, DataType.INT, recordDimName);
    timeVar.addAttribute(new Attribute("units", "secs since 1970-01-01 00:00:00"));
    timeVar.addAttribute(new Attribute("long_name", "date/time of observation"));
    recordVars.add(timeVar);

    // latitude variable
    Variable latVar = ncfile.addVariable(null, latName, DataType.DOUBLE, recordDimName);
    latVar.addAttribute(new Attribute("units", "degrees_north"));
    latVar.addAttribute(new Attribute("long_name", "latitude of observation"));
    latVar.addAttribute(new Attribute("standard_name", "latitude"));
    recordVars.add(latVar);

    // longitude variable
    Variable lonVar = ncfile.addVariable(null, lonName, DataType.DOUBLE, recordDimName);
    lonVar.addAttribute(new Attribute("units", "degrees_east"));
    lonVar.addAttribute(new Attribute("long_name", "longitude of observation"));
    lonVar.addAttribute(new Attribute("standard_name", "longitude"));
    recordVars.add(lonVar);

    if (useAlt) {
      // altitude variable
      Variable altVar = ncfile.addVariable(null, altName, DataType.DOUBLE, recordDimName);
      altVar.addAttribute(new Attribute("units", altUnits));
      altVar.addAttribute(new Attribute("long_name", "altitude of observation"));
      altVar.addAttribute(new Attribute("standard_name", "longitude"));
      altVar.addAttribute(new Attribute("positive", "down")); // LOOK
      recordVars.add(altVar);
    }

    String coordinates = timeName + " " + latName + " " + lonName;
    if (useAlt) coordinates = coordinates + " " + altName;
    Attribute coordAtt = new Attribute("coordinates", coordinates);

    // find all dimensions needed by the data variables
    for (VariableSimpleIF var : simpleVars) {
      List<Dimension> dims = var.getDimensions();
      dimSet.addAll(dims);
    }

    // add them
    for (Dimension d : dimSet) {
      if (isExtraDimension(d))
        ncfile.addDimension(null, new Dimension(d.getName(), d.getLength(), d.isShared(), false, d.isVariableLength()));
    }

    // add the data variables all using the record dimension
    for (VariableSimpleIF oldVar : simpleVars) {
      List<Dimension> dims = oldVar.getDimensions();
      StringBuffer dimNames = new StringBuffer(recordDimName);
      for (Dimension d : dims) {
        if (isExtraDimension(d))
          dimNames.append(" ").append(d.getName());
      }
      Variable newVar = ncfile.addVariable(null, oldVar.getName(), oldVar.getDataType(), dimNames.toString());
      recordVars.add(newVar);

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts)
        newVar.addAttribute(att);
      newVar.addAttribute(coordAtt);
    }

  }

  // LOOK kludge to identify time dimension
  private boolean isExtraDimension(Dimension d) {
    return (!d.isUnlimited() && !d.getName().equalsIgnoreCase("time"));
  }

  public void writeRecord(double lat, double lon, double alt, Date time, double[] vals, String[] svals) throws IOException {
    int count = writeCoordinates(lat, lon, alt, time);

    Variable v;

    // double data
    for (double val : vals) {
      ArrayDouble.D0 data = new ArrayDouble.D0();
      data.set(val);
      v = recordVars.get(count++);
      v.setCachedData(data, false);
    }

    // String data
    for (String sval : svals) {
      ArrayObject.D0 data = new ArrayObject.D0(String.class);
      data.set(sval);
      v = recordVars.get(count++);
      v.setCachedData(data, false);
    }

    ncfile.writeRecordData(recordVars);
  }

  private int writeCoordinates(double lat, double lon, double alt, Date time) {
    int count = 0;

    // time
    ArrayInt.D0 tdata = new ArrayInt.D0();
    int secs = (int) (time.getTime() / 1000);
    tdata.set(secs);
    Variable v = recordVars.get(count++);
    v.setCachedData(tdata, false);

    // lat
    ArrayDouble.D0 latData = new ArrayDouble.D0();
    latData.set(lat);
    v = recordVars.get(count++);
    v.setCachedData(latData, false);

    // lon
    ArrayDouble.D0 lonData = new ArrayDouble.D0();
    lonData.set(lon);
    v = recordVars.get(count++);
    v.setCachedData(lonData, false);

    // alt
    if (useAlt) {
      ArrayDouble.D0 altData = new ArrayDouble.D0();
      altData.set(alt);
      v = recordVars.get(count++);
      v.setCachedData(altData, false);
    }

    return count;
  }

  public void writeRecord(PointObsDatatype pobs, StructureData sdata) throws IOException {
    if (debug) System.out.println("pobs= " + pobs);

    EarthLocation loc = pobs.getLocation(); // LOOK we dont know this until we see the obs
    int count = writeCoordinates(loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), pobs.getObservationTimeAsDate());

    for (int i = count; i < recordVars.size(); i++) {
      Variable v = recordVars.get(i);
      v.setCachedData(sdata.getArray(v.getShortName()), false);
    }

    ncfile.writeRecordData(recordVars);
  }

  public void finish() throws IOException {
    //writeDataFinish();
    ncfile.close();
  }

  private class NetcdfFileStream extends NetcdfFile {
    N3outputStreamWriter swriter;
    DataOutputStream stream;

    NetcdfFileStream(DataOutputStream stream) {
      super();
      this.stream = stream;
      swriter = new N3outputStreamWriter(this);
    }

    void writeHeader() throws IOException {
      swriter.writeHeader(stream);
    }

    void writeNonRecordData(String varName, Array data) throws IOException {
      swriter.writeNonRecordData(findVariable(varName), stream, data);
    }

    void writeRecordData(List<Variable> varList) throws IOException {
      swriter.writeRecordData(stream, varList);
    }
  }


  //////////////////////////////////////////////////////////////////////////////////
  public static void rewrite(String fileIn, String fileOut, boolean inMemory, boolean sort) throws IOException {
    System.out.println("Rewrite .nc files from " + fileIn + " to " + fileOut + "inMem= " + inMemory + " sort= " + sort);

    long start = System.currentTimeMillis();

    // do it in memory for speed
    NetcdfFile ncfile = inMemory ? NetcdfFile.openInMemory(fileIn) : NetcdfFile.open(fileIn);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);

    StringBuilder errlog = new StringBuilder();
    PointObsDataset pobsDataset = (PointObsDataset) TypedDatasetFactory.open(FeatureType.POINT, ncd, null, errlog);

    FileOutputStream fos = new FileOutputStream(fileOut);
    DataOutputStream out = new DataOutputStream(fos);
    WriterCFPointObsDataset writer = null;

    boolean first = true;
    DataIterator iter = pobsDataset.getDataIterator(1000 * 1000);
    while (iter.hasNext()) {
      PointObsDatatype pobsData = (PointObsDatatype) iter.nextData();
      StructureData data = pobsData.getData();
      if (first) {
        EarthLocation loc = pobsData.getLocation(); // LOOK we dont know this until we see the obs
        String altUnits = Double.isNaN(loc.getAltitude()) ? null : "meters";
        writer = new WriterCFPointObsDataset(out, ncfile.getGlobalAttributes(), altUnits);
        writer.writeHeader(pobsDataset.getDataVariables());
        first = false;
      }
      writer.writeRecord(pobsData, data);
    }

    writer.finish();

    long took = System.currentTimeMillis() - start;
    System.out.println("Rewrite " + fileIn + " to " + fileOut + " took = " + took);
  }

  public static void rewrite2(String fileIn, String fileOut, boolean inMemory, boolean sort) throws IOException {
    System.out.println("Rewrite2 .nc files from " + fileIn + " to " + fileOut + "inMem= " + inMemory + " sort= " + sort);

    long start = System.currentTimeMillis();

    // do it in memory for speed
    NetcdfFile ncfile = inMemory ? NetcdfFile.openInMemory(fileIn) : NetcdfFile.open(fileIn);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);

    StringBuilder errlog = new StringBuilder();
    PointObsDataset pobsDataset = (PointObsDataset) TypedDatasetFactory.open(FeatureType.POINT, ncd, null, errlog);

    FileOutputStream fos = new FileOutputStream(fileOut);
    DataOutputStream out = new DataOutputStream(fos);
    WriterCFPointObsDataset writer = new WriterCFPointObsDataset(out, ncfile.getGlobalAttributes(), "meters");
    List<VariableSimpleIF> vars = pobsDataset.getDataVariables();

    // put vars in order
    List<VariableSimpleIF> nvars = new ArrayList<VariableSimpleIF>(vars.size());
    for (VariableSimpleIF v : vars) {
      if (v.getDataType().isNumeric())
        nvars.add(v);
    }
    int ndoubles = vars.size();
    double[] dvals = new double[vars.size()];
    for (VariableSimpleIF v : vars) {
      if (v.getDataType().isString())
        nvars.add(v);
    }
    String[] svals = new String[vars.size() - ndoubles];
    writer.writeHeader(nvars);

    DataIterator iter = pobsDataset.getDataIterator(1000 * 1000);
    while (iter.hasNext()) {
      PointObsDatatype pobsData = (PointObsDatatype) iter.nextData();
      StructureData sdata = pobsData.getData();

      int dcount = 0;
      int scount = 0;
      for (VariableSimpleIF v : nvars) {
        if (v.getDataType().isNumeric()) {
          Array data = sdata.getArray(v.getShortName());
          if (data.hasNext())
            dvals[dcount++] = data.nextDouble();

        } else if (v.getDataType().isString()) {
          ArrayChar data = (ArrayChar) sdata.getArray(v.getShortName());
          svals[scount++] = data.getString();
        }
      }

      EarthLocation loc = pobsData.getLocation();
      writer.writeRecord(loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), pobsData.getObservationTimeAsDate(),
              dvals, svals);

    }

    writer.finish();

    long took = System.currentTimeMillis() - start;
    System.out.println("Rewrite2 " + fileIn + " to " + fileOut + " took = " + took);
  }

  public static void main(String args[]) throws IOException {
    String location = "R:/testdata/point/netcdf/971101.PAM_Atl_met.nc";
    File file = new File(location);
    rewrite2(location, "C:/temp/MS2" + file.getName(), true, true);
  }
}
