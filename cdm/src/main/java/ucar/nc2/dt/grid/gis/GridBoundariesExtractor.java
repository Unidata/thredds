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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
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


	private GridDataset gridDataset;
	private double minLon=0;
	private double maxLon=0;
	private double maxDiff =0;
	private boolean crossesDateLine = false;


	private GridBoundariesExtractor(){}

	private GridBoundariesExtractor(GridDataset gridDataset){
		this.gridDataset = gridDataset;
	}

	/**
	 * 
	 * Takes a GridDataset and returns the boundary for the first gridset in the dataset in as a polygon in WKT   
	 * 
	 * @param gds
	 * @return
	 */
	public String getDatasetBoundariesWKT(){

		StringBuilder polygonWKT = new StringBuilder( "POLYGON((");

		List<Double> polLons = new ArrayList<Double>();
		List<Double> polLats = new ArrayList<Double>();		
		getLatLonsForPolygon(polLons, polLats );

		//Build string from lists
		//Crosses dateLine?			
		//Assuming grid cells don't extend more than 270 deg.
		if( (maxLon > 0 && minLon < 0) && maxDiff > 270 ){
			//either crosses 0 or -180
			crossesDateLine = true;								
		}		

		int nPoints = polLats.size();
		for( int i=0; i< nPoints; i++ ){

			double lon = polLons.get(i);

			if(crossesDateLine && lon < 0) lon+=360;

			if(i < nPoints -1)
				polygonWKT.append( lon ).append(" ").append( polLats.get(i) ).append(",");
			else
				polygonWKT.append( lon ).append(" ").append( polLats.get(i) );
		}		

		polygonWKT.append("))");
		return polygonWKT.toString();
	}


	public String getDatasetBoundariesGeoJSON(){

		StringBuilder polygonJSON = new StringBuilder( "{\"type\":\"Polygon\", \"coordinates\":[ [ " );

		List<Double> polLons = new ArrayList<Double>();
		List<Double> polLats = new ArrayList<Double>();		
		getLatLonsForPolygon(polLons, polLats );

		//Build string from lists
		//Crosses dateLine?			
		//Assuming grid cells don't extend more than 270 deg.
		if( (maxLon > 0 && minLon < 0) && maxDiff > 270 ){
			//either crosses 0 or -180
			crossesDateLine = true;								
		}		

		int nPoints = polLats.size();
		for( int i=0; i< nPoints; i++ ){

			double lon = polLons.get(i);

			if(crossesDateLine && lon < 0) lon+=360;

			if(i < nPoints -1)
				polygonJSON.append("[").append( lon ).append(", ").append( polLats.get(i) ).append("],");
			else
				polygonJSON.append("[").append( lon ).append(", ").append( polLats.get(i) ).append("]");
		}		



		polygonJSON.append(" ] ]}");

		return polygonJSON.toString();
	}

	private void getLatLonsForPolygon( List<Double> polLons, List<Double> polLats){

		Gridset gridset = gridDataset.getGridsets().get(0);

		GridCoordSystem coordSystem = gridset.getGeoCoordSystem();
		ProjectionImpl fromProj = coordSystem.getProjection();
		coordSystem.getLatLonBoundingBox();

		if( coordSystem.getYHorizAxis() instanceof CoordinateAxis1D &&  coordSystem.getXHorizAxis() instanceof CoordinateAxis1D ){

			CoordinateAxis1D xAxis = (CoordinateAxis1D) coordSystem.getXHorizAxis();
			CoordinateAxis1D yAxis  =(CoordinateAxis1D) coordSystem.getYHorizAxis();			

			if(coordSystem.isGlobalLon()) {

				double maxy = yAxis.getMaxValue();
				double miny = yAxis.getMinValue();

				polLons.add(0.0);
				polLats.add(miny);
				
				polLons.add(360.0);
				polLats.add(miny);
				
				polLons.add(360.0);
				polLats.add(maxy);
				
				polLons.add(0.0);
				polLats.add(maxy);
				
				polLons.add(0.0);
				polLats.add(miny);				
				
				//polygonWKT.append("-180 "+ miny +", 180 "+ miny +", 180 "+ maxy +", -180 "+ maxy +", -180 "+ miny +"] ))");
				//polygonWKT.append("0 "+ miny +", 360 "+ miny +", 360 "+ maxy +", 0 "+ maxy +", 0 "+ miny +"] ))");
				//return polygonWKT.toString();

			}else{

				double[] xCoords = xAxis.getCoordValues();
				double[] yCoords = yAxis.getCoordValues();


				LatLonPoint prev = fromProj.projToLatLon(xCoords[0], yCoords[0]);

				//Bottom edge
				for( double x : xCoords  ){
					LatLonPoint point = fromProj.projToLatLon(x, yCoords[0]);

					if( point.getLongitude() < minLon ) minLon = point.getLongitude();  
					if( point.getLongitude() > maxLon ) maxLon = point.getLongitude();

					if( Math.abs(prev.getLongitude() - point.getLongitude() ) > maxDiff   )
						maxDiff = Math.abs(prev.getLongitude() - point.getLongitude() );

					polLons.add(point.getLongitude());
					polLats.add(point.getLatitude());
					prev = point;
				}

				//Right edge
				for( double y : yCoords  ){
					LatLonPoint point = fromProj.projToLatLon(xCoords[xCoords.length-1], y);

					if( point.getLongitude() < minLon ) minLon = point.getLongitude();  
					if( point.getLongitude() > maxLon ) maxLon = point.getLongitude();	

					if( Math.abs(prev.getLongitude() - point.getLongitude() ) > maxDiff   )
						maxDiff = Math.abs(prev.getLongitude() - point.getLongitude() );				

					polLons.add(point.getLongitude());
					polLats.add(point.getLatitude());				
					prev = point;

				}	  

				//Top
				for( int i = xCoords.length-1; i>=0; i--  ){
					LatLonPoint point = fromProj.projToLatLon(xCoords[i], yCoords[yCoords.length-1]);

					if( point.getLongitude() < minLon ) minLon = point.getLongitude();  
					if( point.getLongitude() > maxLon ) maxLon = point.getLongitude();

					if( Math.abs(prev.getLongitude() - point.getLongitude() ) > maxDiff   )
						maxDiff = Math.abs(prev.getLongitude() - point.getLongitude() );

					polLons.add(point.getLongitude());
					polLats.add(point.getLatitude());				
					prev = point;

				}	  

				//Left edge
				for( int i = yCoords.length-1; i>=0; i--  ){

					LatLonPoint point = fromProj.projToLatLon(xCoords[0], yCoords[i]);

					if( point.getLongitude() < minLon ) minLon = point.getLongitude();  
					if( point.getLongitude() > maxLon ) maxLon = point.getLongitude();	

					if( Math.abs(prev.getLongitude() - point.getLongitude() ) > maxDiff   )
						maxDiff = Math.abs(prev.getLongitude() - point.getLongitude() );				

					polLons.add(point.getLongitude());
					polLats.add(point.getLatitude() );				
					prev = point;
				}

			}			

		}else if( coordSystem.getYHorizAxis() instanceof CoordinateAxis2D &&  coordSystem.getXHorizAxis() instanceof CoordinateAxis2D ){

			//Get boundaries from 2d axis...
			CoordinateAxis2D xAxis = (CoordinateAxis2D) coordSystem.getXHorizAxis();
			CoordinateAxis2D yAxis  =(CoordinateAxis2D) coordSystem.getYHorizAxis();

			int[] xShape = xAxis.getShape();
			int[] yShape = yAxis.getShape();


			LatLonPoint prev = fromProj.projToLatLon(xAxis.getCoordValue(0, 0), yAxis.getCoordValue(0, 0));

			for(int i = 0; i < xShape[0]; i++ ){
				double x = xAxis.getCoordValue(i, 0);
				double y = yAxis.getCoordValue(i, 0);

				LatLonPoint point = fromProj.projToLatLon(x, y);

				if( point.getLongitude() < minLon ) minLon = point.getLongitude();  
				if( point.getLongitude() > maxLon ) maxLon = point.getLongitude();

				if( Math.abs(prev.getLongitude() - point.getLongitude() ) > maxDiff )
					maxDiff = Math.abs(prev.getLongitude() - point.getLongitude() );

				polLons.add(point.getLongitude());
				polLats.add(point.getLatitude());					
				prev = point;
			}

			for(int i = 0; i < xShape[1]; i++ ){
				double x = xAxis.getCoordValue(xShape[0]-1, i);
				double y = yAxis.getCoordValue(xShape[0]-1, i);

				LatLonPoint point = fromProj.projToLatLon(x, y);
				if( point.getLongitude() < minLon ) minLon = point.getLongitude();  
				if( point.getLongitude() > maxLon ) maxLon = point.getLongitude();

				if( Math.abs(prev.getLongitude() - point.getLongitude() ) > maxDiff )
					maxDiff = Math.abs(prev.getLongitude() - point.getLongitude() );

				polLons.add(point.getLongitude());
				polLats.add(point.getLatitude());					
				prev = point;					
			}

			for(int i = xShape[0]-1 ; i >= 0; i-- ){
				double x = xAxis.getCoordValue(i, xShape[1]-1);
				double y = yAxis.getCoordValue(i, xShape[1]-1);

				LatLonPoint point = fromProj.projToLatLon(x, y);
				if( point.getLongitude() < minLon ) minLon = point.getLongitude();  
				if( point.getLongitude() > maxLon ) maxLon = point.getLongitude();

				if( Math.abs(prev.getLongitude() - point.getLongitude() ) > maxDiff )
					maxDiff = Math.abs(prev.getLongitude() - point.getLongitude() );

				polLons.add(point.getLongitude());
				polLats.add(point.getLatitude());					
				prev = point;				
			}

			for(int i = xShape[1]-1 ; i >= 0; i-- ){
				double x = xAxis.getCoordValue(0, i);
				double y = yAxis.getCoordValue(0, i);

				LatLonPoint point = fromProj.projToLatLon(x, y);
				if( point.getLongitude() < minLon ) minLon = point.getLongitude();  
				if( point.getLongitude() > maxLon ) maxLon = point.getLongitude();

				if( Math.abs(prev.getLongitude() - point.getLongitude() ) > maxDiff )
					maxDiff = Math.abs(prev.getLongitude() - point.getLongitude() );

				polLons.add(point.getLongitude());
				polLats.add(point.getLatitude());					
				prev = point;				
			}			

		}



	}


	public static GridBoundariesExtractor valueOf(GridDataset gds){

		return new GridBoundariesExtractor(gds);
	}

}
