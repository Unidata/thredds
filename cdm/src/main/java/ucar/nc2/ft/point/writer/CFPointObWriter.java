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

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ft.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.dt.DataIterator;
import ucar.nc2.dt.PointObsDatatype;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.unidata.geoloc.EarthLocation;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;

/**
 * Write point obs data in CF obs format.
 *
 * @author caron
 * @since Oct 22, 2008
 */
public class CFPointObWriter {
  private WriterCFPointDataset ncWriter;

  /**
   * Constructor
   *
   * @param stream     write to this stream
   * @param globalAtts optional list of global attributes (may be null)
   * @param altUnits   optional altitude units (set to null if no altitude variable)
   * @param dataVars   the set of data variables: first the double values, then the String values
   * @throws IOException if write error
   */
  public CFPointObWriter(DataOutputStream stream, List<Attribute> globalAtts, String altUnits, List<PointObVar> dataVars, int numrec) throws IOException {
    ncWriter = new WriterCFPointDataset(stream, globalAtts, altUnits);

    List<VariableSimpleIF> vars = new ArrayList<VariableSimpleIF>(dataVars.size());
    for (PointObVar pvar : dataVars)
      vars.add(new PointObVarAdapter(pvar));

    ncWriter.writeHeader(vars, numrec);
  }

  /**
   * Add one data point to the file
   *
   * @param lat   latitude value in units of degrees_north
   * @param lon   longitude value in units of degrees_east
   * @param alt   altitude value in units of altUnits (may be NaN)
   * @param time  time value as a date
   * @param vals  list of data values, matching dataVars in the constructor
   * @param svals list of String values, matching dataVars in the constructor
   * @throws IOException if write error
   */
  public void addPoint(double lat, double lon, double alt, Date time, double[] vals, String[] svals) throws IOException {
    ncWriter.writeRecord(lat, lon, alt, time, vals, svals);
  }

  /**
   * Call this when all done, output is flushed
   *
   * @throws IOException if write error
   */
  public void finish() throws IOException {
    ncWriter.finish();
  }

  private class PointObVarAdapter implements VariableSimpleIF {
    PointObVar pov;
    List<Attribute> atts = new ArrayList<Attribute>(2);

    PointObVarAdapter(PointObVar pov) {
      this.pov = pov;
    }

    public String getName() {
      return pov.getName();
    }

    public String getShortName() {
      return pov.getName();
    }

    public String getDescription() {
      return pov.getDesc();
    }

    public String getUnitsString() {
      return pov.getUnits();
    }

    public int getRank() {
      return (pov.getLen() > 1) ? 1 : 0;
    }

    public int[] getShape() {
      return (pov.getLen() > 1) ? new int[]{pov.getLen()} : new int[0];
    }

    public List<Dimension> getDimensions() {
      if (pov.getLen() > 1) {
        List<Dimension> dims = new ArrayList<Dimension>(1);
        String suffix = (pov.getDataType() == DataType.STRING) || (pov.getDataType() == DataType.CHAR) ? "_strlen" : "_len";
        dims.add(new Dimension(pov.getName() + suffix, pov.getLen(), false, false, false));
        return dims;
      } else
        return new ArrayList<Dimension>(0);
    }

    public DataType getDataType() {
      return (pov.getDataType() == DataType.STRING) ? DataType.CHAR : pov.getDataType();
    }

    public List<Attribute> getAttributes() {
      if (atts == null)
        atts = new ArrayList<Attribute>(2);
      
      if (pov.getDesc() != null) atts.add(new Attribute("long_name", pov.getDesc()));
      if (pov.getUnits() != null) atts.add(new Attribute("units", pov.getUnits()));
      return atts;
    }

    public Attribute findAttributeIgnoreCase(String name) {
      for (Attribute att : getAttributes())
        if (att.getName().equalsIgnoreCase(name))
          return att;
      return null;
    }

    public int compareTo(VariableSimpleIF o) {
      return -1;
    }
  }

  /**
   * Open a ucar.nc2.dt.PointObsDataset, write out in CF point format.
   *
   * @param fileIn   open through TypedDatasetFactory.open(FeatureType.POINT, ..)
   * @param fileOut  write to this netcdf-3 file
   * @param inMemory if true, read file into memory for efficiency
   * @return true on success
   * @throws IOException on read/write error
   */
  public static boolean rewritePointObsDataset(String fileIn, String fileOut, boolean inMemory) throws IOException {
    System.out.println("Rewrite2 .nc files from " + fileIn + " to " + fileOut + " inMemory= " + inMemory);

    long start = System.currentTimeMillis();

    // do it in memory for speed
    NetcdfFile ncfile = inMemory ? NetcdfFile.openInMemory(fileIn) : NetcdfFile.open(fileIn);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);

    StringBuilder errlog = new StringBuilder();
    PointObsDataset pobsDataset = (PointObsDataset) TypedDatasetFactory.open(FeatureType.POINT, ncd, null, errlog);
    if (pobsDataset == null) return false;

    writePointObsDataset(pobsDataset, fileOut);
    pobsDataset.close();

    long took = System.currentTimeMillis() - start;
    System.out.println(" that took " + (took - start) + " msecs");
    return true;
  }

  /**
   * write data from a ucar.nc2.dt.PointObsDataset into CF point format.
   *
   * @param pobsDataset rewrite data from here
   * @param fileOut     write to tehis netcdf-3 file
   * @throws IOException on read/write error
   */
  public static void writePointObsDataset(PointObsDataset pobsDataset, String fileOut) throws IOException {

    // see if we have an altitude
    String altUnits = null;
    DataIterator iterOne = pobsDataset.getDataIterator(-1);
    while (iterOne.hasNext()) {
      PointObsDatatype pobsData = (PointObsDatatype) iterOne.nextData();
      ucar.unidata.geoloc.EarthLocation loc = pobsData.getLocation();
      altUnits = Double.isNaN(loc.getAltitude()) ? null : "meters";
      break;
    }

    List<VariableSimpleIF> vars = pobsDataset.getDataVariables();
    List<PointObVar> nvars = new ArrayList<PointObVar>(vars.size());

    // put vars in order
    for (VariableSimpleIF v : vars) {
      if (v.getDataType().isNumeric())
        nvars.add(new PointObVar(v));
    }
    int ndoubles = vars.size();
    double[] dvals = new double[ndoubles];

    for (VariableSimpleIF v : vars) {
      if (v.getDataType().isString())
        nvars.add(new PointObVar(v));
    }
    String[] svals = new String[vars.size() - ndoubles];

    FileOutputStream fos = new FileOutputStream(fileOut);
    DataOutputStream out = new DataOutputStream(fos);
    CFPointObWriter writer = new CFPointObWriter(out, pobsDataset.getGlobalAttributes(), altUnits, nvars, -1);

    DataIterator iter = pobsDataset.getDataIterator(1000 * 1000);
    while (iter.hasNext()) {
      PointObsDatatype pobsData = (PointObsDatatype) iter.nextData();
      StructureData sdata = pobsData.getData();

      int dcount = 0;
      int scount = 0;
      for (PointObVar v : nvars) {
        if (v.getDataType().isNumeric()) {
          Array data = sdata.getArray(v.getName());
          data.resetLocalIterator();
          if (data.hasNext())
            dvals[dcount++] = data.nextDouble();

        } else if (v.getDataType().isString()) {
          ArrayChar data = (ArrayChar) sdata.getArray(v.getName());
          svals[scount++] = data.getString();
        }
      }

      ucar.unidata.geoloc.EarthLocation loc = pobsData.getLocation();
      writer.addPoint(loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), pobsData.getObservationTimeAsDate(),
          dvals, svals);
    }

    writer.finish();
  }

  /**
   * Open a ucar.nc2.ft.PointFeatureCollection, write out in CF point format.
   *
   * @param fileIn   open through TypedDatasetFactory.open(FeatureType.POINT, ..)
   * @param fileOut  write to this netcdf-3 file
   * @param inMemory if true, read file into memory for efficiency
   * @return true on success
   * @throws IOException on read/write error
   */
  public static boolean rewritePointFeatureDataset(String fileIn, String fileOut, boolean inMemory) throws IOException {
    System.out.println("Rewrite2 .nc files from " + fileIn + " to " + fileOut + " inMemory= " + inMemory);

    long start = System.currentTimeMillis();

    // do it in memory for speed
    NetcdfFile ncfile = inMemory ? NetcdfFile.openInMemory(fileIn) : NetcdfFile.open(fileIn);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);

    Formatter errlog = new Formatter();
    FeatureDataset fd = FeatureDatasetFactoryManager.wrap(FeatureType.ANY_POINT, ncd, null, errlog);
    if (fd == null) return false;

    if (fd instanceof FeatureDatasetPoint) {
      writePointFeatureCollection((FeatureDatasetPoint) fd, fileOut);
      fd.close();
      long took = System.currentTimeMillis() - start;
      System.out.println(" that took " + (took - start) + " msecs");
      return true;
    }

    return false;

  }

  /**
   * Write a ucar.nc2.ft.PointFeatureCollection in CF point format.
   *
   * @param pfDataset find the first PointFeatureCollection, and write all data from it
   * @param fileOut   write to this netcdf-3 file
   * @return number of records written
   * @throws IOException on read/write error, or if no PointFeatureCollection in pfDataset
   */
  public static int writePointFeatureCollection(FeatureDatasetPoint pfDataset, String fileOut) throws IOException {
    // extract the PointFeatureCollection
    PointFeatureCollection pointFeatureCollection = null;
    List<FeatureCollection> featureCollectionList = pfDataset.getPointFeatureCollectionList();
    for (FeatureCollection featureCollection : featureCollectionList) {
      if (featureCollection instanceof PointFeatureCollection)
        pointFeatureCollection = (PointFeatureCollection) featureCollection;
    }
    if (null == pointFeatureCollection)
      throw new IOException("There is no PointFeatureCollection in  " + pfDataset.getLocation());

    long start = System.currentTimeMillis();

    FileOutputStream fos = new FileOutputStream(fileOut);
    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fos, 10000));
    WriterCFPointDataset writer = null;

    /* LOOK BAD
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
    } */

    int count = 0;
    pointFeatureCollection.resetIteration();
    while (pointFeatureCollection.hasNext()) {
      PointFeature pointFeature = (PointFeature) pointFeatureCollection.next();
      StructureData data = pointFeature.getData();
      if (count == 0) {
        EarthLocation loc = pointFeature.getLocation(); // LOOK we dont know this until we see the obs
        String altUnits = Double.isNaN(loc.getAltitude()) ? null : "meters"; // LOOK units may be wrong
        writer = new WriterCFPointDataset(out, pfDataset.getGlobalAttributes(), altUnits);
        writer.writeHeader(pfDataset.getDataVariables(), -1);
      }
      writer.writeRecord(pointFeature, data);
      count++;
    }

    writer.finish();
    out.flush();
    out.close();

    long took = System.currentTimeMillis() - start;
    System.out.printf("Write %d records from %s to %s took %d msecs %n", count, pfDataset.getLocation(), fileOut, took);
    return count;
  }

  public static void main2(String args[]) throws IOException {
    String location = "R:/testdata/point/netcdf/madis.nc";
    File file = new File(location);
    rewritePointFeatureDataset(location, "C:/TEMP/" + file.getName(), true);
  }

  public static void main(String args[]) throws IOException {

    List<PointObVar> dataVars = new ArrayList<PointObVar>();
    dataVars.add(new PointObVar("test1", "units1", "desc1", DataType.CHAR, 4));
    dataVars.add(new PointObVar("test2", "units2", "desc3", DataType.CHAR, 4));

    //   public CFPointObWriter(DataOutputStream stream, List<Attribute> globalAtts, String altUnits, List<PointObVar> dataVars) throws IOException {

    FileOutputStream fos = new FileOutputStream("C:/temp/test.out");
    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fos, 10000));

    CFPointObWriter writer = new CFPointObWriter(out, new ArrayList<Attribute>(), "meters", dataVars, 1);

    double[] dvals = new double[0];
    String[] svals = new String[] {"valu", "value"};
    writer.addPoint(1.0, 2.0, 3.0, new Date(), dvals, svals);
    writer.finish();

  }
}
