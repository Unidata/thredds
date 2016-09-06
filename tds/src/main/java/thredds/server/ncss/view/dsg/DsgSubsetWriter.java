/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncss.view.dsg;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.exception.VariableNotContainedInDatasetException;
import thredds.server.ncss.format.SupportedFormat;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Created by cwardgar on 2014/05/20.
 */
public abstract class DsgSubsetWriter {
    protected final FeatureDatasetPoint fdPoint;
    protected final SubsetParams ncssParams;
    protected final List<VariableSimpleIF> wantedVariables;
    protected final CalendarDateRange wantedRange;

    public DsgSubsetWriter(FeatureDatasetPoint fdPoint, SubsetParams ncssParams) throws NcssException {
        this.fdPoint = fdPoint;
        this.ncssParams = ncssParams;
        this.wantedVariables = getWantedVariables(fdPoint, ncssParams);
        this.wantedRange = getWantedDateRange(ncssParams);
    }

    abstract public HttpHeaders getHttpHeaders(String datasetPath, boolean isStream);
    abstract public void write() throws Exception;

    ////////////////////////////////////////////////////// Static //////////////////////////////////////////////////////

    public void respond(HttpServletResponse res, FeatureDataset ft, String requestPathInfo, SubsetParams queryParams,
                        SupportedFormat format) throws Exception {
        write();
    }


    // TODO: This needs testing.
    public static List<VariableSimpleIF> getWantedVariables(FeatureDatasetPoint fdPoint, SubsetParams ncssParams)
            throws VariableNotContainedInDatasetException {
        List<String> vars = ncssParams.getVariables();
        if (vars.size() == 1 && vars.get(0).equals("all")) {
            return fdPoint.getDataVariables();  // Return all variables.
        }

        // restrict to these variables
        Map<String, VariableSimpleIF> dataVarsMap = new HashMap<>();
        for (VariableSimpleIF dataVar : fdPoint.getDataVariables()) {
            dataVarsMap.put(dataVar.getShortName(), dataVar);
        }

        List<String> allVarNames = new ArrayList<>(dataVarsMap.keySet());
        List<VariableSimpleIF> wantedVars = new ArrayList<>();

        for (String varName : vars) {
            if (allVarNames.contains(varName)) {
                VariableSimpleIF var = dataVarsMap.get(varName);
                wantedVars.add(var);
            } else {
                throw new VariableNotContainedInDatasetException(
                        "Variable: " + varName + " is not contained in the requested dataset");
            }
        }

        return wantedVars;
    }

    // TODO: This needs testing.
    public static CalendarDateRange getWantedDateRange(SubsetParams ncssParams) throws NcssException {
        if (ncssParams.isTrue(SubsetParams.timePresent)) {  // present
            CalendarDate time = CalendarDate.present();
            CalendarPeriod timeWindow = ncssParams.getTimeWindow();
            if (timeWindow == null) timeWindow = CalendarPeriod.Hour;
            return CalendarDateRange.of(time.subtract(timeWindow), time.add(timeWindow));

        } else if (ncssParams.getTime() != null) {  // Means we want just one single time.
            CalendarDate time = ncssParams.getTime();
            CalendarPeriod timeWindow = ncssParams.getTimeWindow();
            if (timeWindow == null) timeWindow = CalendarPeriod.Hour;

            // To prevent features from being too agressively excluded, we are accepting times that are within
            // an hour of the specified time.
            // LOOK: Do we really need the +- increment?
            //CalendarDate startR = CalendarDate.parseISOformat(null, time);
            //startR = startR.subtract(CalendarPeriod.Hour);
            //CalendarDate endR = CalendarDate.parseISOformat(null, time);
            //endR = endR.add(CalendarPeriod.Hour);
            return CalendarDateRange.of(time.subtract(timeWindow), time.add(timeWindow));

        } else if (ncssParams.getTimeRange() != null) {
            return ncssParams.getTimeRange();

        } else if (ncssParams.isTrue(SubsetParams.timeAll )) {  // Client explicitly requested all times.
            return null;   // "null" means that we want ALL times, i.e. "do not subset".

        } else {          // Client didn't specify a time parameter.
            return null;  // Do not subset.
        }
    }
}
