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
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.io.IOException;
import java.util.*;


/**
 * Make a NEXRAD Level 2 NetcdfDataset into a RadialDataset.
 *
 * @author yuan
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
      if (format.equals("ARCHIVE2") || format.equals("AR2V0001") || format.equals("CINRAD-SA")
              || format.equals("AR2V0003") || format.equals("AR2V0002") || format.equals("AR2V0004"))
        return true;
    }
    return false;
  }

  public TypedDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new LevelII2Dataset(ncd);
  }

  public FeatureType getScientificDataType() { return FeatureType.RADIAL; }


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
        RadialVariable rv = (RadialVariable) iter.next();
        Sweep sp = rv.getSweep(0);
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



  public boolean isVolume() {
    return true;
  }

  public boolean isHighResolution(NetcdfDataset nds) {
   // return true;
    Dimension r = nds.findDimension("scanR_HI");
    Dimension v = nds.findDimension("scanV_HI");
    if(r != null || v != null)
        return true;
    else
        return false;
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

  public void getRadialsNum() {
       Variable rayVars = ds.findVariable("numRadialsV");
  }

  protected void addRadialVariable(NetcdfDataset nds, Variable var) {
      RadialVariable rsvar = null;
      String vName = var.getShortName() ;
      int rnk = var.getRank();

      if ( rnk == 3 ) {
          if (!isHighResolution(nds)) {
             VariableSimpleIF v = new MyRadialVariableAdapter(vName, var.getAttributes());
             rsvar = makeRadialVariable(nds, v, var);
          } else {
            if(! vName.endsWith("_HI")) {
                 VariableSimpleIF v = new MyRadialVariableAdapter(vName, var.getAttributes());
                 rsvar = makeRadialVariable(nds, v, var);
          }
      }
      }

      if(rsvar != null)
        dataVariables.add(rsvar);
  }



  protected RadialVariable makeRadialVariable(NetcdfDataset nds, VariableSimpleIF v, Variable v0)  {
      // this function is null in level 2
      return new LevelII2Variable(nds, v, v0);
  }

  public String getInfo() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("LevelII2Dataset\n");
    sbuff.append(super.getDetailInfo());
    sbuff.append("\n\n");
    sbuff.append(parseInfo.toString());
    return sbuff.toString();
  }


  private class LevelII2Variable extends MyRadialVariableAdapter implements RadialDatasetSweep.RadialVariable {
    int nsweeps;
    int nsweepsHR;
    ArrayList sweeps;
    String name;

    private LevelII2Variable(NetcdfDataset nds, VariableSimpleIF v, Variable v0) {
      super(v.getName(), v0.getAttributes());

      nsweepsHR = 0;
      sweeps = new ArrayList();
      name = v.getName();
      if(isHighResolution(nds)) {
          String vname = v0.getName();
          Variable vehr = nds.findVariable(vname+"_HI");

          int [] shape1;
          if(vehr != null) {
            shape1 = vehr.getShape();
            int count1 = vehr.getRank() - 1;
            int ngatesHR = shape1[count1];
            count1--;
            int nraysHR = shape1[count1];
            count1--;
            nsweepsHR = shape1[count1];
            for(int i = 0; i< nsweepsHR; i++)
                sweeps.add(new LevelII2Sweep(vehr, i, nraysHR, ngatesHR) );

          }
       }

      int[] shape = v0.getShape();
      int count = v0.getRank() - 1;

      int ngates = shape[count];
      count--;
      int nrays = shape[count];
      count--;
      nsweeps = shape[count];
      for(int i = nsweepsHR; i< (nsweeps+nsweepsHR); i++)
        sweeps.add( new LevelII2Sweep(v0, i, nrays, ngates)) ;


    }
    public String toString() {
        return name;
    }

    public int getNumSweeps() {
      if(isHighResolution(ds)) {
          return nsweepsHR + nsweeps;
      }
      return nsweeps;
    }

    public Sweep getSweep(int sweepNo) {
       return (Sweep) sweeps.get(sweepNo);
    }

    public int getNumRadials() {
      return 0;
    }

    // a 3D array nsweep * nradials * ngates
    // if high resolution data, it will be transfered to the same dimension
    public float[] readAllData() throws IOException {
      Array allData;
      Array hrData = null;
      Sweep spn = (Sweep)sweeps.get(sweeps.size()-1);
      Variable v = spn.getsweepVar();
      try {
        allData = v.read();
      } catch (IOException e) {
        throw new IOException(e.getMessage());
      }
      if( !isHighResolution(ds) )
            return (float []) allData.get1DJavaArray(float.class);
      else {
            Sweep sp0 = (Sweep)sweeps.get(0);
            Variable v0 = sp0.getsweepVar();
            int [] stride;

            if(v0.getName().startsWith("Reflect"))
                  stride = new int [] {1, 2, 4};
            else
                  stride = new int [] {1, 2, 1};
          
            int[] shape1 = v.getShape();
            int[] shape2 = v0.getShape();
            int shp1 = (shape1[1]*stride[1] > shape2[1]) ? shape2[1] : shape1[1]*stride[1];
            int shp2 = (shape1[2]*stride[2] > shape2[2]) ? shape2[2] : shape1[2]*stride[2];
            
            int[] shape = {shape2[0], shp1, shp2};
            int [] origin = {0, 0, 0};
             Section section = null;

            try {
                section = new Section(origin, shape, stride);
            } catch (ucar.ma2.InvalidRangeException e) {
                e.printStackTrace();
            }

            try {
                hrData = v0.read(section);
            } catch (ucar.ma2.InvalidRangeException e) {
                e.printStackTrace();
            }
            // now append hrData and allData
            float [] fa1 =(float []) hrData.get1DJavaArray(float.class);
            float [] fa2 =(float []) allData.get1DJavaArray(float.class);
            float [] fa = new float[fa1.length + fa2.length];
            System.arraycopy(fa1, 0, fa, 0, fa1.length);
            System.arraycopy(fa2, 0, fa, fa1.length, fa2.length);

            return fa;
      }
    }

    public void clearVariableMemory() {
        for(int i = 0; i < nsweeps; i++) {

        }
    }


   //////////////////////////////////////////////////////////////////////
   // Checking all azi to make sure there is no missing data at sweep
   // level, since the coordinate is 1D at this level, this checking also
   // remove those missing radials within a sweep.

    private class LevelII2Sweep implements RadialDatasetSweep.Sweep {
      double meanElevation = Double.NaN;
      double meanAzimuth = Double.NaN;
      int nrays, ngates;
      int sweepno;
      Variable sweepVar;


      LevelII2Sweep(Variable v, int sweepno, int rays, int gates) {
        this.sweepVar = v;
        this.sweepno = sweepno;
        this.nrays = rays;
        this.ngates = gates;
        // ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable mine");
      }

      public Variable getsweepVar(){
          return sweepVar;
      }

      /* read 2d sweep data nradials * ngates */
      public float[] readData() throws java.io.IOException {

        if(!isHighResolution(ds) )  {
            return  sweepData(sweepno);
        } else {
            if( sweepno > (nsweepsHR-1) ) {
                int swpNo = sweepno - nsweepsHR;
                return  sweepData(swpNo);
            }
            else {
                return  sweepData(sweepno);
            }
        }

      }

      /* read from the radial variable */
      private float [] sweepData(int swpNumber) {
        int [] shape = sweepVar.getShape();
        int[] origin = new int[3];
        Array sweepTmp = null;

        // init section
        origin[0] = swpNumber;
        shape[0] = 1;

        try {
            sweepTmp = sweepVar.read(origin, shape).reduce();
        } catch (ucar.ma2.InvalidRangeException e) {
            e.printStackTrace();
        } catch (java.io.IOException e ) {
            e.printStackTrace();
        }
        return (float []) sweepTmp.get1DJavaArray(Float.TYPE);
      }

      //  private Object MUTEX =new Object();
      /* read 1d data ngates */
      public float[] readData(int ray) throws java.io.IOException {
        if(!isHighResolution(ds) )  {
             return  rayData(sweepno, ray);
        }  else {
            if( sweepno >= nsweepsHR ) {
                int swpNo = sweepno - (nsweepsHR);
                return  rayData(swpNo, ray);
            }
            else {
                return  rayData(sweepno, ray);
            }
        }
      }

     /* read the radial data from the radial variable */
      public float[] rayData( int swpNumber, int ray) throws java.io.IOException {
        int[] shape = sweepVar.getShape();
        int[] origin = new int[3];
        Array sweepTmp = null;

        // init section
        origin[0] = swpNumber;
        origin[1] = ray;  //shape[1] - numRadial + ray ;
        shape[0] = 1;
        shape[1] = 1;

        try {
            sweepTmp = sweepVar.read(origin, shape).reduce();
        } catch (ucar.ma2.InvalidRangeException e) {
            e.printStackTrace();
        } catch (java.io.IOException e ) {
            e.printStackTrace();
        }

        return (float []) sweepTmp.get1DJavaArray(Float.TYPE);
      }


      public void setMeanElevation() {
         String eleName;
         if(sweepVar.getName().startsWith("Reflectivity"))
            eleName = "elevationR";
         else
            eleName = "elevationV";

         if(!isHighResolution(ds) )  {
              setMeanEle(eleName, sweepno);
         }  else {
            if( sweepno >= nsweepsHR ) {
                int swpNo = sweepno - (nsweepsHR);
                setMeanEle(eleName,  swpNo);
            }
            else {
                setMeanEle(eleName+"_HI", sweepno);
            }
        }
      }

      private void setMeanEle(String elevName,  int swpNumber) {
        Array eleData = null;
        float sum = 0;
        int sumSize = 0;

        try{
            Variable evar = ds.findVariable(elevName);
            Array eleTmp =  evar.read();
            evar.setCachedData(eleTmp, false);
            int [] eleOrigin = new int[2];
            eleOrigin[0] = swpNumber;
            eleOrigin[1] = 0;
            int [] eleShape = {1, getRadialNumber()};
            eleData = eleTmp.section(eleOrigin, eleShape);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ucar.ma2.InvalidRangeException e) {
            e.printStackTrace();
        }

        float [] eleArray = (float []) eleData.get1DJavaArray(Float.TYPE);
        int size = (int)eleData.getSize();
        for(int i= 0; i< size; i++) {
            if(!Float.isNaN(eleArray[i])) {
                sum = sum + eleArray[i];
                sumSize++;
            }
        }
        meanElevation = sum/sumSize;  //MAMath.sumDouble(eleData) / eleData.getSize();

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
        return sum / size;
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
         String aziName;
         if(sweepVar.getName().startsWith("Reflectivity"))
            aziName = "azimuthR";
         else
            aziName = "azimuthV";
       if(!isHighResolution(ds) )  {
              setMeanAzi(aziName, sweepno);
        }  else {
            if( sweepno >= nsweepsHR ) {
                int swpNo = sweepno - (nsweepsHR);
                setMeanAzi(aziName, swpNo);
            }
            else {
                setMeanAzi(aziName+"_HI", sweepno);
            }
        }
     }

     private void setMeanAzi(String aziName,  int swpNumber) {
       Array aziData = null;

       if (getType() != null) {
          try {
               Array data =  ds.findVariable(aziName).read();
               int [] aziOrigin = new int[2];
               aziOrigin[0] = swpNumber;
               aziOrigin[1] = 0;  //shape[1] - getRadialNumber();
               int [] aziShape = {1, getRadialNumber()};
               aziData = data.section(aziOrigin, aziShape);
               meanAzimuth = MAMath.sumDouble( aziData) / aziData.getSize();
           } catch (IOException e) {
               e.printStackTrace();
               meanAzimuth = 0.0;
           }  catch (ucar.ma2.InvalidRangeException e) {
            e.printStackTrace();
        }

       } else
           meanAzimuth = 0.0;

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
        String eleName;
        if(sweepVar.getName().startsWith("Reflectivity"))
           eleName = "elevationR";
        else
            eleName = "elevationV";
        if(!isHighResolution(ds) )  {
             return  getEle(eleName, sweepno, ray);
        }  else {
            if( sweepno >= nsweepsHR ) {
                int swpNo = sweepno - (nsweepsHR);
                return  getEle(eleName, swpNo, ray);
            }
            else {
                return  getEle(eleName+"_HI", sweepno, ray);
            }
        }
      }

      public float getEle(String elevName, int swpNumber, int ray) throws IOException {
        Array eleData = null;

        try {
            Variable evar = ds.findVariable(elevName);
            Array eleTmp = evar.read();
            evar.setCachedData(eleTmp, false);
            int [] eleOrigin = new int[2];
            eleOrigin[0] = swpNumber;
            eleOrigin[1] = 0; //shape[1] - getRadialNumber();
            int [] eleShape = {1, getRadialNumber()};
            eleData = eleTmp.section(eleOrigin, eleShape);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ucar.ma2.InvalidRangeException e) {
            e.printStackTrace();
        }

        // if(eleData == null) initAzi();
        Index index = eleData.getIndex();
        return eleData.getFloat(index.set(ray));
      }

      public float[] getElevation() throws IOException {
         String eleName;
         if(sweepVar.getName().startsWith("Reflectivity"))
            eleName = "elevationR";
         else
            eleName = "elevationV";
        if(!isHighResolution(ds) )  {
             return  getEle(eleName, sweepno);
        }  else {
            if( sweepno >= nsweepsHR ) {
                int swpNo = sweepno - (nsweepsHR);
                return  getEle(eleName, swpNo);
            }
            else {
                return  getEle(eleName+"_HI", sweepno);
            }
        }
      }

     public float[] getEle(String elevName, int swpNumber) throws IOException {
        Array eleData = null;

        if(eleData == null) {
            try {
                Variable evar = ds.findVariable(elevName);
                Array eleTmp = evar.read();
                evar.setCachedData(eleTmp, false);
                int [] eleOrigin = new int[2];
                eleOrigin[0] = swpNumber;
                eleOrigin[1] = 0;
                int [] eleShape = {1, getRadialNumber()};
                eleData = eleTmp.section(eleOrigin, eleShape);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ucar.ma2.InvalidRangeException e) {
                e.printStackTrace();
            }
        }

        return (float [])eleData.get1DJavaArray(Float.TYPE);
     }

     public float[] getAzimuth() throws IOException {
        String aziName;
        if(sweepVar.getName().startsWith("Reflectivity"))
            aziName = "azimuthR";
        else
            aziName = "azimuthV";

        if(!isHighResolution(ds) )  {
             return  getAzi(aziName, sweepno);
        }  else {
            if( sweepno >= nsweepsHR ) {
                int swpNo = sweepno - (nsweepsHR);
                return  getAzi(aziName, swpNo);
            }
            else {
                return  getAzi(aziName+"_HI", sweepno);
            }
        }
     }

     public float[] getAzi(String aziName, int swpNumber) throws IOException {
        Array aziData = null;

        if(aziData == null) {
            try {
                Variable avar = ds.findVariable(aziName);
                Array aziTmp = avar.read();
                avar.setCachedData(aziTmp, false);
                int [] aziOrigin = new int[2];
                aziOrigin[0] = swpNumber;
                aziOrigin[1] = 0;  //shape[1] - getRadialNumber();
                int [] aziShape = {1, getRadialNumber()};
                aziData = aziTmp.section(aziOrigin, aziShape);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ucar.ma2.InvalidRangeException e) {
                e.printStackTrace();
            }
        }

        return (float [])aziData.get1DJavaArray(Float.TYPE);
     }

     public float getAzimuth(int ray) throws IOException {
        String aziName;
        if(sweepVar.getName().startsWith("Reflectivity"))
            aziName = "azimuthR";
        else
            aziName = "azimuthV";

        if(!isHighResolution(ds) )  {
             return  getAzi(aziName, sweepno, ray);
        }  else {
            if( sweepno >= nsweepsHR ) {
                int swpNo = sweepno - (nsweepsHR);
                return  getAzi(aziName, swpNo, ray);
            }
            else {
                return  getAzi(aziName+"_HI", sweepno, ray);
            }
        }
     }

      public float getAzi(String aziName, int swpNumber, int ray) throws IOException {
        Array aziData = null;
       // int[] shape = ve.getShape();
        if(aziData == null) {
            try {
                Variable avar = ds.findVariable(aziName);
                Array aziTmp = avar.read();
                avar.setCachedData(aziTmp, false);
                int [] aziOrigin = new int[2];
                aziOrigin[0] = swpNumber;
                aziOrigin[1] = 0; //shape[1] - getRadialNumber();
                int [] aziShape = {1, getRadialNumber()};
                aziData = aziTmp.section(aziOrigin, aziShape);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ucar.ma2.InvalidRangeException e) {
                e.printStackTrace();
            }
        }
        // if(aziData == null) initAzi();
        //System.out.println("*** radial number is: \n" + ray);
        Index index = aziData.getIndex();
        return aziData.getFloat(index.set(ray));
      }

      public float getRadialDistance(int gate) throws IOException {
        String disName;
        if(sweepVar.getName().startsWith("Reflectivity"))
            disName = "distanceR";
        else
            disName = "distanceV";

        if(!isHighResolution(ds) )  {
             return  getRadialDist(disName, gate);
        }  else {
            if( sweepno >= nsweepsHR ) {

                return  getRadialDist(disName, gate);
            }
            else {
                return  getRadialDist(disName+"_HI", gate);
            }
        }
      }

      public float getRadialDist(String dName, int gate) throws IOException {
        Variable dvar = ds.findVariable(dName);
        Array data = dvar.read();
        dvar.setCachedData(data, false);
        Index index = data.getIndex();
        return data.getFloat(index.set(gate));
      }

      public float getTime(int ray) throws IOException {
        String tName;
        if(sweepVar.getName().startsWith("Reflectivity"))
            tName = "timeR";
        else
            tName = "timeV";
        if(!isHighResolution(ds) )  {
             return  getT(tName, sweepno, ray);
        }  else {
            if( sweepno >= nsweepsHR ) {
                int swpNo = sweepno - (nsweepsHR);
                return  getT(tName, swpNo, ray);
            }
            else {
                return  getT(tName+"_HI", sweepno,ray);
            }
        }
      }

      public float getT(String tName, int swpNumber, int ray) throws IOException {
        Variable tvar = ds.findVariable(tName);
        Array timeData = tvar.read();
        tvar.setCachedData(timeData, false);
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
    Sweep sw;
    float mele;
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
      float [] azz = sw.getAzimuth();
      float [] dat = sw.readData();
      // System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
    }
    sw = rv.getSweep(0);
      //ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable readData");
    float [] data = rv.readAllData();
    float [] ddd = sw.readData();
    float [] da = sw.getAzimuth();
    float [] de = sw.getElevation();
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


  public static void main(String args[]) throws Exception, IOException, InstantiationException, IllegalAccessException {
   String fileIn = "/home/yuanho/Download/KCLX_20091019_2021";
   // String fileIn ="/upc/share/testdata2/radar/NOP3_20071112_1633";
    //RadialDatasetSweepFactory datasetFactory = new RadialDatasetSweepFactory();
    //RadialDatasetSweep rds = datasetFactory.open(fileIn, null);
  //ucar.unidata.util.Trace.call1("LevelII2Dataset:main dataset");
    RadialDatasetSweep rds = (RadialDatasetSweep) TypedDatasetFactory.open( FeatureType.RADIAL, fileIn, null, new StringBuilder());
      //ucar.unidata.util.Trace.call2("LevelII2Dataset:main dataset");
    String st = rds.getStartDate().toString();
    String et = rds.getEndDate().toString();
    String id = rds.getRadarID();
    String name = rds.getRadarName();
    if (rds.isStationary()) {
      System.out.println("*** radar is stationary with name and id: " + name + " " + id);
    }
    List rvars = rds.getDataVariables();
    RadialDatasetSweep.RadialVariable vDM = (RadialDatasetSweep.RadialVariable) rds.getDataVariable("Reflectivity");
    vDM.readAllData();
    testRadialVariable(vDM);
    for (int i = 0; i < rvars.size(); i++) {
      RadialDatasetSweep.RadialVariable rv = (RadialDatasetSweep.RadialVariable) rvars.get(i);
       testRadialVariable(rv);

      //  RadialCoordSys.makeRadialCoordSys( "desc", CoordinateSystem cs, VariableEnhanced v);
      // ucar.nc2.dt.radial.RadialCoordSys rcsys = rv.getRadialCoordSys();
    }

  }
} // LevelII2Dataset
