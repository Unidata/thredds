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


import ucar.ma2.*;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.dt.TypedDatasetFactoryIF;
import ucar.nc2.units.DateUnit;

import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Jun 13, 2011
 * Time: 1:15:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class CFnetCDF2Dataset extends RadialDatasetSweepAdapter implements TypedDatasetFactoryIF {

    /** _more_          */
    private NetcdfDataset ds = null;

    /** _more_          */
    double latv, lonv, elev;

    /** _more_          */
    double[] time;

    /** _more_          */
    float[] elevation;

    /** _more_          */
    float[] azimuth;

    /** _more_          */
    float[] range;

    /** _more_          */
    int[] rayStartIdx;

    /** _more_          */
    int[] rayEndIdx;

    /** _more_          */
    int nsweeps;

    /////////////////////////////////////////////////
    // TypedDatasetFactoryIF

    /**
     * _more_
     *
     * @param ds _more_
     *
     * @return _more_
     */
    public boolean isMine(NetcdfDataset ds) {
        String convention = ds.findAttValueIgnoreCase(null, "Conventions",
                                null);
        if ((null != convention) && convention.startsWith("CF/Radial")) {
            return true;
        }
        return false;
    }

    /**
     * _more_
     *
     * @param ncd _more_
     * @param task _more_
     * @param errlog _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public TypedDataset open(NetcdfDataset ncd,
                             ucar.nc2.util.CancelTask task,
                             StringBuilder errlog)
            throws IOException {
        return new CFnetCDF2Dataset(ncd);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public FeatureType getScientificDataType() {
        return FeatureType.RADIAL;
    }


    /**
     * _more_
     */
    public CFnetCDF2Dataset() {}

    /**
     * Constructor.
     *
     * @param ds must be from nexrad2 IOSP
     */
    public CFnetCDF2Dataset(NetcdfDataset ds) {
        //super(ds);
        this.ds = ds;
        desc    = "CF netCDF 2 radar dataset";
        init();

        List vars = ds.getVariables();
        for (int i = 0; i < vars.size(); i++) {
            // VariableEnhanced varDS = (VariableEnhanced) vars.get(i);
            addRadialVariable(ds, (Variable) vars.get(i));
        }
    }

    /**
     * _more_
     */
    public void init() {
        Variable t       = ds.findVariable("time");
        Variable ele     = ds.findVariable("elevation");
        Variable azi     = ds.findVariable("azimuth");
        Variable rng     = ds.findVariable("range");
        Variable sidx0   = ds.findVariable("sweep_start_ray_index");
        Variable sidx1   = ds.findVariable("sweep_end_ray_index");
        Variable snumber = ds.findVariable("sweep_number");

        setEarthLocation();
        try {
            Array tArray = t.read();
            time = (double[]) tArray.copyTo1DJavaArray();
            Array eArray = ele.read();
            elevation = (float[]) eArray.copyTo1DJavaArray();
            Array aArray = azi.read();
            azimuth = (float[]) aArray.copyTo1DJavaArray();
            Array rArray = rng.read();
            range       = (float[]) rArray.copyTo1DJavaArray();
            rayStartIdx = (int[]) sidx0.read().copyTo1DJavaArray();
            rayEndIdx   = (int[]) sidx1.read().copyTo1DJavaArray();
            Array sn = snumber.read();
            nsweeps = ((int[]) sn.copyTo1DJavaArray()).length;

            setTimeUnits();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setStartDate();
        setEndDate();
        setBoundingBox();
    }


    /**
     * _more_
     */
    protected void setBoundingBox() {
        LatLonRect bb;

        if (origin == null) {
            return;
        }

        double dLat = Math.toDegrees(getMaximumRadialDist()
                                     / Earth.getRadius());
        double latRadians = Math.toRadians(origin.getLatitude());
        double dLon       = dLat * Math.cos(latRadians);

        double lat1       = origin.getLatitude() - dLat / 2;
        double lon1       = origin.getLongitude() - dLon / 2;
        bb = new LatLonRect(new LatLonPointImpl(lat1, lon1), dLat, dLon);

        boundingBox = bb;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    double getMaximumRadialDist() {
        double   maxdist = 0.0;
        Iterator iter    = dataVariables.iterator();

        while (iter.hasNext()) {
            RadialVariable rv   = (RadialVariable) iter.next();
            Sweep          sp   = rv.getSweep(0);
            double         dist = sp.getGateNumber() * sp.getGateSize();

            if (dist > maxdist) {
                maxdist = dist;
            }
        }

        return maxdist;
    }

    /**
     * _more_
     */
    protected void setEarthLocation() {

        try {
            Variable ga = ds.findVariable("latitude");
            if (ga != null) {
                latv = ga.readScalarDouble();
            } else {
                latv = 0.0;
            }

            ga = ds.findVariable("longitude");

            if (ga != null) {
                lonv = ga.readScalarDouble();
            } else {
                lonv = 0.0;
            }

            ga = ds.findVariable("altitude");
            if (ga != null) {
                elev = ga.readScalarDouble();
            } else {
                elev = 0.0;
            }
        } catch (IOException e) {}

        origin = new ucar.unidata.geoloc.EarthLocationImpl(latv, lonv, elev);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ucar.unidata.geoloc.EarthLocation getCommonOrigin() {
        return origin;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRadarID() {
        Attribute ga = ds.findGlobalAttribute("Station");
        if (ga != null) {
            return ga.getStringValue();
        } else {
            return "XXXX";
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRadarName() {
        Attribute ga = ds.findGlobalAttribute("StationName");
        if (ga != null) {
            return ga.getStringValue();
        } else {
            return "Unknown Station";
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDataFormat() {
        return "CF/RadialNetCDF";
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isVolume() {
        return true;
    }

    /**
     * _more_
     *
     * @param nds _more_
     *
     * @return _more_
     */
    public boolean isHighResolution(NetcdfDataset nds) {
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isStationary() {
        return true;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void setTimeUnits() throws Exception {
        Variable t  = ds.findVariable("time");
        String   ut = t.getUnitsString();
        dateUnits = new DateUnit(ut);
    }

    /**
     * _more_
     */
    protected void setStartDate() {
        startDate = new Date((long) time[0]);
    }

    /**
     * _more_
     */
    protected void setEndDate() {
        endDate = new Date((long) time[time.length - 1]);
    }

    /**
     * _more_
     */
    public void clearDatasetMemory() {
        List     rvars = getDataVariables();
        Iterator iter  = rvars.iterator();
        while (iter.hasNext()) {
            RadialVariable radVar = (RadialVariable) iter.next();
            radVar.clearVariableMemory();
        }
    }



    /**
     * _more_
     *
     * @param nds _more_
     * @param var _more_
     */
    protected void addRadialVariable(NetcdfDataset nds, Variable var) {

        RadialVariable rsvar = null;
        String         vName = var.getShortName();
        int            tIdx  = var.findDimensionIndex("time");
        int            rIdx  = var.findDimensionIndex("range");

        if ((tIdx == 0) && (rIdx == 1)) {
            VariableSimpleIF v = new MyRadialVariableAdapter(vName,
                                     var.getAttributes());
            rsvar = makeRadialVariable(nds, v, var);
        }

        if (rsvar != null) {
            dataVariables.add(rsvar);
        }
    }



    /**
     * _more_
     *
     * @param nds _more_
     * @param v _more_
     * @param v0 _more_
     *
     * @return _more_
     */
    protected RadialVariable makeRadialVariable(NetcdfDataset nds,
            VariableSimpleIF v, Variable v0) {
        // this function is null in level 2
        return new CFRadial2Variable(nds, v, v0);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getInfo() {
        StringBuffer sbuff = new StringBuffer();
        sbuff.append("CFRadial2Dataset\n");
        sbuff.append(super.getDetailInfo());
        sbuff.append("\n\n");
        sbuff.append(parseInfo.toString());
        return sbuff.toString();
    }


    /**
     * Class description
     *
     *
     * @version        Enter version here..., Mon, Jun 13, '11
     * @author         Enter your name here...    
     */
    private class CFRadial2Variable extends MyRadialVariableAdapter implements RadialDatasetSweep
        .RadialVariable {

        /** _more_          */
        ArrayList sweeps;

        /** _more_          */
        String name;


        /**
         * _more_
         *
         * @param nds _more_
         * @param v _more_
         * @param v0 _more_
         */
        private CFRadial2Variable(NetcdfDataset nds, VariableSimpleIF v,
                                  Variable v0) {
            super(v.getShortName(), v0.getAttributes());

            sweeps = new ArrayList();
            name   = v.getShortName();

            int[] shape  = v0.getShape();
            int   count  = v0.getRank() - 1;

            int   ngates = shape[count];
            count--;
            int nrays = shape[count];

            for (int i = 0; i < nsweeps; i++) {
                sweeps.add(new CFRadial2Sweep(v0, i, nrays, ngates,
                        rayStartIdx[i], rayEndIdx[i]));
            }
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return name;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getNumSweeps() {
            return nsweeps;
        }

        /**
         * _more_
         *
         * @param sweepNo _more_
         *
         * @return _more_
         */
        public Sweep getSweep(int sweepNo) {
            return (Sweep) sweeps.get(sweepNo);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getNumRadials() {
            return azimuth.length;
        }

        // a 3D array nsweep * nradials * ngates
        // if high resolution data, it will be transfered to the same dimension

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public float[] readAllData() throws IOException {
            Array    allData;
            Array    hrData    = null;
            Sweep    spn       = (Sweep) sweeps.get(0);
            Variable v         = spn.getsweepVar();

            int minRadial = getMinRadialNumber();
            int minRadials = minRadial * nsweeps;
            int radials = getNumRadials();
            int gates = range.length;
            try {
                allData = v.read();
            } catch (IOException e) {
                throw new IOException(e.getMessage());
            }
            if(minRadials == radials) {
                return (float []) allData.get1DJavaArray(float.class);
            } else {

                float[] fa0 = (float[]) allData.get1DJavaArray(float.class);
                float[] fa  = new float[minRadials*gates*nsweeps];
                int pos = 0;
                for (int i = 0; i < nsweeps; i++) {

                    int startIdx = rayStartIdx[i];
                   // int endIdx = rayEndIdx[i];
                    int len = (minRadial) * gates;
                    System.arraycopy(fa0, startIdx*gates, fa, pos, len);
                    pos = pos + len;
                }                
                return fa;
            }
        }


        public int getMinRadialNumber(){
            int minRadialNumber = 1000;
            for (int i = 0; i < nsweeps; i++) {
                Sweep swp = (Sweep)this.sweeps.get(i);
                int radialNumber = swp.getRadialNumber();
                if(radialNumber < minRadialNumber){
                    minRadialNumber = radialNumber;
                }
            }

            return minRadialNumber;
        }
        /**
         * _more_
         */
        public void clearVariableMemory() {
            for (int i = 0; i < nsweeps; i++) {}
        }


        //////////////////////////////////////////////////////////////////////
        // Checking all azi to make sure there is no missing data at sweep
        // level, since the coordinate is 1D at this level, this checking also
        // remove those missing radials within a sweep.

        /**
         * Class description
         *
         *
         * @version        Enter version here..., Mon, Jun 13, '11
         * @author         Enter your name here...    
         */
        private class CFRadial2Sweep implements RadialDatasetSweep.Sweep {

            /** _more_          */
            double meanElevation = Double.NaN;

            /** _more_          */
            double meanAzimuth = Double.NaN;

            /** _more_          */
            int nrays, ngates;

            /** _more_          */
            public int startIdx, endIdx;

            /** _more_          */
            int sweepno;

            /** _more_          */
            Variable sweepVar;


            /**
             * _more_
             *
             * @param v _more_
             * @param sweepno _more_
             * @param rays _more_
             * @param gates _more_
             * @param startIdx _more_
             * @param endIdx _more_
             */
            CFRadial2Sweep(Variable v, int sweepno, int rays, int gates,
                           int startIdx, int endIdx) {
                this.sweepVar = v;
                this.sweepno  = sweepno;
                this.nrays    = rays;
                this.ngates   = gates;
                this.startIdx = startIdx;
                this.endIdx   = endIdx;

            }

            public int getStartIdx(){
                return startIdx;
            }

            public int getEndIdx(){
                return endIdx;
            }
            /**
             * _more_
             *
             * @return _more_
             */
            public Variable getsweepVar() {
                return sweepVar;
            }

            /* read 2d sweep data nradials * ngates */

            /**
             * _more_
             *
             * @return _more_
             *
             * @throws java.io.IOException _more_
             */
            public float[] readData() throws java.io.IOException {
                return sweepData();
            }



            /**
             * _more_
             *
             * @return _more_
             */
            private float[] sweepData() {
                int[] shape    = sweepVar.getShape();
                int[] origin   = new int[2];
                Array sweepTmp = null;

                // init section
                origin[0] = startIdx;
                shape[0]  = endIdx - startIdx;

                try {
                    sweepTmp = sweepVar.read(origin, shape).reduce();
                } catch (ucar.ma2.InvalidRangeException e) {
                    e.printStackTrace();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
                return (float[]) sweepTmp.get1DJavaArray(Float.TYPE);
            }

            //  private Object MUTEX =new Object();
            /* read 1d data ngates */

            /**
             * _more_
             *
             * @param ray _more_
             *
             * @return _more_
             *
             * @throws java.io.IOException _more_
             */
            public float[] readData(int ray) throws java.io.IOException {
                return rayData(ray);
            }

            /* read the radial data from the radial variable */

            /**
             * _more_
             *
             * @param ray _more_
             *
             * @return _more_
             *
             * @throws java.io.IOException _more_
             */
            public float[] rayData(int ray) throws java.io.IOException {
                int[] shape    = sweepVar.getShape();
                int[] origin   = new int[2];
                Array sweepTmp = null;

                // init section
                origin[0] = startIdx + ray;
                shape[0]  = 1;

                try {
                    sweepTmp = sweepVar.read(origin, shape).reduce();
                } catch (ucar.ma2.InvalidRangeException e) {
                    e.printStackTrace();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }

                return (float[]) sweepTmp.get1DJavaArray(Float.TYPE);
            }


            /**
             * _more_
             */
            public void setMeanElevation() {

                double sum     = 0.0;
                int    sumSize = 0;
                int    size    = endIdx - startIdx;
                for (int i = 0; i < size; i++) {
                    if ( !Double.isNaN(elevation[i])) {
                        sum = sum + elevation[startIdx + i];
                        sumSize++;
                    }
                }
                meanElevation = sum / sumSize;
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public float getMeanElevation() {
                if (Double.isNaN(meanElevation)) {
                    setMeanElevation();
                }
                return (float) meanElevation;
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public int getGateNumber() {
                return ngates;
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public int getRadialNumber() {
                return endIdx - startIdx;
            }


            /**
             * _more_
             *
             * @return _more_
             */
            public RadialDatasetSweep.Type getType() {
                return null;
            }

            /**
             * _more_
             *
             * @param ray _more_
             *
             * @return _more_
             */
            public ucar.unidata.geoloc.EarthLocation getOrigin(int ray) {
                return origin;
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public Date getStartingTime() {
                return startDate;
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public Date getEndingTime() {
                return endDate;
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public int getSweepIndex() {
                return sweepno;
            }


            /**
             * _more_
             */
            public void setMeanAzimuth() {
                double sum     = 0.0;
                int    sumSize = 0;
                int    size    = endIdx - startIdx;
                for (int i = 0; i < size; i++) {
                    if ( !Double.isNaN(azimuth[i])) {
                        sum = sum + azimuth[startIdx + i];
                        sumSize++;
                    }
                }
                meanAzimuth = sum / sumSize;

            }

            /**
             * _more_
             *
             * @return _more_
             */
            public float getMeanAzimuth() {
                if (Double.isNaN(meanAzimuth)) {
                    setMeanAzimuth();
                }
                return (float) meanAzimuth;
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public boolean isConic() {
                return true;
            }

            /**
             * _more_
             *
             * @param ray _more_
             *
             * @return _more_
             *
             * @throws IOException _more_
             */
            public float getElevation(int ray) throws IOException {
                return (float) elevation[ray + startIdx];
            }

            /**
             * _more_
             *
             * @return _more_
             *
             * @throws IOException _more_
             */
            public float[] getElevation() throws IOException {
                int     size = endIdx - startIdx;
                float[] elev = new float[size];

                for (int i = startIdx; i < endIdx; i++) {
                    elev[i - startIdx] = (float) elevation[i];
                }
                return elev;

            }


            /**
             * _more_
             *
             * @return _more_
             *
             * @throws IOException _more_
             */
            public float[] getAzimuth() throws IOException {
                int     size  = endIdx - startIdx;
                float[] azimu = new float[size];

                for (int i = startIdx; i < endIdx; i++) {
                    azimu[i - startIdx] = (float) azimuth[i];
                }
                return azimu;
            }


            /**
             * _more_
             *
             * @param ray _more_
             *
             * @return _more_
             *
             * @throws IOException _more_
             */
            public float getAzimuth(int ray) throws IOException {
                return azimuth[ray + startIdx];
            }


            /**
             * _more_
             *
             * @param gate _more_
             *
             * @return _more_
             *
             * @throws IOException _more_
             */
            public float getRadialDistance(int gate) throws IOException {
                return range[gate];
            }

            /**
             * _more_
             *
             * @param ray _more_
             *
             * @return _more_
             *
             * @throws IOException _more_
             */
            public float getTime(int ray) throws IOException {

                return (float) time[ray + startIdx];

            }

            /**
             * _more_
             *
             * @return _more_
             */
            public float getBeamWidth() {
                return 0.95f;  // degrees, info from Chris Burkhart
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public float getNyquistFrequency() {
                return 0;  // LOOK this may be radial specific
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public float getRangeToFirstGate() {
                try {
                    return getRadialDistance(0);
                } catch (IOException e) {
                    e.printStackTrace();
                    return 0.0f;
                }
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public float getGateSize() {
                try {
                    return getRadialDistance(1) - getRadialDistance(0);
                } catch (IOException e) {
                    e.printStackTrace();
                    return 0.0f;
                }
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public boolean isGateSizeConstant() {
                return true;
            }

            /**
             * _more_
             */
            public void clearSweepMemory() {}
        }  // LevelII2Sweep class

    }      // LevelII2Variable


    /**
     * _more_
     *
     * @param rv _more_
     *
     * @throws IOException _more_
     */
    private static void testRadialVariable(
            RadialDatasetSweep.RadialVariable rv)
            throws IOException {
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

            System.out.println("*** radar Sweep mean elevation of sweep " + i
                               + " is: " + me);
            int     nrays = sw.getRadialNumber();
            float[] az    = new float[nrays];
            for (int j = 0; j < nrays; j++) {
                float azi = sw.getAzimuth(j);
                az[j] = azi;
            }
            float[] azz = sw.getAzimuth();
            float[] dat = sw.readData();
            // System.out.println("*** radar Sweep mean elevation of sweep " + i + " is: " + me);
        }
        sw = rv.getSweep(0);
        //ucar.unidata.util.Trace.call1("LevelII2Dataset:testRadialVariable readData");
        float[] data = rv.readAllData();
        float[] ddd  = sw.readData();
        float[] da   = sw.getAzimuth();
        float[] de   = sw.getElevation();
        //ucar.unidata.util.Trace.call2("LevelII2Dataset:testRadialVariable readData");
        assert (null != ddd);
        int     nrays = sw.getRadialNumber();
        float[] az    = new float[nrays];
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


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     * @throws IOException _more_
     * @throws IllegalAccessException _more_
     * @throws InstantiationException _more_
     */
    public static void main(String args[])
            throws Exception, IOException, InstantiationException,
                   IllegalAccessException {
        // String fileIn = "/home/yuanho/Download/KCLX_20091019_2021";
        String fileIn = "C:/Users/yuanho/Desktop/idvData/cfrad.20080604_002217_000_SPOL_v36_SUR.nc";
           // "C:/Users/yuanho/Downloads/Level2_KCBW_20110307_2351.ar2v";

        RadialDatasetSweep rds =
            (RadialDatasetSweep) TypedDatasetFactory.open(FeatureType.RADIAL,
                fileIn, null, new StringBuilder());
        //ucar.unidata.util.Trace.call2("LevelII2Dataset:main dataset");
        String st   = rds.getStartDate().toString();
        String et   = rds.getEndDate().toString();
        String id   = rds.getRadarID();
        String name = rds.getRadarName();
        if (rds.isStationary()) {
            System.out.println("*** radar is stationary with name and id: "
                               + name + " " + id);
        }
        List rvars = rds.getDataVariables();
        RadialDatasetSweep.RadialVariable vDM =
            (RadialDatasetSweep.RadialVariable) rds.getDataVariable(
                "DBZ");
        float[] adata = vDM.readAllData();
        testRadialVariable(vDM);
        for (int i = 0; i < rvars.size(); i++) {
            RadialDatasetSweep.RadialVariable rv =
                (RadialDatasetSweep.RadialVariable) rvars.get(i);
            //   testRadialVariable(rv);

            //  RadialCoordSys.makeRadialCoordSys( "desc", CoordinateSystem cs, VariableEnhanced v);
            // ucar.nc2.dt.radial.RadialCoordSys rcsys = rv.getRadialCoordSys();
        }

    }
}
