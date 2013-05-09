package thredds.server.ncSubset.validation;

import java.text.ParseException;
import java.util.Date;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import thredds.server.ncSubset.params.RequestParamsBean;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.units.TimeDuration;


/**
 * 
 *  Time params validator for a ncss request. 
 *   
 * @author mhermida
 *
 */
public class TimeParamsValidator implements ConstraintValidator<TimeParamsConstraint, RequestParamsBean> {

	public void initialize(TimeParamsConstraint constraintAnnotation){
		
	}
	
	public boolean isValid(RequestParamsBean params, ConstraintValidatorContext constraintValidatorContext) {
		
		constraintValidatorContext.disableDefaultConstraintViolation();
		
		boolean isValid =true;
		String time = params.getTime();
		String time_window = params.getTime_window();
		
		if( time !=null ){
			
			if( time_window != null){
				try{
					TimeDuration tdTime_window;
					tdTime_window =new TimeDuration(time_window);
					
				}catch(ParseException pe){
					isValid = false;
					constraintValidatorContext
					.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.ncsstimeparamsvalidator.message.parseerror.param.time_window}")
					.addConstraintViolation();					
				}	
			}			
			
			return isValid && validateISOString( time,"{thredds.server.ncSubset.validation.ncsstimeparamsvalidator.message.parseerror.param.time}", constraintValidatorContext);
			
			
		}
		
		String time_start = params.getTime_start();
		String time_end = params.getTime_end();
		String time_duration = params.getTime_duration();
		
		
		//if all of them are null --> returns the whole time series
		//so all null are valid parameters
		if(time_start==null & time_end==null && time_duration==null   ) return true;
		
		if(!hasValidDateRange(time_start, time_end, time_duration)){		
			constraintValidatorContext
				.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.ncsstimeparamsvalidator.message.twoofthree}")
				.addConstraintViolation();
				isValid = false;
		}else{
			//check the formats		
			if( time_start != null){				
				isValid = validateISOString( time_start,"{thredds.server.ncSubset.validation.ncsstimeparamsvalidator.message.parseerror.param.time_start}", constraintValidatorContext);
			}
				
			if( time_end != null){
				isValid = validateISOString( time_end,"{thredds.server.ncSubset.validation.ncsstimeparamsvalidator.message.parseerror.param.time_end}", constraintValidatorContext) && isValid;
			}
								
			if( time_duration != null){
				try{
					TimeDuration tdTime_duration;
					tdTime_duration =new TimeDuration(time_duration);
					
				}catch(ParseException pe){
					isValid = false;
					constraintValidatorContext
					.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.ncsstimeparamsvalidator.message.parseerror.param.time_duration}")
					.addConstraintViolation();					
				}	
			}
									
			//check time_start < time_end
			if( isValid && time_start != null && time_end != null  ){
				Date start = isoString2Date(time_start);
				Date end = isoString2Date(time_end);
				
				if( start.after(end) ){
					isValid = false;
					constraintValidatorContext
					.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.ncsstimeparamsvalidator.message.start_gt_end}")
					.addConstraintViolation();					
				}
			}
			
		}

		return isValid;
	}
	
	  /**
	   * 
	   * Determine if a valid date range was specified
	   * 
	   * @param time_start
	   * @param time_end
	   * @param time_duration
	   * 
	   * @return @return true if there is a valid date range, false if not.
	   */
	  private boolean hasValidDateRange(String time_start, String time_end, String time_duration) {
	    // no range
	    if ((null == time_start) && (null == time_end) && (null == time_duration))
	      return false;

	    if ((null != time_start) && (null != time_end))
	      return true;

	    if ((null != time_start) && (null != time_duration))
	      return true;

	    if ((null != time_end) && (null != time_duration))
	      return true;

	    // misformed range
	    //errs.append("Must have 2 of 3 parameters: time_start, time_end, time_duration\n");
	    return false;

	  }
	  
	  private boolean validateISOString(String isoString, String msg, ConstraintValidatorContext constraintValidatorContext){
		    		  		  	
		  	if("present".equals(isoString)) return true;
		  	
		  	boolean isValid = true;
		  	Date date= null;
		  	
		  	try {
		  		date = isoString2Date(isoString);
		  		
			} catch (IllegalArgumentException iea) {
				//Invalid format for param time!!!
				isValid = false;
				constraintValidatorContext
				.buildConstraintViolationWithTemplate(msg)
				.addConstraintViolation();				
			}		  			  
		  	return isValid;  
	  }
	  
	  private Date isoString2Date(String isoString){
		  
		  if("present".equals(isoString)) return new Date();
		  CalendarDate cd = CalendarDateFormatter.isoStringToCalendarDate(Calendar.getDefault(), isoString);		  
		  return new Date(cd.getMillis());
	  }
}
