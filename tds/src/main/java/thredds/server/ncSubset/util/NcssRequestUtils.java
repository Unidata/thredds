package thredds.server.ncSubset.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import thredds.server.config.TdsContext;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

@Component 
public final class NcssRequestUtils implements ApplicationContextAware{

	private static ApplicationContext applicationContext;
	
	private NcssRequestUtils(){
		
	}
	
	public static GridAsPointDataset buildGridAsPointDataset(GridDataset gds, List<String> vars) {

		List<GridDatatype> grids = new ArrayList<GridDatatype>();
		for (String gridName : vars) {
			GridDatatype grid = gds.findGridDatatype(gridName);
			if (grid == null)
				continue;
			grids.add(grid);
		}
		return new GridAsPointDataset(grids);

	}

	public static List<CalendarDate> wantedDates(GridAsPointDataset gap, CalendarDateRange dates) throws OutOfBoundariesException{

		CalendarDate start = dates.getStart();
		CalendarDate end = dates.getEnd();
		List<CalendarDate> gdsDates = gap.getDates();

		if (  start.isAfter(gdsDates.get(gdsDates.size()-1))  || end.isBefore(gdsDates.get(0))  )
			throw new OutOfBoundariesException("Requested time range does not intersect the Data Time Range = " + gdsDates.get(0) + " to " + gdsDates.get(gdsDates.size()-1) );
		
		List<CalendarDate> wantDates = new ArrayList<CalendarDate>();
		

		for (CalendarDate date : gdsDates) {
			if (date.isBefore(start) || date.isAfter(end))
				continue;
			wantDates.add(date);
		}

		return wantDates;
	}
	
	public static TdsContext getTdsContext(){
		
		return applicationContext.getBean(TdsContext.class);
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		NcssRequestUtils.applicationContext =applicationContext;
		
	}
	

}
