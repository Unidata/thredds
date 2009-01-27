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

package ucar.nc2.ft.point.writer;

import ucar.nc2.*;
import ucar.nc2.dt.PointObsDatatype;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.dt.DataIterator;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.CF;
import ucar.nc2.iosp.netcdf3.N3outputStreamWriter;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.*;
import ucar.ma2.DataType;

import java.util.*;
import java.io.*;

/**
 * Write point data in CF point convention.
 * Also experiment with streaming netcdf.
 *
 * @see "http://cf-pcmdi.llnl.gov/trac/wiki/PointObservationConventions"
 * @author caron
 */
public class WriterCFPointObsDataset {
  private static final String recordDimName = "record";
  private static final String latName = "latitude";
  private static final String lonName = "longitude";
  private static final String altName = "altitude";
  private static final String timeName = "time";

  private NetcdfFileStream ncfileOut;
  private List<Attribute> globalAtts;
  private String altUnits;

  private Set<Dimension> dimSet = new HashSet<Dimension>();
  private List<Variable> recordVars = new ArrayList<Variable>();

  private boolean useAlt = false;

  private boolean debug = false;

  public WriterCFPointObsDataset(DataOutputStream stream, List<Attribute> globalAtts, String altUnits) {
    ncfileOut = new NetcdfFileStream(stream);
    this.globalAtts = globalAtts;
    this.altUnits = altUnits;
    useAlt = (altUnits != null);
  }

  public void writeHeader(List<? extends VariableSimpleIF> vars) throws IOException {
    createGlobalAttributes();
    createRecordVariables(vars);

    ncfileOut.finish(); // done with define mode
    ncfileOut.writeHeader();
  }

  private void createGlobalAttributes() {
    if (globalAtts != null) {
      for (Attribute att : globalAtts) {
        if (att.getName().equalsIgnoreCase("cdm_data_type")) continue;
        if (att.getName().equalsIgnoreCase("cdm_datatype")) continue;
        if (att.getName().equalsIgnoreCase("thredds_data_type")) continue;
        
        ncfileOut.addAttribute(null, att);
      }
    }
    ncfileOut.addAttribute(null, new Attribute("Conventions", "CF-1"));  // LOOK CF-1.?
    ncfileOut.addAttribute(null, new Attribute("CF:featureType", CF.FeatureType.point.name()));
  }

  // private ArrayInt.D1 timeArray = new ArrayInt.D1(1);
  //private ArrayInt.D1 parentArray = new ArrayInt.D1(1);

  private void createRecordVariables(List<? extends VariableSimpleIF> dataVars) {

    ncfileOut.addDimension(null, new Dimension(recordDimName, 0, true, true, false));

    // time variable
    Variable timeVar = ncfileOut.addVariable(null, timeName, DataType.INT, recordDimName);
    timeVar.addAttribute(new Attribute("units", "secs since 1970-01-01 00:00:00"));
    timeVar.addAttribute(new Attribute("long_name", "date/time of observation"));
    recordVars.add(timeVar);

    // latitude variable
    Variable latVar = ncfileOut.addVariable(null, latName, DataType.DOUBLE, recordDimName);
    latVar.addAttribute(new Attribute("units", "degrees_north"));
    latVar.addAttribute(new Attribute("long_name", "latitude of observation"));
    latVar.addAttribute(new Attribute("standard_name", "latitude"));
    recordVars.add(latVar);

    // longitude variable
    Variable lonVar = ncfileOut.addVariable(null, lonName, DataType.DOUBLE, recordDimName);
    lonVar.addAttribute(new Attribute("units", "degrees_east"));
    lonVar.addAttribute(new Attribute("long_name", "longitude of observation"));
    lonVar.addAttribute(new Attribute("standard_name", "longitude"));
    recordVars.add(lonVar);

    if (useAlt) {
      // altitude variable
      Variable altVar = ncfileOut.addVariable(null, altName, DataType.DOUBLE, recordDimName);
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
    for (VariableSimpleIF var : dataVars) {
      List<Dimension> dims = var.getDimensions();
      dimSet.addAll(dims);
    }

    // add them
    for (Dimension d : dimSet) {
      if (isExtraDimension(d))
        ncfileOut.addDimension(null, new Dimension(d.getName(), d.getLength(), true, false, d.isVariableLength()));
    }

    // add the data variables all using the record dimension
    for (VariableSimpleIF oldVar : dataVars) {
      if (ncfileOut.findVariable(oldVar.getShortName()) != null) continue;
      List<Dimension> dims = oldVar.getDimensions();
      StringBuffer dimNames = new StringBuffer(recordDimName);
      for (Dimension d : dims) {
        if (isExtraDimension(d))
          dimNames.append(" ").append(d.getName());
      }
      Variable newVar = ncfileOut.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), dimNames.toString());
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

    ncfileOut.writeRecordData(recordVars);
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

  public void writeRecord(PointFeature pf, StructureData sdata) throws IOException {
    if (debug) System.out.println("PointFeature= " + pf);

    ucar.nc2.ft.EarthLocation loc = pf.getLocation();
    int count = writeCoordinates(loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), pf.getObservationTimeAsDate());

    for (int i = count; i < recordVars.size(); i++) {
      Variable v = recordVars.get(i);
      v.setCachedData(sdata.getArray(v.getShortName()), false);
    }

    ncfileOut.writeRecordData(recordVars);
  }

  public void writeRecord(PointObsDatatype pobs, StructureData sdata) throws IOException {
    if (debug) System.out.println("pobs= " + pobs);

    ucar.nc2.dt.EarthLocation loc = pobs.getLocation();
    int count = writeCoordinates(loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), pobs.getObservationTimeAsDate());

    for (int i = count; i < recordVars.size(); i++) {
      Variable v = recordVars.get(i);
      assert v.hasCachedData();
      v.setCachedData(sdata.getArray(v.getShortName()), false);
    }

    ncfileOut.writeRecordData(recordVars);
  }

  public void finish() throws IOException {
    //writeDataFinish();
    ncfileOut.close();
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

  /**
   * Write a ucar.nc2.ft.PointFeatureCollection in CF point format.
   *
   * @param pfDataset find the first PointFeatureCollection, and write all data from it
   * @param fileOut write to this netcdf-3 file
   * @return number of records written
   * @throws IOException on read/write error, or if no PointFeatureCollection in pfDataset
   */
  public static int writePointFeatureCollection(FeatureDatasetPoint pfDataset, String fileOut) throws IOException {
    // extract the PointFeatureCollection
    PointFeatureCollection pointFeatureCollection = null;
    List<FeatureCollection> featureCollectionList = pfDataset.getPointFeatureCollectionList();
    for ( FeatureCollection featureCollection : featureCollectionList) {
      if (featureCollection instanceof PointFeatureCollection)
        pointFeatureCollection = (PointFeatureCollection) featureCollection;
    }
    if (null == pointFeatureCollection)
      throw new IOException("There is no PointFeatureCollection in  "+pfDataset.getLocationURI());

     long start = System.currentTimeMillis();

    FileOutputStream fos = new FileOutputStream(fileOut);
    DataOutputStream out = new DataOutputStream( new BufferedOutputStream(fos, 10000));
    WriterCFPointObsDataset writer = null;

    // LOOK BAD
    List<VariableSimpleIF> dataVars = new ArrayList<VariableSimpleIF>();
    ucar.nc2.NetcdfFile ncfile = pfDataset.getNetcdfFile();
    if ((ncfile == null) || !(ncfile instanceof NetcdfDataset))  {
      dataVars.addAll(pfDataset.getDataVariables());
    } else {
      NetcdfDataset ncd = (NetcdfDataset) ncfile;
      for (VariableSimpleIF vs : pfDataset.getDataVariables()) {
        if (ncd.findCoordinateAxis(vs.getName()) == null)
          dataVars.add(vs);
      }
    }

    int count = 0;
    pointFeatureCollection.resetIteration();
    while (pointFeatureCollection.hasNext()) {
      PointFeature pointFeature = (PointFeature) pointFeatureCollection.next();
      StructureData data = pointFeature.getData();
      if (count == 0) {
        ucar.nc2.ft.EarthLocation loc = pointFeature.getLocation(); // LOOK we dont know this until we see the obs
        String altUnits = Double.isNaN(loc.getAltitude()) ? null : "meters"; // LOOK units may be wrong
        writer = new WriterCFPointObsDataset(out, pfDataset.getGlobalAttributes(), altUnits);
        writer.writeHeader( dataVars);
      }
      writer.writeRecord(pointFeature, data);
      count++;
    }

    writer.finish();
    out.flush();
    out.close();

    long took = System.currentTimeMillis() - start;
    System.out.printf("Write %d records from %s to %s took %d msecs %n", count, pfDataset.getLocationURI(),fileOut,took);
    return count;
  }

  /**
   * Open a ucar.nc2.dt.PointObsDataset, write out in CF point format.
   *
   * @param fileIn open through TypedDatasetFactory.open(FeatureType.POINT, ..)
   * @param fileOut write to tehis netcdf-3 file
   * @param inMemory  if true, write in memory for efficiency
   * @throws IOException on read/write error
   */
  public static void rewritePointObsDataset(String fileIn, String fileOut, boolean inMemory) throws IOException {
    System.out.println("Rewrite .nc files from " + fileIn + " to " + fileOut + "inMem= " + inMemory);

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
        ucar.nc2.dt.EarthLocation loc = pobsData.getLocation();
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

  public static void main(String args[]) throws IOException {
    String location = "C:/data/ft/point/971201.PAM_Cle_met.nc";
    File file = new File(location);
    rewritePointObsDataset(location, "C:/data/temp/CFobs/"+ file.getName(), true);
  }
}
