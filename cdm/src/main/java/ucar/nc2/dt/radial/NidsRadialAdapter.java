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

import ucar.nc2.dataset.*;
import ucar.nc2.constants.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.*;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateUnit;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Variable;
import ucar.ma2.*;

import java.io.IOException;
import java.util.*;

/**
 * Make a Nids NetcdfDataset into a RadialDataset.
 *
 * @author yuan
 */

public class NidsRadialAdapter extends AbstractRadialAdapter {
  private NetcdfDataset ds;

  /////////////////////////////////////////////////
  public Object isMine( FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException {
    String convention = ncd.findAttValueIgnoreCase(null, "Conventions", null);
    if ((null != convention) && convention.equals(_Coordinate.Convention)) {
      String format = ncd.findAttValueIgnoreCase(null, "Format", null);
      if (format != null && format.equals("Level3/NIDS"))
        return this;
    }
    return null;
  }

  public FeatureDataset open( FeatureType ftype, NetcdfDataset ncd, Object analysis, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    return new NidsRadialAdapter(ncd);
  }

  public FeatureType getScientificDataType() {
    return FeatureType.RADIAL;
  }

  // needed for FeatureDatasetFactory
  public NidsRadialAdapter() {
  }

  /**
   * Constructor.
   *
   * @param ds must be from nids IOSP
   */
  public NidsRadialAdapter(NetcdfDataset ds) {
    super(ds);
    this.ds = ds;
    desc = "Nids 2 radar dataset";

    try {
      if (ds.findGlobalAttribute("isRadial").getNumericValue().intValue() == 0) {
        parseInfo.append("*** Dataset is not a radial data\n");
        throw new IOException("Dataset is not a radial data\n");
      }

      setEarthLocation();
      setTimeUnits();
      setStartDate();
      setEndDate();
      setBoundingBox();
    } catch (Throwable e) {
      System.err.println("CDM radial dataset failed to open this dataset " + e);
    }
  }

  public ucar.unidata.geoloc.EarthLocation getCommonOrigin() {
    return origin;
  }

  public String getRadarID() {
    ucar.nc2.Attribute att = ds.findGlobalAttribute("ProductStation");
    if (att == null)
      att = ds.findGlobalAttribute("Product_station");
    return att.getStringValue();
  }

  public boolean isStationary() {
    return true;
  }

  public String getRadarName() {
    return ds.findGlobalAttribute("ProductStationName").getStringValue();
  }

  public String getDataFormat() {
    return "Level III";
  }

  public boolean isVolume() {
    return false;
  }

  protected void setEarthLocation() {
    double lat = 0.0;
    double lon = 0.0;
    double elev = 0.0;
    ucar.nc2.Attribute attLat = ds.findGlobalAttribute("RadarLatitude");
    ucar.nc2.Attribute attLon = ds.findGlobalAttribute("RadarLongitude");
    ucar.nc2.Attribute attElev = ds.findGlobalAttribute("RadarAltitude");
    try {
      if (attLat != null)
        lat = attLat.getNumericValue().doubleValue();
      else
        throw new IOException("Unable to init radar location!\n");


      if (attLon != null)
        lon = attLon.getNumericValue().doubleValue();
      else
        throw new IOException("Unable to init radar location!\n");


      if (attElev != null)
        elev = attElev.getNumericValue().intValue();
      else
        throw new IOException("Unable to init radar location!\n");


    } catch (Throwable e) {
      System.err.println("CDM radial dataset failed to open this dataset " + e);

    }
    origin = new ucar.unidata.geoloc.EarthLocationImpl(lat, lon, elev);
  }

  protected void setTimeUnits() throws Exception {
    CoordinateAxis axis = ds.findCoordinateAxis(AxisType.Time);
    if (axis == null) {
      parseInfo.append("*** Time Units not Found\n");

    } else {
      String units = axis.getUnitsString();
      dateUnits = new DateUnit(units);
      calDateUnits = CalendarDateUnit.of(null, units);
    }

  }

  protected void setStartDate() {

    String start_datetime = ds.findAttValueIgnoreCase(null, "time_coverage_start", null);
    if (start_datetime != null) {
      startDate = DateUnit.getStandardOrISO(start_datetime);
      return;
    } else {
      CoordinateAxis axis = ds.findCoordinateAxis(AxisType.Time);
      if (axis != null) {
        double val = axis.getMinValue();
        startDate = dateUnits.makeDate(val);
        return;
      }
    }
    parseInfo.append("*** start_datetime not Found\n");
  }

  protected void setEndDate() {

    String end_datetime = ds.findAttValueIgnoreCase(null, "time_coverage_end", null);
    if (end_datetime != null) {
      endDate = DateUnit.getStandardOrISO(end_datetime);
    } else {
      CoordinateAxis axis = ds.findCoordinateAxis(AxisType.Time);
      if (axis != null) {
        double val = axis.getMaxValue();
        endDate = dateUnits.makeDate(val);
        return;
      }
    }

    parseInfo.append("*** end_datetime not Found\n");
  }


  protected void addRadialVariable(NetcdfDataset nds, Variable var) {
    RadialVariable rsvar = null;
    String vName = var.getShortName();
    int rnk = var.getRank();

    if (!var.getShortName().endsWith("RAW") && rnk == 2) {
      VariableSimpleIF v = new MyRadialVariableAdapter(vName, var.getAttributes());
      rsvar = new Nids2Variable(nds, v, var);
    }

    if (rsvar != null)
      dataVariables.add(rsvar);
  }

  public void clearDatasetMemory() {
    List rvars = getDataVariables();
    Iterator iter = rvars.iterator();
    while (iter.hasNext()) {
      RadialVariable radVar = (RadialVariable) iter.next();
      radVar.clearVariableMemory();
    }
  }

  protected RadialVariable makeRadialVariable(NetcdfDataset nds, VariableSimpleIF v, Variable v0) {
    // this function is null in level 2
    return null;
  }

  public String getInfo() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("Nids2Dataset\n");
    sbuff.append(super.getDetailInfo());
    sbuff.append("\n\n");
    sbuff.append(parseInfo.toString());
    return sbuff.toString();
  }


  private class Nids2Variable extends MyRadialVariableAdapter implements RadialDatasetSweep.RadialVariable {
    ArrayList sweeps;
    String name;


    private Nids2Variable(NetcdfDataset nds, VariableSimpleIF v, Variable v0) {
      super(v.getShortName(), v0.getAttributes());
      sweeps = new ArrayList();
      name = v.getShortName();

      int[] shape = v0.getShape();
      int count = v0.getRank() - 1;

      int ngates = shape[count];
      count--;
      int nrays = shape[count];
      count--;

      sweeps.add(new Nids2Sweep(nds, v0, 0, nrays, ngates));
      //sweep = new Sweep[nsweeps];
    }

    public String toString() {
      return name;
    }

    public float[] readAllData() throws IOException {
      Array allData;
      Sweep spn = (Sweep) sweeps.get(0);
      Variable v = spn.getsweepVar();
      try {
        allData = v.read();
      } catch (IOException e) {
        throw new IOException(e.getMessage());
      }
      return (float[]) allData.get1DJavaArray(float.class);
    }

    public int getNumSweeps() {
      return 1;
    }

    public Sweep getSweep(int sn) {
      if (sn != 0) return null;
      return (Sweep) sweeps.get(sn);
    }

    public void clearVariableMemory() {
      // doing nothing
    }

    //////////////////////////////////////////////////////////////////////
    private class Nids2Sweep implements RadialDatasetSweep.Sweep {
      double meanElevation = Double.NaN;
      double meanAzimuth = Double.NaN;
      double gateSize = Double.NaN;
      int nrays, ngates;
      Variable sweepVar;
      NetcdfDataset ds;

      Nids2Sweep(NetcdfDataset nds, Variable v, int sweepno, int rays, int gates) {
        this.sweepVar = v;
        this.nrays = rays;
        this.ngates = gates;
        this.ds = nds;
        //setMeanElevation(nds);
        //setMeanAzimuth(nds);
      }

      public Variable getsweepVar() {
        return sweepVar;
      }

      private void setMeanElevation() {
        if (Double.isNaN(meanElevation)) {
          try {
            Variable sp = ds.findVariable("elevation");
            Array spData = sp.read();
            sp.setCachedData(spData, false);
            meanElevation = MAMath.sumDouble(spData) / spData.getSize();
          } catch (IOException e) {
            e.printStackTrace();
            meanElevation = 0.0;
          }
        }
      }

      public float getMeanElevation() {
        if (Double.isNaN(meanElevation))
          setMeanElevation();
        return (float) meanElevation;
      }

      private void setMeanAzimuth() {
        if (getType() != null) {
          Array spData = null;
          try {
            Variable sp = ds.findVariable("azimuth");
            spData = sp.read();
            sp.setCachedData(spData, false);

          } catch (IOException e) {
            e.printStackTrace();
            meanAzimuth = 0.0;
          }
          meanAzimuth = MAMath.sumDouble(spData) / spData.getSize();

        } else meanAzimuth = 0.0;

      }

      public float getMeanAzimuth() {
        if (Double.isNaN(meanAzimuth))
          setMeanAzimuth();
        return (float) meanAzimuth;
      }

      public int getNumRadials() {
        return nrays;
      }

      public int getNumGates() {
        return ngates;
      }

      /* a 2D array nradials * ngates   */
      public float[] readData() throws IOException {
        Array allData = null;
        int[] shape = sweepVar.getShape();
        int[] origind = new int[sweepVar.getRank()];
        try {
          allData = sweepVar.read(origind, shape);
        } catch (InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
        return (float[]) allData.get1DJavaArray(float.class);
      }

      /* read 1d data ngates */
      public float[] readData(int ray) throws java.io.IOException {
        Array rayData;
        int[] shape = sweepVar.getShape();
        int[] origind = new int[sweepVar.getRank()];
        shape[0] = 1;
        origind[0] = ray;
        try {
          rayData = sweepVar.read(origind, shape);
        } catch (ucar.ma2.InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
        return (float[]) rayData.get1DJavaArray(float.class);
      }


      public RadialDatasetSweep.Type getType() {
        return null;
      }

      public boolean isConic() {
        return true;
      }

      public float getElevation(int ray) throws IOException {
        // setMeanElevation();
        return (float) meanElevation;
      }

      public float[] getElevation() throws IOException {
        float[] spArray = null;
        try {
          Variable sp = ds.findVariable("elevation");
          Array spData = sp.read();
          sp.setCachedData(spData, false);
          spArray = (float[]) spData.get1DJavaArray(float.class);

        } catch (IOException e) {
          e.printStackTrace();
        }

        return spArray;
      }

      public float getAzimuth(int ray) throws IOException {
        Variable sp = ds.findVariable("azimuth");
        Array spData = sp.read();
        sp.setCachedData(spData, false);
        Index index = spData.getIndex();
        return spData.getFloat(index.set(ray));
      }

      public float[] getAzimuth() throws IOException {
        Variable sp = ds.findVariable("azimuth");
        Array spData = sp.read();
        sp.setCachedData(spData, false);
        return (float[]) spData.get1DJavaArray(float.class);
      }

      public float getRadialDistance(int gate) throws IOException {
        Variable sp = ds.findVariable("gate");
        Array spData = sp.read();
        sp.setCachedData(spData, false);
        Index index = spData.getIndex();
        return spData.getFloat(index.set(gate));
      }

      public float getTime(int ray) throws IOException {
        Variable sp = ds.findVariable("rays_time");
        Array timeData = sp.read();
        sp.setCachedData(timeData, false);
        Index index = timeData.getIndex();
        return timeData.getFloat(index.set(ray));
      }

      public float getBeamWidth() {
        return 0.95f; // degrees, info from Chris Burkhart
      }

      public float getNyquistFrequency() {
        return 0; // LOOK this may be radial specific
      }

      public float getRangeToFirstGate() {
        return 0.0f;
      }

      public float getGateSize() {
        try {
            if (Double.isNaN(gateSize))
               gateSize = getRadialDistance(1) - getRadialDistance(0);
            return (float) gateSize;

        } catch (IOException e) {
          e.printStackTrace();
          return 0.0f;
        }
      }

      public Date getStartingTime() {
        return startDate;
      }

      public Date getEndingTime() {
        return endDate;
      }

      public boolean isGateSizeConstant() {
        return true;
      }

      public int getGateNumber() {
        return ngates;
      }

      public int getRadialNumber() {
        return nrays;
      }

      public ucar.unidata.geoloc.EarthLocation getOrigin(int ray) {
        return origin;
      }

      public int getSweepIndex() {
        return 0;
      }

      public void clearSweepMemory() {
        // doing nothing for nids adapter
      }
    } // Nids2Sweep class

  } // Nids2Variable

} // Nids2Dataset
