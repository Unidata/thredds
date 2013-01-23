/*
 * Copyright 1998-2012 University Corporation for Atmospheric Research/Unidata
 *
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
 
package ucar.nc2.dt.grid.gis;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;

/**
 * 
 * Provides methods for extracting the grid boundaries in standard GIS text formats: WKT and GeoJSON 
 * 
 * @author mhermida
 *
 */
public final class GridBoundariesExtractor {

	private GridBoundariesExtractor(){}
	
	/**
	 * 
	 * Takes a GridDataset and returns the boundary for the first gridset in the dataset in as a polygon in WKT   
	 * 
	 * @param gds
	 * @return
	 */
	public static final String getDatasetBoundariesWKT(GridDataset gds){

		StringBuilder polygonWKT = new StringBuilder( "POLYGON((");
		Gridset gridset = gds.getGridsets().get(0);

		GridCoordSystem coordSystem = gridset.getGeoCoordSystem();
		ProjectionImpl fromProj = coordSystem.getProjection();
		
		if( coordSystem.getYHorizAxis() instanceof CoordinateAxis1D &&  coordSystem.getXHorizAxis() instanceof CoordinateAxis1D ){
			
			CoordinateAxis1D xAxis = (CoordinateAxis1D) coordSystem.getXHorizAxis();
			CoordinateAxis1D yAxis  =(CoordinateAxis1D) coordSystem.getYHorizAxis();			

			if(coordSystem.isGlobalLon()) {

				double maxy = yAxis.getMaxValue();
				double miny = yAxis.getMinValue();

				//polygonWKT.append("-180 "+ miny +", 180 "+ miny +", 180 "+ maxy +", -180 "+ maxy +", -180 "+ miny +"] ))");
				polygonWKT.append("0 "+ miny +", 360 "+ miny +", 360 "+ maxy +", 0 "+ maxy +", 0 "+ miny +"] ))");
				return polygonWKT.toString();
			}

			double[] xCoords = xAxis.getCoordValues();
			double[] yCoords = yAxis.getCoordValues();

			//Bottom edge
			int k = 0;
			for( double x : xCoords  ){
				LatLonPoint point = fromProj.projToLatLon(x, yCoords[0]);

				double lon = point.getLongitude();
				if(lon < 0) lon+=360;			

				polygonWKT.append( lon ).append(" ").append(point.getLatitude()).append(",");		  
			}

			//Right edge
			for( double y : yCoords  ){
				LatLonPoint point = fromProj.projToLatLon(xCoords[xCoords.length-1], y);

				double lon = point.getLongitude();
				if(lon < 0) lon+=360;			

				polygonWKT.append( lon ).append(" ").append(point.getLatitude()).append(",");
			}	  

			//Top
			for( int i = xCoords.length-1; i>=0; i--  ){
				LatLonPoint point = fromProj.projToLatLon(xCoords[i], yCoords[yCoords.length-1]);

				double lon = point.getLongitude();
				if(lon < 0) lon+=360;			

				polygonWKT.append( lon ).append(" ").append(point.getLatitude()).append(",");		  
			}	  

			//Left edge
			for( int i = yCoords.length-1; i>=0; i--  ){

				LatLonPoint point = fromProj.projToLatLon(xCoords[0], yCoords[i]);

				double lon = point.getLongitude();
				if(lon < 0) lon+=360;			

				if(i != 0)
					polygonWKT.append( lon ).append(" ").append(point.getLatitude()).append(",");
				else
					polygonWKT.append( lon ).append(" ").append(point.getLatitude());
			}			
		}else if( coordSystem.getYHorizAxis() instanceof CoordinateAxis2D &&  coordSystem.getXHorizAxis() instanceof CoordinateAxis2D ){
			
			//Get boundaries from 2d axis...
			CoordinateAxis2D xAxis = (CoordinateAxis2D) coordSystem.getXHorizAxis();
			CoordinateAxis2D yAxis  =(CoordinateAxis2D) coordSystem.getYHorizAxis();
			
			int[] xShape = xAxis.getShape();
			int[] yShape = yAxis.getShape();
			
			for(int i = 0; i < xShape[0]; i++ ){
					double x = xAxis.getCoordValue(i, 0);
					double y = yAxis.getCoordValue(i, 0);
					
					LatLonPoint point = fromProj.projToLatLon(x, y);
					polygonWKT.append( point.getLongitude() ).append(" ").append(point.getLatitude()).append(",");					
			}
			
			for(int i = 0; i < xShape[1]; i++ ){
					double x = xAxis.getCoordValue(xShape[0]-1, i);
					double y = yAxis.getCoordValue(xShape[0]-1, i);
					
					LatLonPoint point = fromProj.projToLatLon(x, y);
					polygonWKT.append( point.getLongitude() ).append(" ").append(point.getLatitude()).append(",");					
			}
			
			for(int i = xShape[0]-1 ; i >= 0; i-- ){
				double x = xAxis.getCoordValue(i, xShape[1]-1);
				double y = yAxis.getCoordValue(i, xShape[1]-1);
				
				LatLonPoint point = fromProj.projToLatLon(x, y);
				polygonWKT.append( point.getLongitude() ).append(" ").append(point.getLatitude()).append(",");				
			}
			
			for(int i = xShape[1]-1 ; i >= 0; i-- ){
				double x = xAxis.getCoordValue(0, i);
				double y = yAxis.getCoordValue(0, i);
				
				LatLonPoint point = fromProj.projToLatLon(x, y);
				if(i > 0)
					polygonWKT.append( point.getLongitude() ).append(" ").append(point.getLatitude()).append(",");
				else
					polygonWKT.append( point.getLongitude() ).append(" ").append(point.getLatitude());				
			}			
			
			
			
		}

		polygonWKT.append("))");
		return polygonWKT.toString();
	}
	
	
	public static final String getDatasetBoundariesGeoJSON(GridDataset gds){

		StringBuilder polygonJSON = new StringBuilder( "{\"type\":\"Polygon\", \"coordinates\":[ [ " );
		Gridset gridset = gds.getGridsets().get(0);
		
		GridCoordSystem coordSystem = gridset.getGeoCoordSystem();
		ProjectionImpl fromProj = coordSystem.getProjection();
		
		if( coordSystem.getYHorizAxis() instanceof CoordinateAxis1D &&  coordSystem.getXHorizAxis() instanceof CoordinateAxis1D ){
			
			CoordinateAxis1D xAxis = (CoordinateAxis1D) coordSystem.getXHorizAxis();
			CoordinateAxis1D yAxis  =(CoordinateAxis1D) coordSystem.getYHorizAxis();			

			if(coordSystem.isGlobalLon()) {

				double maxy = yAxis.getMaxValue();
				double miny = yAxis.getMinValue();

				polygonJSON.append("[0, "+ miny +"], [360, "+ miny +"], [360, "+ maxy +"], [0, "+ maxy +"],[0, "+ miny +"] ]]}");			
				return polygonJSON.toString();
			}

			double[] xCoords = xAxis.getCoordValues();
			double[] yCoords = yAxis.getCoordValues();

			//Bottom edge
			int k = 0;
			for( double x : xCoords  ){
				LatLonPoint point = fromProj.projToLatLon(x, yCoords[0]);

				double lon = point.getLongitude();
				if(lon < 0) lon+=360;			

				polygonJSON.append("[").append( lon ).append(", ").append(point.getLatitude()).append("],");		  
			}

			//Right edge
			for( double y : yCoords  ){
				LatLonPoint point = fromProj.projToLatLon(xCoords[xCoords.length-1], y);

				double lon = point.getLongitude();
				if(lon < 0) lon+=360;			

				polygonJSON.append("[").append( lon ).append(", ").append(point.getLatitude()).append("],");
			}	  

			//Top
			for( int i = xCoords.length-1; i>=0; i--  ){
				LatLonPoint point = fromProj.projToLatLon(xCoords[i], yCoords[yCoords.length-1]);

				double lon = point.getLongitude();
				if(lon < 0) lon+=360;			

				polygonJSON.append("[").append( lon ).append(", ").append(point.getLatitude()).append("],");		  
			}	  

			//Left edge
			for( int i = yCoords.length-1; i>=0; i--  ){

				LatLonPoint point = fromProj.projToLatLon(xCoords[0], yCoords[i]);

				double lon = point.getLongitude();
				if(lon < 0) lon+=360;			

				if(i != 0)
					polygonJSON.append("[").append( lon ).append(", ").append(point.getLatitude()).append("],");
				else
					polygonJSON.append("[").append( lon ).append(", ").append(point.getLatitude()).append("]");
			}			
		}else if( coordSystem.getYHorizAxis() instanceof CoordinateAxis2D &&  coordSystem.getXHorizAxis() instanceof CoordinateAxis2D ){
			
			//Get boundaries from 2d axis...
			CoordinateAxis2D xAxis = (CoordinateAxis2D) coordSystem.getXHorizAxis();
			CoordinateAxis2D yAxis  =(CoordinateAxis2D) coordSystem.getYHorizAxis();
			
			int[] xShape = xAxis.getShape();
			int[] yShape = yAxis.getShape();
			
			for(int i = 0; i < xShape[0]; i++ ){
					double x = xAxis.getCoordValue(i, 0);
					double y = yAxis.getCoordValue(i, 0);
					
					LatLonPoint point = fromProj.projToLatLon(x, y);
					polygonJSON.append("[").append( point.getLongitude() ).append(", ").append(point.getLatitude()).append("],");
			}
			
			for(int i = 0; i < xShape[1]; i++ ){
					double x = xAxis.getCoordValue(xShape[0]-1, i);
					double y = yAxis.getCoordValue(xShape[0]-1, i);
					
					LatLonPoint point = fromProj.projToLatLon(x, y);
					polygonJSON.append("[").append( point.getLongitude() ).append(", ").append(point.getLatitude()).append("],");					
			}
			
			for(int i = xShape[0]-1 ; i >= 0; i-- ){
				double x = xAxis.getCoordValue(i, xShape[1]-1);
				double y = yAxis.getCoordValue(i, xShape[1]-1);
				
				LatLonPoint point = fromProj.projToLatLon(x, y);
				polygonJSON.append("[").append( point.getLongitude() ).append(", ").append(point.getLatitude()).append("],");				
			}
			
			for(int i = xShape[1]-1 ; i >= 0; i-- ){
				double x = xAxis.getCoordValue(0, i);
				double y = yAxis.getCoordValue(0, i);
				
				LatLonPoint point = fromProj.projToLatLon(x, y);
				if(i > 0)
					polygonJSON.append("[").append( point.getLongitude() ).append(", ").append(point.getLatitude()).append("],");
				else
					polygonJSON.append("[").append( point.getLongitude() ).append(", ").append(point.getLatitude()).append("]");				
			}			
			
			
			
		}		
					

		polygonJSON.append(" ] ]}");

		return polygonJSON.toString();
	}	
	
}
