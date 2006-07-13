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
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.*;
import java.io.IOException;
import java.util.List;
import java.util.Date;


/**
 * Make a LevelII2 NetcdfDataset into a RadialDataset.
 *
 * @author yuan
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class LevelII2Dataset extends RadialDatasetSweepAdapter {
    private NetcdfDataset ds;
    double latv, lonv, elev;
    DateFormatter formatter = new DateFormatter();

  /**
   * Constructor.
   * @param ds must be from nexrad2 IOSP
   */
    public LevelII2Dataset( NetcdfDataset ds) {
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
        latv = ds.findGlobalAttribute( "StationLatitude").getNumericValue().doubleValue();
        lonv = ds.findGlobalAttribute( "StationLongitude").getNumericValue().doubleValue();
        elev = ds.findGlobalAttribute( "StationElevationInMeters").getNumericValue().doubleValue();

        origin = new EarthLocationImpl(latv, lonv, elev);
    }

    public ucar.nc2.dt.EarthLocation  getCommonOrigin() {
        return origin;
    }

    public String getRadarID() {
        return ds.findGlobalAttribute( "Station").getStringValue();
    }

    public String getRadarName() {
        return ds.findGlobalAttribute( "StationName").getStringValue();
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

    public boolean isStationary(){
        return true;
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

    protected RadialVariable makeRadialVariable( VariableEnhanced varDS, RadialCoordSys gcs) {
        return new LevelII2Variable( varDS, gcs);
    }

    public String getInfo() {
        StringBuffer sbuff = new StringBuffer();
        sbuff.append("LevelII2Dataset\n");
        sbuff.append(super.getDetailInfo());
        sbuff.append("\n\n");
        sbuff.append(parseInfo.toString());
        return sbuff.toString();
    }


    private class LevelII2Variable  extends VariableSimpleAdapter implements RadialDatasetSweep.RadialVariable {
        protected RadialCoordSys radialCoordsys;
        protected VariableEnhanced ve;
        int nsweeps, nrays, ngates;
        RadialDatasetSweep.Sweep[] sweep;

        private LevelII2Variable( VariableEnhanced v, RadialCoordSys rcys) {
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

        public int getNumSweeps() { return nsweeps; }

        public Sweep getSweep(int sweepNo) {
            if (sweep[sweepNo] == null)
                sweep[sweepNo] = new LevelII2Sweep( sweepNo);
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
                throw new IOException( e.getMessage());
            }
            return (float []) allData.get1DJavaArray( float.class);
        }


    //////////////////////////////////////////////////////////////////////
        private class LevelII2Sweep implements RadialDatasetSweep.Sweep {
            double meanElevation = Double.NaN;
            double meanAzimuth = Double.NaN;
            int sweepno;

            LevelII2Sweep( int sweepno) {
                this.sweepno = sweepno;
            }

           /* read 2d sweep data nradials * ngates */
            public float[] readData() throws java.io.IOException {
                Array sweepData ;

                int[] shape = ve.getShape();
                int[] origin = new int[ ve.getRank()];
                shape[0] = 1;
                origin[0] = sweepno;

                try{
                    sweepData = ve.read( origin, shape);
                } catch (ucar.ma2.InvalidRangeException e) {
                    throw new IOException( e.getMessage());
                }
                return (float [])sweepData.get1DJavaArray(Float.class);
            }

            /* read 1d data ngates */
            public float[] readData(int ray) throws java.io.IOException {
                Array rayData;

                int[] shape = ve.getShape();
                int[] origin = new int[ ve.getRank()];
                shape[0] = 1;
                origin[0] = sweepno;
                shape[1] = 1;
                origin[1] = ray;

                try{
                    rayData = ve.read( origin, shape);
                } catch (ucar.ma2.InvalidRangeException e) {
                    throw new IOException( e.getMessage());
                }
                return (float [])rayData.get1DJavaArray(Float.class);
            }

            public float getMeanElevation() {
                //if (Double.isNaN(meanElevation)) {
                Array data;
                Array dataa = null;
                int[] shap;
                int[] orig;
                try {
                    data =  radialCoordsys.getElevationAxisDataCached();
                    shap = data.getShape();
                    shap[0] = 1;
                    orig = new int[ data.getRank()];
                    orig[0] = sweepno;
                    dataa = data.section(orig, shap);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ucar.ma2.InvalidRangeException r) {
                    r.printStackTrace();
                }

                meanElevation = meanDouble( dataa );

                return (float) meanElevation ;
            }

            public  double meanDouble( Array a) {
                double sum = 0;
                int size = 0;

                IndexIterator iterA = a.getIndexIterator();
                while (iterA.hasNext()){
                    double s = iterA.getDoubleNext();

                    if( ! Double.isNaN(s) ) {
                        sum += s;
                        size ++;
                    }
                }
                return sum/size;
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

            public ucar.nc2.dt.EarthLocation getOrigin(int ray) {
                return  origin;
            }

            public Date getStartingTime() {
                return startDate;
            }

            public Date getEndingTime() {
                return endDate;
            }

            public int getNumRadials() {
                return nrays;
            }

            public int getNumGates() {
                return ngates;
            }

            public float getMeanAzimuth() {
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

                return (float)meanAzimuth;
            }

            public boolean isConic() {
                return true;
            }

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

        } // LevelII2Sweep class

    } // LevelII2Variable


    private static void testRadialVariable( RadialDatasetSweep.RadialVariable rv) throws IOException {
        int nsweep = rv.getNumSweeps();
        System.out.println("*** radar Sweep number is: \n" + nsweep);
        Sweep sw;

        for (int i = 0; i< nsweep; i++){
            sw = rv.getSweep(i);
            float me = sw.getMeanElevation();
            System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
            int nrays = sw.getRadialNumber();
            float [] az = new float[nrays];
            for (int j = 0; j < nrays; j++) {
                float azi =  sw.getAzimuth(j);
                az[j] = azi;
            }
            //System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
        }
        sw = rv.getSweep(0);
        float [] ddd = sw.readData();
        assert(null != ddd);
        int nrays = sw.getRadialNumber();
        float [] az = new float[nrays];
        for (int i = 0; i < nrays; i++) {

            int ngates = sw.getGateNumber();
            assert(ngates > 0);
            float [] d =  sw.readData(i);
            assert(null != d);
            float azi =  sw.getAzimuth(i);
            assert(azi > 0);
            az[i] = azi;
            float ele =  sw.getElevation(i);
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
        String fileIn = "/home/yuanho/dorade/KATX_20040113_0107";

        RadialDatasetSweepFactory datasetFactory = new RadialDatasetSweepFactory();
        RadialDatasetSweep rds =  datasetFactory.open( fileIn, null);
        String st = rds.getStartDate().toString();
        String et = rds.getEndDate().toString();
        String id = rds.getRadarID();
        String name = rds.getRadarName();
        if(rds.isStationary()) {
           System.out.println("*** radar is stationary with name and id: "+ name + " " + id);
        }
        List rvars = rds.getDataVariables();
        RadialDatasetSweep.RadialVariable vDM = (RadialDatasetSweep.RadialVariable) rds.getDataVariable("Reflectivity");
        testRadialVariable(vDM);
        for (int i = 0; i < rvars.size(); i++) {
            RadialDatasetSweep.RadialVariable rv = (RadialDatasetSweep.RadialVariable) rvars.get(i);
            testRadialVariable( rv);

          //  RadialCoordSys.makeRadialCoordSys( "desc", CoordinateSystem cs, VariableEnhanced v);
           // ucar.nc2.dt.radial.RadialCoordSys rcsys = rv.getRadialCoordSys();
        }

   }
} // LevelII2Dataset
