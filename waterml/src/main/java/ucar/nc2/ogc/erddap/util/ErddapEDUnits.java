/* 
 * EDUnits Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package ucar.nc2.ogc.erddap.util;

import com.google.common.math.DoubleMath;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


/**
 * This class has static methods to convert units from one standard to another.
 */
public class ErddapEDUnits {

    /**
     * UDUNITS and UCUM support metric prefixes.
     */
    public static String metricName[] = {
            "yotta", "zetta", "exa", "peta", "tera",
            "giga", "mega", "kilo", "hecto", "deka",
            "deci", "centi", "milli", "micro", "nano",
            "pico", "femto", "atto", "zepto", "yocto",
            "µ",};
    public static String metricAcronym[] = {
            "Y", "Z", "E", "P", "T",
            "G", "M", "k", "h", "da",
            "d", "c", "m", "u", "n",
            "p", "f", "a", "z", "y",
            "u"};
    public static int nMetric = metricName.length;

    /**
     * UCUM supports power-of-two prefixes, but UDUNITS doesn't.
     */
    public static String twoAcronym[] = {
            "Ki", "Mi", "Gi", "Ti"};
    public static String twoValue[] = {
            "1024", "1048576", "1073741824", "1.099511627776e12"};
    public static int nTwo = twoAcronym.length;

    private static final HashMap<String, String> udHashMap;
    private static final HashMap<String, String> ucHashMap;

    static {
        try (   InputStream udunitsToUcumStream = ErddapEDUnits.class.getResourceAsStream("UdunitsToUcum.properties");
                InputStream ucumToUdunitsStream = ErddapEDUnits.class.getResourceAsStream("UcumToUdunits.properties")) {
            udHashMap = getHashMapStringString(udunitsToUcumStream, "ISO-8859-1");
            ucHashMap = getHashMapStringString(ucumToUdunitsStream, "ISO-8859-1");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    /**
     * This converts UDUnits to UCUM.
     * <br>UDUnits: http://www.unidata.ucar.edu/software/udunits/udunits-1/etc/udunits.dat
     * http://www.unidata.ucar.edu/software/udunits/udunits-2/udunits2.html
     * I worked with v 2.1.9
     * <br>UCUM: http://unitsofmeasure.org/ucum.html
     * I worked with Version: 1.8, $Revision: 28894 $
     * <p/>
     * <p>UDUnits supports lots of aliases (short and long)
     * and plurals (usually by adding 's').
     * These all get reduced to UCUM's short and canonical-only units.
     * <p/>
     * <p>Notes:
     * <ul>
     * <li>This method is a strictly case sensitive.
     * The only UDUnits that should be capitalized (other than acronyms) are
     * Btu, Gregorian..., Julian..., PI.
     * <br>The only UDUnits that may be capitalized are
     * Celsius, Fahrenheit, Kelvin, Rankine.
     * <li>For "10 to the", UCUM allows 10* or 10^. This method uses 10^.
     * <li>NTU becomes {ntu}.
     * <li>PSU or psu becomes {psu}.
     * </ul>
     * <p/>
     * return the UDUnits converted to UCUM.
     * null returns null. "" returns "".
     * throws Exception if trouble.
     */
    public static String udunitsToUcum(String udunits) {
        if (udunits == null) {
            return null;
        }

        //is it a point in time? e.g., seconds since 1970-01-01T00:00:00T
        int sincePo = udunits.indexOf(" since ");
        if (sincePo > 0) {
            try {
                //test if really appropriate
                double baf[] = ErddapCalendar2.getTimeBaseAndFactor(udunits); //throws exception if trouble

                //use 'factor', since it is more forgiving than udunitsToUcum converter
                String u;
                if (DoubleMath.fuzzyEquals(baf[1], 0.001, 1e-6)) {  // Can't simply do "baf[1] == 0.001".
                    u = "ms";
                } else if (baf[1] == 1) {
                    u = "s";
                } else if (baf[1] == ErddapCalendar2.SECONDS_PER_MINUTE) {
                    u = "min";
                } else if (baf[1] == ErddapCalendar2.SECONDS_PER_HOUR) {
                    u = "h";
                } else if (baf[1] == ErddapCalendar2.SECONDS_PER_DAY) {
                    u = "d";
                } else if (baf[1] == 30 * ErddapCalendar2.SECONDS_PER_DAY) {  // mo_j ?
                    u = "mo";
                } else if (baf[1] == 360 * ErddapCalendar2.SECONDS_PER_DAY) {  // a_j ?
                    u = "a";
                } else {
                    u = udunitsToUcum(udunits.substring(0, sincePo)); //shouldn't happen, but weeks? microsec?
                }

                //make "s{since 1970-01-01T00:00:00T}
                return u + "{" + udunits.substring(sincePo + 1) + "}";
            } catch (Exception e) {
            }
        }

        //parse udunits and build ucum, till done
        StringBuilder ucum = new StringBuilder();
        int udLength = udunits.length();
        int po = 0;  //po is next position to be read
        while (po < udLength) {
            char ch = udunits.charAt(po);

            //letter  
            if (isUdunitsLetter(ch)) {     //includes 'µ' and '°'
                //find contiguous letters|_|digit (no '-') 
                int po2 = po + 1;
                while (po2 < udLength &&
                        (isUdunitsLetter(udunits.charAt(po2)) || udunits.charAt(po2) == '_' ||
                                ErddapString2.isDigit(udunits.charAt(po2)))) {
                    po2++;
                }
                String tUdunits = udunits.substring(po, po2);
                po = po2;

                //some udunits have internal digits, but none end in digits 
                //if it ends in digits, treat as exponent
                //find contiguous digits at end
                int firstDigit = tUdunits.length();
                while (firstDigit >= 1 && ErddapString2.isDigit(tUdunits.charAt(firstDigit - 1))) {
                    firstDigit--;
                }
                String exponent = tUdunits.substring(firstDigit);
                tUdunits = tUdunits.substring(0, firstDigit);
                String tUcum = oneUdunitsToUcum(tUdunits);

                //deal with PER -> / 
                if (tUcum.equals("/")) {
                    char lastUcum = ucum.length() == 0 ? '\u0000' : ucum.charAt(ucum.length() - 1);
                    if (lastUcum == '/') {
                        ucum.setCharAt(ucum.length() - 1, '.'); //2 '/' cancel out
                    } else if (lastUcum == '.') {
                        ucum.setCharAt(ucum.length() - 1, '/'); //  '/' replaces '.'
                    } else {
                        ucum.append('/');
                    }

                } else {
                    ucum.append(tUcum);
                }

                //add the exponent
                ucum.append(exponent);
                //catch -exponent as a number below

                continue;
            }

            //number
            if (ch == '-' || ErddapString2.isDigit(ch)) {
                //find contiguous digits
                int po2 = po + 1;
                while (po2 < udLength && ErddapString2.isDigit(udunits.charAt(po2))) {
                    po2++;
                }

                //decimal place + digit (not just .=multiplication)
                boolean hasDot = false;
                if (po2 < udLength - 1 && udunits.charAt(po2) == '.' && ErddapString2.isDigit(udunits.charAt(po2 + 1))) {
                    hasDot = true;
                    po2 += 2;
                    while (po2 < udLength && ErddapString2.isDigit(udunits.charAt(po2))) {
                        po2++;
                    }
                }

                //exponent?     e-  or e{digit}
                boolean hasE = false;
                if (po2 < udLength - 1 && Character.toLowerCase(udunits.charAt(po2)) == 'e' &&
                        (udunits.charAt(po2 + 1) == '-' || ErddapString2.isDigit(udunits.charAt(po2 + 1)))) {
                    hasE = true;
                    po2 += 2;
                    while (po2 < udLength && ErddapString2.isDigit(udunits.charAt(po2))) {
                        po2++;
                    }
                }
                String num = udunits.substring(po, po2);
                po = po2;

                //convert floating point to rational number
                if (hasDot || hasE) {
                    int rational[] = ErddapString2.toRational(ErddapString2.parseDouble(num));
                    if (rational[1] == Integer.MAX_VALUE) {
                        ucum.append(num); //ignore the trouble !!! ???
                    } else if (rational[1] == 0) //includes {0, 0}
                    {
                        ucum.append(rational[0]);
                    } else {
                        ucum.append(rational[0]).append(".10^").append(rational[1]);
                    }
                } else {
                    //just copy num
                    ucum.append(num);
                }

                continue;
            }

            //space or . or · (183) (multiplication)
            if (ch == ' ' || ch == '.' || ch == 183) {
                char lastUcum = ucum.length() == 0 ? '\u0000' : ucum.charAt(ucum.length() - 1);
                if (lastUcum == '/' || lastUcum == '.') {
                    //if last token was / or .,  do nothing
                } else {
                    ucum.append('.');
                }
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
                    char lastUcum = ucum.length() == 0 ? '\u0000' : ucum.charAt(ucum.length() - 1);
                    if (lastUcum == '/' || lastUcum == '.') {
                        //if last token was / or .,  do nothing
                    } else {
                        ucum.append('.');
                    }
                }
                continue;
            }

            // /
            if (ch == '/') {
                po++;
                char lastUcum = ucum.length() == 0 ? '\u0000' : ucum.charAt(ucum.length() - 1);
                if (lastUcum == '/') {
                    ucum.setCharAt(ucum.length() - 1, '.'); //  2 '/' cancel out
                } else if (lastUcum == '.') {
                    ucum.setCharAt(ucum.length() - 1, '/'); //  '/' replaces '.'
                } else {
                    ucum.append('/');
                }

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
        return ErddapString2.isLetter(ch) || ch == 'µ' || ch == '°';
    }

    /**
     * This converts one udunits term (perhaps with metric prefix(es)) to the corresponding ucum string.
     * If udunits is just metric prefix(es), this returns the prefix acronym(s) with "{count}" as suffix
     * (e.g., dkilo returns dk{count}).
     * If this can't completely convert udunits, it returns the original udunits
     * (e.g., kiloBobs remains kiloBobs  (to avoid 'exact' becoming 'ect' ).
     */
    private static String oneUdunitsToUcum(String udunits) {
        //repeatedly pull off start of udunits and build ucum, till done
        String oldUdunits = udunits;
        StringBuilder ucum = new StringBuilder();
        MAIN:
        while (true) {
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

            return oldUdunits;
        }
    }

    /**
     * This converts UCUM to UDUnits.
     * <br>UDUnits: http://www.unidata.ucar.edu/software/udunits/udunits-1/etc/udunits.dat
     * http://www.unidata.ucar.edu/software/udunits/udunits-2/udunits2.html
     * <br>UCUM: http://unitsofmeasure.org/ucum.html
     * <p/>
     * <p>UCUM tends to be short, canonical-only, and strict.
     * Many UCUM units are the same in UDUnits.
     * <p/>
     * <p>UDUnits supports lots of aliases (short and long)
     * and plurals (usually by adding 's').
     * This tries to convert UCUM to a short, common UDUNIT units.
     * <p/>
     * <p>Problems:
     * <ul>
     * <li> UCUM has only "deg", no concept of degree_east|north|true|true.
     * </ul>
     * <p/>
     * <p>Notes:
     * <ul>
     * <li>This method is a strictly case sensitive.
     * <li>For "10 to the", UCUM allows 10* or 10^. This method uses 10^.
     * <li>{ntu} becomes NTU.
     * <li>{psu} becomes PSU.
     * </ul>
     * <p/>
     * return the UCUM converted to UDUNITS.
     * null returns null. "" returns "".
     */
    public static String ucumToUdunits(String ucum) {
        if (ucum == null) {
            return null;
        }

        StringBuilder udunits = new StringBuilder();
        int ucLength = ucum.length();
        if (ucLength == 0) {
            return "";
        }

        //is it a time point?  e.g., s{since 1970-01-01T00:00:00T}
        if (ucum.charAt(ucLength - 1) == '}' &&  //quick reject
                ucum.indexOf('}') == ucLength - 1) { //reasonably quick reject
            int sincePo = ucum.indexOf("{since ");
            if (sincePo > 0) {
                //is first part an atomic ucum unit?
                String tUdunits = ucHashMap.get(ucum.substring(0, sincePo));
                if (tUdunits != null) {
                    return tUdunits + " " + ucum.substring(sincePo + 1, ucLength - 1);
                }
            }
        }

        //parse ucum and build udunits, till done        
        int po = 0;  //po is next position to be read
        while (po < ucLength) {
            char ch = ucum.charAt(po);

            //letter  
            if (isUcumLetter(ch)) {     //includes [, ], {, }, 'µ' and "'"
                //find contiguous letters|_|digit (no '-') 
                int po2 = po + 1;
                while (po2 < ucLength &&
                        (isUcumLetter(ucum.charAt(po2)) || ucum.charAt(po2) == '_' ||
                                ErddapString2.isDigit(ucum.charAt(po2)))) {
                    po2++;
                }
                String tUcum = ucum.substring(po, po2);
                po = po2;

                //some ucum have internal digits, but none end in digits 
                //if it ends in digits, treat as exponent
                //find contiguous digits at end
                int firstDigit = tUcum.length();
                while (firstDigit >= 1 && ErddapString2.isDigit(tUcum.charAt(firstDigit - 1))) {
                    firstDigit--;
                }
                String exponent = tUcum.substring(firstDigit);
                tUcum = tUcum.substring(0, firstDigit);
                String tUdunits = oneUcumToUdunits(tUcum);

                //deal with PER -> / 
                if (tUdunits.equals("/")) {
                    char lastUdunits = udunits.length() == 0 ? '\u0000' : udunits.charAt(udunits.length() - 1);
                    if (lastUdunits == '/') {
                        udunits.setCharAt(udunits.length() - 1, '.'); //2 '/' cancel out
                    } else if (lastUdunits == '.') {
                        udunits.setCharAt(udunits.length() - 1, '/'); //  '/' replaces '.'
                    } else {
                        udunits.append('/');
                    }

                } else {
                    udunits.append(tUdunits);
                }

                //add the exponent
                udunits.append(exponent);
                //catch -exponent as a number below

                continue;
            }

            //number
            if (ch == '-' || ErddapString2.isDigit(ch)) {
                //find contiguous digits
                int po2 = po + 1;
                while (po2 < ucLength && ErddapString2.isDigit(ucum.charAt(po2))) {
                    po2++;
                }

                // ^-  or ^{digit}
                if (po2 < ucLength - 1 && Character.toLowerCase(ucum.charAt(po2)) == '^' &&
                        (ucum.charAt(po2 + 1) == '-' || ErddapString2.isDigit(ucum.charAt(po2 + 1)))) {
                    po2 += 2;
                    while (po2 < ucLength && ErddapString2.isDigit(ucum.charAt(po2))) {
                        po2++;
                    }
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
        return ErddapString2.isLetter(ch) ||
                ch == '[' || ch == ']' ||
                ch == '{' || ch == '}' ||
                ch == 'µ' || ch == '\'';
    }

    /**
     * This converts one ucum term (perhaps with metric prefix(es))
     * (   to the corresponding udunits string.
     * If ucum is just metric prefix(es), this returns the metric prefix
     * acronym(s) with "{count}" as suffix (e.g., dkilo returns dk{count}).
     * If this can't completely convert ucum, it returns the original ucum
     * (e.g., kiloBobs remains kiloBobs  (to avoid 'exact' becoming 'ect' ).
     */
    private static String oneUcumToUdunits(String ucum) {
        //repeatedly pull off start of ucum and build udunits, till done
        String oldUcum = ucum;
        StringBuilder udunits = new StringBuilder();
        MAIN:
        while (true) {
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
                    char udch = udunits.length() > 0 ? udunits.charAt(udunits.length() - 1) : '\u0000';
                    if (udch != '\u0000' && udch != '.' && udch != '/') {
                        udunits.append('.');
                    }
                    if (ucum.length() == 0) {
                        udunits.append("{count}");
                        return udunits.toString();
                    }
                    udunits.append(twoValue[p]).append(".");
                    continue MAIN;
                }
            }

            //ends in comment?  try to just convert the beginning
            int po1 = oldUcum.lastIndexOf('{');
            if (po1 > 0 && oldUcum.endsWith("}")) {
                return oneUcumToUdunits(oldUcum.substring(0, po1)) + oldUcum.substring(po1);
            }

            return oldUcum;
        }
    }

    /**
     * Reads the contents of {@code inputStream} into a HashMap. Each key-value pair in the input will result in an
     * entry in the map.
     * <p/>
     * The specified stream remains open after this method returns.
     *
     * @param inputStream a stream with line-based, key-value pairs.
     * @param charset     the name of a supported {@link java.nio.charset.Charset charset}.
     * @return a HashMap initialized from the stream.
     * @throws java.io.IOException if an I/O error occurs
     */
    public static HashMap<String, String> getHashMapStringString(InputStream inputStream, String charset)
            throws IOException {
        HashMap<String, String> ht = new HashMap<>();
        ErddapStringArray sa = ErddapStringArray.fromInputStream(inputStream, charset);
        int n = sa.size();
        int i = 0;
        while (i < n) {
            String s = sa.get(i++);
            if (s.startsWith("#")) {
                continue;
            }
            while (i < n && s.endsWith("\\")) {
                s = s.substring(0, s.length() - 1) + sa.get(i++);
            }
            int po = s.indexOf('=');
            if (po < 0) {
                continue;
            }
            //new String: so not linked to big source file's text
            ht.put(s.substring(0, po).trim(), s.substring(po + 1).trim());
        }
        return ht;
    }
}
