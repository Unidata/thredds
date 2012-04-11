package thredds.server.ncSubset.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import thredds.server.ncSubset.controller.SupportedFormat;

public class SupportedFormatTest{
	

	@Test
	public void testIsSupportedFormat(){
		
		String format ="text/xml";
		SupportedFormat sf = SupportedFormat.isSupportedFormat(format, SupportedOperation.POINT_REQUEST);		
		assertEquals("XML", sf.getFormatName());
	}
	
	@Test
	public void testNotSupportedFormat(){
		String format="gml/xml";
		SupportedFormat sf = SupportedFormat.isSupportedFormat(format, SupportedOperation.GRID_REQUEST);
		
		assertEquals(null, sf);
	}

}
