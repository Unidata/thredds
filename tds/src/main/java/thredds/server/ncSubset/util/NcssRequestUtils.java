package thredds.server.ncSubset.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import thredds.server.config.TdsContext;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.TimeOutOfWindowException;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
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
	
	public static List<String> getAllVarsAsList(GridDataset gds){
		List<String> vars = new ArrayList<String>();
		
		List<VariableSimpleIF> allVars = gds.getDataVariables();
		for(VariableSimpleIF var : allVars  ){
			vars.add(var.getShortName());
		}
		
		return vars;
	}
	
	

	public static List<CalendarDate> wantedDates(GridAsPointDataset gap, CalendarDateRange dates, long timeWindow) throws TimeOutOfWindowException, OutOfBoundariesException{

		CalendarDate start = dates.getStart();
		CalendarDate end = dates.getEnd();
				
		
		List<CalendarDate> gdsDates = gap.getDates();

		if (  start.isAfter(gdsDates.get(gdsDates.size()-1))  || end.isBefore(gdsDates.get(0))  )
			throw new OutOfBoundariesException("Requested time range does not intersect the Data Time Range = " + gdsDates.get(0) + " to " + gdsDates.get(gdsDates.size()-1) );
		
		List<CalendarDate> wantDates = new ArrayList<CalendarDate>();
		
		if(dates.isPoint()){
	      int best_index = 0;
	      long best_diff = Long.MAX_VALUE;
	      for (int i = 0; i < gdsDates.size(); i++) {
	        CalendarDate date =  gdsDates.get(i);
	        long diff = Math.abs( date.getDifferenceInMsecs( start) );
	        if (diff < best_diff) {
	          best_index = i;
	          best_diff = diff;
	        }
	      }
	      if( timeWindow > 0 && best_diff > timeWindow) //Best time is out of our acceptable timeWindow
	    	  throw new TimeOutOfWindowException("There is not time within the provided time window"); 
	    	  
	    	  
	      wantDates.add(gdsDates.get(best_index));		
		}else{				
			for (CalendarDate date : gdsDates) {
				if (date.isBefore(start) || date.isAfter(end))
					continue;
				wantDates.add(date);
			}
		}
		return wantDates;
	}
	

	public static List<VariableSimpleIF> wantedVars2VariableSimple(List<String> wantedVars, GridDataset gds, NetcdfDataset ncfile ){

        // need VariableSimpleIF for each variable
        List<VariableSimpleIF> varList = new ArrayList<VariableSimpleIF>(wantedVars.size());
        
        //And wantedVars must be in the dataset 
        for(String var : wantedVars){        	
        	VariableEnhanced ve = gds.findGridDatatype(var).getVariable();
        	/*List<Dimension> lDims =ve.getDimensions();
        	StringBuilder dims = new StringBuilder("");
        	for(Dimension d: lDims){
        		dims.append(" ").append(d.getName());
        	}*/
            String dims = ""; // always scalar ????
            VariableSimpleIF want = new VariableDS( ncfile, null, null, ve.getShortName(), ve.getDataType(), dims, ve.getUnitsString(), ve.getDescription());
            varList.add( want);        	
        }
		
		return varList;
	}
	
	public static Double getTimeCoordValue(GridDatatype grid, CalendarDate date){
		
		CoordinateAxis1DTime tAxis = grid.getCoordinateSystem().getTimeAxis1D();
    	Integer wIndex = tAxis.findTimeIndexFromCalendarDate( date );
    	Double coordVal = tAxis.getCoordValue(wIndex);		
		return coordVal;
	}	
	
	public static Double getTargetLevelForVertCoord(CoordinateAxis1D zAxis, Double vertLevel){
		
		Double targetLevel = vertLevel;
		int coordLevel = 0;
		// If zAxis has one level zAxis.findCoordElement(vertLevel) returns -1 and only works with vertLevel = 0
		// Workaround while not fixed in CoordinateAxis1D
		if (zAxis.getSize() == 1) {
			targetLevel = 0.0;
		} else {
			coordLevel = zAxis.findCoordElement(vertLevel);
			if (coordLevel > 0) {
				targetLevel = zAxis.getCoordValue(coordLevel);
			}
		}		
		
		return targetLevel;
	}	
	
	
	public static TdsContext getTdsContext(){
		
		return applicationContext.getBean(TdsContext.class);
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		NcssRequestUtils.applicationContext =applicationContext;
		
	}
	

}
