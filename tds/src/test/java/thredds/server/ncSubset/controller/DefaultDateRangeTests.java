/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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
package thredds.server.ncSubset.controller;

import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import thredds.server.ncSubset.params.RequestParamsBean;
import ucar.nc2.time.CalendarDateRange;

/**
 * @author marcos
 *
 */
@RunWith(Parameterized.class)
public class DefaultDateRangeTests {

	private GridDataController gridDataController; 
	private RequestParamsBean requestParams;
	
	private long durationInSeconds;
	
	@Parameters
	public static Collection<Object[]> getParameters(){
		
		DateTime now = new DateTime();
		DateTime dayAfter = now.plusDays(1);
		DateTime dayBefore = now.plusDays(-1);
		
		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		String nowStr = fmt.print(now);
		String dayAfterStr  = fmt.print(dayAfter);		
		String dayBeforeStr = fmt.print(dayBefore);
		
		return Arrays.asList( new Object[][]{				
				{0L, "present", null, null, null},
				{86400L, null, "present", dayAfterStr, null},
				{86400L, null, dayBeforeStr,"present",  null},
				{86400L*2,null, dayBeforeStr,dayAfterStr,  null},
				{86400L*3,null, nowStr,null,  "P3D"},
				{86400L*2,null,null, dayBeforeStr,  "P2D"}
		});
	}
	
	public DefaultDateRangeTests(long expectedDuration, String time, String time_start, String time_end, String time_duration){
		durationInSeconds = expectedDuration;
		requestParams = new RequestParamsBean();
		requestParams.setTime(time);
		requestParams.setTime_start(time_start);
		requestParams.setTime_end(time_end);
		requestParams.setTime_duration(time_duration);
	} 
	
	@Before
	public void setUp(){
		
		gridDataController = new GridDataController();
	} 
	
	@Test
	public void shouldGetPresent() throws ParseException{
		
		CalendarDateRange range= gridDataController.getRequestedDateRange(requestParams);
		
		//assertEquals(durationInSeconds, range.getDurationInSecs() );
		//long duration =Math.abs( durationInSeconds - range.getDurationInSecs() );		
		assertTrue(Math.abs( durationInSeconds - range.getDurationInSecs() ) < 2 );
		
	}
}
