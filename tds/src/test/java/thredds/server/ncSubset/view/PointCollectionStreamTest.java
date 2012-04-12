package thredds.server.ncSubset.view;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.controller.SupportedFormat;
import thredds.server.ncSubset.exception.DateUnitException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.DatasetHandlerAdapter;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.nc2.dataset.CoordinateAxis1D;
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
	private CalendarDateRange range;
	private List<String> vars;
	private List<Double> vertCoords;	
	
	@Parameters
	public static List<Object[]> getTestParameters(){
		
		return Arrays.asList(new Object[][]{  
				{SupportedFormat.NETCDF, PointDataWritersParameters.getVars().get(1) , PointDataWritersParameters.getPathInfo().get(1), PointDataWritersParameters.getPoints().get(2), PointDataWritersParameters.getVerticalLevels().get(1) },
				{SupportedFormat.NETCDF, PointDataWritersParameters.getVars().get(2) , PointDataWritersParameters.getPathInfo().get(2), PointDataWritersParameters.getPoints().get(2), PointDataWritersParameters.getVerticalLevels().get(2) }				
		});				
	}
	
	public PointCollectionStreamTest(SupportedFormat supportedFormat,  List<String> vars ,  String pathInfo, LatLonPoint point, Double verticalLevel){
		this.supportedFormat = supportedFormat;
		this.vars = vars;
		this.pathInfo = pathInfo;
		this.point = point;
		this.verticalLevel = verticalLevel;
	}

	@Before
	public void setUp() throws IOException, OutOfBoundariesException, Exception{
		
		gridDataset = DatasetHandlerAdapter.openGridDataset(pathInfo);
		GridAsPointDataset gridAsPointDataset = NcssRequestUtils.buildGridAsPointDataset(gridDataset, vars);
		pointDataStream = PointDataStream.createPointDataStream(supportedFormat, new ByteArrayOutputStream());	
		List<CalendarDate> dates = gridAsPointDataset.getDates();
		Random rand = new Random();
		int randInt =     rand.nextInt( dates.size());
		int randIntNext = rand.nextInt(dates.size());
		int start = Math.min(randInt, randIntNext);
		int end = Math.max(randInt, randIntNext);
		range = CalendarDateRange.of( dates.get(start), dates.get(end));
		
		if(verticalLevel >= 0){
			vertCoords = new ArrayList<Double>();
			vertCoords.add(verticalLevel);
		}else{
			CoordinateAxis1D zAxis = gridDataset.findGridDatatype(vars.get(0)).getCoordinateSystem().getVerticalAxis();
			double[] dVertLevels=  zAxis.getCoordValues();
			vertCoords = new ArrayList<Double>();
			for( Double d : dVertLevels  ) vertCoords.add(d); 
		}
		
		
	}
	
	@Test
	public void shouldStreamStationCollection() throws OutOfBoundariesException, DateUnitException{
		
		assertTrue( pointDataStream.stream(gridDataset, point, range, vars, vertCoords) );
		
	}	
	
}
