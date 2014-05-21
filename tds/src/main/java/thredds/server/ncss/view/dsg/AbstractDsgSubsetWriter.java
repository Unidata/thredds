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
public abstract class AbstractDsgSubsetWriter implements DsgSubsetWriter {

  protected FeatureDatasetPoint fdPoint;
  protected NcssParamsBean ncssParams;
  protected CalendarDate start, end;
  protected List<VariableSimpleIF> wantVars;
  protected CalendarDateRange wantRange;
  protected ucar.nc2.util.DiskCache2 diskCache;

  protected AbstractDsgSubsetWriter(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams, ucar.nc2.util.DiskCache2 diskCache)
          throws IOException, NcssException {
    this.fdPoint = fdPoint;
    this.ncssParams = ncssParams;
    this.diskCache = diskCache;
    start = fdPoint.getCalendarDateStart();
    end = fdPoint.getCalendarDateEnd();

    // restrict to these variables
    List<? extends VariableSimpleIF> dataVars = fdPoint.getDataVariables();
    Map<String, VariableSimpleIF> dataVarsMap = new HashMap<>();
    for (VariableSimpleIF v : dataVars) {
      dataVarsMap.put(v.getShortName(), v);
    }

    List<String> allVarNames = new ArrayList<>(dataVarsMap.keySet());
    wantVars = new ArrayList<>();

    for (String varName : ncssParams.getVar()) {
      if (allVarNames.contains(varName)) {
        VariableSimpleIF var = dataVarsMap.get(varName);
        wantVars.add(var);
      } else {
        throw new VariableNotContainedInDatasetException(
                "Variable: " + varName + " is not contained in the requested dataset");
      }
    }

    // times
    // for closest time, set wantRange to the time LOOK - do we need +- increment ??
    if (ncssParams.getTime() != null) { //Means we want just one single time
      CalendarDate startR = CalendarDate.parseISOformat(null, ncssParams.getTime());
      startR = startR.subtract(CalendarPeriod.Hour);
      CalendarDate endR = CalendarDate.parseISOformat(null, ncssParams.getTime());
      endR = endR.add(CalendarPeriod.Hour);
      //wantRange = new DateRange( new Date(startR.getMillis()), new Date(endR.getMillis()));
      wantRange = CalendarDateRange.of(new Date(startR.getMillis()), new Date(endR.getMillis()));

    } else {
      //Time range: need to compute the time range from the params
      if (ncssParams.isAllTimes()) {
        //Full time range -->CHECK: wantRange=null means all range???

      } else {
        if (ncssParams.getTime_start() != null && ncssParams.getTime_end() != null) {
          CalendarDate startR = CalendarDate.parseISOformat(null, ncssParams.getTime_start());
          CalendarDate endR = CalendarDate.parseISOformat(null, ncssParams.getTime_end());
          //wantRange = new DateRange( new Date(startR.getMillis()), new Date(endR.getMillis()));;
          wantRange = CalendarDateRange.of(new Date(startR.getMillis()), new Date(endR.getMillis()));

        } else if (ncssParams.getTime_start() != null && ncssParams.getTime_duration() != null) {
          CalendarDate startR = CalendarDate.parseISOformat(null, ncssParams.getTime_start());
          TimeDuration td = ncssParams.parseTimeDuration();

          //wantRange = new DateRange( new Date(startR.getMillis()), td);
          wantRange = new CalendarDateRange(startR, (long) td.getValueInSeconds());

        } else if (ncssParams.getTime_end() != null && ncssParams.getTime_duration() != null) {
          CalendarDate endR = CalendarDate.parseISOformat(null, ncssParams.getTime_end());
          TimeDuration td = ncssParams.parseTimeDuration();
          //wantRange = new DateRange(null, new DateType( qb.getTime_end(), null, null), td, null);
          wantRange = new CalendarDateRange(endR, (long) td.getValueInSeconds() * (-1));
        }
      }

    }

  }

}
