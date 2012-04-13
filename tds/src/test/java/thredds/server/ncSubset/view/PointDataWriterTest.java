package thredds.server.ncSubset.view;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncSubset.controller.SupportedFormat;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.DatasetHandlerAdapter;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.test.context.junit4.SpringJUnit4ParameterizedClassRunner.Parameters;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridAsPointDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPoint;

@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
public class PointDataWriterTest {
	
	private PointDataWriter pointDataWriter;
	private SupportedFormat supportedFormat;
	
	private String pathInfo;

	
	private List<String> vars; 
	private GridDataset gridDataset;
	private GridAsPointDataset gridAsPointDataset;
	private LatLonPoint point;
	private CalendarDate date; 
	private List<CalendarDate> wDates;
	private CoordinateAxis1D zAxis;
	private CoordinateAxis1DTime tAxis;
	private DateUnit dateUnit;
	
	@Parameters
	public static List<Object[]> getTestParameters(){
		
		return Arrays.asList(new Object[][]{  
				{SupportedFormat.CSV, PointDataWritersParameters.getVars().get(0) , PointDataWritersParameters.getPathInfo().get(0), PointDataWritersParameters.getPoints().get(0) },
				{SupportedFormat.CSV, PointDataWritersParameters.getVars().get(1) , PointDataWritersParameters.getPathInfo().get(1), PointDataWritersParameters.getPoints().get(1) },
				{SupportedFormat.CSV, PointDataWritersParameters.getVars().get(2) , PointDataWritersParameters.getPathInfo().get(2), PointDataWritersParameters.getPoints().get(2) },
				
				{SupportedFormat.XML, PointDataWritersParameters.getVars().get(0) , PointDataWritersParameters.getPathInfo().get(0), PointDataWritersParameters.getPoints().get(0) },
				{SupportedFormat.XML, PointDataWritersParameters.getVars().get(1) , PointDataWritersParameters.getPathInfo().get(1), PointDataWritersParameters.getPoints().get(1) },
				{SupportedFormat.XML, PointDataWritersParameters.getVars().get(2) , PointDataWritersParameters.getPathInfo().get(2), PointDataWritersParameters.getPoints().get(2) },
				
				{SupportedFormat.NETCDF, PointDataWritersParameters.getVars().get(0) , PointDataWritersParameters.getPathInfo().get(0), PointDataWritersParameters.getPoints().get(0) },
				{SupportedFormat.NETCDF, PointDataWritersParameters.getVars().get(1) , PointDataWritersParameters.getPathInfo().get(1), PointDataWritersParameters.getPoints().get(1) },
				{SupportedFormat.NETCDF, PointDataWritersParameters.getVars().get(2) , PointDataWritersParameters.getPathInfo().get(2), PointDataWritersParameters.getPoints().get(2) }				
		});				
	}
	
	public PointDataWriterTest(SupportedFormat supportedFormat,  List<String> vars ,  String pathInfo, LatLonPoint point  ){
		
		this.supportedFormat = supportedFormat;
		this.vars = vars;		
		this.pathInfo = pathInfo;		
		this.point = point;
	}	
	
	@Before
	public void setUp() throws IOException, OutOfBoundariesException, Exception{
		
		gridDataset = DatasetHandlerAdapter.openGridDataset(pathInfo);
		gridAsPointDataset = NcssRequestUtils.buildGridAsPointDataset(gridDataset, vars);		
		pointDataWriter = AbstractPointDataWriterFactory.createPointDataWriterFactory(supportedFormat).createPointDataWriter(new ByteArrayOutputStream());
		
		List<CalendarDate> dates = gridAsPointDataset.getDates();
		Random rand = new Random();
		int randInt =     rand.nextInt( dates.size());
		int randIntNext = rand.nextInt(dates.size());
		int start = Math.min(randInt, randIntNext);
		int end = Math.max(randInt, randIntNext);
		CalendarDateRange range = CalendarDateRange.of( dates.get(start), dates.get(end));		
		wDates = NcssRequestUtils.wantedDates(gridAsPointDataset, range);
		
		date = dates.get(randInt);
		zAxis = gridDataset.findGridDatatype(vars.get(0)).getCoordinateSystem().getVerticalAxis();
		tAxis = gridDataset.findGridDatatype(vars.get(0)).getCoordinateSystem().getTimeAxis1D();
		dateUnit = new DateUnit(tAxis.getUnitsString());
	}
	
	
 
	
	@Test
	public void shouldWriteResponse(){
		assertTrue( pointDataWriter.header(vars, gridDataset, wDates, dateUnit, point));
		assertTrue( pointDataWriter.write(vars, gridDataset, gridAsPointDataset, date, point ));
	}
	
	@After
	public void tearDown(){
	
		gridDataset = null;
		gridAsPointDataset = null;
		//....
	}

}
