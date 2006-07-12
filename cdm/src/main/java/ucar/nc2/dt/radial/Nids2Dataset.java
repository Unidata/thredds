// $Id$
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
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.VariableSimpleIF;
import ucar.ma2.*;

import java.io.IOException;
import java.util.List;
import java.util.Date;

/**
 * Make a Nids NetcdfDataset into a RadialDataset.
 *
 * @author yuan
 * @version $Revision$ $Date$
 */

public class Nids2Dataset extends RadialDatasetSweepAdapter {
  private NetcdfDataset ds;

  /**
   * Constructor.
   * @param ds must be from nids IOSP
   */
  public Nids2Dataset( NetcdfDataset ds) {
    super(ds);
    this.ds = ds;
    desc = "Nids 2 radar dataset";

    try {
        if( ds.findGlobalAttribute( "isRadial").getNumericValue().intValue() == 0 ){
            parseInfo.append("*** Dataset is not a radial data\n");
            throw new IOException("Dataset is not a radial data\n");
        }
    } catch (Throwable e) {
      System.err.println("*********** Dataset is not a radial data "+e);
    }
    setEarthLocation();
    setTimeUnits();
    setStartDate();
    setEndDate();
    setBoundingBox();
  }

  public ucar.nc2.dt.EarthLocation  getCommonOrigin() {
        return origin;
  }

  public String getRadarID() {
        return ds.findGlobalAttribute( "Product_station").getStringValue();
  }

  public boolean isStationary(){
        return true;
  }

  public String getRadarName() {
        return ds.findGlobalAttribute( "Product_station").getStringValue();
  }

  public String getDataFormatName() {
        return "level III radar";
  }

  public boolean isRadial() {
        return (ds.findGlobalAttribute( "isRadial").getNumericValue().intValue() == 1);
  }

  public boolean isVolume() {
        return false;
  }


  protected void setEarthLocation() {
    double lat = ds.findGlobalAttribute( "RadarLatitude").getNumericValue().doubleValue();
    double lon = ds.findGlobalAttribute( "RadarLongitude").getNumericValue().doubleValue();
    double elev = ds.findGlobalAttribute( "RadarAltitude").getNumericValue().intValue();

    origin = new EarthLocationImpl(lat, lon, elev);
  }


  protected void setTimeUnits() {
    List axes = ds.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      if (axis.getAxisType() == AxisType.Time) {
        String units = axis.getUnitsString();
        dateUnits = (DateUnit) SimpleUnit.factory( units);
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

  public VariableSimpleIF getDataVariable( String name) {
    VariableEnhanced c = (VariableEnhanced) ds.findVariable(name);
    RadialCoordSys rcs = null;
    List csys  = c.getCoordinateSystems();
    for (int j=0; j< csys.size(); j++) {
      CoordinateSystem cs = (CoordinateSystem) csys.get(j);
      rcs = RadialCoordSys.makeRadialCoordSys( parseInfo, cs, c);
      if (rcs != null) break;
    }

    if (rcs != null){
      return makeRadialVariable( c, rcs);
    } else {

      return null;
    }
  }

  protected RadialVariable makeRadialVariable( VariableEnhanced varDS, RadialCoordSys gcs) {
    return new Nids2Variable( varDS, gcs);
  }

  public String getInfo() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("Nids2Dataset\n");
    sbuff.append(super.getDetailInfo());
    sbuff.append("\n\n");
    sbuff.append(parseInfo.toString());
    return sbuff.toString();
  }


  private class Nids2Variable extends VariableSimpleAdapter implements RadialDatasetSweep.RadialVariable {
    int nrays, ngates;
    Sweep sweep;
    protected RadialCoordSys radialCoordsys;
    protected VariableEnhanced ve;


    private Nids2Variable( VariableEnhanced v, RadialCoordSys rcys) {
      super(v);
      this.ve = v;
      this.radialCoordsys = rcys;

      int[] shape = v.getShape();
      int count = v.getRank() - 1;

      ngates = shape[count];
      count--;
      nrays = shape[count];
      count--;

      //sweep = new Sweep[nsweeps];
    }

    public float[] readAllData() throws IOException {
            Array allData;
            try {
                allData = ve.read();
            } catch (IOException e) {
                throw new IOException( e.getMessage());
            }
            return (float []) allData.get1DJavaArray( float.class);
        }
    public int getNumSweeps() { return 1; }

    public Sweep getSweep(int sn) {
      if(sn != 0) return null;
      if (sweep == null)
        sweep = new Nids2Sweep();
      return sweep;
    }

    public int getNumRadials() {
      return nrays;
    }



    //////////////////////////////////////////////////////////////////////
    private class Nids2Sweep  implements RadialDatasetSweep.Sweep {
      double meanElevation = Double.NaN;
      double meanAzimuth = Double.NaN;
      //int[] shape, origind;

      Nids2Sweep( ) {
        //shape = ve.getShape();
        //origind = new int[ ve.getRank()];
        setMeanElevation();
        setMeanAzimuth();
       // shape[0] = 1;
       // origind[0] = 1;
      }

      private void setMeanElevation() {
          if (Double.isNaN(meanElevation)) {
            try {
               Array data =  radialCoordsys.getElevationAxisDataCached();
               meanElevation = MAMath.sumDouble( data) / data.getSize();
             } catch (IOException e) {
               e.printStackTrace();
               meanElevation = 0.0;
             }
          }
        }

      public float getMeanElevation() {
        return (float) meanElevation;
      }

      private void setMeanAzimuth() {
        if(getType()!= null) {
           try {
               Array data =  radialCoordsys.getAzimuthAxisDataCached();
               meanAzimuth = MAMath.sumDouble( data) / data.getSize();
           } catch (IOException e) {
               e.printStackTrace();
               meanAzimuth = 0.0;
           }
         }
         else  meanAzimuth = 0.0;

      }

      public float getMeanAzimuth() {
         return (float)meanAzimuth;
      }

      public int getNumRadials() { return nrays; }

      public int getNumGates() { return ngates; }

      /* a 2D array nradials * ngates   */
      public float[] readData() throws IOException {
        Array allData = null;
        int [] shape = ve.getShape();
        int [] origind = new int[ ve.getRank()];
        try {
          allData = ve.read( origind, shape);
        } catch (InvalidRangeException e) {
          throw new IOException( e.getMessage());
        }
        return (float []) allData.get1DJavaArray( float.class);
      }

      /* read 1d data ngates */
      public float[] readData(int ray) throws java.io.IOException {
        Array rayData;
        int [] shape = ve.getShape();
        int [] origind = new int[ ve.getRank()];
        shape[0] = 1;
        origind[0] = ray;
        try{
            rayData = ve.read( origind, shape);
        } catch (ucar.ma2.InvalidRangeException e) {
            throw new IOException( e.getMessage());
        }
        return (float [])rayData.get1DJavaArray(Float.class);
      }


      public RadialDatasetSweep.Type getType() {
         return null;
      }

      public boolean isConic() { return true; }

      public float getElevation(int ray) throws IOException {
        Array data =  radialCoordsys.getElevationAxisDataCached();
        Index index = data.getIndex();
        return data.getFloat( index.set(ray));
      }

      public float getAzimuth(int ray) throws IOException {
        Array data =  radialCoordsys.getAzimuthAxisDataCached();
        Index index = data.getIndex();
        return data.getFloat( index.set(ray));
      }

      public float getRadialDistance(int gate) throws IOException {
        Array data =  radialCoordsys.getRadialAxisDataCached();
        Index index = data.getIndex();
        return data.getFloat( index.set(gate));
      }

      public float getTime(int ray) throws IOException {
        Array timeData =  radialCoordsys.getTimeAxisDataCached();
        Index timeIndex = timeData.getIndex();
        return timeData.getFloat( timeIndex.set( ray));
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

      public ucar.nc2.dt.EarthLocation getOrigin(int ray) {
        return  origin;
      }

    } // Nids2Sweep class

  } // Nids2Variable

    private static void testRadialVariable( RadialDatasetSweep.RadialVariable rv) throws IOException {
        int nsweep = rv.getNumSweeps();
        System.out.println("*** radar Sweep number is: \n" + nsweep);
        Sweep sw;

        sw = rv.getSweep(0);
        float [] ddd = sw.readData();
        assert(null != ddd);
        int nrays = sw.getRadialNumber();
        for (int i = 0; i < nrays; i++) {
            float t = sw.getTime(i);
            assert(t > 0);
            int ngates = sw.getGateNumber();
            assert(ngates > 0);
            float [] d =  sw.readData(i);
            assert(null != d);
            float azi =  sw.getAzimuth(i);
            assert(azi > 0);
            float ele =  sw.getElevation(i);
            assert(ele > 0);
            float la = (float) sw.getOrigin(i).getLatitude();
            assert(la > 0);
            float lo = (float) sw.getOrigin(i).getLongitude();
            assert(lo > 0);
            float al = (float) sw.getOrigin(i).getAltitude();
            assert(al > 0);
        }
    }

    public static void main(String args[]) throws Exception, IOException, InstantiationException, IllegalAccessException {
        String fileIn = "/home/yuanho/NIDS/Level3_BBB_N3R_20050119_1548.nids";
        //String fileIn = "/home/yuanho/NIDS/Level3_BYX_N1V_20051012_0000.nids";
        RadialDatasetSweepFactory datasetFactory = new RadialDatasetSweepFactory();
        RadialDatasetSweep rds =  datasetFactory.open( fileIn, null);
        //String st = rds.getStartDate().toString();
        //String et = rds.getEndDate().toString();
        //String id = rds.getRadarID();
        //String name = rds.getRadarName();

        //List rvars = rds.getDataVariables();
        RadialDatasetSweep.RadialVariable rf = (RadialDatasetSweep.RadialVariable) rds.getDataVariable("BaseReflectivity");
        rf.getSweep(0);

        String fileIn1 = "/home/yuanho/NIDS/Level3_BYX_N0V_20051013_0908.nids";
        RadialDatasetSweepFactory datasetFactory1 = new RadialDatasetSweepFactory();
        RadialDatasetSweep rds1 =  datasetFactory1.open( fileIn1, null);

        //List rvars1 = rds1.getDataVariables();
        RadialDatasetSweep.RadialVariable rf1 = (RadialDatasetSweep.RadialVariable) rds1.getDataVariable("Velocity");
        rf1.getSweep(0);

        testRadialVariable(rf);

    }

} // Nids2Dataset
