package thredds.server.ncSubset.view;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import thredds.mock.params.GridAsPointDataParameters;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.controller.AbstractFeatureDatasetController;
import thredds.server.ncSubset.controller.NcssDiskCache;
import thredds.server.ncSubset.exception.DateUnitException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.server.ncSubset.view.gridaspoint.PointDataStream;
import thredds.server.ncSubset.dataservice.DatasetHandlerAdapter;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.geoloc.LatLonPoint;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class GridAsPointStreamTest {

	private PointDataStream pointDataStream;
	private SupportedFormat supportedFormat;
	private String pathInfo;
	private LatLonPoint point;
	private Double verticalLevel;
	
	private GridDataset gridDataset;
	private List<CalendarDate> wantedDates;
	private Map<String,List<String>> vars;
	private Double vertCoord;
	
	@Parameters
	public static List<Object[]> getTestParameters(){
						
		return Arrays.asList(new Object[][]{  
				{SupportedFormat.NETCDF3, GridAsPointDataParameters.getGroupedVars().get(2) , GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2), GridAsPointDataParameters.getVerticalLevels().get(2) },
				{SupportedFormat.NETCDF3, GridAsPointDataParameters.getGroupedVars().get(2) , GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2), GridAsPointDataParameters.getVerticalLevels().get(2) },
				{SupportedFormat.NETCDF3, GridAsPointDataParameters.getGroupedVars().get(1) , GridAsPointDataParameters.getPathInfo().get(1), GridAsPointDataParameters.getPoints().get(1), GridAsPointDataParameters.getVerticalLevels().get(1) },
				
				{SupportedFormat.NETCDF4, GridAsPointDataParameters.getGroupedVars().get(2) , GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2), GridAsPointDataParameters.getVerticalLevels().get(2) },
				{SupportedFormat.NETCDF4, GridAsPointDataParameters.getGroupedVars().get(2) , GridAsPointDataParameters.getPathInfo().get(2), GridAsPointDataParameters.getPoints().get(2), GridAsPointDataParameters.getVerticalLevels().get(2) },
				{SupportedFormat.NETCDF4, GridAsPointDataParameters.getGroupedVars().get(1) , GridAsPointDataParameters.getPathInfo().get(1), GridAsPointDataParameters.getPoints().get(1), GridAsPointDataParameters.getVerticalLevels().get(1) }

		});				
	}
	
	public GridAsPointStreamTest(SupportedFormat supportedFormat, Map<String, List<String>> vars, String pathInfo, LatLonPoint point, Double verticalLevel){
		
		this.supportedFormat = supportedFormat;
		this.vars = vars;
		this.pathInfo = pathInfo;
		this.point = point;
		this.verticalLevel = verticalLevel;
	}

	@Before
	public void setUp() throws IOException, OutOfBoundariesException, Exception {
		
    String datasetPath = AbstractFeatureDatasetController.getDatasetPath(this.pathInfo);
		gridDataset = DatasetHandlerAdapter.openGridDataset(datasetPath);
    assert gridDataset != null;

		List<String> keys = new ArrayList<String>( vars.keySet());		
		GridAsPointDataset gridAsPointDataset = NcssRequestUtils.buildGridAsPointDataset(gridDataset, vars.get(keys.get(0)) );
		DiskCache2 diskCache = NcssDiskCache.getInstance().getDiskCache();
		pointDataStream = PointDataStream.factory(supportedFormat, new ByteArrayOutputStream(), diskCache);
		List<CalendarDate> dates = gridAsPointDataset.getDates();
		Random rand = new Random();
		int randInt =     rand.nextInt( dates.size());
		int randIntNext = rand.nextInt(dates.size());
		int start = Math.min(randInt, randIntNext);
		int end = Math.max(randInt, randIntNext);
		CalendarDateRange range = CalendarDateRange.of( dates.get(start), dates.get(end));
		
		//CalendarDateRange range = CalendarDateRange.of( dates.get(0), dates.get(0));
		wantedDates = NcssRequestUtils.wantedDates(gridAsPointDataset, range,0);
		
		/*if(verticalLevel >= 0){
			vertCoords = new ArrayList<Double>();
			vertCoords.add(verticalLevel);
		}else{
			CoordinateAxis1D zAxis = gridDataset.findGridDatatype(vars.get(0)).getCoordinateSystem().getVerticalAxis();
			double[] dVertLevels=  zAxis.getCoordValues();
			vertCoords = new ArrayList<Double>();
			for( Double d : dVertLevels  ) vertCoords.add(d); 
		}*/
		
		
	}
	
	@Test
	public void shouldStreamStationCollection() throws OutOfBoundariesException, DateUnitException, UnsupportedOperationException, InvalidRangeException{
		
		assertTrue( pointDataStream.stream(gridDataset, point, wantedDates, vars, vertCoord) );
		
	}	
	
}
