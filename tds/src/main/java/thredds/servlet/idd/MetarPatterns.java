package thredds.servlet.idd;

/*  a class to contain all the patterns in the ldm  THREDDS servers
 *
 *  Author Robb Kambic
 *  Date  3/4/07
 *
 */

import java.util.regex.Pattern;

public class MetarPatterns {

    public static final Pattern p_9999 = Pattern.compile(" 9999\\s+");

    public static final Pattern p_ACFT = Pattern.compile(" \\(?ACFT( |_)?MSHP\\)?\\s+");

    public static final Pattern p_AUTOS = Pattern.compile(" AUTO\\s+");

    public static final Pattern p_automatic_report = Pattern.compile(" (A01|A01A|A02|A02A|AO1|AO1A|AO2|AO2A|AOA)\\s+");

    //public static final Pattern p_B_slash = Pattern.compile("^\\/");

    public static final Pattern p_B_CR = Pattern.compile("^\\n");

    public static final Pattern p_B_metar = Pattern.compile("^(METAR|SPECI|TESTM|TESTS) ");

    //public static final Pattern p_B_P = Pattern.compile("^P");

    public static final Pattern p_FIRST = Pattern.compile(" FIRST\\s+");

    public static final Pattern p_FZRANO = Pattern.compile(" FZRANO\\s+");

    public static final Pattern p_LAST = Pattern.compile(" LAST\\s+");

    public static final Pattern p_NOSPECI = Pattern.compile(" NOSPECI\\s+");

    public static final Pattern p_PNO = Pattern.compile(" PNO\\s+");

    public static final Pattern p_station = Pattern.compile("^(\\w{4})\\s+");

    //public static final Pattern p_station_dateZ1 = Pattern.compile("^\\w{4} \\d{2}(\\d{2})(\\d{2})Z");

    public static final Pattern p_ddhhmmZ = Pattern.compile(" (\\d{2})(\\d{2})(\\d{2})Z\\s+");

    //public static final Pattern p_isodate = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})");

    //public static final Pattern p_station_dateZ = Pattern.compile("^\\w{4} (\\d{6,8})Z");

    public static final Pattern p_CAVOKS = Pattern.compile(" CAVOK\\s+");

    public static final Pattern p_altimeter = Pattern.compile(" (A|Q)(\\d{4}\\.?\\d?)\\s+");

    public static final Pattern p_CIG = Pattern.compile(" CIG (\\d{1,4})V(\\d{1,4})\\s+");

    public static final Pattern p_CIG_EST = Pattern.compile(" CIG(E)? (\\d{3})\\s+");

    public static final Pattern p_CIGNO = Pattern.compile(" CIGNO\\s+");

    public static final Pattern p_CIG_RY = Pattern.compile(" CIG (\\d{3}) (RY\\d{1,2})\\s+");

    public static final Pattern p_CLR_or_SKC = Pattern.compile(" (CLR|SKC)\\s+");

    public static final Pattern p_min_max_wind_dir = Pattern.compile(" (\\d{3})V(\\d{3})\\s+");

    public static final Pattern p_visibility_direction = Pattern.compile(" (\\d{4})((NE)|(NW)|(SE)|(SW)|(N)|(S)|(E)|(W))\\s+");

    public static final Pattern p_visibility2 = Pattern.compile(" (\\d{1,3})/(\\d{1,3})(KM|SM)\\s+");

    public static final Pattern p_visibility3 = Pattern.compile(" P?(\\d{1,4})(KM|SM)\\s+");

    public static final Pattern p_visibility1 = Pattern.compile(" (\\d{1,4}) (\\d{1,3})/(\\d{1,3})(SM|KM)\\s+");

    public static final Pattern p_hourly_precip = Pattern.compile(" P ?(\\d{1,5})\\s+");

    public static final Pattern p_wind_direction_speed = Pattern.compile(" (E|W|N|S)?(\\d{3}|VRB)(\\d{2,3})(G)?(\\d{2,3})?(KMH|KT|MPS|MPH)\\s+");

    public static final Pattern p_temperature = Pattern.compile(" (M|-)?(\\d{2})/(M|-)?(\\d{2})?\\s+");

    public static final Pattern p_temperature_tenths = Pattern.compile(" T(0|1)(\\d{3})(0|1)?(\\d{3})?\\s+");

    public static final Pattern p_COR = Pattern.compile(" COR\\s+");

    public static final Pattern p_cloud_cover = Pattern.compile(" (\\+|-)?(OVC|SCT|FEW|BKN)(\\d{3})(\\w{1,3})?\\s+");

    public static final Pattern p_cloud_height = Pattern.compile(" 8/(\\d|/)(\\d|/)(\\d|/)\\s+");

    //public static final Pattern p_weather = Pattern.compile(" (\\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?\\s+");

    //public static final Pattern p_recentWeather = Pattern.compile(" (\\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?B(\\d{2,4})E(\\d{2,4})\\s+" );

    //public static final Pattern p_recentWeather1 = Pattern.compile(" (\\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?(B|E)(\\d{2,4})\\s+" );

    public static final Pattern p_GR = Pattern.compile(" GS\\s+");

    public static final Pattern p_GR1 = Pattern.compile(" GR M1/4\\s+");

    public static final Pattern p_GR2 = Pattern.compile(" GR (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_GR3 = Pattern.compile(" GR (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_GR4 = Pattern.compile(" GR (\\d{1,3})\\s+");

    public static final Pattern p_Lightning = Pattern.compile(" (((OCNL|FRQ|CNS) )?LTG\\s?(CG|IC|CC|CA)?\\s?(DSNT|AP|VCY STN|VCNTY STN)?\\s?(NE|NW|SE|SW|N|S|E|W)?)\\s+");

    public static final Pattern p_NIL = Pattern.compile("NIL\\s*");

    //public static final Pattern p_obscuring = Pattern.compile(" -X(VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?(\\d)\\s+");

    //public static final Pattern p_obscuringPhen = Pattern.compile(" (VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)? (FEW|SCT|BKN|OVC)(\\d3)\\s+" );

    public static final Pattern p_variableSky = Pattern.compile(" (FEW|SCT|BKN|OVC)(\\d{3})? V (FEW|SCT|BKN|OVC)\\s+");

    public static final Pattern p_Visibility2ndSite1 = Pattern.compile(" (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2}) (RY\\d{1,2})\\s+");

    public static final Pattern p_Visibility2ndSite2 = Pattern.compile(" (VIS|VSBY) (\\d{1,3}) (RY\\d{1,2})\\s+");

    public static final Pattern p_Visibility2ndSite3 = Pattern.compile(" (VIS|VSBY) (\\d{1,2})/(\\d{1,2}) (RY\\d{1,2})\\s+");

    public static final Pattern p_sectorVisibility1 = Pattern.compile(" (VIS|VSBY) (NE|NW|SE|SW|N|S|E|W) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_sectorVisibility2 = Pattern.compile(" (VIS|VSBY) (NE|NW|SE|SW|N|S|E|W) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_sectorVisibility3 = Pattern.compile(" (VIS|VSBY) (NE|NW|SE|SW|N|S|E|W) (\\d{1,3})\\s+");

    //public static final Pattern p_significantCloud = Pattern.compile(" (CB|CBMAM|TCU|ACC|SCSL|ACSL|ROTOR CLD|ROPE|ROPE CLD)\\s+");

    //public static final Pattern p_significantCloud1 = Pattern.compile(" (VCNTY STN|VCY STN|VC STN|VCY|VC|DSNT|OMT)\\s+");

    //public static final Pattern p_significantCloud2 = Pattern.compile(" (NE|NW|SE|SW|N|S|E|W)(\\-| MOV )?(NE|NW|SE|SW|N|S|E|W)?/?\\s+" ) ;

    public static final Pattern p_surfaceVisibility1 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_surfaceVisibility2 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_surfaceVisibility3 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,3})\\s+");

    public static final Pattern p_towerVisibility1 = Pattern.compile(" TWR (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_towerVisibility2 = Pattern.compile(" TWR (VIS|VSBY) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_towerVisibility3 = Pattern.compile(" TWR (VIS|VSBY) (\\d{1,3})\\s+");

    public static final Pattern p_variableVisibility1 = Pattern.compile(" (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})V(\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_variableVisibility2 = Pattern.compile(" (VIS|VSBY) (\\d{1,3})V(\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_variableVisibility3 = Pattern.compile(" (VIS|VSBY) (\\d{1,2})/(\\d{1,2})V(\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_variableVisibility4 = Pattern.compile(" (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})V(\\d{1,3})\\s+");

    public static final Pattern p_variableVisibility5 = Pattern.compile(" (VIS|VSBY) (\\d{1,3})V(\\d{1,3})\\s+");

    public static final Pattern p_variableVisibility6 = Pattern.compile(" (VIS|VSBY) (\\d{1,2})/(\\d{1,2})V(\\d{1,3})\\s+");

    public static final Pattern p_visibilityKM = Pattern.compile(" M1/4KM\\s+|<1/4KM\\s+");

    public static final Pattern p_visibilitySM = Pattern.compile(" M1/4SM\\s+|<1/4SM\\s+");

    public static final Pattern p_visibilitySMKM = Pattern.compile(" (\\d{1,4}) (\\d{1,3})/(\\d{1,3})(SM|KM)\\s+");

    public static final Pattern p_FROPA = Pattern.compile(" FROPA\\s+");

    public static final Pattern p_NOSIG = Pattern.compile(" NOSIG\\s+");

    public static final Pattern p_PWINO = Pattern.compile(" PWINO\\s+");

    public static final Pattern p_PRESFR = Pattern.compile(" PRESFR/?\\s+");

    public static final Pattern p_PRESRR = Pattern.compile(" PRESRR/?\\s+");

    public static final Pattern p_SLPNO = Pattern.compile(" SLPNO\\s+");

    public static final Pattern p_SLP = Pattern.compile(" SLP\\s?(\\d{3})\\s+");

    public static final Pattern p_TSNO = Pattern.compile(" TSNO\\s+");

    public static final Pattern p_peak_wind_speed = Pattern.compile(" PK WND\\s+(\\d{3})(\\d{1,3})/(\\d\\d)?(\\d\\d)\\s+");

    public static final Pattern p_runway = Pattern.compile(" (R(\\d{2})(R|RR|L|LL|C)?/(M|P)?(\\d{1,4})V?(M|P)?(\\d{1,4})?(FT|N|D)?)\\s+");

    public static final Pattern p_REMARKS = Pattern.compile("(RMK|REMARKS)");

    public static final Pattern p_RVRNO = Pattern.compile(" RVRNO\\s+");

    //public static final Pattern p_space = Pattern.compile(" ");

    public static final Pattern p_spaces = Pattern.compile("\\s+");

    public static final Pattern p_surface_visibility1 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_surface_visibility2 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_surface_visibility3 = Pattern.compile(" SFC (VIS|VSBY) (\\d{1,3})\\s+");

    public static final Pattern p_SNINCR = Pattern.compile(" SNINCR (\\d{1,3})/(\\d{1,3})\\s+");

    public static final Pattern p_snowDepth = Pattern.compile(" 4/(\\d{1,3})\\s+");

    public static final Pattern p_vertical_VIS = Pattern.compile(" VV(\\d{3})\\s+");

    public static final Pattern p_VIRGA = Pattern.compile(" VIRGA( DSNT)?( NE| NW| SE| SW| N| S| E| W)?\\s+");

    //public static final Pattern p_colon = Pattern.compile(":");

    public static final Pattern p_maintenace = Pattern.compile(" \\$\\s+");

    public static final Pattern p_maxTemperature = Pattern.compile(" 1(0|1|/)(\\d{3}|///)\\s+");

    public static final Pattern p_minTemperature = Pattern.compile(" 2(0|1|/)(\\d{3}|///)\\s+");

    public static final Pattern p_maxMinTemp24 = Pattern.compile(" 4(0|1|/)(\\d{3}|///)(0|1|/)(\\d{3}|///)\\s+");

    public static final Pattern p_peakWind = Pattern.compile(" PK WND (\\d{3})(\\d{1,3})/(\\d\\d)?(\\d\\d)\\s+");

    //public static final Pattern p_plainText = Pattern.compile(" (\\w.*)\\s+");

    public static final Pattern p_precipitation = Pattern.compile(" 6(\\d{4}|////)\\s+");

    public static final Pattern p_precipitation24 = Pattern.compile(" 7(\\d{4}|////)\\s+");

    public static final Pattern p_pressureTendency = Pattern.compile(" 5(0|1|2|3|4|5|6|7|8)(\\d{3}|///)\\s+");

    //public static final Pattern p_slash = Pattern.compile("/");

    //public static final Pattern p_slashslash = Pattern.compile("//");

    public static final Pattern p_sunShine = Pattern.compile(" 98(\\d{1,3}|///)\\s+");

    public static final Pattern p_tornado = Pattern.compile(" (TORNADO\\w{0,2}|WATERSPOUTS.*|FUNNEL CLOUDS.*)\\s+");

    public static final Pattern p_tornadoTime = Pattern.compile(" (B|E)(\\d\\d)?(\\d\\d)\\s+");

    public static final Pattern p_tornadoLocation = Pattern.compile(" (DSNT|VCY STN|VC STN|VCY|VC)\\s+");

    public static final Pattern p_tornadoDirection = Pattern.compile(" (NE|NW|SE|SW|N|S|E|W)\\s+");

    public static final Pattern p_waterEquiv = Pattern.compile(" 933(\\d{3})\\s+");

    public static final Pattern p_windShift = Pattern.compile(" WSHFT (\\d\\d)?(\\d\\d)\\s+");

}
