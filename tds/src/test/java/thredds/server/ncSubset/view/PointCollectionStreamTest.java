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

import thredds.mock.params.PointDataParameters;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.exception.DateUnitException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.DatasetHandlerAdapter;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class PointCollectionStreamTest {

	private PointDataStream pointDataStream;
	private SupportedFormat supportedFormat;	
	private String pathInfo;
	private LatLonPoint point;
	private Double verticalLevel;
	
	private GridDataset gridDataset;
	//private CalendarDateRange range;
	private List<CalendarDate> wantedDates;
	private Map<String,List<String>> vars;
	//private List<Double> vertCoords;
	private Double vertCoord;
	
	@Parameters
	public static List<Object[]> getTestParameters(){
						
		return Arrays.asList(new Object[][]{  
				{SupportedFormat.NETCDF3, PointDataParameters.getGroupedVars().get(2) , PointDataParameters.getPathInfo().get(2), PointDataParameters.getPoints().get(2), PointDataParameters.getVerticalLevels().get(2) },
				{SupportedFormat.NETCDF3, PointDataParameters.getGroupedVars().get(2) , PointDataParameters.getPathInfo().get(2), PointDataParameters.getPoints().get(2), PointDataParameters.getVerticalLevels().get(2) },
				{SupportedFormat.NETCDF3, PointDataParameters.getGroupedVars().get(1) , PointDataParameters.getPathInfo().get(1), PointDataParameters.getPoints().get(1), PointDataParameters.getVerticalLevels().get(1) },
				
				{SupportedFormat.NETCDF4, PointDataParameters.getGroupedVars().get(2) , PointDataParameters.getPathInfo().get(2), PointDataParameters.getPoints().get(2), PointDataParameters.getVerticalLevels().get(2) },
				{SupportedFormat.NETCDF4, PointDataParameters.getGroupedVars().get(2) , PointDataParameters.getPathInfo().get(2), PointDataParameters.getPoints().get(2), PointDataParameters.getVerticalLevels().get(2) },
				{SupportedFormat.NETCDF4, PointDataParameters.getGroupedVars().get(1) , PointDataParameters.getPathInfo().get(1), PointDataParameters.getPoints().get(1), PointDataParameters.getVerticalLevels().get(1) }				

		});				
	}
	
	public PointCollectionStreamTest(SupportedFormat supportedFormat,  Map<String,List<String>> vars ,  String pathInfo, LatLonPoint point, Double verticalLevel){
		
		this.supportedFormat = supportedFormat;
		this.vars = vars;
		this.pathInfo = pathInfo;
		this.point = point;
		this.verticalLevel = verticalLevel;
	}

	@Before
	public void setUp() throws IOException, OutOfBoundariesException, Exception{
		
		gridDataset = DatasetHandlerAdapter.openGridDataset(pathInfo);
		List<String> keys = new ArrayList<String>( vars.keySet());		
		GridAsPointDataset gridAsPointDataset = NcssRequestUtils.buildGridAsPointDataset(gridDataset, vars.get(keys.get(0)) );
		pointDataStream = PointDataStream.createPointDataStream(supportedFormat, new ByteArrayOutputStream());	
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
