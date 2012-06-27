package thredds.server.ncSubset.validation;
	

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import thredds.server.ncSubset.params.GridDataRequestParamsBean;
import thredds.server.ncSubset.params.PointDataRequestParamsBean;

public class NcssRequestParamsBeanTest {
	
	private static Validator validator;
	
	@BeforeClass
	public static void setUp(){
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}
	
	@Test
	public void testNcssRequestParamsBeanTwoMissing(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(42.04);
		params.setLongitude(-105.0);		
		params.setVar( Arrays.asList("var1", "var2") );
		params.setPoint(true);
		params.setAccept("text/csv");
		params.setTime_start("present");						
		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		
		assertEquals(1 , constraintViolations.size());
		assertEquals("Must have 2 of 3 parameters: time_start, time_end, time_duration",constraintViolations.iterator().next().getMessage());
	}

	@Test
	public void testNcssRequestParamsBeanTimePresent(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(42.04);
		params.setLongitude(-105.0);		
		params.setVar( Arrays.asList("var1", "var2") );
		params.setPoint(true);
		params.setAccept("text/csv");
		params.setTime("present");							
		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		
		assertTrue(constraintViolations.isEmpty());
		
	}
	
	@Test
	public void testNcssRequestParamsBeanInvalidFormat(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(42.04);
		params.setLongitude(-105.0);		
		params.setVar( Arrays.asList("var1", "var2") );
		params.setPoint(true);
		params.setAccept("text/csv");
		params.setTime("2012x0327");							
		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		assertEquals(1 , constraintViolations.size());
		assertEquals("Invalid data format for param time", constraintViolations.iterator().next().getMessage());
		
	}	
	
	@Test
	public void testNcssRequestParamsBeanInvalidDuration(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(42.04);
		params.setLongitude(-105.0);		
		params.setVar( Arrays.asList("var1", "var2") );
		params.setPoint(true);
		params.setAccept("text/csv");
		params.setTime_start("2012-03-27T00:00:00Z");
		params.setTime_duration("fff");
		
		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		assertEquals(1 , constraintViolations.size());
		assertEquals("Invalid data format for param time_duration", constraintViolations.iterator().next().getMessage());		
		
	}
	
	@Test
	public void testNcssRequestParamsBeanInvalidWindow(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(42.04);
		params.setLongitude(-105.0);		
		params.setVar( Arrays.asList("var1", "var2") );
		params.setPoint(true);
		params.setAccept("text/csv");
		params.setTime("2012-03-27T00:00:00Z");
		params.setTime_window("fff");
		
		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		assertEquals(1 , constraintViolations.size());
		assertEquals("Invalid data format for param time_window", constraintViolations.iterator().next().getMessage());		
		
	}	
	
	@Test
	public void testNcssRequestParamsBeanValidParams(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(42.04);
		params.setLongitude(-105.0);		
		params.setVar( Arrays.asList("var1", "var2") );
		params.setPoint(true);
		params.setTime_start("2012-03-27T00:00:00Z");
		//params.setTime_start("2012-0-27T08:00:00+0200");
		params.setTime_end("2012-03-28");
		params.setAccept("text/csv");
		//params.setTime_duration("PT18H");		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		assertTrue(constraintViolations.isEmpty());
				
	}
	
	@Test
	public void testNcssRequestParamsBeanVarsAll(){
		
		GridDataRequestParamsBean params = new GridDataRequestParamsBean();
		List<String> varsAll = new ArrayList<String>();
		varsAll.add("all");
		params.setVar(varsAll);
		Set<ConstraintViolation<GridDataRequestParamsBean>> constraintViolations = validator.validate(params);
		assertTrue(constraintViolations.isEmpty());
				
	}	
}
