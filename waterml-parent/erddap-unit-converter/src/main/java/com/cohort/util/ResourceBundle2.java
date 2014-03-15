/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.util;

import com.cohort.array.StringComparatorIgnoreCase;

import java.io.StringReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.xpath.XPath;   //requires java 1.5
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class expands the capabilities of a ResourceBundle.
 */
public class ResourceBundle2 {
 
    /**
     * Set this to true (by calling verbose=true in your program, 
     * not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    /**
     * Set this to true (by calling reallyVerbose=true in your program, 
     * not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false; 

    protected ResourceBundle primaryRB, secondaryRB; //both or just secondary may be null
    protected ConcurrentHashMap concurrentHashMap; //it is thread-safe (and needs to be)

    /**
     * A constructor that uses the one specified primary ResourceBundle.
     * 
     * @param primaryBaseName e.g., "com.cohort.util.TestResourceBundle2"
     *   for a file in the class path named 
     *   "com/cohort/util/TestResourceBundle2.properties".
     * @param locale
     * @throws Exception if properties file not found
     */
    public ResourceBundle2(String primaryBaseName, Locale locale) {
        primaryRB = ResourceBundle.getBundle(primaryBaseName, locale);
    }

    /**
     * A constructor that uses the two specified ResourceBundles.
     * For the getXxx methods, if no value is found in primary, secondary is searched.
     * 
     * @param primaryBaseName e.g., "com.cohort.util.TestResourceBundle2"
     *   for a file in the class path named 
     *   "com/cohort/util/TestResourceBundle2.properties".
     * @param secondaryBaseName e.g., "com.cohort.util.DefaultResourceBundle2"
     *   for a file in the class path named 
     *   "com/cohort/util/DefaultResourceBundle2.properties".
     * @param locale
     * @throws Exception if properties files not found
     */
    public ResourceBundle2(String primaryBaseName, String secondaryBaseName, Locale locale) {
        primaryRB = ResourceBundle.getBundle(primaryBaseName, locale);
        secondaryRB = ResourceBundle.getBundle(secondaryBaseName, locale);
    }

    /**
     * A constructor that uses the one specified primary ResourceBundle.
     * 
     * @param primaryBaseName
     * @throws Exception if properties file not found
     */
    public ResourceBundle2(String primaryBaseName) {
        primaryRB = ResourceBundle.getBundle(primaryBaseName);
    }

    /**
     * A constructor that uses the one specified primary ResourceBundle.
     * For the getXxx methods, if no value is found in primary, secondary is searched.
     * 
     * @param primaryBaseName
     * @param secondaryBaseName
     * @throws Exception if properties file not found
     */
    public ResourceBundle2(String primaryBaseName, String secondaryBaseName) {
        primaryRB = ResourceBundle.getBundle(primaryBaseName);
        secondaryRB = ResourceBundle.getBundle(secondaryBaseName);
    }

    /**
     * A constructor that uses the specified ConcurrentHashMap instead of a ResourceBundle.
     * 
     * @param concurrentHashMap
     */
    public ResourceBundle2(ConcurrentHashMap concurrentHashMap) {
        this.concurrentHashMap = concurrentHashMap;
    }

    /** 
     * A constructor based on an xml file with a very simple structure:
     * 1 outer tag + many 2nd level tags. This make a concurrentHashMap
     * with the 2nd level tag names and their content.
     *
     * @param doc e.g., from XML.parseXml
     * @return the resourceBundle2 made from it
     * @throws Exception if trouble
     */
    public static ResourceBundle2 fromXml(Document doc) throws Exception {
        XPath xPath = XML.getXPath();
        NodeList nodeList = XML.getNodeList(doc, xPath, "/*/*"); //all elements directly under root element
        int n = nodeList.getLength();
        ConcurrentHashMap tHash = new ConcurrentHashMap(Math2.roundToInt(1.4 * 16), 0.75f, 4);
        //String2.log("ResourceBundle2.fromXml  nNodes=" + n);
        for (int i = 0; i < n; i++) {
            Element element = (Element)nodeList.item(i);
            String key = element.getNodeName();
            String value = XML.getTextContent(element);
            //String2.log("  key=" + key + " value=" + value);
            tHash.put(key, value);
        }
        return new ResourceBundle2(tHash);
    }
   

    /**
     * This gets a boolean from the resourceBundle(s).
     * This returns true if value.trim().toLowerCase().equals("true").
     *
     * @param key the identifier for the desired value
     * @param theDefault the default value
     * @return the value associated with the key in the resourceBundle
     *     (or theDefault, if the key isn't found or can't be converted
     *     to a boolean)
     */
    public boolean getBoolean(String key, boolean theDefault) {
        try {
            String s = getString(key, null);

            //what does Boolean.getBoolean want? just forget it and do my own
            return s.toLowerCase().equals("true");
        } catch (Exception e) {
            return theDefault;
        }
    }

    /**
     * This gets an int from the resourceBundle(s).
     *
     * @param key the identifier for the desired value
     * @param theDefault the default value
     * @return the value associated with the key in the resourceBundle
     *     (or theDefault, if the key isn't found or can't be converted
     *     to an int (it is strict!))
     */
    public int getInt(String key, int theDefault) {
        return String2.parseInt(getString(key, null), theDefault);
    }

    /**
     * This gets a long from the resourceBundle(s).
     *
     * @param key the identifier for the desired value
     * @param theDefault the default value
     * @return the value associated with the key in the resourceBundle
     *     (or theDefault, if the key isn't found or can't be converted
     *     to a long (it is strict!))
     */
    public long getLong(String key, long theDefault) {
        long tl = String2.parseLong(getString(key, null));
        return tl == Long.MAX_VALUE? theDefault : tl;
    }

    /**
     * This gets a double from the resourceBundle(s).
     *
     * @param key the identifier for the desired value
     * @param theDefault the default value
     * @return the value associated with the key in the resourceBundle
     *     (or theDefault, if the key isn't found or can't be converted
     *     to a double (it is strict!))
     */
    public double getDouble(String key, double theDefault) {
        double d = String2.parseDouble(getString(key, null));
        return Double.isNaN(d)? theDefault : d;
    }

    /**
     * This gets a StringArray from the resourceBundle(s).
     * StringArrays are encoded in the properties file as a string,
     * with each of the separate strings separated by "\f".
     * There should be no "\f" after the last string.
     * [Where is the documentation for the Sun-standard way to store
     * StringArrays in a properties file?]
     *
     * @param key the identifier for the desired value
     * @param theDefault the default value
     * @return the value associated with the key in the resourceBundle
     *     (or theDefault, if the key isn't found or can't be converted
     *     to a String array)
     */
    public String[] getStringArray(String key, String[] theDefault) {
        try {
            return getString(key, null).split("\\f"); //a regex for a formfeed
        } catch (Exception e) {
            return theDefault;
        }
    }

    /**
     * This gets a String from the resourceBundle(s) and trim's it.
     *
     * @param key the identifier for the desired value
     * @param theDefault the default value
     * @return the value associated with the key in the resourceBundle
     *     (or theDefault, if the key isn't found)
     */
    public String getString(String key, String theDefault) {
        try {
            String s;
            if (concurrentHashMap == null) {
                try {
                    s = primaryRB.getString(key);
                } catch (Exception e) {
                    if (secondaryRB == null)
                        return theDefault;
                    else s = secondaryRB.getString(key);
                }
            } else {
                s = (String)concurrentHashMap.get(key);
            }
            s = s.trim();
            if (reallyVerbose)
                String2.log("  ResourceBundle2.getString(" + key + ") found \"" + s + "\".");
            return s;
        } catch (Exception e) {
            if (reallyVerbose)
                String2.log("  ResourceBundle2.getString(" + key + ") exception: \"" + 
                    e.toString() + "\"; so using default=\"" + theDefault + "\".");
            return theDefault;
        }
    }

    /**
     * This gets a String from the resourceBundle(s) and ensures isn't null.
     *
     * @param key
     * @param errorInMethod the start of an Error message
     * @throws Exception if there is no value for key
     */
    public String getNotNullString(String key, String errorInMethod) {
        String value = getString(key, null);
        Test.ensureNotNull(value, errorInMethod + key + " is null!"); 
        return value;
    }

    /**
     * This gets a String from the resourceBundle(s) and ensures isn't null or ''.
     *
     * @param key
     * @param errorInMethod the start of an Error message
     * @throws Exception if there is no value for key
     */
    public String getNotNothingString(String key, String errorInMethod) {
        String value = getString(key, null);
        Test.ensureNotNothing(value, errorInMethod + key + " is null or ''!"); 
        return value;
    }

    /**
     * This gets all the keys (sorted, ignoreCase) from concurrentHashMap, or primaryRB and secondaryRB.
     *
     * @return all the keys (sorted, ignoreCase) from concurrentHashMap, or primaryRB and secondaryRB.
     */
    public String[] getKeys() {
        ArrayList cumulative = new ArrayList();
        if (concurrentHashMap != null) 
            cumulative.addAll(String2.toArrayList(concurrentHashMap.keys()));
        if (primaryRB != null) 
            cumulative.addAll(String2.toArrayList(primaryRB.getKeys()));
        if (secondaryRB != null) 
            cumulative.addAll(String2.toArrayList(secondaryRB.getKeys()));
        String sar[] = String2.toStringArray(cumulative.toArray());
        Arrays.sort(sar, new StringComparatorIgnoreCase());
        return sar;
    }

    /**
     * Test the methods in this class.
     *
     * @throws Exception if trouble
     */
    public static void test() throws Exception {
        String2.log("\n*************************************************ResourceBundle2.test");

        ResourceBundle2 rb2 = fromXml(XML.parseXml(new StringReader(
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
            "<testr>\n" +
            "  <level1 att1=\"value1\" att2=\"value 2\" > level 1 &amp; <!-- comment < > -->text  \n" +
            "  </level1>\n" +
            "  <levela />\n" +   //"empty tag" appears as two tags, begin and end
            "  <levelb> 16</levelb>\n" +
            "  <bool> true</bool>\n" +
            "  <dbl> 17.1</dbl>\n" +
            "\n" +
            "\n" +
            "\n" +
            "</testr>"), 
            false));
        Test.ensureEqual(rb2.getString("level1", ""), "level 1 & text", "");
        Test.ensureEqual(rb2.getString("levela", ""), "", "");
        Test.ensureEqual(rb2.getBoolean("bool", false), true, "");
        Test.ensureEqual(rb2.getInt(   "levelb", 5), 16, "");
        Test.ensureEqual(rb2.getLong(  "levelb", 5), 16, "");
        Test.ensureEqual(rb2.getDouble("dbl", 5.5), 17.1, "");
        Test.ensureEqual(rb2.getString("testr", ""),  "", "");

        Test.ensureEqual(rb2.getBoolean("Z", true), true, "");
        Test.ensureEqual(rb2.getInt(    "Z", 5), 5, "");
        Test.ensureEqual(rb2.getDouble( "Z", 5.5), 5.5, "");
        Test.ensureEqual(rb2.getString( "Z", "word"), "word", "");
    }

}
