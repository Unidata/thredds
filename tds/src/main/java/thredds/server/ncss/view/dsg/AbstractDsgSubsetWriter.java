package thredds.server.ncss.view.dsg;

import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.exception.VariableNotContainedInDatasetException;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.units.TimeDuration;

import java.util.*;

/**
 * Created by cwardgar on 2014/05/20.
 */
public abstract class AbstractDsgSubsetWriter implements DsgSubsetWriter {
    public List<VariableSimpleIF> getWantedVariables(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams)
            throws VariableNotContainedInDatasetException {
        // restrict to these variables
        List<? extends VariableSimpleIF> dataVars = fdPoint.getDataVariables();
        Map<String, VariableSimpleIF> dataVarsMap = new HashMap<>();
        for (VariableSimpleIF v : dataVars) {
            dataVarsMap.put(v.getShortName(), v);
        }

        List<String> allVarNames = new ArrayList<>(dataVarsMap.keySet());
        List<VariableSimpleIF> wantedVars = new ArrayList<>();

        for (String varName : ncssParams.getVar()) {
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

    public CalendarDateRange getWantedRange(NcssParamsBean ncssParams) throws NcssException {
        CalendarDateRange wantedRange;

        // for closest time, set wantRange to the time LOOK - do we need +- increment ??
        if (ncssParams.getTime() != null) { //Means we want just one single time
            CalendarDate startR = CalendarDate.parseISOformat(null, ncssParams.getTime());
            startR = startR.subtract(CalendarPeriod.Hour);
            CalendarDate endR = CalendarDate.parseISOformat(null, ncssParams.getTime());
            endR = endR.add(CalendarPeriod.Hour);
            wantedRange = CalendarDateRange.of(new Date(startR.getMillis()), new Date(endR.getMillis()));
        } else {
            //Time range: need to compute the time range from the params
            if (ncssParams.isAllTimes()) {
                //Full time range -->CHECK: wantRange=null means all range???
                wantedRange = null;
            } else {
                if (ncssParams.getTime_start() != null && ncssParams.getTime_end() != null) {
                    CalendarDate startR = CalendarDate.parseISOformat(null, ncssParams.getTime_start());
                    CalendarDate endR = CalendarDate.parseISOformat(null, ncssParams.getTime_end());
                    wantedRange = CalendarDateRange.of(new Date(startR.getMillis()), new Date(endR.getMillis()));
                } else if (ncssParams.getTime_start() != null && ncssParams.getTime_duration() != null) {
                    CalendarDate startR = CalendarDate.parseISOformat(null, ncssParams.getTime_start());
                    TimeDuration td = ncssParams.parseTimeDuration();
                    wantedRange = new CalendarDateRange(startR, (long) td.getValueInSeconds());
                } else if (ncssParams.getTime_end() != null && ncssParams.getTime_duration() != null) {
                    CalendarDate endR = CalendarDate.parseISOformat(null, ncssParams.getTime_end());
                    TimeDuration td = ncssParams.parseTimeDuration();
                    wantedRange = new CalendarDateRange(endR, (long) td.getValueInSeconds() * (-1));
                } else {
                    // We may already have handled this case upstream, but it doesn't hurt to check again.
                    throw new NcssException("Two of \"time_start\", \"time_end\", and \"time_duration\" " +
                            "must be present to define a valid time range.");
                }
            }
        }

        return wantedRange;
    }
}
