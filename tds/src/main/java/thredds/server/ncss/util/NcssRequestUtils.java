/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.ncss.util;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import thredds.server.config.TdsContext;
import thredds.server.ncss.exception.OutOfBoundariesException;
import thredds.server.ncss.exception.TimeOutOfWindowException;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.vertical.VerticalTransform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public final class NcssRequestUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    private NcssRequestUtils() {

    }

    public static GridAsPointDataset buildGridAsPointDataset(GridDataset gds, List<String> vars) {

        List<GridDatatype> grids = new ArrayList<>();
        for (String gridName : vars) {
            GridDatatype grid = gds.findGridDatatype(gridName);

            if (grid == null) {
                continue;
            }
            grids.add(grid);
        }
        return new GridAsPointDataset(grids);
    }

    public static List<String> getAllVarsAsList(GridDataset gds) {
        List<String> vars = new ArrayList<>();

        List<VariableSimpleIF> allVars = gds.getDataVariables();
        for (VariableSimpleIF var : allVars) {
            vars.add(var.getShortName());
        }

        return vars;
    }


    public static List<CalendarDate> wantedDates(GridAsPointDataset gap, CalendarDateRange dates,
            long timeWindow) throws TimeOutOfWindowException, OutOfBoundariesException {

        CalendarDate start = dates.getStart();
        CalendarDate end = dates.getEnd();


        List<CalendarDate> gdsDates = gap.getDates();

        if (start.isAfter(gdsDates.get(gdsDates.size() - 1)) || end.isBefore(gdsDates.get(0))) {
            throw new OutOfBoundariesException("Requested time range does not intersect the Data Time Range = " +
                    gdsDates.get(0) + " to " + gdsDates.get(gdsDates.size() - 1));
        }

        List<CalendarDate> wantDates = new ArrayList<>();

        if (dates.isPoint()) {
            int best_index = 0;
            long best_diff = Long.MAX_VALUE;
            for (int i = 0; i < gdsDates.size(); i++) {
                CalendarDate date = gdsDates.get(i);
                long diff = Math.abs(date.getDifferenceInMsecs(start));
                if (diff < best_diff) {
                    best_index = i;
                    best_diff = diff;
                }
            }
            if (timeWindow > 0 && best_diff > timeWindow) //Best time is out of our acceptable timeWindow
            {
                throw new TimeOutOfWindowException("There is not time within the provided time window");
            }


            wantDates.add(gdsDates.get(best_index));
        } else {
            for (CalendarDate date : gdsDates) {
                boolean tooEarly = date.isBefore(start);
                boolean tooLate = date.isAfter(end);
                if (tooEarly || tooLate) {
                    continue;
                }
                wantDates.add(date);
            }
        }
        return wantDates;
    }


    public static List<VariableSimpleIF> wantedVars2VariableSimple(List<String> wantedVars, GridDataset gds,
            NetcdfDataset ncfile) {

        // need VariableSimpleIF for each variable
        List<VariableSimpleIF> varList = new ArrayList<>(wantedVars.size());

        //And wantedVars must be in the dataset
        for (String var : wantedVars) {
            VariableEnhanced ve = gds.findGridDatatype(var).getVariable();

            //List<Dimension> lDims =ve.getDimensions();
            //StringBuilder dims = new StringBuilder("");
            //for(Dimension d: lDims){
            //	dims.append(" ").append(d.getName());
            //}
            String dims = ""; // always scalar ????

            VariableSimpleIF want = new VariableDS(ncfile, null, null, ve.getShortName(), ve.getDataType(), dims,
                    ve.getUnitsString(), ve.getDescription());

            varList.add(want);
        }

        return varList;
    }


    public static GridDatatype getTimeGrid(Map<String, List<String>> groupedVars, GridDataset gridDataset) {

        List<String> keys = new ArrayList<>(groupedVars.keySet());
        GridDatatype timeGrid = null;
        List<String> allVars = new ArrayList<>();
        for (String key : keys) {
            allVars.addAll(groupedVars.get(key));
        }

        Iterator<String> it = allVars.iterator();

        while (timeGrid == null && it.hasNext()) {
            String var = it.next();
            if (gridDataset.findGridDatatype(var).getCoordinateSystem().hasTimeAxis()) {
                timeGrid = gridDataset.findGridDatatype(var);
            }
        }
        ///
        return timeGrid;
    }

    public static Double getTimeCoordValue(GridDatatype grid, CalendarDate date, CalendarDate origin) {

        CoordinateAxis1DTime tAxis = grid.getCoordinateSystem().getTimeAxis1D();

        if (tAxis == null) {
            return -1.0;
        }

        Integer wIndex = tAxis.findTimeIndexFromCalendarDate(date);
        Double coordVal;

        //Check axis dataType --> Time axis for some collections (joinExistingOne) is String
        //In that case we use the seconds since the origin of the time axis as unit
        if (tAxis.getDataType() == DataType.STRING) {
            CalendarDate wanted = tAxis.getCalendarDate(wIndex);
            coordVal = (double) wanted.getDifferenceInMsecs(origin) / 1000;

        } else {
            coordVal = tAxis.getCoordValue(wIndex);
        }

        return coordVal;
    }

    public static Double getTargetLevelForVertCoord(CoordinateAxis1D zAxis, Double vertLevel) {

        Double targetLevel = vertLevel;
        int coordLevel;
        // If zAxis has one level zAxis.findCoordElement(vertLevel) returns -1 and only works with vertLevel = 0
        // Workaround while not fixed in CoordinateAxis1D
        if (zAxis.getSize() == 1) {
            targetLevel = 0.0;
        } else {
            //coordLevel = zAxis.findCoordElement(vertLevel);
            coordLevel = zAxis.findCoordElementBounded(vertLevel);

            if (coordLevel > 0) {
                targetLevel = zAxis.getCoordValue(coordLevel);
            }
        }

        return targetLevel;
    }

    /**
     * Returns the actual vertical level if the grid has vertical transformation or -9999.9 otherwise
     */
    public static double getActualVertLevel(GridDatatype grid, CalendarDate date, LatLonPoint point,
            double targetLevel) throws IOException, InvalidRangeException {

        double actualLevel = -9999.9;

        //Check vertical transformations for the grid
        GridCoordSystem cs = grid.getCoordinateSystem();
        VerticalTransform vt = cs.getVerticalTransform();

        if (vt != null) {
            int[] result = new int[2];
            cs.findXYindexFromLatLon(point.getLatitude(), point.getLongitude(), result);
            CoordinateAxis1DTime timeAxis = cs.getTimeAxis1D();
            int vertCoord = cs.getVerticalAxis().findCoordElement(targetLevel);

            int timeIndex = 0;
            if (timeAxis != null) {
                timeIndex = timeAxis.findTimeIndexFromCalendarDate(date);
            }//If null timAxis might be 2D -> not supported (handle this)

            ArrayDouble.D1 actualLevels = vt.getCoordinateArray1D(timeIndex, result[0], result[1]);
            actualLevel = actualLevels.get(vertCoord);
        }

        return actualLevel;
    }

    /**
     * Makes the TdsContext available
     *
     * @return TdsContext
     */
    public static TdsContext getTdsContext() {
        return applicationContext.getBean(TdsContext.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        NcssRequestUtils.applicationContext = applicationContext;
    }

    public static String getFileNameForResponse(String pathInfo, NetcdfFileWriter.Version version) {
        Preconditions.checkNotNull(version, "version == null");
        return getFileNameForResponse(pathInfo, version.getSuffix());
    }

    public static String getFileNameForResponse(String pathInfo, String extension) {
        Preconditions.checkArgument(pathInfo != null && !pathInfo.isEmpty(), "pathInfo == %s", pathInfo);
        Preconditions.checkNotNull(extension, "extension == null");

        String parentDirName = FilenameUtils.getBaseName(FilenameUtils.getPathNoEndSeparator(pathInfo));
        String baseName = FilenameUtils.getBaseName(pathInfo);
        String dotExtension = extension.startsWith(".") ? extension : "." + extension;

        if (!parentDirName.isEmpty()) {
            return parentDirName + "_" + baseName + dotExtension;
        } else {
            return baseName + dotExtension;
        }
    }
}
