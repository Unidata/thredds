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
    protected final FeatureDatasetPoint fdPoint;
    protected final NcssParamsBean ncssParams;
    protected final List<VariableSimpleIF> wantedVariables;
    protected final CalendarDateRange wantedRange;

    public AbstractDsgSubsetWriter(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams) throws NcssException {
        this.fdPoint = fdPoint;
        this.ncssParams = ncssParams;
        this.wantedVariables = getWantedVariables(fdPoint, ncssParams);
        this.wantedRange = getWantedRange(ncssParams);
    }

    // TODO: This needs testing.
    public static List<VariableSimpleIF> getWantedVariables(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams)
            throws VariableNotContainedInDatasetException {
        if (ncssParams.getVar().size() == 1 && ncssParams.getVar().get(0).equals("all")) {
            return fdPoint.getDataVariables();  // Return all variables.
        }

        // restrict to these variables
        Map<String, VariableSimpleIF> dataVarsMap = new HashMap<>();
        for (VariableSimpleIF dataVar : fdPoint.getDataVariables()) {
            dataVarsMap.put(dataVar.getShortName(), dataVar);
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

    // TODO: This needs testing.
    public static CalendarDateRange getWantedRange(NcssParamsBean ncssParams) throws NcssException {
        // There are 5 temporal parameters we must examine for DSGs.
        boolean isAllTimes = ncssParams.isAllTimes();
        String time = ncssParams.getTime();
        String timeStart = ncssParams.getTime_start();
        String timeEnd = ncssParams.getTime_end();
        String timeDuration = ncssParams.getTime_duration();

        if (isAllTimes) {  // Client explicitly requested all times.
            return null;   // "null" means that we want ALL times, i.e. "do not subset".
        } else if (time != null) {  // Means we want just one single time.
            // To prevent features from being too agressively excluded, we are accepting times that are within
            // an hour of the specified time.
            // LOOK: Do we really need the +- increment?
            CalendarDate startR = CalendarDate.parseISOformat(null, time);
            startR = startR.subtract(CalendarPeriod.Hour);
            CalendarDate endR = CalendarDate.parseISOformat(null, time);
            endR = endR.add(CalendarPeriod.Hour);

            return CalendarDateRange.of(new Date(startR.getMillis()), new Date(endR.getMillis()));
        } else if (timeStart == null && timeEnd == null && timeDuration == null) {
            // Client did not set any of the time parameters, so give them everything.
            return null;  // "null" means that we want ALL times, i.e. "do not subset".
        } else if (timeStart != null && timeEnd != null) {
            CalendarDate startR = CalendarDate.parseISOformat(null, timeStart);
            CalendarDate endR = CalendarDate.parseISOformat(null, timeEnd);

            return CalendarDateRange.of(new Date(startR.getMillis()), new Date(endR.getMillis()));
        } else if (timeStart != null && timeDuration != null) {
            CalendarDate startR = CalendarDate.parseISOformat(null, timeStart);
            TimeDuration td = ncssParams.parseTimeDuration();

            return new CalendarDateRange(startR, (long) td.getValueInSeconds());
        } else if (timeEnd != null && timeDuration != null) {
            CalendarDate endR = CalendarDate.parseISOformat(null, timeEnd);
            TimeDuration td = ncssParams.parseTimeDuration();

            return new CalendarDateRange(endR, (long) td.getValueInSeconds() * (-1));
        } else {
            // We probably already handled this case upstream, but it doesn't hurt to handle it again.
            throw new NcssException("Two of \"time_start\", \"time_end\", and \"time_duration\" " +
                    "must be present to define a valid time range.");
        }
    }
}
