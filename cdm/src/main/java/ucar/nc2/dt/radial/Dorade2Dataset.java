// $Id:Dorade2Dataset.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt.radial;

import ucar.nc2.dataset.*;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.VariableSimpleIF;
import ucar.ma2.*;

import java.io.IOException;
import java.util.List;
import java.util.Date;
import java.util.Iterator;

/**
 * Make a Dorade 2 NetcdfDataset into a RadialDataset.
 *
 * @author yuan
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class Dorade2Dataset extends RadialDatasetSweepAdapter implements TypedDatasetFactoryIF {
  protected ucar.nc2.units.DateUnit dateUnits;
  private NetcdfDataset ncd;
  float [] elev, aziv, disv, lonv, altv, latv;
  double[] timv;
  float ranv, cellv, angv, nyqv, rangv, contv, rgainv, bwidthv;

  /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) {
    String convention = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if ((null != convention) && convention.equals(_Coordinate.Convention)) {
      String format = ds.findAttValueIgnoreCase(null, "Format", null);
      if (format.equals("Unidata/netCDF/Dorade"))
        return true;
    }

    return false;
  }

  public TypedDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    return new Dorade2Dataset(ncd);
  }

  public Dorade2Dataset() {}

  /**
   * Constructor.
   *
   * @param ds must be from dorade IOSP
   */
  public Dorade2Dataset(NetcdfDataset ds) {
    super(ds);
    this.ncd = ds;

    desc = "dorade radar dataset";
    //EarthLocation y = getEarthLocation() ;
    try {
      elev = (float []) ncd.findVariable("elevation").read().get1DJavaArray(Float.TYPE);
      aziv = (float []) ncd.findVariable("azimuth").read().get1DJavaArray(Float.TYPE);
      altv = (float []) ncd.findVariable("altitudes_1").read().get1DJavaArray(Float.TYPE);
      lonv = (float []) ncd.findVariable("longitudes_1").read().get1DJavaArray(Float.TYPE);
      latv = (float []) ncd.findVariable("latitudes_1").read().get1DJavaArray(Float.TYPE);
      disv = (float []) ncd.findVariable("distance_1").read().get1DJavaArray(Float.TYPE);
      timv = (double []) ncd.findVariable("rays_time").read().get1DJavaArray(Double.TYPE);

      angv = ncd.findVariable("Fixed_Angle").readScalarFloat();
      nyqv = ncd.findVariable("Nyquist_Velocity").readScalarFloat();
      rangv = ncd.findVariable("Unambiguous_Range").readScalarFloat();
      contv = ncd.findVariable("Radar_Constant").readScalarFloat();
      rgainv = ncd.findVariable("rcvr_gain").readScalarFloat();
      //bwidthv = ncd.findVariable("bm_width").readScalarFloat();
    } catch (IOException e) {
      e.printStackTrace();
    }
    setStartDate();
    setEndDate();
  }

  public String getRadarID() {
    return ncd.findGlobalAttribute("radar_name").getStringValue();
  }

  public String getRadarName() {
    return "Dorade Radar";
  }

  public String getDataFormat() {
    return "DORADE";
  }

  public ucar.nc2.dt.EarthLocation getCommonOrigin() {
    if (isStationary())
      return new EarthLocationImpl(latv[0], lonv[0], elev[0]);
    return null;
  }

  public boolean isStationary() {
    String t = ncd.findGlobalAttribute("IsStationary").getStringValue();

    return t.equals("1");       //  if t == "1" return true
  }

  //public boolean isRadial() {
  //     return true;
  //}

  public boolean isVolume() {
    return false;
  }

  protected void setEarthLocation() {
    if (isStationary())
      origin = new EarthLocationImpl(latv[0], lonv[0], elev[0]);
    origin = null;
  }

  protected RadialVariable makeRadialVariable(VariableEnhanced varDS, RadialCoordSys gcs) {
    return new Dorade2Variable(varDS, gcs);
  }

  public java.util.List getDataVariables() {
    return dataVariables;
  }

  protected void setStartDate() {
    Date da = new Date((long) timv[0]);
    String start_datetime = da.toString();
    if (start_datetime != null)
      startDate = da;
    else
      parseInfo.append("*** start_datetime not Found\n");
  }

  protected void setEndDate() {
    Date da = new Date((long) timv[timv.length - 1]);
    String end_datetime = da.toString();
    if (end_datetime != null)
      endDate = da;
    else
      parseInfo.append("*** end_datetime not Found\n");
  }


  protected void setTimeUnits() {
    List axes = ncd.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      if (axis.getAxisType() == AxisType.Time) {
        String units = axis.getUnitsString();
        dateUnits = (DateUnit) SimpleUnit.factory(units);
        return;
      }
    }
    parseInfo.append("*** Time Units not Found\n");
  }

  public VariableSimpleIF getDataVariable(String name) {
    VariableEnhanced c = (VariableEnhanced) ncd.findVariable(name);
    RadialCoordSys rcs = null;
    List csys = c.getCoordinateSystems();
    for (int j = 0; j < csys.size(); j++) {
      CoordinateSystem cs = (CoordinateSystem) csys.get(j);
      rcs = RadialCoordSys.makeRadialCoordSys(parseInfo, cs, c);
      if (rcs != null) break;
    }

    if (rcs != null) {
      return makeRadialVariable(c, rcs);
    } else {

      return null;
    }
  }

  public List getAttributes() {
    return ncd.getRootGroup().getAttributes();
  }

  public ucar.nc2.units.DateUnit getTimeUnits() {
    return dateUnits;
  }

  public void getTimeUnits(ucar.nc2.units.DateUnit dateUnits) {
    this.dateUnits = dateUnits;
  }

  public void clearDatasetMemory() {
          List  rvars = getDataVariables();
          Iterator iter = rvars.iterator();
          while (iter.hasNext()) {
              RadialVariable radVar = (RadialVariable)iter.next();
              radVar.clearVariableMemory();
          }
  }

  private class Dorade2Variable extends VariableSimpleAdapter implements RadialDatasetSweep.RadialVariable {//extends VariableSimpleAdapter {
    protected RadialCoordSys radialCoordsys;
    protected VariableEnhanced ve;
    int nrays, ngates;
    float ele, azi, alt, lon, lat;
    //float rt;
    RadialDatasetSweep.Sweep sweep;


    public int getNumSweeps() {
      return 1;
    }

    public Sweep getSweep(int nsw) {
      if (sweep == null)
        sweep = new Dorade2Sweep();

      return sweep;
    }

    private Dorade2Variable(VariableEnhanced v, RadialCoordSys rcys) {
      super(v);
      this.ve = v;
      this.radialCoordsys = rcys;
      int[] shape = v.getShape();
      int count = v.getRank() - 1;

      ngates = shape[count];
      count--;
      nrays = shape[count];

      // sweep = new RadialDatasetSweep.Sweep[] ;
    }

    public int getNumRadials() {
      return nrays;
    }

    public float[] readAllData() throws java.io.IOException {
      Array allData;
      try {
        allData = ve.read();
      } catch (IOException e) {
        throw new IOException(e.getMessage());
      }
      return (float []) allData.get1DJavaArray(Float.TYPE);
    }

    public void clearVariableMemory() {
         // doing nothing
    }
    //////////////////////////////////////////////////////////////////////
    private class Dorade2Sweep implements RadialDatasetSweep.Sweep {

      double meanElevation = Double.NaN;
      //int[] shape, origi;

      Dorade2Sweep() {
        // shape = ve.getShape();
        // origi = new int[ ve.getRank()];
        //      shape[0] = 1;
        //    origi[0] = 1;
      }

      public RadialDatasetSweep.Type getType() {
        return null;
      }

      public float getLon(int ray) {
        return lonv[ray];
      }

      public int getGateNumber() {
        return ngates;
      }

      public int getRadialNumber() {
        return nrays;
      }

      public int getSweepIndex() {
        return 0;
      }

      public float[] readData() throws java.io.IOException {
        return readAllData();
      }

      public float[] readData(int ray) throws java.io.IOException {
        Array rayData;
        int [] shape = ve.getShape();
        int [] origi = new int[ ve.getRank()];
        shape[0] = 1;
        origi[0] = ray;
        try {
          rayData = ve.read(origi, shape);
        } catch (ucar.ma2.InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
        return (float []) rayData.get1DJavaArray(Float.TYPE);
      }

      public float getBeamWidth() { // degrees
        try {
          bwidthv = ncd.findVariable("bm_width").readScalarFloat();
        } catch (java.io.IOException e) {
          e.printStackTrace();
        }
        return bwidthv;
      }

      public float getNyquistFrequency() {
        return 0.0f;
      }

      public float getRangeToFirstGate() { // meters
        try {
          ranv = ncd.findVariable("Range_to_First_Cell").readScalarFloat();
        } catch (java.io.IOException e) {
          e.printStackTrace();
        }
        return ranv;
      }

      public float getGateSize() { // meters
        try {
          cellv = ncd.findVariable("Cell_Spacing").readScalarFloat();
        } catch (java.io.IOException e) {
          e.printStackTrace();
        }
        return cellv;
      }

      // coordinates of the radial data, relative to radial origin.
      public float getMeanElevation() {// degrees from horisontal earth tangent, towards zenith
        int[] shapeRadial = new int[1];
        shapeRadial[0] = nrays;

        Array data = Array.factory(Float.class, shapeRadial, elev);
        meanElevation = MAMath.sumDouble(data) / data.getSize();

        return (float) meanElevation;
      }

      public float getElevation(int ray) {// degrees from horisontal earth tangent, towards zenith
        return elev[ray];
      }

      public float getAzimuth(int ray) { // degrees clockwise from true North
        return aziv[ray];
      }

      public float getTime() {
        return (float) timv[0];
      }

      public float getTime(int ray) {
        return (float) timv[ray];
      }

      /**
       * Location of the origin of the radial
       */
      public ucar.nc2.dt.EarthLocation getOrigin(int ray) {
        return new EarthLocationImpl(latv[ray], lonv[ray], altv[ray]);
      }

      public float getMeanAzimuth() {
        if (getType() != null)
          return azi;
        return 0f;
      }

      public Date getStartingTime() {
        return startDate;
      }

      public Date getEndingTime() {
        return endDate;
      }

      public void clearSweepMemory() {
         // doing nothing for dorade adapter
      }
    } // Dorade2Sweep class

  } // Dorade2Variable

  private static void testRadialVariable(RadialDatasetSweep.RadialVariable rv) throws IOException {

    //ucar.nc2.dt.radial.RadialCoordSys rcsys = rv.getRadialCoordSys();
    //assert rcsys != null;

    int nsweep = rv.getNumSweeps();
    if (nsweep != 1) {

    }
    Sweep sw = rv.getSweep(1);
    int nrays = sw.getRadialNumber();
    float [] ddd = sw.readData();
    for (int i = 0; i < nrays; i++) {

      int ngates = sw.getGateNumber();

      float [] d = sw.readData(i);
      float azi = sw.getAzimuth(i);
      float ele = sw.getElevation(i);
      double t = sw.getTime(i);
      Date da = new Date((long) t);
      //da.setTime((long)t);
      String start_datetime = da.toString();
      float dis = sw.getRangeToFirstGate();
      float beamW = sw.getBeamWidth();
      float gsize = sw.getGateSize();

      float la = (float) sw.getOrigin(i).getLatitude();
      float lo = (float) sw.getOrigin(i).getLongitude();
      float al = (float) sw.getOrigin(i).getAltitude();
    }
  }


  public static void main(String args[]) throws Exception, IOException, InstantiationException, IllegalAccessException {
    String fileIn = "/home/yuanho/dorade/swp.1020511015815.SP0L.573.1.2_SUR_v1";
    RadialDatasetSweep rds = (RadialDatasetSweep) TypedDatasetFactory.open( thredds.catalog.DataType.RADIAL, fileIn, null, new StringBuffer());
    String st = rds.getStartDate().toString();
    String et = rds.getEndDate().toString();
    if (rds.isStationary()) {
      System.out.println("*** radar is stationary\n");
    }
    List rvars = rds.getDataVariables();
    RadialDatasetSweep.RadialVariable vDM = (RadialDatasetSweep.RadialVariable) rds.getDataVariable("DM");
    testRadialVariable(vDM);
    for (int i = 0; i < rvars.size(); i++) {
      RadialDatasetSweep.RadialVariable rv = (RadialDatasetSweep.RadialVariable) rvars.get(i);
      testRadialVariable(rv);

      //  RadialCoordSys.makeRadialCoordSys( "desc", CoordinateSystem cs, VariableEnhanced v);
      // ucar.nc2.dt.radial.RadialCoordSys rcsys = rv.getRadialCoordSys();
    }

  }


} // Dorade2Dataset
