package thredds.servlet.ldm;

/*  a class to contain all the patterns in the ldm  THREDDS servers
 *
 *  Author Robb Kambic
 *  Date  3/4/05
 *
 */

import java.util.regex.Pattern;

public class ThreddsServerPatterns {

    public static final Pattern p_9999S = Pattern.compile("9999\\s*");

    public static final Pattern p_99GBD3GE = Pattern.compile("99(\\d{3})");

    public static final Pattern p_ADDE_i = Pattern.compile("ADDE", Pattern.CASE_INSENSITIVE);

    public static final Pattern p_ACFT = Pattern.compile("\\(?ACFT\\s?MSHP\\)?\\s+");

    public static final Pattern p_ascii_i = Pattern.compile("ascii", Pattern.CASE_INSENSITIVE);

    public static final Pattern p_ASCIIServer_i = Pattern.compile("ASCIIServer", Pattern.CASE_INSENSITIVE);

    public static final Pattern p_AUTOS = Pattern.compile("AUTO\\s*");

    public static final Pattern p_automatic_report = Pattern.compile("(A01|A01A|A02|A02A|AO1|AO1A|AO2|AO2A|AOA)\\s+");

    public static final Pattern p_B_pound = Pattern.compile("^#");

    public static final Pattern p_B_slash = Pattern.compile("^\\/");

    public static final Pattern p_B_2 = Pattern.compile("^2");

    public static final Pattern p_B_name_version = Pattern.compile("^.*name=\"(.*)\".*version.*>");

    public static final Pattern p_B_D8 = Pattern.compile("^\\d{8}");

    public static final Pattern p_D8_D4 = Pattern.compile("\\d{8}_\\d{4}");

    public static final Pattern p_B_CR = Pattern.compile("^\\n");

    //public static final Pattern p_B_GBALLGECR = Pattern.compile("^(.*)\\n");

    //public static final Pattern p_B_GBW6GE = Pattern.compile("^(\\w{6})");

    public static final Pattern p_B_metar = Pattern.compile("^(METAR|SPECI|TESTM|TESTS) ");

    public static final Pattern p_B_P = Pattern.compile("^P");

    //public static final Pattern p_B_SLTaccess = Pattern.compile("^\\s*<access");

    //public static final Pattern p_B_SLTaccessSM = Pattern.compile("^\\s*<access\\s+");

    //public static final Pattern p_B_SLTaccessSserviceName_urlPath = Pattern.compile("^\\s*<access serviceName=\"(.*)\"\\s+urlPath=\"(.*)\"");

    //public static final Pattern p_B_SLTcatalog = Pattern.compile("^\\s*<catalog");

    //public static final Pattern p_B_SLTcatalogRef = Pattern.compile("^\\s*<catalogRef");

    //public static final Pattern p_B_SLTcatalogRefS = Pattern.compile("^\\s*<catalogRef\\s*");

    //public static final Pattern p_B_SLTcatalogRefSxlinkxlink = Pattern.compile("^\\s*<catalogRef xlink:href=\"(.*)\"\\s+xlink:title=\"(.*)\"");

    //public static final Pattern p_B_SLTcatalogRefSxlinkdetail = Pattern.compile("^\\s*<catalogRef xlink:href=\"(((\\w|\\d|,|\\(|\\)|:|/)+\\s*)+)\"\\s*");

    //public static final Pattern p_B_SLTcatalognameversion = Pattern.compile("^\\s*<catalog .* name=\"(.*)\"\\s+version=\"(.*)\">");

    //public static final Pattern p_B_SLTdataset = Pattern.compile("^\\s*<dataset");

    //public static final Pattern p_B_SLTdatasetDesc = Pattern.compile("^\\s*<datasetDesc");

    //public static final Pattern p_B_SLTdatasetDescSxlink = Pattern.compile("^\\s*<datasetDesc xlink:href=\"(.*)\" />");

    //public static final Pattern p_B_SLTdatasetDescSxmlns = Pattern.compile("^\\s*<datasetDesc xmlns:xlink=\"(.*)\" xlink:href=\"(.*)\" />");

    //public static final Pattern p_B_SLTdatasetGT = Pattern.compile("^\\s*<\\/dataset>");

    //public static final Pattern p_B_SLTdatasetSM = Pattern.compile("^\\s*<dataset\\s+");

    //public static final Pattern p_B_SLTdatasetSname = Pattern.compile("^\\s*<dataset name=\"(.*)\"\\s+");

    //public static final Pattern p_B_SLTdatasetSnameid = Pattern.compile("^\\s*<dataset name=\"(.*)\"\\s+ID=\"(.*)\"");

    //public static final Pattern p_B_SLTdatasetSnamedetail1 = Pattern.compile("^\\s*<dataset name=\"(((\\w|\\d|,|\\(|\\))+\\s*)+)\"\\s*");

    //public static final Pattern p_B_SLTdatasetSnamedetail2 = Pattern.compile("^\\s*<dataset name=\"((\\w+\\s*)+)\"\\s*");

    //public static final Pattern p_B_SLTdatasetSnamedetail3 = Pattern.compile("^\\s*<dataset name=\"((\\w+\\s*)+)\"\\s+ID=\"((\\w+\\s*)+)\"");

    //public static final Pattern p_B_SLTdocumentation = Pattern.compile("^\\s*<documentation");

    //public static final Pattern p_B_SLTdocumentationtag = Pattern.compile("^\\s*<documentation>(.*)</documentation>");

    //public static final Pattern p_B_SLTdocumentationSxlink = Pattern.compile("^\\s*<documentation xlink:href=\"(.*)\"");

    //public static final Pattern p_B_SLTmetadataGT = Pattern.compile("^\\s*<\\/metadata>");

    //public static final Pattern p_B_SLTmetadataSM = Pattern.compile("^\\s*<metadata\\s+");

    //public static final Pattern p_B_SLTpropertySM = Pattern.compile("^\\s*<property\\s+");

    //public static final Pattern p_B_SLTservice = Pattern.compile("^\\s*<service");

    //public static final Pattern p_B_SLTserviceSM = Pattern.compile("^\\s*<service\\s+");

    //public static final Pattern p_B_SLTserviceSMname = Pattern.compile("^\\s*<service\\s+name=\"(.*)\"\\s+serviceType=\"(.*)\"\\s+base=\"(.*)\"");

    //public static final Pattern p_B_W2ZD3toGBD6GE = Pattern.compile("^\\w{2}Z\\d{3}-(\\d{6})");

    //public static final Pattern p_B_W2ZD3toGBD6GEtoCR = Pattern.compile("^\\w{2}Z\\d{3}-(\\d{6})-\\n(.*)\\n");

    public static final Pattern p_FIRST = Pattern.compile("FIRST\\s+");

    public static final Pattern p_FZRANO = Pattern.compile("FZRANO\\s+");

    public static final Pattern p_LAST = Pattern.compile("LAST\\s+");

    public static final Pattern p_NOSPECI = Pattern.compile("NOSPECI\\s+");

    public static final Pattern p_PNO = Pattern.compile("PNO\\s+");

    public static final Pattern p_station = Pattern.compile("^(\\w{4})\\s+");

    public static final Pattern p_station_dateZ1 = Pattern.compile("^\\w{4} \\d{2}(\\d{2})(\\d{2})Z");

    public static final Pattern p_ddhhmmZ = Pattern.compile("(\\d{2})(\\d{2})(\\d{2})Z\\s+");

    public static final Pattern p_isodate = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})");

    public static final Pattern p_station_dateZ = Pattern.compile("^\\w{4} (\\d{6,8})Z");

    //public static final Pattern p_B_W6toGBD6GE = Pattern.compile("^\\w{6}-(\\d{6})");

    //public static final Pattern p_B_WplusSD6GBD2GEGBD2GEZ = Pattern.compile("^\\w{3,5} \\d{6}(\\d{2})(\\d{2})Z");

    //public static final Pattern p_B_WplusSD6GBD2GEGBD2GEZSGBW4GE = Pattern.compile("^\\w{3,5} \\d{6}(\\d{2})(\\d{2})Z (\\w{4})");

    //public static final Pattern p_B_WplusSD6GBDplusGEZ = Pattern.compile("^\\w{3,5} \\d{6}(\\d{4,6})Z");

    //public static final Pattern p_B_WplusSW4SD6GBD2GEGBD2GEZ = Pattern.compile("^\\w{3,7} \\w{4} \\d{6}(\\d{2})(\\d{2})Z");

    //public static final Pattern p_B_WplusSW4SD6GBDplusGEZ = Pattern.compile("^\\w{3,5} \\w{4} \\d{6}(\\d{4,6})Z");

    //public static final Pattern p_B_http = Pattern.compile("^http");

    public static final Pattern p_B_nids_D8 = Pattern.compile("^nids_\\d{8}");

    //public static final Pattern p_B_toGBD6GEtoCR = Pattern.compile("^-(\\d{6})-\\n");

    public static final Pattern p_CAVOKS = Pattern.compile("CAVOK\\s*");

    //public static final Pattern p_DEFAULT_i = Pattern.compile("DEFAULT", Pattern.CASE_INSENSITIVE);

    public static final Pattern p_DODS_i = Pattern.compile("DODS", Pattern.CASE_INSENSITIVE);

    public static final Pattern p_altimeter = Pattern.compile("(A|Q)(\\d{4}\\.?\\d?)\\s*");

    public static final Pattern p_CIG = Pattern.compile("CIG (\\d{1,4})V(\\d{1,4})\\s+");

    public static final Pattern p_CIG_EST = Pattern.compile("CIG(E)?(\\d{3})\\s+");

    public static final Pattern p_CIGNO = Pattern.compile("CIGNO\\s+");

    public static final Pattern p_CIG_RY = Pattern.compile("CIG (\\d3) (RY\\d{1,2})\\s+");

    public static final Pattern p_CLR_or_SKC = Pattern.compile("(CLR|SKC)\\s*");

    public static final Pattern p_min_max_wind_dir = Pattern.compile("^(\\d{3})V(\\d{3})\\s*");

    public static final Pattern p_visibility_direction = Pattern.compile("(\\d{4})((NE)|(NW)|(SE)|(SW)|(N)|(S)|(E)|(W))\\s*");

    //public static final Pattern p_GBD4GEGBD2GEGBD2GE_GBD2GEGBD2GE = Pattern.compile("(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)_(\\d\\d)(\\d\\d)$");

    //public static final Pattern p_GBDDGEGBD3GE = Pattern.compile("(\\d\\d)(\\d{3})");

    //public static final Pattern p_GBDGEGB5GE = Pattern.compile("(\\d)(.{5})");

    //public static final Pattern p_GBDGEGBD4GE = Pattern.compile("(\\d)(\\d{4})");

    public static final Pattern p_visibility2 = Pattern.compile("(\\d{1,3})/(\\d{1,3})(KM|SM)\\s*");

    public static final Pattern p_visibility3 = Pattern.compile("(\\d{1,4})(KM|SM)\\s*");

    public static final Pattern p_visibility1 = Pattern.compile("(\\d{1,4}) (\\d{1,3})/(\\d{1,3})(SM|KM)\\s*");

    public static final Pattern p_hourly_precip = Pattern.compile("P ?(\\d{1,5})\\s+");

    public static final Pattern p_wind_direction_speed = Pattern.compile("(E|W|N|S)?(\\d{3}|VRB)(\\d{2,3})(G)?(\\d{2,3})?(KMH|KT|MPS|MPH)\\s+");

    public static final Pattern p_temperature = Pattern.compile("(M|-)?(\\d{2})/(M|-)?(\\d{2})?\\s*");

    public static final Pattern p_temperature_tenths = Pattern.compile("T(0|1)(\\d{3})(0|1)?(\\d{3})?\\s+");

    public static final Pattern p_COR = Pattern.compile("COR\\s+");

    public static final Pattern p_cloud_cover = Pattern.compile("(\\+|-)?(OVC|SCT|FEW|BKN)(\\d{3})(\\w{1,3})?\\s*");

    public static final Pattern p_cloud_height = Pattern.compile("8/(\\d|/)(\\d|/)(\\d|/)/?\\s+");

    public static final Pattern p_weather = Pattern.compile("(\\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?\\s+");

    public static final Pattern p_recentWeather = Pattern.compile("(\\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?B(\\d{2,4})E(\\d{2,4})\\s+" ); 

    public static final Pattern p_recentWeather1 = Pattern.compile("(\\+|-|VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?(B|E)(\\d{2,4})\\s+" ); 

    public static final Pattern p_GT = Pattern.compile(">");

    //public static final Pattern p_GTGBALLGELTdocumentationGT = Pattern.compile(">(.*)</documentation>");

    public static final Pattern p_GR = Pattern.compile("GS\\s+");

    public static final Pattern p_GR1 = Pattern.compile("GR M1/4\\s+");

    public static final Pattern p_GR2 = Pattern.compile("GR (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_GR3 = Pattern.compile("GR (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_GR4 = Pattern.compile("GR (\\d{1,3})\\s+");

    public static final Pattern p_ID = Pattern.compile("ID=");

    //public static final Pattern p_IDGBALLGES = Pattern.compile("ID=\"(.*)\"\\s*");

    //public static final Pattern p_IDGBGBWMSGEMGES = Pattern.compile("ID=\"((\\w+\\s*)+)\"\\s*");

    public static final Pattern p_Lightning = Pattern.compile("(OCNL|FRQ|CNS) LTG\\s?(CG|IC|CC|CA)\\s?(DSNT|AP|VCY STN|VCNTY STN)?\\s?(NE|NW|SE|SW|N|S|E|W)?\\s+");

    public static final Pattern p_LT = Pattern.compile("<");

    //public static final Pattern p_LTexplanation = Pattern.compile("<!--");

    public static final Pattern p_NILS = Pattern.compile("NIL\\s*");

    public static final Pattern p_obscuring = Pattern.compile("-X(VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)?(\\d)\\s+");

    public static final Pattern p_obscuringPhen = Pattern.compile("(VC|PR)?(MI|BC|DR|BL|SH|TS|FZ)?(DZ|RA|SN|SG|IC|PE|PL|GR|GS|UP)?(BR|FG|FU|VA|DU|SA|HZ|PY)?(PO|SQ|FC|SS|DS)? (FEW|SCT|BKN|OVC)(\\d3)\\s+" ); 

    public static final Pattern p_variableSky = Pattern.compile("(FEW|SCT|BKN|OVC)(\\d{3})? V (FEW|SCT|BKN|OVC)\\s+");

    public static final Pattern p_Visibility2ndSite1 = Pattern.compile("(VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2}) (RY\\d{1,2})\\s+");

    public static final Pattern p_Visibility2ndSite2 = Pattern.compile("(VIS|VSBY) (\\d{1,3}) (RY\\d{1,2})\\s+");

    public static final Pattern p_Visibility2ndSite3 = Pattern.compile("(VIS|VSBY) (\\d{1,2})/(\\d{1,2}) (RY\\d{1,2})\\s+");

    public static final Pattern p_sectorVisibility1 = Pattern.compile("(VIS|VSBY) (NE|NW|SE|SW|N|S|E|W)(\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_sectorVisibility2 = Pattern.compile("(VIS|VSBY) (NE|NW|SE|SW|N|S|E|W) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_sectorVisibility3 = Pattern.compile("(VIS|VSBY) (NE|NW|SE|SW|N|S|E|W)(\\d{1,3})\\s+");

    public static final Pattern p_significantCloud = Pattern.compile("(CB|CBMAM|TCU|ACC|SCSL|ACSL|ROTOR CLD|ROPE|ROPE CLD)\\s+");

    public static final Pattern p_significantCloud1 = Pattern.compile("^(VCNTY STN|VCY STN|VC STN|VCY|VC|DSNT|OMT)\\s+");

    public static final Pattern p_significantCloud2 = Pattern.compile("^(NE|NW|SE|SW|N|S|E|W)(\\-| MOV )?(NE|NW|SE|SW|N|S|E|W)?/?\\s+" ) ;

    public static final Pattern p_surfaceVisibility1 = Pattern.compile("SFC (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_surfaceVisibility2 = Pattern.compile("SFC (VIS|VSBY) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_surfaceVisibility3 = Pattern.compile("SFC (VIS|VSBY) (\\d{1,3})\\s+");

    public static final Pattern p_towerVisibility1 = Pattern.compile("TWR (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_towerVisibility2 = Pattern.compile("TWR (VIS|VSBY) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_towerVisibility3 = Pattern.compile("TWR (VIS|VSBY) (\\d{1,3})\\s+");

    public static final Pattern p_variableVisibility1 = Pattern.compile("(VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})V(\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_variableVisibility2 = Pattern.compile("(VIS|VSBY) (\\d{1,3})V(\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_variableVisibility3 = Pattern.compile("(VIS|VSBY) (\\d{1,2})/(\\d{1,2})V(\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_variableVisibility4 = Pattern.compile("(VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})V(\\d{1,3})\\s+");

    public static final Pattern p_variableVisibility5 = Pattern.compile("(VIS|VSBY) (\\d{1,3})V(\\d{1,3})\\s+");

    public static final Pattern p_variableVisibility6 = Pattern.compile("(VIS|VSBY) (\\d{1,2})/(\\d{1,2})V(\\d{1,3})\\s+");

    public static final Pattern p_visibilityKM = Pattern.compile("M1/4KM\\s+|<1/4KM\\s+");

    public static final Pattern p_visibilitySM = Pattern.compile("M1/4SM\\s+|<1/4SM\\s+");

    public static final Pattern p_visibilitySMKM = Pattern.compile("(\\d{1,4}) (\\d{1,3})/(\\d{1,3})(SM|KM)\\s*");

    public static final Pattern p_FROPA = Pattern.compile("FROPA\\s*");

    public static final Pattern p_NOSIG = Pattern.compile("NOSIG\\s*");

    public static final Pattern p_PWINO = Pattern.compile("PWINO\\s*");

    public static final Pattern p_PRESFR = Pattern.compile("PRESFR/?\\s+");

    public static final Pattern p_PRESRR = Pattern.compile("PRESRR/?\\s+");

    public static final Pattern p_SLPNO = Pattern.compile("SLPNO\\s*");

    public static final Pattern p_SLP = Pattern.compile("SLP\\s?(\\d{3})\\s+");

    public static final Pattern p_TSNO = Pattern.compile("TSNO\\s*");

    //public static final Pattern p_PGBDplusGEKMS = Pattern.compile("P(\\d{1,3})KM\\s*");

    //public static final Pattern p_PGBDplusGESMS = Pattern.compile("P(\\d{1,3})SM\\s*");

    public static final Pattern p_peak_wind_speed = Pattern.compile("PK WND\\s+(\\d{3})(\\d{1,3})/(\\d\\d)?(\\d\\d)\\s+");

    //public static final Pattern p_PSGBDplusGESM = Pattern.compile("P ?(\\d{1,5})\\s+");

    public static final Pattern p_runway = Pattern.compile("R(\\d{2})(R|RR|L|LL|C)?/(M|P)?(\\d{1,4})V?(M|P)?(\\d{1,4})?(FT|N|D)?\\s+");

    public static final Pattern p_REMARKS = Pattern.compile("(RMK|REMARKS)\\s+");

    public static final Pattern p_RVRNOS = Pattern.compile("RVRNO\\s*");

    public static final Pattern p_space = Pattern.compile(" ");

    public static final Pattern p_space20 = Pattern.compile("%20");

    public static final Pattern p_spaces = Pattern.compile("\\s+");

    public static final Pattern p_surface_visibility1 = Pattern.compile("SFC (VIS|VSBY) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_surface_visibility2 = Pattern.compile("SFC (VIS|VSBY) (\\d{1,3}) (\\d{1,2})/(\\d{1,2})\\s+");

    public static final Pattern p_surface_visibility3 = Pattern.compile("SFC (VIS|VSBY) (\\d{1,3})\\s+");

    public static final Pattern p_SLPNOSM = Pattern.compile("SLPNO\\s+");

    //public static final Pattern p_SLPSGBD3GESM = Pattern.compile("SLP\\s?(\\d{3})\\s+");

    //public static final Pattern p_SLTexplanation = Pattern.compile("\\s*<!--");

    public static final Pattern p_station_name = Pattern.compile("\\s*<station\\s+name");

    //public static final Pattern p_SM = Pattern.compile("\\s+");

    public static final Pattern p_SNINCR = Pattern.compile("SNINCR (\\d{1,3})/(\\d{1,3})\\s+");

    public static final Pattern p_snowDepth = Pattern.compile("4/(\\d1,3)\\s+");

    public static final Pattern p_vertical_VIS = Pattern.compile("VV(\\d{3})\\s+");

    public static final Pattern p_TGB0_or_1GEGBD3GEGB0_or_1GEGBD3GESM = Pattern.compile("T(0|1)(\\d{3})(0|1)?(\\d{3})?\\s+");

    public static final Pattern p_VIRGA = Pattern.compile("VIRGA (DSNT )?(NE|NW|SE|SW|N|S|E|W)?\\s+");

    //public static final Pattern p_VVGBD3GES = Pattern.compile("VV(\\d{3})\\s*");

    public static final Pattern p_config = Pattern.compile("(\\w+)\\s*=\\s+?(.*)");

    public static final Pattern p_station_dateZ2 = Pattern.compile("\\w{4} (\\d{2})(\\d{2})(\\d{2})?Z ");

    public static final Pattern p_bad_date = Pattern.compile("\\d{4,6}Z.*\\d{4,6}Z");

    //public static final Pattern p_WplusSGBDplusZGES = Pattern.compile("\\w{3,5} (\\d{10}Z) ");

    public static final Pattern p_HTTPServer_i = Pattern.compile("HTTPServer", Pattern.CASE_INSENSITIVE);

    //public static final Pattern p_aliasGBALLGES = Pattern.compile("alias=\"(.*)\"\\s*");

    public static final Pattern p_all_i = Pattern.compile("all", Pattern.CASE_INSENSITIVE);

    //public static final Pattern p_base = Pattern.compile("base=\"([A-Za-z0-9 ()_,-:/.]*)\"\\s*");

    public static final Pattern p_cM = Pattern.compile("\\cM");

    public static final Pattern p_catalog_i = Pattern.compile("catalog", Pattern.CASE_INSENSITIVE);

    public static final Pattern p_colon = Pattern.compile(":");

    //public static final Pattern p_dataTypeGBGBWMSGEMGES = Pattern.compile("dataType=\"((\\w+\\s*)+)\"\\s*");

    public static final Pattern p_data_i = Pattern.compile("data", Pattern.CASE_INSENSITIVE);

    public static final Pattern p_html_i = Pattern.compile("html", Pattern.CASE_INSENSITIVE);

    public static final Pattern p_hyphen = Pattern.compile("-");

    public static final Pattern p_latitude_longitude = Pattern.compile("latitude=\"([-.0-9]*)\"\\s+longitude=\"([-.0-9]*)\"");

    public static final Pattern p_maintenace = Pattern.compile("\\$\\s+");

    public static final Pattern p_maxTemperature = Pattern.compile("1(0|1|/)(\\d{3}|///)\\s+");

    public static final Pattern p_minTemperature = Pattern.compile("2(0|1|/)(\\d{3}|///)\\s+");

    public static final Pattern p_maxMinTemp24 = Pattern.compile("4(0|1|/)(\\d{3}|///)(0|1|/)(\\d{3}|///)\\s+");

    //public static final Pattern p_metadataTypeGBGBWMSGEMGES = Pattern.compile("metadataType=\"((\\w+\\s*)+)\"\\s*");

    //public static final Pattern p_name_serviceType_base = Pattern.compile("name=\"(.*)\"\\s+serviceType=\"(.*)\"\\s+base=\"(.*)\"");

    //public static final Pattern p_nameGBGBWMSGEMGES = Pattern.compile("name=\"((\\w+\\s*)+)\"\\s*");

    public static final Pattern p_name_value1 = Pattern.compile("name=\"([A-Za-z0-9 ()_,-:]*)\"{1}?\\s*");

    public static final Pattern p_name_value2 = Pattern.compile("name=\"([A-Za-z0-9 ()_,-:]*)\"\\s*");

    public static final Pattern p_peakWind = Pattern.compile("PK WND (\\d{3})(\\d{1,3})/(\\d\\d)?(\\d\\d)\\s+");

    public static final Pattern p_plainText = Pattern.compile("(\\w.*)\\s+");

    public static final Pattern p_precipitation = Pattern.compile("6(\\d{4}|////)\\s+");

    public static final Pattern p_precipitation24 = Pattern.compile("7(\\d{4}|////)\\s+");

    public static final Pattern p_pressureTendency = Pattern.compile("5(0|1|2|3|4|5|6|7|8)(\\d{3}/?|///)\\s+");

    public static final Pattern p_qc_or_dqc_i = Pattern.compile("(qc|dqc)", Pattern.CASE_INSENSITIVE);

    public static final Pattern p_serviceName_urlPath = Pattern.compile("serviceName=\"(.*)\"\\s+urlPath=\"(.*)\"");

    public static final Pattern p_serviceName = Pattern.compile("serviceName=\"((\\w+\\s*)+)\"\\s*");

    public static final Pattern p_serviceName_or_urlPath = Pattern.compile("serviceName|urlPath");

    public static final Pattern p_serviceType = Pattern.compile("serviceType=\"([A-Za-z0-9 ()_,-:]*)\"\\s*");

    public static final Pattern p_setSsetM = Pattern.compile("[ ]+");

    public static final Pattern p_setSTABCRsetM = Pattern.compile("[ \\t\\n]+");

    public static final Pattern p_slash = Pattern.compile("/");

    public static final Pattern p_slashslash = Pattern.compile("//");

    public static final Pattern p_sunShine = Pattern.compile("98(\\d{1,3}|///)\\s+");

    public static final Pattern p_tornado = Pattern.compile("(TORNADO\\w{0,2}|WATERSPOUTS.*|FUNNEL CLOUDS.*)\\s+");

    public static final Pattern p_tornadoTime = Pattern.compile("(B|E)(\\d\\d)(\\d\\d)?\\s+");

    public static final Pattern p_tornadoLocation = Pattern.compile("^(DSNT|VCY STN|VC STN|VCY|VC)\\s+");

    public static final Pattern p_tornadoDirection = Pattern.compile("^(NE|NW|SE|SW|N|S|E|W)\\s+");

    public static final Pattern p_urlPath = Pattern.compile("urlPath=\"(.*)\"\\s*");

    public static final Pattern p_urlPath_or_ID_or_alias = Pattern.compile("urlPath|ID|alias");

    public static final Pattern p_value1 = Pattern.compile("value=\"(.*)\"\\s*");

    public static final Pattern p_value2 = Pattern.compile("value=\"([A-Z0-9]*)\"");

    public static final Pattern p_waterEquiv = Pattern.compile("933(\\d3)\\s+");

    public static final Pattern p_windShift = Pattern.compile("WSHFT (\\d\\d)?(\\d\\d)\\s+");

    //public static final Pattern p_xlinkcolonhref1 = Pattern.compile("xlink:href=\"(.*)\"");

   // public static final Pattern p_xlinkcolonhref2 = Pattern.compile("xlink:href=\"([A-Za-z0-9/_.-:]*)\"\\s*");

    //public static final Pattern p_xlinkcolontitle1 = Pattern.compile("xlink:title=\"(.*)\"");

    //public static final Pattern p_xlinkcolontitle2 = Pattern.compile("xlink:title=\"([A-Za-z0-9/_,-:() ]*)\"\\s*");

    public static final Pattern p_xml_i = Pattern.compile("xml", Pattern.CASE_INSENSITIVE);

}
