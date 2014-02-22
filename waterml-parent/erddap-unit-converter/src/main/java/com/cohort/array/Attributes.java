/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.array;

import com.cohort.util.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * This class holds a list of attributes (name=value, where name is a String
 * and value is a PrimitiveArray). 
 * The backing datastructure (ConcurrentHashMap) is thread-safe.
 *
 * @author Bob Simons (info@cohort.com)
 */
public class Attributes {
    //FUTURE: implement as ArrayString for names and ArrayList for values?

    /**
     * Set this to true (by calling verbose=true in your program, not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /** The backing data structure.  It is thread-safe. */
    private ConcurrentHashMap hashmap = new ConcurrentHashMap(16, 0.75f, 4);

    /**
     * This constructs a new, empty Attributes object.
     */
    public Attributes() {
    }
    
    /**
     * This constructs a new Attributes object which is a clone of 
     *   'attributes'.
     *
     * @param attributes 
     */
    public Attributes(Attributes attributes) {
        attributes.copyTo(this);

    }
    
   
    /**
     * This constructs a new Attributes object which has the
     * contents of moreImportant and lessImportant.
     *
     * @param moreImportant
     * @param lessImportant
     */
    public Attributes(Attributes moreImportant, Attributes lessImportant) {
        lessImportant.copyTo(this);
        set(moreImportant); //duplicate keys in 'more' will overwrite keys from 'less'
    }
    


    /**
     * This clears all the attributes being held.
     * The result isValid().
     */
    public void clear() {
        hashmap.clear();  
    }

    /**
     * This returns the number of nameValue pairs in the data structure.
     *
     * @return the number of nameValue pairs in the data structure.
     */
    public int size() {
        return hashmap.size();
    }

    /**
     * This makes a deep clone of the current table (data and attributes).
     *
     * @return a new Table.
     */
    public Object clone() {
        Attributes attributes2 = new Attributes();
        this.copyTo(attributes2);
        return attributes2;
    }

    /**
     * This returns the value of a specific attribute (or null
     * if the name isn't defined).
     * 
     * @param name
     * @return the attribute's value (a PrimitiveArray).
     */
    public PrimitiveArray get(String name) {
        return (PrimitiveArray)hashmap.get(name);
    }

    /**
     * This returns an array with the names of all of the attributes.
     * 
     * @return an array with the names of all of the attributes, sorted in 
     *     a case-insensitive way.
     */
    public String[] getNames() {
        StringArray names = new StringArray(hashmap.keys());
        names.sortIgnoreCase();
        return names.toArray();
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a String, regardless of the type used to store it.
     *
     * @param name
     * @return the String attribute or null if trouble (e.g., not found)
     */
    public String getString(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return null;
            return pa.getString(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A convenience method which assumes the first element of the attribute's 
     * value PrimitiveArray is a CSV String and which splits the string into parts.
     *
     * @param name
     * @return a String[] or null if trouble (e.g., not found)
     */
    public String[] getStringsFromCSV(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return null;
            String csv = pa.getString(0);
            return StringArray.arrayFromCSV(csv);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a double, regardless of the type used to store it.
     *
     * @param name
     * @return the attribute as a double or Double.NaN if trouble (e.g., not found)
     */
    public double getDouble(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Double.NaN;
            return pa.getDouble(0);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a double, regardless of the type used to store it.
     * If the type was float, this returns Math2.floatToDouble(value).
     *
     * @param name
     * @return the attribute as a nice double or Double.NaN if trouble (e.g., not found)
     */
    public double getNiceDouble(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Double.NaN;
            return pa.getNiceDouble(0);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a float, regardless of the type used to store it.
     *
     * @param name
     * @return the attribute as a float or Float.NaN if trouble (e.g., not found)
     */
    public float getFloat(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Float.NaN;
            return pa.getFloat(0);
        } catch (Exception e) {
            return Float.NaN;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as a long, regardless of the type used to store it.
     *
     * @param name
     * @return the attribute as a long or Long.MAX_VALUE if trouble (e.g., not found)
     */
    public long getLong(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Long.MAX_VALUE;
            return pa.getLong(0);
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * A convenience method which returns the first element of the attribute's 
     * value PrimitiveArray as an int, regardless of the type used to store it.
     *
     * @param name
     * @return the attribute as an int or Integer.MAX_VALUE if trouble (e.g., not found)
     */
    public int getInt(String name) {
        try {
            PrimitiveArray pa = get(name);
            if (pa == null || pa.size() == 0)
                return Integer.MAX_VALUE;
            return pa.getInt(0);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * This removes a specific attribute.
     * 
     * @param name the name of the attribute
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray remove(String name) {
        return (PrimitiveArray)hashmap.remove(name);
    }

    /**
     * This is the main method to set the value of a specific attribute (adding it if it
     * doesn't exist, revising it if it does, or removing it if value is
     * (PrimitiveArray)null).
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'.
     *    If value is null or size==0 or it is one String="", name is removed from attributes.
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, PrimitiveArray value) {
        if (value == null || value.size() == 0 || 
            (value.size() == 1 && value instanceof StringArray && value.getString(0).trim().length() == 0)) 
            return (PrimitiveArray)hashmap.remove(name);
        return (PrimitiveArray)hashmap.put(String2.canonical(name), value);
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, PrimitiveArray value) {
        set(name, value);
        return this;
    }

    /**
     * This calls set() for all the attributes in 'additional'.
     * In case of duplicate names, the 'additional' attributes have precedence.
     *
     * @param moreImportant the Attributes to be added.
     */
    public void set(Attributes moreImportant) {
        Enumeration en = moreImportant.hashmap.keys();
        while (en.hasMoreElements()) {
            String name = (String)en.nextElement();
            set(name, (PrimitiveArray)moreImportant.get(name).clone());
        }
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param moreImportant the Attributes to be added
     * @return 'this'
     */
    public Attributes add(Attributes moreImportant) {
        set(moreImportant);
        return this;
    }

    /** 
     * A convenience method which stores the String in a StringArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'.
     *    If value is null or "", name is removed from attributes.
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, String value) {
        //netCDF doesn't allow 0 length strings
        if (value == null || value.trim().length() == 0) 
            return remove(name);
        return set(name, new StringArray(new String[]{value})); //new StringArray calls String2.canonical
    }

    /** 
     * Like set, but only sets the value if there is no current value.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'.
     *    If value is null or "", name is removed from attributes.
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray setIfNotAlreadySet(String name, String value) {
        PrimitiveArray pa = get(name);
        if (pa != null)
            return pa;
        return set(name, value);
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a String which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, String value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the double in a DoubleArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, double value) {
        return set(name, new DoubleArray(new double[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a double which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, double value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the float in a FloatArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, float value) {
        return set(name, new FloatArray(new float[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a float which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, float value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the long in an LongArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, long value) {
        return set(name, new LongArray(new long[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a long which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, long value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the int in an IntArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, int value) {
        return set(name, new IntArray(new int[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value an int which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, int value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the short in an ShortArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, short value) {
        return set(name, new ShortArray(new short[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a short which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, short value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the char in an CharArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, char value) {
        return set(name, new CharArray(new char[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a char which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, char value) {
        set(name, value);
        return this;
    }

    /** A convenience method which stores the byte in an ByteArray then stores the attribute.
     * 
     * @param name the name of the attribute
     * @param value a PrimitiveArray which is the value associate with the 'name'
     * @return the previous value stored for attributeName, or null if none
     */
    public PrimitiveArray set(String name, byte value) {
        return set(name, new ByteArray(new byte[]{value}));
    }

    /**
     * This is like the similar set() method, but returns 'this'.
     * add() lets you string several set commands together,
     * e.g., (new Attributes()).add("name", "Bob").add("height", 197);
     *
     * @param name the name of the attribute
     * @param value a byte which is the value associate with the 'name'
     * @return 'this'
     */
    public Attributes add(String name, byte value) {
        set(name, value);
        return this;
    }

    /** This prints the attributes to a newline separated String, one per line: "&nbsp;&nbsp;&nbsp;&nbsp;[name]=[value]".*/
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String names[] = getNames();
        for (int i = 0; i < names.length; i++) {
            sb.append("    " + names[i] + "=" + get(names[i]).toJsonCsvString() + "\n");
        }
        return sb.toString();
    }

    /** This removes any entry which has a String value of 'value'. */
    public void removeValue(String value) {
        Iterator it = hashmap.keySet().iterator(); //iterator (not enumeration) since I use it.remove() below
        while (it.hasNext()) {
            String name = (String)it.next();
            if (get(name).toString().equals(value))
                it.remove();
        }
    }

    /**
     * This generates a String with 
     * "[prefix][name]=[value][suffix]" on each line.
     *
     * <p>This nc-style version is used to print netcdf header attributes. 
     * It uses String2.toJson for String attributes. 
     *
     * @param prefix
     * @param suffix
     * @return the desired string representation
     */
    public String toNcString(String prefix, String suffix) {
//<p>Was: It puts quotes around String attributeValues,
// converts \ to \\, and " to \", and appends 'f' to float values.
        StringBuilder sb = new StringBuilder();
        String names[] = getNames();
        for (int index = 0; index < names.length; index++) {
            sb.append(prefix + names[index] + " = ");
            Object o = hashmap.get(names[index]);
            String connect = "";
            if (o instanceof StringArray) {
                StringArray sa = (StringArray)o;
                int n = sa.size();
                for (int i = 0; i < n; i++) {
                    sb.append(connect);
                    connect = ", ";
                    //we don't need/want full String2.toJson encoding, just encode \ and ".
                    String s = String2.replaceAll(sa.get(i), "\\", "\\\\");  //  \ becomes \\
                    s = String2.replaceAll(s, "\"", "\\\"");                 //  " becomes \"
                    sb.append("\"" + s + "\"");
                }
            } else if (o instanceof FloatArray) {
                FloatArray fa = (FloatArray)o;
                int n = fa.size();
                for (int i = 0; i < n; i++) {
                    sb.append(connect);
                    connect = ", ";
                    sb.append(fa.get(i));
                    sb.append("f");
                }
            } else {
                sb.append(o.toString());
            }
            sb.append(suffix + "\n");
        }
        return sb.toString();
    }

    /**
     * This makes destination's contents equal this Attribute's contents.
     * The values (primitiveArrays) are cloned.
     *
     * @param destination the Attributes which will be made equal to 'source'.
     */
    public void copyTo(Attributes destination) {
        destination.hashmap.clear();
        Enumeration en = hashmap.keys();
        while (en.hasMoreElements()) {
            String name = (String)en.nextElement();
            destination.set(name, (PrimitiveArray)get(name).clone());
        }
    }

    /** 
     * This tests if o is an Attributes and has the same data. 
     * This doesn't throw an Exception if a difference is found.
     *
     * @param o an object, presumably an Attributes
     */
    public boolean equals(Object o) {
        return testEquals(o).length() == 0;
    }

    /** 
     * This returns a string indicating the differents of this Attributes and o,
     * or "" if no difference. 
     * This doesn't throw an Exception if a difference is found.
     *
     * @param o an object, presumably an Attributes
     */
    public String testEquals(Object o) {
        if (o == null)
            return "The new Attributes object is null.";
        if (!(o instanceof Attributes))
            return "The new object isn't an Attributes object.";
        return Test.testEqual(toString(), ((Attributes)o).toString(), "");
    }

    /**
     * This returns a netcdf-style String representation of a PrimitiveArray:
     * StringArray is newline separated, others are comma separated.
     *
     * @param pa
     * @return a String representation of a value PrimitiveArray.
     */
    public static String valueToNcString(PrimitiveArray pa) {
        if (pa instanceof StringArray)
            return String2.toSVString(((StringArray)pa).toStringArray(), "\n", false); //false=no trailing newline
        return pa.toString();
    }

    /**
     * This removes keys and values from this Attributes which 
     * are the same in otherAtts.
     * 
     * @param otherAtts
     */
    public void removeIfSame(Attributes otherAtts) {
        Iterator it = hashmap.keySet().iterator(); //iterator (not enumeration) since I use it.remove() below
        while (it.hasNext()) {
            String name = (String)it.next();
            PrimitiveArray otherPa = otherAtts.get(name);
            if (otherPa != null) {
                PrimitiveArray pa = get(name);
                if (pa.equals(otherPa)) 
                    it.remove();
            }
        }
    }

    /**
     * This trim()s all names/keys and trim()s all 1 String values.
     */
    public void trim() {
        Iterator it = hashmap.keySet().iterator(); 
        while (it.hasNext()) {
            String name = (String)it.next();
            String tName = name.trim();
            PrimitiveArray pa = null;
            if (!name.equals(tName)) {
                //switch to trim'd name
                pa = remove(name);
                set(name = tName, pa);
            } else {
                pa = get(name);
            }

            //trim value?
            if (pa instanceof StringArray && pa.size() > 0) {
                pa.setString(0, String2.trimStart(pa.getString(0)));
                pa.setString(pa.size() - 1, String2.trimEnd(pa.getString(pa.size() - 1)));
            }
        }

    }



    /**
     * This tests the methods in this class.
     */
    public static void test() throws Exception {
        String2.log("\n*** test Attributes...");

        //set  and size
        Attributes atts = new Attributes();
        Test.ensureEqual(atts.size(), 0, "");
        atts.set("byte", (byte)1);
        atts.set("char", 'a');
        atts.set("short", (short)3000);
        atts.set("int",  1000000);
        atts.set("long", 1000000000000L);
        atts.set("float", 2.5f);
        atts.set("double", Math.PI);
        atts.set("String", "a, csv, string");
        atts.set("PA", new IntArray(new int[]{1,2,3}));

        Test.ensureEqual(atts.size(), 9, "");

        //add and remove an item
        Test.ensureEqual(atts.set("zz", new IntArray(new int[]{1})), null, "");
        Test.ensureEqual(atts.size(), 10, "");

        Test.ensureEqual(atts.set("zz", (PrimitiveArray)null), new IntArray(new int[]{1}), "");
        Test.ensureEqual(atts.size(), 9, "");

        //add and remove an item
        Test.ensureEqual(atts.set("zz", new IntArray(new int[]{2})), null, "");
        Test.ensureEqual(atts.size(), 10, "");

        Test.ensureEqual(atts.remove("zz"), new IntArray(new int[]{2}), "");
        Test.ensureEqual(atts.size(), 9, "");

        ////empty string same as null; attribute removed
        atts.set("zz", "a"); 
        Test.ensureEqual(atts.size(), 10, "");
        atts.set("zz", ""); 
        Test.ensureEqual(atts.size(), 9, "");

        //get
        Test.ensureEqual(atts.get("byte"),   new ByteArray(new byte[]{(byte)1}), "");
        Test.ensureEqual(atts.get("char"),   new CharArray(new char[]{'a'}), "");
        Test.ensureEqual(atts.get("short"),  new ShortArray(new short[]{(short)3000}), "");
        Test.ensureEqual(atts.get("int"),    new IntArray(new int[]{1000000}), "");
        Test.ensureEqual(atts.get("long"),   new LongArray(new long[]{1000000000000L}), "");
        Test.ensureEqual(atts.get("float"),  new FloatArray(new float[]{2.5f}), "");
        Test.ensureEqual(atts.get("double"), new DoubleArray(new double[]{Math.PI}), "");
        Test.ensureEqual(atts.get("String"), new StringArray(new String[]{"a, csv, string"}), "");
        Test.ensureEqual(atts.get("PA"),     new IntArray(new int[]{1,2,3}), "");

        Test.ensureEqual(atts.getInt("byte"),  1, "");
        Test.ensureEqual(atts.getInt("char"),  97, "");
        Test.ensureEqual(atts.getInt("short"), 3000, "");
        Test.ensureEqual(atts.getInt(   "int"), 1000000, "");
        Test.ensureEqual(atts.getLong(  "int"), 1000000, "");
        Test.ensureEqual(atts.getString("int"), "1000000", "");
        Test.ensureEqual(atts.getLong(  "long"), 1000000000000L, "");
        Test.ensureEqual(atts.getInt(   "long"), Integer.MAX_VALUE, "");
        Test.ensureEqual(atts.getFloat( "float"), 2.5f, "");
        Test.ensureEqual(atts.getDouble("float"), 2.5f, "");
        Test.ensureEqual(atts.getString("float"), "2.5", "");
        Test.ensureEqual(atts.getDouble("double"), Math.PI, "");
        Test.ensureEqual(atts.getInt(   "double"), 3, "");
        Test.ensureEqual(atts.getString(        "String"), "a, csv, string", "");
        Test.ensureEqual(atts.getStringsFromCSV("String"), new String[]{"a", "csv", "string"}, "");
        Test.ensureEqual(atts.getInt(           "String"), Integer.MAX_VALUE, "");
        Test.ensureEqual(atts.get(      "PA"), new IntArray(new int[]{1,2,3}), "");
        Test.ensureEqual(atts.getInt(   "PA"), 1, "");
        Test.ensureEqual(atts.getDouble("PA"), 1, "");
        Test.ensureEqual(atts.getString("PA"), "1", "");

        //getNames
        Test.ensureEqual(atts.getNames(), 
            new String[]{"byte", "char", "double", "float", "int", "long", "PA", "short", "String"}, 
            "");

        //toString
        Test.ensureEqual(atts.toString(), 
            "    byte=1\n" +
            "    char=97\n" +
            "    double=3.141592653589793\n" +
            "    float=2.5\n" +
            "    int=1000000\n" +
            "    long=1000000000000\n" +
            "    PA=1, 2, 3\n" +   
            "    short=3000\n" +
            "    String=\"a, csv, string\"\n",
            "");

        //clone   
        Attributes atts2 = (Attributes)atts.clone();
        Test.ensureEqual(atts2.get("byte"),   new ByteArray(new byte[]{(byte)1}), "");
        Test.ensureEqual(atts2.get("char"),   new CharArray(new char[]{'a'}), "");
        Test.ensureEqual(atts2.get("short"),  new ShortArray(new short[]{(short)3000}), "");
        Test.ensureEqual(atts2.get("int"),    new IntArray(new int[]{1000000}), "");
        Test.ensureEqual(atts2.get("long"),   new LongArray(new long[]{1000000000000L}), "");
        Test.ensureEqual(atts2.get("float"),  new FloatArray(new float[]{2.5f}), "");
        Test.ensureEqual(atts2.get("double"), new DoubleArray(new double[]{Math.PI}), "");
        Test.ensureEqual(atts2.get("String"), new StringArray(new String[]{"a, csv, string"}), "");
        Test.ensureEqual(atts2.get("PA"),     new IntArray(new int[]{1,2,3}), "");

        Test.ensureEqual(atts2.getNames(), 
            new String[]{"byte", "char", "double", "float", "int", "long", "PA", "short", "String"},
            "");
        
        //clear
        atts2.clear();
        Test.ensureEqual(atts2.getNames(), new String[]{}, "");

        //copyTo
        atts.copyTo(atts2);
        Test.ensureEqual(atts2.getNames(), 
            new String[]{"byte", "char", "double", "float", "int", "long", "PA", "short", "String"},
            "");
        Test.ensureEqual(atts2.get("String"), new StringArray(new String[]{"a, csv, string"}), "");

        //equals
        Test.ensureTrue(atts.equals(atts2), "");

        //add
        Attributes atts3 = (new Attributes()).add("byte", (byte)1)
            .add("char", 'a')
            .add("short", (short)3000)
            .add("int",  1000000)
            .add("long", 1000000000000L)
            .add("float", 2.5f)
            .add("double", Math.PI)
            .add("String", "a, csv, string")
            .add("PA", new IntArray(new int[]{1,2,3}));
        Test.ensureTrue(atts3.equals(atts), "");

        //set(attributes)
        Attributes atts4 = (new Attributes()).add("zztop", 77) //new name
            .add("char", 'd')   //diff value
            .add("short", (short)3000); //same value
        atts3.set(atts4);
        Test.ensureEqual(atts3.toString(), 
            "    byte=1\n" +
            "    char=100\n" +
            "    double=3.141592653589793\n" +
            "    float=2.5\n" +
            "    int=1000000\n" +
            "    long=1000000000000\n" +
            "    PA=1, 2, 3\n" +   
            "    short=3000\n" +
            "    String=\"a, csv, string\"\n" +
            "    zztop=77\n",
            "");

        //_FillValue
        atts.clear();
        atts.set("_FillValue", new ShortArray(new short[]{(short)32767}));
        String2.log("atts.size()=" + atts.size());
        PrimitiveArray fv = atts.get("_FillValue");
        if (fv == null || fv.size() != 1 || !(fv instanceof ShortArray))
            throw new Exception("fv=" + fv);
        atts.remove("_FillValue");
        
        //keepIfDifferent
        atts.clear();
        atts.add("a", 1);
        atts.add("b", "2");
        atts.add("c", 3f);
        atts2.clear();
        atts2.add("b", "2");
        atts2.add("c", 3d); //changed to d
        atts2.add("d", 4d);
        atts.removeIfSame(atts2);
        Test.ensureEqual(atts.toString(), 
            "    a=1\n" +
            "    c=3.0\n",
            "");

        //trim
        atts.clear();
        atts.add(" a ", " A ");
        atts.add("b ", "B");
        atts.add("c",  "C");
        atts.add("d",  4);
        atts.trim();
        Test.ensureEqual(atts.toString(), 
            "    a=\"A\"\n" +
            "    b=\"B\"\n" +
            "    c=\"C\"\n" +
            "    d=4\n",
            "");




        String2.log("*** test Attributes finished successfully.");
    } 

}
