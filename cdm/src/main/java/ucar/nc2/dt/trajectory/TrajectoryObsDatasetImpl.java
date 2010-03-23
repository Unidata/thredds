/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.dt.trajectory;


import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.StructurePseudo;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Jul 14, 2009
 * Time: 1:30:40 PM
 * To change this template use File | Settings | File Templates.
 * @deprecated use ucar.nc2.ft.*
 */
public abstract class TrajectoryObsDatasetImpl extends TypedDatasetImpl implements TrajectoryObsDataset {

    /** _more_          */
    protected String trajectoryId;  // the Id String for the trajectory.

    /** _more_          */
    protected int trajectoryNumPoint;  // the num of points in the trajectory.

    /** _more_          */
    protected HashMap trajectoryVarsMap;  // a Map of all the TypedDataVariables in the trajectory keyed by their names.

    /** _more_          */
    protected Dimension trajectoryDim;

    /** _more_          */
    protected Variable dimVar;    // the variable associated with the dimension of the trajectory

    /** _more_          */
    protected Structure recordVar;

    /** _more_          */
    protected Variable latVar;

    /** _more_          */
    protected Variable lonVar;

    /** _more_          */
    protected Variable elevVar;

    /** _more_          */
    protected String dimVarUnitsString;

    /** _more_          */
    protected double elevVarUnitsConversionFactor;

    /** _more_          */
    protected TrajectoryObsDatatype trajectory;

    /**
     * _more_
     */
    public TrajectoryObsDatasetImpl() {}

    /**
     * _more_
     *
     * @param ncfile _more_
     */
    public TrajectoryObsDatasetImpl(NetcdfDataset ncfile) {
        super(ncfile);
    }

    /**
     * Setup needed for all SingleTrajectoryObsDatatypes. Can only be called once.
     *
     * Units of time varible must be udunits time units.
     * Units of latitude variable must be convertible to "degrees_north" by udunits.
     * Units of longitude variable must be convertible to "degrees_east" by udunits.
     * Units of altitude variable must be convertible to "meters" by udunits.
     *
     *
     * @param trajConfig _more_
     * @throws IllegalArgumentException if units of time, latitude, longitude, or altitude variables are not as required.
     * @throws IllegalStateException if this method has already been called.
     *
     * @throws IOException _more_
     */
    public void setTrajectoryInfo(Config trajConfig) throws IOException {

        if (trajectoryDim != null) {
            throw new IllegalStateException(
                "The setTrajectoryInfo() method can only be called once.");
        }

        this.trajectoryId  = trajConfig.getTrajectoryId();
        this.trajectoryDim = trajConfig.getTrajectoryDim();
        this.dimVar        = trajConfig.getDimensionVar();
        this.latVar        = trajConfig.getLatVar();
        this.lonVar        = trajConfig.getLonVar();
        this.elevVar       = trajConfig.getElevVar();

        trajectoryNumPoint = this.trajectoryDim.getLength();
        dimVarUnitsString =
            this.dimVar.findAttribute("units").getStringValue();

        // Check that time, lat, lon, elev units are acceptable.
     //   if (DateUnit.getStandardDate(dimVarUnitsString) == null) {
      //      throw new IllegalArgumentException("Units of time variable <"
     //               + dimVarUnitsString + "> not a date unit.");
     //   }
        String latVarUnitsString =
            this.latVar.findAttribute("units").getStringValue();
        if ( !SimpleUnit.isCompatible(latVarUnitsString, "deg")) {
            throw new IllegalArgumentException("Units of lat var <"
                    + latVarUnitsString
                    + "> not compatible with \"degrees_north\".");
        }
        String lonVarUnitsString =
            this.lonVar.findAttribute("units").getStringValue();
        if ( !SimpleUnit.isCompatible(lonVarUnitsString, "deg")) {
            throw new IllegalArgumentException("Units of lon var <"
                    + lonVarUnitsString
                    + "> not compatible with \"degrees_east\".");
        }
        String elevVarUnitsString =
            this.elevVar.findAttribute("units").getStringValue();
        if ( !SimpleUnit.isCompatible(elevVarUnitsString, "meters")) {
            throw new IllegalArgumentException("Units of elev var <"
                    + elevVarUnitsString
                    + "> not compatible with \"meters\".");
        }

        try {
            elevVarUnitsConversionFactor =
                getMetersConversionFactor(elevVarUnitsString);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Exception on getMetersConversionFactor() for the units of elev var <"
                + elevVarUnitsString + ">.");
        }

        if (this.ncfile.hasUnlimitedDimension()
                && this.ncfile.getUnlimitedDimension().equals(trajectoryDim)) {
            Object result = this.ncfile.sendIospMessage(
                                NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
            if ((result != null) && (Boolean) result) {
                this.recordVar =
                    (Structure) this.ncfile.getRootGroup().findVariable(
                        "record");
            } else {
                this.recordVar = new StructurePseudo(this.ncfile, null,
                        "record", trajectoryDim);
            }
        } else {
            this.recordVar = new StructurePseudo(this.ncfile, null, "record",
                    trajectoryDim);
        }

        // @todo HACK, HACK, HACK - remove once addRecordStructure() deals with ncd attribute changes.
        Variable elevVarInRecVar =
            this.recordVar.findVariable(this.elevVar.getName());
        if ( !elevVarUnitsString.equals(
                elevVarInRecVar.findAttribute("units").getStringValue())) {
            elevVarInRecVar.addAttribute(new Attribute("units",
                    elevVarUnitsString));
        }

        trajectoryVarsMap = new HashMap();
        //for ( Iterator it = this.recordVar.getVariables().iterator(); it.hasNext(); )
        for (Iterator it =
                this.ncfile.getRootGroup().getVariables().iterator();
                it.hasNext(); ) {
            Variable curVar = (Variable) it.next();
            if ((curVar.getRank() > 0) && !curVar.equals(this.dimVar)
                    && !curVar.equals(this.latVar)
                    && !curVar.equals(this.lonVar)
                    && !curVar.equals(this.elevVar)
                    && ((this.recordVar == null)
                        ? true
                        : !curVar.equals(this.recordVar))) {
                MyTypedDataVariable typedVar =
                    new MyTypedDataVariable(new VariableDS(null, curVar,
                        true));
                dataVariables.add(typedVar);
                trajectoryVarsMap.put(typedVar.getName(), typedVar);
            }
        }

        trajectory = new Trajectory(this.trajectoryId,
                                          trajectoryNumPoint, this.dimVar,
                                          dimVarUnitsString, this.latVar,
                                          this.lonVar, this.elevVar,
                                          dataVariables, trajectoryVarsMap);

        startDate = getStartDate();
        endDate   = getEndDate();

        ((Trajectory) trajectory).setStartDate(startDate);
        ((Trajectory) trajectory).setEndDate(endDate);

    }

    /**
     * _more_
     *
     * @param unitsString _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected static double getMetersConversionFactor(
            String unitsString) throws Exception {
        SimpleUnit unit = SimpleUnit.factoryWithExceptions(unitsString);
        return unit.convertTo(1.0, SimpleUnit.meterUnit);
    }

    /**
     * _more_
     */
  //  Date getStartDate() {}

    /**
     * _more_
     */
  //  protected void setEndDate() {}

    /**
     * _more_
     */
    protected void setBoundingBox() {}

    /**
     * _more_
     *
     * @return _more_
     */
    public List getTrajectoryIds() {
        List l = new ArrayList();
        l.add(trajectoryId);
        return (l);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getTrajectories()  // throws IOException;
    {
        List l = new ArrayList();
        l.add(trajectory);
        return (l);
    }

    /**
     * _more_
     *
     * @param trajectoryId _more_
     *
     * @return _more_
     */
    public TrajectoryObsDatatype getTrajectory(String trajectoryId)  // throws IOException;
    {
        if ( !trajectoryId.equals(this.trajectoryId)) {
            return (null);
        }
        return (trajectory);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDetailInfo() {
        StringBuffer sbuff = new StringBuffer();
        sbuff.append("TrajectoryObsDataset\n");
        sbuff.append("  adapter   = " + getClass().getName() + "\n");
        sbuff.append("  trajectories:" + "\n");
        for (Iterator it =
                this.getTrajectoryIds().iterator(); it.hasNext(); ) {
            sbuff.append("      " + (String) it.next() + "\n");
        }
        sbuff.append(super.getDetailInfo());

        return sbuff.toString();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean syncExtend() {
        if ( !this.ncfile.hasUnlimitedDimension()) {
            return false;
        }
        try {
            if ( !this.ncfile.syncExtend()) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        // Update number of points in this TrajectoryObsDataset and in the child TrajectoryObsDatatype.
        int newNumPoints = this.trajectoryDim.getLength();
        if (this.trajectoryNumPoint >= newNumPoints) {
            return false;
        }
        this.trajectoryNumPoint = newNumPoints;
        ((Trajectory) this.trajectory).setNumPoints(
            this.trajectoryNumPoint);

        // Update end date in this TrajectoryObsDataset and in the child TrajectoryObsDatatype.
        try {
            endDate = trajectory.getTime(trajectoryNumPoint - 1);
        } catch (IOException e) {
            return false;
        }
        ((Trajectory) trajectory).setEndDate(endDate);

        return true;
    }

    /**
     * Class Config _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.1 $
     */
    public static class Config {

        /** _more_          */
        protected String trajectoryId;  // the Id String for the trajectory.

        /** _more_          */
        protected Dimension trajectoryDim;

        /** _more_          */
        protected Variable dimVar;

        /** _more_          */
        protected Variable latVar;

        /** _more_          */
        protected Variable lonVar;

        /** _more_          */
        protected Variable elevVar;

        /**
         * _more_
         */
        public Config() {}

        /**
         * _more_
         *
         * @param trajectoryId _more_
         * @param trajectoryDim _more_
         * @param dimVar _more_
         * @param latVar _more_
         * @param lonVar _more_
         * @param elevVar _more_
         */
        public Config(String trajectoryId, Dimension trajectoryDim,
                      Variable dimVar, Variable latVar, Variable lonVar,
                      Variable elevVar) {
            this.trajectoryId = trajectoryId;
            this.trajectoryDim       = trajectoryDim;
            this.dimVar       = dimVar;
            this.latVar       = latVar;
            this.lonVar       = lonVar;
            this.elevVar      = elevVar;
        }

        /**
         * _more_
         *
         * @param trajectoryId _more_
         */
        public void setTrajectoryId(String trajectoryId) {
            this.trajectoryId = trajectoryId;
        }

        /**
         * _more_
         *
         * @param trajectoryDim _more_
         */
        public void setTrajectoryDim(Dimension trajectoryDim) {
            this.trajectoryDim = trajectoryDim;
        }

        /**
         * _more_
         *
         * @param dimVar _more_
         */
        public void setDimensionVar(Variable dimVar) {
            this.dimVar = dimVar;
        }

        /**
         * _more_
         *
         * @param latVar _more_
         */
        public void setLatVar(Variable latVar) {
            this.latVar = latVar;
        }

        /**
         * _more_
         *
         * @param lonVar _more_
         */
        public void setLonVar(Variable lonVar) {
            this.lonVar = lonVar;
        }

        /**
         * _more_
         *
         * @param elevVar _more_
         */
        public void setElevVar(Variable elevVar) {
            this.elevVar = elevVar;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getTrajectoryId() {
            return trajectoryId;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Dimension getTrajectoryDim() {
            return trajectoryDim;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Variable getDimensionVar() {
            return dimVar;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public Variable getLatVar() {
            return latVar;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Variable getLonVar() {
            return lonVar;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Variable getElevVar() {
            return elevVar;
        }
    }

    /**
     * Class SingleTrajectory _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.1 $
     */
    private class Trajectory implements TrajectoryObsDatatype {

        /** _more_          */
        private String id;

        /** _more_          */
        private String description;

        /** _more_          */
        private int numPoints;

        /** _more_          */
        private Date startDate;

        /** _more_          */
        private Date endDate;

        /** _more_          */
        private String dimVarUnitsString;

        /** _more_          */
        private Variable dimVar;

        /** _more_          */
        private Variable latVar;

        /** _more_          */
        private Variable lonVar;

        /** _more_          */
        private Variable elevVar;

        /** _more_          */
        private List variables;

        /** _more_          */
        private HashMap variablesMap;

        //private Structure struct;


        /**
         * _more_
         *
         * @param id _more_
         * @param numPoints _more_
         * @param dimVar _more_
         * @param dimVarUnitsString _more_
         * @param latVar _more_
         * @param lonVar _more_
         * @param elevVar _more_
         * @param variables _more_
         * @param variablesMap _more_
         */
        private Trajectory(String id, int numPoints, Variable dimVar,
                                 String dimVarUnitsString, Variable latVar,
                                 Variable lonVar, Variable elevVar,
                                 List variables, HashMap variablesMap) {
            this.description        = null;
            this.id                 = id;
            this.numPoints          = numPoints;
            this.dimVarUnitsString = dimVarUnitsString;
            this.dimVar            = dimVar;
            this.variables          = variables;
            this.variablesMap       = variablesMap;
            this.latVar             = latVar;
            this.lonVar             = lonVar;
            this.elevVar            = elevVar;

            //this.struct = new Structure( ncfile, ncfile.getRootGroup(), null, "struct for building StructureDatas");
        }

        /**
         * _more_
         *
         * @param numPoints _more_
         */
        protected void setNumPoints(int numPoints) {
            this.numPoints = numPoints;
        }

        /**
         * _more_
         *
         * @param startDate _more_
         */
        protected void setStartDate(Date startDate) {
            if (this.startDate != null) {
                throw new IllegalStateException(
                    "Can only call setStartDate() once.");
            }
            this.startDate = startDate;
        }

        /**
         * _more_
         *
         * @param endDate _more_
         */
        protected void setEndDate(Date endDate) {
            //if ( this.endDate != null ) throw new IllegalStateException( "Can only call setEndDate() once.");
            this.endDate = endDate;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getId() {
            return (id);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getDescription() {
            return (description);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getNumberPoints() {
            return (numPoints);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public List getDataVariables() {
            return (variables);
        }

        /**
         * _more_
         *
         * @param name _more_
         *
         * @return _more_
         */
        public VariableSimpleIF getDataVariable(String name) {
            return ((VariableSimpleIF) variablesMap.get(name));
        }

        /**
         * _more_
         *
         * @param point _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public PointObsDatatype getPointObsData(
                int point) throws IOException {
            return (new MyPointObsDatatype(point));
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Date getStartDate() {
            return (startDate);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Date getEndDate() {
            return (endDate);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public LatLonRect getBoundingBox() {
            return null;
        }

        /**
         * _more_
         *
         * @param point _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public Date getTime(int point) throws IOException {
            if(SimpleUnit.isDateUnit( dimVarUnitsString))
                return (DateUnit.getStandardDate(getTimeValue(point) + " "
                                             + dimVarUnitsString));
            else
                return startDate;
        }

        /**
         * _more_
         *
         * @param point _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public ucar.unidata.geoloc.EarthLocation getLocation(
                int point) throws IOException {
            return (new MyEarthLocation(point));
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getTimeUnitsIdentifier() {
            return (dimVarUnitsString);
        }

        /**
         * _more_
         *
         * @param point _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public double getTimeValue(int point) throws IOException {
            Array array = null;
            try {
                array = getTime(this.getPointRange(point));
            } catch (InvalidRangeException e) {
                IllegalArgumentException iae =
                    new IllegalArgumentException("Point <" + point
                        + "> not in valid range <0, "
                        + (this.getNumberPoints() - 1) + ">: "
                        + e.getMessage());
                iae.initCause(e);
                throw iae;
            }
            if (array instanceof ArrayDouble) {
                return (array.getDouble(array.getIndex()));
            } else if (array instanceof ArrayFloat) {
                return (array.getFloat(array.getIndex()));
            } else if (array instanceof ArrayInt) {
                return (array.getInt(array.getIndex()));
            } else {
                throw new IOException(
                    "Time variable not float, double, or integer <"
                    + array.getElementType().toString() + ">.");
            }
        }

        // @todo Make sure units are degrees_north

        /**
         * _more_
         *
         * @param point _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public double getLatitude(int point) throws IOException  // required, units degrees_north
        {
            Array array = null;
            try {
                array = getLatitude(this.getPointRange(point));
            } catch (InvalidRangeException e) {
                IllegalArgumentException iae =
                    new IllegalArgumentException("Point <" + point
                        + "> not in valid range <0, "
                        + (this.getNumberPoints() - 1) + ">: "
                        + e.getMessage());
                iae.initCause(e);
                throw iae;
            }
            if (array instanceof ArrayDouble) {
                return (array.getDouble(array.getIndex()));
            } else if (array instanceof ArrayFloat) {
                return (array.getFloat(array.getIndex()));
            } else {
                throw new IOException(
                    "Latitude variable not float or double <"
                    + array.getElementType().toString() + ">.");
            }
        }

        // @todo Make sure units are degrees_east

        /**
         * _more_
         *
         * @param point _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public double getLongitude(int point) throws IOException  // required, units degrees_east
        {
            Array array = null;
            try {
                array = getLongitude(this.getPointRange(point));
            } catch (InvalidRangeException e) {
                IllegalArgumentException iae =
                    new IllegalArgumentException("Point <" + point
                        + "> not in valid range <0, "
                        + (this.getNumberPoints() - 1) + ">: "
                        + e.getMessage());
                iae.initCause(e);
                throw iae;
            }
            if (array instanceof ArrayDouble) {
                return (array.getDouble(array.getIndex()));
            } else if (array instanceof ArrayFloat) {
                return (array.getFloat(array.getIndex()));
            } else {
                throw new IOException(
                    "Longitude variable not float or double <"
                    + array.getElementType().toString() + ">.");
            }
        }

        // @todo Make sure units are meters

        /**
         * _more_
         *
         * @param point _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public double getElevation(int point) throws IOException  // optional; units meters;  missing = NaN.
        {
            Array array = null;
            try {
                array = getElevation(this.getPointRange(point));
            } catch (InvalidRangeException e) {
                IllegalArgumentException iae =
                    new IllegalArgumentException("Point <" + point
                        + "> not in valid range <0, "
                        + (this.getNumberPoints() - 1) + ">: "
                        + e.getMessage());
                iae.initCause(e);
                throw iae;
            }
            if (array instanceof ArrayDouble) {
                return array.getDouble(array.getIndex());
            } else if (array instanceof ArrayFloat) {
                return array.getFloat(array.getIndex());
            } else {
                throw new IOException(
                    "Elevation variable not float or double <"
                    + array.getElementType().toString() + ">.");
            }
        }

        /**
         * _more_
         *
         * @param point _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         * @throws InvalidRangeException _more_
         */
        public StructureData getData(int point) throws IOException,
                InvalidRangeException {
            return TrajectoryObsDatasetImpl.this.recordVar.readStructure(point);
        }

        /**
         * _more_
         *
         * @param point _more_
         * @param parameterName _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public Array getData(int point,
                             String parameterName) throws IOException {
            try {
                return (getData(this.getPointRange(point), parameterName));
            } catch (InvalidRangeException e) {
                IllegalArgumentException iae =
                    new IllegalArgumentException("Point <" + point
                        + "> not in valid range <0, "
                        + (this.getNumberPoints() - 1) + ">: "
                        + e.getMessage());
                iae.initCause(e);
                throw iae;
            }
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Range getFullRange() {
            Range range = null;
            try {
                range = new Range(0, this.getNumberPoints() - 1);
            } catch (InvalidRangeException e) {
                IllegalStateException ise =
                    new IllegalStateException(
                        "Full trajectory range invalid <0, "
                        + (this.getNumberPoints() - 1) + ">: "
                        + e.getMessage());
                ise.initCause(e);
                throw (ise);
            }
            return range;
        }

        /**
         * _more_
         *
         * @param point _more_
         *
         * @return _more_
         *
         * @throws InvalidRangeException _more_
         */
        public Range getPointRange(int point) throws InvalidRangeException {
            if (point >= this.getNumberPoints()) {
                throw new InvalidRangeException("Point <" + point
                        + "> not in acceptible range <0, "
                        + (this.getNumberPoints() - 1) + ">.");
            }
            return (new Range(point, point));
        }

        /**
         * _more_
         *
         * @param start _more_
         * @param end _more_
         * @param stride _more_
         *
         * @return _more_
         *
         * @throws InvalidRangeException _more_
         */
        public Range getRange(int start, int end,
                              int stride) throws InvalidRangeException {
            if (end >= this.getNumberPoints()) {
                throw new InvalidRangeException("End point <" + end
                        + "> not in acceptible range <0, "
                        + (this.getNumberPoints() - 1) + ">.");
            }
            return (new Range(start, end, stride));
        }

        /**
         * _more_
         *
         * @param range _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         * @throws InvalidRangeException _more_
         */
        public Array getTime(Range range) throws IOException,
                InvalidRangeException {
            Array array = null;
            List section = new ArrayList(1);
            section.add(range);
            array = dimVar.read(section);
            if(SimpleUnit.isDateUnit( dimVarUnitsString)){
                return array;
            }

            int size = (int) array.getSize();
            double [] pa = new double[size];
            for (int i = 0; i < size; i++) {
                 pa[i] = (double)startDate.getTime();
            }

            Array ay = Array.factory(double.class, array.getShape(), pa);

            return ay;
        }

        // @todo Make sure units are degrees_north

        /**
         * _more_
         *
         * @param range _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         * @throws InvalidRangeException _more_
         */
        public Array getLatitude(Range range) throws IOException,
                InvalidRangeException {
            List section = new ArrayList(1);
            section.add(range);
            return (latVar.read(section));
        }

        // @todo Make sure units are degrees_east

        /**
         * _more_
         *
         * @param range _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         * @throws InvalidRangeException _more_
         */
        public Array getLongitude(Range range) throws IOException,
                InvalidRangeException {
            List section = new ArrayList(1);
            section.add(range);
            return (lonVar.read(section));
        }

        // @todo Make sure units are meters

        /**
         * _more_
         *
         * @param range _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         * @throws InvalidRangeException _more_
         */
        public Array getElevation(Range range) throws IOException,
                InvalidRangeException {
            List section = new ArrayList(1);
            section.add(range);
            Array a = elevVar.read(section);
            if (elevVarUnitsConversionFactor == 1.0) {
                return (a);
            }
            for (IndexIterator it = a.getIndexIterator(); it.hasNext(); ) {
                if (elevVar.getDataType() == DataType.DOUBLE) {
                    double val = it.getDoubleNext();
                    it.setDoubleCurrent(val * elevVarUnitsConversionFactor);
                } else if (elevVar.getDataType() == DataType.FLOAT) {
                    float val = it.getFloatNext();
                    it.setFloatCurrent((float) (val
                            * elevVarUnitsConversionFactor));
                } else if (elevVar.getDataType() == DataType.INT) {
                    int val = it.getIntNext();
                    it.setIntCurrent((int) (val
                                            * elevVarUnitsConversionFactor));
                } else if (elevVar.getDataType() == DataType.LONG) {
                    long val = it.getLongNext();
                    it.setLongCurrent((long) (val
                            * elevVarUnitsConversionFactor));
                } else {
                    throw new IllegalStateException(
                        "Elevation variable type <"
                        + elevVar.getDataType().toString()
                        + "> not double, float, int, or long.");
                }
            }
            return (a);
        }

        /**
         * _more_
         *
         * @param range _more_
         * @param parameterName _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         * @throws InvalidRangeException _more_
         */
        public Array getData(Range range,
                             String parameterName) throws IOException,
                                 InvalidRangeException {
            Variable variable =
                ncfile.getRootGroup().findVariable(parameterName);
            int   varRank  = variable.getRank();
            int[] varShape = variable.getShape();
            List  section  = new ArrayList(varRank);
            section.add(range);
            for (int i = 1; i < varRank; i++) {
                section.add(new Range(0, varShape[i] - 1));
            }
            Array array = variable.read(section);
            if (array.getShape()[0] == 1) {
                return (array.reduce(0));
            } else {
                return (array);
            }
            //return( array.getShape()[0] == 1 ? array.reduce( 0 ) : array);
        }

        /**
         * _more_
         *
         * @param bufferSize _more_
         *
         * @return _more_
         *
         * @throws IOException _more_
         */
        public DataIterator getDataIterator(
                int bufferSize) throws IOException {
            return new PointDatatypeIterator(recordVar, bufferSize);
        }

        /**
         * Class PointDatatypeIterator _more_
         *
         *
         * @author IDV Development Team
         * @version $Revision: 1.1 $
         */
        private class PointDatatypeIterator extends DatatypeIterator {

            /**
             * _more_
             *
             * @param recnum _more_
             * @param sdata _more_
             *
             * @return _more_
             */
            protected Object makeDatatypeWithData(int recnum,
                    StructureData sdata) {
                return new MyPointObsDatatype(recnum, sdata);
            }

            /**
             * _more_
             *
             * @param struct _more_
             * @param bufferSize _more_
             */
            PointDatatypeIterator(Structure struct, int bufferSize) {
                super(struct, bufferSize);
            }
        }


        // PointObsDatatype implementation used by SingleTrajectory.

        /**
         * Class MyPointObsDatatype _more_
         *
         *
         * @author IDV Development Team
         * @version $Revision: 1.1 $
         */
        private class MyPointObsDatatype implements PointObsDatatype {

            /** _more_          */
            private int point;

            /** _more_          */
            private StructureData sdata;

            /** _more_          */
            private double time;

            /** _more_          */
            private ucar.unidata.geoloc.EarthLocation earthLoc;

            /**
             * _more_
             *
             * @param point _more_
             *
             * @throws IOException _more_
             */
            private MyPointObsDatatype(int point) throws IOException {
                this.point    = point;
                this.time     = Trajectory.this.getTimeValue(point);
                this.earthLoc = Trajectory.this.getLocation(point);
            }

            /**
             * _more_
             *
             * @param point _more_
             * @param sdata _more_
             */
            private MyPointObsDatatype(int point, StructureData sdata) {
                this.point = point;
                this.sdata = sdata;
                this.time = sdata.convertScalarDouble(
                    Trajectory.this.dimVar.getName());
                this.earthLoc = new MyEarthLocation(sdata);
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public double getNominalTime() {
                return (this.time);
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public double getObservationTime() {
                return (this.time);
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public Date getNominalTimeAsDate() {
                String dateStr = getNominalTime() + " " + dimVarUnitsString;
                return DateUnit.getStandardDate(dateStr);
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public Date getObservationTimeAsDate() {
                String dateStr = getObservationTime() + " "
                                 + dimVarUnitsString;
                return DateUnit.getStandardDate(dateStr);
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public ucar.unidata.geoloc.EarthLocation getLocation() {
                return (this.earthLoc);
            }

            /**
             * _more_
             *
             * @return _more_
             *
             * @throws IOException _more_
             */
            public StructureData getData() throws IOException {
                if (sdata != null) {
                    return sdata;
                }
                try {
                    return (Trajectory.this.getData(point));
                } catch (InvalidRangeException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }
        }

        // EarthLocation implementation used by SingleTrajectory.

        /**
         * Class MyEarthLocation _more_
         *
         *
         * @author IDV Development Team
         * @version $Revision: 1.1 $
         */
        private class MyEarthLocation extends ucar.unidata.geoloc
            .EarthLocationImpl {

            /** _more_          */
            private double latitude;

            /** _more_          */
            private double longitude;

            /** _more_          */
            private double elevation;

            /**
             * _more_
             *
             * @param point _more_
             *
             * @throws IOException _more_
             */
            private MyEarthLocation(int point) throws IOException {
                this.latitude  = Trajectory.this.getLatitude(point);
                this.longitude = Trajectory.this.getLongitude(point);
                this.elevation = Trajectory.this.getElevation(point);
            }

            /**
             * _more_
             *
             * @param sdata _more_
             */
            private MyEarthLocation(StructureData sdata) {
                this.latitude = sdata.convertScalarDouble(
                    Trajectory.this.latVar.getName());
                this.longitude = sdata.convertScalarDouble(
                    Trajectory.this.lonVar.getName());
                this.elevation = sdata.convertScalarDouble(
                    Trajectory.this.elevVar.getName());
                if (elevVarUnitsConversionFactor != 1.0) {
                    this.elevation *= elevVarUnitsConversionFactor;
                }
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public double getLatitude() {
                return (this.latitude);
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public double getLongitude() {
                return (this.longitude);
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public double getAltitude() {
                return (this.elevation);
            }
        }
    }

    /**
     * Class MyTypedDataVariable _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.1 $
     */
    private class MyTypedDataVariable extends VariableSimpleSubclass {

        /** _more_          */
        private int rank;

        /** _more_          */
        private int[] shape;

        /**
         * _more_
         *
         * @param v _more_
         */
        private MyTypedDataVariable(VariableDS v) {
            super(v);

            // Calculate the rank and shape of the variable, removing trajectory dimesion.
            rank = super.getRank() - 1;
            int[] varShape = super.getShape();
            shape = new int[varShape.length - 1];
            int trajDimIndex =
                v.findDimensionIndex(
                    TrajectoryObsDatasetImpl.this.trajectoryDim.getName());
            for (int i = 0, j = 0; i < varShape.length; i++) {
                if (i == trajDimIndex) {
                    continue;
                }
                shape[j++] = varShape[i];
            }
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getRank() {
            return (rank);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int[] getShape() {
            return (shape);
        }
    }

}

