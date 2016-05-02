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
package thredds.server.ncss.controller.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import thredds.junit4.SpringJUnit4ParameterizedClassRunner;
import thredds.mock.web.MockTdsContextLoader;
import thredds.server.ncss.controller.GridDatasetResponder;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.time.*;
import ucar.unidata.util.test.category.NeedsContentRoot;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author marcos
 *
 */
@RunWith(SpringJUnit4ParameterizedClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@Category(NeedsContentRoot.class)
public class DefaultDateRangeTest {

	private NcssParamsBean requestParams;
	
	private long durationInSeconds;
	
	@SpringJUnit4ParameterizedClassRunner.Parameters
  public static List<Object[]> getTestParameters() {

		CalendarDate now = CalendarDate.present();
    CalendarDate dayAfter = now.add(1, CalendarPeriod.Field.Day);
    CalendarDate dayBefore = now.add(-1, CalendarPeriod.Field.Day);

		String nowStr = CalendarDateFormatter.toDateTimeStringISO(now);
		String dayAfterStr  = CalendarDateFormatter.toDateTimeStringISO(dayAfter);
		String dayBeforeStr = CalendarDateFormatter.toDateTimeStringISO(dayBefore);
		
		return Arrays.asList( new Object[][]{				
				{0L, "present", null, null, null},
				{86400L, null, "present", dayAfterStr, null},
				{86400L, null, dayBeforeStr,"present",  null},
				{86400L*2,null, dayBeforeStr, dayAfterStr,  null},
				{86400L*3,null, nowStr,null,  "P3D"},
				{86400L*2,null,null, dayBeforeStr,  "P2D"}
		});
	}
	
	public DefaultDateRangeTest(long expectedDuration, String time, String time_start, String time_end, String
          time_duration){
		durationInSeconds = expectedDuration;
		requestParams = new NcssParamsBean();
		requestParams.setTime(time);
		requestParams.setTime_start(time_start);
		requestParams.setTime_end(time_end);
		requestParams.setTime_duration(time_duration);
    System.out.printf("range=[%s - %s]%n", time_start, time_end);
	}
	
//	@Before
//	public void setUp(){
//		
//		gridDataController = new GridDataController();
//	} 
	
	@Test
	public void shouldGetPresent() throws ParseException{
		
		CalendarDateRange range= GridDatasetResponder.getRequestedDateRange(requestParams, Calendar.getDefault());
		System.out.printf("range=%s%n", range);
		System.out.printf(" duration: expected=%d actual=%d%n", durationInSeconds, range.getDurationInSecs());
		//assertEquals(durationInSeconds, range.getDurationInSecs() );
		//long duration =Math.abs( durationInSeconds - range.getDurationInSecs() );
		assertTrue(Math.abs( durationInSeconds - range.getDurationInSecs() ) < 100 );
		
	}
}
