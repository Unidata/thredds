package thredds.servlet.idd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import java.util.LinkedHashMap;
//import java.util.Iterator;
//import java.util.HashMap;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * Created by IntelliJ IDEA.
 * User: rkambic
 * Date: Jan 24, 2007
 * Time: 4:14:54 PM
 * To change this template use File | Settings | File Templates.
 */


public class MetarObservation {

    static final int missingValue = -9999;

    static final Pattern Station = Pattern.compile("^(\\w{4})\\s+");

    static final Pattern ddhhmmZ = Pattern.compile("(\\d{2})(\\d{2})(\\d{2})Z\\s+");
    static final Pattern Wind_direction_speed = Pattern.compile("(E|W|N|S)?(\\d{3}|VRB)(\\d{2,3})(G)?(\\d{2,3})?(KMH|KT|MPS|MPH)\\s+");

    static final Pattern WeatherPrecip = Pattern.compile("(\\+|-|VC|PR| )(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)\\s+");

    static final Pattern WeatherObs = Pattern.compile("(\\+|-|VC|PR| )(MI|BC|DR|BL|SH|TS|FZ)?(BR|FG|FU|VA|DU|SA|HZ|PY)\\s+");

    static final Pattern WeatherOther = Pattern.compile("(\\+|-|VC|PR| )(MI|BC|DR|BL|SH|TS|FZ)?(PO|SQ|FC|SS|DS)\\s+");
    //static final Pattern Weather = Pattern.compile("(\\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?\\s+");

    static final Pattern Temperature_tenths = Pattern.compile("T(0|1)(\\d{3})(0|1)?(\\d{3})?\\s+");

    static final Pattern Temperature = Pattern.compile("(M|-)?(\\d{2})/(M|-)?(\\d{2})?\\s*");

    static final Pattern Altimeter = Pattern.compile("(A|Q)(\\d{4}\\.?\\d?)\\s*");
    static final Pattern SLP = Pattern.compile("SLP\\s?(\\d{3})\\s+");

    static final Pattern Hourly_precip = Pattern.compile("P ?(\\d{1,5})\\s+");

    static final Pattern Metar = Pattern.compile("^(METAR|SPECI|TESTM|TESTS) ");

    static protected SimpleDateFormat dateFormatISO;

    static {
        dateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormatISO.setTimeZone(TimeZone.getTimeZone("GMT")); // same as UTC
    }


    protected String key;

    protected String station = null;

    protected String dateISO = null;

    protected long timeObs;

    protected int wind_direction = missingValue;

    protected float wind_speed = missingValue;

    protected float wind_gust = missingValue;

    protected String weather = "";

    protected float air_temperature = missingValue;

    protected float dew_point_temperature = missingValue;

    protected float altimeter_inches = missingValue;

    protected float altimeter_hectopascals = missingValue;

    protected float sea_level_pressure = missingValue;

    protected float hourly_precipitation = missingValue;

    protected String report;

    protected int length;

    public MetarObservation() {};

    public MetarObservation( MetarObservation mo ) {

        this.key = mo.key;
        this.station = mo.station;
        this.dateISO = mo.dateISO;
        this.timeObs = mo.timeObs;
        this.wind_direction = mo.wind_direction;
        this.wind_speed = mo.wind_speed;
        this.wind_gust = mo.wind_gust;
        this.weather = mo.weather;
        this.air_temperature = mo.air_temperature;
        this.dew_point_temperature = mo.dew_point_temperature;
        this.altimeter_inches = mo.altimeter_inches;
        this.altimeter_hectopascals = mo.altimeter_hectopascals;
        this.sea_level_pressure = mo.sea_level_pressure;
        this.hourly_precipitation = mo.hourly_precipitation;
        this.report = mo.report;
        this.length = mo.length;

    }

    public boolean decode(String input) {

        Matcher m;

//	$rep_type = $1 if( s#^(METAR|SPECI|TESTM|TESTS) ## ) ;
        m = Metar.matcher(input);
        if (m.find()) {
            input = m.replaceFirst("");
        }

        report = input;
        length = input.length();
        key = input.substring( 0, 13 );

//	$stn_name = $1 if( s#^(\w4) ## ) ;
//	next unless( $stn_name ) ;
        m = Station.matcher( input );
        if (m.find()) {
            station = m.group(1);
            input = m.replaceFirst("");
        } else {
            return false;
        }

        
        Calendar cal = Calendar.getInstance( java.util.TimeZone.getTimeZone("GMT"));
        int year = cal.get( Calendar.YEAR );
        int month = cal.get( Calendar.MONTH );
        int curDay = cal.get( Calendar.DAY_OF_MONTH );
        int curHour = cal.get( Calendar.HOUR_OF_DAY );
        //System.out.println( "Year ="+ year +" Month ="+ month );
        // get day hour minute
//	if( s#^(\d2)(\d2)(\d2)Z## )
        m = ddhhmmZ.matcher( input );
        if (m.find()) {
//		$rday = $1 ;
            int day = Integer.parseInt( m.group(1) );
//		$rhour = $2 ;
            int hour = Integer.parseInt( m.group(2) );
//		$rmin = $3 ;
            int minute = Integer.parseInt( m.group(3) );
            input = m.replaceFirst("");
            //System.out.println( "day ="+ day +" curDay ="+ curDay
            //+" Hour ="+ hour +" curHour ="+ curHour +" Min ="+ minute);
            // check report times, optimized for current time reports 
            if( day == curDay ) {
                    if( hour > curHour ) return false; // in the future
            } else if( day < (curDay -1) ) { // older than 1 day
                    return false;
            } else if( day > curDay ) { // only permit 1st of the month
                    if( curDay != 1 || day < 28) return false;
                    // work here need to set month and year
            } else if( hour < curHour ) {
                    return false; // 1 day older by hours
            }
            cal.set( year, month, day, hour, minute, 0 );
            timeObs = cal.getTimeInMillis(); // / 1000;
            dateISO = dateFormatISO.format( cal.getTime() );

            //System.out.println( "DataISO ="+ dateISO );

        } else {
            return false;
        }

        // get wind direction and speed
//	if( s#(E|W|N|S)?(\d3|VRB)(\d2,3)(G)?(\d2,3)?(KMH|KT|MPS|MPH)\s+## )
        m = Wind_direction_speed.matcher(input);
        if (m.find()) {
//		if( $2 eq "VRB" )
            if (m.group(2).equals("VRB")) {
//			$VRB = 1 ;
/*  needs work E=90,S=180......
                if( m.group(1) == null ) {
                    field.put("Variable_Wind_direction", "" );
                } else {
                    field.put("Variable_Wind_direction", m.group(1));
                }
*/
            } else {
//			$DIR = $2 ;
                wind_direction = Integer.parseInt( m.group(2));
//
            }
//		$SPD = $3 ;
            wind_speed = Float.parseFloat( m.group(3));
//		$GUST = $5 if( $4 eq "G" ) ;
            if (m.group(4) != null && m.group(4).equals("G")) {
                wind_gust = Float.parseFloat( m.group(5));
            }
//		$UNITS = $6 ; need work  if units != KT
            //Wind_Units", m.group(6));
            input = m.replaceFirst("");
        }

// 	Get weather conditions
// 	code table 4678
        boolean done = true;
        StringBuilder WX = new StringBuilder();
        for (int i = 0; i < 4; i++) {
//          if( s#(\+|-|VC|PR| )(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)\s+## )
            m = WeatherPrecip.matcher(input);
            //System.out.println( "before if input ="+ input );
            if (m.find()) {
                done = false;
                //System.out.println( "after if input ="+ input );
                
                //if (m.group(1) != null && ! m.group(1).equals( " " )) {
                if (! m.group(1).equals( " " )) {
                    WX.append( m.group(1) );
                }
                if (m.group(2) != null) {
                    WX.append( m.group(2) );
                }
                //if (m.group(3) != null) {
                    WX.append( m.group(3) );
                //}
                input = m.replaceFirst("");
            }
//          if( s#(\+|-|VC|PR| )(MI|BC|DR|BL|SH|TS|FZ)?(BR|FG|FU|VA|DU|SA|HZ|PY)\s+## )
            m = WeatherObs.matcher(input);
            if (m.find()) {
                done = false;
                //System.out.println( "after if input ="+ input );
                
                //if (m.group(1) != null && ! m.group(1).equals( " " )) {
                if (! m.group(1).equals( " " )) {
                    WX.append( m.group(1) );
                }
                if (m.group(2) != null) {
                    WX.append( m.group(2) );
                }
                //if (m.group(3) != null) {
                    WX.append( m.group(3) );
                //}
                input = m.replaceFirst("");
            }
//          if( s#(\+|-|VC|PR| )(MI|BC|DR|BL|SH|TS|FZ)?(PO|SQ|FC|SS|DS)\s+## )
            m = WeatherOther.matcher(input);
            if (m.find()) {
                done = false;
                //System.out.println( "after if input ="+ input );
                
                //if (m.group(1) != null && ! m.group(1).equals( " " )) {
                if (! m.group(1).equals( " " )) {
                    WX.append( m.group(1) );
                }
                if (m.group(2) != null) {
                    WX.append( m.group(2) );
                }
                //if (m.group(3) != null) {
                    WX.append( m.group(3) );
                //}
                input = m.replaceFirst("");
            }
            if( done )
                break;
            done = true;
            WX.append( " " );
        }
        if (WX.length() > 0) {
            WX.setLength( WX.length() -1 );
            weather = WX.toString();
        }

// 	Temperature and Dew Point
//	if( s#T(0|1)(\d3)(0|1)?(\d3)?\s+## )
        m = Temperature_tenths.matcher(input);
        if (m.find()) {
//		if( $1 == 0 ) 
//			$T_tenths = 0.1 * $2 ;
//	 	else 
//			$T_tenths = -0.1 * $2 ;
//
            //String T = Double.toString(Float.parseFloat(m.group(2)) * .1);
            air_temperature = (float)(Float.parseFloat(m.group(2)) * .1);
            if (m.group(1).equals("1")) {
                //T = "-" + T;
                air_temperature *= -1;
            }
            //field.put("Temperature", T);

//		if( defined( $3 ) && $3 == 0 ) 
//			$TD_tenths = 0.1 * $4 ;
//	 	elsif( defined( $3 ) && $3 == 1 ) 
//			$TD_tenths = -0.1 * $4 ;

            if (m.group(3) != null) {
                //String TD = Double.toString(Float.parseFloat(m.group(4)) * .1);
                dew_point_temperature = (float)(Float.parseFloat(m.group(4)) * .1);
//			$TD *= -1 if( $3 ) ;
                if (m.group(3).equals("1")) {
                    //TD = "-" + TD;
                    dew_point_temperature *= -1;
                }
                //field.put("DewPoint", TD);
            }
            input = m.replaceFirst("");
        } else { // check for course temperature

// 	get temperature and dew point
//	if( s#^(M)?(\d2)/(M)?(\d2)?\s+## )
        m = Temperature.matcher(input);
        if (m.find()) {
//		$T = $2 ;
            //String T = m.group(2);
            air_temperature = Float.parseFloat(m.group(2));
//		$T *= -1 if( $1 ) ;
            //if(  m.group( 1 ).equals( "M" )  )
            if (m.group(1) != null) {
                //T = "-" + T;
                air_temperature *= -1;
            }
            //field.put("Temperature", T);
//		$TD = $4 if( defined( $4 ) ) ;
            if (m.group(4) != null) {
                //String TD = m.group(4);
                dew_point_temperature = Float.parseFloat(m.group(4));
//			$TD *= -1 if( $3 ) ;
                if (m.group(3) != null) {
                    //TD = "-" + TD;
                    dew_point_temperature *= -1;
                }
                //field.put("DewPoint", TD);
            }
            input = m.replaceFirst("");
        } // end T and TD
        }

// 	get Altimeter settings
//	if( s#^(A|Q)(\d4\.?\d?)\s+## )
        m = Altimeter.matcher(input);
        if (m.find()) {
//		if( $1 eq "A" )
            if (m.group(1).equals("A")) {
//			$inches_ALTIM = $2 * 0.01 ;
                altimeter_inches = (float)(Float.parseFloat(m.group(2)) * .01);
            } else {
//			$hectoPasc_ALTIM = $2 ;
                altimeter_hectopascals = (float)(Float.parseFloat(m.group(2)));
            }
            input = m.replaceFirst("");
        }

        //if( s#SLP\s?(\d3)\s+## )
        m = SLP.matcher(input);
        if (m.find()) {
            //	if( $1 >= 550 )
            if (Integer.parseInt(m.group(1)) >= 550) {
                //		$SLP = $1 / 10. + 900. ;
                //field.put("Sea_Level_Pressure", Double.toString(Float.parseFloat(m.group(1)) * 0.1 + 900));
                sea_level_pressure = (float)(Float.parseFloat(m.group(1)) * 0.1 + 900);

            } else {
                //		$SLP =  $1 / 10. + 1000. ;
                //field.put("Sea_Level_Pressure", Double.toString(Float.parseFloat(m.group(1)) * 0.1 + 1000));
                sea_level_pressure = (float)(Float.parseFloat(m.group(1)) * 0.1 + 1000);
            }
            input = m.replaceFirst("");
        }

// 	Hourly precipitation amount
//	$PRECIP_hourly = $1 / 100 if( s#P ?(\d1,5)\s+## ) ;
        m = Hourly_precip.matcher(input);
        if (m.find()) {
            //field.put("Hourly_Precipitation", Double.toString(Float.parseFloat(m.group(1)) * .01));
            hourly_precipitation = (float)(Float.parseFloat(m.group(1)) * .01);
            input = m.replaceFirst("");
        }

        return true;
    }

    /**
     *
     * returns original Metar observation
     * @return String
     */
     public String toString() {
         return "key ="+ key
         +"\nreport ="+ report
         +"\nstation ="+ station
         +"\ndateISO ="+ dateISO
         +"\nwind_direction ="+ wind_direction
         +"\nwind_speed ="+ wind_speed
         +"\nwind_gust ="+ wind_gust
         +"\nweather ="+ weather
         +"\nair_temperature ="+ air_temperature
         +"\ndew_point_temperature ="+ dew_point_temperature
         +"\naltimeter_inches ="+ altimeter_inches
         +"\naltimeter_hectopascals ="+ altimeter_hectopascals
         +"\nsea_level_pressure ="+ sea_level_pressure
         +"\nhourly_precipitation ="+ hourly_precipitation ;

     }
}
