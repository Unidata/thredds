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
package ucar.nc2.dt.radial;


import ucar.ma2.*;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateUnit;

import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

import java.util.*;

/**
 * CF-Radial
 */
public class CFRadialAdapter extends AbstractRadialAdapter {

  private NetcdfDataset ds = null;
  private double latv, lonv, elev;
  private double[] time;
  private float[] elevation;
  private float[] azimuth;
  private float[] range;
  private int[] rayStartIdx;
  private int[] rayEndIdx;
  private int[] ray_n_gates;
  private int[] ray_start_index;
  private int nsweeps;

  /////////////////////////////////////////////////
  // TypedDatasetFactoryIF

  public Object isMine( FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException {
    String convStr = ncd.findAttValueIgnoreCase(null, "Conventions", null);
    if ((null != convStr) && convStr.startsWith("CF/Radial"))
      return this;
    return null;
  }

  public FeatureDataset open( FeatureType ftype, NetcdfDataset ncd, Object analysis, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    return new CFRadialAdapter(ncd);
  }

  public FeatureType getScientificDataType() {
    return FeatureType.RADIAL;
  }

  // needed for FeatureDatasetFactory
  public CFRadialAdapter() {
  }

  /**
   * Constructor.
   *
   * @param ds Source NetCDF dataset
   */
  public CFRadialAdapter(NetcdfDataset ds) {
    this.ds = ds;
    desc = "CF/Radial radar dataset";
    init();

    for (Variable var : ds.getVariables()) {
      addRadialVariable(ds, var);
    }
  }

  public void init() {

    setEarthLocation();
    try {
      Variable t = ds.findVariable("time");
      Array tArray = t.read();
      time = (double[]) tArray.copyTo1DJavaArray();

      Variable ele = ds.findVariable("elevation");
      Array eArray = ele.read();
      elevation = (float[]) eArray.copyTo1DJavaArray();

      Variable azi = ds.findVariable("azimuth");
      Array aArray = azi.read();
      azimuth = (float[]) aArray.copyTo1DJavaArray();

      Variable rng = ds.findVariable("range");
      Array rArray = rng.read();
      range = (float[]) rArray.copyTo1DJavaArray();

      Variable sidx0 = ds.findVariable("sweep_start_ray_index");
      rayStartIdx = (int[]) sidx0.read().copyTo1DJavaArray();

      Variable sidx1 = ds.findVariable("sweep_end_ray_index");
      rayEndIdx = (int[]) sidx1.read().copyTo1DJavaArray();

      nsweeps = ds.findDimension("sweep").getLength();

      Variable var = ds.findVariable("ray_n_gates");
      if (var != null)
          ray_n_gates = (int[]) var.read().copyTo1DJavaArray();

      var = ds.findVariable("ray_start_index");
      if (var != null)
          ray_start_index = (int[]) var.read().copyTo1DJavaArray();

      setTimeUnits();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    setStartDate();
    setEndDate();
    setBoundingBox();
  }

  protected void setBoundingBox() {
    LatLonRect bb;

    if (origin == null) {
      return;
    }

    double dLat = Math.toDegrees(getMaximumRadialDist()
            / Earth.getRadius());
    double latRadians = Math.toRadians(origin.getLatitude());
    double dLon = dLat * Math.cos(latRadians);

    double lat1 = origin.getLatitude() - dLat / 2;
    double lon1 = origin.getLongitude() - dLon / 2;
    bb = new LatLonRect(new LatLonPointImpl(lat1, lon1), dLat, dLon);

    boundingBox = bb;
  }

  double getMaximumRadialDist() {
    double maxdist = 0.0;

    for (Object dataVariable : dataVariables) {
      RadialVariable rv = (RadialVariable) dataVariable;
      Sweep sp = rv.getSweep(0);
      double dist = sp.getGateNumber() * sp.getGateSize();

      if (dist > maxdist) {
        maxdist = dist;
      }
    }

    return maxdist;
  }

  protected void setEarthLocation() {

    try {
      Variable ga = ds.findVariable("latitude");
      if (ga != null) {
        latv = ga.readScalarDouble();
      } else {
        latv = 0.0;
      }

      ga = ds.findVariable("longitude");

      if (ga != null) {
        lonv = ga.readScalarDouble();
      } else {
        lonv = 0.0;
      }

      ga = ds.findVariable("altitude");
      if (ga != null) {
        elev = ga.readScalarDouble();
      } else {
        elev = 0.0;
      }
    } catch (IOException e) {
    }

    origin = new ucar.unidata.geoloc.EarthLocationImpl(latv, lonv, elev);
  }

  @Override
  public ucar.unidata.geoloc.EarthLocation getCommonOrigin() {
    return origin;
  }

  @Override
  public String getRadarID() {
    Attribute ga = ds.findGlobalAttribute("Station");
    if (ga != null) {
      return ga.getStringValue();
    } else {
      return "XXXX";
    }
  }

  @Override
  public String getRadarName() {
    Attribute ga = ds.findGlobalAttribute("instrument_name");
    if (ga != null) {
      return ga.getStringValue();
    } else {
      return "Unknown Station";
    }
  }

  @Override
  public String getDataFormat() {
    return "CF/RadialNetCDF";
  }


  @Override
  public boolean isVolume() {
    return true;
  }

  public boolean isHighResolution(NetcdfDataset nds) {
    return true;
  }

  public boolean isStationary() {
    Variable lat = ds.findVariable("latitude");
    return lat.getSize() == 1;
  }

  protected void setTimeUnits() throws Exception {
    Variable t = ds.findVariable("time");
    String ut = t.getUnitsString();
    dateUnits = new DateUnit(ut);
    calDateUnits = CalendarDateUnit.of(null, ut);
  }

  protected void setStartDate() {
    String datetime = ds.findAttValueIgnoreCase(null, "time_coverage_start", null);
    if (datetime != null) {
      startDate = CalendarDate.parseISOformat(null, datetime).toDate();
    } else {
      startDate = calDateUnits.makeCalendarDate(time[0]).toDate();
    }
  }

  protected void setEndDate() {
    String datetime = ds.findAttValueIgnoreCase(null, "time_coverage_end", null);
    if (datetime != null) {
      endDate = CalendarDate.parseISOformat(null, datetime).toDate();
    } else {
      endDate = calDateUnits.makeCalendarDate(time[time.length - 1]).toDate();
    }
  }

  public void clearDatasetMemory() {
    for (VariableSimpleIF rvar : getDataVariables()) {
        RadialVariable radVar = (RadialVariable) rvar;
        radVar.clearVariableMemory();
    }
  }


  /**
   * _more_
   *
   * @param nds _more_
   * @param var _more_
   */
  protected void addRadialVariable(NetcdfDataset nds, Variable var) {

    RadialVariable rsvar = null;
    String vName = var.getShortName();
    int tIdx = var.findDimensionIndex("time");
    int rIdx = var.findDimensionIndex("range");
    int ptsIdx = var.findDimensionIndex("n_points");

    if (((tIdx == 0) && (rIdx == 1)) || (ptsIdx == 0)) {
      VariableSimpleIF v = new MyRadialVariableAdapter(vName, var.getAttributes());
      rsvar = makeRadialVariable(nds, v, var);
    }

    if (rsvar != null) {
      dataVariables.add(rsvar);
    }
  }


  /**
   * _more_
   *
   * @param nds _more_
   * @param v   _more_
   * @param v0  _more_
   * @return _more_
   */
  protected RadialVariable makeRadialVariable(NetcdfDataset nds,
                                              VariableSimpleIF v, Variable v0) {
    // this function is null in level 2
    return new CFRadial2Variable(nds, v0);
  }

  /**
   * _more_
   *
   * @return _more_
   */
  public String getInfo() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("CFRadial2Dataset\n");
    sbuff.append(super.getDetailInfo());
    sbuff.append("\n\n");
    sbuff.append(parseInfo.toString());
    return sbuff.toString();
  }


  /**
   * Class description
   *
   * @author Enter your name here...
   * @version Enter version here..., Mon, Jun 13, '11
   */
  private class CFRadial2Variable extends MyRadialVariableAdapter implements RadialDatasetSweep.RadialVariable {

    /**
     * _more_
     */
    ArrayList<CFRadial2Sweep> sweeps;

    /**
     * _more_
     */
    String name;
    private boolean flattened;


    /**
     * _more_
     *
     * @param nds _more_
     * @param v0  _more_
     */
    private CFRadial2Variable(NetcdfDataset nds, Variable v0) {
      super(v0.getShortName(), v0.getAttributes());

      sweeps = new ArrayList<>();
      name = v0.getShortName();

      int[] shape = v0.getShape();
      int ngates = shape[v0.getRank() - 1];
      flattened = v0.findDimensionIndex("n_points") == 0;

      for (int i = 0; i < nsweeps; i++) {
        // For flattened (1D stored data) find max number of gates
        if (flattened) {
            ngates = ray_n_gates[rayStartIdx[i]];
            for (int ray = rayStartIdx[i]; ray <= rayEndIdx[i]; ++ray)
                ngates = ray_n_gates[ray] > ngates ? ray_n_gates[ray] : ngates;
        }
        sweeps.add(new CFRadial2Sweep(v0, i, ngates, rayStartIdx[i],
                rayEndIdx[i]));
      }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
      return name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getNumSweeps() {
      return nsweeps;
    }

    /**
     * _more_
     *
     * @param sweepNo _more_
     * @return _more_
     */
    public Sweep getSweep(int sweepNo) {
      return sweeps.get(sweepNo);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getNumRadials() {
      return azimuth.length;
    }

    // a 3D array nsweep * nradials * ngates
    // if high resolution data, it will be transferred to the same dimension

    /**
     * _more_
     *
     * @return _more_
     * @throws IOException _more_
     */
    public float[] readAllData() throws IOException {
      Array allData;
      Sweep spn = sweeps.get(0);
      Variable v = spn.getsweepVar();
      Attribute missing = v.findAttribute("_FillValue");
      float missingVal = missing == null ?
              Float.NaN : missing.getNumericValue().floatValue();

      int minRadial = getMinRadialNumber();
      int radials = getNumRadials();
      int gates = range.length;
      try {
        allData = v.read();
      } catch (IOException e) {
        throw new IOException(e.getMessage());
      }
      if (flattened) {
          float[] fa0 = (float[]) allData.get1DJavaArray(float.class);
          float[] fa = new float[minRadial * gates * nsweeps];
          Arrays.fill(fa, missingVal);
          for (int s = 0; s < nsweeps; ++s) {
              for (int r = 0; r < minRadial; ++r) {
                  System.arraycopy(fa0, ray_start_index[rayStartIdx[s] + r],
                          fa, s * minRadial * gates + r * gates,
                          ray_n_gates[rayStartIdx[s] + r]);
              }
          }

          return fa;
      } else if (minRadial == radials) {
        return (float[]) allData.get1DJavaArray(float.class);
      } else {
        float[] fa = new float[minRadial * gates * nsweeps];
        float[] fa0 = (float[]) allData.get1DJavaArray(float.class);
        int pos = 0;
        for (int i = 0; i < nsweeps; i++) {

          int startIdx = rayStartIdx[i];
          // int endIdx = rayEndIdx[i];
          int len = minRadial * gates;
          System.arraycopy(fa0, startIdx * gates, fa, pos, len);
          pos = pos + len;
        }
        return fa;
      }
    }


    public int getMinRadialNumber() {
      int minRadialNumber = Integer.MAX_VALUE;
      for (int i = 0; i < nsweeps; i++) {
        Sweep swp = this.sweeps.get(i);
        int radialNumber = swp.getRadialNumber();
        if (radialNumber < minRadialNumber) {
          minRadialNumber = radialNumber;
        }
      }

      return minRadialNumber;
    }

    /**
     * _more_
     */
    public void clearVariableMemory() {
      for (int i = 0; i < nsweeps; i++) {
      }
    }


    //////////////////////////////////////////////////////////////////////
    // Checking all azi to make sure there is no missing data at sweep
    // level, since the coordinate is 1D at this level, this checking also
    // remove those missing radials within a sweep.

    /**
     * Class description
     *
     * @author Enter your name here...
     * @version Enter version here..., Mon, Jun 13, '11
     */
    private class CFRadial2Sweep implements RadialDatasetSweep.Sweep {

      /**
       * _more_
       */
      double meanElevation = Double.NaN;

      /**
       * _more_
       */
      double meanAzimuth = Double.NaN;

      /**
       * _more_
       */
      int ngates;

      /**
       * _more_
       */
      public int startIdx, endIdx, numRays;

      /**
       * _more_
       */
      int sweepno;

      /**
       * _more_
       */
      Variable sweepVar;


      /**
       * _more_
       *
       * @param v        _more_
       * @param sweepno  _more_
       * @param gates    _more_
       * @param startIdx _more_
       * @param endIdx   _more_
       */
      CFRadial2Sweep(Variable v, int sweepno, int gates,
                     int startIdx, int endIdx) {
        this.sweepVar = v;
        this.sweepno = sweepno;
        this.ngates = gates;
        this.startIdx = startIdx;
        this.endIdx = endIdx;
        this.numRays = endIdx - startIdx + 1;
      }

      public int getStartIdx() {
        return startIdx;
      }

      public int getEndIdx() {
        return endIdx;
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public Variable getsweepVar() {
        return sweepVar;
      }

            /* read 2d sweep data nradials * ngates */

      /**
       * _more_
       *
       * @return _more_
       * @throws java.io.IOException _more_
       */
      public float[] readData() throws java.io.IOException {
        return sweepData();
      }


      /**
       * _more_
       *
       * @return _more_
       */
      private float[] sweepData() throws IOException {
        int[] origin;
        int[] shape;

        // init section
        try {
            if (flattened) {
                // Get the 1D data for the sweep
                origin = new int[1];
                origin[0] = ray_start_index[startIdx];
                shape = new int[1];
                shape[0] = ray_start_index[endIdx] + ray_n_gates[endIdx] -
                        origin[0];
                Array tempArray = sweepVar.read(origin, shape).reduce();
                float[] tempD = (float[]) tempArray.get1DJavaArray(Float.TYPE);

                // Figure out what to use as the initializer
                float missingVal = Float.NaN;
                Attribute missing = sweepVar.findAttribute("_FillValue");
                if (missing != null)
                    missingVal = missing.getNumericValue().floatValue();

                // Create evenly strided output array and fill
                float[] ret = new float[ngates * numRays];
                Arrays.fill(ret, missingVal);
                int srcInd = 0;
                for (int ray = 0; ray < numRays; ++ray) {
                    int gates = ray_n_gates[startIdx + ray];
                    System.arraycopy(tempD, srcInd, ret, ray * ngates, gates);
                    srcInd += gates;
                }

                return ret;
            } else {
                origin = new int[2];
                origin[0] = startIdx;
                shape = sweepVar.getShape();
                shape[0] = numRays;
                Array sweepTmp = sweepVar.read(origin, shape).reduce();
                return (float[]) sweepTmp.get1DJavaArray(Float.TYPE);
            }
        } catch (ucar.ma2.InvalidRangeException e) {
          throw new IOException(e);
        }
      }

      /**
       * Return data for 1 ray
       *
       * @param ray _more_
       * @return _more_
       * @throws java.io.IOException _more_
       */
      public float[] readData(int ray) throws java.io.IOException {
        return rayData(ray);
      }

            /* read the radial data from the radial variable */

      /**
       * _more_
       *
       * @param ray _more_
       * @return _more_
       * @throws java.io.IOException _more_
       */
      public float[] rayData(int ray) throws java.io.IOException {
        int[] origin;
        int[] shape;

        // init section
        if (flattened) {
            origin = new int[1];
            origin[0] = ray_start_index[startIdx + ray];
            shape = new int[1];
            shape[0] = ray_n_gates[startIdx + ray];
        } else {
            origin = new int[2];
            origin[0] = startIdx + ray;
            shape = sweepVar.getShape();
            shape[0] = 1;
        }

        try {
          Array sweepTmp = sweepVar.read(origin, shape).reduce();
          return (float[]) sweepTmp.get1DJavaArray(Float.TYPE);
        } catch (ucar.ma2.InvalidRangeException e) {
          throw new IOException(e);
        }
      }


      /**
       * _more_
       */
      public void setMeanElevation() {
        double sum = 0.0;
        int sumSize = 0;
        for (int i = 0; i < numRays; i++) {
          if (!Double.isNaN(elevation[i])) {
            sum = sum + elevation[startIdx + i];
            sumSize++;
          }
        }
        if (sumSize > 0)
            meanElevation = sum / sumSize;
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public float getMeanElevation() {
        if (Double.isNaN(meanElevation)) {
          setMeanElevation();
        }
        return (float) meanElevation;
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public int getGateNumber() {
        return ngates;
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public int getRadialNumber() {
        return numRays;
      }


      /**
       * _more_
       *
       * @return _more_
       */
      public RadialDatasetSweep.Type getType() {
        return null;
      }

      /**
       * _more_
       *
       * @param ray _more_
       * @return _more_
       */
      public ucar.unidata.geoloc.EarthLocation getOrigin(int ray) {
        return origin;
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public Date getStartingTime() {
        return startDate;
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public Date getEndingTime() {
        return endDate;
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public int getSweepIndex() {
        return sweepno;
      }


      /**
       * _more_
       */
      public void setMeanAzimuth() {
        double sum = 0.0;
        int sumSize = 0;
        for (int i = 0; i < numRays; i++) {
          if (!Double.isNaN(azimuth[i])) {
            sum = sum + azimuth[startIdx + i];
            sumSize++;
          }
        }
        if (sumSize > 0)
            meanAzimuth = sum / sumSize;
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public float getMeanAzimuth() {
        if (Double.isNaN(meanAzimuth)) {
          setMeanAzimuth();
        }
        return (float) meanAzimuth;
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public boolean isConic() {
        return true;
      }

      /**
       * _more_
       *
       * @param ray _more_
       * @return _more_
       * @throws IOException _more_
       */
      public float getElevation(int ray) throws IOException {
        return elevation[ray + startIdx];
      }

      /**
       * _more_
       *
       * @return _more_
       * @throws IOException _more_
       */
      public float[] getElevation() throws IOException {
        float[] elev = new float[numRays];
        System.arraycopy(elevation, startIdx, elev, 0, numRays);
        return elev;
      }


      /**
       * _more_
       *
       * @return _more_
       * @throws IOException _more_
       */
      public float[] getAzimuth() throws IOException {
        float[] azimu = new float[numRays];
        System.arraycopy(azimuth, startIdx, azimu, 0, numRays);
        return azimu;
      }


      /**
       * _more_
       *
       * @param ray _more_
       * @return _more_
       * @throws IOException _more_
       */
      public float getAzimuth(int ray) throws IOException {
        return azimuth[ray + startIdx];
      }


      /**
       * _more_
       *
       * @param gate _more_
       * @return _more_
       * @throws IOException _more_
       */
      public float getRadialDistance(int gate) throws IOException {
        return range[gate];
      }

      /**
       * _more_
       *
       * @param ray _more_
       * @return _more_
       * @throws IOException _more_
       */
      public float getTime(int ray) throws IOException {

        return (float) time[ray + startIdx];

      }

      /**
       * _more_
       *
       * @return _more_
       */
      public float getBeamWidth() {
        return 0.95f;  // degrees, info from Chris Burkhart
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public float getNyquistFrequency() {
        return 0;  // LOOK this may be radial specific
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public float getRangeToFirstGate() {
        try {
          return getRadialDistance(0);
        } catch (IOException e) {
          e.printStackTrace();
          return 0.0f;
        }
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public float getGateSize() {
        try {
          return getRadialDistance(1) - getRadialDistance(0);
        } catch (IOException e) {
          e.printStackTrace();
          return 0.0f;
        }
      }

      /**
       * _more_
       *
       * @return _more_
       */
      public boolean isGateSizeConstant() {
        return true;
      }

      /**
       * _more_
       */
      public void clearSweepMemory() {
      }
    }  // LevelII2Sweep class

  }      // LevelII2Variable
}
