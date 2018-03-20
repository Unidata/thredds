/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt.grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import ucar.ma2.Array;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * Add Point operations to a GridDataset.
 *
 * @author caron
 */
public class GridAsPointDataset {
  private List<CalendarDate> dates;

  public GridAsPointDataset( List<GridDatatype> grids) {
    //HashSet<Date> dateHash = new HashSet<Date>(); ????
    HashSet<CalendarDate> dateHash = new HashSet<>();
    List<CoordinateAxis1DTime> timeAxes = new ArrayList<>();

    for (GridDatatype grid : grids) {
      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
      if ((timeAxis != null) && !timeAxes.contains(timeAxis)) {
        timeAxes.add(timeAxis);

        //Date[] timeDates = timeAxis.getTimeDates();
        List<CalendarDate> timeDates = timeAxis.getCalendarDates();
        //for (Date timeDate : timeDates)
        for (CalendarDate timeDate : timeDates)
          //dateHash.add( CalendarDate.of( timeDate) );
        	dateHash.add( timeDate );
      }
    }
    
    // -->if dateHash is HashSet<Date> hashSet.toArray returns Date[], not CalendarDate[] ???
    dates = Arrays.asList( dateHash.toArray(new CalendarDate[dateHash.size()]));   
    Collections.sort(dates);
  }

  public List<CalendarDate> getDates() { return dates; }

  public boolean hasTime( GridDatatype grid, CalendarDate date) {
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
    return (timeAxis != null) && timeAxis.hasCalendarDate( date);
  }

  public double getMissingValue(GridDatatype grid) {
    return Double.NaN;
    //VariableEnhanced ve = grid.getVariable();
    //return ve.getValidMax(); // LOOK bogus
  }

  public Point readData(GridDatatype grid, CalendarDate date, double lat, double lon)  throws java.io.IOException {
    GridCoordSystem gcs = grid.getCoordinateSystem();

    //CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
    //int tidx = timeAxis.findTimeIndexFromCalendarDate(date);
    
    int tidx =-1;
    //Date may be null if the grid does not have time axis 
    if(date != null)
    	tidx = findTimeIndexForCalendarDate(gcs, date);
    
    //int[] xy = gcs.findXYindexFromLatLonBounded(lat, lon, null);
    int[] xy = gcs.findXYindexFromLatLon(lat, lon, null);

    Array data  = grid.readDataSlice(tidx, -1, xy[1], xy[0]);

    // use actual grid midpoint
    LatLonPoint latlon = gcs.getLatLon(xy[0], xy[1]);

    Point p = new Point();
    p.lat = latlon.getLatitude();
    p.lon = latlon.getLongitude();
    p.dataValue = data.getDouble( data.getIndex());
    return p;
  }

  public boolean hasVert( GridDatatype grid, double zCoord) {
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis1D zAxis = gcs.getVerticalAxis();
    if (zAxis == null) return false;
    return (zAxis.findCoordElement( zCoord) >= 0);
  }

  public Point readData(GridDatatype grid, CalendarDate date, double zCoord, double lat, double lon)  throws java.io.IOException {
    GridCoordSystem gcs = grid.getCoordinateSystem();

    //CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
    //int tidx = timeAxis.findTimeIndexFromCalendarDate(date);
    int tidx = -1;
    //Date may be null if the grid does not have time axis 
    if(date != null)
    	tidx= findTimeIndexForCalendarDate(gcs, date);
    
    CoordinateAxis1D zAxis = gcs.getVerticalAxis();
    int zidx = zAxis.findCoordElement( zCoord);

    int[] xy = gcs.findXYindexFromLatLon(lat, lon, null);

    Array data  = grid.readDataSlice(tidx, zidx, xy[1], xy[0]);
    
    // use actual grid midpoint
    LatLonPoint latlon = gcs.getLatLon(xy[0], xy[1]);

    Point p = new Point();
    p.lat = latlon.getLatitude();
    p.lon = latlon.getLongitude();
    p.z = zAxis.getCoordValue( zidx);
    p.dataValue = data.getDouble( data.getIndex());
    return p;
  }
  
  /**
   * 
   * Reads one single data for one point.
   * Takes the ensemble and elevation coordinates allowing them to be < 0 and in that case they'll be ignored. 
   * 
   * @param grid
   * @param date
   * @param ensCoord
   * @param zCoord
   * @param lat
   * @param lon
   * @return Point matching lat/lon for this grid
   * @throws java.io.IOException
   */
  public Point readData(GridDatatype grid, CalendarDate date, double ensCoord, double zCoord, double lat, double lon)  throws java.io.IOException {
	    GridCoordSystem gcs = grid.getCoordinateSystem();

	    //CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
	    //int tidx = timeAxis.findTimeIndexFromCalendarDate(date);
	    int tidx = -1;
	    //Date may be null if the grid does not have time axis 
	    if(date != null)
	    	tidx= findTimeIndexForCalendarDate(gcs, date);
	    
	    Point p = new Point();
	    int zidx = -1;
	    //if(zCoord != -1){ LOOK!! --> zCoord may be -1 (ocean sigma levels usually go from 0 to -1)!!!
	    CoordinateAxis1D zAxis = gcs.getVerticalAxis();
	    if( zAxis !=null ){
	    	zidx = zAxis.findCoordElement( zCoord);
	    	if(zidx != -1)
	    		p.z = zAxis.getCoordValue(zidx);
	    }	
	    //}
	    	    
	    int eidx =-1;	    
	    if(ensCoord != -1){
	    	CoordinateAxis1D ensAxis = gcs.getEnsembleAxis();
	    	eidx = ensAxis.findCoordElement( ensCoord);
	    	p.ens = ensAxis.getCoordValue(eidx);
	    }
	    
	    int[] xy = gcs.findXYindexFromLatLon(lat, lon, null);

	    //Array data  = grid.readDataSlice(tidx, zidx, xy[1], xy[0]);
	    Array data  = grid.readDataSlice(0, eidx, tidx, zidx, xy[1], xy[0]);
	    
	    // use actual grid midpoint
	    LatLonPoint latlon = gcs.getLatLon(xy[0], xy[1]);

	    
	    p.lat = latlon.getLatitude();
	    p.lon = latlon.getLongitude();	    
	    
	    p.dataValue = data.getDouble( data.getIndex());
	    return p;
	  }  
  
  /**
   * Reads data for the given point (earthlocation) and if bounded is true returns data for the closest point within the grid, for points outside of the grid
   * 
   * @param grid read data from here
   * @param date at this time
   * @param location EarthLocation, if altitude is NaN assume that is 2D point
   * @param bounded if bounded, location must be in grid cell; otherwise get nearest grid point to location
   * @return the location and data value
   * @throws java.io.IOException on bad stuff
   */
  public Point readData(GridDatatype grid, CalendarDate date, EarthLocation location, boolean bounded)  throws java.io.IOException {

  
	  if(!bounded){
		  if( Double.isNaN(location.getAltitude()) ){
			  return readData(grid, date, location.getLatitude(), location.getLongitude());
		  }else{
			  return readData(grid, date, location.getAltitude(), location.getLatitude(), location.getLongitude());
		  }
	  }
	  
	  //Bounded --> Read closest data 
	  GridCoordSystem gcs = grid.getCoordinateSystem();
	  int tidx = findTimeIndexForCalendarDate(gcs, date);	  
	  int[] xy = gcs.findXYindexFromLatLonBounded(location.getLatitude(), location.getLongitude(), null);
	    	  
	  LatLonPoint latlon = gcs.getLatLon(xy[0], xy[1]);
	  Point p = new Point();
	  p.lat = latlon.getLatitude();
	  p.lon = latlon.getLongitude();
	    	  
	  int zidx=-1;
	  if( !Double.isNaN(location.getAltitude())){
		    CoordinateAxis1D zAxis = gcs.getVerticalAxis();
		    zidx = zAxis.findCoordElement( location.getAltitude());
		    p.z = zAxis.getCoordValue( zidx);
	  }
	  
	  Array data = grid.readDataSlice(tidx, zidx, xy[1], xy[0]);	  	  
	  p.dataValue = data.getDouble( data.getIndex());
	  
	  return p;
  }

  private int findTimeIndexForCalendarDate(GridCoordSystem gcs, CalendarDate date){	  
	  
	  CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();	  
	  return timeAxis.findTimeIndexFromCalendarDate(date);
  }

  public static class Point {
    public double lat,lon,z,ens,dataValue;
  }

  public static void main(String[] args) throws IOException {
    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open("Q:/cdmUnitTest/transforms/Eumetsat.VerticalPerspective.grb");
    GridDatatype grid = gds.findGridDatatype( "Pixel_scene_type");
    GridCoordSystem gcs = grid.getCoordinateSystem();

    double lat = 8.0;
    double lon = 21.0;

    // find the x,y point for a specific lat/lon position
    int[] xy = gcs.findXYindexFromLatLon(lat, lon, null); // xy[0] = x, xy[1] = y

    // read the data at that lat, lon a specific t and z value
    Array data  = grid.readDataSlice(0, 0, xy[1], xy[0]); // note t, z, y, x
    double val = data.getDouble(0);
    System.out.printf("Value at %f %f == %f%n", lat, lon, val);
  }

}
