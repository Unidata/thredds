/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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
package thredds.server.ncSubset.validation;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import thredds.server.ncSubset.params.PointDataRequestParamsBean;

/**
 * @author mhermida
 *
 */
public class NcssSubsetTypeValidationTest {
	
	private static Validator validator;
	
	@BeforeClass
	public static void setUp(){
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}	
	
	
	@Test
	public void testNcssInvalidSubsetTypeMissingLat(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();

		params.setLongitude(-105.0);		
		params.setVar( Arrays.asList("var1", "var2") );
		params.setAccept("text/csv");
		params.setTime("2012-03-27T00:00:00Z");
				
		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		assertEquals(1 , constraintViolations.size());
		assertEquals("Must provide latitude and longitude parameters for point subsetting", constraintViolations.iterator().next().getMessage());		
		
	}	
	
	@Test
	public void testNcssInvalidSubsetTypeMissingLon(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setLatitude(42.04);		
		params.setVar( Arrays.asList("var1", "var2") );
		params.setAccept("text/csv");
		params.setTime("2012-03-27T00:00:00Z");
				
		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		assertEquals(1 , constraintViolations.size());
		assertEquals("Must provide latitude and longitude parameters for point subsetting", constraintViolations.iterator().next().getMessage());		
		
	}
	
	@Test
	public void testNcssInvalidSubsetTypeMissingStns(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setSubset("stns");		
		params.setVar( Arrays.asList("var1", "var2") );
		params.setAccept("text/csv");
		params.setTime("2012-03-27T00:00:00Z");
				
		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		assertEquals(1 , constraintViolations.size());
		assertEquals("stns param must be provided for station list subsetting", constraintViolations.iterator().next().getMessage());		
		
	}
	
	@Test
	public void testNcssInvalidSubsetWrongSubsetType(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setSubset("wrong");		
		params.setVar( Arrays.asList("var1", "var2") );
		params.setAccept("text/csv");
		params.setTime("2012-03-27T00:00:00Z");
				
		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		assertEquals(1 , constraintViolations.size());
		assertEquals("Wrong subset type", constraintViolations.iterator().next().getMessage());		
		
	}
	
	@Test
	public void testNcssBBSubsetType(){
		
		PointDataRequestParamsBean params = new PointDataRequestParamsBean();
		params.setSubset("bb");
		params.setNorth(43.0);
		params.setSouth(38.0);
		params.setWest(-107.0);
		params.setEast(-102.0);
		params.setVar( Arrays.asList("var1", "var2") );
		params.setAccept("text/csv");
		params.setTime("2012-03-27T00:00:00Z");
				
		
		Set<ConstraintViolation<PointDataRequestParamsBean>> constraintViolations = validator.validate(params);
		assertEquals(0 , constraintViolations.size());
		//assertEquals("Wrong subset type", constraintViolations.iterator().next().getMessage());		
		
	}	

}
