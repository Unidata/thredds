/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.radial;

import ucar.nc2.dt.*;
import ucar.nc2.dataset.*;
import ucar.nc2.constants.*;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateUnit;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.ma2.*;
import ucar.ma2.DataType;

import java.io.IOException;
import java.util.*;

public class NsslRadialAdapter extends AbstractRadialAdapter {
  private NetcdfDataset ds;
  private boolean isVolume;
  private double latv, lonv, elev;

  /////////////////////////////////////////////////
  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException {
    String format = ncd.findAttValueIgnoreCase(null, "format", null);
    if (format != null) {
      if (format.startsWith("nssl/netcdf"))
        return this;
    }

    Dimension az = ncd.findDimension("Azimuth");
    Dimension gt = ncd.findDimension("Gate");
    if ((null != az) && (null != gt)) {
      return this;
    }
    return null;
  }

  public FeatureDataset open( FeatureType ftype, NetcdfDataset ncd, Object analysis, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    return new NsslRadialAdapter(ncd);
  }

  public FeatureType getScientificDataType() {
    return FeatureType.RADIAL;
  }

  // needed for FeatureDatasetFactory
  public NsslRadialAdapter() {
  }

  /**
   * Constructor.
   *
   * @param ds must be from netcdf IOSP
   */
  public NsslRadialAdapter(NetcdfDataset ds) {
    super(ds);
    this.ds = ds;
    desc = "Netcdf/NCML 2 radar dataset";

    setEarthLocation();
    try {
      setTimeUnits();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    setStartDate();
    setEndDate();
    setBoundingBox();


    try {
      addCoordSystem(ds);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private void addCoordSystem(NetcdfDataset ds) throws IOException {


    //  int time = ds.findGlobalAttributeIgnoreCase("Time").getNumericValue().intValue();
    double ele = 0;
    Attribute attr = ds.findGlobalAttributeIgnoreCase("Elevation");
    if (attr != null)
      ele = attr.getNumericValue().doubleValue();

    // ncml agg add this sweep variable as agg dimension
    Variable sp = ds.findVariable("sweep");

    if (sp == null) {
      // add Elevation
      ds.addDimension(null, new Dimension("Elevation", 1, true));
      String lName = "elevation angle in degres: 0 = parallel to pedestal base, 90 = perpendicular";
      CoordinateAxis v = new CoordinateAxis1D(ds, null, "Elevation", DataType.DOUBLE, "Elevation", "degrees", lName);
      ds.setValues(v, 1, ele, 0);
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString()));
      ds.addVariable(null, v);

    } else {
      Array spdata = sp.read();
      float[] spd = (float[]) spdata.get1DJavaArray(float.class);
      int spsize = spd.length;

      // add Elevation
      ds.addDimension(null, new Dimension("Elevation", spsize, true));
      String lName = "elevation angle in degres: 0 = parallel to pedestal base, 90 = perpendicular";
      CoordinateAxis v = new CoordinateAxis1D(ds, null, "Elevation", DataType.DOUBLE, "Elevation", "degrees", lName);
      //ds.setValues(v, (ArrayList)spdata);
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString()));
      ds.addVariable(null, v);


    }
    ds.addAttribute(null, new Attribute("IsRadial", new Integer(1)));
    attr = ds.findGlobalAttributeIgnoreCase("vcp-value");
    String vcp;
    if (attr == null)
      vcp = "11";
    else
      vcp = attr.getStringValue();

    ds.addAttribute(null, new Attribute("VolumeCoveragePatternName", vcp));
    ds.finish();

  }

  public ucar.unidata.geoloc.EarthLocation getCommonOrigin() {
    return origin;
  }

  public String getRadarID() {
    Attribute ga = ds.findGlobalAttribute("radarName-value");
    if (ga != null)
      return ga.getStringValue();
    else
      return "XXXX";
  }

  public boolean isStationary() {
    return true;
  }

  public String getRadarName() {
    return ds.findGlobalAttribute("ProductStationName").getStringValue();
  }

  public String getDataFormat() {
    return "NetCDF Level II";
  }


  public void setIsVolume(NetcdfDataset nds) {
    String format = nds.findAttValueIgnoreCase(null, "volume", null);
    if (format == null) {
      isVolume = false;
      return;
    }
    if (format.equals("true"))
      isVolume = true;
    else
      isVolume = false;
  }

  public boolean isVolume() {
    return isVolume;
  }


  protected void setEarthLocation() {
    Attribute ga = ds.findGlobalAttribute("Latitude");
    if (ga != null)
      latv = ga.getNumericValue().doubleValue();
    else
      latv = 0.0;

    ga = ds.findGlobalAttribute("Longitude");
    if (ga != null)
      lonv = ga.getNumericValue().doubleValue();
    else
      lonv = 0.0;

    ga = ds.findGlobalAttribute("Height");
    if (ga != null)
      elev = ga.getNumericValue().doubleValue();
    else
      elev = 0.0;

    origin = new ucar.unidata.geoloc.EarthLocationImpl(latv, lonv, elev);
  }


  protected void setTimeUnits() throws Exception {
    List axes = ds.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      if (axis.getAxisType() == AxisType.Time) {
        String units = axis.getUnitsString();
        dateUnits = new DateUnit(units);
        calDateUnits = CalendarDateUnit.of(null, units);
        return;
      }
    }
    parseInfo.append("*** Time Units not Found\n");
  }

  protected void setStartDate() {
    String start_datetime = ds.findAttValueIgnoreCase(null, "time_coverage_start", null);
    if (start_datetime != null)
      startDate = DateUnit.getStandardOrISO(start_datetime);
    else
      parseInfo.append("*** start_datetime not Found\n");
  }

  protected void setEndDate() {
    String end_datetime = ds.findAttValueIgnoreCase(null, "time_coverage_end", null);
    if (end_datetime != null)
      endDate = DateUnit.getStandardOrISO(end_datetime);
    else
      parseInfo.append("*** end_datetime not Found\n");
  }

  protected void addRadialVariable(NetcdfDataset nds, Variable var) {
    RadialVariable rsvar = null;
    String vName = var.getShortName();
    int rnk = var.getRank();

    setIsVolume(nds);

    if (isVolume && rnk == 3) {
      VariableSimpleIF v = new MyRadialVariableAdapter(vName, var.getAttributes());
      rsvar = makeRadialVariable(nds, v, var);
    } else if (!isVolume && rnk == 2) {
      VariableSimpleIF v = new MyRadialVariableAdapter(vName, var.getAttributes());
      rsvar = makeRadialVariable(nds, v, var);
    }

    if (rsvar != null)
      dataVariables.add(rsvar);
  }

  protected RadialVariable makeRadialVariable(NetcdfDataset nds, VariableSimpleIF v, Variable v0) {
    return new Netcdf2Variable(nds, v, v0);
  }


  public void clearDatasetMemory() {
    List rvars = getDataVariables();
    Iterator iter = rvars.iterator();
    while (iter.hasNext()) {
      RadialVariable radVar = (RadialVariable) iter.next();
      radVar.clearVariableMemory();
    }
  }

  public String getInfo() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("Netcdfs2Dataset\n");
    sbuff.append(super.getDetailInfo());
    sbuff.append("\n\n");
    sbuff.append(parseInfo.toString());
    return sbuff.toString();
  }

  private class Netcdf2Variable extends MyRadialVariableAdapter implements RadialDatasetSweep.RadialVariable {
    ArrayList sweeps;
    int nsweeps;
    String name;

    private Netcdf2Variable(NetcdfDataset nds, VariableSimpleIF v, Variable v0) {
      super(v.getShortName(), v0.getAttributes());

      sweeps = new ArrayList();
      nsweeps = 0;
      name = v.getShortName();
      int[] shape = v0.getShape();
      int count = v0.getRank() - 1;

      int ngates = shape[count];
      count--;
      int nrays = shape[count];
      count--;

      if (shape.length == 3)
        nsweeps = shape[count];
      else
        nsweeps = 1;

      for (int i = 0; i < nsweeps; i++)
        sweeps.add(new Netcdf2Sweep(v0, i, nrays, ngates));

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
      return nsweeps;
    }


    public Sweep getSweep(int sweepNo) {
      return (Sweep) sweeps.get(sweepNo);
    }


    public void clearVariableMemory() {
      // doing nothing
    }

    //////////////////////////////////////////////////////////////////////
    private class Netcdf2Sweep implements RadialDatasetSweep.Sweep {
      double meanElevation = Double.NaN;
      double meanAzimuth = Double.NaN;
      int sweepno, nrays, ngates;
      Variable sweepVar;


      Netcdf2Sweep(Variable v, int sweepno, int rays, int gates) {
        this.sweepno = sweepno;
        this.nrays = rays;
        this.ngates = gates;
        this.sweepVar = v;

        //setMeanElevation();
        //setMeanAzimuth();
      }

      public Variable getsweepVar() {
        return sweepVar;
      }

      public float[] readData() throws java.io.IOException {

        Array allData;
        int[] shape = sweepVar.getShape();
        int[] origind = new int[sweepVar.getRank()];

        if (isVolume) {
          origind[0] = sweepno;
          origind[1] = 0;
          shape[0] = 1;
        }

        try {
          allData = sweepVar.read(origind, shape);
        } catch (InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
        int nradials = shape[0];
        int ngates = shape[1];
        IndexIterator dataIter = allData.getIndexIterator();
        for (int j = 0; j < nradials; j++) {
          for (int i = 0; i < ngates; i++) {
            float tp = dataIter.getFloatNext();
            if (tp == -32768.0f || tp == -99900.0f) {
              dataIter.setFloatCurrent(Float.NaN);
            }
          }
        }

        return (float[]) allData.get1DJavaArray(float.class);
      }

      //  private Object MUTEX =new Object();
      /* read 1d data ngates */
      public float[] readData(int ray) throws java.io.IOException {

        Array rayData;
        int[] shape = sweepVar.getShape();
        int[] origind = new int[sweepVar.getRank()];

        if (isVolume) {
          origind[0] = sweepno;
          origind[1] = ray;
          shape[0] = 1;
          shape[1] = 1;
        } else {
          shape[0] = 1;
          origind[0] = ray;
        }

        try {
          rayData = sweepVar.read(origind, shape);
        } catch (ucar.ma2.InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
        int ngates = shape[1];
        IndexIterator dataIter = rayData.getIndexIterator();
        for (int i = 0; i < ngates; i++) {
          float tp = dataIter.getFloatNext();
          if (tp == -32768.0f || tp == -99900.0f) {
            dataIter.setFloatCurrent(Float.NaN);
          }
        }
        return (float[]) rayData.get1DJavaArray(float.class);
      }

      private void setMeanElevation() {

        if (isVolume) {
          try {
            Variable sp = ds.findVariable("sweep");
            Array spData = sp.read();
            float[] spArray = (float[]) spData.get1DJavaArray(float.class);
            meanElevation = spArray[sweepno];
          } catch (IOException e) {
            e.printStackTrace();
            meanElevation = 0.0;
          }
        } else {
          Attribute data = ds.findGlobalAttribute("Elevation");
          meanElevation = data.getNumericValue().doubleValue();
        }
      }

      public float getMeanElevation() {
        if (Double.isNaN(meanElevation))
          setMeanElevation();
        return (float) meanElevation;
      }

      public double meanDouble(Array a) {
        double sum = 0;
        int size = 0;

        IndexIterator iterA = a.getIndexIterator();
        while (iterA.hasNext()) {
          double s = iterA.getDoubleNext();

          if (!Double.isNaN(s)) {
            sum += s;
            size++;
          }
        }
        if (size > 0)
          return sum / size;
        else
          return Double.POSITIVE_INFINITY;
      }

      public int getGateNumber() {
        return ngates;
      }

      public int getRadialNumber() {
        return nrays;
      }

      public RadialDatasetSweep.Type getType() {
        return null;
      }

      public ucar.unidata.geoloc.EarthLocation getOrigin(int ray) {
        return origin;
      }

      public Date getStartingTime() {
        return startDate;
      }

      public Date getEndingTime() {
        return endDate;
      }

      public int getSweepIndex() {
        return sweepno;
      }

      // public int getNumGates() {
      //   return ngates;
      // }

      private void setMeanAzimuth() {
        meanAzimuth = 0.0;
      }

      public float getMeanAzimuth() {
        if (Double.isNaN(meanAzimuth))
          setMeanAzimuth();
        return (float) meanAzimuth;
      }

      public boolean isConic() {
        return true;
      }

      public float getElevation(int ray) throws IOException {
        return (float) meanElevation;
      }

      public float[] getElevation() throws IOException {

        float[] dataValue = new float[nrays];
        for (int i = 0; i < nrays; i++) {
          dataValue[i] = (float) meanElevation;
        }
        return dataValue;
      }

      public float[] getAzimuth() throws IOException {
        Array aziData = ds.findVariable("Azimuth").read();
        return (float[]) aziData.get1DJavaArray(float.class);
      }


      public float getAzimuth(int ray) throws IOException {
        String aziName = "Azimuth";

        try {
          Array aziData = ds.findVariable(aziName).read();
          if (isVolume) {
            int[] aziOrigin = new int[2];
            aziOrigin[0] = sweepno;
            aziOrigin[1] = 0;
            int[] aziShape = {1, getRadialNumber()};
            aziData = aziData.section(aziOrigin, aziShape);
          }
          Index index = aziData.getIndex();
          return aziData.getFloat(index.set(ray));
        } catch (ucar.ma2.InvalidRangeException e) {
          throw new IOException(e);
        }
      }


      public float getRadialDistance(int gate) throws IOException {
        float gateStart = getRangeToFirstGate();
        Variable gateSize = ds.findVariable("GateWidth");
        float[] data = (float[]) gateSize.read().get1DJavaArray(float.class);
        float dist = gateStart + gate * data[0];

        return dist;
      }

      public float getTime(int ray) throws IOException {
        return startDate.getTime();
      }

      public float getBeamWidth() {
        return 0.95f; // degrees, info from Chris Burkhart
      }

      public float getNyquistFrequency() {
        return 0; // LOOK this may be radial specific
      }

      public float getRangeToFirstGate() {
        Attribute firstGate = ds.findGlobalAttributeIgnoreCase("RangeToFirstGate");
        double gateStart = firstGate.getNumericValue().doubleValue();
        return (float) gateStart;
      }

      public float getGateSize() {
        try {
          return getRadialDistance(1) - getRadialDistance(0);
        } catch (IOException e) {
          e.printStackTrace();
          return 0.0f;
        }
      }

      public boolean isGateSizeConstant() {
        return true;
      }

      public void clearSweepMemory() {

      }
    } // Netcdf2Sweep class

  } // Netcdf2Variable

  private static void testRadialVariable(RadialDatasetSweep.RadialVariable rv) throws IOException {
    int nsweep = rv.getNumSweeps();
    //System.out.println("*** radar Sweep number is: \n" + nsweep);
    Sweep sw;
    float[] az;
    for (int i = 0; i < nsweep; i++) {
      //ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable getSweep " + i);
      sw = rv.getSweep(i);
      sw.getMeanElevation();
      //ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable getSweep " + i);
      float me = sw.getMeanElevation();
      System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
      int nrays = sw.getRadialNumber();
      az = new float[nrays];
      for (int j = 0; j < nrays; j++) {
        float azi = sw.getAzimuth(j);
        az[j] = azi;
      }
      //System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
    }
    sw = rv.getSweep(0);
    //ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable readData");
    float[] ddd = sw.readData();
    sw.getAzimuth();
    sw.getElevation();
    //ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable readData");
    assert (null != ddd);
    int nrays = sw.getRadialNumber();
    az = new float[nrays];
    for (int i = 0; i < nrays; i++) {

      int ngates = sw.getGateNumber();
      assert (ngates > 0);
      float[] d = sw.readData(i);
      assert (null != d);
      // float [] e = sw.readDataNew(i);
      // assert(null != e);
      float azi = sw.getAzimuth(i);
      assert (azi > 0);
      az[i] = azi;
      float ele = sw.getElevation(i);
      assert (ele > 0);
      float la = (float) sw.getOrigin(i).getLatitude();
      assert (la > 0);
      float lo = (float) sw.getOrigin(i).getLongitude();
      assert (lo > 0);
      float al = (float) sw.getOrigin(i).getAltitude();
      assert (al > 0);
    }
    assert (0 != nrays);
  }

}