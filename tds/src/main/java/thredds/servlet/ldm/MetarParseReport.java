/*
 * MetarParseReport
 *
 * parses one METAR report into it's variables
 *
 */
package thredds.servlet.ldm;

import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Matcher;

public class MetarParseReport {

    public LinkedHashMap parseReport(String input) {

        LinkedHashMap metar = new LinkedHashMap();
        ThreddsServerPatterns p = new ThreddsServerPatterns();
        Matcher m;
        float var1, var2, var3;

//	next if( /\d4,6Z.*\d4,6Z/ ) ;
        if (p.p_bad_date.matcher(input).find())
            return null;
//	next if( /^\n/ ) ;
        if (p.p_B_CR.matcher(input).find())
            return null;

//	s#\n|\s+# #g ;

//	$_ = "$_ " ;
        input = input + " ";

//	$rep_type = $bulletin_type ;
        metar.put("Report_Type", "METAR");
//	$rep_type = $1 if( s#^(METAR|SPECI|TESTM|TESTS) ## ) ;
        m = p.p_B_metar.matcher(input);
        if (m.find()) {
            metar.put("Report_Type", m.group(1));
            input = m.replaceFirst("");
        }
//	$stn_name = $1 if( s#^(\w4) ## ) ;
//	next unless( $stn_name ) ;
        m = p.p_station.matcher(input);
        if (m.find()) {
            metar.put("Station", m.group(1));
            //input = p.p_B_W4.matcher( input ).replaceFirst( "" );
            input = m.replaceFirst("");
        } else {
            return null;
        }
        // get day hour minute
//	if( s#^(\d2)(\d2)(\d2)Z## )
        m = p.p_ddhhmmZ.matcher(input);
        if (m.find()) {
//		$rday = $1 ;
            metar.put("Day", m.group(1));
//		$rhour = $2 ;
            metar.put("Hour", m.group(2));
//		$rmin = $3 ;
            metar.put("Minute", m.group(3));
            input = m.replaceFirst("");
        } else {
            return null;
        }

        // skip NIL reports
//	next if( s#NIL\s*## ) ;
        if (p.p_NILS.matcher(input).find())
            return null;

        m = p.p_COR.matcher(input);
        if (m.find()) {
            input = m.replaceFirst("");
        }

//	$AUTO = 1 if( s#AUTO\s+## ) ;
        m = p.p_AUTOS.matcher(input);
        if (m.find()) {
            metar.put("AUTOS", "1");
            input = m.replaceFirst("");
        }

        // get wind direction and speed
//	if( s#(E|W|N|S)?(\d3|VRB)(\d2,3)(G)?(\d2,3)?(KMH|KT|MPS|MPH)\s+## )
        m = p.p_wind_direction_speed.matcher(input);
        if (m.find()) {
//		if( $2 eq "VRB" )
            if (m.group(2).equals("VRB")) {
//			$VRB = 1 ;
                if( m.group(1) == null ) {
                    metar.put("Variable_Wind_direction", "1" );
                } else {
                    metar.put("Variable_Wind_direction", m.group(1));
                }
            } else {
//			$DIR = $2 ;
                metar.put("Wind_Direction", m.group(2));
//
            }
//		$SPD = $3 ;
            metar.put("Wind_Speed_" +  m.group(6), m.group(3));
//		$GUST = $5 if( $4 eq "G" ) ;
            if (m.group(4) != null && m.group(4).equals("G")) {
                metar.put("Wind_Gust_" +  m.group(6), m.group(5));
            }
//		$UNITS = $6 ;
            //metar.put("Wind_Units", m.group(6));
            input = m.replaceFirst("");
//
        }
        // get min|max wind direction
//	if( s#^(\d3)V(\d3)\s+## )
        m = p.p_min_max_wind_dir.matcher(input);
        if (m.find()) {
//		$DIRmin = $1 ;
            metar.put("Wind_Direction_Min", m.group(1));
//		$DIRmax = $2 ;
            metar.put("Wind_Direction_Max", m.group(2));
            input = m.replaceFirst("");
        }
        // some reports use a place holder for visibility
        //		s#9999\s+## ;
        input = p.p_9999S.matcher(input).replaceFirst("");

        // get visibility
//	$prevail_VIS_SM = 0.0 if( s#^M1/4SM\s+|<1/4SM\s+## ) ;
        m = p.p_visibilitySM.matcher(input);
        if (m.find()) {
            metar.put("Visibility_SM", "0.0");
            input = m.replaceFirst("");
        }

//	$prevail_VIS_KM = 0.0 if( s#^M1/4KM\s+|<1/4KM\s+## ) ;
        m = p.p_visibilityKM.matcher(input);
        if (m.find()) {
            metar.put("Visibility_KM", "0.0");
            input = m.replaceFirst("");
        }

//	$plus_VIS_SM = 1 if( s#^P(\d1,3)SM\s+#$1SM # ) ;
        m = p.p_B_P.matcher(input);
        if (m.find()) {
            metar.put("Plus_Visibility", "1");
            input = m.replaceFirst("");
        }

//	$plus_VIS_KM = 1 if( s#^P(\d1,3)KM\s+#$1KM # ) ;
        //m = p.p_B_P.matcher(input);
        //if (m.find()) {
            //metar.put("Plus_Visibility_KM", "1");
            //input = m.replaceFirst("");
        //}

//	if( s#^(\d1,4) (\d1,3)/(\d1,3)(SM|KM)\s+## )
        m = p.p_visibility1.matcher(input);
        if (m.find()) {
//		$prevail_VIS_SM = $1 + ( $2 / $3 ) if( $4 eq "SM" ) ;
            var1 = Float.parseFloat(m.group(1));
            var2 = Float.parseFloat(m.group(2));
            var3 = Float.parseFloat(m.group(3));
            var1 = var1 + (var2 / var3);
            if (m.group(4).equals("SM")) {
                metar.put("Visibility_SM", Float.toString(var1));
			
//		$prevail_VIS_KM = $1 + ( $2 / $3 ) if( $4 eq "KM" ) ;
            } else {
                metar.put("Visibility_KM", Float.toString(var1));
            }
            input = m.replaceFirst("");

//	 elsif( s#^(\d1,3)/(\d1,3)(KM|SM)\s+## )
        } else {
            m = p.p_visibility2.matcher(input);
            if (m.find()) {
                var1 = Float.parseFloat(m.group(1));
                var2 = Float.parseFloat(m.group(2));
                var1 = var1 / var2;
//			$prevail_VIS_SM = $1 / $2  if( $3 eq "SM" ) ;
                if (m.group(3).equals("SM")) {
                    metar.put("Visibility_SM", Float.toString(var1));
			
//			$prevail_VIS_KM = $1 / $2  if( $3 eq "KM" ) ;
                } else {
                    metar.put("Visibility_KM", Float.toString(var1));
                }
                input = m.replaceFirst("");

//		 elsif( s#^(\d1,4)(KM|SM)\s+## )
            } else {
                m = p.p_visibility3.matcher(input);
                if (m.find()) {
//				$prevail_VIS_SM = $1 if( $2 eq "SM" ) ;
                    if (m.group(2).equals("SM")) {
                        metar.put("Visibility_SM", m.group(1));
//				$prevail_VIS_KM = $1 if( $2 eq "KM" ) ;
                    } else {
                        metar.put("Visibility_KM", m.group(1));
                    }
                    input = m.replaceFirst("");

//		 	elsif( s#^(\d4)((NE)|(NW)|(SE)|(SW)|(N)|(S)|(E)|(W))\s+## )
                } else {
                    m = p.p_visibility_direction.matcher(input);
                    if (m.find()) {
//					$prevail_VIS_M = $1 ;
                        metar.put("Visibility_M", m.group(1));
//					$VIS_dir = $2 ;
                        metar.put("Visibility_Direction", m.group(2));
                        input = m.replaceFirst("");
                    }
                }
            }
        }
// 	clear
//	$CAVOK = 1 if( s#CAVOK\s+## ) ;
        m = p.p_CAVOKS.matcher(input);
        if (m.find()) {
            metar.put("Clear_Air", "1");
            input = m.replaceFirst("");
        }

// 	runway decoding here
//	$RVRNO = 1 if( s#RVRNO\s+## ) ;
        m = p.p_RVRNOS.matcher(input);
        if (m.find()) {
            metar.put("RVRNOS", "1");
            input = m.replaceFirst("");
        }
//	for( $i = 0; $i < 4; $i++ )
        for (int i = 0; i < 4; i++) {
//		if( s#R(\d2)(R|L|C)?/(M|P)?(\d1,4)V?(M|P)?(\d1,4)?(FT|N|D)?\s+## )
            m = p.p_runway.matcher(input);
            if (m.find()) {
//			$RV_designator[ $i ] = "$1$2" ;
                String RV = "RV" + Integer.toString(i +1);
                metar.put(RV, m.group(1) + m.group(2));

//			$RV_above_max[ $i ] = 1 
//				if( $3 eq "P" || $5 eq "P" ) ;
                if ((m.group(3) != null && m.group(3).equals("P")) || 
                    (m.group(5) != null && m.group(5).equals("P")))
                    metar.put(RV + "_Above_Max", "1");

//			$RV_below_min[ $i ] = 1 
//				if( $3 eq "M" || $5 eq "M" ) ;
                if ((m.group(3) != null && m.group(3).equals("M")) || 
                    (m.group(5) != null && m.group(5).equals("M")))
                    metar.put(RV + "_Below_Min", "1");

//			$RV_vrbl[ $i ] = 1 if( $6 ne "" ) ;
//			if( $RV_vrbl[ $i ] ) 
//				$RV_min[ $i ] = $4 * 1;
//				$RV_max[ $i ] = $6 * 1;
                if (m.group(6) != null) {
                    metar.put(RV + "_Vrbl", "1");
                    metar.put(RV + "_Min_FT", m.group(4));
                    metar.put(RV + "_Max_FT", m.group(6));
                } else {
//				$RV_visRange[ $i ] = $4 * 1;
                    if(m.group(7) != null && m.group(7).equals( "FT")) {
                        metar.put(RV + "_Visibility_Range_FT", m.group(4));
                    } else {
                        metar.put(RV + "_Visibility_Range", m.group(4));
                    }
                }
                input = m.replaceFirst("");
            } else {
                break;
            }
        } // end runway decoding

// 	Get weather conditions
// 	code table 4678
//	for( $i = 0; $i < 4; $i++ )
        String WX = "";
        for (int i = 0; i < 4; i++) {
//		if( s#^(\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?\s+## )
            m = p.p_weather.matcher(input);
            if (m.find()) {
//			last unless "$1$2$3$4$5" ;
                String tmp = "";
                if (m.group(1) != null) {
                    tmp = tmp + m.group(1);
                }
                if (m.group(2) != null) {
                    tmp = tmp + m.group(2);
                }
                if (m.group(3) != null) {
                    tmp = tmp + m.group(3);
                }
                if (m.group(4) != null) {
                    tmp = tmp + m.group(4);
                }
                if (m.group(5) != null) {
                    tmp = tmp + m.group(5);
                }
                if (tmp.equals("")) {
                    break;
                }
//			$WX .= "$1$2$3$4$5 " ;
                if( WX.equals( "" )) {
                    WX = tmp;
                } else {
                    WX = WX +" "+ tmp;
                }
                input = m.replaceFirst("");
            } else {
//			last ;
                break;
            }
        }
        if (WX.length() > 0) {
            metar.put("Weather", WX);
        }
// 	Interpret cloud conditions
//	$cloud_type[ 0 ] = $1 if( s#(CLR|SKC)\s+## ) ;
        m = p.p_CLR_or_SKC.matcher(input);
        if (m.find()) {
            metar.put("Cloud_Type", "1");
            input = m.replaceFirst("");
        }
//	$vert_VIS = cloud_hgt2_meters( $1 ) if( s#^VV(\d3)\s+## ) ;
        m = p.p_vertical_VIS.matcher(input);
        if (m.find()) {
            metar.put("Vertical_Visibility", cloud_hgt2_meters(m.group(1)));
            input = m.replaceFirst("");
        }

//	for( $i = 0; $i < 6; $i++ )
        for (int i = 0; i < 6; i++) {
// 	cloud layers up to 6
//		if( s#^(\+|-)?(OVC|SCT|FEW|BKN)(\d3)(\w1,3)?\s+## )
            m = p.p_cloud_cover.matcher(input);
            if (m.find()) {
                String cloud = "Cloud_Layer_" + Integer.toString(i + 1);
//			$cloud_type[ $i ] = padstr( "$1$2", 4 ) ;
                if (m.group(1) == null) {
                    metar.put(cloud + "_Type", m.group(2));
                } else {
                    metar.put(cloud + "_Type", m.group(1) + m.group(2));
                }
//			$cloud_hgt[ $i ] = $3 * 100 ;
                metar.put(cloud + "_Height_Feet", Integer.toString(Integer.parseInt(m.group(3)) * 100));

//			$cloud_meters[ $i ] = cloud_hgt2_meters( $3 ) ;
                metar.put(cloud + "_Height_Meters", cloud_hgt2_meters(m.group(3)));
//			$cloud_phenom[ $i ] = padstr( $4, 4 ) if( $4 ) ;
                if (m.group(4) != null) {
                    metar.put(cloud + "_Phenom", m.group(4));
                }
                input = m.replaceFirst("");
            } else {
                break;
            }
        } // end clouds

// 	get temperature and dew point
//	if( s#^(M)?(\d2)/(M)?(\d2)?\s+## )
        m = p.p_temperature.matcher(input);
        if (m.find()) {
//		$T = $2 ;
            String T = m.group(2);
//		$T *= -1 if( $1 ) ;
            //if(  m.group( 1 ).equals( "M" )  )
            if (m.group(1) != null) {
                T = "-" + T;
            }
            metar.put("Temperature", T);
//		$TD = $4 if( defined( $4 ) ) ;
            if (m.group(4) != null) {
                String TD = m.group(4);
//			$TD *= -1 if( $3 ) ;
                if (m.group(3) != null) {
                    TD = "-" + TD;
                }
                metar.put("DewPoint", TD);
            }
            input = m.replaceFirst("");
        } // end T and TD

// 	get Altimeter settings
//	if( s#^(A|Q)(\d4\.?\d?)\s+## )
        m = p.p_altimeter.matcher(input);
        if (m.find()) {
//		if( $1 eq "A" )
            if (m.group(1).equals("A")) {
//			$inches_ALTIM = $2 * 0.01 ;
                metar.put("Inches_Altimeter", Double.toString(Float.parseFloat(m.group(2)) * 0.01));
            } else {
//			$hectoPasc_ALTIM = $2 ;
                metar.put("HectoPasc_Altimeter", m.group(2));
            }
            input = m.replaceFirst("");
        }

//	$NOSIG = 1 if( s#NOSIG## ) ;
        m = p.p_NOSIG.matcher(input);
        if (m.find()) {
            metar.put("No_Weather", "1");
            input = m.replaceFirst("");
        }

// 	check for remarks or done
        m = p.p_REMARKS.matcher(input);
        if (m.find()) {
            input = m.replaceFirst("");
        } else {
            if(input.length() > 0)
                metar.put("Plain_Language_remarks", input);
            return metar;
        }

        // process remarks now, looking for most used ones first

        // get Automated reports
//	 if( s#(A01|A01A|A02|A02A|AO1|AO1A|AO2|AO2A|AOA)\s+## ) ;
//		$AUTOindicator = padstr( $1, 4 )
        m = p.p_automatic_report.matcher(input);
        if (m.find()) {
            metar.put("Automatic_Report", m.group(1));
            input = m.replaceFirst("");
        }

//	check if no more info in report
        if (input.equals("") || p.p_spaces.matcher(input).matches()) {
            return metar;
        }

        // Sea-Level presure
        //$SLPNO = 1 if( s#SLPNO\s+## ) ;
        m = p.p_SLPNO.matcher(input);
        if (m.find()) {
            metar.put("SLPNO", "1");
            input = m.replaceFirst("");
        }

        //if( s#SLP\s?(\d3)\s+## )
        m = p.p_SLP.matcher(input);
        if (m.find()) {
            float slp;
            //	if( $1 >= 550 )
            if (Integer.parseInt(m.group(1)) >= 550) {
                //		$SLP = $1 / 10. + 900. ;
                metar.put("Sea_Level_Pressure", Double.toString(Float.parseFloat(m.group(1)) * 0.1 + 900));

            } else {
                //		$SLP =  $1 / 10. + 1000. ;
                metar.put("Sea_Level_Pressure", Double.toString(Float.parseFloat(m.group(1)) * 0.1 + 1000));
            }
            input = m.replaceFirst("");
        }
//	check if no more info in report
        if (input.equals("") || p.p_spaces.matcher(input).matches()) {
            return metar;
        }

// 	Temperature and Dew Point
//	if( s#T(0|1)(\d3)(0|1)?(\d3)?\s+## )
        m = p.p_temperature_tenths.matcher(input);
        if (m.find()) {
//		if( $1 == 0 ) 
//			$T_tenths = 0.1 * $2 ;
//	 	else 
//			$T_tenths = -0.1 * $2 ;
//
            String T = Double.toString(Float.parseFloat(m.group(2)) * .1);
            if (m.group(1).equals("1")) {
                T = "-" + T;
            }
            metar.put("Temperature", T);

//		if( defined( $3 ) && $3 == 0 ) 
//			$TD_tenths = 0.1 * $4 ;
//	 	elsif( defined( $3 ) && $3 == 1 ) 
//			$TD_tenths = -0.1 * $4 ;

            if (m.group(3) != null) {
                String TD = Double.toString(Float.parseFloat(m.group(4)) * .1);
//			$TD *= -1 if( $3 ) ;
                if (m.group(3).equals("1")) {
                    TD = "-" + TD;
                }
                metar.put("DewPoint", TD);
            }
            input = m.replaceFirst("");
        } // end Temperature and Dew Point

//	check if no more info in report
        if (input.equals("") || p.p_spaces.matcher(input).matches()) {
            return metar;
        }

// 	Hourly precipitation amount
//	$PRECIP_hourly = $1 / 100 if( s#P ?(\d1,5)\s+## ) ;
        m = p.p_hourly_precip.matcher(input);
        if (m.find()) {
            metar.put("Hourly_Precipitation", Double.toString(Float.parseFloat(m.group(1)) * .01));
            input = m.replaceFirst("");
        }

//	check if no more info in report
        if (input.equals("") || p.p_spaces.matcher(input).matches()) {
            return metar;
        }

        // precipitation sensor not working  PWINO
        //$PWINO = 1 if( s#PWINO\s+## ) ;
        m = p.p_PWINO.matcher(input);
        if (m.find()) {
            metar.put("PWINO", "1");
            input = m.replaceFirst("");
        }

//	check if no more info in report
        if (input.equals("") || p.p_spaces.matcher(input).matches()) {
            return metar;
        }

// 	Lightning detection sensor not working  TSNO
//	$TSNO = 1 if( s#TSNO\s+## ) ;
        m = p.p_TSNO.matcher(input);
        if (m.find()) {
            metar.put("TSNO", "1");
            input = m.replaceFirst("");
        }

//	check if no more info in report
        if (input.equals("") || p.p_spaces.matcher(input).matches()) {
            return metar;
        }

// 	get Tornado data if present
//	if( s#(TORNADO\w0,2|WATERSPOUTS*|FUNNEL CLOUDS*)\s+## )
        m = p.p_tornado.matcher(input);
        if (m.find()) {
//		$TornadicType = padstr( $1, 15 ) ;
            metar.put("TornadicType", m.group(1));
            input = m.replaceFirst("");
//		if( s#(B|E)(\d\d)(\d\d)?\s+## )
            m = p.p_tornadoTime.matcher(input);
            if (m.find()) {
//			if( $1 eq "B" )
                if (m.group(1).equals("B")) {
//				$BTornadic_hh = $2 ;
//				$BTornadic_mm = $3 ;
                    metar.put("Begin_Tornado", m.group(2) + m.group(3));
                } else {
//				$ETornadic_hh = $2 ;
//				$ETornadic_mm = $3 if( defined( $3 ) ) ;
                    metar.put("End_Tornado", m.group(2) + m.group(3));
                }
                input = m.replaceFirst("");
            }
//		$TornadicLOC = padstr( $1, 10 )
//			if( s#^(DSNT|VCY STN|VC STN|VCY|VC)\s+## ) ;
            m = p.p_tornadoLocation.matcher(input);
            if (m.find()) {
                metar.put("Tornado_Location", m.group(1));
                input = m.replaceFirst("");
            }

//		$TornadicDIR = padstr( $1, 2 )
//			if( s#^(NE|NW|SE|SW|N|S|E|W)\s+## ) ;
            m = p.p_tornadoDirection.matcher(input);
            if (m.find()) {
                metar.put("Tornado_Direction", m.group(1));
                input = m.replaceFirst("");
            }
        } // end tornado

// 	get Peak winds
//	if( s#PK WND (\d3)(\d1,3)/(\d\d)?(\d\d)\s+## )
        m = p.p_peakWind.matcher(input);
        if (m.find()) {
//		$PKWND_dir = $1 ;
            metar.put("Peak_Wind_Direction", m.group(1));
//		$PKWND_spd = $2 ;
            metar.put("Peak_Wind_Speed", m.group(2));
//		$PKWND_hh = $3 if( defined( $3 ) ) ;
//		$PKWND_mm = $4 ;
            metar.put("Peak_Wind_Time", m.group(3) + m.group(4));
            input = m.replaceFirst("");
        }

// 	get Wind shift
//	if( s#WSHFT (\d\d)?(\d\d)\s+## )
        m = p.p_windShift.matcher(input);
        if (m.find()) {
//		$WshfTime_hh = $1 if( defined( $1 ) );
//		$WshfTime_mm = $2 ;
            metar.put("Wind_Shift", m.group(1) + m.group(2));
            input = m.replaceFirst("");
        }

// 	get FROPO ( wind shift because of frontal passage )
//	$Wshft_FROPA = 1 if( s#FROPA\s+## ) ;
        m = p.p_FROPA.matcher(input);
        if (m.find()) {
            metar.put("Wind_Shift_Frontal_Passage", "1");
            input = m.replaceFirst("");
        }

// 	Tower visibility
//	if( s#TWR (VIS|VSBY) (\d1,3) (\d1,2)/(\d1,2)\s+## ) 
//	$VIS_TWR = $2 + ( $3 / $4 ) ;
        m = p.p_towerVisibility1.matcher(input);
        if (m.find()) {
            var1 = Float.parseFloat(m.group(2));
            var2 = Float.parseFloat(m.group(3));
            var3 = Float.parseFloat(m.group(4));
            var1 = var1 + (var2 / var3);
            metar.put("Tower_Visibility", Float.toString(var1));
            input = m.replaceFirst("");
// 	elsif( s#TWR (VIS|VSBY) (\d1,2)/(\d1,2)\s+## ) 
//		$VIS_TWR = ( $2 / $3 ) ;
        } else {
            m = p.p_towerVisibility2.matcher(input);
            if (m.find()) {
                var1 = Float.parseFloat(m.group(2));
                var2 = Float.parseFloat(m.group(3));
                var1 = var1 / var2;
                metar.put("Tower_Visibility", Float.toString(var1));
                input = m.replaceFirst("");
// 		elsif( s#TWR (VIS|VSBY) (\d1,3)\s+## ) 
//			$VIS_TWR = $2 ;
            } else {
                m = p.p_towerVisibility3.matcher(input);
                if (m.find()) {
                    metar.put("Tower_Visibility", m.group(2));
                    input = m.replaceFirst("");
                }
            }
        }
// 	Surface visibility
//	if( s#SFC (VIS|VSBY) (\d1,3) (\d1,2)/(\d1,2)\s+## ) 
//	$VIS_SFC = $2 + ( $3 / $4 ) ;
        m = p.p_surfaceVisibility1.matcher(input);
        if (m.find()) {
            var1 = Float.parseFloat(m.group(2));
            var2 = Float.parseFloat(m.group(3));
            var3 = Float.parseFloat(m.group(4));
            var1 = var1 + (var2 / var3);
            metar.put("Surface_Visibility", Float.toString(var1));
            input = m.replaceFirst("");
// 		elsif( s#SFC (VIS|VSBY) (\d1,2)/(\d1,2)\s+## ) 
//			$VIS_SFC = ( $2 / $3 ) ;
        } else {
            m = p.p_surfaceVisibility2.matcher(input);
            if (m.find()) {
                var1 = Float.parseFloat(m.group(2));
                var2 = Float.parseFloat(m.group(3));
                var1 = var1 / var2;
                metar.put("Surface_visibility", Float.toString(var1));
                input = m.replaceFirst("");
// 		elsif( s#SFC (VIS|VSBY) (\d1,3)\s+## ) 
//			$VIS_SFC = $2 ;
            } else {
                m = p.p_surfaceVisibility3.matcher(input);
                if (m.find()) {
                    metar.put("Surface_visibility", m.group(2));
                    input = m.replaceFirst("");
                }
            }
        }
// 	Variable visibility
//	if( s#(VIS|VSBY) (\d1,3) (\d1,2)/(\d1,2)V(\d1,3) (\d1,2)/(\d1,2)\s+## ) 
//		$VISmin = $2 + ( $3 / $4 ) ;
//		$VISmax = $5 + ( $6 / $7 ) ;
        m = p.p_variableVisibility1.matcher(input);
        if (m.find()) {
            var1 = Float.parseFloat(m.group(2));
            var2 = Float.parseFloat(m.group(3));
            var3 = Float.parseFloat(m.group(4));
            var1 = var1 + (var2 / var3);
            metar.put("Variable_Visibility_Min", Float.toString(var1));
            var1 = Float.parseFloat(m.group(5));
            var2 = Float.parseFloat(m.group(6));
            var3 = Float.parseFloat(m.group(7));
            var1 = var1 + (var2 / var3);
            metar.put("Variable_Visibility_Max", Float.toString(var1));
            input = m.replaceFirst("");
//	 elsif( s#(VIS|VSBY) (\d1,3)V(\d1,3) (\d1,2)/(\d1,2)\s+## ) 
//		$VISmin = $2 ;
//		$VISmax = $3 + ( $4 / $5 ) ;
        } else {
            m = p.p_variableVisibility2.matcher(input);
            if (m.find()) {
                metar.put("Variable_Visibility_Min", m.group(2));
                var1 = Float.parseFloat(m.group(2));
                var2 = Float.parseFloat(m.group(3));
                var3 = Float.parseFloat(m.group(4));
                var1 = var1 + (var2 / var3);
                metar.put("Variable_Visibility_Max", Float.toString(var1));
                input = m.replaceFirst("");
// 		elsif( s#(VIS|VSBY) (\d1,2)/(\d1,2)V(\d1,3) (\d1,2)/(\d1,2)\s+## ) 
//			$VISmin = ( $2 / $3 ) ;
//			$VISmax = $4 + ( $5 / $6 ) ;
            } else {
                m = p.p_variableVisibility3.matcher(input);
                if (m.find()) {
                    var1 = Float.parseFloat(m.group(2));
                    var2 = Float.parseFloat(m.group(3));
                    var1 = var1 / var2;
                    metar.put("Variable_Visibility_Min", Float.toString(var1));
                    var1 = Float.parseFloat(m.group(2));
                    var2 = Float.parseFloat(m.group(3));
                    var3 = Float.parseFloat(m.group(4));
                    var1 = var1 + (var2 / var3);
                    metar.put("Variable_Visibility_Max", Float.toString(var1));
                    input = m.replaceFirst("");
// 			elsif( s#(VIS|VSBY) (\d1,3) (\d1,2)/(\d1,2)V(\d1,3)\s+## ) 
//				$VISmin = $2 + ( $3 / $4 ) ;
//				$VISmax = $5 ;
                } else {
                    m = p.p_variableVisibility4.matcher(input);
                    if (m.find()) {
                        var1 = Float.parseFloat(m.group(2));
                        var2 = Float.parseFloat(m.group(3));
                        var3 = Float.parseFloat(m.group(4));
                        var1 = var1 + (var2 / var3);
                        metar.put("Variable_Visibility_Min", Float.toString(var1));
                        metar.put("Variable_Visibility_Max", m.group(5));
                        input = m.replaceFirst("");
// 				elsif( s#(VIS|VSBY) (\d1,3)V(\d1,3)\s+## ) 
//					$VISmin = $2 ;
//					$VISmax = $3 ;
                    } else {
                        m = p.p_variableVisibility5.matcher(input);
                        if (m.find()) {
                            metar.put("Variable_Visibility_Min", m.group(2));
                            metar.put("Variable_Visibility_Max", m.group(3));
                            input = m.replaceFirst("");
//	 				elsif( s#(VIS|VSBY) (\d1,2)/(\d1,2)V(\d1,3)\s+## ) 
//						$VISmin = ( $2 / $3 ) ;
//						$VISmax = $4 ;
                        } else {
                            m = p.p_variableVisibility6.matcher(input);
                            if (m.find()) {
                                var1 = Float.parseFloat(m.group(2));
                                var2 = Float.parseFloat(m.group(3));
                                var1 = var1 / var2;
                                metar.put("Variable_Visibility_Min", Float.toString(var1));
                                metar.put("Variable_Visibility_Max", m.group(4));
                                input = m.replaceFirst("");
                            }
                        }
                    }
                }
            }
        } // end variableVisibility

// 	Second site visiblity
//	if( s#(VIS|VSBY) (\d1,3) (\d1,2)/(\d1,2) (RY\d1,2)\s+## ) 
//		$VIS_2ndSite = $2 + ( $3 / $4 ) ;
//		$VIS_2ndSite_LOC = padstr( $5, 10 ) ;
        m = p.p_Visibility2ndSite1.matcher(input);
        if (m.find()) {
            var1 = Float.parseFloat(m.group(2));
            var2 = Float.parseFloat(m.group(3));
            var3 = Float.parseFloat(m.group(4));
            var1 = var1 + (var2 / var3);
            metar.put("Second_Site_Visibility", Float.toString(var1));
            metar.put("Second_Site_Location", m.group(5));
            input = m.replaceFirst("");
// 	elsif( s#(VIS|VSBY) (\d1,3) (RY\d1,2)\s+## ) 
//		$VIS_2ndSite = $2 ;
//		$VIS_2ndSite_LOC = padstr( $3, 10 ) ;
        } else {
            m = p.p_Visibility2ndSite2.matcher(input);
            if (m.find()) {
                metar.put("Second_Site_Visibility", m.group(2));
                metar.put("Second_Site_Location", m.group(3));
                input = m.replaceFirst("");
// 		elsif( s#(VIS|VSBY) (\d1,2)/(\d1,2) (RY\d1,2)\s+## ) 
//			$VIS_2ndSite = ( $2 / $3 ) ;
//			$VIS_2ndSite_LOC = padstr( $4, 10 ) ;
            } else {
                m = p.p_Visibility2ndSite3.matcher(input);
                if (m.find()) {
                    var1 = Float.parseFloat(m.group(2));
                    var2 = Float.parseFloat(m.group(3));
                    var1 = var1 / var2;
                    metar.put("Second_Site_Visibility", Float.toString(var1));
                    metar.put("Second_Site_Location", m.group(4));
                    input = m.replaceFirst("");
                }
            }
        } // end Second site visiblity

// 	Lightning data ( Occasional,Frequent,Continuous) and
//	(Cloud-Ground,In-Cloud,Cloud-Cloud,Cloud-Air)
//	if( s#(OCNL|FRQ|CNS) LTG\s?(CG|IC|CC|CA)\s?(DSNT|AP|VCY STN|VCNTY STN)?\s?(NE|NW|SE|SW|N|S|E|W)?\s+## )
        m = p.p_Lightning.matcher(input);
        if (m.find()) {
//		$LTG_OCNL = 1 if( $1 eq "OCNL" ) ;
//		$LTG_FRQ = 1 if( $1 eq "FRQ" ) ;
//		$LTG_CNS = 1 if( $1 eq "CNS" ) ;
//		$LTG_CG = 1 if( $2 eq "CG" ) ;
//		$LTG_IC = 1 if( $2 eq "IC" ) ;
//		$LTG_CC = 1 if( $2 eq "CC" ) ;
//		$LTG_CA = 1 if( $2 eq "CA" ) ;
//		$LTG_DSNT = 1 if( $3 eq "DSNT" ) ;
//		$LTG_AP = 1 if( $3 eq "AP" ) ;
//		$LTG_VcyStn = 1 if( $3 eq "VCY STN" || $3 eq "VCNTY STN" ) ;
//		$LTG_DIR = padstr( $4, 2 ) if( defined( $4 ) ) ;
            metar.put("Lightning", m.group(0));
            input = m.replaceFirst("");
        } // end Lightning data

// 	get min/max for Variable Ceiling
//	if( s#CIG (\d1,4)V(\d1,4)\s+## ) 
//		$Ceiling_min = $1 ;
//		$Ceiling_max = $2 ;
        m = p.p_CIG.matcher(input);
        if (m.find()) {
            metar.put("Ceiling_Min", Integer.toString(Integer.parseInt(m.group(1)) * 100));
            metar.put("Ceiling_Max", Integer.toString(Integer.parseInt(m.group(2)) * 100));
            input = m.replaceFirst("");
        }

//	 ? about SKY condition at 2nd location
// 	get 2nd site ceiling and location
//	if( s#CIG (\d3) (RY\d1,2)\s+## ) 
//		$CIG_2ndSite_meters = $1 * 10 ;
//		$CIG_2ndSite_LOC = $2 ;
        m = p.p_CIG_RY.matcher(input);
        if (m.find()) {
            var1 = Float.parseFloat(m.group(1)) * 10;
            metar.put("Second_Site_Sky", Float.toString(var1));
            metar.put("Second_Site_Sky_Location", m.group(2));
            input = m.replaceFirst("");
        }

// 	Presure falling rapidly
//	$PRESFR = 1 if( s#PRESFR/?\s+## ) ;
        m = p.p_PRESFR.matcher(input);
        if (m.find()) {
            metar.put("Pressure_Falling_Rapidly", "1");
            input = m.replaceFirst("");
        }

// 	Presure rising rapidly
//	$PRESRR = 1 if( s#PRESRR/?\s+## ) ;
        m = p.p_PRESRR.matcher(input);
        if (m.find()) {
            metar.put("Pressure_Rising_Rapidly", "1");
            input = m.replaceFirst("");
        }

// 	Sector visibility
//	if( s#(VIS|VSBY) (NE|NW|SE|SW|N|S|E|W)(\d1,3) (\d1,2)/(\d1,2)\s+## ) 
//		$SectorVIS_DIR = padstr( $2, 2 ) ;
//		$SectorVIS = $3 + ( $4 / $5 ) ;
        m = p.p_sectorVisibility1.matcher(input);
        if (m.find()) {
            metar.put("Sector_Visibility_Direction", m.group(2));
            var1 = Float.parseFloat(m.group(3));
            var2 = Float.parseFloat(m.group(4));
            var3 = Float.parseFloat(m.group(5));
            var1 = var1 + (var2 / var3);
            metar.put("Sector_Visibility", Float.toString(var1));
            input = m.replaceFirst("");
// 	elsif( s#(VIS|VSBY) (NE|NW|SE|SW|N|S|E|W) (\d1,2)/(\d1,2)\s+## ) 
//		$SectorVIS_DIR = padstr( $2, 2 ) ;
//		$SectorVIS = ( $3 / $4 ) ;
        } else {
            m = p.p_sectorVisibility2.matcher(input);
            if (m.find()) {
                metar.put("Sector_Visibility_Direction", m.group(2));
                var1 = Float.parseFloat(m.group(1));
                var2 = Float.parseFloat(m.group(2));
                var1 = var1 / var2;
                metar.put("Sector_Visibility", Float.toString(var1));
                input = m.replaceFirst("");
// 		elsif( s#(VIS|VSBY) (NE|NW|SE|SW|N|S|E|W)(\d1,3)\s+## ) 
//			$SectorVIS_DIR = padstr( $2, 2 ) ;
//			$SectorVIS = $3 ;
            } else {
                m = p.p_sectorVisibility3.matcher(input);
                if (m.find()) {
                    metar.put("Sector_Visibility_Direction", m.group(2));
                    metar.put("Sector_Visibility", m.group(3));
                    input = m.replaceFirst("");
                }
            }
        }

// 	Hailstone activity and size
//	if( s#GR M1/4\s+## ) 
//		$GR = 1 ;
//		$GRsize = 1 / 8 ;
        m = p.p_GR1.matcher(input);
        if (m.find()) {
            metar.put("Hailstone_Activity", "1");
            metar.put("Hailstone_Size", "0.25");
            input = m.replaceFirst("");
// 	elsif( s#GR (\d1,3) (\d1,2)/(\d1,2)\s+## ) 
//		$GR = 1 ;
//		$GRsize = $1 + ( $2 / $3 ) ;
        } else {
            m = p.p_GR2.matcher(input);
            if (m.find()) {
                metar.put("Hailstone_Activity", "1");
                var1 = Float.parseFloat(m.group(1));
                var2 = Float.parseFloat(m.group(2));
                var3 = Float.parseFloat(m.group(3));
                var1 = var1 + (var2 / var3);
                metar.put("Hailstone_Size", Float.toString(var1));
                input = m.replaceFirst("");
// 		elsif( s#GR (\d1,2)/(\d1,2)\s+## ) 
//			$GR = 1 ;
//			$GRsize = ( $1 / $2 ) ;
            } else {
                m = p.p_GR3.matcher(input);
                if (m.find()) {
                    metar.put("Hailstone_Activity", "1");
                    var1 = Float.parseFloat(m.group(1));
                    var2 = Float.parseFloat(m.group(2));
                    var1 = var1 / var2;
                    metar.put("Hailstone_Size", Float.toString(var1));
// 			elsif( s#GR (\d1,3)\s+## ) 
//				$GR = 1 ;
//				$GRsize = $1 ;
                } else {
                    m = p.p_GR4.matcher(input);
                    if (m.find()) {
                        metar.put("Hailstone_Activity", "1");
                        metar.put("Hailstone_Size", m.group(1));
                        input = m.replaceFirst("");
                    }
                }
            }
        }
//	$GR = 1 if( s#GS\s+## ) ;
        m = p.p_GR.matcher(input);
        if (m.find()) {
            metar.put("Hailstone_Activity", "1");
            input = m.replaceFirst("");
        }

// 	VIRGA activity
//	if( s#VIRGA (DSNT )?(NE|NW|SE|SW|N|S|E|W)?\s+## ) 
//		$VIRGA = 1 ;
//		$VIRGAdir = padstr( $2, 2 ) if( $2 ) ;
        m = p.p_VIRGA.matcher(input);
        if (m.find()) {
            metar.put("Virga_Activity", "1");
            metar.put("Virga_Direction", m.group(2));
            input = m.replaceFirst("");
        }

// 	Surface-based Obscuring Phenomena  SfcObscuration weather conditions
// 	code table 4678
//	if( s#-X(VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?(\d)\s+## ) 
//		$SfcObscuration = padstr( "$1$2$3$4$5", 8 ) ;
//		$OctsSkyObscured = $6 ;
        m = p.p_obscuring.matcher(input);
        if (m.find()) {
            String tmp = "";
            if (m.group(1) != null) {
                tmp = m.group(1);
            }
            if (m.group(2) != null) {
                tmp = m.group(2);
            }
            if (m.group(3) != null) {
                tmp = m.group(3);
            }
            if (m.group(4) != null) {
                tmp = m.group(4);
            }
            if (m.group(5) != null) {
                tmp = m.group(5);
            }
            if ( ! tmp.equals("")) {
                metar.put("Surface_Obscuration", tmp );
            }
            metar.put("OctsSkyObscured", m.group(6));
            input = m.replaceFirst("");
        }

// 	get Ceiling_est or Ceiling height
//	$CIGNO = 1 if( s#CIGNO\s+## ) ;
        m = p.p_CIGNO.matcher(input);
        if (m.find()) {
            metar.put("CIGNO", "1");
            input = m.replaceFirst("");
        }
//	if( s#CIG(E)?(\d3)\s+## ) 
//		if( $1 eq "E" ) 
//			$Ceiling_est = $2 * 100 ;
//		 else 
//			$Ceiling = $2 * 100 ;
        m = p.p_CIG_EST.matcher(input);
        if (m.find()) {
            String est = Integer.toString(Integer.parseInt(m.group(2)) * 100);
            if (m.group(1).equals("E")) {
                metar.put("Ceiling_Estimate", est);
            } else {
                metar.put("Ceiling", est);
            }
            input = m.replaceFirst("");
        }

// 	Variable Sky conditions
//	if( s#(FEW|SCT|BKN|OVC)(\d3)? V (FEW|SCT|BKN|OVC)\s+## ) 
//		$VrbSkyBelow = $1 ;
//		$VrbSkyLayerHgt = $2 * 100 if( defined( $2 ) ) ;
//		$VrbSkyAbove = $3 ;
        m = p.p_variableSky.matcher(input);
        if (m.find()) {
            metar.put("Variable_Sky_Below", m.group(1));
            //String est = Integer.toString(Integer.parseInt(m.group(2)) * 100);
            if (m.group(2) != null) {
                metar.put("Variable_Sky_Height", Integer.toString(Integer.parseInt(m.group(2)) * 100));
            }
            metar.put("Variable_Sky_Above", m.group(3));
            input = m.replaceFirst("");
        }

// 	Significant Cloud Types
//	if( s#(CB|CBMAM|TCU|ACC|SCSL|ACSL|ROTOR CLD|ROPE|ROPE CLD)\s+## ) 
//		$Sign_cloud = padstr( $1, 10 ) ;
//		$Sign_dist = padstr( $1, 10 ) 
//			if( s#^(VCNTY STN|VCY STN|VC STN|VCY|VC|DSNT|OMT)\s+## ) ;
//		$Sign_dir = padstr( "$1$2$3", 10 ) 
//			if( s#^(NE|NW|SE|SW|N|S|E|W)(\-| MOV )?(NE|NW|SE|SW|N|S|E|W)?/?\s+## ) ;
        m = p.p_significantCloud.matcher(input);
        if (m.find()) {
            metar.put("Significant_Cloud", m.group(1));
            input = m.replaceFirst("");
            m = p.p_significantCloud1.matcher(input);
            if (m.find()) {
                metar.put("Significant_Cloud_Vicinity", m.group(1));
                input = m.replaceFirst("");
                m = p.p_significantCloud2.matcher(input);
                if (m.find()) {
                    metar.put("Significant_Cloud_Direction", m.group(1));
                    input = m.replaceFirst("");
                }
            }
        }

// 	Obscuring Phenomena Aloft
//	if( s#(VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)? (FEW|SCT|BKN|OVC)(\d3)\s+## ) 
//		$ObscurAloft = padstr( "$1$2$3$4$5", 8 ) ;
//		$ObscurAloftSkyCond = $6 ;
//		$ObscurAloftHgt = $7 * 100 ;
//
        m = p.p_obscuringPhen.matcher(input);
        if (m.find()) {
            String tmp = "";
            if (m.group(1) != null) {
                tmp = m.group(1);
            }
            if (m.group(2) != null) {
                tmp = m.group(2);
            }
            if (m.group(3) != null) {
                tmp = m.group(3);
            }
            if (m.group(4) != null) {
                tmp = m.group(4);
            }
            if (m.group(5) != null) {
                tmp = m.group(5);
            }
            if ( ! tmp.equals("")) {
                metar.put("Surface_Phenomena", tmp );
            }
            metar.put("Obscuring_Phenomena_Sky", m.group(6));
            if (m.group(7) != null) {
                metar.put("Obscuring_Phenomena_Sky_Height", Integer.toString(Integer.parseInt(m.group(7)) * 100));
            }
            input = m.replaceFirst("");
        }

// 	Air craft mishap  ACFTMSHP
//	$ACFTMSHP = 1 if( s#\(?ACFT\s?MSHP\)?\s+## ) ;
        m = p.p_ACFT.matcher(input);
        if (m.find()) {
            metar.put("Air_craft_mishap", "1");
            input = m.replaceFirst("");
        }

// 	No changes in weather conditions until next report  NOSPECI
//	$NOSPECI = 1 if( s#NOSPECI\s+## ) ;
        m = p.p_NOSPECI.matcher(input);
        if (m.find()) {
            metar.put(" No_changes_in_weather", "1");
            input = m.replaceFirst("");
        }

// 	This is first report of the day  FIRST
//	$FIRST = 1 if( s#FIRST\s+## ) ;
        m = p.p_FIRST.matcher(input);
        if (m.find()) {
            metar.put("First_Report_Today", "1");
            input = m.replaceFirst("");
        }

// 	This is last report in observation coverage  LAST
//	$LAST = 1 if( s#LAST\s+## ) ;
        m = p.p_LAST.matcher(input);
        if (m.find()) {
            metar.put("Last_Report_Today", "1");
            input = m.replaceFirst("");
        }

// 	Cloud Types
//	if( s# 8/(\d|/)(\d|/)(\d|/)/?\s+# # ) 
//		$Cloud_low = $1 ;
//		$Cloud_medium = $2 ;
//		$Cloud_high = $3 ;
        m = p.p_cloud_height.matcher(input);
        if (m.find()) {
            metar.put("Cloud_Low", m.group(1));
            metar.put("Cloud_Medium", m.group(2));
            metar.put("Cloud_High", m.group(3));
            input = m.replaceFirst("");
        }
//
// 	Snow Increasing Rapidly   SNINCR
//	if( s#SNINCR (\d1,3)/(\d1,3)\s+## ) 
//		$SNINCR = $1 ;
//		$SNINCR_TotalDepth = $2 ;
        m = p.p_SNINCR.matcher(input);
        if (m.find()) {
            metar.put("Snow_Increasing_Rapidly", m.group(1));
            metar.put("Snow_Increasing_Depth", m.group(2));
            input = m.replaceFirst("");
        }
//
// 	Snow depth on ground
//	if( s#4/(\d1,3)\s+# # ) 
//		$SN_depth = $1 ;
        m = p.p_snowDepth.matcher(input);
        if (m.find()) {
            metar.put("Snow_Depth", m.group(1));
            input = m.replaceFirst("");
        }
//
//	 Water equivalent of snow on ground
//	$SN_waterequiv = $1 / 10 if( s# 933(\d3)\s+# # ) ;
        m = p.p_waterEquiv.matcher(input);
        if (m.find()) {
            metar.put("Water_Equivalent_of_Snow", Double.toString(Float.parseFloat(m.group(1)) * 0.1 ));
            input = m.replaceFirst("");
        }

// 	Duration of sunshine
//	if( s# 98(\d1,3|///)\s+# # ) 
//		if( $1 eq "///" ) 
//			$SunSensorOut = 1 ;
//		 else 
//			$SunShineDur = $1 ;
        m = p.p_sunShine.matcher(input);
        if (m.find()) {
            if( m.group(1).equals("///") ) {
                metar.put("Sun_Sensor_Out", "1");
            } else {
                metar.put("Sun_Sensor_Duration", m.group(1));
            }
            input = m.replaceFirst("");
        }

// 	Precipitation amount
//	if( s# 6(\d4|////)\s+# # ) 
//		$PRECIP_amt = $1 / 100 if( $1 ne "////" ) ;
        m = p.p_precipitation.matcher(input);
        if (m.find()) {
            if( ! m.group(1).equals("////") ) {
                metar.put("Precipitation_amount", Double.toString(Float.parseFloat(m.group(1)) * 0.01 ));
            }
            input = m.replaceFirst("");
        }
//
// 	24 Hour Precipitation amount
//	if( s# 7(\d4|////)\s+# # ) 
//		$PRECIP_24_amt = $1 / 100 if( $1 ne "////" ) ;
        m = p.p_precipitation24.matcher(input);
        if (m.find()) {
            if( ! m.group(1).equals("////") ) {
                metar.put("Precipitation_amount_24Hours", Double.toString(Float.parseFloat(m.group(1)) * 0.01 ));
            }
            input = m.replaceFirst("");
        }
//
// 	Maximum Temperature
//	if( s# 1(0|1|/)(\d3|///)\s+# # ) 
//		$Tmax = $2 / 10 if( $2 ne "///" ) ;
//		$Tmax *= -1.0 if( $1 == 1 ) ;
//
        m = p.p_maxTemperature.matcher(input);
        if (m.find()) {
            if( ! m.group(2).equals("////") && m.group(1).equals("1") ) {
                metar.put("Max_Temperature", Double.toString(Float.parseFloat(m.group(2)) * -0.1 ));
            } else {
                metar.put("Max_Temperature", Double.toString(Float.parseFloat(m.group(2)) * 0.1 ));
            }
            input = m.replaceFirst("");
        }
// 	Minimum Temperature
//	if( s# 2(0|1|/)(\d3|///)\s+# # ) 
//		$Tmin = $2 / 10 if( $2 ne "///" ) ;
//		$Tmin *= -1.0 if( $1 == 1 ) ;
        m = p.p_minTemperature.matcher(input);
        if (m.find()) {
            if( ! m.group(2).equals("////") && m.group(1).equals("1") ) {
                metar.put("Min_Temperature", Double.toString(Float.parseFloat(m.group(2)) * -0.1 ));
            } else {
                metar.put("Min_Temperature", Double.toString(Float.parseFloat(m.group(2)) * 0.1 ));
            }
            input = m.replaceFirst("");
        }
//
// 	24-Hour Maximum and Minimum Temperature
//	if( s# 4(0|1|/)(\d3|///)(0|1|/)(\d3|///)\s+# # ) 
//		$Tmax24 = $2 / 10 if( $2 ne "///" ) ;
//		$Tmax24 *= -1.0 if( $1 == 1 ) ;
//		$Tmin24 = $4 / 10 if( $4 ne "///" ) ;
//		$Tmin24 *= -1.0 if( $3 == 1 ) ;
        m = p.p_maxMinTemp24.matcher(input);
        if (m.find()) {
            if( ! m.group(2).equals("////") && m.group(1).equals("1") ) {
                metar.put("Max_Temperature_24Hours", Double.toString(Float.parseFloat(m.group(2)) * -0.1 ));
            } else {
                metar.put("Max_Temperature_24Hours", Double.toString(Float.parseFloat(m.group(2)) * 0.1 ));
            }
            if( ! m.group(4).equals("////") && m.group(3).equals("1") ) {
                metar.put("Min_Temperature_24Hours", Double.toString(Float.parseFloat(m.group(4)) * -0.1 ));
            } else {
                metar.put("Min_Temperature_24Hours", Double.toString(Float.parseFloat(m.group(4)) * 0.1 ));
            }
            input = m.replaceFirst("");
        }
//
// 	Presure Tendency
//	if( s# 5(0|1|2|3|4|5|6|7|8)(\d3/?|///)\s+# # ) 
//		$char_Ptend = $1 ;
//		$Ptend = $2 / 10 if( $2 ne "///" ) ;
//
        m = p.p_pressureTendency.matcher(input);
        if (m.find()) {
            metar.put("Presure_Tendency_char", m.group(1));
            if( ! m.group(2).equals("////") ) {
                metar.put("Presure_Tendency", Double.toString(Float.parseFloat(m.group(2)) * 0.1 ));
            }
            input = m.replaceFirst("");
        }

// 	Freezing Rain sensor not working  FZRANO
//	$FZRANO = 1 if( s#FZRANO\s+## ) ;
        m = p.p_FZRANO.matcher(input);
        if (m.find()) {
            metar.put("Freezing_Rain_sensor_not_working", "1");
            input = m.replaceFirst("");
        }

// 	Tipping bucket rain gauge is inoperative.
//	$PNO = 1 if( s#PNO\s+## ) ;
        m = p.p_PNO.matcher(input);
        if (m.find()) {
            metar.put("Tipping_bucket_rain_gauge_inoperative", "1");
            input = m.replaceFirst("");
        }

// 	Maintenance is needed on system Indicator
//	$maintIndicator = 1 if( s#\$\s+## ) ;
        m = p.p_maintenace.matcher(input);
        if (m.find()) {
            metar.put("Maintenance_needed_on_system", "1");
            input = m.replaceFirst("");
        }

// 	Get Recent weather conditions with Beginning and Ending times, moved 
//	because the RE are too general and they match wrongly
// 	code table 4678
	for(int i = 0; i < 3; i++ ) {
            String RWX = "Recent_Weather_"+ Integer.toString( i +1 );
//	    if( s#(\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?B(\d2,4)E(\d2,4)\s+## ) 
            m = p.p_recentWeather.matcher(input);
            if (m.find()) {
//		$Recent_WX[ $i ] = padstr( "$1$2$3$4$5", 8 ) ;
                String tmp = "";
                if (m.group(1) != null) {
                    tmp = m.group(1);
                }
                if (m.group(2) != null) {
                    tmp = tmp + m.group(2);
                }
                if (m.group(3) != null) {
                    tmp = tmp + m.group(3);
                }
                if (m.group(4) != null) {
                    tmp = tmp + m.group(4);
                }
                if (m.group(5) != null) {
                    tmp = tmp + m.group(5);
                }
                if ( ! tmp.equals("")) {
                    metar.put( RWX, tmp );
                } else {
                    break;
                }
//		if( length( $6 ) == 5 ) 
//			$Recent_WX_Bhh[ $i ] = substr( $6, 1, 2 ) * 1 ;
//			$Recent_WX_Bmm[ $i ] = substr( $6, 3, 2 ) * 1 ;
//		elsif( length( $6 ) == 3 ) 
//			$Recent_WX_Bmm[ $i ] = substr( $6, 1, 2 ) * 1 ;
                metar.put( RWX +"_Begin_Time", m.group(6) );
//		if( length( $7 ) == 5 ) 
//			$Recent_WX_Ehh[ $i ] = substr( $7, 1, 2 ) * 1 ;
//			$Recent_WX_Emm[ $i ] = substr( $7, 3, 2 ) * 1 ;
//	   	elsif( length( $7 ) == 3 ) 
//			$Recent_WX_Emm[ $i ] = substr( $7, 1, 2 ) * 1 ;
                metar.put( RWX +"_End_Time", m.group(7) );
                input = m.replaceFirst("");
//	    elsif( s#(\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?(B|E)(\d2,4)\s+## ) 
            } else {
                m = p.p_recentWeather1.matcher(input);
                if (m.find()) {
//		$Recent_WX[ $i ] = padstr( "$1$2$3$4$5", 8 ) ;
                    String tmp = "";
                    if (m.group(1) != null) {
                        tmp = m.group(1);
                    }
                    if (m.group(2) != null) {
                        tmp = tmp + m.group(2);
                    }
                    if (m.group(3) != null) {
                        tmp = tmp + m.group(3);
                    }
                    if (m.group(4) != null) {
                        tmp = tmp + m.group(4);
                    }
                    if (m.group(5) != null) {
                        tmp = tmp + m.group(5);
                    }
                    if ( ! tmp.equals("")) {
                        metar.put( RWX, tmp );
                    } else {
                        break;
                    }
//		if( $6 eq "B" && ( length( $7 ) == 4 )) 
//			$Recent_WX_Bhh[ $i ] = substr( $7, 0, 2 ) * 1 ;
//			$Recent_WX_Bmm[ $i ] = substr( $7, 2, 2 ) * 1 ;
//		 elsif( $6 eq "B" && ( length( $7 ) == 2 )) 
//			$Recent_WX_Bmm[ $i ] = substr( $7, 0, 2 ) * 1 ;
                    if( m.group(6).equals("B" ) ) {
                        metar.put( RWX +"_Begin_Time", m.group(7) );
//		 elsif( $6 eq "E" && ( length( $7 ) == 4 )) 
//			$Recent_WX_Ehh[ $i ] = substr( $7, 0, 2 ) * 1 ;
//			$Recent_WX_Emm[ $i ] = substr( $7, 2, 2 ) * 1 ;
//		 elsif( $6 eq "E" && ( length( $7 ) == 2 )) 
//			$Recent_WX_Emm[ $i ] = substr( $7, 0, 2 ) * 1 ;
                     } else {
                        metar.put( RWX +"_End_Time", m.group(7) );
		             }
                     input = m.replaceFirst("");
	        } else {
//		last ;
                    break;
	        }
            }
        } // end for recent weather

// 	Plain Language remarks includes Volcanic eruptions
//	$PlainText = padstr( $1, 128 ) if( s#(\w.*)## ) ;
        m = p.p_plainText.matcher(input);
        if (m.find()) {
            metar.put("Plain_Language_remarks", m.group(1));
            input = m.replaceFirst("");
        }

        return metar; // all the fields decoded

    } // end parseReport

// convert cloud height to  meters
    public String cloud_hgt2_meters(String height) {

        if (height.equals("999")) {
            return "30000";
        } else {
//		$meters = 30 * $height ;
            return Integer.toString(30 * Integer.parseInt(height));
        }
    } // end cloud_hgt2_meters

    public static void main(String args[]) {

        //String report = "KD07 150256Z AUTO 28005KT M08/M11 A3005 RMK AO2 SLP223  T10781111 50006 PWINO FZRANO";
      String report = "METAR K1V4 251254Z AUTO 01/01 A3002 RMK AO2 SLP172 P0001 T00110006 PWINO FZRANO TSNO ";

        //String report = "KDEN 201453Z 35007KT 1/4SM R35L/5000V6000FT -DZ BR OVC003 05/04 A2996 RMK CIG 003V006";
        //String report = "KDEN 201553Z 01006KT 1/4SM R35L/4500VP6000FT  -DZ BR OVC003 05/04 A2996 RMK AO2 SFC VIS 3/4 CIG 003V006 SLP124 P0000 T00500044";
        //String report = "SOCA 202000Z 10003KT 9999 VCSH FEW010CB SCT015TCU 27/26 Q1010 TEMPO 4000 CB & TCU AU S ET W";
        System.out.println( report );
        // Function References
        MetarParseReport func = new MetarParseReport();

        LinkedHashMap metar = func.parseReport(report);
        String key;

        if (metar == null) {
            System.out.println("return null Hash parse");
            System.exit(1);
        }
        for( Iterator it = metar.keySet().iterator(); it.hasNext(); ) {
            key = (String) it.next();
            System.out.println(key + "\t\t" + (String) metar.get(key));
        }

    }

} // end MetarParseReport
