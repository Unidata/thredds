/* 
 * EDUnits Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;


/** 
 * This class has static methods to convert units from one standard to another.
 */
public class EDUnits { 

    /** UDUNITS and UCUM support metric prefixes. */
    public static String metricName[] = {
        "yotta", "zetta", "exa",   "peta",  "tera", 
        "giga",  "mega",  "kilo",  "hecto", "deka", 
        "deci",  "centi", "milli", "micro", "nano", 
        "pico",  "femto", "atto",  "zepto", "yocto", 
        "µ",     };
    public static String metricAcronym[] = {
        "Y", "Z", "E", "P", "T", 
        "G", "M", "k", "h", "da",
        "d", "c", "m", "u", "n", 
        "p", "f", "a", "z", "y",
        "u"};
    public static int nMetric = metricName.length;

    /** UCUM supports power-of-two prefixes, but UDUNITS doesn't. */
    public static String twoAcronym[] = {
        "Ki", "Mi", "Gi", "Ti"};
    public static String twoValue[] = {
        "1024", "1048576", "1073741824", "1.099511627776e12"}; 
    public static int nTwo = twoAcronym.length;

    // These don't need to be thread-safe because they are read-only after creation.
    private static final HashMap<String,String> udHashMap;
    private static final HashMap<String,String> ucHashMap;

    static {
        try {
            File udunitsToUcumFile = new File(EDUnits.class.getResource("UdunitsToUcum.properties").toURI());
            File ucumToUdunitsFile = new File(EDUnits.class.getResource("UcumToUdunits.properties").toURI());

            udHashMap = getHashMapStringString(udunitsToUcumFile.getAbsolutePath(), "ISO-8859-1");
            ucHashMap = getHashMapStringString(ucumToUdunitsFile.getAbsolutePath(), "ISO-8859-1");
        } catch (URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Set this to true (by calling reallyReallyVerbose=true in your program, 
     * not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyReallyVerbose = false; 


    /** 
     * This is like udunitsToUcum() but won't throw an exception. 
     * If there is trouble, it returns udunits.
     */
    public static String safeUdunitsToUcum(String udunits) {
        try {
           return udunitsToUcum(udunits); 
        } catch (Throwable t) {
            String2.log(String2.ERROR + " while converting udunits=" + udunits + " to ucum:\n" +
                MustBe.throwableToString(t));
            return udunits;
        }
    }
        

    /** 
     * This converts UDUnits to UCUM.
     * <br>UDUnits: http://www.unidata.ucar.edu/software/udunits/udunits-1/etc/udunits.dat
     *              http://www.unidata.ucar.edu/software/udunits/udunits-2/udunits2.html
     *   I worked with v 2.1.9
     * <br>UCUM: http://unitsofmeasure.org/ucum.html
     *   I worked with Version: 1.8, $Revision: 28894 $
     *
     * <p>UDUnits supports lots of aliases (short and long)
     *   and plurals (usually by adding 's'). 
     *   These all get reduced to UCUM's short and canonical-only units.
     *
     * <p>Notes: 
     * <ul>
     * <li>This method is a strictly case sensitive.
     *   The only UDUnits that should be capitalized (other than acronyms) are
     *     Btu, Gregorian..., Julian..., PI.
     *   <br>The only UDUnits that may be capitalized are
     *     Celsius, Fahrenheit, Kelvin, Rankine.
     * <li>For "10 to the", UCUM allows 10* or 10^. This method uses 10^.
     * <li>NTU becomes {ntu}.
     * <li>PSU or psu becomes {psu}.
     * </ul>
     *
     * return the UDUnits converted to UCUM.
     *    null returns null. "" returns "".
     * throws Exception if trouble.
     */
    public static String udunitsToUcum(String udunits) {
        if (udunits == null)
            return null;
        if (reallyReallyVerbose) 
            String2.log("  udunits=" + udunits);

        //is it a point in time? e.g., seconds since 1970-01-01T00:00:00T
        int sincePo = udunits.indexOf(" since ");
        if (sincePo > 0) {
            try {
                //test if really appropriate
                double baf[] = Calendar2.getTimeBaseAndFactor(udunits); //throws exception if trouble

                //use 'factor', since it is more forgiving than udunitsToUcum converter
                String u = baf[1] == 0.001? "ms" :
                           baf[1] == 1? "s"  :
                           baf[1] == Calendar2.SECONDS_PER_MINUTE? "min" :
                           baf[1] == Calendar2.SECONDS_PER_HOUR? "h" :
                           baf[1] == Calendar2.SECONDS_PER_DAY? "d" :
                           baf[1] == 30 * Calendar2.SECONDS_PER_DAY? "mo" :  //mo_j?
                           baf[1] == 360 * Calendar2.SECONDS_PER_DAY? "a" : //a_j?
                           udunitsToUcum(udunits.substring(0, sincePo)); //shouldn't happen, but weeks? microsec?

                //make "s{since 1970-01-01T00:00:00T}
                return u + "{" +
                    udunits.substring(sincePo + 1) + "}";                 
            } catch (Exception e) {                
            }
        }

        //parse udunits and build ucum, till done
        StringBuilder ucum = new StringBuilder();        
        int udLength = udunits.length();
        int po = 0;  //po is next position to be read
        while (po < udLength) {
            if (reallyReallyVerbose && ucum.length() > 0) String2.log("  ucum=" + ucum);
            char ch = udunits.charAt(po);

            //letter  
            if (isUdunitsLetter(ch)) {     //includes 'µ' and '°'
                //find contiguous letters|_|digit (no '-') 
                int po2 = po + 1;
                while (po2 < udLength && 
                    (isUdunitsLetter(udunits.charAt(po2)) || udunits.charAt(po2) == '_' ||
                     String2.isDigit(udunits.charAt(po2)))) 
                    po2++;
                String tUdunits = udunits.substring(po, po2);
                po = po2;

                //some udunits have internal digits, but none end in digits 
                //if it ends in digits, treat as exponent
                //find contiguous digits at end
                int firstDigit = tUdunits.length();
                while (firstDigit >= 1 && String2.isDigit(tUdunits.charAt(firstDigit - 1)))
                    firstDigit--;
                String exponent = tUdunits.substring(firstDigit);
                tUdunits = tUdunits.substring(0, firstDigit);
                String tUcum = oneUdunitsToUcum(tUdunits);

                //deal with PER -> / 
                if (tUcum.equals("/")) {   
                    char lastUcum = ucum.length() == 0? '\u0000' : ucum.charAt(ucum.length() - 1);
                    if (lastUcum == '/') 
                        ucum.setCharAt(ucum.length() - 1, '.'); //2 '/' cancel out
                    else if (lastUcum == '.') 
                        ucum.setCharAt(ucum.length() - 1, '/'); //  '/' replaces '.'
                    else ucum.append('/');

                } else {
                    ucum.append(tUcum);
                }                

                //add the exponent
                ucum.append(exponent);
                //catch -exponent as a number below

                continue;
            }

            //number
            if (ch == '-' || String2.isDigit(ch)) {
                //find contiguous digits
                int po2 = po + 1;
                while (po2 < udLength && String2.isDigit(udunits.charAt(po2))) 
                    po2++;

                //decimal place + digit (not just .=multiplication)
                boolean hasDot = false;
                if (po2 < udLength-1 && udunits.charAt(po2) == '.' && String2.isDigit(udunits.charAt(po2+1))) {
                    hasDot = true;
                    po2 += 2;
                    while (po2 < udLength && String2.isDigit(udunits.charAt(po2))) 
                        po2++;
                }

                //exponent?     e-  or e{digit}
                boolean hasE = false;
                if (po2 < udLength-1 && Character.toLowerCase(udunits.charAt(po2)) == 'e' &&
                    (udunits.charAt(po2+1) == '-' || String2.isDigit(udunits.charAt(po2+1)))) {
                    hasE = true;
                    po2 += 2;
                    while (po2 < udLength && String2.isDigit(udunits.charAt(po2))) 
                        po2++;
                }
                String num = udunits.substring(po, po2);
                po = po2;

                //convert floating point to rational number
                if (hasDot || hasE) {
                    int rational[] = String2.toRational(String2.parseDouble(num));
                    if (rational[1] == Integer.MAX_VALUE)
                        ucum.append(num); //ignore the trouble !!! ???
                    else if (rational[1] == 0) //includes {0, 0}
                        ucum.append(rational[0]);
                    else ucum.append(rational[0] + ".10^" + rational[1]);

                    /*double d = Math2.niceDouble(String2.parseDouble(num), 13);
                    if (Double.isNaN(d)) {
                        ucum.append(num);  //ignore the trouble !!! ???
                    } else if (d == 0) {
                        ucum.append("0");    //ignore the trouble !!! ???
                    } else if (d == Math2.roundToInt(d)) {
                        ucum.append(Math2.roundToInt(d));
                    } else {
                        int ie = Math2.intExponent(d);
                        String man = "" + Math2.mantissa(d);  //e.g. -1.76948
                        if (reallyReallyVerbose) String2.log("  man=" + man + " ie=" + ie);
                        if (ie == Integer.MAX_VALUE) {
                            ucum.append(num);  //infinity    //ignore the trouble !!! ???
                        } else if (man.endsWith(".0")) {
                            ucum.append(man.substring(0, man.length() - 2) + ".10^" + ie);
                        } else {
                            int dotPo = man.indexOf('.');
                            if (dotPo < 0) 
                                ucum.append(man + ".10^" + ie);                            
                            else 
                                ucum.append(
                                    man.substring(0, dotPo) +
                                    man.substring(dotPo + 1, man.length()) + 
                                    ".10^" + (ie - (man.length() - dotPo - 1)));                            
                        }
                    }*/
                } else {
                    //just copy num
                    ucum.append(num);
                }                

                continue;
            }

            //space or . or · (183) (multiplication)
            if (ch == ' ' || ch == '.' || ch == 183) {
                char lastUcum = ucum.length() == 0? '\u0000' : ucum.charAt(ucum.length() - 1);
                if (lastUcum == '/' || lastUcum == '.') {} //if last token was / or .,  do nothing
                else ucum.append('.');
                po++;
                continue;
            }

            // *  (multiplication * or exponent **)
            if (ch == '*') {
                po++;
                if (po < udLength && udunits.charAt(po) == '*') {
                    ucum.append('^');  // exponent: ** -> ^
                    po++;
                } else {
                    char lastUcum = ucum.length() == 0? '\u0000' : ucum.charAt(ucum.length() - 1);
                    if (lastUcum == '/' || lastUcum == '.') {} //if last token was / or .,  do nothing
                    else ucum.append('.');
                }
                continue;
            }

            // /
            if (ch == '/') {
                po++;
                char lastUcum = ucum.length() == 0? '\u0000' : ucum.charAt(ucum.length() - 1);
                if (lastUcum == '/') 
                    ucum.setCharAt(ucum.length() - 1, '.'); //  2 '/' cancel out
                else if (lastUcum == '.') 
                    ucum.setCharAt(ucum.length() - 1, '/'); //  '/' replaces '.'
                else ucum.append('/');

                continue;
            }

            // "
            if (ch == '\"') {
                po++;
                ucum.append("''");
                continue;
            }

            //otherwise, punctuation.   copy it
            ucum.append(ch);
            po++;
        }

        return ucum.toString();
    }

    private static boolean isUdunitsLetter(char ch) {
        return String2.isLetter(ch) || ch == 'µ' || ch == '°';
    }

    /**
     * This converts one udunits term (perhaps with metric prefix(es)) to the corresponding ucum string.
     * If udunits is just metric prefix(es), this returns the prefix acronym(s) with "{count}" as suffix
     *   (e.g., dkilo returns dk{count}).
     * If this can't completely convert udunits, it returns the original udunits
     *   (e.g., kiloBobs remains kiloBobs  (to avoid 'exact' becoming 'ect' ).
     */
    private static String oneUdunitsToUcum(String udunits) {


        //repeatedly pull off start of udunits and build ucum, till done
        String oldUdunits = udunits;
        StringBuilder ucum = new StringBuilder();        
        MAIN:
        while (true) {
            if (reallyReallyVerbose) 
                String2.log(ucum.length() == 0? "" : "  ucum=" + ucum + " udunits=" + udunits);

            //try to find udunits in hashMap
            String tUcum = udHashMap.get(udunits);
            if (tUcum != null) {
                //success! done!
                ucum.append(tUcum);
                return ucum.toString();
            }

            //try to separate out a metricName prefix (e.g., "kilo")
            for (int p = 0; p < nMetric; p++) {
                if (udunits.startsWith(metricName[p])) {
                    udunits = udunits.substring(metricName[p].length());
                    ucum.append(metricAcronym[p]);
                    if (udunits.length() == 0) {
                        ucum.append("{count}");
                        return ucum.toString();
                    }
                    continue MAIN;
                }
            }

            //try to separate out a metricAcronym prefix (e.g., "k")
            for (int p = 0; p < nMetric; p++) {
                if (udunits.startsWith(metricAcronym[p])) {
                    udunits = udunits.substring(metricAcronym[p].length());
                    ucum.append(metricAcronym[p]);
                    if (udunits.length() == 0) {
                        ucum.append("{count}");
                        return ucum.toString();
                    }
                    continue MAIN;
                }
            }

            //no change? failure; return original udunits
            //  because I don't want to change "exact" into "ect" 
            //  (i.e., seeing a metric prefix that isn't a metric prefix)
            if (reallyReallyVerbose) String2.log("EDUnits.oneUdunitsToUcum fail: ucum=" + 
                ucum + " udunits=" + udunits);
            //ucum.append(udunits);
            return oldUdunits; //ucum.toString();
        }
    }


    /**
     * This tests udunitsToUcum.
     * The most likely bugs are:
     * <li> Excess/incorrect substitutions, e.g., "avoirdupois_pounds" -> "[lb_av]" -> "[[lb_av]_av]".
     *   <br>This is the only thing that this method has pretty good tests for.
     * <li> Plural vs Singular conversions
     * <li> Typos 
     * <li> Misunderstanding (e.g., use of [H2O])
     * </ul>
     *
     * @throws RuntimeException if trouble
     */
    public static void testUdunitsToUcum() {

        String2.log("\n*** EDUnits.testUdunitsToUcum");
        //reallyReallyVerbose = true;

        //time point (try odd time units)
        testUdunitsToUcum("seconds since 1970-01-01T00:00:00Z", "s{since 1970-01-01T00:00:00Z}");
        testUdunitsToUcum("millisec since 1971-02-03T01", "ms{since 1971-02-03T01}");
        testUdunitsToUcum("sec since 1971-02-03T00:00:00-4:00", "s{since 1971-02-03T00:00:00-4:00}");
        testUdunitsToUcum("min since 1971-02-03", "min{since 1971-02-03}");
        testUdunitsToUcum("hr since 1971-02-03", "h{since 1971-02-03}");
        testUdunitsToUcum("days since 1971-02-03", "d{since 1971-02-03}");
        testUdunitsToUcum("months since 1971-02-03", "mo{since 1971-02-03}");  //mo_j?
        testUdunitsToUcum("yrs since 1971-02-03", "a{since 1971-02-03}"); //a_j?
        testUdunitsToUcum("zztops since 1971-02-03", "zztops.since.1971-02-03"); //not a point in time

        //main alphabetical section
        testUdunitsToUcum("abampere",          "(10.A)");
        testUdunitsToUcum("abfarad",           "GF");
        testUdunitsToUcum("abhenry",           "nH");
        testUdunitsToUcum("abmho",             "GS");
        testUdunitsToUcum("abohm",             "nO");
        testUdunitsToUcum("abvolt",            "(V/10^8)");
        testUdunitsToUcum("acre_feet",         "([acr_us].ft)");
        testUdunitsToUcum("acre_foot",         "([acr_us].ft)");
        testUdunitsToUcum("acre",              "[acr_us]");
        testUdunitsToUcum("ampere",            "A");
        testUdunitsToUcum("amp",               "A");
        testUdunitsToUcum("amu",               "u");
        testUdunitsToUcum("Å",                 "Ao");
        testUdunitsToUcum("Ångström",          "Ao");
        testUdunitsToUcum("angstrom",          "Ao");  
        testUdunitsToUcum("angular_degree",    "deg");  
        testUdunitsToUcum("angular_minute",    "'");  
        testUdunitsToUcum("angular_second",    "''");  //it doesn't look like "!
        testUdunitsToUcum("apostilb",          "(cd/[pi]/m2)");
        testUdunitsToUcum("apdram",            "[dr_ap]");
        testUdunitsToUcum("apothecary_dram",   "[dr_ap]");
        testUdunitsToUcum("apothecary_ounce",  "[oz_ap]");
        testUdunitsToUcum("apothecary_pound",  "[lb_ap]");
        testUdunitsToUcum("apounce",           "[oz_ap]");
        testUdunitsToUcum("appound",           "[lb_ap]");
        testUdunitsToUcum("arcdeg",            "deg");
        testUdunitsToUcum("arcminute",         "'");  
        testUdunitsToUcum("arcsecond",         "''");  //it doesn't look like "!
        testUdunitsToUcum("are",               "ar");  
        testUdunitsToUcum("arpentlin",         "(191835/1000.[ft_i])");   //101.835
        testUdunitsToUcum("assay_ton",         "(2916667/10^8.kg)");      //2.916667e-2
        testUdunitsToUcum("astronomical_unit", "AU");
        testUdunitsToUcum("at",                "att"); 
        testUdunitsToUcum("atmosphere",        "atm");
        testUdunitsToUcum("atomic_mass_unit",  "u");
        testUdunitsToUcum("atomicmassunit",    "u");        
        testUdunitsToUcum("avogadro_constant", "mol");  //???
        testUdunitsToUcum("avoirdupois_ounce", "[oz_av]");
        testUdunitsToUcum("avoirdupois_pound", "[lb_av]");

        testUdunitsToUcum("bag",               "(94.[lb_av])");
        testUdunitsToUcum("bakersdozen",       "13");
        testUdunitsToUcum("barie",             "(dN/m2)");
        testUdunitsToUcum("barleycorn",        "([in_i]/3)");
        testUdunitsToUcum("barn",              "b");
        testUdunitsToUcum("barrel",            "[bbl_us]");
        testUdunitsToUcum("bars",              "bar");
        testUdunitsToUcum("baud",              "Bd");
        testUdunitsToUcum("barye",             "(dN/m2)");
        testUdunitsToUcum("bbl",               "[bbl_us]");
        testUdunitsToUcum("becquerel",         "Bq");
        testUdunitsToUcum("bel",               "B");
        testUdunitsToUcum("bev",               "GeV");
        testUdunitsToUcum("big_point",         "([in_i]/72)");
        testUdunitsToUcum("biot",              "Bi");
        testUdunitsToUcum("bit",               "bit");
        testUdunitsToUcum("blondel",           "(cd/[pi]/m2)");
        testUdunitsToUcum("board_feet",        "[bf_i]");
        testUdunitsToUcum("board_foot",        "[bf_i]");
        testUdunitsToUcum("boiler_horsepower", "(98095/10.W)"); //9.80950e3
        testUdunitsToUcum("bps",               "(bit/s)");
        testUdunitsToUcum("Btu",               "[Btu_IT]");
        testUdunitsToUcum("bushel",            "[bu_us]");
        testUdunitsToUcum("bu",                "[bu_us]");
        testUdunitsToUcum("byte",              "By");

        testUdunitsToUcum("C",                 "C");   //In udunits and ucum, "C" means coulomb, not Celsius
        testUdunitsToUcum("c",                 "[c]"); //can't deal with "c" -> "[c]" (velocity of light) without parsing everything
        testUdunitsToUcum("C12_faraday",       "(9648531/100.C)"); //9.648531e4
        testUdunitsToUcum("cal",               "cal_IT");
        testUdunitsToUcum("calorie",           "cal_IT");
        testUdunitsToUcum("Canadian_liquid_gallon", "(454609/10^8.m3)"); //4.546090e-3
        testUdunitsToUcum("candela",           "cd");
        testUdunitsToUcum("candle",            "cd");
        testUdunitsToUcum("carat",             "[car_m]");
        testUdunitsToUcum("cc",                "cm3");
        testUdunitsToUcum("celsius",           "Cel");
        testUdunitsToUcum("Celsius",           "Cel");
        testUdunitsToUcum("chemical_faraday",  "(964957/10.C)"); //9.64957e4
        testUdunitsToUcum("circle",            "circ");
        testUdunitsToUcum("circular_mil",      "[cml_i]");
        testUdunitsToUcum("clo",               "(155/1000.K.m2/W)"); //1.55e-1
        testUdunitsToUcum("cmH2O",             "cm[H2O]");  
        testUdunitsToUcum("cmHg",              "cm[Hg]");  
        testUdunitsToUcum("common_year",       "(365.d)");
        testUdunitsToUcum("conventional_mercury", "[Hg]");  
        testUdunitsToUcum("conventional_water",   "[H2O]");  
        testUdunitsToUcum("coulomb",           "C");
        testUdunitsToUcum("count",             "{count}");    //no perfect UCUM conversion
        testUdunitsToUcum("cps",               "Hz");
        testUdunitsToUcum("cup",               "[cup_us]");
        testUdunitsToUcum("curie",             "Ci");

        testUdunitsToUcum("darcy",             "(9869233/10^19.m2)"); //9.869233e-13
        testUdunitsToUcum("day",               "d");

        testUdunitsToUcum("degree",            "deg");

        //there are no south options
        testUdunitsToUcum("degree_east",       "deg{east}");
        testUdunitsToUcum("degree_E",          "deg{east}");
        testUdunitsToUcum("degreeE",           "deg{east}");
        testUdunitsToUcum("degrees_east",      "deg{east}");
        testUdunitsToUcum("degrees_E",         "deg{east}");
        testUdunitsToUcum("degreesE",          "deg{east}");

        testUdunitsToUcum("degree_north",      "deg{north}");  
        testUdunitsToUcum("degree_N",          "deg{north}");  
        testUdunitsToUcum("degreeN",           "deg{north}");  
        testUdunitsToUcum("degrees_north",     "deg{north}");  
        testUdunitsToUcum("degrees_N",         "deg{north}");  
        testUdunitsToUcum("degreesN",          "deg{north}");  

        testUdunitsToUcum("degree_true",       "deg{true}");
        testUdunitsToUcum("degree_T",          "deg{true}");
        testUdunitsToUcum("degreeT",           "deg{true}");
        testUdunitsToUcum("degrees_true",      "deg{true}");
        testUdunitsToUcum("degrees_T",         "deg{true}");
        testUdunitsToUcum("degreesT",          "deg{true}");

        testUdunitsToUcum("degree_west",       "deg{west}");
        testUdunitsToUcum("degree_W",          "deg{west}");
        testUdunitsToUcum("degreeW",           "deg{west}");
        testUdunitsToUcum("degrees_west",      "deg{west}");
        testUdunitsToUcum("degrees_W",         "deg{west}");
        testUdunitsToUcum("degreesW",          "deg{west}");

        //most plurals are only in udunits-2
        testUdunitsToUcum("°C",                "Cel");  //udunits-2
        testUdunitsToUcum("degree_centigrade", "Cel");
        testUdunitsToUcum("degree_Celsius",    "Cel");  
        testUdunitsToUcum("degrees_Celsius",   "Cel");  
        testUdunitsToUcum("degC",              "Cel");
        testUdunitsToUcum("degreeC",           "Cel");
        testUdunitsToUcum("degreesC",          "Cel"); 
        testUdunitsToUcum("degree_C",          "Cel");
        testUdunitsToUcum("degrees_C",         "Cel"); 
        testUdunitsToUcum("degree_c",          "Cel");
        testUdunitsToUcum("degrees_c",         "Cel"); 
        testUdunitsToUcum("deg_C",             "Cel");
        testUdunitsToUcum("degs_C",            "Cel");
        testUdunitsToUcum("deg_c",             "Cel");
        testUdunitsToUcum("degs_c",            "Cel");

        //ucum seems to always treat degF as a unit of heat
        //  but C and K can be measures on a scale.
        //  F is a Farad.
        testUdunitsToUcum("°F",                "[degF]");  //udunits-2
        testUdunitsToUcum("degree_Fahrenheit", "[degF]");
        testUdunitsToUcum("degrees_Fahrenheit","[degF]"); //non-standard
        testUdunitsToUcum("degF",              "[degF]");  
        testUdunitsToUcum("degreeF",           "[degF]");
        testUdunitsToUcum("degreesF",          "[degF]"); //non-standard
        testUdunitsToUcum("degree_F",          "[degF]");
        testUdunitsToUcum("degrees_F",         "[degF]"); //non-standard
        testUdunitsToUcum("degree_f",          "[degF]"); 
        testUdunitsToUcum("degrees_f",         "[degF]"); //non-standard
        testUdunitsToUcum("deg_F",             "[degF]");
        testUdunitsToUcum("degs_F",            "[degF]");
        testUdunitsToUcum("deg_f",             "[degF]");
        testUdunitsToUcum("degs_f",            "[degF]");

        testUdunitsToUcum("°K",                "K");  //udunits-2
        testUdunitsToUcum("degree_Kelvin",     "K");
        testUdunitsToUcum("degrees_Kelvin",    "K"); //non-standard
        testUdunitsToUcum("degK",              "K");
        testUdunitsToUcum("degreeK",           "K");
        testUdunitsToUcum("degreesK",          "K"); //non-standard
        testUdunitsToUcum("degree_K",          "K");
        testUdunitsToUcum("degrees_K",         "K"); //non-standard
        testUdunitsToUcum("degree_k",          "K");
        testUdunitsToUcum("degrees_k",         "K"); //non-standard
        testUdunitsToUcum("deg_K",             "K");
        testUdunitsToUcum("degs_K",            "K");
        testUdunitsToUcum("deg_k",             "K");
        testUdunitsToUcum("degs_k",            "K");

        testUdunitsToUcum("°R",                "(5/9.K)");  //udunits-2
        testUdunitsToUcum("degree_Rankine",    "(5/9.K)");
        testUdunitsToUcum("degrees_Rankine",   "(5/9.K)"); //non-standard
        testUdunitsToUcum("degR",              "(5/9.K)");
        testUdunitsToUcum("degreeR",           "(5/9.K)");
        testUdunitsToUcum("degreesR",          "(5/9.K)"); //non-standard
        testUdunitsToUcum("degree_R",          "(5/9.K)");
        testUdunitsToUcum("degrees_R",         "(5/9.K)"); //non-standard
        testUdunitsToUcum("degree_r",          "(5/9.K)");
        testUdunitsToUcum("degrees_r",         "(5/9.K)"); //non-standard
        testUdunitsToUcum("deg_R",             "(5/9.K)");
        testUdunitsToUcum("degs_R",            "(5/9.K)");
        testUdunitsToUcum("deg_r",             "(5/9.K)");
        testUdunitsToUcum("degs_r",            "(5/9.K)");

        testUdunitsToUcum("denier",            "(1111111/10^13.kg/m)"); //1.111111e-7
        testUdunitsToUcum("diopter",           "[diop]");
        testUdunitsToUcum("dozen",             "12{count}");
        testUdunitsToUcum("dram",              "[fdr_us]");  //udunits dr is a volume, not a weight
        testUdunitsToUcum("dr",                "[fdr_uk]");  //in udunits, dr is a volume, not a weight
        testUdunitsToUcum("drop",              "[drp]");
        testUdunitsToUcum("dry_pint",          "[dpt_us]");
        testUdunitsToUcum("dry_quart",         "[dqt_us]");
        //see "dr" below
        testUdunitsToUcum("dynamic",           "[g]");
        testUdunitsToUcum("dyne",              "dyn");

        testUdunitsToUcum("EC_therm",          "(105506/10^13.J)"); //1.05506e8
        testUdunitsToUcum("electric_horsepower","(746.W)");  
        testUdunitsToUcum("electronvolt",      "eV");
        testUdunitsToUcum("eon",               "Ga");    //10^9.a
        testUdunitsToUcum("ergs",              "erg");

        testUdunitsToUcum("F",                 "[degF]");   //  In ucum, F means Farad.
        testUdunitsToUcum("Fahrenheit",        "[degF]");
        testUdunitsToUcum("fahrenheit",        "[degF]");
        testUdunitsToUcum("faraday",           "(9648531/100.C)");  //9.648531e4
        testUdunitsToUcum("farad",             "F");
        testUdunitsToUcum("fathom",            "[fth_us]");
        testUdunitsToUcum("feet",              "[ft_i]");
        testUdunitsToUcum("fermi",             "fm");
        testUdunitsToUcum("firkin",            "([bbl_us]/4)");
        testUdunitsToUcum("fldr",              "[dr_av]");
        testUdunitsToUcum("floz",              "[foz_us]");
        testUdunitsToUcum("fluid_ounce",       "[foz_us]");
        testUdunitsToUcum("fluid_dram",        "[fdr_us]");
        testUdunitsToUcum("footcandle",        "(1076391/10^7.lx)");  //0.1076391
        testUdunitsToUcum("footlambert",       "(3426259/10^6.cd/m2)");    //3.426259
        testUdunitsToUcum("foot_H2O",          "[ft_i'H2O]");
        testUdunitsToUcum("footH2O",           "[ft_i'H2O]");
        testUdunitsToUcum("foot",              "[ft_i]");

        testUdunitsToUcum("force_gram",        "gf");
        testUdunitsToUcum("force_ounce",       "([lbf_av]/16)");  //there is no [ozf_av]
        testUdunitsToUcum("force_pound",       "[lbf_av]");
        testUdunitsToUcum("force_kilogram",    "kgf");
        testUdunitsToUcum("force_ton",         "(2000.[lbf_av])"); //there is no ...
        testUdunitsToUcum("force",             "[g]");

        testUdunitsToUcum("fortnight",         "(14.d)");
        testUdunitsToUcum("free_fall",         "[g]");
        testUdunitsToUcum("ftH2O",             "[ft_i'H2O]");
        testUdunitsToUcum("ft",                "[ft_i]");  
        testUdunitsToUcum("furlong",           "[fur_us]");

        testUdunitsToUcum("gallon",            "[gal_us]");
        testUdunitsToUcum("gal",               "Gal");
        testUdunitsToUcum("gamma",             "nT");
        testUdunitsToUcum("gauss",             "G");
        testUdunitsToUcum("geopotential",      "[g]");
        testUdunitsToUcum("gilbert",           "Gb");
        testUdunitsToUcum("gill",              "[gil_us]");
        testUdunitsToUcum("gp",                "[g]");
        testUdunitsToUcum("gr",                "[gr]");
        testUdunitsToUcum("grade",             "gon");
        testUdunitsToUcum("grain",             "[gr]");
        testUdunitsToUcum("gram_force",        "gf");
        testUdunitsToUcum("gram",              "g");
        testUdunitsToUcum("gravity",           "[g]");
        testUdunitsToUcum("gray",              "Gy");
        testUdunitsToUcum("Gregorian_year",    "a_g");

        testUdunitsToUcum("h2o",               "[H2O]");  
        testUdunitsToUcum("H2O",               "[H2O]");  
        testUdunitsToUcum("hectare",           "Har");    //it's a hecto are
        testUdunitsToUcum("henry",             "H");
        testUdunitsToUcum("hertz",             "Hz");
        testUdunitsToUcum("hg",                "[Hg]");
        testUdunitsToUcum("Hg",                "[Hg]");  
        testUdunitsToUcum("horsepower",        "[HP]");
        testUdunitsToUcum("hour",              "h");
        testUdunitsToUcum("hr",                "h");
        testUdunitsToUcum("hundred",           "100");

        testUdunitsToUcum("inch_H2O_39F",      "[in_i'H2O]{39F}");  
        testUdunitsToUcum("inch_H2O_60F",      "[in_i'H2O]{60F}");  
        testUdunitsToUcum("inch_Hg_32F",       "[in_i'Hg]{32F}");   
        testUdunitsToUcum("inch_Hg_60F",       "[in_i'Hg]{60F}");   
        testUdunitsToUcum("inch_Hg",           "[in_i'Hg]");        
        testUdunitsToUcum("inches",            "[in_i]");
        testUdunitsToUcum("inch",              "[in_i]");

        testUdunitsToUcum("inhg",              "[in_i'Hg]");       
        testUdunitsToUcum("in",                "[in_i]"); 

        testUdunitsToUcum("international_feet",  "[ft_i]");
        testUdunitsToUcum("international_foot",  "[ft_i]");
        testUdunitsToUcum("international_inches","[in_i]");
        testUdunitsToUcum("international_inch",  "[in_i]");
        testUdunitsToUcum("international_knot",  "[kn_i]");
        testUdunitsToUcum("international_mile",  "[mi_i]");
        testUdunitsToUcum("international_yard",  "[yd_i]");
        
        testUdunitsToUcum("IT_Btu",            "[Btu_IT]"); 
        testUdunitsToUcum("IT_calorie",        "cal_IT");   

        testUdunitsToUcum("jiffies",           "cs");  
        testUdunitsToUcum("jiffy",             "cs");
        testUdunitsToUcum("joule",             "J");
        testUdunitsToUcum("Julian_year",       "a_j");

        testUdunitsToUcum("K",                 "K");
        testUdunitsToUcum("kat",               "kat");
        testUdunitsToUcum("katal",             "kat");
        testUdunitsToUcum("kayser",            "Ky");
        testUdunitsToUcum("kelvin",            "K");
        testUdunitsToUcum("Kelvin",            "K");
        testUdunitsToUcum("kip",               "(1000.[lbf_av])");
        testUdunitsToUcum("knot_international","[kn_i]");
        testUdunitsToUcum("knot",              "[kn_i]");
        testUdunitsToUcum("ksi",               "(1000.[lbf_av]/[in_i]2)");
        testUdunitsToUcum("kt",                "[kn_i]");

        testUdunitsToUcum("L",                 "L");  //seems like both allowed in ucum
        testUdunitsToUcum("l",                 "l");  //seems like both allowed in ucum
        testUdunitsToUcum("lambert",           "Lmb");
        testUdunitsToUcum("lbf",               "[lbf_av]");
        testUdunitsToUcum("lb",                "[lb_av]");
        testUdunitsToUcum("leap_year",         "(366.d)");
        testUdunitsToUcum("light_year",        "[ly]");
        testUdunitsToUcum("liquid_gallon",     "[gal_us]");  
        testUdunitsToUcum("liquid_pint",       "[pt_us]");
        testUdunitsToUcum("liquid_quart",      "[qt_us]");
        testUdunitsToUcum("liter",             "l");    //or use "L"?
        testUdunitsToUcum("litre",             "l");    //or use "L"?
        //testUdunitsToUcum("L",                 "l");  seems like: both allowed in ucum
        testUdunitsToUcum("long_hundredweight","[lcwt_av]");  
        testUdunitsToUcum("long_ton",          "t"); 
        testUdunitsToUcum("longton",           "t"); 
        testUdunitsToUcum("lumen",             "lm");
        testUdunitsToUcum("lunar_month",       "mo_s");
        testUdunitsToUcum("luxes",             "lx");
        testUdunitsToUcum("lux",               "lx");

        testUdunitsToUcum("m",                 "m");
        testUdunitsToUcum("maxwell",           "Mx");
        
        testUdunitsToUcum("mercury_0C",        "[Hg]");   
        testUdunitsToUcum("mercury_32F",       "[Hg]"); 
        testUdunitsToUcum("mercury_60F",       "[Hg]{60F}"); 
        testUdunitsToUcum("mercury",           "[Hg]");

        testUdunitsToUcum("meter",             "m");
        testUdunitsToUcum("metric_horsepower", "(735499/10^5.W)");  //7.35499,  
        testUdunitsToUcum("metric_ton",        "t");
        testUdunitsToUcum("metricton",         "t");
        testUdunitsToUcum("mhos",              "mho");  //=S
        testUdunitsToUcum("micron",            "um");
        testUdunitsToUcum("mile",              "[mi_i]");
        testUdunitsToUcum("millimeter_Hg",     "mm[Hg]");   
        testUdunitsToUcum("million",           "10^6");
        testUdunitsToUcum("mil",               "[mil_i]"); 
        testUdunitsToUcum("mins",              "min");
        testUdunitsToUcum("minute",            "min");
        testUdunitsToUcum("mi",                "[mi_i]"); //mile
        testUdunitsToUcum("mm_Hg",             "mm[Hg]");      
        testUdunitsToUcum("mm_hg",             "mm[Hg]");     
        testUdunitsToUcum("mm_Hg_0C",          "mm[Hg]{0C}");    
        testUdunitsToUcum("mmHg",              "mm[Hg]");     
        testUdunitsToUcum("mole",              "mol");
        testUdunitsToUcum("month",             "mo");

        testUdunitsToUcum("N",                 "N");
        testUdunitsToUcum("neper",             "Np");
        testUdunitsToUcum("nautical_mile",     "[nmi_i]");  
        testUdunitsToUcum("nmile",             "[nmi_i]");      
        testUdunitsToUcum("nmi",               "[nmi_i]");
        testUdunitsToUcum("newton",            "N");
        testUdunitsToUcum("nit",               "(cd/m2)");
        testUdunitsToUcum("NTU",               "{ntu}"); //???
        testUdunitsToUcum("nt",                "(cd/m2)");

        testUdunitsToUcum("O",                 "O");
        testUdunitsToUcum("oersted",           "Oe");
        testUdunitsToUcum("ohm",               "O");
        testUdunitsToUcum("osmole",            "osm"); 
        testUdunitsToUcum("ounce_force",       "([lbf_av]/16)");  //there is no [ozf_av]
        testUdunitsToUcum("ozf",               "([lbf_av]/16)");
        testUdunitsToUcum("oz",                "[foz_us]");

        testUdunitsToUcum("pair",              "2");
        testUdunitsToUcum("parsec",            "pc");
        testUdunitsToUcum("pascal",            "Pa");
        testUdunitsToUcum("peck",              "[pk_us]");
        testUdunitsToUcum("pennyweight",       "[pwt_tr]"); 
        testUdunitsToUcum("percent",           "%");
        testUdunitsToUcum("perches",           "[rd_us]");
        testUdunitsToUcum("perch",             "[rd_us]");
        testUdunitsToUcum("perm_0C",           "(S.572135.10^-16.kg/(Pa.s.m2))");
        testUdunitsToUcum("perm_23C",          "(S.574525.10^-16.kg/(Pa.s.m2))");

        testUdunitsToUcum("phot",              "ph");  
        testUdunitsToUcum("pH",                "[pH]"); 
        testUdunitsToUcum("physical_faraday",  "(965219/10.C)"); //9.65219e4
        testUdunitsToUcum("pica",              "[pca_pr]");
        testUdunitsToUcum("pint",              "[pt_us]");
        testUdunitsToUcum("pi",                "[pi]"); 
        testUdunitsToUcum("PI",                "[pi]");   
        testUdunitsToUcum("pk",                "[pk_us]");
        testUdunitsToUcum("poise",             "P");      
        testUdunitsToUcum("pole",              "[rd_us]");
        testUdunitsToUcum("pond",              "(980665/10^5.N)"); //9.806650e-3
        testUdunitsToUcum("poundal",           "(138255/10^6.N)"); //1.382550e-1
        testUdunitsToUcum("pound_force",       "[lbf_av]");
        testUdunitsToUcum("pound",             "[lb_av]");

        testUdunitsToUcum("ppth",              "[ppth]");
        testUdunitsToUcum("ppm",               "[ppm]");
        testUdunitsToUcum("ppb",               "[ppb]");
        testUdunitsToUcum("pptr",              "[pptr]");
        
        testUdunitsToUcum("printers_pica",     "[pca_pr]");  
        testUdunitsToUcum("printers_point",    "[pnt_pr]");
        testUdunitsToUcum("psi",               "[psi]");
        testUdunitsToUcum("PSU",               "{psu}"); //???
        testUdunitsToUcum("psu",               "{psu}"); //???
        testUdunitsToUcum("pt",                "[pt_us]");

        testUdunitsToUcum("quart",             "[qt_us]");

        testUdunitsToUcum("R",                 "R");
        testUdunitsToUcum("radian",            "rad");
        testUdunitsToUcum("rad",               "RAD");
        testUdunitsToUcum("Rankine",           "(5/9.K)");
        testUdunitsToUcum("Rankines",          "(5/9.K)");
        testUdunitsToUcum("rankine",           "(5/9.K)");
        testUdunitsToUcum("rankines",          "(5/9.K)");
        testUdunitsToUcum("rd",                "RAD");
        testUdunitsToUcum("refrigeration_ton", "(12000.[Btu_IT]/hr)");
        testUdunitsToUcum("register_ton",      "(2831685/10^6.m3)"); //2.831685
        testUdunitsToUcum("rem",               "REM");
        testUdunitsToUcum("rhe",               "(10.Pa-1.s-1)");
        testUdunitsToUcum("rod",               "[rd_us]");
        testUdunitsToUcum("roentgen",          "R");
        testUdunitsToUcum("rps",               "Hz");

        testUdunitsToUcum("S",                 "S");
        testUdunitsToUcum("score",             "20");
        testUdunitsToUcum("scruple",           "[sc_ap]");
        testUdunitsToUcum("second",            "s");
        testUdunitsToUcum("sec",               "s");
        testUdunitsToUcum("shaft_horsepower",  "[HP]");  
        testUdunitsToUcum("shake",             "(10^-8.s)");
        testUdunitsToUcum("short_hundredweight","[scwt_av]");  
        testUdunitsToUcum("short_ton",         "[ston_av]");
        testUdunitsToUcum("shortton",          "[ston_av]");

        testUdunitsToUcum("sidereal_day",      "(8616409/10^2.s)"); //8.616409e4
        testUdunitsToUcum("sidereal_hour",     "(359017/10^2.s)");   //3.590170e3
        testUdunitsToUcum("sidereal_minute",   "(5983617/10^5.s)");   //5.983617e1
        testUdunitsToUcum("sidereal_month",    "(27321661/10^6.d)");    //27.321661
        testUdunitsToUcum("sidereal_second",   "(9972696/10^7.s)");    //0.9972696
        testUdunitsToUcum("sidereal_year",     "(31558150.s)");   //3.155815e7
        
        testUdunitsToUcum("siemens",           "S"); //always has s at end
        testUdunitsToUcum("sievert",           "Sv");
        testUdunitsToUcum("slug",              "(145939/10^4.kg)"); //14.5939
        testUdunitsToUcum("sphere",            "(4.[pi].sr)");
        testUdunitsToUcum("standard_atmosphere","atm"); 
        testUdunitsToUcum("standard_free_fall","[g]");
        
        testUdunitsToUcum("statampere",        "(333564/10^15.A)");  //3.335640e-10
        testUdunitsToUcum("statcoulomb",       "(333564/10^15.C)");  //3.335640e-10
        testUdunitsToUcum("statfarad",         "(111265/10^17.F)");  //1.112650e-12
        testUdunitsToUcum("stathenry",         "(8987554.10^5.H)");   //8.987554e11
        testUdunitsToUcum("statmho",           "(111265/10^17.S)");  //1.112650e-12 
        testUdunitsToUcum("statohm",           "(8987554.10^5.O)");   //8.987554e11
        testUdunitsToUcum("statvolt",          "(2997925/10^4.V)");    //2.997925e2
        
        testUdunitsToUcum("steradian",         "sr");    
        testUdunitsToUcum("stere",             "st");
        testUdunitsToUcum("stilb",             "sb");
        testUdunitsToUcum("stokes",            "St");
        testUdunitsToUcum("stone",             "[stone_av]");  
        testUdunitsToUcum("svedberg",          "[S]");

        testUdunitsToUcum("T",                 "T");
        testUdunitsToUcum("t",                 "t");
        testUdunitsToUcum("tablespoon",        "[tbs_us]");
        testUdunitsToUcum("Tblsp",             "[tbs_us]");
        testUdunitsToUcum("tblsp",             "[tbs_us]");
        testUdunitsToUcum("Tbl",               "[tbs_us]");
        testUdunitsToUcum("Tbsp",              "[tbs_us]");
        testUdunitsToUcum("tbsp",              "[tbs_us]");
        testUdunitsToUcum("tbs",               "[tbs_us]");
        testUdunitsToUcum("teaspoon",          "[tsp_us]");
        testUdunitsToUcum("tsp",               "[tsp_us]");
        testUdunitsToUcum("technical_atmosphere","att"); 
        testUdunitsToUcum("ten",               "10");
        testUdunitsToUcum("tesla",             "T");
        testUdunitsToUcum("tex",               "(mg/m)");
        testUdunitsToUcum("thermochemical_calorie","cal_th"); 
        testUdunitsToUcum("therm",             "(105480400.J)");  
        testUdunitsToUcum("thm",               "(105480400.J)");  
        testUdunitsToUcum("thousand",          "1000");
        testUdunitsToUcum("ton_force",         "(2000.[lbf_av])"); //there is no ...
        testUdunitsToUcum("ton_of_refrigeration","(12000.[Btu_IT]/hr)");
        testUdunitsToUcum("ton_TNT",           "(4184.10^6.J)");  //4.184e9
        testUdunitsToUcum("tonne",             "t");  
        testUdunitsToUcum("ton",               "[ston_av]"); //U.S. short ton
        testUdunitsToUcum("torr",              "mm[Hg]"); 
        testUdunitsToUcum("tropical_month",    "(27321582/10^6.d)"); //27.321582
        testUdunitsToUcum("tropical_year",     "a_t");
        testUdunitsToUcum("troy_ounce",        "[oz_tr]");
        testUdunitsToUcum("troy_pound",        "[lb_tr]");
        testUdunitsToUcum("turn",              "circ");

        testUdunitsToUcum("u",                 "u");
        testUdunitsToUcum("ua",                "AU");
        testUdunitsToUcum("UK_fluid_ounce",    "[foz_br]");
        testUdunitsToUcum("UK_horsepower",     "(7457/10.W)");  //745.7
        testUdunitsToUcum("UK_liquid_cup",     "([pt_br]/2)");   
        testUdunitsToUcum("UK_liquid_gallon",  "[gal_br]");   
        testUdunitsToUcum("UK_liquid_gill",    "[gil_br]");   
        testUdunitsToUcum("UK_liquid_ounce",   "[foz_br]");
        testUdunitsToUcum("UK_liquid_pint",    "[pt_br]");    
        testUdunitsToUcum("UK_liquid_quart",   "[qt_br]");    
        
        testUdunitsToUcum("unified_atomic_mass_unit", "u");
        testUdunitsToUcum("unit_pole",         "(1256637/10^13.Wb)"); //1.256637e-7
        
        testUdunitsToUcum("US_dry_gallon",     "[gal_wi]");  
        testUdunitsToUcum("US_dry_pint",       "[dpt_us]");  
        testUdunitsToUcum("US_dry_quart",      "[dqt_us]");  
        testUdunitsToUcum("US_fluid_ounce",    "[foz_us]");
        testUdunitsToUcum("US_liquid_cup",     "[cup_us]");  
        testUdunitsToUcum("US_liquid_gallon",  "[gal_us]");  
        testUdunitsToUcum("US_liquid_gill",    "[gil_us]");  
        testUdunitsToUcum("US_liquid_ounce",   "[foz_us]");
        testUdunitsToUcum("US_liquid_pint",    "[pt_us]");   
        testUdunitsToUcum("US_liquid_quart",   "[qt_us]");   
        testUdunitsToUcum("US_statute_mile",   "[mi_us]");
        testUdunitsToUcum("US_survey_feet",    "[ft_us]");
        testUdunitsToUcum("US_survey_foot",    "[ft_us]");
        testUdunitsToUcum("US_survey_mile",    "[mi_us]");
        testUdunitsToUcum("US_survey_yard",    "[yd_us]"); 
        testUdunitsToUcum("US_therm",          "(105480400.J)");  //1.054804e8
        
        testUdunitsToUcum("V",                 "V");
        testUdunitsToUcum("volt",              "V");

        testUdunitsToUcum("W",                 "W");    
        testUdunitsToUcum("water_32F",         "[H2O]");    
        testUdunitsToUcum("water_39F",         "[H2O]{39F}"); 
        testUdunitsToUcum("water_4C",          "[H2O]{4Cel}"); 
        testUdunitsToUcum("water_60F",         "[H2O]{60F}"); 
        testUdunitsToUcum("water",             "[H2O]");

        testUdunitsToUcum("watt",              "W"); 
        testUdunitsToUcum("weber",             "Wb");
        testUdunitsToUcum("week",              "wk");
        testUdunitsToUcum("work_month",        "(2056.hr/12)");
        testUdunitsToUcum("work_year",         "(2056.hr)");

        testUdunitsToUcum("yard",              "[yd_i]");
        testUdunitsToUcum("yd",                "[yd_i]"); 
        testUdunitsToUcum("year",              "a");
        testUdunitsToUcum("yr",                "a");


        //metric prefixes
        testUdunitsToUcum("atto",              "a{count}");
        testUdunitsToUcum("centi",             "c{count}");
        testUdunitsToUcum("deci",              "d{count}");
        testUdunitsToUcum("deka",              "da{count}");
        testUdunitsToUcum("exa",               "E{count}");
        testUdunitsToUcum("femto",             "f{count}");
        testUdunitsToUcum("giga",              "G{count}");
        testUdunitsToUcum("hecto",             "h{count}");
        testUdunitsToUcum("kilo",              "k{count}");
        testUdunitsToUcum("mega",              "M{count}");
        testUdunitsToUcum("micro",             "u{count}");
        testUdunitsToUcum("µ",                 "u{count}");
        testUdunitsToUcum("milli",             "m{count}");
        testUdunitsToUcum("nano",              "n{count}");
        testUdunitsToUcum("peta",              "P{count}");
        testUdunitsToUcum("pico",              "p{count}");
        testUdunitsToUcum("tera",              "T{count}");
        testUdunitsToUcum("yocto",             "y{count}");
        testUdunitsToUcum("yotta",             "Y{count}");
        testUdunitsToUcum("zepto",             "z{count}");  
        testUdunitsToUcum("zetta",             "Z{count}");


        //multiple metric prefixes
        testUdunitsToUcum("decikilocentinpgrams", "dkcnpg");

        //per
        testUdunitsToUcum("kilograms / second2",     "kg/s2");
        testUdunitsToUcum("kilograms PER second**2", "kg/s^2");
        testUdunitsToUcum("kilograms per second^2",  "kg/s^2");

        //numbers
        testUdunitsToUcum("-12 kg",            "-12.kg");
        testUdunitsToUcum("-0.0e14 kg",        "0.kg");
        testUdunitsToUcum("-20.0e14 kg",       "-2.10^15.kg");
        testUdunitsToUcum("-1.2e1 kg",         "-12.kg");
        testUdunitsToUcum("-12340e-1 kg",      "-1234.kg");
        testUdunitsToUcum("-12340e2 kg",       "-1234000.kg");
        testUdunitsToUcum("-12.345e2 kg",      "-12345.10^-1.kg");
        testUdunitsToUcum("-12.345e400 kg",    "-12.345e400.kg");  //trouble is passed through

        //failures
        testUdunitsToUcum("dkilo",             "dk{count}");  //just metric prefixes
        testUdunitsToUcum("kiloBobs",          "kiloBobs");   //original returned

        //punctuation
        //PER should be handled as special case to avoid ./.
        testUdunitsToUcum("per",               "/");
        testUdunitsToUcum("PER",               "/");
        testUdunitsToUcum("**",                "^");  //exponent
        testUdunitsToUcum("*",                 ".");  //explicit multiplication
        testUdunitsToUcum("·",                 ".");  //explicit multiplication
        testUdunitsToUcum(" ",                 ".");  //implied multiplication
        testUdunitsToUcum("\"",                "''");
        testUdunitsToUcum("'",                 "'");

        testUdunitsToUcum("°",                 "deg");
        testUdunitsToUcum("°F",                "[degF]");
        testUdunitsToUcum("°R",                "(5/9.K)");
        testUdunitsToUcum("°C",                "Cel");
        testUdunitsToUcum("°K",                "K");
        testUdunitsToUcum("°north",            "°north"); //invalid

        reallyReallyVerbose = false;
    }

    private static void testUdunitsToUcum(String udunits, String ucum) {
        Test.ensureEqual(udunitsToUcum(udunits), ucum, "original=" + udunits);
    }


    /** 
     * This is like ucumToUdunits() but won't throw an exception. 
     * If there is trouble, it returns ucum.
     */
    public static String safeUcumToUdunits(String ucum) {
        try {
           return ucumToUdunits(ucum); 
        } catch (Throwable t) {
            String2.log(String2.ERROR + " while converting ucum=" + ucum + " to udunits:\n" +
                MustBe.throwableToString(t));
            return ucum;
        }
    }

    /** 
     * This converts UCUM to UDUnits.
     * <br>UDUnits: http://www.unidata.ucar.edu/software/udunits/udunits-1/etc/udunits.dat
     *              http://www.unidata.ucar.edu/software/udunits/udunits-2/udunits2.html
     * <br>UCUM: http://unitsofmeasure.org/ucum.html
     *
     * <p>UCUM tends to be short, canonical-only, and strict.
     *    Many UCUM units are the same in UDUnits.
     *
     * <p>UDUnits supports lots of aliases (short and long)
     *   and plurals (usually by adding 's'). 
     *   This tries to convert UCUM to a short, common UDUNIT units.
     *
     * <p>Problems:
     * <ul>
     * <li> UCUM has only "deg", no concept of degree_east|north|true|true.
     * </ul>
     * 
     * <p>Notes: 
     * <ul>
     * <li>This method is a strictly case sensitive.
     * <li>For "10 to the", UCUM allows 10* or 10^. This method uses 10^.
     * <li>{ntu} becomes NTU.
     * <li>{psu} becomes PSU.
     * </ul>
     *
     * return the UCUM converted to UDUNITS.
     *    null returns null. "" returns "".
     */
    public static String ucumToUdunits(String ucum) {
        if (ucum == null)
            return null;
        if (reallyReallyVerbose) 
            String2.log("  ucum=" + ucum);
        
        StringBuilder udunits = new StringBuilder();        
        int ucLength = ucum.length();
        if (ucLength == 0)
            return "";

        //is it a time point?  e.g., s{since 1970-01-01T00:00:00T}
        if (ucum.charAt(ucLength - 1) == '}' &&  //quick reject
            ucum.indexOf('}') == ucLength - 1) { //reasonably quick reject
            int sincePo = ucum.indexOf("{since ");
            if (sincePo > 0) {
                //is first part an atomic ucum unit?
                String tUdunits = ucHashMap.get(ucum.substring(0, sincePo));
                if (tUdunits != null) 
                    return tUdunits + " " + ucum.substring(sincePo + 1, ucLength - 1);
            }
        }

        
        //parse ucum and build udunits, till done        
        int po = 0;  //po is next position to be read
        while (po < ucLength) {
            if (reallyReallyVerbose && udunits.length() > 0) String2.log("  udunits=" + udunits);
            char ch = ucum.charAt(po);

            //letter  
            if (isUcumLetter(ch)) {     //includes [, ], {, }, 'µ' and "'"
                //find contiguous letters|_|digit (no '-') 
                int po2 = po + 1;
                while (po2 < ucLength && 
                    (isUcumLetter(ucum.charAt(po2)) || ucum.charAt(po2) == '_' ||
                     String2.isDigit(ucum.charAt(po2)))) 
                    po2++;
                String tUcum = ucum.substring(po, po2);
                po = po2;

                //some ucum have internal digits, but none end in digits 
                //if it ends in digits, treat as exponent
                //find contiguous digits at end
                int firstDigit = tUcum.length();
                while (firstDigit >= 1 && String2.isDigit(tUcum.charAt(firstDigit - 1)))
                    firstDigit--;
                String exponent = tUcum.substring(firstDigit);
                tUcum = tUcum.substring(0, firstDigit);
                String tUdunits = oneUcumToUdunits(tUcum);

                //deal with PER -> / 
                if (tUdunits.equals("/")) {   
                    char lastUdunits = udunits.length() == 0? '\u0000' : udunits.charAt(udunits.length() - 1);
                    if (lastUdunits == '/') 
                        udunits.setCharAt(udunits.length() - 1, '.'); //2 '/' cancel out
                    else if (lastUdunits == '.') 
                        udunits.setCharAt(udunits.length() - 1, '/'); //  '/' replaces '.'
                    else udunits.append('/');

                } else {
                    udunits.append(tUdunits);
                }                

                //add the exponent
                udunits.append(exponent);
                //catch -exponent as a number below

                continue;
            }

            //number
            if (ch == '-' || String2.isDigit(ch)) {
                //find contiguous digits
                int po2 = po + 1;
                while (po2 < ucLength && String2.isDigit(ucum.charAt(po2))) 
                    po2++;

                // ^-  or ^{digit}
                boolean hasE = false;
                if (po2 < ucLength-1 && Character.toLowerCase(ucum.charAt(po2)) == '^' &&
                    (ucum.charAt(po2+1) == '-' || String2.isDigit(ucum.charAt(po2+1)))) {
                    hasE = true;
                    po2 += 2;
                    while (po2 < ucLength && String2.isDigit(ucum.charAt(po2))) 
                        po2++;
                }
                String num = ucum.substring(po, po2);
                po = po2;
                udunits.append(num);
                continue;
            }

            // .
            if (ch == '.') {
                po++;
                udunits.append(' '); // ' ' is more common than '.' in udunits
                continue;
            }

            // *
            if (ch == '*') {
                po++;
                udunits.append('^');
                continue;
            }

            // '  ''
            if (ch == '\'') {
                po++;
                if (po < ucLength && ucum.charAt(po) == '\'') {
                    udunits.append("arc_second"); 
                    po++;
                } else {
                    udunits.append("arc_minute");
                }
                continue;
            }
            //otherwise, punctuation.   copy it
            //  / (division), " doesn't occur,
            udunits.append(ch);
            po++;
        }

        return udunits.toString();
    }

    private static boolean isUcumLetter(char ch) {
        return String2.isLetter(ch) || 
            ch == '[' || ch == ']' || 
            ch == '{' || ch == '}' ||
            ch == 'µ' || ch == '\'';
    }

    /**
     * This converts one ucum term (perhaps with metric prefix(es)) 
     (   to the corresponding udunits string.
     * If ucum is just metric prefix(es), this returns the metric prefix 
     *   acronym(s) with "{count}" as suffix (e.g., dkilo returns dk{count}).
     * If this can't completely convert ucum, it returns the original ucum
     *   (e.g., kiloBobs remains kiloBobs  (to avoid 'exact' becoming 'ect' ).
     */
    private static String oneUcumToUdunits(String ucum) {


        //repeatedly pull off start of ucum and build udunits, till done
        String oldUcum = ucum;
        StringBuilder udunits = new StringBuilder();        
        MAIN:
        while (true) {
            if (reallyReallyVerbose) 
                String2.log(udunits.length() == 0? "" : "  udunits=" + udunits + " ucum=" + ucum);

            //try to find ucum in hashMap
            String tUdunits = ucHashMap.get(ucum);
            if (tUdunits != null) {
                //success! done!
                udunits.append(tUdunits);
                return udunits.toString();
            }

            //try to separate out a metricAcronym prefix (e.g., "k")
            for (int p = 0; p < nMetric; p++) {
                if (ucum.startsWith(metricAcronym[p])) {
                    ucum = ucum.substring(metricAcronym[p].length());
                    udunits.append(metricAcronym[p]);
                    if (ucum.length() == 0) {
                        udunits.append("{count}");
                        return udunits.toString();
                    }
                    continue MAIN;
                }
            }

            //try to separate out a twoAcronym prefix (e.g., "Ki")
            for (int p = 0; p < nTwo; p++) {
                if (ucum.startsWith(twoAcronym[p])) {
                    ucum = ucum.substring(twoAcronym[p].length());
                    char udch = udunits.length() > 0? udunits.charAt(udunits.length() - 1) : '\u0000';
                    if (udch != '\u0000' && udch != '.' && udch != '/')
                        udunits.append('.');
                    if (ucum.length() == 0) {
                        udunits.append("{count}");
                        return udunits.toString();
                    }
                    udunits.append(twoValue[p] + ".");
                    continue MAIN;
                }
            }

            //ends in comment?  try to just convert the beginning
            int po1 = oldUcum.lastIndexOf('{');
            if (po1 > 0 && oldUcum.endsWith("}")) 
                return oneUcumToUdunits(oldUcum.substring(0, po1)) + oldUcum.substring(po1);

            //no change? failure; return original ucum
            //  because I don't want to change "exact" into "ect" 
            // (i.e., seeing a metric prefix that isn't a metric prefix)
            if (reallyReallyVerbose) String2.log("EDUnits.oneUcumToUdunits fail: udunits=" + 
                udunits + " ucum=" + ucum);
            //udunits.append(ucum);
            return oldUcum; 
        }
    }

    /**
     * This tests UcumToUdunits.
     * The most likely bugs are:
     * <li> Excess/incorrect substitutions, e.g., "avoirdupois_pounds" -> "[lb_av]" -> "[[lb_av]_av]".
     *   <br>This is the only thing that this method has pretty good tests for.
     * <li> Plural vs Singular conversions
     * <li> Typos 
     * <li> Misunderstanding (e.g., use of [H2O])
     * </ul>
     *
     * @throws RuntimeException if trouble
     */
    public static void testUcumToUdunits() {

        String2.log("\n*** EDUnits.testUcumToUdunits");
        reallyReallyVerbose = true;

        //main alphabetical section
        testUcumToUdunits("A",                 "A");
        testUcumToUdunits("[acr_us]",          "acre");


        testUcumToUdunits("deg",               "degree");
        testUcumToUdunits("deg{east}",         "degree_east");
        testUcumToUdunits("deg{north}",        "degree_north");
        testUcumToUdunits("deg{true}",         "degree_true");
        testUcumToUdunits("deg{west}",         "degree_west");

        //twoAcronym
        testUcumToUdunits("KiBd",              "1024.baud");

        //failures
        testUcumToUdunits("dk{count}",         "dkcount"); //not ideal
        testUcumToUdunits("kiloBobs",          "kiloBobs");   //original returned
        testUcumToUdunits("deg{bob}",          "degree{bob}");  //comment left intact

        //punctuation
        //PER should be handled as special case to avoid ./.
        testUcumToUdunits("^",                 "^");  //exponent
        testUcumToUdunits(".",                 " ");  //explicit multiplication
        testUcumToUdunits("''",                "arc_second");
        testUcumToUdunits("'",                 "arc_minute");

        testUcumToUdunits("deg",               "degree");
        testUcumToUdunits("[degF]",            "degree_F");
        testUcumToUdunits("5/9.K",             "5/9 degree_K");
        testUcumToUdunits("Cel",               "degree_C");
        testUcumToUdunits("K",                 "degree_K");

        //time point
        testUcumToUdunits("s{since 1970-01-01T00:00:00Z}", "s since 1970-01-01T00:00:00Z");
        testUcumToUdunits("Gb{since 1970-01-01T00}", "gilbert since 1970-01-01T00");  //absurd but okay
        testUcumToUdunits("s.m{ since 1970-01-01T00}", "s m{ since 1970-01-01T00}"); //fail, so comment falls through

        reallyReallyVerbose = false;

    }


    private static void testUcumToUdunits(String ucum, String udunits) {
        Test.ensureEqual(ucumToUdunits(ucum), udunits, "original=" + ucum);
    }

    /** 
     * This tests some of the methods in this class.
     * @throws Throwable if trouble.
     */
    public static void test() throws Throwable {
        testUdunitsToUcum();
        testUcumToUdunits();
    }

    /**
     * This makes a HashMap&lt;String, String&gt; from the ResourceBundle-like file's keys=values.
     * The key and value strings are trim()'d.
     *
     * @param fileName  the full file name of the key=value file.
     * @param charset e.g., ISO-8859-1; or "" or null for the default
     * @throws RuntimeException if trouble
     */
    public static HashMap<String, String> getHashMapStringString(String fileName, String charset) 
        throws RuntimeException {
        try {
            HashMap ht = new HashMap();
            StringArray sa = StringArray.fromFile(fileName, charset);
            int n = sa.size();
            int i = 0;
            while (i < n) {
                String s = sa.get(i++);
                if (s.startsWith("#"))
                    continue;
                while (i < n && s.endsWith("\\")) 
                    s = s.substring(0, s.length() - 1) + sa.get(i++);
                int po = s.indexOf('=');
                if (po < 0) 
                    continue;
                //new String: so not linked to big source file's text
                ht.put(
                    new String(s.substring(0, po).trim()), 
                    new String(s.substring(po + 1).trim())); 
            }
            return ht;
        } catch (Throwable t) {
            throw new RuntimeException(t); 
        }
    }



    /**
     * This is used by Bob a few times to find udunits2 units that aren't yet in UdunitsToUcum.properties.
     * The source files are c:/programs/udunits-2.1.9/lib/udunits2-XXX.xml
     * No one else will need to use this.
     */
    public static void checkUdunits2File(String fileName) throws Exception {
        String2.log("\n*** EDUnits.checkUdUnits2File " + fileName);
        StringArray sa = StringArray.fromFile(fileName);
        StringArray names = new StringArray();
        int n = sa.size();
        int nDef = 0, nPlural = 0, nSingular = 0, nSymbol = 0;
        for (int i = 0; i < n; i++) {
            String s = sa.get(i);
            int po1;

            po1 = s.indexOf("<def>");
            if (po1 >= 0) {
                po1 += 5;
                int po2 = s.indexOf("</def>", po1);
                if (po2 >= 0) {
                    String term = s.substring(po1, po2);
                    if (term.indexOf(' ') < 0 && term.indexOf('/') < 0 &&
                        term.indexOf('.') < 0 && term.indexOf('^') < 0 &&
                        udHashMap.get(term) == null) {
                        names.add(term);
                        nDef++;
                    }
                }
            }

            po1 = s.indexOf("<singular>");
            if (po1 >= 0) {
                po1 += 10;
                int po2 = s.indexOf("</singular>", po1);
                if (po2 >= 0) {
                    String term = s.substring(po1, po2);
                    if (term.indexOf(' ') < 0 && term.indexOf('/') < 0 &&
                        term.indexOf('.') < 0 && term.indexOf('^') < 0 &&
                        udHashMap.get(term) == null) {
                        names.add(term);
                        nSingular++;
                    }
                }
            }

            po1 = s.indexOf("<plural>");
            if (po1 >= 0) {
                po1 += 8;
                int po2 = s.indexOf("</plural>", po1);
                if (po2 >= 0) {
                    String term = s.substring(po1, po2);
                    if (term.indexOf(' ') < 0 && term.indexOf('/') < 0 &&
                        term.indexOf('.') < 0 && term.indexOf('^') < 0 &&
                        udHashMap.get(term) == null) {
                        names.add(term);
                        nPlural++;
                    }
                }
            }

            po1 = s.indexOf("<symbol>");
            if (po1 >= 0) {
                po1 += 8;
                int po2 = s.indexOf("</symbol>", po1);
                if (po2 >= 0) {
                    String term = s.substring(po1, po2);
                    if (term.indexOf(' ') < 0 && term.indexOf('/') < 0 &&
                        term.indexOf('.') < 0 && term.indexOf('^') < 0 &&
                        udHashMap.get(term) == null) {
                        names.add(term);
                        nSymbol++;
                    }
                }
            }
        }
        names.sortIgnoreCase();
        String2.log(names.toNewlineString());
        String2.log("nDef=" + nDef + " nPlural=" + nPlural +
            " nSingular=" + nSingular + " nSymbol=" + nSymbol);

    }

    /**
     * This was used by Bob once to generate a crude UcumToUdunits.properties file.
     * No one else will need to use this.
     */
    public static void makeCrudeUcumToUdunits() {
        String2.log("\n*** EDUnits.makeCrudeUcumToUdunits");
        StringArray sa = new StringArray();
        Object keys[] = udHashMap.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            String key = (String)keys[i];
            if (key.endsWith("s") && udHashMap.get(key.substring(0, key.length() - 1)) != null) {
                //skip this plural (a singular exists)
            } else {
                sa.add(String2.left(udHashMap.get(key) + " = ", 21) + key);
            }
        }
        sa.sortIgnoreCase();
        String2.log(sa.toNewlineString());
    }

    /**
     * This is used by Bob as a one time test.
     * No one else will need to use this.
     */
    public static void testRoundTripConversions() {
        //uc -> ud -> uc is more likely to work cleanly because it starts with acronym
        String2.log("\n*** EDUnits.testRoundTripConversions");
        Object ucKeys[] = ucHashMap.keySet().toArray();
        Arrays.sort(ucKeys);
        for (int i = 0; i < ucKeys.length; i++) {
            String uc1 = (String)ucKeys[i];
            String ud1 = ucumToUdunits(uc1);
            String uc2 = udunitsToUcum(ud1);
            if (!uc1.equals(uc2))
                String2.log(
                    "\nuc1=" + uc1 +
                    "\nud1=" + ud1 +
                    "\nuc2=" + uc2);
        }
    }

    /**
     * This checks if two udunits are equivalent (either both are null or "", 
     * or equal each other, or have same ucom units).
     * Sometimes different but equivalent (e.g., degrees_north and degree_north).
     */
    public static boolean udunitsAreEquivalent(String ud1, String ud2) {
        if (ud1 == null) ud1 = "";
        if (ud2 == null) ud2 = ""; 
        if (ud1.equals(ud2))
            return true;
        return udunitsToUcum(ud1).equals(udunitsToUcum(ud2));
    }

    public static void main(String[] args) throws Throwable {
        test();
    }
}
