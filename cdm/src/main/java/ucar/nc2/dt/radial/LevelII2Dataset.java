// $Id:LevelII2Dataset.java 51 2006-07-12 17:13:13Z caron $
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
import ucar.nc2.units.DateFormatter;
import ucar.ma2.*;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import java.io.IOException;
import java.util.List;
import java.util.Date;
import java.util.Iterator;

import thredds.catalog.*;


/**
 * Make a LevelII2 NetcdfDataset into a RadialDataset.
 *
 * @author yuan
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class LevelII2Dataset extends RadialDatasetSweepAdapter implements TypedDatasetFactoryIF {
  private NetcdfDataset ds;
  double latv, lonv, elev;
  DateFormatter formatter = new DateFormatter();

  /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) {
    String convention = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if ((null != convention) && convention.equals(_Coordinate.Convention)) {
      String format = ds.findAttValueIgnoreCase(null, "Format", null);
      if (format.equals("ARCHIVE2") || format.equals("AR2V0001") || format.equals("CINRAD-SA"))
        return true;
    }
    return false;
  }

  public TypedDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    return new LevelII2Dataset(ncd);
  }

  public thredds.catalog.DataType getScientificDataType() { return thredds.catalog.DataType.RADIAL; }


  public LevelII2Dataset() {}

  /**
   * Constructor.
   *
   * @param ds must be from nexrad2 IOSP
   */
  public LevelII2Dataset(NetcdfDataset ds) {
    super(ds);
    this.ds = ds;
    desc = "Nexrad 2 radar dataset";

    setEarthLocation();
    setTimeUnits();
    setStartDate();
    setEndDate();
    setBoundingBox();
  }

  protected void setEarthLocation() {
    Attribute ga = ds.findGlobalAttribute("StationLatitude");
    if(ga != null )
        latv = ga.getNumericValue().doubleValue();
    else
        latv = 0.0;

    ga = ds.findGlobalAttribute("StationLongitude");
    if(ga != null)
        lonv = ga.getNumericValue().doubleValue();
    else
        lonv = 0.0;

    ga = ds.findGlobalAttribute("StationElevationInMeters");
    if(ga != null)
      elev = ga.getNumericValue().doubleValue();
    else
      elev = 0.0;

    origin = new EarthLocationImpl(latv, lonv, elev);
  }

  public ucar.nc2.dt.EarthLocation getCommonOrigin() {
    return origin;
  }

  public String getRadarID() {
    Attribute ga = ds.findGlobalAttribute("Station");
    if(ga != null)
        return ga.getStringValue();
    else
        return "XXXX";
  }

  public String getRadarName() {
    Attribute ga = ds.findGlobalAttribute("StationName");
    if(ga != null)
        return ga.getStringValue();
    else
        return "Unknown Station";
  }

  public String getDataFormat() {
    return RadialDatasetSweep.LevelII;
  }

  //public boolean isRadial() {
  //    return true;
  //}

  public boolean isVolume() {
    return true;
  }

  public boolean isStationary() {
    return true;
  }

  protected void setTimeUnits() {
    List axes = ds.getCoordinateAxes();
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

  protected void setStartDate() {
    String start_datetime = ds.findAttValueIgnoreCase(null, "time_coverage_start", null);
    if (start_datetime != null)
      startDate = formatter.getISODate(start_datetime);
    else
      parseInfo.append("*** start_datetime not Found\n");
  }

  protected void setEndDate() {
    String end_datetime = ds.findAttValueIgnoreCase(null, "time_coverage_end", null);
    if (end_datetime != null)
      endDate = formatter.getISODate(end_datetime);
    else
      parseInfo.append("*** end_datetime not Found\n");
  }

  public void clearDatasetMemory() {
      List  rvars = getDataVariables();
      Iterator iter = rvars.iterator();
      while (iter.hasNext()) {
          RadialVariable radVar = (RadialVariable)iter.next();
          radVar.clearVariableMemory();
      }
  }

  protected RadialVariable makeRadialVariable(VariableEnhanced varDS, RadialCoordSys gcs) {
    return new LevelII2Variable(varDS, gcs);
  }

  public String getInfo() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("LevelII2Dataset\n");
    sbuff.append(super.getDetailInfo());
    sbuff.append("\n\n");
    sbuff.append(parseInfo.toString());
    return sbuff.toString();
  }


  private class LevelII2Variable extends VariableSimpleAdapter implements RadialDatasetSweep.RadialVariable {
    protected RadialCoordSys radialCoordsys;
    protected VariableEnhanced ve;
    int nsweeps, nrays, ngates;
    RadialDatasetSweep.Sweep[] sweep;

    private LevelII2Variable(VariableEnhanced v, RadialCoordSys rcys) {
      super(v);
      this.ve = v;
      this.radialCoordsys = rcys;
      int[] shape = v.getShape();
      int count = v.getRank() - 1;

      ngates = shape[count];
      count--;
      nrays = shape[count];
      count--;
      nsweeps = shape[count];

      sweep = new RadialDatasetSweep.Sweep[nsweeps];
    }

    public int getNumSweeps() {
      return nsweeps;
    }

    public Sweep getSweep(int sweepNo) {
      if (sweep[sweepNo] == null)
        sweep[sweepNo] = new LevelII2Sweep(sweepNo);
      return sweep[sweepNo];
    }

    public int getNumRadials() {
      return nsweeps * nrays;
    }

    // a 3D array nsweep * nradials * ngates
    public float[] readAllData() throws IOException {
      Array allData;
      try {
        allData = ve.read();
      } catch (IOException e) {
        throw new IOException(e.getMessage());
      }
      return (float []) allData.get1DJavaArray(float.class);
    }

    public void clearVariableMemory() {
        for(int i = 0; i < nsweeps; i++) {
             if(sweep[i] != null)
                sweep[i].clearSweepMemory();
        }
    }


   //////////////////////////////////////////////////////////////////////
   // Checking all azi to make sure there is no missing data at sweep
   // level, since the coordinate is 1D at this level, this checking also
   // remove those missing radials within a sweep.

    private class LevelII2Sweep implements RadialDatasetSweep.Sweep {
      double meanElevation = Double.NaN;
      double meanAzimuth = Double.NaN;
      int sweepno;
      Array sweepData = null;
      Array aziData = null;
      Array eleData = null;

      LevelII2Sweep(int sweepno) {
        this.sweepno = sweepno;

        // ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable mine");
      }

      private void init( ) {

        Array sweepTmp = null;
        Array aziTmp = null;
        Array eleTmp = null;

        int[] shape = ve.getShape();
        int[] origin = new int[ ve.getRank()];
        shape[0] = 1;
        origin[0] = sweepno;

        //first reading  sweep data into array
       //   ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable first" );
        try {
            Array a = radialCoordsys.getAzimuthAxisDataCached();
            Array b = radialCoordsys.getElevationAxisDataCached();
            int [] aziOrigin = new int[2];
            aziOrigin[0] = sweepno;
            int [] aziShape = {1, shape[1]};
            aziTmp =  a.section(aziOrigin, aziShape);
            eleTmp =  b.section(aziOrigin, aziShape);
            sweepTmp = ve.read(origin, shape).reduce();
        } catch (ucar.ma2.InvalidRangeException e) {
            e.printStackTrace();
        } catch (java.io.IOException e ) {
            e.printStackTrace();
        }
     //    ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable first");

        Index ima = aziTmp.getIndex();
        Index imb = sweepTmp.getIndex();
        int nRadials = 0;
        // now checking the number of radial which azi is not NaN
        for (int azi = 0; azi < nrays; azi++) {
            float f = aziTmp.getFloat(ima.set(azi)) ;
            if(!Float.isNaN(f)) {
                nRadials++;
            }
        }
       // ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable mine" );
        // copying the data into aziData and sweepData
        if(nRadials == nrays ) {
            sweepData = sweepTmp;
            aziData = aziTmp;
            eleData = eleTmp;
        } else {
            aziData = new ArrayFloat.D1( nRadials);
            eleData = new ArrayFloat.D1( nRadials);
            sweepData = new ArrayFloat.D2( nRadials, ngates);
            Index imd = sweepData.getIndex();
            Index imc = aziData.getIndex();
            int radial = 0;
            for (int azi = 0; azi < nrays; azi++) {
                //System.out.println("*** radar radial number is: \n" + azi);
                float f = aziTmp.getFloat(ima.set(azi));
                float e = eleTmp.getFloat(ima.set(azi));
                if(!Float.isNaN(f)) {
                    aziData.setFloat(imc.set(radial), f);
                    eleData.setFloat(imc.set(radial), e);
                    for (int gate = 0; gate < ngates; gate++) {
                        float f1 = sweepTmp.getFloat(imb.set(azi,gate));
                        try {
                            sweepData.setFloat(imd.set(radial,gate), f1);
                        } catch (java.lang.NullPointerException m) {
                                m.printStackTrace();
                        }
                    }
                    radial++;
                }
            }
        }
        // ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable mine");
      }

      private void initAzi( ) {
        Array aziTmp = null;
        Array eleTmp = null;

        int[] shape = ve.getShape();
        int[] origin = new int[ ve.getRank()];
        shape[0] = 1;
        origin[0] = sweepno;

        //first reading  sweep data into array

        try {
            Array a = radialCoordsys.getAzimuthAxisDataCached();
            Array b = radialCoordsys.getElevationAxisDataCached();
            int [] aziOrigin = new int[2];
            aziOrigin[0] = sweepno;
            int [] aziShape = {1, shape[1]};
            aziTmp =  a.section(aziOrigin, aziShape);
            eleTmp =  b.section(aziOrigin, aziShape);

        } catch (ucar.ma2.InvalidRangeException e) {
            e.printStackTrace();
        } catch (java.io.IOException e ) {
            e.printStackTrace();
        }


        Index ima = aziTmp.getIndex();

        int nRadials = 0;
        // now checking the number of radial which azi is not NaN
        for (int azi = 0; azi < nrays; azi++) {
            float f = aziTmp.getFloat(ima.set(azi)) ;
            if(!Float.isNaN(f)) {
                nRadials++;
            }
        }

        // copying the data into aziData and sweepData
        if(nRadials == nrays ) {

            aziData = aziTmp;
            eleData = eleTmp;
        } else {
            aziData = new ArrayFloat.D1( nRadials);
            eleData = new ArrayFloat.D1( nRadials);
            Index imc = aziData.getIndex();
            int radial = 0;
            for (int azi = 0; azi < nrays; azi++) {
                //System.out.println("*** radar radial number is: \n" + azi);
                float f = aziTmp.getFloat(ima.set(azi));
                float e = eleTmp.getFloat(ima.set(azi));
                if(!Float.isNaN(f)) {
                    aziData.setFloat(imc.set(radial), f);
                    eleData.setFloat(imc.set(radial), e);

                    radial++;
                }
            }
        }
        // ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable mine");
      }
      //int[] shape ;
      //int[] origin ;
      /* read 2d sweep data nradials * ngates */
      public float[] readData() throws java.io.IOException {
            if(sweepData == null) init();
            return (float []) sweepData.get1DJavaArray(Float.TYPE);
      }

      //              private Object MUTEX =new Object();
      /* read 1d data ngates */
      public float[] readData(int ray) throws java.io.IOException {
        Array rayData;
        //synchronized(MUTEX) {
        //    if(shape ==null) {
        if(sweepData == null) init();
        int [] shape = sweepData.getShape();
        int [] origin = new int[2];
        //     }
        shape[0] = 1;
        origin[0] = ray;

        try {
          rayData = sweepData.section(origin, shape);
        } catch (ucar.ma2.InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
        // }
        return (float []) rayData.get1DJavaArray(Float.TYPE);
      }

      public float getMeanElevation() {
       // if (getType() != null) {
       // meanElevation = MAMath.sumDouble(eleData) / eleData.getSize();
        //} else meanElevation = 0.0;
       // return (float) meanElevation;
        if(eleData == null) initAzi();
        meanElevation = MAMath.sumDouble(eleData) / eleData.getSize();
        return (float) meanElevation ;
      }

      public double meanDouble(Array a) {
        double sum = 0;
        int size = 0;

        IndexIterator iterA = a.getIndexIterator();
        while (iterA.hasNext()) {
          double s = iterA.getDoubleNext();

          if (! Double.isNaN(s)) {
            sum += s;
            size ++;
          }
        }
        return sum / size;
      }

      public int getGateNumber() {
        return ngates;
      }

      public int getRadialNumber() {
        if(aziData == null) initAzi();
        return (int)aziData.getSize();
      }

      public RadialDatasetSweep.Type getType() {
        return null;
      }

      public ucar.nc2.dt.EarthLocation getOrigin(int ray) {
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

      public float getMeanAzimuth() {
        if (getType() != null) {
           try {
                Array data =  radialCoordsys.getAzimuthAxisDataCached();
                meanAzimuth = MAMath.sumDouble( data) / data.getSize();
            } catch (IOException e) {
                e.printStackTrace();
                meanAzimuth = 0.0;
            }
        } else meanAzimuth = 0.0;

        return (float) meanAzimuth;
      }

      public boolean isConic() {
        return true;
      }

      public float getElevation(int ray) throws IOException {
        if(eleData == null) initAzi();                      
        Index index = eleData.getIndex();
        return eleData.getFloat(index.set(ray));
      }

      public float getAzimuth(int ray) throws IOException {
        if(aziData == null) initAzi();
        //System.out.println("*** radial number is: \n" + ray);
        Index index = aziData.getIndex();
        return aziData.getFloat(index.set(ray));
      }

      public float getRadialDistance(int gate) throws IOException {
        Array data = radialCoordsys.getRadialAxisDataCached();
        Index index = data.getIndex();
        return data.getFloat(index.set(gate));
      }

      public float getTime(int ray) throws IOException {
        Array timeData = radialCoordsys.getTimeAxisDataCached();
        Index timeIndex = timeData.getIndex();
        return timeData.getFloat(timeIndex.set(sweepno, ray));
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
        sweepData = null;
        aziData = null;
        eleData = null; 
      }
    } // LevelII2Sweep class

  } // LevelII2Variable


  private static void testRadialVariable(RadialDatasetSweep.RadialVariable rv) throws IOException {
    int nsweep = rv.getNumSweeps();
    //System.out.println("*** radar Sweep number is: \n" + nsweep);
    Sweep sw;
    float mele;
    for (int i = 0; i < nsweep; i++) {
      //ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable getSweep " + i);
      sw = rv.getSweep(i);
      mele = sw.getMeanElevation();
      //ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable getSweep " + i);
      float me = sw.getMeanElevation();
      System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
      int nrays = sw.getRadialNumber();
      float [] az = new float[nrays];
      for (int j = 0; j < nrays; j++) {
        float azi = sw.getAzimuth(j);
        az[j] = azi;
      }
      //System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
    }
    sw = rv.getSweep(0);
      //ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable readData");
    float [] ddd = sw.readData();
      //ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable readData");
    assert(null != ddd);
    int nrays = sw.getRadialNumber();
    float [] az = new float[nrays];
    for (int i = 0; i < nrays; i++) {

      int ngates = sw.getGateNumber();
      assert(ngates > 0);
      float [] d = sw.readData(i);
      assert(null != d);
      float azi = sw.getAzimuth(i);
      assert(azi > 0);
      az[i] = azi;
      float ele = sw.getElevation(i);
      assert(ele > 0);
      float la = (float) sw.getOrigin(i).getLatitude();
      assert(la > 0);
      float lo = (float) sw.getOrigin(i).getLongitude();
      assert(lo > 0);
      float al = (float) sw.getOrigin(i).getAltitude();
      assert(al > 0);
    }
    assert(0 != nrays);
  }


  public static void main(String args[]) throws Exception, IOException, InstantiationException, IllegalAccessException {
    String fileIn = "/home/yuanho/dorade/CHGZ_2006071512.0300";

    //RadialDatasetSweepFactory datasetFactory = new RadialDatasetSweepFactory();
    //RadialDatasetSweep rds = datasetFactory.open(fileIn, null);
 // ucar.unidata.util.Trace.call1("LevelII2Dataset:main dataset");
    RadialDatasetSweep rds = (RadialDatasetSweep) TypedDatasetFactory.open( thredds.catalog.DataType.RADIAL, fileIn, null, new StringBuffer());
 // ucar.unidata.util.Trace.call2("LevelII2Dataset:main dataset");
    String st = rds.getStartDate().toString();
    String et = rds.getEndDate().toString();
    String id = rds.getRadarID();
    String name = rds.getRadarName();
    if (rds.isStationary()) {
      System.out.println("*** radar is stationary with name and id: " + name + " " + id);
    }
    List rvars = rds.getDataVariables();
    RadialDatasetSweep.RadialVariable vDM = (RadialDatasetSweep.RadialVariable) rds.getDataVariable("Reflectivity");
    testRadialVariable(vDM);
    for (int i = 0; i < rvars.size(); i++) {
      RadialDatasetSweep.RadialVariable rv = (RadialDatasetSweep.RadialVariable) rvars.get(i);
       testRadialVariable(rv);

      //  RadialCoordSys.makeRadialCoordSys( "desc", CoordinateSystem cs, VariableEnhanced v);
      // ucar.nc2.dt.radial.RadialCoordSys rcsys = rv.getRadialCoordSys();
    }

  }
} // LevelII2Dataset
