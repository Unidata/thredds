package thredds.server.ncss.validation;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.*;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.server.ncss.params.NcssGridParamsBean;

public class TestNcssParams {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static Validator validator;
  private static Properties resolver = new Properties();

  @BeforeClass
  public static void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();

    Class c = resolver.getClass();
    InputStream is = c.getResourceAsStream ("/ValidationMessages.properties");
    if (is != null) {
      try {
        resolver.load(is);
        resolver.list(System.out);
        is.close();
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    }
  }

  @Test
  public void testNcssRequestParamsBeanVarsAll() {
    NcssGridParamsBean params = new NcssGridParamsBean();
    List<String> varsAll = new ArrayList<>();
    varsAll.add("all");
    params.setVar(varsAll);
    Set<ConstraintViolation<NcssGridParamsBean>> constraintViolations = validator.validate(params);
    assertTrue(constraintViolations.isEmpty());
  }

  @Test
  public void testNcssRequestParamsBeanValidTimeRange() {
    NcssGridParamsBean params = new NcssGridParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setTime_start("2012-03-27T00:00:00Z");
    //params.setTime_start("2012-0-27T08:00:00+0200");
    params.setTime_end("2012-03-28");
    params.setAccept("text/csv");
    //params.setTime_duration("PT18H");
    Set<ConstraintViolation<NcssGridParamsBean>> constraintViolations = validator.validate(params);
    assertTrue(constraintViolations.isEmpty());
  }


  @Test
  public void testNcssRequestParamsBeanTimeRangeIncomplete() {

    NcssGridParamsBean params = new NcssGridParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setAccept("text/csv");
    params.setTime_start("present");

    Set<ConstraintViolation<NcssGridParamsBean>> constraintViolations = validator.validate(params);

    assertEquals(1, constraintViolations.size());
    assertEquals(resolver.get("thredds.server.ncSubset.validation.time.2of3"), constraintViolations.iterator().next().getMessage());
  }

  @Test
  public void testNcssRequestParamsBeanTimePresent() {

    NcssGridParamsBean params = new NcssGridParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setAccept("text/csv");
    params.setTime("present");

    Set<ConstraintViolation<NcssGridParamsBean>> constraintViolations = validator.validate(params);
    assertTrue(constraintViolations.isEmpty());
  }

  @Test
  public void testNcssRequestParamsBeanTimeInvalidFormat() {
    NcssGridParamsBean params = new NcssGridParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setAccept("text/csv");
    params.setTime("2012x0327");

    Set<ConstraintViolation<NcssGridParamsBean>> constraintViolations = validator.validate(params);
    assertEquals(1, constraintViolations.size());
    Object expect = resolver.get("thredds.server.ncSubset.validation.param.time");
    assertEquals(expect, constraintViolations.iterator().next().getMessage());
  }

  @Test
  public void testNcssRequestParamsBeanInvalidTimeDuration() {
    NcssGridParamsBean params = new NcssGridParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setAccept("text/csv");
    params.setTime_start("2012-03-27T00:00:00Z");
    params.setTime_duration("fff");

    Set<ConstraintViolation<NcssGridParamsBean>> constraintViolations = validator.validate(params);
    assertEquals(1, constraintViolations.size());
    assertEquals(resolver.get("thredds.server.ncSubset.validation.param.time_duration"), constraintViolations.iterator().next().getMessage());
  }

  @Test
  public void testNcssRequestParamsBeanInvalidTimeWindow() {
    NcssGridParamsBean params = new NcssGridParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setAccept("text/csv");
    params.setTime("2012-03-27T00:00:00Z");
    params.setTime_window("fff");

    Set<ConstraintViolation<NcssGridParamsBean>> constraintViolations = validator.validate(params);
    assertEquals(1, constraintViolations.size());
    assertEquals(resolver.get("thredds.server.ncSubset.validation.param.time_window"), constraintViolations.iterator().next().getMessage());
  }

  @Test
 	public void testNcssMissingLatLon(){

 		NcssGridParamsBean params = new NcssGridParamsBean();

 		params.setLongitude(-105.0);
 		params.setVar( Arrays.asList("var1", "var2") );
 		params.setAccept("text/csv");
 		params.setTime("2012-03-27T00:00:00Z");

 		Set<ConstraintViolation<NcssGridParamsBean>> constraintViolations = validator.validate(params);
 		assertEquals(1 , constraintViolations.size());
    assertEquals(resolver.get("thredds.server.ncSubset.validation.lat_or_lon_missing"), constraintViolations.iterator().next().getMessage());
 	}

 	@Test
 	public void testNcssInvalidSubsetTypeMissingLon(){

 		NcssGridParamsBean params = new NcssGridParamsBean();
 		params.setLatitude(42.04);
 		params.setVar( Arrays.asList("var1", "var2") );
 		params.setAccept("text/csv");
 		params.setTime("2012-03-27T00:00:00Z");

 		Set<ConstraintViolation<NcssGridParamsBean>> constraintViolations = validator.validate(params);
 		assertEquals(1 , constraintViolations.size());
    assertEquals(resolver.get("thredds.server.ncSubset.validation.lat_or_lon_missing"), constraintViolations.iterator().next().getMessage());
 	}

  @Test
 	public void testNcssBBSubsetType(){

 		NcssGridParamsBean params = new NcssGridParamsBean();
 		params.setNorth(43.0);
 		params.setSouth(38.0);
 		params.setWest(-107.0);
 		params.setEast(-102.0);
 		params.setVar( Arrays.asList("var1", "var2") );
 		params.setAccept("text/csv");
 		params.setTime("2012-03-27T00:00:00Z");


 		Set<ConstraintViolation<NcssGridParamsBean>> constraintViolations = validator.validate(params);
 		assertEquals(0 , constraintViolations.size());
 		//assertEquals("Wrong subset type", constraintViolations.iterator().next().getMessage());

 	}



}
