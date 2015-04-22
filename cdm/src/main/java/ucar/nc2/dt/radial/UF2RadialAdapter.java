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
import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.constants.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.*;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.io.IOException;
import java.util.*;


public class UF2RadialAdapter extends AbstractRadialAdapter {
  private NetcdfDataset ds;
  double latv, lonv, elev;
  DateFormatter formatter = new DateFormatter();

  /////////////////////////////////////////////////
  public Object isMine( FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException {
    String convention = ncd.findAttValueIgnoreCase(null, "Conventions", null);
    if ((null != convention) && convention.equals(_Coordinate.Convention)) {
      String format = ncd.findAttValueIgnoreCase(null, "Format", null);
      if (format != null && format.equals("UNIVERSALFORMAT"))
        return this;
    }
    return null;
  }

  public FeatureDataset open( FeatureType ftype, NetcdfDataset ncd, Object analysis, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    return new UF2RadialAdapter(ncd);
  }

  public FeatureType getScientificDataType() { return FeatureType.RADIAL; }

  // needed for FeatureDatasetFactory
  public UF2RadialAdapter() {}

  /**
   * Constructor.
   *
   * @param ds must be from nexrad2 IOSP
   */
  public UF2RadialAdapter(NetcdfDataset ds) {
    super(ds);
    this.ds = ds;
    desc = "UF 2 radar dataset";

    setEarthLocation();
    try {
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

    if (origin == null)
      return;

    double dLat = Math.toDegrees( getMaximumRadialDist() / Earth.getRadius());
    double latRadians = Math.toRadians( origin.getLatitude());
    double dLon = dLat * Math.cos(latRadians);

    double lat1 = origin.getLatitude() - dLat/2;
    double lon1 = origin.getLongitude() - dLon/2;
    bb = new LatLonRect( new LatLonPointImpl( lat1, lon1), dLat, dLon);

    boundingBox = bb;
  }

  double getMaximumRadialDist() {
    double maxdist = 0.0;
    Iterator iter = dataVariables.iterator();

    while (iter.hasNext()) {
        RadialDatasetSweep.RadialVariable rv = (RadialDatasetSweep.RadialVariable) iter.next();
        RadialDatasetSweep.Sweep sp = rv.getSweep(0);
        double dist = sp.getGateNumber() * sp.getGateSize();

        if (dist > maxdist)
          maxdist = dist;
    }

    return maxdist;
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

    origin = new ucar.unidata.geoloc.EarthLocationImpl(latv, lonv, elev);
  }

  public ucar.unidata.geoloc.EarthLocation getCommonOrigin() {
    return origin;
  }

  public String getRadarID() {
    Attribute ga = ds.findGlobalAttribute("instrument_name");
    if(ga != null)
        return ga.getStringValue();
    else
        return "XXXX";
  }

  public String getRadarName() {
    Attribute ga = ds.findGlobalAttribute("site_name");
    if(ga != null)
        return ga.getStringValue();
    else
        return "Unknown Station";
  }

  public String getDataFormat() {
    return "Universal Format";
  }



  public boolean isVolume() {
    return true;
  }



  public boolean isStationary() {
    return true;
  }

  protected void setTimeUnits() throws Exception {
    List axes = ds.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      if (axis.getAxisType() == AxisType.Time) {
        String units = axis.getUnitsString();
        dateUnits =  new DateUnit(units);
        calDateUnits = CalendarDateUnit.of(null, units);
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
          RadialDatasetSweep.RadialVariable radVar = (RadialDatasetSweep.RadialVariable)iter.next();
          radVar.clearVariableMemory();
      }
  }


  protected void addRadialVariable(NetcdfDataset nds, Variable var) {
      RadialDatasetSweep.RadialVariable rsvar = null;
      String vName = var.getShortName() ;
      int rnk = var.getRank();

      if ( rnk == 3 ) {
         VariableSimpleIF v = new AbstractRadialAdapter.MyRadialVariableAdapter(vName, var.getAttributes());
         rsvar = makeRadialVariable(nds, v, var);
      }

      if(rsvar != null)
        dataVariables.add(rsvar);
  }



  protected RadialDatasetSweep.RadialVariable makeRadialVariable(NetcdfDataset nds, VariableSimpleIF v, Variable v0)  {
      // this function is null in level 2
      return new UF2Variable(nds, v, v0);
  }

  public String getInfo() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("UF2Dataset\n");
    sbuff.append(super.getDetailInfo());
    sbuff.append("\n\n");
    sbuff.append(parseInfo.toString());
    return sbuff.toString();
  }


  private class UF2Variable extends AbstractRadialAdapter.MyRadialVariableAdapter implements RadialDatasetSweep.RadialVariable {
    int nsweeps;

    ArrayList sweeps;
    String name;

    private UF2Variable(NetcdfDataset nds, VariableSimpleIF v, Variable v0) {
      super(v.getShortName(), v0.getAttributes());


      sweeps = new ArrayList();
      name = v.getShortName();


      int[] shape = v0.getShape();
      int count = v0.getRank() - 1;

      int ngates = shape[count];
      count--;
      int nrays = shape[count];
      count--;
      nsweeps = shape[count];

      for(int i = 0; i< nsweeps; i++)
        sweeps.add( new UF2Sweep(v0, i, nrays, ngates)) ;

    }
    public String toString() {
        return name;
    }

    public int getNumSweeps() {
      return nsweeps;
    }

    public RadialDatasetSweep.Sweep getSweep(int sweepNo) {
       return (RadialDatasetSweep.Sweep) sweeps.get(sweepNo);
    }

    public int getNumRadials() {
      return 0;
    }

    // a 3D array nsweep * nradials * ngates
    // if high resolution data, it will be transfered to the same dimension
    public float[] readAllData() throws IOException {
      Array allData;
      Array hrData = null;
      RadialDatasetSweep.Sweep spn = (RadialDatasetSweep.Sweep)sweeps.get(sweeps.size()-1);
      Variable v = spn.getsweepVar();
      try {
        allData = v.read();
      } catch (IOException e) {
        throw new IOException(e.getMessage());
      }

      return (float []) allData.get1DJavaArray(float.class);

    }

    public void clearVariableMemory() {
        for(int i = 0; i < nsweeps; i++) {

        }
    }


   //////////////////////////////////////////////////////////////////////
   // Checking all azi to make sure there is no missing data at sweep
   // level, since the coordinate is 1D at this level, this checking also
   // remove those missing radials within a sweep.

    private class UF2Sweep implements RadialDatasetSweep.Sweep {
      double meanElevation = Double.NaN;
      double meanAzimuth = Double.NaN;
      int nrays, ngates;
      int sweepno;
      Variable sweepVar;
      String abbrev;

      UF2Sweep(Variable v, int sweepno, int rays, int gates) {
        this.sweepVar = v;
        this.sweepno = sweepno;
        this.nrays = rays;
        this.ngates = gates;
        // ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable mine");

        Attribute att = sweepVar.findAttribute("abbrev");
        abbrev = att.getStringValue();
      }

      public Variable getsweepVar(){
          return sweepVar;
      }

      /* read 2d sweep data nradials * ngates */
      public float[] readData() throws java.io.IOException {
          return  sweepData(sweepno);
      }

      /* read from the radial variable */
      private float [] sweepData(int swpNumber) throws java.io.IOException {
        int[] shape = sweepVar.getShape();
        int[] origin = new int[shape.length];

        // init section
        origin[0] = swpNumber;
        shape[0] = 1;

        try {
            Array sweepTmp = sweepVar.read(origin, shape).reduce();
            return (float []) sweepTmp.get1DJavaArray(Float.TYPE);
        } catch (ucar.ma2.InvalidRangeException e) {
            throw new IOException(e);
        }
      }

      //  private Object MUTEX =new Object();
      /* read 1d data ngates */
      public float[] readData(int ray) throws java.io.IOException {
             return  rayData(sweepno, ray);
      }

     /* read the radial data from the radial variable */
      public float[] rayData( int swpNumber, int ray) throws java.io.IOException {
        int[] shape = sweepVar.getShape();
        int[] origin = new int[shape.length];

        // init section
        origin[0] = swpNumber;
        origin[1] = ray;  //shape[1] - numRadial + ray ;
        shape[0] = 1;
        shape[1] = 1;

        try {
            Array sweepTmp = sweepVar.read(origin, shape).reduce();
            return (float []) sweepTmp.get1DJavaArray(Float.TYPE);
        } catch (ucar.ma2.InvalidRangeException e) {
            throw new IOException(e);
        }
      }

      public void setMeanElevation() {
          String eleName;

          eleName = "elevation" + abbrev;
          setMeanEle(eleName, sweepno);
      }

      private void setMeanEle(String elevName, int swpNumber) {
          try {
              float[] eleData = getEle(elevName, swpNumber);

              float sum = 0;
              int sumSize = 0;
              for (float v : eleData)
                  if(!Float.isNaN(v)) {
                      sum += v;
                      sumSize++;
                  }

              if (sumSize > 0)
                  meanElevation = sum / sumSize;
          } catch (IOException e) {
              e.printStackTrace();
          }
      }

      public float getMeanElevation() {
        if( Double.isNaN(meanElevation) )
            setMeanElevation();
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


      public void setMeanAzimuth() {
         String aziName = "azimuth" + abbrev;
         setMeanAzi(aziName, sweepno);
     }

     private void setMeanAzi(String aziName,  int swpNumber) {
       if (getType() != null) {
          try {
               Array data =  ds.findVariable(aziName).read();
               int [] aziOrigin = new int[2];
               aziOrigin[0] = swpNumber;
               aziOrigin[1] = 0;  //shape[1] - getRadialNumber();
               int [] aziShape = {1, getRadialNumber()};
              Array aziData = data.section(aziOrigin, aziShape);
               meanAzimuth = MAMath.sumDouble( aziData) / aziData.getSize();
           } catch (IOException e) {
               e.printStackTrace();
               meanAzimuth = 0.0;
           }  catch (ucar.ma2.InvalidRangeException e) {
            e.printStackTrace();
        }

       } else {
           meanAzimuth = 0.0;
       }
     }

      public float getMeanAzimuth() {
        if(Double.isNaN(meanAzimuth))
            setMeanAzimuth();
        return (float) meanAzimuth;
      }

      public boolean isConic() {
          return true;
      }

      public float getElevation(int ray) throws IOException {
          String eleName = "elevation" + abbrev;
          return  getEle(eleName, sweepno, ray);

      }

      public float getEle(String elevName, int swpNumber, int ray) throws IOException {
          float[] eleData = getEle(elevName, swpNumber);
          return eleData[ray];
      }

      public float[] getElevation() throws IOException {
          String eleName = "elevation" + abbrev;
          return  getEle(eleName, sweepno);
      }

     public float[] getEle(String elevName, int swpNumber) throws IOException {
         try {
             Array eleData = ds.findVariable(elevName).read();
             int [] eleOrigin = new int[2];
             eleOrigin[0] = swpNumber;
             eleOrigin[1] = 0;
             int [] eleShape = {1, getRadialNumber()};
             eleData = eleData.section(eleOrigin, eleShape);
             return (float [])eleData.get1DJavaArray(Float.TYPE);
         } catch (ucar.ma2.InvalidRangeException e) {
             throw new IOException(e);
         }
     }

     public float[] getAzimuth() throws IOException {
         String aziName = "azimuth" + abbrev;
         return  getAzi(aziName, sweepno);
     }

     public float[] getAzi(String aziName, int swpNumber) throws IOException {
         try {
             Array aziData = ds.findVariable(aziName).read();
             int [] aziOrigin = new int[2];
             aziOrigin[0] = swpNumber;
             aziOrigin[1] = 0;  //shape[1] - getRadialNumber();
             int [] aziShape = {1, getRadialNumber()};
             aziData = aziData.section(aziOrigin, aziShape);
             return (float [])aziData.get1DJavaArray(Float.TYPE);
         } catch (ucar.ma2.InvalidRangeException e) {
             throw new IOException(e);
         }
     }

     public float getAzimuth(int ray) throws IOException {
         String aziName = "azimuth" + abbrev;
         return  getAzi(aziName, sweepno, ray);
     }

      public float getAzi(String aziName, int swpNumber, int ray) throws IOException {
          float[] aziData = getAzi(aziName, swpNumber);
          return aziData[ray];
      }

      public float getRadialDistance(int gate) throws IOException {
         String disName = "distance" + abbrev;
         return  getRadialDist(disName, gate);

      }

      public float getRadialDist(String dName, int gate) throws IOException {
        Array data = ds.findVariable(dName).read();
        Index index = data.getIndex();
        return data.getFloat(index.set(gate));
      }

      public float getTime(int ray) throws IOException {
        String tName = "time" + abbrev;
        return  getT(tName, sweepno, ray);
      }

      public float getT(String tName, int swpNumber, int ray) throws IOException {
        Array timeData = ds.findVariable(tName).read();
        Index timeIndex = timeData.getIndex();
        return timeData.getFloat(timeIndex.set(swpNumber, ray));
      }

      public float getBeamWidth() {
        return 0.95f; // degrees, info from Chris Burkhart
      }

      public float getNyquistFrequency() {
        return 0; // LOOK this may be radial specific
      }

      public float getRangeToFirstGate() {
        try {
          return getRadialDistance(0);
        } catch (IOException e) {
          e.printStackTrace();
          return 0.0f;
        }
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
    } // LevelII2Sweep class

  } // LevelII2Variable


  private static void testRadialVariable(RadialDatasetSweep.RadialVariable rv) throws IOException {
    int nsweep = rv.getNumSweeps();
    //System.out.println("*** radar Sweep number is: \n" + nsweep);
    RadialDatasetSweep.Sweep sw;
    for (int i = 0; i < nsweep; i++) {
      //ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable getSweep " + i);
      sw = rv.getSweep(i);
      //mele = sw.getMeanElevation();
      //ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable getSweep " + i);
      float me = sw.getMeanElevation();

      System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
      int nrays = sw.getRadialNumber();
      float [] az = new float[nrays];
      for (int j = 0; j < nrays; j++) {
        float azi = sw.getAzimuth(j);
        az[j] = azi;
      }
      // System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
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
     // float [] e = sw.readDataNew(i);
     // assert(null != e);
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

}
