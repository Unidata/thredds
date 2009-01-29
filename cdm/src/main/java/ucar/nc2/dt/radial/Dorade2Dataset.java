// $Id:Dorade2Dataset.java 51 2006-07-12 17:13:13Z caron $
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
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Variable;
import ucar.ma2.*;

import java.io.IOException;
import java.util.List;
import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;

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

    public TypedDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
        return new Dorade2Dataset(ncd);
    }

    public FeatureType getScientificDataType() { return FeatureType.RADIAL; }


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

    public ucar.unidata.geoloc.EarthLocation getCommonOrigin() {
        if (isStationary())
          return new ucar.unidata.geoloc.EarthLocationImpl(latv[0], lonv[0], elev[0]);
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
          origin = new ucar.unidata.geoloc.EarthLocationImpl(latv[0], lonv[0], elev[0]);
        origin = null;
    }

    protected void addRadialVariable(NetcdfDataset nds, Variable var) {
        RadialVariable rsvar = null;
        String vName = var.getShortName() ;
        int rnk = var.getRank();

        if(rnk == 2)  {
            VariableSimpleIF v = new MyRadialVariableAdapter(vName, var.getAttributes());
            rsvar = makeRadialVariable(nds, v, var);
        }

        if(rsvar != null)
            dataVariables.add(rsvar);
    }

    protected RadialVariable makeRadialVariable(NetcdfDataset nds, VariableSimpleIF v, Variable v0) {
        return new Dorade2Variable(nds, v, v0);
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


    protected void setTimeUnits() throws Exception {
        List axes = ncd.getCoordinateAxes();
        for (int i = 0; i < axes.size(); i++) {
          CoordinateAxis axis = (CoordinateAxis) axes.get(i);
          if (axis.getAxisType() == AxisType.Time) {
            String units = axis.getUnitsString();
            dateUnits =  new DateUnit(units);
            return;
          }
        }
        parseInfo.append("*** Time Units not Found\n");
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

    private class Dorade2Variable extends MyRadialVariableAdapter implements RadialDatasetSweep.RadialVariable {//extends VariableSimpleAdapter {
        ArrayList sweeps;
        int nsweeps;
        String name;

        float ele, azi, alt, lon, lat;
        //float rt;
        //RadialDatasetSweep.Sweep sweep;


        public int getNumSweeps() {
          return 1;
        }

        public Sweep getSweep(int nsw) {
          return (Sweep) sweeps.get(nsw);
        }

        private Dorade2Variable(NetcdfDataset nds, VariableSimpleIF v, Variable v0) {
          super(v.getName(), v0.getAttributes());
          sweeps = new ArrayList();
          nsweeps = 0;
          name = v.getName();

          int[] shape = v0.getShape();
          int count = v0.getRank() - 1;

          int ngates = shape[count];
          count--;
          int nrays = shape[count];

          sweeps.add( new Dorade2Sweep(v0, 0, nrays, ngates)) ;
        }

        public String toString() {
            return name;
        }

        public float[] readAllData() throws java.io.IOException {
          Array allData;
          Sweep spn = (Sweep)sweeps.get(0);
          Variable v = spn.getsweepVar();

          try {
            allData = v.read();
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
          int nrays, ngates;
          double meanElevation = Double.NaN;
          Variable sweepVar;
          int sweepno;
          //int[] shape, origi;

          Dorade2Sweep(Variable v, int sweepno, int rays, int gates){
            this.sweepVar = v;
            this.nrays = rays;
            this.ngates = gates;
            this.sweepno = sweepno;
          }

          public Variable getsweepVar(){
              return sweepVar;
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
            int [] shape = sweepVar.getShape();
            int [] origi = new int[ sweepVar.getRank()];
            shape[0] = 1;
            origi[0] = ray;
            try {
              rayData = sweepVar.read(origi, shape);
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

          public float[] getElevation() {// degrees from horisontal earth tangent, towards zenith
            return elev;
          }

          public float getAzimuth(int ray) { // degrees clockwise from true North
            return aziv[ray];
          }

          public float[] getAzimuth() { // degrees clockwise from true North
            return aziv;
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
          public ucar.unidata.geoloc.EarthLocation getOrigin(int ray) {
            return new ucar.unidata.geoloc.EarthLocationImpl(latv[ray], lonv[ray], altv[ray]);
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
    Sweep sw = rv.getSweep(0);
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
    RadialDatasetSweep rds = (RadialDatasetSweep) TypedDatasetFactory.open( FeatureType.RADIAL, fileIn, null, new StringBuilder());
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
