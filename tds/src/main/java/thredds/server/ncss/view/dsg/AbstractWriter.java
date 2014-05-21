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

import java.io.IOException;
import java.util.*;

/**
 * superclass for DSG writers
 *
 * @author caron
 * @since 10/5/13
 */
public class AbstractWriter {

    protected FeatureDatasetPoint fd;
    protected NcssParamsBean qb;
    protected CalendarDate start, end;
    protected List<VariableSimpleIF> wantVars;
    protected CalendarDateRange wantRange;
    protected ucar.nc2.util.DiskCache2 diskCache;

    protected AbstractWriter(FeatureDatasetPoint fd, NcssParamsBean qb, ucar.nc2.util.DiskCache2 diskCache) throws IOException, NcssException {
        this.fd = fd;
        this.qb = qb;
        this.diskCache = diskCache;
        start = fd.getCalendarDateStart();
        end = fd.getCalendarDateEnd();

        // restrict to these variables
        List<? extends VariableSimpleIF> dataVars = fd.getDataVariables();
        Map<String, VariableSimpleIF> dataVarsMap = new HashMap<String, VariableSimpleIF>();
        for (VariableSimpleIF v : dataVars) {
            dataVarsMap.put(v.getShortName(), v);
        }

        List<String> varNames = qb.getVar();
        if (varNames.equals("all")) {
            wantVars = new ArrayList<VariableSimpleIF>(dataVars);
        } else {
            List<String> allVars = new ArrayList<String>(dataVarsMap.keySet());
            wantVars = new ArrayList<VariableSimpleIF>();
            for (String v : varNames) {
                if (allVars.contains(v)) {
                    VariableSimpleIF var = dataVarsMap.get(v);
                    wantVars.add(var);
                } else {
                    throw new VariableNotContainedInDatasetException("Variable: " + v + " is not contained in the requested dataset");
                }
            }
        }

        // times
        // for closest time, set wantRange to the time LOOK - do we need +- increment ??
        if (qb.getTime() != null) { //Means we want just one single time
            CalendarDate startR = CalendarDate.parseISOformat(null, qb.getTime());
            startR = startR.subtract(CalendarPeriod.Hour);
            CalendarDate endR = CalendarDate.parseISOformat(null, qb.getTime());
            endR = endR.add(CalendarPeriod.Hour);
            //wantRange = new DateRange( new Date(startR.getMillis()), new Date(endR.getMillis()));
            wantRange = CalendarDateRange.of(new Date(startR.getMillis()), new Date(endR.getMillis()));

        } else {
            //Time range: need to compute the time range from the params
            if (qb.isAllTimes()) {
                //Full time range -->CHECK: wantRange=null means all range???

            } else {
                if (qb.getTime_start() != null && qb.getTime_end() != null) {
                    CalendarDate startR = CalendarDate.parseISOformat(null, qb.getTime_start());
                    CalendarDate endR = CalendarDate.parseISOformat(null, qb.getTime_end());
                    //wantRange = new DateRange( new Date(startR.getMillis()), new Date(endR.getMillis()));;
                    wantRange = CalendarDateRange.of(new Date(startR.getMillis()), new Date(endR.getMillis()));

                } else if (qb.getTime_start() != null && qb.getTime_duration() != null) {
                    CalendarDate startR = CalendarDate.parseISOformat(null, qb.getTime_start());
                    TimeDuration td = qb.parseTimeDuration();

                    //wantRange = new DateRange( new Date(startR.getMillis()), td);
                    wantRange = new CalendarDateRange(startR, (long) td.getValueInSeconds());

                } else if (qb.getTime_end() != null && qb.getTime_duration() != null) {
                    CalendarDate endR = CalendarDate.parseISOformat(null, qb.getTime_end());
                    TimeDuration td = qb.parseTimeDuration();
                    //wantRange = new DateRange(null, new DateType( qb.getTime_end(), null, null), td, null);
                    wantRange = new CalendarDateRange(endR, (long) td.getValueInSeconds() * (-1));
                }
            }

        }

    }

}
