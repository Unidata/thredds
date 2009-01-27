/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.dt.point.decode;

/*  a class to contain the patterns in the Metar servers
 *
 *  Author Robb Kambic
 *  Date  3/4/07
 *
 */

import java.util.regex.Pattern;

public class MP {

    public static final Pattern N9999 = Pattern.compile(" 9999\\s+");

    public static final Pattern ACFT = Pattern.compile(" \\(?ACFT( |_)?MSHP\\)?\\s+");

    public static final Pattern AUTOS = Pattern.compile(" AUTO\\s+");

    public static final Pattern automatic_report = Pattern.compile(" (A01|A01A|A02|A02A|AO1|AO1A|AO2|AO2A|AOA)\\s+");

    //public static final Pattern B_slash = Pattern.compile("^\\/");

    public static final Pattern B_CR = Pattern.compile("^\\n");

    public static final Pattern B_metar = Pattern.compile("^(METAR|SPECI|TESTM|TESTS) ");

    //public static final Pattern B_P = Pattern.compile("^P");

    public static final Pattern FIRST = Pattern.compile(" FIRST\\s+");

    public static final Pattern FZRANO = Pattern.compile(" FZRANO\\s+");

    public static final Pattern LAST = Pattern.compile(" LAST\\s+");

    public static final Pattern NOSPECI = Pattern.compile(" NOSPECI\\s+");

    public static final Pattern PNO = Pattern.compile(" PNO\\s+");

    public static final Pattern station = Pattern.compile("^(\\w{4})\\s+");

    //public static final Pattern station_dateZ1 = Pattern.compile("^\\w{4} \\d{2}(\\d{2})(\\d{2})Z");

    public static final Pattern ddhhmmZ = Pattern.compile(" (\\d{2})(\\d{2})(\\d{2})Z\\s+");

    //public static final Pattern isodate = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})");

    //public static final Pattern station_dateZ = Pattern.compile("^\\w{4} (\\d{6,8})Z");

    public static final Pattern CAVOKS = Pattern.compile(" CAVOK\\s+");

    public static final Pattern altimeter = Pattern.compile(" (A|Q)(\\d{4}\\.?\\d?)\\s+");

    public static final Pattern CIG = Pattern.compile(" CIG (\\d{1,4})V(\\d{1,4})\\s+");

    public static final Pattern CIG_EST = Pattern.compile(" CIG(E)? (\\d{3})\\s+");

    public static final Pattern CIGNO = Pattern.compile(" CIGNO\\s+");

    public static final Pattern CIG_RY = Pattern.compile(" CIG (\\d{3}) (RY\\d{1,2})\\s+");

    public static final Pattern CLR_or_SKC = Pattern.compile(" (CLR|SKC)\\s+");

    public static final Pattern min_max_wind_dir = Pattern.compile(" (\\d{3})V(\\d{3})\\s+");

    public static final Pattern visibility_direction = Pattern.compile(" (\\d{4})((NE)|(NW)|(SE)|(SW)|(N)|(S)|(E)|(W))\\s+");

    public static final Pattern visibility2 = Pattern.compile(" (\\d{1,3})/(\\d{1,3})(KM|SM)\\s+");

    public static final Pattern visibility3 = Pattern.compile(" P?(\\d{1,4})(KM|SM)\\s+");

    public static final Pattern visibility1 = Pattern.compile(" (\\d{1,4}) (\\d{1,3})/(\\d{1,3})(SM|KM)\\s+");

    public static final Pattern hourly_precip = Pattern.compile(" P ?(\\d{1,5})\\s+");

    public static final Pattern wind_direction_speed = Pattern.compile(" (E|W|N|S)?(\\d{3}|VRB)(\\d{2,3})(G)?(\\d{2,3})?(KMH|KT|MPS|MPH)\\s+");

    static final Pattern Temperature = Pattern.compile("(M|-)?(\\d{2})/(M|-)?(\\d{2})?\\s*");

    static final Pattern Temperature_tenths = Pattern.compile("T(0|1)(\\d{3})(0|1)?(\\d{3})?\\s+");

    public static final Pattern COR = Pattern.compile(" COR\\s+");

    public static final Pattern cloud_cover = Pattern.compile(" (\\+|-)?(OVC|SCT|FEW|BKN)(\\d{3})(\\w{1,3})?\\s+");

    public static final Pattern cloud_height = Pattern.compile(" 8/(\\d|/)(\\d|/)(\\d|/)\\s+");

    //public static final Pattern weather = Pattern.compile(" (\\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?\\s+");

    static final Pattern WeatherObs = Pattern.compile("(\\+|-|VC|PR| )(MI|BC|DR|BL|SH|TS|FZ)?(BR|FG|FU|VA|DU|SA|HZ|PY)\\s+");

    static final Pattern WeatherOther = Pattern.compile("(\\+|-|VC|PR| )(MI|BC|DR|BL|SH|TS|FZ)?(PO|SQ|FC|SS|DS)\\s+");

    static final Pattern WeatherPrecip = Pattern.compile("(\\+|-|VC|PR| )(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)\\s+");

    //public static final Pattern recentWeather = Pattern.compile(" (\\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?B(\\d{2,4})E(\\d{2,4})\\s+" );

    //public static final Pattern recentWeather1 = Pattern.compile(" (\\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?(B|E)(\\d{2,4})\\s+" );

    public static final Pattern GR = Pattern.compile(" GS\\s+");

    public static final Pattern GR1 = Pattern.compile(" GR M1/4\\s+");

    public static final Pattern GR2 = Pattern.compile(" GR (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern GR3 = Pattern.compile(" GR (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern GR4 = Pattern.compile(" GR (\\d{1,3})\\s+");

    public static final Pattern Lightning = Pattern.compile(" (((OCNL|FRQ|CNS) )?LTG\\s?(CG|IC|CC|CA)?\\s?(DSNT|AP|VCY STN|VCNTY STN)?\\s?(NE|NW|SE|SW|N|S|E|W)?)\\s+");

    public static final Pattern NIL = Pattern.compile("NIL\\s*");

    //public static final Pattern obscuring = Pattern.compile(" -X(VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?(\\d)\\s+");

    //public static final Pattern obscuringPhen = Pattern.compile(" (VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)? (FEW|SCT|BKN|OVC)(\\d3)\\s+" );

    public static final Pattern variableSky = Pattern.compile(" (FEW|SCT|BKN|OVC)(\\d{3})? V (FEW|SCT|BKN|OVC)\\s+");

    public static final Pattern Visibility2ndSite1 = Pattern.compile(" (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2}) (RY\\d{1,2})\\s+");

    public static final Pattern Visibility2ndSite2 = Pattern.compile(" (VIS|VSBY) (\\d{1,3}) (RY\\d{1,2})\\s+");

    public static final Pattern Visibility2ndSite3 = Pattern.compile(" (VIS|VSBY) (\\d{1,2})/(\\d{1,2}) (RY\\d{1,2})\\s+");

    public static final Pattern sectorVisibility1 = Pattern.compile(" (VIS|VSBY) (NE|NW|SE|SW|N|S|E|W) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern sectorVisibility2 = Pattern.compile(" (VIS|VSBY) (NE|NW|SE|SW|N|S|E|W) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern sectorVisibility3 = Pattern.compile(" (VIS|VSBY) (NE|NW|SE|SW|N|S|E|W) (\\d{1,3})\\s+");

    //public static final Pattern significantCloud = Pattern.compile(" (CB|CBMAM|TCU|ACC|SCSL|ACSL|ROTOR CLD|ROPE|ROPE CLD)\\s+");

    //public static final Pattern significantCloud1 = Pattern.compile(" (VCNTY STN|VCY STN|VC STN|VCY|VC|DSNT|OMT)\\s+");

    //public static final Pattern significantCloud2 = Pattern.compile(" (NE|NW|SE|SW|N|S|E|W)(\\-| MOV )?(NE|NW|SE|SW|N|S|E|W)?/?\\s+" ) ;

    public static final Pattern surfaceVisibility1 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern surfaceVisibility2 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern surfaceVisibility3 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,3})\\s+");

    public static final Pattern towerVisibility1 = Pattern.compile(" TWR (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern towerVisibility2 = Pattern.compile(" TWR (VIS|VSBY) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern towerVisibility3 = Pattern.compile(" TWR (VIS|VSBY) (\\d{1,3})\\s+");

    public static final Pattern variableVisibility1 = Pattern.compile(" (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})V(\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern variableVisibility2 = Pattern.compile(" (VIS|VSBY) (\\d{1,3})V(\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern variableVisibility3 = Pattern.compile(" (VIS|VSBY) (\\d{1,2})/(\\d{1,2})V(\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern variableVisibility4 = Pattern.compile(" (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})V(\\d{1,3})\\s+");

    public static final Pattern variableVisibility5 = Pattern.compile(" (VIS|VSBY) (\\d{1,3})V(\\d{1,3})\\s+");

    public static final Pattern variableVisibility6 = Pattern.compile(" (VIS|VSBY) (\\d{1,2})/(\\d{1,2})V(\\d{1,3})\\s+");

    public static final Pattern visibilityKM = Pattern.compile(" M1/4KM\\s+|<1/4KM\\s+");

    public static final Pattern visibilitySM = Pattern.compile(" M1/4SM\\s+|<1/4SM\\s+");

    public static final Pattern visibilitySMKM = Pattern.compile(" (\\d{1,4}) (\\d{1,3})/(\\d{1,3})(SM|KM)\\s+");

    public static final Pattern FROPA = Pattern.compile(" FROPA\\s+");

    public static final Pattern NOSIG = Pattern.compile(" NOSIG\\s+");

    public static final Pattern PWINO = Pattern.compile(" PWINO\\s+");

    public static final Pattern PRESFR = Pattern.compile(" PRESFR/?\\s+");

    public static final Pattern PRESRR = Pattern.compile(" PRESRR/?\\s+");

    public static final Pattern SLPNO = Pattern.compile(" SLPNO\\s+");

    public static final Pattern SLP = Pattern.compile(" SLP\\s?(\\d{3})\\s+");

    public static final Pattern TSNO = Pattern.compile(" TSNO\\s+");

    public static final Pattern peak_wind_speed = Pattern.compile(" PK WND\\s+(\\d{3})(\\d{1,3})/(\\d\\d)?(\\d\\d)\\s+");

    public static final Pattern runway = Pattern.compile(" (R(\\d{2})(R|RR|L|LL|C)?/(M|P)?(\\d{1,4})V?(M|P)?(\\d{1,4})?(FT|N|D)?)\\s+");

    public static final Pattern REMARKS = Pattern.compile("(RMK|REMARKS)");

    public static final Pattern RVRNO = Pattern.compile(" RVRNO\\s+");

    //public static final Pattern space = Pattern.compile(" ");

    public static final Pattern spaces = Pattern.compile("\\s+");

    public static final Pattern surface_visibility1 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern surface_visibility2 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern surface_visibility3 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,3})\\s+");

    public static final Pattern SNINCR = Pattern.compile(" SNINCR (\\d{1,3})/(\\d{1,3})\\s+");

    public static final Pattern snowDepth = Pattern.compile(" 4/(\\d{1,3})\\s+");

    public static final Pattern vertical_VIS = Pattern.compile(" VV(\\d{3})\\s+");

    public static final Pattern maintenace = Pattern.compile(" \\$\\s+");

    public static final Pattern maxTemperature = Pattern.compile(" 1(0|1|/)(\\d{3}|///)\\s+");

    public static final Pattern minTemperature = Pattern.compile(" 2(0|1|/)(\\d{3}|///)\\s+");

    public static final Pattern maxMinTemp24 = Pattern.compile(" 4(0|1|/)(\\d{3}|///)(0|1|/)(\\d{3}|///)\\s+");

    public static final Pattern peakWind = Pattern.compile(" PK WND (\\d{3})(\\d{1,3})/(\\d\\d)?(\\d\\d)\\s+");

    //public static final Pattern plainText = Pattern.compile(" (\\w.*)\\s+");

    public static final Pattern precipitation = Pattern.compile(" 6(\\d{4}|////)\\s+");

    public static final Pattern precipitation24 = Pattern.compile(" 7(\\d{4}|////)\\s+");

    public static final Pattern pressureTendency = Pattern.compile(" 5(0|1|2|3|4|5|6|7|8)(\\d{3}|///)\\s+");

    public static final Pattern sunShine = Pattern.compile(" 98(\\d{1,3}|///)\\s+");

    public static final Pattern tornado = Pattern.compile(" (TORNADO\\w{0,2}|WATERSPOUTS.*|FUNNEL CLOUDS.*)\\s+");

    public static final Pattern tornadoTime = Pattern.compile(" (B|E)(\\d\\d)?(\\d\\d)\\s+");

    public static final Pattern tornadoLocation = Pattern.compile(" (DSNT|VCY STN|VC STN|VCY|VC)\\s+");

    public static final Pattern tornadoDirection = Pattern.compile(" (NE|NW|SE|SW|N|S|E|W)\\s+");

    public static final Pattern VIRGA = Pattern.compile(" VIRGA( DSNT)?( NE| NW| SE| SW| N| S| E| W)?\\s+");

    public static final Pattern waterEquiv = Pattern.compile(" 933(\\d{3})\\s+");

    public static final Pattern windShift = Pattern.compile(" WSHFT (\\d\\d)?(\\d\\d)\\s+");

}
