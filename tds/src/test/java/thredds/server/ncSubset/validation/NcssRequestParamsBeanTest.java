package thredds.server.ncSubset.validation;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import thredds.server.ncSubset.params.NcssParamsBean;

public class NcssRequestParamsBeanTest {

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
    NcssParamsBean params = new NcssParamsBean();
    List<String> varsAll = new ArrayList<String>();
    varsAll.add("all");
    params.setVar(varsAll);
    Set<ConstraintViolation<NcssParamsBean>> constraintViolations = validator.validate(params);
    assertTrue(constraintViolations.isEmpty());
  }

  @Test
  public void testNcssRequestParamsBeanValidTimeRange() {
    NcssParamsBean params = new NcssParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setTime_start("2012-03-27T00:00:00Z");
    //params.setTime_start("2012-0-27T08:00:00+0200");
    params.setTime_end("2012-03-28");
    params.setAccept("text/csv");
    //params.setTime_duration("PT18H");
    Set<ConstraintViolation<NcssParamsBean>> constraintViolations = validator.validate(params);
    assertTrue(constraintViolations.isEmpty());
  }


  @Test
  public void testNcssRequestParamsBeanTimeRangeIncomplete() {

    NcssParamsBean params = new NcssParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setAccept("text/csv");
    params.setTime_start("present");

    Set<ConstraintViolation<NcssParamsBean>> constraintViolations = validator.validate(params);

    assertEquals(1, constraintViolations.size());
    assertEquals(resolver.get("thredds.server.ncSubset.validation.time.range"), constraintViolations.iterator().next().getMessage());
  }

  @Test
  public void testNcssRequestParamsBeanTimePresent() {

    NcssParamsBean params = new NcssParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setAccept("text/csv");
    params.setTime("present");

    Set<ConstraintViolation<NcssParamsBean>> constraintViolations = validator.validate(params);

    assertTrue(constraintViolations.isEmpty());

  }

  @Test
  public void testNcssRequestParamsBeanTimeInvalidFormat() {
    NcssParamsBean params = new NcssParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setAccept("text/csv");
    params.setTime("2012x0327");

    Set<ConstraintViolation<NcssParamsBean>> constraintViolations = validator.validate(params);
    assertEquals(1, constraintViolations.size());
    Object expect = resolver.get("thredds.server.ncSubset.validation.param.time");
    assertEquals(expect, constraintViolations.iterator().next().getMessage());
  }

  @Test
  public void testNcssRequestParamsBeanInvalidTimeDuration() {
    NcssParamsBean params = new NcssParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setAccept("text/csv");
    params.setTime_start("2012-03-27T00:00:00Z");
    params.setTime_duration("fff");

    Set<ConstraintViolation<NcssParamsBean>> constraintViolations = validator.validate(params);
    assertEquals(1, constraintViolations.size());
    assertEquals(resolver.get("thredds.server.ncSubset.validation.param.time_duration"), constraintViolations.iterator().next().getMessage());
  }

  @Test
  public void testNcssRequestParamsBeanInvalidTimeWindow() {
    NcssParamsBean params = new NcssParamsBean();
    params.setLatitude(42.04);
    params.setLongitude(-105.0);
    params.setVar(Arrays.asList("var1", "var2"));
    //params.setPoint(true);
    params.setAccept("text/csv");
    params.setTime("2012-03-27T00:00:00Z");
    params.setTime_window("fff");

    Set<ConstraintViolation<NcssParamsBean>> constraintViolations = validator.validate(params);
    assertEquals(1, constraintViolations.size());
    assertEquals(resolver.get("thredds.server.ncSubset.validation.param.time_window"), constraintViolations.iterator().next().getMessage());
  }

  @Test
 	public void testNcssMissingLatLon(){

 		NcssParamsBean params = new NcssParamsBean();

 		params.setLongitude(-105.0);
 		params.setVar( Arrays.asList("var1", "var2") );
 		params.setAccept("text/csv");
 		params.setTime("2012-03-27T00:00:00Z");

 		Set<ConstraintViolation<NcssParamsBean>> constraintViolations = validator.validate(params);
 		assertEquals(1 , constraintViolations.size());
    assertEquals(resolver.get("thredds.server.ncSubset.validation.lat_or_lon_missing"), constraintViolations.iterator().next().getMessage());
 	}

 	@Test
 	public void testNcssInvalidSubsetTypeMissingLon(){

 		NcssParamsBean params = new NcssParamsBean();
 		params.setLatitude(42.04);
 		params.setVar( Arrays.asList("var1", "var2") );
 		params.setAccept("text/csv");
 		params.setTime("2012-03-27T00:00:00Z");

 		Set<ConstraintViolation<NcssParamsBean>> constraintViolations = validator.validate(params);
 		assertEquals(1 , constraintViolations.size());
    assertEquals(resolver.get("thredds.server.ncSubset.validation.lat_or_lon_missing"), constraintViolations.iterator().next().getMessage());
 	}

  @Test
 	public void testNcssBBSubsetType(){

 		NcssParamsBean params = new NcssParamsBean();
 		params.setNorth(43.0);
 		params.setSouth(38.0);
 		params.setWest(-107.0);
 		params.setEast(-102.0);
 		params.setVar( Arrays.asList("var1", "var2") );
 		params.setAccept("text/csv");
 		params.setTime("2012-03-27T00:00:00Z");


 		Set<ConstraintViolation<NcssParamsBean>> constraintViolations = validator.validate(params);
 		assertEquals(0 , constraintViolations.size());
 		//assertEquals("Wrong subset type", constraintViolations.iterator().next().getMessage());

 	}



}
