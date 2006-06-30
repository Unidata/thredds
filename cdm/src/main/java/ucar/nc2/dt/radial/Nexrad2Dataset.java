// $Id: Nexrad2Dataset.java,v 1.7 2005/10/20 18:39:41 caron Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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
import ucar.nc2.dt.RadialDataset.RadialVariable;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.ma2.*;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Make a Nexrad2 NetcdfDataset into a RadialDataset.
 *
 * @author caron
 * @version $Revision: 1.7 $ $Date: 2005/10/20 18:39:41 $
 */

public class Nexrad2Dataset extends RadialDatasetAdapterFixed {
  private NetcdfDataset ds;

  /**
   * Constructor.
   * @param ds must be from nexrad2 IOSP
   */
  public Nexrad2Dataset( NetcdfDataset ds) {
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
    double lat = ds.findGlobalAttribute( "StationLatitude").getNumericValue().doubleValue();
    double lon = ds.findGlobalAttribute( "StationLongitude").getNumericValue().doubleValue();
    double elev = ds.findGlobalAttribute( "StationElevationInMeters").getNumericValue().doubleValue();

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
    String start_datetime = ds.findAttValueIgnoreCase(null, "start_datetime", null);
    if (start_datetime != null)
      startDate = DateUnit.getStandardDate(start_datetime);
    else
      parseInfo.append("*** start_datetime not Found\n");
  }

  protected void setEndDate() {
    String end_datetime = ds.findAttValueIgnoreCase(null, "end_datetime", null);
    if (end_datetime != null)
      endDate = DateUnit.getStandardDate(end_datetime);
    else
      parseInfo.append("*** end_datetime not Found\n");
  }

  protected RadialVariable makeRadialVariable( VariableEnhanced varDS, RadialCoordSys gcs) {
    return new Nexrad2Variable( varDS, gcs);
  }

  public String getInfo() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("Nexrad2Dataset\n");
    sbuff.append(super.getDetailInfo());
    sbuff.append("\n\n");
    sbuff.append(parseInfo.toString());
    return sbuff.toString();
  }


  private class Nexrad2Variable extends RadialVariableAdapterFixed {
    int nsweeps, nrays, ngates;
    RadialDatasetFixed.Sweep[] sweep;

    private Nexrad2Variable( VariableEnhanced v, RadialCoordSys rcys) {
      super(v, rcys);

      int[] shape = v.getShape();
      int count = v.getRank() - 1;

      ngates = shape[count];
      count--;
      nrays = shape[count];
      count--;
      nsweeps = shape[count];

      sweep = new RadialDatasetFixed.Sweep[nsweeps];
    }

    public int getNumSweeps() { return nsweeps; }

    public RadialDatasetFixed.Sweep getSweep(int sweepNo) {
      if (sweep[sweepNo] == null)
        sweep[sweepNo] = new Nexrad2Sweep( sweepNo);
      return sweep[sweepNo];
    }

    public int getNumRadials() {
      return nsweeps * nrays;
    }

    public Radial getRadial(int nradial) throws IOException {
      int sweepNo = nradial / nsweeps;
      int rayInSweep = nradial % nsweeps;
      Nexrad2Sweep sweep= (Nexrad2Sweep) getSweep(sweepNo);
      return new Nexrad2Radial( sweep, rayInSweep);
    }

    // pretty inefficient !!
    public List getAllRadials() throws IOException {
      ArrayList all = new ArrayList();
      for (int i = 0; i < getNumRadials(); i++) {
        all.add( getRadial(i));
      }
      return all;
    }

    //////////////////////////////////////////////////////////////////////
    private class Nexrad2Sweep extends RadialVariableAdapterFixed.RadialSweep {
      double meanElevation = Double.NaN;
      int sweepno;
      int[] shape, origin;

      Nexrad2Sweep( int sweepno) {
        this.sweepno = sweepno;
        shape = ve.getShape();
        origin = new int[ ve.getRank()];
        shape[0] = 1;
        origin[0] = sweepno;
      }

      public float getMeanElevation() {
        if (Double.isNaN(meanElevation)) {
          try {
            Array data =  radialCoordsys.getElevationAxisDataCached();
            meanElevation = MAMath.sumDouble( data) / data.getSize();
          } catch (IOException e) {
            e.printStackTrace();
            meanElevation = 0.0;
          }
        }
        return (float) meanElevation;
      }

      public int getNumRadials() { return nrays; }
      public int getNumGates() { return ngates; }

      // a 2D array nradials * ngates
      public float[] readData() throws IOException {
        Array allData = null;
        try {
          allData = ve.read( origin, shape);
        } catch (InvalidRangeException e) {
          throw new IOException( e.getMessage());
        }
        return (float []) allData.get1DJavaArray( float.class);
      }

      public boolean isConic() { return true; }

      public float getElevation(int ray) throws IOException {
        Array data =  radialCoordsys.getElevationAxisDataCached();
        Index index = data.getIndex();
        return data.getFloat( index.set(sweepno, ray));
      }

      public float getAzimuth(int ray) throws IOException {
        Array data =  radialCoordsys.getAzimuthAxisDataCached();
        Index index = data.getIndex();
        return data.getFloat( index.set(sweepno, ray));
      }

      public float getRadialDistance(int gate) throws IOException {
        Array data =  radialCoordsys.getRadialAxisDataCached();
        Index index = data.getIndex();
        return data.getFloat( index.set(gate));
      }

      public float getTime(int ray) throws IOException {
        Array timeData =  radialCoordsys.getTimeAxisDataCached();
        Index timeIndex = timeData.getIndex();
        return timeData.getFloat( timeIndex.set(sweepno, ray));
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

      public boolean isGateSizeConstant() { return true; }

    } // Nexrad2Sweep class

            //////////////////////////////////////////////////////////////////////
    private class Nexrad2Radial implements Radial {
      private Nexrad2Sweep sweep;
      private int radialNo;

      Nexrad2Radial(Nexrad2Sweep sweep, int radialNo) {
        this.sweep = sweep;
        this.radialNo = radialNo;
      }

      public int getNumGates() {
        return ngates;
      }

      public float[] getData() throws IOException {
        return new float[0];
      }

      public float getBeamWidth() { return sweep.getBeamWidth(); }

      public float getNyquistFrequency() { return sweep.getNyquistFrequency(); }

      public float getRangeToFirstGate() { return sweep.getRangeToFirstGate(); }

      public float getGateSize() { return sweep.getGateSize(); }

      // coordinates of the radial data, relative to radial origin.
      public float getElevation() {
        try {
          return sweep.getElevation(radialNo);
        } catch (IOException e) {
          e.printStackTrace();
          return 0.0f;
        }
      }

      public float getAzimuth() {
        try {
          return sweep.getAzimuth(radialNo);
        } catch (IOException e) {
          e.printStackTrace();
          return 0.0f;
        }
      }

      public EarthLocation getOrigin() {
        return Nexrad2Dataset.this.getEarthLocation();
      }

      public double getTime() {
        try {
          return sweep.getTime(radialNo);
        } catch (IOException e) {
          e.printStackTrace();
          return 0.0f;
        }
      }

    } // Nexrad2Radial


  } // Nexrad2Variable

} // Nexrad2Dataset
