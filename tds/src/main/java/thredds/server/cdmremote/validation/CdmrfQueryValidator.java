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
package thredds.server.cdmremote.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import thredds.server.cdmremote.params.CdmrfQueryBean;
import thredds.server.cdmremote.params.CdmrfQueryBean.RequestType;
import thredds.server.cdmremote.params.CdmrfQueryBean.SpatialSelection;
import thredds.server.cdmremote.params.CdmrfQueryBean.TemporalSelection;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

/**
 * @author mhermida
 *
 */
public class CdmrfQueryValidator implements ConstraintValidator<CdmrfQueryConstraint, CdmrfQueryBean> {

	private boolean isValid = true;

	/* (non-Javadoc)
	 * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
	 */
	@Override
	public void initialize(CdmrfQueryConstraint constraint) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.validation.ConstraintValidator#isValid(java.lang.Object, javax.validation.ConstraintValidatorContext)
	 */
	@Override
	public boolean isValid(CdmrfQueryBean queryBean,	ConstraintValidatorContext context) {

		validate(queryBean, context);

		return isValid;
	}

	/*
	 * TODO: test this!!!
	 */
	private void validate(CdmrfQueryBean queryBean, ConstraintValidatorContext context) {
		RequestType reqType = queryBean.getRequestType();
		if (reqType == RequestType.data) {
			parseVariablesForm(queryBean, context);
			parseSpatialExtentForm(queryBean, context);
			parseTemporalExtentForm(queryBean, context);

		} else {
			parseSpatialExtent(queryBean, context);
			parseTimeExtent(queryBean);

			//if ((spatialSelection == null) && (stn != null))
			if (( queryBean.getSpatialSelection() == null) && ( queryBean.getStn()  != null))
			//spatialSelection = SpatialSelection.stns;
				queryBean.setSpatialSelection(SpatialSelection.stns);
		}
	}

	private void parseVariablesForm(CdmrfQueryBean queryBean, ConstraintValidatorContext context) {  // from the form
		String variables = queryBean.getVariables();

		if ( variables == null) {
			//errs.format("form must have variables=(all|some)%n");
			context.buildConstraintViolationWithTemplate("{thredds.server.cdmremote.validation.error.musthavevars}").addConstraintViolation(); 
			isValid = false;
			return;
		}

		if (variables.equalsIgnoreCase("all")) {
			queryBean.setVar(null);
		}
	}

	private void parseSpatialExtentForm(CdmrfQueryBean queryBean, ConstraintValidatorContext context) { // from the form
		String spatial = queryBean.getSpatial();  
		if (spatial == null) {
			//errs.format("form must have spatial=(all|bb|point|stns)%n");
			context.buildConstraintViolationWithTemplate("{thredds.server.cdmremote.validation.error.musthavespatial}").addConstraintViolation();	
			isValid = false;
			return;
		}

		if (spatial.equalsIgnoreCase("all")) queryBean.setSpatialSelection(SpatialSelection.all); //spatialSelection = SpatialSelection.all;
		else if (spatial.equalsIgnoreCase("bb")) queryBean.setSpatialSelection(SpatialSelection.bb);  //spatialSelection = SpatialSelection.bb;
		else if (spatial.equalsIgnoreCase("point")) queryBean.setSpatialSelection(SpatialSelection.point); //spatialSelection = SpatialSelection.point;
		else if (spatial.equalsIgnoreCase("stns")) queryBean.setSpatialSelection(SpatialSelection.stns); //spatialSelection = SpatialSelection.stns;

		if (queryBean.getSpatialSelection() == SpatialSelection.bb) {
			parseSpatialExtent(queryBean, context);

		} else if (queryBean.getSpatialSelection() == SpatialSelection.point) {
			double lat = queryBean.parseLat("latitude", queryBean.getLatitude() );
			double lon = queryBean.parseLon("longitude", queryBean.getLongitude());
			//latlonPoint = new LatLonPointImpl(lat, lon);
			queryBean.setLatLonPoint(new LatLonPointImpl(lat, lon));
		}

	}	

	private void parseSpatialExtent(CdmrfQueryBean queryBean, ConstraintValidatorContext context){
		String bbox = queryBean.getBbox();
		//String west="", east="", south="", north="";
		String west=queryBean.getWest(), east=queryBean.getEast(), south=queryBean.getSouth(), north=queryBean.getNorth();
		if (bbox != null) {
			String[] s = bbox.split(",");
			if (s.length != 4) {
				//errs.format("bbox must have form 'bbox=west,east,south,north'; found 'bbox=%s'%n", bbox);
				context.buildConstraintViolationWithTemplate("{thredds.server.cdmremote.validation.error.bboxmusbevalid}").addConstraintViolation();
				isValid = false;
				return;
			}
			west = s[0];
			east = s[1];
			south = s[2];
			north = s[3];
		}

		if ((west != null) || (east != null) || (south != null) || (north != null)) {
			if ((west == null) || (east == null) || (south == null) || (north == null)) {
				//errs.format("All edges (west,east,south,north) must be specified; found west=%s east=%s south=%s north=%s %n", west, east, south, north);
				context.buildConstraintViolationWithTemplate("{thredds.server.cdmremote.validation.error.missingedges}").addConstraintViolation();
				//fatal = true;
				isValid = false;
				return;
			}
			double westd = queryBean.parseLon("west", west);
			double eastd = queryBean.parseLon("east", east);
			double southd = queryBean.parseLat("south", south);
			double northd = queryBean.parseLat("north", north);

			if (isValid) {	        	
				//llbb = new LatLonRect(new LatLonPointImpl(southd, westd), new LatLonPointImpl(northd, eastd));
				queryBean.setLLBB(new LatLonRect(new LatLonPointImpl(southd, westd), new LatLonPointImpl(northd, eastd)));	
				queryBean.setSpatialSelection( SpatialSelection.bb);
			}
		}
	}

	private void parseTemporalExtentForm(CdmrfQueryBean queryBean, ConstraintValidatorContext context) { // from the form
		String temporal = queryBean.getTemporal();
		if (temporal == null) {
			//errs.format("form must have temporal=(all|range|point)%n");
			context.buildConstraintViolationWithTemplate("{thredds.server.cdmremote.validation.error.musthavetemporal}").addConstraintViolation();
			isValid = false;
			return;
		}

		if (temporal.equalsIgnoreCase("all")) queryBean.setTemporalSelection(TemporalSelection.all); // temporalSelection = TemporalSelection.all;
		else if (temporal.equalsIgnoreCase("range")) queryBean.setTemporalSelection(TemporalSelection.range); //temporalSelection = TemporalSelection.range;
		else if (temporal.equalsIgnoreCase("point")) queryBean.setTemporalSelection(TemporalSelection.point); //temporalSelection = TemporalSelection.point;

		if (temporal.equalsIgnoreCase("range")) {
			try {
				parseTimeExtent(queryBean);
			} catch (Throwable t) {
				//errs.format("badly specified time range");
				context.buildConstraintViolationWithTemplate("{thredds.server.cdmremote.validation.error.wrongtimerange}").addConstraintViolation();
				isValid = false;
				return;
			}
		} else if (temporal.equalsIgnoreCase("point")) {
			//timePoint = parseDate("time", time);
			queryBean.setTimepoint( queryBean.parseDate("time", queryBean.getTime() ) );
		}
	}

	private void parseTimeExtent(CdmrfQueryBean queryBean) {
		DateType startDate = queryBean.parseDate("time_start", queryBean.getTime_start());
		DateType endDate = queryBean.parseDate("time_end", queryBean.getTime_end());
		TimeDuration duration = queryBean.parseW3CDuration("time_duration", queryBean.getTime_duration() );

		// no range
		if ((startDate != null) && (endDate != null))
			//dateRange = new DateRange(startDate, endDate, null, null);
			queryBean.setDateRange( new DateRange(startDate, endDate, null, null) );
		else if ((startDate != null) && (duration != null))
			//dateRange = new DateRange(startDate, null, duration, null);
			queryBean.setDateRange(new DateRange(startDate, null, duration, null));
		else if ((endDate != null) && (duration != null))
			//dateRange = new DateRange(null, endDate, duration, null);
			queryBean.setDateRange(new DateRange(null, endDate, duration, null));
		if (queryBean.getDateRange() != null)
			//temporalSelection = TemporalSelection.range;
			queryBean.setTemporalSelection(TemporalSelection.range);
	}	  

}
