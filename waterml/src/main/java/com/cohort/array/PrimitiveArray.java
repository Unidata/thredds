/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.array;

import com.cohort.util.*;

import java.io.*;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PrimitiveArray defines the methods to be implemented by various XxxArray classes
 * for 1D arrays of primitives with methods like ArrayLists's methods.
 *
 * <p> Primitive Arrays for integer types support the idea of a NaN or missing value,
 * by designating their MAX_VALUE as the missing value. This is consistent with JGOFS
 * ("the server will set the missing value field to the largest value 
 * possible for specified type.",
 * http://www.opendap.org/server/install-html/install_22.html). 
 * This has the convenient side effect that missing values sort high
 * (as to NaNs for floats and doubles).
 *
 * <p>PrimitiveArrays are not synchronized and so are not thread safe
 * (i.e., they are not safe to use with multiple threads).
 */  
public abstract class PrimitiveArray {

    /** The number of active values (which may be different from the array's capacity). */
    protected int size = 0;

    /**
     * The constants identify the items in the array returned by calculateStats.
     */
    public final static int STATS_N = 0, STATS_MIN = 1, STATS_MAX = 2, STATS_SUM = 3; 

    //these are carefully ordered from smallest to largest data class
    public final static int CLASS_INDEX_BYTE = 0;
    public final static int CLASS_INDEX_SHORT = 1;
    public final static int CLASS_INDEX_CHAR = 2;
    public final static int CLASS_INDEX_INT = 3;
    public final static int CLASS_INDEX_LONG = 4;
    public final static int CLASS_INDEX_FLOAT = 5;
    public final static int CLASS_INDEX_DOUBLE = 6;
    public final static int CLASS_INDEX_STRING = 7;     

    /** The regular expression operator. The OPeNDAP spec says =~, so that is the ERDDAP standard.
     * But some implementations use ~=, so see sourceRegexOp. 
     * See also Table.REGEX_OP. */
    public final static String REGEX_OP = "=~";

    /** 
     * These are *not* final so EDStatic can replace them with translated Strings. 
     * These are MessageFormat-style strings, so any single quote ' must be escaped as ''. 
     * Note that some are errors; some are just messages.
     */
    public static String ArrayAddN =
        String2.ERROR + " in {0}.addN: n ({1}) < 0.";
    public static String ArrayAppendTables =
        String2.ERROR + " in PrimitiveArray.append:\n" +
        "the tables have a different number of columns ({0} != {1}).";
    public static String ArrayDiff =                      
        String2.ERROR + ": The PrimitiveArrays differ at [{0}] ({1} != {2}).";
    public static String ArrayDifferentSize =
        "The other primitiveArray has a different size ({0} != {1}).";
    public static String ArrayDifferentValue =
        "The other primitiveArray has a different value for [{0}] ({1} != {2}).";
    public static String ArrayDiffString =
        "old [{0}]={1},\n  new [{0}]={2}.";  //keep 2 spaces before "new"
    public static String ArrayMissingValue = 
        "missing value";
    public static String ArrayNotAscending =
        "{0} isn''t sorted in ascending order: {1}.";
    public static String ArrayNotDescending =
        "{0} isn''t sorted in descending order: {1}.";
    public static String ArrayNotEvenlySpaced =
        "{0} isn''t evenly spaced: [{1}]={2}, [{3}]={4}, spacing={5}, expected spacing={6}.";
    public static String ArrayRemove =
        String2.ERROR + " in {0}.remove: index ({1}) >= size ({2}).";
    public static String ArraySubsetStart =
        String2.ERROR + " in {0}.subset: startIndex={1} must be at least 0.";
    public static String ArraySubsetStride =
        String2.ERROR + " in {0}.subset: stride={1} must greater than 0.";

    /** 
     * This returns a PrimitiveArray wrapped around a String[] or array of primitives.
     *
     * @param o a char[][], String[] or primitive[] (e.g., int[])
     * @return a PrimitiveArray which (at least initially) uses the array for data storage.
     */
    public static PrimitiveArray factory(Object o) {
        if (o instanceof char[][]) {
            char[][] car = (char[][])o;
            int nStrings = car.length;
            StringArray sa = new StringArray(nStrings, false);
            for (int i = 0; i < nStrings; i++) {
                String s = new String(car[i]);
                int po0 = s.indexOf('\u0000');
                if (po0 >= 0)
                    s = s.substring(0, po0);
                sa.add(s);
            }
            return sa;
        }
        if (o instanceof double[]) return new DoubleArray((double[])o);
        if (o instanceof float[])  return new FloatArray((float[])o);
        if (o instanceof long[])   return new LongArray((long[])o);
        if (o instanceof int[])    return new IntArray((int[])o);
        if (o instanceof short[])  return new ShortArray((short[])o);
        if (o instanceof byte[])   return new ByteArray((byte[])o);
        if (o instanceof char[])   return new CharArray((char[])o);
        if (o instanceof String[]) return new StringArray((String[])o);
        if (o instanceof Object[]) {
            Object oar[] = (Object[])o;
            int n = oar.length;
            StringArray sa = new StringArray(n, false);
            for (int i = 0; i < n; i++)
                sa.add(oar[i].toString());
            return sa;
        }

        if (o == null)
            throw new IllegalArgumentException(String2.ERROR + 
                " in PrimitiveArray.factory: o is null.");
        throw new IllegalArgumentException(String2.ERROR + 
            " in PrimitiveArray.factory: unexpected object type: " + o.toString());
    }

    /**
     * This returns the current capacity (number of elements) of the internal data array.
     * 
     * @return the current capacity (number of elements) of the internal data array.
     */
    abstract public int capacity();

    /**
     * This makes a new object which is a copy of this object.
     *
     * @return a new object, with the same elements.
     *    It will have a new backing array with a capacity equal to its size.
     */
    public Object clone() {
        return subset(0, 1, size - 1);
    }

    /**
     * This returns a PrimitiveArray of a specified type and capacity.
     *
     * @param elementClass e.g., float.class
     * @param capacity
     * @param active if true, size will be set to capacity, else size = 0.
     * @return a PrimitiveArray
     */
    public static PrimitiveArray factory(Class elementClass, int capacity, boolean active) {
        if (elementClass == double.class) return new DoubleArray(capacity, active);
        if (elementClass == float.class)  return new FloatArray(capacity, active);
        if (elementClass == long.class)   return new LongArray(capacity, active);
        if (elementClass == int.class)    return new IntArray(capacity, active);
        if (elementClass == short.class)  return new ShortArray(capacity, active);
        if (elementClass == byte.class)   return new ByteArray(capacity, active);
        if (elementClass == char.class)   return new CharArray(capacity, active);
        if (elementClass == String.class) return new StringArray(capacity, active);

        throw new IllegalArgumentException(String2.ERROR + 
            " in PrimitiveArray.factory: unexpected elementClass: " + elementClass);
    }

    /**
     * This returns the current pa (if already correct type) 
     * or a new pa of a specified type.
     *
     * @param elementClass e.g., float.class
     * @return a PrimitiveArray
     */
    public static PrimitiveArray factory(Class elementClass, PrimitiveArray pa) {
        if (elementClass == double.class) 
            return pa.elementClass() == double.class? pa : new DoubleArray(pa);
        if (elementClass == float.class)  
            return pa.elementClass() == float.class?  pa : new FloatArray(pa);
        if (elementClass == long.class)   
            return pa.elementClass() == long.class?   pa : new LongArray(pa);
        if (elementClass == int.class)    
            return pa.elementClass() == int.class?    pa : new IntArray(pa);
        if (elementClass == short.class)  
            return pa.elementClass() == short.class?  pa : new ShortArray(pa);
        if (elementClass == byte.class)   
            return pa.elementClass() == byte.class?   pa : new ByteArray(pa);
        if (elementClass == char.class)   
            return pa.elementClass() == char.class?   pa : new CharArray(pa);
        if (elementClass == String.class) 
            return pa.elementClass() == String.class? pa : new StringArray(pa);

        throw new IllegalArgumentException(String2.ERROR + 
            " in PrimitiveArray.factory: unexpected elementClass: " + elementClass);
    }

    /** 
     * This returns a PrimitiveArray with size constantValues.
     *
     * @param elementClass e.g., float.class
     * @param size the desired number of elements
     * @param constantValue the value for all of the elements (e.g., "1.6").
     *    For numeric elementClasses, constantValue is parsed.
     *    For char, the first character is used (e.g., "ab" -&gt; 'a' -&gt; 97).
     * @return a PrimitiveArray with size constantValues.
     */
    public static PrimitiveArray factory(Class elementClass, int size, String constantValue) {
        Test.ensureNotNull(constantValue, 
            String2.ERROR + " in PrimitiveArray.factory: constantValue is null.");

        if (elementClass == double.class) {
            Math2.ensureMemoryAvailable(size * 8L, "PrimitiveArray.factory(double, " + size + ")");
            double ar[] = new double[size];
            Arrays.fill(ar, String2.parseDouble(constantValue)); //does to/from String bruise it?
            return new DoubleArray(ar);
        }
        if (elementClass == float.class) {
            Math2.ensureMemoryAvailable(size * 4L, "PrimitiveArray.factory(float, " + size + ")");
            float ar[] = new float[size];
            Arrays.fill(ar, String2.parseFloat(constantValue)); //does to/from String bruise it?
            return new FloatArray(ar);
        }
        if (elementClass == long.class) {
            Math2.ensureMemoryAvailable(size * 8L, "PrimitiveArray.factory(long, " + size + ")");
            long ar[] = new long[size];
            Arrays.fill(ar, String2.parseLong(constantValue)); 
            return new LongArray(ar);
        }  
        if (elementClass == int.class) {
            Math2.ensureMemoryAvailable(size * 4L, "PrimitiveArray.factory(int, " + size + ")");
            int ar[] = new int[size];
            Arrays.fill(ar, String2.parseInt(constantValue)); 
            return new IntArray(ar);
        }
        if (elementClass == short.class) {
            Math2.ensureMemoryAvailable(size * 2L, "PrimitiveArray.factory(short, " + size + ")");
            short ar[] = new short[size];
            Arrays.fill(ar, Math2.roundToShort(String2.parseInt(constantValue)));
            return new ShortArray(ar);
        }
        if (elementClass == byte.class) {
            Math2.ensureMemoryAvailable(size, "PrimitiveArray.factory(byte, " + size + ")");
            byte ar[] = new byte[size];
            Arrays.fill(ar, Math2.roundToByte(String2.parseInt(constantValue))); 
            return new ByteArray(ar);
        }
        if (elementClass == char.class) {
            Math2.ensureMemoryAvailable(size * 2L, "PrimitiveArray.factory(char, " + size + ")");
            char ar[] = new char[size];
            Arrays.fill(ar, constantValue.charAt(0)); 
            return new CharArray(ar);
        }
        if (elementClass == String.class) {
            Math2.ensureMemoryAvailable(size * 8L, "PrimitiveArray.factory(String, " + size + ")"); //8L is crude estimate of size
            String ar[] = new String[size];
            Arrays.fill(ar, constantValue); //does to/from String bruise it?
            return new StringArray(ar);
        }
        throw new IllegalArgumentException(String2.ERROR + 
            " in PrimitiveArray.factory: unexpected elementClass: " + elementClass);
    }

    /**
     * This returns a PrimitiveArray of the specified type from the comma-separated values.
     *
     * @param elementClass e.g., float.class or String.class
     * @param csv For elementClass=String.class, individual values with interior commas
     *    must be completely enclosed in double quotes with interior double
     *    quotes converted to 2 double quotes or backslash quote. For String values without interior commas,
     *    you don't have to double quote the whole value.    
     * @return a PrimitiveArray
     */
    public static PrimitiveArray csvFactory(Class elementClass, String csv) {

        String[] sa = StringArray.arrayFromCSV(csv);
        if (elementClass == String.class) 
            return new StringArray(sa);
        int n = sa.length;
        PrimitiveArray pa = factory(elementClass, n, false);
        for (int i = 0; i < n; i++)
            pa.addString(sa[i]);
        return pa;

        /* retired 2010-10-13
        if (elementClass == String.class) 
            return StringArray.fromCSV(csv);

        String[] sa = String2.split(csv, ',');
        int n = sa.length;
        PrimitiveArray pa = factory(elementClass, n, false);
        for (int i = 0; i < n; i++)
            pa.addString(sa[i]);
        return pa;
        */
    }

    /**
     * This returns a PrimitiveArray of the specified type from the space or comma-separated values.
     *
     * @param elementClass e.g., float.class or String.class
     * @param ssv For elementClass=String.class, individual values with interior spaces or commas
     *    must be completely enclosed in double quotes with interior double
     *    quotes converted to 2 double quotes. For String values without interior spaces or commas,
     *    you don't have to double quote the whole value.    
     * @return a PrimitiveArray
     */
    public static PrimitiveArray ssvFactory(Class elementClass, String ssv) {
        StringArray sa = StringArray.wordsAndQuotedPhrases(ssv);
        if (elementClass == String.class) 
            return sa;
        int n = sa.size();
        PrimitiveArray pa = factory(elementClass, n, false);
        for (int i = 0; i < n; i++)
            pa.addString(sa.get(i));
        return pa;
    }

    /**
     * Return the number of elements in the array.
     *
     * @return the number of elements in the array.
     */
    public int size() {
        return size;
    }

    /**
     * This sets size to 0.
     * XxxArrays with objects override this to set the no-longer-accessible 
     * elements to null.
     *
     */
    public void clear() {
        size = 0;
    }

    /**
     * This returns the class (e.g., float.class or String.class) 
     * of the element type.
     *
     * @return the class (e.g., float.class) of the element type.
     */
    abstract public Class elementClass();

    /**
     * This returns the string form (e.g., "float" or "String") 
     * of the element type.
     *
     * @return the string form (e.g., "float" or "String") 
     * of the element type.
     */
    public String elementClassString() {
        return elementClassToString(elementClass());
    }

    /**
     * This converts an element type (e.g., float.class) to a String (e.g., float).
     *
     * @param type an element type (e.g., float.class)
     * @return the string representation of the element type (e.g., float)
     * @throws exception if not one of the PrimitiveArray types
     */
    public static String elementClassToString(Class type) {
        if (type == double.class) return "double";
        if (type == float.class)  return "float";
        if (type == long.class)   return "long";
        if (type == int.class)    return "int";
        if (type == short.class)  return "short";
        if (type == byte.class)   return "byte";
        if (type == char.class)   return "char";
        if (type == String.class) return "String";
        throw new IllegalArgumentException(
            "PrimitiveArray.elementClassToString unsupported type: "  + type.toString());
    }

    /**
     * This converts an element type String (e.g., "float") to an element type (e.g., float.class).
     *
     * @param type an element type string (e.g., "float")
     * @return the corresponding element type (e.g., float.class)
     * @throws exception if not one of the PrimitiveArray types
     */
    public static Class elementStringToClass(String type) {
        if (type.equals("double")) return double.class;
        if (type.equals("float"))  return float.class;
        if (type.equals("long"))   return long.class;
        if (type.equals("int"))    return int.class;
        if (type.equals("short"))  return short.class;
        if (type.equals("byte"))   return byte.class;
        if (type.equals("char"))   return char.class;
        if (type.equals("String")) return String.class;
        throw new IllegalArgumentException("PrimitiveArray.elementStringToType unsupported type: " + type);
    }

    /**
     * This indicates the number of bytes per element of the given type.
     * The value for "String" isn't a constant, so this returns 20.
     *
     * @param type an element type string (e.g., "float")
     * @return the corresponding number of bytes
     * @throws exception if not one of the PrimitiveArray types
     */
    public static int elementSize(String type) {
        if (type.equals("double")) return 8;
        if (type.equals("float"))  return 4;
        if (type.equals("long"))   return 8;
        if (type.equals("int"))    return 4;
        if (type.equals("short"))  return 2;
        if (type.equals("byte"))   return 1;
        if (type.equals("char"))   return 2;
        if (type.equals("String")) return 20;
        throw new IllegalArgumentException("PrimitiveArray.sizeOf unsupported type: " + type);
    }

    /**
     * This indicates the number of bytes per element of the given type.
     * The value for String.class isn't a constant, so this returns 20.
     *
     * @param type an element type (e.g., float.class)
     * @return the corresponding number of bytes
     * @throws exception if not one of the PrimitiveArray types
     */
    public static int elementSize(Class type) {
        return elementSize(elementClassToString(type));
    }

    /**
     * This returns the number of bytes per element for this PrimitiveArray.
     * The value for "String" isn't a constant, so this returns 20.
     *
     * @return the number of bytes per element for this PrimitiveArray.
     * The value for "String" isn't a constant, so this returns 20.
     */
    public int elementSize() {
        return elementSize(elementClassString());
    }


    /** 
     * This converts a data class into an ESRI Pixel Type.
     * http://help.arcgis.com/en/arcgismobile/10.0/apis/android/api/com/esri/core/map/ImageServiceParameters.PIXEL_TYPE.html
     * Currently, long.class returns F64
     * Currently, byte.class returns a Java-like S8, not an OPenDAP-like U8.
     * Currently, char.class returns a numeric U16.
     * Currently, String.class and others return UNKNOWN.
     *
     * @param tClass e.g., double.class or String.class
     * @return the corresponding ESRI pixel type
     */
    public static String classToEsriPixelType(Class tClass) {
        //I can't find definitions of C64 and C128
        if (tClass == double.class) return "F64";
        if (tClass == float.class)  return "F32"; 
        if (tClass == long.class)   return "F64"; //not ideal, but no S64
        if (tClass == int.class)    return "S32";
        if (tClass == short.class)  return "S16";
        if (tClass == byte.class)   return "S8"; //Java-like S8 or OPeNDAP-like U8 ?!
        if (tClass == char.class)   return "U16"; //
        //if (tClass == String.class) return ...
        return "UNKNOWN";
    }

    /** 
     * This returns the recommended sql data type for this PrimitiveArray.
     * See 
     * <ul>
     * <li> For Strings this is difficult, because it is hard to know what max length
     *   might be in the future.  See stringLengthFactor below.
     *   (PostgresQL supports varchar without a length, or TEXT,
     *   but it isn't a SQL standard.)
     * <li> This doesn't deal with sql DATE or TIMESTAMP, since I often store 
     *   seconds since epoch in a DoubleArray.
     * <li> Not all SQL types are universally supported (e.g., BIGINT for LongArray).
     *   See http://www.techonthenet.com/sql/datatypes.php .
     * <li> For safety, ByteArray returns SMALLINT (not TINYINT,
     *   which isn't universally supported, e.g., postgresql).
     * <ul>
     *
     * @param stringLengthFactor for StringArrays, this is the factor (typically 1.5)  
     *   to be multiplied by the current max string length (then rounded up to 
     *   a multiple of 10, but only if stringLengthFactor &gt; 1) 
     *   to estimate the varchar length.  
     * @return the recommended sql type as a string e.g., varchar(40)
     */
    public String getSqlTypeString(double stringLengthFactor) {
        Class type = elementClass();
        if (type == double.class) return "double precision";
        if (type == float.class)  return "real";  //postgres treats "float" as double precision
        if (type == long.class)   return "bigint"; //not universally supported (pgsql does support it)
        if (type == int.class)    return "integer";
        if (type == short.class)  return "smallint";
        if (type == byte.class)   return "smallint"; //not TINYINT, not universally supported (even pgsql)
        if (type == char.class)   return "char(1)";
        if (type == String.class) {
            StringArray sa = (StringArray)this;
            int max = Math.max(1, sa.maxStringLength());
            if (stringLengthFactor > 1) {
                max = Math2.roundToInt(max * stringLengthFactor);
                max = Math2.hiDiv(max, 10) * 10;            
            }
            //postgresql doesn't use longvarchar and allows varchar's max to be very large
            return "varchar(" + max + ")";  
        }
        throw new IllegalArgumentException(String2.ERROR + 
            " in PrimitiveArray.getSqlTypeString: unexpected type: " + type.toString());
    }

    /** 
     * This returns the suggested elementClass for the given java.sql.Types.
     * This conversion is not standardized across databases (see 
     * http://www.onlamp.com/pub/a/onlamp/2001/09/13/aboutSQL.html?page=last).
     * But choices below are fairly safe.
     * I can't find a table to link java.sql.Types constants to Postgres types.
     * See postgresql types at
     * http://www.postgresql.org/docs/8.2/static/datatype-numeric.html
     *
     * @param sqlType  a java.sql.Types constant
     * @return a PrimitiveArray of the suggested type.
     *   Basically, numeric types return numeric PrimitiveArrays;
     *     other other types return StringArray.
     *   Bit and Boolean return ByteArray.
     *   Date, Time and Timestamp return a StringArray.
     *   If the type is unexpected, this returns StringArray.
     */
    public static PrimitiveArray sqlFactory(int sqlType) {

        //see recommended types in table at
        //  http://download.oracle.com/javase/7/docs/guide/jdbc/getstart/resultset.html
        //see JDBC API Tutorial book, pg 1087
        if (sqlType == Types.BIT ||      //PrimitiveArray doesn't have a separate BooleanArray
            sqlType == Types.BOOLEAN ||  //PrimitiveArray doesn't have a separate BooleanArray
            sqlType == Types.TINYINT)   return new ByteArray();
        if (sqlType == Types.SMALLINT)  return new ShortArray();
        if (sqlType == Types.INTEGER)   return new IntArray();
        if (sqlType == Types.BIGINT)    return new LongArray();
        if (sqlType == Types.REAL)      return new FloatArray();
        if (sqlType == Types.FLOAT ||   //a 64 bit value(!)
            sqlType == Types.DOUBLE ||
            sqlType == Types.DECIMAL ||  //DECIMAL == NUMERIC; infinite precision!!!
            sqlType == Types.NUMERIC)   return new DoubleArray();
        if (sqlType == Types.DATE ||    //getTimestamp().getTime()/1000.0 -> epochSeconds
            sqlType == Types.TIMESTAMP) return new DoubleArray(); 
        //if (sqlType == Types.CHAR ||
        //    sqlType == Types.LONGVARCHAR ||
        //    sqlType == Types.VARCHAR ||
        //    sqlType == Types.CLOB ||
        //    sqlType == Types.DATALINK ||
        //    sqlType == Types.REF ||
        //    sqlType == Types.TIME ||     //or convert to time in 0001-01-01?
        //       true //What the heck!!!!! just get everything else as String, too.
             return new StringArray(); 
    }

    /**
     * This indicates if a given type (e.g., short.class) can be contained in a long.
     *
     * @param type an element type (e.g., short.class)
     * @return true if the given type (e.g., short.class) can be contained in a long.
     */
    public static boolean isIntegerType(Class type) {
        return 
            type == long.class ||
            type == int.class ||
            type == short.class ||
            type == byte.class ||
            type == char.class;
    }

    /**
     * This returns for missing value for a given element type (e.g., byte.class).
     *
     * @param type an element type (e.g., byte.class)
     * @return the string representation of the element type (e.g., Byte.MAX_VALUE).
     *   Note that the mv for float is Float.NaN, but it gets converted
     *   to Double.NaN when returned by this method.
     *   StringArray supports several incoming missing values, but
     *   "" is used as the outgoing missing value.
     */
    public static double getMissingValue(Class type) {
        if (type == double.class) return Double.NaN;
        if (type == float.class)  return Double.NaN;
        if (type == long.class)   return Long.MAX_VALUE;
        if (type == int.class)    return Integer.MAX_VALUE;
        if (type == short.class)  return Short.MAX_VALUE;
        if (type == byte.class)   return Byte.MAX_VALUE;
        if (type == char.class)   return Character.MAX_VALUE;
        if (type == String.class) return Double.NaN;
        return Double.NaN;
    }

    /**
     * This returns the class index (e.g., CLASS_INDEX_INT) of the element type.
     *
     * @return the class index (e.g., CLASS_INDEX_INT) of the element type.
     */
    abstract public int elementClassIndex();

    /**
     * This adds an element to the array at the specified index.
     *
     * @param index 0..
     * @param value the value, as a String.
     */
    abstract public void addString(int index, String value);

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a String.
     */
    abstract public void addString(String value);

    /**
     * This adds n Strings to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as a String.
     */
    abstract public void addNStrings(int n, String value);

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a float.
     */
    abstract public void addFloat(float value);

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a Double.
     */
    abstract public void addDouble(double value);

    /**
     * This adds n doubles to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as a double.
     */
    abstract public void addNDoubles(int n, double value);

    /**
     * This adds an element to the array.
     *
     * @param value the value, as an int.
     */
    abstract public void addInt(int value);

    /**
     * This adds n ints to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as an int.
     */
    abstract public void addNInts(int n, int value);

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a long.
     */
    abstract public void addLong(long value);

    /**
     * This adds an element from another PrimitiveArray.
     *
     * @param otherPA
     * @param otherIndex
     */
    abstract public void addFromPA(PrimitiveArray otherPA, int otherIndex);

    /**
     * This sets an element from another PrimitiveArray.
     *
     * @param index the index to be set
     * @param otherPA
     * @param otherIndex
     */
    abstract public void setFromPA(int index, PrimitiveArray otherPA, int otherIndex);


    /**
     * This removes the specified element.
     *
     * @param index the element to be removed, 0 ... size-1
     * @throws Exception if trouble.
     */
    abstract public void remove(int index);

    /**
     * This removes the specified range of elements.
     *
     * @param from the first element to be removed, 0 ... size
     * @param to one after the last element to be removed, from ... size
     * @throws Exception if trouble.
     */
    abstract public void removeRange(int from, int to);

    /**
     * Moves elements 'first' through 'last' (inclusive)
     *   to 'destination', shifting intermediate values to fill the gap.
     *
     * @param first  the first to be move
     * @param last  (exclusive)
     * @param destination the destination, can't be in the range 'first+1..last-1'.
     * @throws Exception if trouble
     */
    abstract public void move(int first, int last, int destination);

    /**
     * This just keeps the rows for the 'true' values in the bitset.
     * Rows that aren't kept are removed.
     * The resulting PrimitiveArray is compacted (i.e., it has a smaller size()).
     *
     * @param bitset
     */
    abstract public void justKeep(BitSet bitset);

    /**
     * This ensures that the capacity is at least 'minCapacity'.
     *
     * @param minCapacity the minimum acceptable capacity.
     *    minCapacity is type long, but &gt;= Integer.MAX_VALUE will throw exception.
     */
    abstract public void ensureCapacity(long minCapacity);

    /**
     * This returns a primitive[] (perhaps 'array') which has 'size' 
     * elements.
     *
     * @return a primitive[] (perhaps 'array') which has 'size' elements.
     */
    abstract public Object toObjectArray();

    /**
     * This returns a double[] which has 'size' elements.
     *
     * @return a double[] which has 'size' elements.
     */
    abstract public double[] toDoubleArray();

    /**
     * This returns a String[] which has 'size' elements.
     *
     * @return a String[] which has 'size' elements.
     */
    abstract public String[] toStringArray();

    /**
     * Return a value from the array as an int.
     * Floating point values are rounded.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as an int. String values are parsed
     *   with String2.parseInt and so may return Integer.MAX_VALUE.
     * @throws Exception if trouble.
     */
    abstract public int getInt(int index);

    /**
     * Set a value in the array as an int.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. For numeric PrimitiveArray's, it is narrowed 
     *   if needed by methods like Math2.narrowToByte(i).
     * @throws Exception if trouble.
     */
    abstract public void setInt(int index, int i);

    /**
     * Return a value from the array as a long.
     * Floating point values are rounded.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a long. String values are parsed
     *   with String2.parseLong and so may return Long.MAX_VALUE.
     * @throws Exception if trouble.
     */
    abstract public long getLong(int index);

    /**
     * Set a value in the array as a long.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. For numeric PrimitiveArray's, it is narrowed 
     *   if needed by methods like Math2.narrowToByte(i).
     * @throws Exception if trouble.
     */
    abstract public void setLong(int index, long i);


    /**
     * Return a value from the array as a float.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a float. String values are parsed
     *   with String2.parseFloat and so may return Float.NaN.
     * @throws Exception if trouble.
     */
    abstract public float getFloat(int index);

    /**
     * Set a value in the array as a float.
     * 
     * @param index the index number 0 ... size-1
     * @param d the value. For numeric PrimitiveArray's, it is narrowed 
     *   if needed by methods like Math2.roundToInt(d).
     * @throws Exception if trouble.
     */
    abstract public void setFloat(int index, float d);

    /**
     * Return a value from the array as a double.
     * FloatArray converts float to double in a simplistic way.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a double. String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     * @throws Exception if trouble.
     */
    abstract public double getDouble(int index);

    /**
     * Return a value from the array as a double.
     * FloatArray converts float to double via Math2.floatToDouble.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a double. String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     * @throws Exception if trouble.
     */
    public double getNiceDouble(int index) {
        return getDouble(index);
    }

    /**
     * Set a value in the array as a double.
     * 
     * @param index the index number 0 ... size-1
     * @param d the value. For numeric PrimitiveArray's, it is narrowed 
     *   if needed by methods like Math2.roundToInt(d).
     * @throws Exception if trouble.
     */
    abstract public void setDouble(int index, double d);

    /**
     * Return a value from the array as a String.
     * 
     * @param index the index number 0 ... size-1 
     * @return For numeric types, this returns ("" + ar[index]), or "" if NaN or infinity.
     * @throws Exception if trouble.
     */
    abstract public String getString(int index);

    /**
     * Set a value in the array as a String.
     * 
     * @param index the index number 0 ... size-1 
     * @param s the value. For numeric PrimitiveArray's, it is parsed
     *   with String2.parse and narrowed if needed by methods like
     *   Math2.roundToInt(d).
     * @throws Exception if trouble.
     */
    abstract public void setString(int index, String s);


    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    abstract public int indexOf(String lookFor, int startIndex);

    /**
     * This finds the first value which equals 'lookFor' starting at index 0.
     *
     * @param lookFor the value to be looked for
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(String lookFor) {return indexOf(lookFor, 0); }

    /**
     * This finds the last value which equals'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1. The search progresses towards 0.
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    abstract public int lastIndexOf(String lookFor, int startIndex);

    /**
     * This finds the last value which equals 'lookFor' starting at index size() - 1.
     *
     * @param lookFor the value to be looked for
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int lastIndexOf(String lookFor) {return lastIndexOf(lookFor, size() - 1); }

    /**
     * If size != capacity, this makes a new 'array' of size 'size'
     * so capacity will equal size.
     */
    abstract public void trimToSize();

    /** 
     * This converts the elements into a Comma-Separated-Value (CSV) String.
     * Chars acting like unsigned shorts.
     * StringArray overwrites this to specially encode the strings.
     *
     * @return a Comma-Separated-Value (CSV) String representation 
     */
    public String toCSVString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(getString(i));
        }
        return sb.toString();
    }

    /** 
     * This converts the elements into a Comma-Space-Separated-Value (CSSV) String.
     * Chars acting like unsigned shorts.
     * StringArray overwrites this to specially encode the strings.
     *
     * @return a Comma-Space-Separated-Value (CSSV) String representation 
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(getString(i));
        }
        return sb.toString();
    }

    /**
     * This returns a JSON-style comma-separated-value list of the elements.
     * StringArray overwrites this to make a JSON-style representation.
     *
     * @return a csv string of the elements.
     */
    public String toJsonCsvString() {
        return toString();
    }

    /** 
     * This sorts the elements in ascending order.
     * To get the elements in reverse order, just read from the end of the list
     * to the beginning.
     * For numeric PrimitiveArrays, this is a numeric sort.
     * For String PrimitiveArrays, this is a lexical (plain, simplistic) sort.
     */
    abstract public void sort();

    /** 
     * This is like sort(), but StringArray calls sortIgnoreCase().
     */
    public void sortIgnoreCase() {
        sort();
    }

    /**
     * This calculates min, max, and nValid for the values in this 
     * PrimitiveArray.
     * Each data type's missing value (e.g., Byte.MAX_VALUE) will 
     * be converted to Double.NaN.
     *
     * @return a double[] with 
     *    dar[STATS_N] containing the number of valid values.
     *    dar[STATS_MIN] containing the minimum value, and
     *    dar[STATS_MAX] containing the maximum value.
     *    dar[STATS_SUM] containing the sum of the values.
     *    If n is 0, min and max will be Double.NaN, and sum will be 0.
     */
    public double[] calculateStats() {
        long time = System.currentTimeMillis();

        int n = 0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE; //not Double.MIN_VALUE
        double sum = 0;

        for (int i = 0; i < size; i++) {
            double d = getDouble(i); //converts local missingValue to Double.NaN
            if (Math2.isFinite(d)) { 
                n++;
                min = Math.min(min, d);
                max = Math.max(max, d);
                sum += d;
            }
        }

        if (n == 0) {
            min = Double.NaN;
            max = Double.NaN;
        }
        return new double[]{n, min, max, sum};
    }

    /**
     * This returns a string with the stats from calculateStats in a consistent way.
     */
    public static String displayStats(double[] stats) {
        return "n=" + String2.right(String2.genEFormat6(stats[STATS_N]), 7) +              
            " min=" + String2.right(String2.genEFormat6(stats[STATS_MIN]), 15) + 
            " max=" + String2.right(String2.genEFormat6(stats[STATS_MAX]), 15);
    }


    /**
     * This calculates and returns a string with the stats for this pa.
     */
    public String statsString() {
        double stats[] = calculateStats();
        return 
            "nNaN=" + String2.right(String2.genEFormat6(size() - stats[STATS_N]), 7) +
            " " + displayStats(stats);
    }
    

    /**
     * This compares the values in row1 and row2 for SortComparator,
     * and returns a negative integer, zero, or a positive integer if the 
     * value at index1 is less than, equal to, or greater than 
     * the value at index2.  Think (ar[index1] - ar[index2]).
     *
     * @param index1 an index number 0 ... size-1
     * @param index2 an index number 0 ... size-1
     * @return  a negative integer, zero, or a positive integer if the 
     * value at index1 is less than, equal to, or greater than 
     * the value at index2.
     */
    abstract public int compare(int index1, int index2);

    /**
     * This is like compare(), except for StringArray it is caseInsensitive.
     *
     * @param index1 an index number 0 ... size-1
     * @param index2 an index number 0 ... size-1
     * @return  a negative integer, zero, or a positive integer if the 
     * value at index1 is less than, equal to, or greater than 
     * the value at index2.
     */
    public int compareIgnoreCase(int index1, int index2) {
        return compare(index1, index2);
    }

    /**
     * This copies the value in row 'from' to row 'to'.
     * This does not check that 'from' and 'to' are valid;
     * the caller should be careful.
     * The value for 'from' is unchanged.
     *
     * @param from an index number 0 ... size-1
     * @param to an index number 0 ... size-1
     */
    abstract public void copy(int from, int to);

    /**
     * This reorders the values in 'array' based on rank.
     *
     * @param rank is an Integer[] with values (0 ... size-1) 
     * which points to the row number for a row with a specific 
     * rank (e.g., rank[0] is the row number of the first item 
     * in the sorted list, rank[1] is the row number of the
     * second item in the sorted list, ...).
     */
    abstract public void reorder(int rank[]);

    /**
     * This reverses the order of the values in 'array'.
     *
     */
    public void reverse() {
        //probably more efficient if there were a swap(rowA,rowB) method
        int rank[] = new int[size];
        for (int i = 0; i < size; i++)
            rank[size-1-i] = i;
        reorder(rank);
    }

    /**
     * This writes 'size' elements to a DataOutputStream.
     *
     * @param dos the DataOutputStream
     * @return the number of bytes used per element (for Strings, this is
     *    the size of one of the strings, not others, and so is useless;
     *    for other types the value is consistent).
     *    But if size=0, this returns 0.
     * @throws Exception if trouble
     */
    public int writeDos(DataOutputStream dos) throws Exception {
        //ByteArray overwrites this
        int nb = 0;
        for (int i = 0; i < size; i++)
            nb = writeDos(dos, i);
        return nb;
    }

    /**
     * This writes one element to a DataOutputStream.
     *
     * @param dos the DataOutputStream
     * @param i the index of the element to be written
     * @return the number of bytes used for this element
     *    (for Strings, this varies; for others it is consistent)
     * @throws Exception if trouble
     */
    abstract public int writeDos(DataOutputStream dos, int i) throws Exception;


    /**
     * This reads/adds n elements from a DataInputStream.
     *
     * @param dis the DataInputStream
     * @param n the number of elements to be read/added
     * @throws Exception if trouble
     */
    abstract public void readDis(DataInputStream dis, int n) throws Exception;

    /**
     * This writes all the data to a DataOutputStream in the
     * DODS Array format (see www.opendap.org DAP 2.0 standard, section 7.3.2.1).
     * See also the XDR standard (http://tools.ietf.org/html/rfc4506#section-4.11).
     * ByteArray, ShortArray, StringArray override this.
     * ???Does CharArray need to override this???
     *
     * @param dos
     */
    public void externalizeForDODS(DataOutputStream dos) throws Exception {
        dos.writeInt(size);
        dos.writeInt(size); //yes, a second time
        writeDos(dos);
    }

    /**
     * This writes one element to a DataOutputStream in the
     * DODS Atomic-type format (see www.opendap.org DAP 2.0 standard, section 7.3.2).
     * See also the XDR standard (http://tools.ietf.org/html/rfc4506#section-4.11).
     * ByteArray, ShortArray, StringArray override this.
     * ???Does CharArray need to override this???
     *
     * @param dos
     * @param i the index of the element to be written
     */
    public void externalizeForDODS(DataOutputStream dos, int i) throws Exception {
        writeDos(dos, i);
    }

    /**
     * This reads/appends same-type values to this PrimitiveArray from a DODS DataInputStream,
     * and is thus the complement of externalizeForDODS.
     *
     * @param dis
     * @throws IOException if trouble
     */
    public abstract void internalizeFromDODS(DataInputStream dis) throws java.io.IOException;

    /** 
     * This makes a PrimitiveArray by reading contiguous values from a RandomAccessFile.
     * This doesn't work for String.class.
     * endIndex-startIndex must be less than Integer.MAX_VALUE.
     *
     * @param raf the RandomAccessFile
     * @param type the element type of the original PrimitiveArray
     * @param start the raf offset of the start of the array (nBytes)
     * @param startIndex the index of the desired value (0..)
     * @param endIndex the index after the last desired value (0..)
     * @return a PrimitiveArray
     * @throws Exception if trouble
     */
    public static PrimitiveArray rafFactory(RandomAccessFile raf, Class type, 
        long start, long startIndex, long endIndex) throws Exception {

        long longN = endIndex - startIndex;
        String cause = "PrimitiveArray.rafFactory";
        Math2.ensureArraySizeOkay(longN, cause);
        int n = (int)longN;
        
        //byte
        if (type == byte.class) {
            int nBytesPer = 1;
            byte tar[] = new byte[n];
            Math2.ensureMemoryAvailable(nBytesPer * longN, cause);
            raf.seek(start + nBytesPer * startIndex);  
            for (int i = 0; i < n; i++) 
                tar[i] = raf.readByte();
            return factory(tar);
        }

        //char
        if (type == char.class) {
            int nBytesPer = 2;
            char tar[] = new char[n];
            Math2.ensureMemoryAvailable(nBytesPer * longN, cause);
            raf.seek(start + nBytesPer * startIndex);  
            for (int i = 0; i < n; i++) 
                tar[i] = raf.readChar();
            return factory(tar);
        }

        //double
        if (type == double.class) {
            int nBytesPer = 8;
            double tar[] = new double[n];
            Math2.ensureMemoryAvailable(nBytesPer * longN, cause);
            raf.seek(start + nBytesPer * startIndex);  
            for (int i = 0; i < n; i++) 
                tar[i] = raf.readDouble();
            return factory(tar);
        }

        //float
        if (type == float.class) {
            int nBytesPer = 4;
            float tar[] = new float[n];
            Math2.ensureMemoryAvailable(nBytesPer * longN, cause);
            raf.seek(start + nBytesPer * startIndex);  
            for (int i = 0; i < n; i++) 
                tar[i] = raf.readFloat();
            return factory(tar);
        }

        //int
        if (type == int.class) {
            int nBytesPer = 4;
            int tar[] = new int[n];
            Math2.ensureMemoryAvailable(nBytesPer * longN, cause);
            raf.seek(start + nBytesPer * startIndex);  
            for (int i = 0; i < n; i++) 
                tar[i] = raf.readInt();
            return factory(tar);
        }

        //long
        if (type == byte.class) {
            int nBytesPer = 8;
            long tar[] = new long[n];
            Math2.ensureMemoryAvailable(nBytesPer * longN, cause);
            raf.seek(start + nBytesPer * startIndex);  
            for (int i = 0; i < n; i++) 
                tar[i] = raf.readLong();
            return factory(tar);
        }

        //short
        if (type == byte.class) {
            int nBytesPer = 2;
            short tar[] = new short[n];
            Math2.ensureMemoryAvailable(nBytesPer * longN, cause);
            raf.seek(start + nBytesPer * startIndex);  
            for (int i = 0; i < n; i++) 
                tar[i] = raf.readShort();
            return factory(tar);
        }

        //no support for String 
        throw new Exception("PrimitiveArray.rafFactory type '" + type + "' not supported.");
    }
    /**
     * This reads one number from a randomAccessFile.
     * This doesn't support StringArray (for which you need nBytesPer)
     * and in which you generally wouldn't be storing numbers.
     *
     * @param raf the RandomAccessFile
     * @param type the element type of the original PrimitiveArray
     * @param start the raf offset of the start of the array (nBytes)
     * @param index the index of the desired value (0..)
     * @return the requested value as a double
     * @throws Exception if trouble
     */
    public static double rafReadDouble(RandomAccessFile raf, Class type, 
        long start, long index) throws Exception {

        if (type == byte.class)   return ByteArray.rafReadDouble(  raf, start, index);
        if (type == char.class)   return CharArray.rafReadDouble(  raf, start, index);
        if (type == double.class) return DoubleArray.rafReadDouble(raf, start, index);
        if (type == float.class)  return FloatArray.rafReadDouble( raf, start, index);
        if (type == int.class)    return IntArray.rafReadDouble(   raf, start, index);
        if (type == long.class)   return LongArray.rafReadDouble(  raf, start, index);
        if (type == short.class)  return ShortArray.rafReadDouble( raf, start, index);
        //if (type == String.class) return ByteArray.rafReadDouble(raf, start, index, nBytesPer);
        throw new Exception("PrimitiveArray.rafReadDouble type '" + type + "' not supported.");
    }

    /**
     * This writes one number to a randomAccessFile at the current position.
     * This doesn't support StringArray (for which you need nBytesPer)
     * and in which you generally wouldn't be storing numbers.
     *
     * @param raf the RandomAccessFile
     * @param type the element type of the original PrimitiveArray
     * @param value the value which will be converted to 'type' and then stored
     * @throws Exception if trouble
     */
    public static void rafWriteDouble(RandomAccessFile raf, Class type, 
        double value) throws Exception {

        if (type == byte.class)   {raf.writeByte(Math2.roundToByte(value));       return;}
        if (type == char.class)   {raf.writeChar(Math2.roundToChar(value));       return;}     
        if (type == double.class) {raf.writeDouble(value);                        return;}
        if (type == float.class)  {raf.writeFloat(Math2.doubleToFloatNaN(value)); return;}
        if (type == int.class)    {raf.writeInt(Math2.roundToInt(value));         return;}
        if (type == long.class)   {raf.writeLong(Math2.roundToLong(value));       return;}
        if (type == short.class)  {raf.writeShort(Math2.roundToShort(value));     return;}
        //if (type == String.class) {ByteArray.rafWriteDouble(raf, start, index, nBytesPer); return;}
        throw new Exception("PrimitiveArray.rafWriteDouble type '" + type + "' not supported.");
    }

    /**
     * This writes one number to a randomAccessFile.
     * This doesn't support StringArray (for which you need nBytesPer)
     * and in which you generally wouldn't be storing numbers.
     *
     * @param raf the RandomAccessFile
     * @param type the element type of the original PrimitiveArray
     * @param start the raf offset of the start of the array (nBytes)
     * @param index the index of the value (0..)
     * @param value the value which will be converted to 'type' and then stored
     * @throws Exception if trouble
     */
    public static void rafWriteDouble(RandomAccessFile raf, Class type, 
        long start, long index, double value) throws Exception {

        if (type == byte.class)   {ByteArray.rafWriteDouble(  raf, start, index, value); return;}
        if (type == char.class)   {CharArray.rafWriteDouble(  raf, start, index, value); return;}
        if (type == double.class) {DoubleArray.rafWriteDouble(raf, start, index, value); return;}
        if (type == float.class)  {FloatArray.rafWriteDouble( raf, start, index, value); return;}
        if (type == int.class)    {IntArray.rafWriteDouble(   raf, start, index, value); return;}
        if (type == long.class)   {LongArray.rafWriteDouble(  raf, start, index, value); return;}
        if (type == short.class)  {ShortArray.rafWriteDouble( raf, start, index, value); return;}
        //if (type == String.class) {ByteArray.rafWriteDouble(raf, start, index, nBytesPer); return;}
        throw new Exception("PrimitiveArray.rafWriteDouble type '" + type + "' not supported.");
    }

    /**
     * This tests if the other object is of the same type and has equal values.
     *
     * @param other 
     * @return "" if equal, or message if not.
     *     other=null throws an exception.
     */
    abstract public String testEquals(Object other);

    /**
     * This tests if the other PrimitiveArray has almost equal values.
     * If both are integer types or String types, this is an exact test (and says null==null is true).
     * If either are double types, this tests almostEqual9() (and says NaN==NaN is true).
     * If either are float types, this tests almostEqual5() (and says NaN==NaN is true).
     *
     * @param other 
     * @return "" if almost equal, or message if not.
     *     other=null throws an exception.
     */
    public String almostEqual(PrimitiveArray other) {
        if (size != other.size())
            return MessageFormat.format(ArrayDifferentSize, "" + size, "" + other.size());
        
        if (this instanceof StringArray ||
            other instanceof StringArray) {
            for (int i = 0; i < size; i++) {
                String s1 = getString(i);
                String s2 = other.getString(i);
                if (s1 == null && s2 == null) {
                } else if (s1 != null && s2 != null && s1.equals(s2)) {
                } else {
                    return MessageFormat.format(ArrayDifferentValue, "" + i, 
                        String2.toJson(s1), String2.toJson(s2));
                }
            }
            return "";
        }

        if (this instanceof LongArray && other instanceof LongArray) {
            for (int i = 0; i < size; i++)
                if (!Test.equal(getLong(i), other.getLong(i)))  //this says NaN==NaN is true
                    return MessageFormat.format(ArrayDifferentValue, "" + i, 
                        "" + getLong(i), "" + other.getLong(i));
            return "";
        }

        if (this instanceof DoubleArray || this instanceof LongArray ||
            other instanceof DoubleArray || other instanceof LongArray) {
            for (int i = 0; i < size; i++)
                if (!Test.equal(getDouble(i), other.getDouble(i)))  //this says NaN==NaN is true
                    return MessageFormat.format(ArrayDifferentValue, "" + i, 
                        "" + getDouble(i), "" + other.getDouble(i));
            return "";
        }

        if (this instanceof FloatArray ||
            other instanceof FloatArray) {
            for (int i = 0; i < size; i++)
                if (!Test.equal(getFloat(i), other.getFloat(i))) //this says NaN==NaN is true
                    return MessageFormat.format(ArrayDifferentValue, "" + i, 
                        "" + getFloat(i), "" + other.getFloat(i));
            return "";
        }

        for (int i = 0; i < size; i++)
            if (getInt(i) != other.getInt(i))
                return MessageFormat.format(ArrayDifferentValue, "" + i, 
                    "" + getInt(i), "" + other.getInt(i));
        return "";
    }
        

    /**
     * Given a sorted PrimitiveArray, stored to a randomAccessFile,
     * this finds the index of an instance of the value 
     * (not necessarily the first or last instance)
     * (or -index-1 where it should be inserted).
     *
     * @param raf the RandomAccessFile
     * @param type the element type of the original PrimitiveArray
     * @param start the raf offset of the start of the array
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually 
     *  (originalPrimitiveArray.size() - 1)
     * @param value the value you are searching for
     * @return the index of an instance of the value 
     *     (not necessarily the first or last instance)
     *     (or -index-1 where it should be inserted, with extremes of
     *     -lowPo-1 and -(highPo+1)-1).
     * @throws Exception if trouble
     */
    public static long rafBinarySearch(RandomAccessFile raf, Class type,
        long start, long lowPo, long highPo, double value) throws Exception {
        
        //ensure lowPo <= highPo
        //lowPo == highPo is handled by the following two chunks of code
        Test.ensureTrue(lowPo <= highPo,  
            String2.ERROR + "in PrimitiveArray.rafBinarySearch: lowPo > highPo.");
        
        double tValue = rafReadDouble(raf, type, start, lowPo);
        //String2.log("rafBinarySearch value=" + value + " po=" + lowPo + " tValue=" + tValue);
        if (tValue == value)
            return lowPo;
        if (tValue > value)
            return -lowPo - 1;

        tValue = rafReadDouble(raf, type, start, highPo);
        //String2.log("rafBinarySearch value=" + value + " po=" + highPo + " tValue=" + tValue);
        if (tValue == value)
            return highPo;
        if (tValue < value)
            return -(highPo+1) - 1;

        //repeatedly look at midpoint
        //If no match, this always ends with highPo - lowPo = 1
        //  and desired value would be in between them.
        while (highPo - lowPo > 1) {
            long midPo = (highPo + lowPo) / 2;
            tValue = rafReadDouble(raf, type, start, midPo);
            //String2.log("rafBinarySearch value=" + value + " po=" + midPo + " tValue=" + tValue);
            if (tValue == value)
                return midPo;
            if (tValue < value) 
                lowPo = midPo;
            else highPo = midPo;
        }

        //not found
        return -highPo - 1;
    }
        
    /**
     * Given a sorted PrimitiveArray, stored to a randomAccessFile,
     * this finds the index of the first element >= value. 
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param raf the RandomAccessFile
     * @param type the element type of the original PrimitiveArray
     * @param start the raf offset of the start of the array
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually 
     *  (originalPrimitiveArray.size() - 1)
     * @param value the value you are searching for
     * @return the index of the first element @gt;= value 
     *     (or highPo + 1, if there are none)
     * @throws Exception if trouble
     */
    public static long rafFirstGE(RandomAccessFile raf, Class type,
        long start, long lowPo, long highPo, double value) throws Exception {

        if (lowPo > highPo)
            return highPo + 1;
        long po = rafBinarySearch(raf, type, start, lowPo, highPo, value);

        //an exact match? find the first exact match
        if (po >= 0) {
            while (po > lowPo && rafReadDouble(raf, type, start, po - 1) == value)
                po--;
            return po;
        }

        //no exact match? return the binary search po
        //thus returning a positive number
        //the inverse of -x-1 is -x-1 !
        return -po -1;
    }

    /**
     * Given a sorted PrimitiveArray, stored to a randomAccessFile,
     * this finds the index of the first element &gt; or almostEqual5 to value. 
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param raf the RandomAccessFile
     * @param type the element type of the original PrimitiveArray
     * @param start the raf offset of the start of the array
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually 
     *  (originalPrimitiveArray.size() - 1)
     * @param value the value you are searching for
     * @return the index of the first element &gt; or Math2.almostEqual5 to value 
     *     (or highPo + 1, if there are none)
     * @throws Exception if trouble
     */
    public static long rafFirstGAE5(RandomAccessFile raf, Class type,
        long start, long lowPo, long highPo, double value) throws Exception {

        if (lowPo > highPo)
            return highPo + 1;

        long po = rafBinarySearch(raf, type, start, lowPo, highPo, value);

        //no exact match? return the binary search po
        //thus returning a positive number
        //the inverse of -x-1 is -x-1 !
        if (po < 0)
            po = -po -1;

        //find the first GAE
        while (po > lowPo && Math2.almostEqual(5, rafReadDouble(raf, type, start, po - 1), value))
            po--;

        return po;
    }

    /**
     * Given a sorted PrimitiveArray, stored to a randomAccessFile,
     * this finds the index of the last element &lt;= value. 
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param raf the RandomAccessFile
     * @param type the element type of the original PrimitiveArray
     * @param start the raf offset of the start of the array
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually 
     *  (originalPrimitiveArray.size() - 1)
     * @param value the value you are searching for
     * @return the index of the first element &lt;= value 
     *     (or -1, if there are none)
     * @throws Exception if trouble
     */
    public static long rafLastLE(RandomAccessFile raf, Class type,
        long start, long lowPo, long highPo, double value) throws Exception {

        if (lowPo > highPo)
            return -1;
        long po = rafBinarySearch(raf, type, start, lowPo, highPo, value);

        //an exact match? find the first exact match
        if (po >= 0) {
            while (po < highPo && rafReadDouble(raf, type, start, po + 1) == value)
                po++;
            return po;
        }

        //no exact match? return binary search po -1
        //thus returning a positive number
        //the inverse of -x-1 is -x-1 !
        return -po -1 -1;
    }

    /**
     * Given a sorted PrimitiveArray, stored to a randomAccessFile,
     * this finds the index of the last element &lt; or almostEqual5 to value. 
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param raf the RandomAccessFile
     * @param type the element type of the original PrimitiveArray
     * @param start the raf offset of the start of the array
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually 
     *  (originalPrimitiveArray.size() - 1)
     * @param value the value you are searching for
     * @return the index of the first element &lt; or Math2.almostEqual5 to value 
     *     (or -1, if there are none)
     * @throws Exception if trouble
     */
    public static long rafLastLAE5(RandomAccessFile raf, Class type,
        long start, long lowPo, long highPo, double value) throws Exception {

        if (lowPo > highPo)
            return -1;
        long po = rafBinarySearch(raf, type, start, lowPo, highPo, value);

        //no exact match? return previous value (binary search po -1)
        //thus returning a positive number
        //the inverse of -x-1 is -x-1 !
        if (po < 0)
            po = -po -1 -1;

        //look for last almost equal value
        while (po < highPo && Math2.almostEqual(5, rafReadDouble(raf, type, start, po + 1), value))
            po++;

        return po;
    }

        
    /**
     * Given an ascending sorted PrimitiveArray,
     * this finds the index of an instance of the value 
     * (not necessarily the first or last instance)
     * (or -index-1 where it should be inserted).
     *
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually (size - 1)
     * @param value the value you are searching for
     * @return the index of an instance of the value 
     *     (not necessarily the first or last instance)
     *     (or -index-1 where it should be inserted, with extremes of
     *     -lowPo-1 and -(highPo+1)-1).
     * @throws Exception if lowPo > highPo.
     */
    public int binarySearch(int lowPo, int highPo, double value) {
        
        //ensure lowPo <= highPo
        //lowPo == highPo is handled by the following two chunks of code
        Test.ensureTrue(lowPo <= highPo, 
            String2.ERROR + " in PrimitiveArray.binarySearch: lowPo > highPo.");
        
        double tValue = getDouble(lowPo);
        if (tValue == value)
            return lowPo;
        if (tValue > value)
            return -lowPo - 1;

        tValue = getDouble(highPo);
        if (tValue == value)
            return highPo;
        if (tValue < value)
            return -(highPo+1) - 1;

        //repeatedly look at midpoint
        //If no match, this always ends with highPo - lowPo = 1
        //  and desired value would be in between them.
        while (highPo - lowPo > 1) {
            int midPo = (highPo + lowPo) / 2;
            tValue = getDouble(midPo);
            if (tValue == value)
                return midPo;
            if (tValue < value) 
                lowPo = midPo;
            else highPo = midPo;
        }

        //not found
        return -highPo - 1;
    }
        
    /**
     * Given an ascending sorted PrimitiveArray,
     * this finds the index of the first element &gt;= value. 
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually (size - 1)
     * @param value the value you are searching for
     * @return the index of the first element &gt;= value 
     *     (or highPo + 1, if there are none)
     */
    public int binaryFindFirstGE(int lowPo, int highPo, double value) {

        if (lowPo > highPo)
            return highPo + 1;
         
        int po = binarySearch(lowPo, highPo, value);

        //an exact match? find the first exact match
        if (po >= 0) {
            while (po > lowPo && getDouble(po - 1) == value)
                po--;
            return po;
        }

        //no exact match? return the binary search po
        //thus returning a positive number
        //the inverse of -x-1 is -x-1 !
        return -po -1;
    }

    /**
     * Given an ascending sorted PrimitiveArray,
     * this finds the index of the first element &gt; or almostEqual5 to value. 
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually (size - 1)
     * @param value the value you are searching for
     * @return the index of the first element &gt; or Math2.almostEqual5 to value 
     *     (or highPo + 1, if there are none)
     */
    public int binaryFindFirstGAE5(int lowPo, int highPo, double value) {

        if (lowPo > highPo)
            return highPo + 1;
        int po = binarySearch(lowPo, highPo, value);

        //no exact match? start at high and work back
        //the inverse of -x-1 is -x-1 !
        if (po < 0) 
            po = -po -1;

        //find the first match
        while (po > lowPo && Math2.almostEqual(5, getDouble(po - 1), value))
            po--;

        return po;

    }

    /**
     * Given an ascending sorted PrimitiveArray,
     * this finds the index of the last element &lt;= value. 
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually 
     *  (originalPrimitiveArray.size() - 1)
     * @param value the value you are searching for
     * @return the index of the first element &lt;= value 
     *     (or -1, if there are none)
     */
    public int binaryFindLastLE(int lowPo, int highPo, double value) {

        if (lowPo > highPo)
            return -1;
        int po = binarySearch(lowPo, highPo, value);

        //an exact match? find the first exact match
        if (po >= 0) {
            while (po < highPo && getDouble(po + 1) == value)
                po++;
            return po;
        }

        //no exact match? return binary search po -1
        //thus returning a positive number
        //the inverse of -x-1 is -x-1 !
        return -po -1 -1;
    }


    /**
     * Given an ascending sorted PrimitiveArray,
     * this finds the index of the last element &lt; or almostEqual5 to value. 
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * @param lowPo the low index to start with, usually 0
     * @param highPo the high index to start with, usually 
     *  (originalPrimitiveArray.size() - 1)
     * @param value the value you are searching for
     * @return the index of the first element &lt; or Math2.almostEqual5 to value 
     *     (or -1, if there are none)
     */
    public int binaryFindLastLAE5(int lowPo, int highPo, double value) {

        if (lowPo > highPo)
            return -1;
        int po = binarySearch(lowPo, highPo, value);

        //no exact match? start at lower and work forward
        //the inverse of -x-1 is -x-1 !
        if (po < 0)
            po = -po -1 -1;

        //find the first match
        while (po < highPo && Math2.almostEqual(5, getDouble(po + 1), value))
            po++;

        return po;
    }


    /**
     * Find the closest element to x in an ascending sorted array.
     * If there are duplicates, any may be returned.
     *
     * @param x
     * @return the index of the index of the element closest to x.
     *   If x is NaN, this returns -1.
     */
    public int binaryFindClosest(double x) {
        if (Double.isNaN(x))
            return -1;
        int i = binarySearch(0, size - 1, x);
        if (i >= 0)
            return i; //success, exact match

        //insertionPoint at end point?
        int insertionPoint = -i - 1;  //0.. dar.length
        if (insertionPoint == 0) 
            return 0;
        if (insertionPoint >= size)
            return size - 1;

        //insertionPoint between 2 points 
        if (Math.abs(getDouble(insertionPoint - 1) - x) <
            Math.abs(getDouble(insertionPoint) - x))
             return insertionPoint - 1;
        else return insertionPoint;
    }

    /**
     * Find the closest element to x in an array (regardless of if sorted or not).
     * If there are duplicates, any may be returned (i.e., for now, not specified).
     *
     * @param x
     * @return the index of the index of the element closest to x.
     *   If x is NaN, this returns -1.
     */
    public int linearFindClosest(double x) {
        if (Double.isNaN(x))
            return -1;
        double diff = Double.MAX_VALUE;
        int which = -1;
        for (int i = 0; i < size; i++) {
            double tDiff = Math.abs(getDouble(i) - x);
            if (tDiff < diff) {
                diff = tDiff;
                which = i;
            }
        }
        return which;
    }



    /**
     * This returns an PrimitiveArray which is the simplist possible 
     * type of PrimitiveArray which can accurately hold the data.
     *
     * <p>If this primitiveArray is a StringArray, then null, ".", "", and "NaN" are allowable 
     *   missing values for conversion to numeric types.
     *   And if a value in the column has an internal "." (e.g., 5.0), 
     *   the column will not be converted to an integer type.
     *
     * @return the simpliest possible PrimitiveArray (possibly this PrimitiveArray) 
     *     (although not a CharArray).
     *     A LongArray may return a LongArray, but a StringArray with longs will return a StringArray
     *     (because long ints are often used as String-like identifiers and .nc files don't support longs).
     */
    public PrimitiveArray simplify() {
        int type = 0; //the current, simplest possible type
        int n = size();
        boolean isStringArray = this instanceof StringArray;
        double dar[] = new double[n];
        for (int i = 0; i < n; i++) {
            double d = getDouble(i);
            dar[i] = d; //set this before s.equals tests
            if (isStringArray) {
                String s = getString(i);
                if (s == null || s.equals(".") || s.equals("") || s.equals("NaN"))
                    continue;

                //if a String is found (not an acceptable "NaN" string above), return original array
                //look for e.g., serial number (e.g., 0153) with leading 0 that must be kept as string
                if (s.length() >= 2 && s.charAt(0) == '0' && s.charAt(1) >= '0' && s.charAt(1) <= '9') 
                    return this; 
                if (Double.isNaN(d))
                    return this; //it's already a StringArray

                //if string source column with an internal '.', don't let it be an integer type;
                //always treat as at least a float
                if (s.indexOf('.') >= 0)
                    type = Math.max(type, 4);
            }
            //all types allow NaN
            if (Double.isNaN(d))
                continue;

            //assume column contains only bytes
            //if not true, work way up: short -> int -> long -> float -> double -> String
            if (type == 0) { //byte
                if (d != Math.rint(d) || d < Byte.MIN_VALUE || d > Byte.MAX_VALUE) {
                    type++;
                    if (this instanceof CharArray || this instanceof ShortArray)
                        return this; //don't continue; it would just check that a ShortArray contains shorts
                }
            }
            if (type == 1) { //short
                if (d != Math.rint(d) || d < Short.MIN_VALUE || d > Short.MAX_VALUE) {
                    type++;
                    if (this instanceof IntArray)
                        return this;  //don't continue; it would just check that an IntArray contains ints
                }
            }
            if (type == 2) { //int
                if (d != Math.rint(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
                    type++;
                    if (this instanceof LongArray)
                        return this; //don't continue; it would just check that a LongArray contains longs
                }
            }
            if (type == 3) { //long
                if (d != Math.round(d) || d < Long.MIN_VALUE || d > Long.MAX_VALUE) {
                    type++;
                    if (this instanceof FloatArray)
                        return this;  //don't continue; it would just check that a FloatArray contains floats
                }
            }
            if (type == 4) { //float
                if (d < -Float.MAX_VALUE || d > Float.MAX_VALUE || d != Math2.niceDouble(d, 7)) {
                    type++;
                    if (this instanceof DoubleArray)
                        return this;  //don't continue; it would just check that a DoubletArray contains doubles
                }
            }
            //otherwise it is a valid double
        }

        //make array of simplified type
        if (type == 0) {
            byte[] array = new byte[n];
            for (int i = 0; i < n; i++) 
                array[i] = Math2.roundToByte(dar[i]);
            return new ByteArray(array);
        }
        if (type == 1) {
            short[] array = new short[n];
            for (int i = 0; i < n; i++)
                array[i] = Math2.roundToShort(dar[i]);
            return new ShortArray(array);
        }
        if (type == 2) {
            int[] array = new int[n];
            for (int i = 0; i < n; i++)
                array[i] = Math2.roundToInt(dar[i]);
            return new IntArray(array);
        }
        if (type == 3) {
            //A LongArray may return a LongArray (above),
            //but a StringArray with longs will return a StringArray
            //(because long ints are often used as String-like identifiers
            //and .nc files don't support longs).
            if (isStringArray)
                return this;
            return new StringArray(this);
            //was:
            //long[] array = new long[n];
            //for (int i = 0; i < n; i++)
            //    array[i] = Math2.roundToLong(dar[i]);
            //return new LongArray(array);
        }
        if (type == 4) {
            float[] array = new float[n];
            for (int i = 0; i < n; i++)
                array[i] = (float)dar[i];
            return new FloatArray(array);
        }
        if (type == 5) {
            return new DoubleArray(dar);
        }
        throw new IllegalArgumentException(String2.ERROR + 
            " in PrimitiveArray.simplify: unknown type (" + type + ").");
    }

    /**
     * This appends the data in another primitiveArray to the current data.
     * WARNING: information may be lost from the incoming primitiveArray if this
     * primitiveArray is of a simpler type.
     *
     * @param primitiveArray primitiveArray must be a narrower data type,
     *  or the data will be rounded.
     */
    abstract public void append(PrimitiveArray primitiveArray);
    
    /**
     * Given table[], keys[], and ascending[],
     * this creates an int[] with the ranks the rows of the table. 
     *
     * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
     *
     * @param table a List of PrimitiveArrays 
     * @param keys an array of the key column numbers 
     *    (each is 0..nColumns-1, the first key is the most important)
     *    which are used to determine the sort order
     * @param ascending an array of booleans corresponding to the keys
     *    indicating if the arrays are to be sorted by a given key in 
     *    ascending or descending order.
     * @return an int[] with values (0 ... size-1) 
     *   which points to the row number for a row with a specific 
     *   rank (e.g., rank[0] is the row number of the first item 
     *   in the sorted list, rank[1] is the row number of the
     *   second item in the sorted list, ...).
     */
    public static int[] rank(List table, int keys[], boolean[] ascending) {
        return lowRank(new RowComparator(table, keys, ascending), table);
    }

    /** This is like rank, but StringArrays are tested case insensitively.   */
    public static int[] rankIgnoreCase(List table, int keys[], boolean[] ascending) {
        return lowRank(new RowComparatorIgnoreCase(table, keys, ascending), table);
    }
    
    private static int[] lowRank(RowComparator comparator, List table) {

        //create the rowArray with pointer to specific rows
        int n = ((PrimitiveArray)table.get(0)).size();
        Integer rowArray[] = new Integer[n];
        for (int i = 0; i < n; i++)
            rowArray[i] = new Integer(i);

        //sort the rows
        Arrays.sort(rowArray, comparator);   //this is "stable"
        //String2.log("rank results: " + String2.toCSSVString(integerArray));

        //create the int[] 
        int newArray[] = new int[n];
        for (int i = 0; i < n; i++)
            newArray[i] = rowArray[i].intValue();

        return newArray;
    }


    /**
     * Given a List of PrimitiveArrays, which represents a table of data,
     * this sorts all of the PrimitiveArrays in the table based on the keys and
     * ascending values.
     *
     * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
     *
     * @param table a List of PrimitiveArray[]
     * @param keys an array of the key column numbers 
     *    (each is 0..nColumns-1, the first key is the most important)
     *    which are used to determine the sort order
     * @param ascending an array of booleans corresponding to the keys
     *    indicating if the arrays are to be sorted by a given key in 
     *    ascending or descending order.
     */
    public static void sort(List table, int keys[], boolean[] ascending) {

        //rank the rows
        int ranks[] = rank(table, keys, ascending);

        //reorder the columns
        for (int col = 0; col < table.size(); col++) {
            ((PrimitiveArray)table.get(col)).reorder(ranks);
        }
    }

    /** This is like sort, but StringArrays are tested case insensitively.   */
    public static void sortIgnoreCase(List table, int keys[], boolean[] ascending) {

        //rank the rows
        int ranks[] = rankIgnoreCase(table, keys, ascending);

        //reorder the columns
        for (int col = 0; col < table.size(); col++) {
            ((PrimitiveArray)table.get(col)).reorder(ranks);
        }
    }

    /**
     * Given a List of PrimitiveArrays, which represents a table of data,
     * this copies the values from one row to another (without affecting
     * any other rows).
     *
     * @param table a List of PrimitiveArray
     * @param from the 'from' row
     * @param to the 'to' row
     */
    public static void copyRow(List table, int from, int to) {
        int nColumns = table.size();
        for (int col = 0; col < nColumns; col++) 
            ((PrimitiveArray)table.get(col)).copy(from, to);
    }

    /**
     * Given a sorted (regular or sortIgnoreCase) PrimitiveArray List,
     * this looks for adjacent identical rows of data and removes the duplicates.
     *
     * @return the number of duplicates removed
     */
    public int removeDuplicates() {
        return removeDuplicates(false);
    }

    /**
     * Given a sorted (plain or sortIgnoreCase) PrimitiveArray List,
     * this looks for adjacent identical rows of data and removes the duplicates.
     *
     * @param logDuplicates if true, this prints duplicates to String2.log
     * @return the number of duplicates removed
     */
    public int removeDuplicates(boolean logDuplicates) {

        int nRows = size;
        if (nRows <= 1) 
            return 0;
        int nUnique = 1; //row 0 is unique
        for (int row = 1; row < nRows; row++) { //start at 1; compare to previous row
            //does it equal row above?
            boolean equal = compare(row - 1, row) == 0;
            if (equal) {
                if (logDuplicates) String2.log("  duplicate=" + getString(row));
            } else {
                //not equal? copy row 'row' to row 'nUnique'
                if (row != nUnique) 
                    copy(row, nUnique);
                nUnique++;
            }
        }

        //remove the stuff at the end
        removeRange(nUnique, nRows);

        return nRows - nUnique;
    }

    /**
     * Given a (presumably) PrimitiveArray List, which represents a sorted table of data,
     * this looks for adjacent identical rows of data and removes the duplicates.
     *
     * @param table a List of PrimitiveArray
     * @return the number of duplicates removed
     */
    public static int removeDuplicates(List table) {

        int nRows = ((PrimitiveArray)table.get(0)).size();
        if (nRows <= 1) 
            return 0;
        int nColumns = table.size();
        int nUnique = 1; //row 0 is unique
        for (int row = 1; row < nRows; row++) { //start at 1; compare to previous row
            //does it equal row above?
            boolean equal = true;
            for (int col = 0; col < nColumns; col++) {
                if (((PrimitiveArray)table.get(col)).compare(row - 1, row) != 0) {
                    equal = false;
                    break;
                }
            }
            if (!equal) {
                //no? copy row 'row' to row 'nUnique'
                if (row != nUnique) 
                    for (int col = 0; col < nColumns; col++) 
                        ((PrimitiveArray)table.get(col)).copy(row, nUnique);
                nUnique++;
            }
        }

        //remove the stuff at the end
        for (int col = 0; col < nColumns; col++) 
            ((PrimitiveArray)table.get(col)).removeRange(nUnique, nRows);

        return nRows - nUnique;
    }

    /**
     * Given another PrimitiveArray List sorted (this and other must be sort(), not sortIgnoreCase),
     * this just keeps the values which are in both PAs (the intersection(union) of the two PAs).
     * <br>Like Java Collection.retainAll().
     * 
     * @param pa2 the other PrimitiveArray. It may be a different type.
     *   But this doesn't work quite right if one is a StringArray and the other isn't,
     *   and there are missing values in both.
     */
    public void inCommon(PrimitiveArray pa2) {
        int size1 = size;
        int size2 = pa2.size();
        int po1 = 0, po2 = 0;
        BitSet keep1 = new BitSet(size1);
        keep1.set(0, size1, false);
        boolean isNumeric1 = !(this instanceof StringArray);
        boolean isNumeric2 = !(pa2  instanceof StringArray);
        while (po1 < size1 && po2 < size2) {
            String s1 =     getString(po1);  
            String s2 = pa2.getString(po2);  
            if (isNumeric1) {
                if (s1.length() == 0) 
                    s1 = "NaN"; //to reflect that NaNs sort high in numeric PAs
            } else {
                if (s1 == null) {
                    if (s2 == null) {
                        keep1.set(po1);
                        po1++;
                        po2++;
                        continue;
                    } else {
                        po1++; continue;
                    }
                }
            }
            if (isNumeric2) {
                if (s2.length() == 0) s2 = "NaN"; //to reflect that NaNs sort high in numeric PAs
            } else {
                if (s2 == null) {
                    po2++; continue;
                }
            }

            int compare = s1.compareTo(s2);
            if      (compare < 0) po1++;
            else if (compare > 0) po2++;
            else {
                keep1.set(po1);
                po1++;
                po2++;
            }
        }
        justKeep(keep1);
    }


    /**
     * This removes rows in which the value in 'column' are less than
     * the value in the previous row.
     * Rows with values of NaN or bigger than 1e300 are also removed.
     * !!!Trouble: one erroneous big value will cause all subsequent valid values to be tossed.
     *
     * @param table a List of PrimitiveArray
     * @param column the column which should be ascending
     * @return the number of rows removed
     */
    public static int ensureAscending(List table, int column) {

        PrimitiveArray columnPA = (PrimitiveArray)table.get(column);
        int nRows = columnPA.size();
        int nColumns = table.size();
        int nGood = 0; 
        double lastGood = -Double.MAX_VALUE;
        for (int row = 0; row < nRows; row++) { 
            //is this a good row?
            double d = columnPA.getDouble(row);
            if (Math2.isFinite(d) && d < 1e300 && d >= lastGood) {
                //copy row 'row' to row 'nGood'
                if (row != nGood) 
                    for (int col = 0; col < nColumns; col++) 
                        ((PrimitiveArray)table.get(col)).copy(row, nGood);
                nGood++;
                lastGood = d;
            } else {
            }
        }

        //remove the stuff at the end
        for (int col = 0; col < nColumns; col++) 
            ((PrimitiveArray)table.get(col)).removeRange(nGood, nRows);

        int nRemoved = nRows - nGood;
        if (nRemoved > 0) 
            String2.log("PrimitveArray.ensureAscending nRowsRemoved=" + 
                nRemoved + " lastGood=" + lastGood);

        return nRemoved;
    }

    /**
     * Given two PrimitivesArray[]'s (representing two tables of data
     * with the same number columns), 
     * this appends table2 to the end of table1.
     *
     * <p>The columns types may be different. If table2's column is narrower,
     * the data is simply appended to table1. If table2's column is wider,
     * a new wider table1 is made before appending table2's data.
     * table1 and its columns will be affected by this method; table1 may
     * contain different PrimitiveArrays at the end.
     * table2 and its columns will not be changed by this method.
     *
     * @param table1 a List of PrimitiveArrays; it will contain the resulting
     *    table, perhaps containing some different PrimitiveArrays,
     *    always containing all the data from table1 and table2.
     * @param table2 a List of PrimitiveArrays
     */
    public static void append(List table1, List table2) {

        if (table1.size() != table2.size())
            throw new RuntimeException(MessageFormat.format(ArrayAppendTables,
                "" + table1.size(), "" + table2.size()));

        //append table2 to the end of table1
        for (int col = 0; col < table1.size(); col++) {
            //if needed, make a new wider PrimitiveArray in table1
            if (((PrimitiveArray)table2.get(col)).elementClassIndex() > 
                ((PrimitiveArray)table1.get(col)).elementClassIndex()) {
                PrimitiveArray oldTable1Col = ((PrimitiveArray)table1.get(col));
                PrimitiveArray newTable1Col = factory(((PrimitiveArray)table2.get(col)).elementClass(), 
                    oldTable1Col.size() + ((PrimitiveArray)table2.get(col)).size(), false);
                newTable1Col.append(oldTable1Col);
                table1.set(col, newTable1Col);
            }

            //append the data from table2 to table1
            ((PrimitiveArray)table1.get(col)).append(((PrimitiveArray)table2.get(col)));
        }

    }

    /**
     * Given two PrimitivesArray[]'s (representing two tables of data
     * with the same number columns), 
     * this merges table2 and table1 according to the sort order
     * defined by keys and ascending.
     * The columns types may be different; if table2's column is wider,
     * its data will be narrowed when it is appended.
     * table2 will not be changed by this method.
     *
     * @param table1 a List of PrimitiveArrays; it will contain the resulting
     *    table, perhaps containing some different PrimitiveArrays,
     *    always containing all the data from table1 and table2.
     * @param table2 a List of PrimitiveArrays
     * @param keys an array of the key column numbers 
     *    (each is 0..nColumns-1, the first key is the most important)
     *    which are used to determine the sort order
     * @param ascending an array of booleans corresponding to the keys
     *    indicating if the arrays are to be sorted by a given key in 
     *    ascending or descending order.
     * @param removeDuplicates specifies if completely identical rows should
     *    be removed.
     */
    public static void merge(List table1, List table2,
        int[] keys, boolean ascending[], boolean removeDuplicates) {
        //the current approach is quick, fun, and easy, but uses lots of memory
        //future: if needed, this could be done in a more space-saving way:
        //   sort each table, then merge table2 into table1, 
        //   but would have to be careful to avoid slowness from inserting rows
        //   of table2 into table1, or space lost to copying to a 3rd table.

        append(table1, table2);//this is fast (although uses lots of memory)
        sort(table1, keys, ascending);  //this is fast 
        if (removeDuplicates)
            removeDuplicates(table1);   //this is fast
    }


    /**
     * For all values, this multiplies by scale and then adds addOffset.
     * Calculations are done as doubles then, if necessary, rounded and stored.
     *
     * @param scale
     * @param addOffset
     */
    public void scaleAddOffset(double scale, double addOffset) {
        if (scale == 1 && addOffset == 0)
            return;
        for (int i = 0; i < size; i++)
            setDouble(i, getDouble(i) * scale + addOffset); //NaNs remain NaNs
    }
     
    /**
     * For all values, this adds addOffset then multiplies by scale.
     * Calculations are done as doubles then, if necessary, rounded and stored.
     *
     * @param scale
     * @param addOffset
     */
    public void addOffsetScale(double addOffset, double scale) {
        if (scale == 1 && addOffset == 0)
            return;
        for (int i = 0; i < size; i++)
            setDouble(i, (getDouble(i) + addOffset) * scale); //NaNs remain NaNs
    }
     
    /**
     * This returns a new (always) PrimitiveArray of type elementClass
     * which has had the scale and addOffset values applied.
     * Calculations are done as doubles then, if necessary, rounded and stored.
     *
     * @param elementClass 
     * @param scale
     * @param addOffset
     * @return a new (always) PrimitiveArray
     */
    public PrimitiveArray scaleAddOffset(Class elementClass, double scale, double addOffset) {
        PrimitiveArray pa = factory(elementClass, size, true);
        for (int i = 0; i < size; i++)
            pa.setDouble(i, getDouble(i) * scale + addOffset); //NaNs remain NaNs
        return pa;
    }
     
    /**
     * This returns a new (always) PrimitiveArray of type elementClass
     * which has had the addOffset and scale values applied.
     * Calculations are done as doubles then, if necessary, rounded and stored.
     *
     * @param elementClass 
     * @param addOffset
     * @param scale
     * @return a new (always) PrimitiveArray
     */
    public PrimitiveArray addOffsetScale(Class elementClass, double addOffset, double scale) {
        PrimitiveArray pa = factory(elementClass, size, true);
        for (int i = 0; i < size; i++)
            pa.setDouble(i, (getDouble(i) + addOffset) * scale); //NaNs remain NaNs
        return pa;
    }
     
    /**
     * This populates 'indices' with the indices (ranks) of the values in this PrimitiveArray
     * (ties get the same index). For example, 10,10,25,3 returns 1,1,2,0.
     *
     * @param indices the intArray that will capture the indices of the values 
     *  (ties get the same index). For example, 10,10,25,3 returns 1,1,2,0.
     * @return a PrimitveArray (the same type as this class) with the unique values, sorted
     */
    public abstract PrimitiveArray makeIndices(IntArray indices);

    /**
     * This changes all instances of the first value to the second value.
     * Note that, e.g., ByteArray.switchFromTo("127", "") will correctly
     * detect that 127=127 and do nothing.
     *
     * @param from the original value (use "" for standard missingValue)
     * @param to   the new value (use "" for standard missingValue)
     * @return the number of values switched
     */
    public abstract int switchFromTo(String from, String to);

    /**
     * For non-StringArray, 
     * if the primitiveArray has fake _FillValue and/or missing_values (e.g., -9999999),
     * those values are converted to PrimitiveArray-style missing values 
     * (NaN, or MAX_VALUE for integer types).
     *
     * @param fakeFillValue (e.g., -9999999) from colAttributes.getDouble("_FillValue"); use NaN if none
     * @param fakeMissingValue (e.g., -9999999) from colAttributes.getDouble("missing_value"); use NaN if none
     * @return the number of missing values converted
     */
    public int convertToStandardMissingValues(double fakeFillValue, double fakeMissingValue) {
        //do nothing to String columns
        if (elementClass() == String.class)
            return 0;

        //is _FillValue used?    switch data to standard mv
        //String2.log("Table.convertToStandardMissingValues col=" + column + " fillValue=" + fillValue);
        int nSwitched = 0;
        if (!Double.isNaN(fakeFillValue))
            nSwitched += switchFromTo("" + fakeFillValue, "");

        //is missing_value used?    switch data to standard mv
        //String2.log("Table.convertToStandardMissingValues col=" + column + " missingValue=" + missingValue);
        if (!Double.isNaN(fakeMissingValue) && fakeMissingValue != fakeFillValue) //if fakeFillValue==NaN   2nd clause always true (good)
            nSwitched += switchFromTo("" + fakeMissingValue, "");

        return nSwitched;
    }

    /**
     * For any non-StringArray, this changes all standard 
     * missing values (MAX_VALUE or NaN's) to fakeMissingValues.
     *
     * @param fakeMissingValue
     * @return the number of values switched
     */
    public int switchNaNToFakeMissingValue(double fakeMissingValue) {
        if (Math2.isFinite(fakeMissingValue) && elementClass() != String.class)
            return switchFromTo("", "" + fakeMissingValue);
        return 0;
    }

    /**
     * For FloatArray and DoubleArray, this changes all fakeMissingValues
     * to standard missing values (NaN's).
     *
     * @param fakeMissingValue
     * @return the number of missing values converted
     */
    public int switchFakeMissingValueToNaN(double fakeMissingValue) {
        if (Math2.isFinite(fakeMissingValue) &&
//???why just FloatArray and DoubleArray???
            (this instanceof FloatArray || this instanceof DoubleArray))
            return switchFromTo("" + fakeMissingValue, "");
        return 0;
    }


    /**
     * This tests if the values in the array are sorted in ascending order (ties are ok).
     * This details of this test are geared toward determining if the 
     * values are suitable for binarySearch.
     *
     * @return "" if the values in the array are sorted in ascending order;
     *   or an error message if not (i.e., if descending or unordered).
     *   If size is 0 or 1 (non-missing value), this returns "".
     */
    public abstract String isAscending();

    /**
     * This tests if the values in the array are sorted in descending order (ties are ok).
     *
     * @return "" if the values in the array are sorted in descending order;
     *   or an error message if not (i.e., if ascending or unordered).
     *   If size is 0 or 1 (non-missing value), this returns "".
     */
    public abstract String isDescending();

    /**
     * This tests for adjacent tied values and returns the index of the first tied value.
     * Adjacent NaNs are treated as ties.
     *
     * @return the index of the first tied value (or -1 if none).
     */
    public abstract int firstTie();

    /**
     * This compares this PrimitiveArray's values to anothers, string representation by string representation, 
     * and returns the first index where different.
     * this.get(i)=null and other.get(i)==null is treated as same value.
     *  
     * @param other
     * @return index where first different (or -1 if same).
     *    Note that the index may equal the size of this or the other primitiveArray.
     */
    public int diffIndex(PrimitiveArray other) {
        int i = 0;
        int otherSize = other.size();

        while (true) {
            if (i == size && size == otherSize)
                return -1;
            if (i == size ||
                i == otherSize) 
                return i;
            String s  = getString(i);
            String so = other.getString(i);
            if (s  == null && so != null) return i;
            if (so == null && s  != null) return i;
            if (s != null && so != null && !s.equals(so))
                return i;
            i++;
        }

        //you could do a double test if both pa's were numeric
        //but tests with inifinity and nan are awkward and time consuming
        //so string test is pretty good approach.
    }

    /**
     * This compares this PrimitiveArray's values to anothers, string representation by string representation, 
     * and returns a String indicating where different (or "" if not different).
     * this.get(i)=null and other.get(i)==null is treated as same value.
     *  
     * @param other (or old)
     * @return String indicating where different (or "" if not different).
     */
    public String diffString(PrimitiveArray other) {
        int diffi = diffIndex(other);
        if (diffi == -1)
            return "";
        String s1 = diffi == size? null : getString(diffi);
        String s2 = diffi == other.size()? null : other.getString(diffi);
        return MessageFormat.format("  " + ArrayDiffString, "" + diffi, s2, s1);
    }

    /**
     * This compares this PrimitiveArray's values to anothers, string representation by string representation, 
     * and throws Exception if different.
     * this.get(i)=null and other.get(i)==null is treated as same value.
     *  
     * @param other
     * @throws Exception if different
     */
    public void diff(PrimitiveArray other) throws Exception {
        int diffi = diffIndex(other);
        if (diffi == -1)
            return;
        String s1 = diffi == size? null : getString(diffi);
        String s2 = diffi == other.size()? null : other.getString(diffi);
        if (!Test.equal(s1, s2))
            throw new RuntimeException(MessageFormat.format(ArrayDiff, "" + diffi, s1, s2));
    }

    /**
     * This tests if the values in the array are evenly spaced (ascending or descending).
     *
     * @return "" if the values in the array are evenly spaced;
     *   or an error message if not.
     *   If size is 0 or 1, this returns "".
     */
    public String isEvenlySpaced() {
        //This version works for all integer-based values.
        //StringArray, FloatArray and DoubleArray overwrite this.
        if (size <= 2)
            return "";
        //average is closer to exact than first diff
        //and usually detects not-evenly-spaced anywhere in the array on first test!
        double average = (getDouble(size - 1) - getDouble(0)) / (size - 1.0); 
        for (int i = 1; i < size; i++) {
            if (getDouble(i) - getDouble(i - 1) != average) {  //integer types must be exact
                return MessageFormat.format(ArrayNotEvenlySpaced, getClass().getSimpleName(),
                    "" + (i - 1), "" + getDouble(i - 1), "" + i, "" + getDouble(i),
                    "" + (getDouble(i) - getDouble(i-1)), "" + average);
            }
        }
        return "";
    }

    /**
     * This returns a string indicating the smallest and largest actual spacing 
     * (not absolute values) between two adjacent values.
     * If there are ties, this returns the first one found.
     * If size &lt;= 2, this returns "".
     *
     * @return a string indicating the smallest and largest actual spacing
     * (not absolute values) between two adjacent values.
     */
    public String smallestBiggestSpacing() {
        if (size <= 2)
            return "";
        int smalli = 1, bigi = 1;
        double small = getDouble(1) - getDouble(0); 
        double big = small;
        for (int i = 2; i < size; i++) {
            double diff = getDouble(i) - getDouble(i-1); 
            if        (diff < small) {smalli = i; small = diff; 
            } else if (diff > big)   {bigi = i;   big = diff; 
            }
        }
        return "  smallest spacing=" + small + 
             ": [" + (smalli-1) + "]=" + getDouble(smalli-1) + 
             ", [" + smalli     + "]=" + getDouble(smalli) + "\n" +
               "  biggest  spacing=" + big   + 
             ": [" + (bigi-1)   + "]=" + getDouble(bigi-1) + 
             ", [" + bigi       + "]=" + getDouble(bigi);
    }

    /** This returns the minimum value that can be held by this class. */
    public abstract String minValue();

    /** This returns the maximum value that can be held by this class. */
    public abstract String maxValue();

    /**
     * This finds the number of non-missing values, and the index of the min and
     *    max value.
     *
     * @return int[3], [0]=the number of non-missing values, 
     *    [1]=index of min value (if tie, index of last found; -1 if all mv),
     *    [2]=index of max value (if tie, index of last found; -1 if all mv).
     */
    public abstract int[] getNMinMaxIndex();

    /**
     * Given nHave values and stride, this returns the actual number of points that will be found.
     *
     * @param nHave the size of the array (or  end-start+1)
     * @param stride  (must be >= 1)
     * @return the actual number of points that will be found.
     */
    public static int strideWillFind(int nHave, int stride) {
        return 1 + (nHave - 1) / stride;
    }

    /**
     * This makes a new subset of this PrimitiveArray based on startIndex, stride,
     * and stopIndex.
     *
     * @param startIndex must be a valid index
     * @param stride   must be at least 1
     * @param stopIndex (inclusive) If &gt;= size, it will be changed to size-1.
     * @return a new PrimitiveArray with the desired subset.
     *    It will have a new backing array with a capacity equal to its size.
     *    If stopIndex &lt; startIndex, this returns PrimitiveArray with size=0;
     */
    public abstract PrimitiveArray subset(int startIndex, int stride, int stopIndex);

    /**
     * This tests if 'value1 op value2' is true.
     * The =~ regex test must be tested with String testValueOpValue, not here,
     *   because value2 is a regex (not a double).
     * 
     * @param value1  Long.MAX_VALUE is treated as NaN
     * @param op one of EDDTable.OPERATORS
     * @param value2
     * @return true if 'value1 op value2' is true.
     *    <br>Tests of "NaN = NaN" will evaluate to true.
     *    <br>Tests of "nonNaN != NaN" will evaluate to true.
     *    <br>All other tests where value1 is NaN or value2 is NaN will evaluate to false.
     * @throws RuntimeException if trouble (e.g., invalid op)
     */
     public static boolean testValueOpValue(long value1, String op, long value2) {
         //String2.log("testValueOpValue (long): " + value1 + op + value2);
         if (op.equals("="))  return value1 == value2;
         if (op.equals("!=")) return value1 != value2;

         if (value1 == Long.MAX_VALUE || value2 == Long.MAX_VALUE)
             return false;
         if (op.equals("<=")) return value1 <= value2;
         if (op.equals(">=")) return value1 >= value2;
         if (op.equals("<"))  return value1 <  value2;
         if (op.equals(">"))  return value1 >  value2;

         //Regex test has to be handled via String testValueOpValue 
         //  if (op.equals(PrimitiveArray.REGEX_OP))  
         throw new SimpleException("Query error: " +
             "Unknown operator=\"" + op + "\".");
     }

    /**
     * This tests if 'value1 op value2' is true.
     * The &lt;=, &gt;=, and = tests are (partly) done with Math2.almostEqual9
     *   so there is a little fudge factor.
     * The =~ regex test must be tested with String testValueOpValue, not here,
     *   because value2 is a regex (not a double).
     * 
     * @param value1
     * @param op one of EDDTable.OPERATORS
     * @param value2
     * @return true if 'value1 op value2' is true.
     *    <br>Tests of "NaN = NaN" will evaluate to true.
     *    <br>Tests of "nonNaN != NaN" will evaluate to true.
     *    <br>All other tests where value1 is NaN or value2 is NaN will evaluate to false.
     * @throws RuntimeException if trouble (e.g., invalid op)
     */
     public static boolean testValueOpValue(float value1, String op, float value2) {
         //String2.log("testValueOpValue (float): " + value1 + op + value2);
         if (op.equals("<=")) return value1 <= value2 || Math2.almostEqual(6, value1, value2);
         if (op.equals(">=")) return value1 >= value2 || Math2.almostEqual(6, value1, value2);
         if (op.equals("="))  return (Float.isNaN(value1) && Float.isNaN(value2)) ||
                                     Math2.almostEqual(6, value1, value2);
         if (op.equals("<"))  return value1 < value2;
         if (op.equals(">"))  return value1 > value2;
         if (op.equals("!=")) return Float.isNaN(value1) && Float.isNaN(value2)? false :
                                         value1 != value2;
         //Regex test has to be handled via String testValueOpValue 
         //  if (op.equals(PrimitiveArray.REGEX_OP))  
         throw new SimpleException("Query error: " +
             "Unknown operator=\"" + op + "\".");
     }

    /**
     * This tests if 'value1 op value2' is true.
     * The &lt;=, &gt;=, and = tests are (partly) done with Math2.almostEqual9
     *   so there is a little fudge factor.
     * The =~ regex test must be tested with String testValueOpValue, not here,
     *   because value2 is a regex (not a double).
     * 
     * @param value1
     * @param op one of EDDTable.OPERATORS
     * @param value2
     * @return true if 'value1 op value2' is true.
     *    <br>Tests of "NaN = NaN" will evaluate to true. 
     *    <br>Tests of "nonNaN != NaN" will evaluate to true.
     *    <br>All other tests where value1 is NaN or value2 is NaN will evaluate to false.
     * @throws RuntimeException if trouble (e.g., invalid op)
     */
     public static boolean testValueOpValue(double value1, String op, double value2) {
         //String2.log("testValueOpValue (double): " + value1 + op + value2);
         //if (Double.isNaN(value2) && Double.isNaN(value1)) { //test2 first, less likely to be NaN
         //    return (op.equals("=") || op.equals("<=") || op.equals(">=")); //the '=' matters 
         //}
         if (op.equals("<=")) return value1 <= value2 || Math2.almostEqual(9, value1, value2);
         if (op.equals(">=")) return value1 >= value2 || Math2.almostEqual(9, value1, value2);
         if (op.equals("="))  return (Double.isNaN(value1) && Double.isNaN(value2)) ||
                                     Math2.almostEqual(9, value1, value2);
         if (op.equals("<"))  return value1 < value2;
         if (op.equals(">"))  return value1 > value2;
         if (op.equals("!=")) return Double.isNaN(value1) && Double.isNaN(value2)? false : 
                                     value1 != value2;
         //Regex test has to be handled via String testValueOpValue 
         //  if (op.equals(PrimitiveArray.REGEX_OP))  
         throw new SimpleException("Query error: " +
             "Unknown operator=\"" + op + "\".");
     }

    /**
     * This tests if 'value1 op value2' is true.
     * The ops containing with &lt; and &gt; compare value1.toLowerCase()
     * and value2.toLowerCase().
     *
     * <p>Note that "" is not treated specially.  "" isn't like NaN.  
     * <br>testValueOpValue("a" &gt; "")  will return true.
     * <br>testValueOpValue("a" &lt; "")  will return false.
     * <br>testValueOpValue(""  &lt; "a") will return true.
     * <br>testValueOpValue(""  &gt; "a") will return false.
     * <br>testValueOpValue(""  =    "")  will return true.
     * <br>Users should add another constraint (&amp;col2!="") if they don't want "" values.
     * <br>[I might like it to parallel NaN, but that would be just me --
     * <br>it would defy common tests in all computer languages. 
     * <br>And ERDDAP doesn't support null (hard to represent in many file types).]
     * <br>Stated another way, "" (as value1 or value2) behaves almost like char#0.
     * 
     * @param value1   (shouldn't be null)
     * @param op one of EDDTable.OPERATORS
     * @param value2   (shouldn't be null)
     * @return true if 'value1 op value2' is true.
     * @throws RuntimeException if trouble (e.g., invalid op)
     */
     public static boolean testValueOpValue(String value1, String op, String value2) {
         //String2.log("testValueOpValue (String): " + value1 + op + value2);
         if (op.equals("="))  return value1.equals(value2);
         if (op.equals("!=")) return !value1.equals(value2);
         if (op.equals(REGEX_OP)) return value1.matches(value2);  //regex test

         int t = value1.toLowerCase().compareTo(value2.toLowerCase());
         if (op.equals("<=")) return t <= 0;  
         if (op.equals(">=")) return t >= 0;
         if (op.equals("<"))  return t < 0;
         if (op.equals(">"))  return t > 0;
         throw new SimpleException("Query error: " +
             "Unknown operator=\"" + op + "\".");
     }

    /**
     * This tests the keep=true elements to see if 'get(element) op value2' is true.
     *   If the test is false, the keep element is set to false.
     * <br>For float and double tests, the &lt;=, &gt;=, and = tests are (partly) 
     *   done with Math2.almostEqual(6) and (9), so there is a little fudge factor.
     * <br>The =~ regex test is tested with String testValueOpValue,
     *   because value2 is a regex (not a numeric type).
     *
     * <p>For integer-type PrimitiveArrays, MAX_VALUE is treated as a NaN.
     * <br>Tests of "NaN = NaN" will evaluate to true.
     * <br>Tests of "nonNaN != NaN" will evaluate to true.
     * <br>All other tests where value1 is NaN or value2 is NaN will evaluate to false.
     * 
     * @param keep   The test is only applied to keep=true elements.
     *   If the test is false, the keep element is set to false.
     * @param op one of EDDTable.OPERATORS
     * @param value2
     * @return nStillGood
     * @throws RuntimeException if trouble (e.g., invalid op or invalid keep element)
     */
    public int applyConstraint(BitSet keep, String op, String value2) {

        //regex
        if (op.equals(REGEX_OP)) {
            //String2.log("applyConstraint(regex)");
            int nStillGood = 0;
            Pattern p = Pattern.compile(value2);  //big time savings
            for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
                if (p.matcher(getString(row)).matches()) 
                    nStillGood++;
                else keep.clear(row);
            }
            return nStillGood;
        }

        //string
        if (elementClass() == String.class) {
            //String2.log("applyConstraint(String)");
            int nStillGood = 0;
            for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
                if (testValueOpValue(getString(row), op, value2)) 
                    nStillGood++;
                else keep.clear(row);
            }
            return nStillGood;
        }

        //int types
        long value2l = String2.parseLong(value2);
        if (value2l != Long.MAX_VALUE &&   //value2 parsed cleanly as a long
            isIntegerType(elementClass())) {
            //String2.log("applyConstraint(long)");
            int nStillGood = 0;
            for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
                if (testValueOpValue(getLong(row), op, value2l)) 
                    nStillGood++;
                else keep.clear(row);
            }
            return nStillGood;
        }

        //float
        if (elementClass() == float.class) {
            //String2.log("applyConstraint(float)");
            int nStillGood = 0;
            float value2f = String2.parseFloat(value2);
            for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
                if (testValueOpValue(getFloat(row), op, value2f)) 
                    nStillGood++;
                else keep.clear(row);
            }
            return nStillGood;
        }

        //treat everything else via double tests (that should be all that is left)
        //String2.log("applyConstraint(double)");
        int nStillGood = 0;
        double value2d = String2.parseDouble(value2);
        for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
            if (testValueOpValue(getDouble(row), op, value2d)) 
                nStillGood++;
            else keep.clear(row);
        }
        return nStillGood;
    }



    /**
     * This tests the methods of this class.
     *
     * @throws Exception if trouble.
     */
    public static void testBasic() throws Throwable {
        String2.log("*** PrimitiveArray.testBasic");


        //test factory 
        PrimitiveArray pa;
        Test.ensureEqual(factory(new byte[]{1}).elementClass(), byte.class, "");
        Test.ensureEqual(factory(new char[]{1}).elementClass(), char.class, "");
        Test.ensureEqual(factory(new short[]{1}).elementClass(), short.class, "");
        Test.ensureEqual(factory(new int[]{1}).elementClass(), int.class, "");
        Test.ensureEqual(factory(new long[]{1}).elementClass(), long.class, "");
        Test.ensureEqual(factory(new float[]{1}).elementClass(), float.class, "");
        Test.ensureEqual(factory(new double[]{1}).elementClass(), double.class, "");
        Test.ensureEqual(factory(new String[]{"1"}).elementClass(), String.class, "");

        Test.ensureEqual(factory(byte.class, 1, true).elementClass(), byte.class, "");
        Test.ensureEqual(factory(char.class, 1, true).elementClass(), char.class, "");
        Test.ensureEqual(factory(short.class, 1, true).elementClass(), short.class, "");
        Test.ensureEqual(factory(int.class, 1, true).elementClass(), int.class, "");
        Test.ensureEqual(factory(long.class, 1, true).elementClass(), long.class, "");
        Test.ensureEqual(factory(float.class, 1, true).elementClass(), float.class, "");
        pa = factory(double.class, 1, true);
        Test.ensureEqual(pa.elementClass(), double.class, "");
        Test.ensureEqual(pa.getDouble(0), 0, "");
        pa = factory(String.class, 1, true);
        Test.ensureEqual(pa.elementClass(), String.class, "");
        Test.ensureEqual(pa.getString(0), "", "");

        Test.ensureEqual(factory(byte.class,   1, "10").toString(), "10", "");
        Test.ensureEqual(factory(char.class,   2, "abc").toString(), "97, 97", "");
        Test.ensureEqual(factory(short.class,  3, "30").toString(), "30, 30, 30", "");
        Test.ensureEqual(factory(int.class,    4, "40").toString(), "40, 40, 40, 40", "");
        Test.ensureEqual(factory(long.class,   5, "50").toString(), "50, 50, 50, 50, 50", "");
        Test.ensureEqual(factory(float.class,  6, "60").toString(), "60.0, 60.0, 60.0, 60.0, 60.0, 60.0", "");
        Test.ensureEqual(factory(double.class, 7, "70").toString(), "70.0, 70.0, 70.0, 70.0, 70.0, 70.0, 70.0", "");
        Test.ensureEqual(factory(String.class, 8, "ab").toString(), "ab, ab, ab, ab, ab, ab, ab, ab", "");

        //test simplify
        pa = new StringArray(new String[]{"-127", "126", ".", "NaN", null});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof ByteArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getDouble(0), -127, "");
        Test.ensureEqual(pa.getDouble(1), 126, "");
        Test.ensureEqual(pa.getDouble(2), Double.NaN, "");
        Test.ensureEqual(pa.getDouble(3), Double.NaN, "");
        Test.ensureEqual(pa.getDouble(4), Double.NaN, "");

        //pa = new StringArray(new String[]{"0", "65534", "."});
        //pa = pa.simplify();
        //Test.ensureTrue(pa instanceof CharArray, "elementClass=" + pa.elementClass());
        //Test.ensureEqual(pa.getDouble(0), 0, "");
        //Test.ensureEqual(pa.getDouble(1), 65534, "");
        //Test.ensureEqual(pa.getDouble(2), Character.MAX_VALUE, "");

        pa = new StringArray(new String[]{"-32767", "32766", "."});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof ShortArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getDouble(0), -32767, "");
        Test.ensureEqual(pa.getDouble(1), 32766, "");
        Test.ensureEqual(pa.getDouble(2), Double.NaN, "");

        pa = new StringArray(new String[]{"-2000000000", "2000000000", "."});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof IntArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getDouble(0), -2000000000, "");
        Test.ensureEqual(pa.getDouble(1), 2000000000, "");
        Test.ensureEqual(pa.getDouble(2), Double.NaN, "");

        pa = new StringArray(new String[]{"-2000000000000000", "2000000000000000", ""});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof StringArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getString(0), "-2000000000000000", "");
        Test.ensureEqual(pa.getString(1), "2000000000000000", "");
        Test.ensureEqual(pa.getString(2), "", "");

        pa = new StringArray(new String[]{"-1e33", "1e33", "."});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof FloatArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getDouble(0), -1e33f, ""); //'f' bruises it
        Test.ensureEqual(pa.getDouble(1), 1e33f, "");  //'f' bruises it
        Test.ensureEqual(pa.getDouble(2), Double.NaN, "");

        pa = new StringArray(new String[]{"-1e307", "1e307", "."});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof DoubleArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getDouble(0), -1e307, "");
        Test.ensureEqual(pa.getDouble(1), 1e307, "");
        Test.ensureEqual(pa.getDouble(2), Double.NaN, "");

        pa = new StringArray(new String[]{".", "123", "4b"});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof StringArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getString(0), ".", "");
        Test.ensureEqual(pa.getString(1), "123", "");
        Test.ensureEqual(pa.getString(2), "4b", "");

        pa = new StringArray(new String[]{".", "33.0"}); //with internal "." -> float
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof FloatArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getString(0), "", "");
        Test.ensureEqual(pa.getString(1), "33.0", "");

        pa = new StringArray(new String[]{".", "33"});  //no internal ".", can be integer type
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof ByteArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getString(0), "", "");
        Test.ensureEqual(pa.getString(1), "33", "");

        pa = new DoubleArray(new double[]{Double.NaN, 123.4, 12});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof FloatArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getFloat(0), Float.NaN, "");
        Test.ensureEqual(pa.getFloat(1), 123.4f, "");
        Test.ensureEqual(pa.getFloat(2), 12f, "");

        pa = new DoubleArray(new double[]{Double.NaN, 100000, 12});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof IntArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getInt(0), Integer.MAX_VALUE, "");
        Test.ensureEqual(pa.getInt(1), 100000, "");
        Test.ensureEqual(pa.getInt(2), 12, "");

        pa = new DoubleArray(new double[]{Double.NaN, 100, 12});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof ByteArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getInt(0), Integer.MAX_VALUE, "");
        Test.ensureEqual(pa.getInt(1), 100, "");
        Test.ensureEqual(pa.getInt(2), 12, "");

        pa = new IntArray(new int[]{Integer.MAX_VALUE, 100, 12});
        pa = pa.simplify();
        Test.ensureTrue(pa instanceof ByteArray, "elementClass=" + pa.elementClass());
        Test.ensureEqual(pa.getInt(0), Integer.MAX_VALUE, "");
        Test.ensureEqual(pa.getInt(1), 100, "");
        Test.ensureEqual(pa.getInt(2), 12, "");

        //test rank
        ByteArray arByte     = new ByteArray(   new byte[]  {0, 100, 50, 110});
        FloatArray arFloat   = new FloatArray(  new float[] {1, 3, 3, -5});
        DoubleArray arDouble = new DoubleArray( new double[]{17, 1e300, 3, 0});
        StringArray arString = new StringArray( new String[]{"a", "abe", "A", "ABE"});
        ArrayList table = String2.toArrayList(new Object[]{arByte, arFloat, arDouble, arString});
        Test.ensureEqual(rank(table, new int[]{0}, new boolean[]{true}), //ascending
            new int[]{0, 2, 1, 3}, "");
        Test.ensureEqual(rank(table, new int[]{0}, new boolean[]{false}), //descending
            new int[]{3, 1, 2, 0}, "");
        Test.ensureEqual(rank(table, new int[]{1}, new boolean[]{true}), //ties
            new int[]{3, 0, 1, 2}, "");
        Test.ensureEqual(rank(table, new int[]{2}, new boolean[]{true}),
            new int[]{3, 2, 0, 1}, "");
        Test.ensureEqual(rank(table, new int[]{3}, new boolean[]{true}),
            new int[]{2, 3, 0, 1}, "");
        Test.ensureEqual(rank(table, new int[]{1, 0}, new boolean[]{true, true}), //tie, a/ascending
            new int[]{3, 0, 2, 1}, "");
        Test.ensureEqual(rank(table, new int[]{1, 0}, new boolean[]{true, false}), //tie, a/descending
            new int[]{3, 0, 1, 2}, "");
        Test.ensureEqual(arByte.elementClass(), byte.class, "");
        Test.ensureEqual(arFloat.elementClass(), float.class, "");
        Test.ensureEqual(arDouble.elementClass(), double.class, "");
        Test.ensureEqual(arString.elementClass(), String.class, "");

        Test.ensureEqual(arByte.elementClassString(), "byte", "");
        Test.ensureEqual(arFloat.elementClassString(), "float", "");
        Test.ensureEqual(arDouble.elementClassString(), "double", "");
        Test.ensureEqual(arString.elementClassString(), "String", "");

        //test sort  result = {3, 0, 2, 1});
        sort(table, new int[]{1, 0}, new boolean[]{true, true}); //tie, a/ascending
        Test.ensureEqual(arByte.array,   new byte[]{  110,   0,   50,  100},   ""); //col0
        Test.ensureEqual(arFloat.array,  new float[]{ -5,    1,   3,   3},     ""); //col1
        Test.ensureEqual(arDouble.array, new double[]{0,     17,  3,   1e300}, ""); //col2
        Test.ensureEqual(arString.array, new String[]{"ABE", "a", "A", "abe"}, ""); //col3

        //test sort  result = {3, 0, 1, 2});
        sort(table, new int[]{1, 0}, new boolean[]{true, false}); //tie, a/descending
        Test.ensureEqual(arByte.array,   new byte[]{  110,   0,   100,   50},  "");
        Test.ensureEqual(arFloat.array,  new float[]{ -5,    1,   3,     3},   "");
        Test.ensureEqual(arDouble.array, new double[]{0,     17,  1e300, 3},   "");
        Test.ensureEqual(arString.array, new String[]{"ABE", "a", "abe", "A"}, "");

        //test sortIgnoreCase  
        sortIgnoreCase(table, new int[]{3}, new boolean[]{true}); 
        Test.ensureEqual(arString.array, new String[]{"A", "a", "ABE", "abe"}, "arString=" + arString);  //col3
        Test.ensureEqual(arByte.array,   new byte[]{  50,  0,   110,    100},   ""); //col0
        Test.ensureEqual(arFloat.array,  new float[]{  3,  1,   -5,     3},     ""); //col1
        Test.ensureEqual(arDouble.array, new double[]{ 3,  17,  0,    1e300}, "");   //col2

        //test sortIgnoreCase  
        sortIgnoreCase(table, new int[]{3}, new boolean[]{false}); 
        Test.ensureEqual(arString.array, new String[]{"abe", "ABE", "a", "A"}, "");  //col3
        Test.ensureEqual(arByte.array,   new byte[]{  100,    110,   0,  50},   ""); //col0
        Test.ensureEqual(arFloat.array,  new float[]{   3,    -5,    1,   3},   ""); //col1
        Test.ensureEqual(arDouble.array, new double[]{1e300,   0,   17,   3},   ""); //col2

        //test removeDuplicates
        IntArray arInt3a = new IntArray(new int[]{1,5,5,7,7,7});
        IntArray arInt3b = new IntArray(new int[]{2,6,6,8,8,8});
        ArrayList table3 = String2.toArrayList(new Object[]{arInt3a, arInt3b});
        removeDuplicates(table3);
        Test.ensureEqual(arInt3a.toString(), "1, 5, 7", "");
        Test.ensureEqual(arInt3b.toString(), "2, 6, 8", "");

        //test merge  (which tests append and sort)
        ByteArray arByte2     = new ByteArray(   new byte[]  {5,   15,  50,   25});
        FloatArray arFloat2   = new FloatArray(  new float[] {4,   14,   3,   24});
        IntArray arInt2       = new IntArray(    new int[]{   3,   13,   3,   1}); //test: narrower than arDouble
        StringArray arString2 = new StringArray( new String[]{"b", "aa", "A", "c"});
        ArrayList table2 = String2.toArrayList(new Object[]{arByte2, arFloat2, arInt2, arString2});
        merge(table2, table, new int[]{1, 0}, new boolean[]{true, true}, false);
        Test.ensureEqual(((PrimitiveArray)table2.get(0)).toDoubleArray(), new double[]{110,   0,   50,  50,100,    5,  15,  25},   "");
        Test.ensureEqual(((PrimitiveArray)table2.get(1)).toDoubleArray(), new double[]{-5,    1,   3,   3,   3,    4,  14,  24},   "");
        Test.ensureEqual(((PrimitiveArray)table2.get(2)).toDoubleArray(), new double[]{0,     17,  3,   3, 1e300,  3,  13,  1},    "");
        Test.ensureEqual(((PrimitiveArray)table2.get(3)).toStringArray(), new String[]{"ABE", "a", "A", "A", "abe","b","aa","c"},  "");

        merge(table2, table, new int[]{1, 0}, new boolean[]{true, false}, true);
        Test.ensureEqual(((PrimitiveArray)table2.get(0)).toDoubleArray(), new double[]{110,   0,   100,  50,   5,   15,   25},   "");
        Test.ensureEqual(((PrimitiveArray)table2.get(1)).toDoubleArray(), new double[]{-5,    1,   3,    3,    4,   14,   24},   "");
        Test.ensureEqual(((PrimitiveArray)table2.get(2)).toDoubleArray(), new double[]{0,     17, 1e300, 3,    3,   13,   1},    "");
        Test.ensureEqual(((PrimitiveArray)table2.get(3)).toStringArray(), new String[]{"ABE", "a", "abe","A",  "b","aa","c"},    "");

        

        //** test speed
        int n = 10000000;
        long time1 = System.currentTimeMillis();
        int iar[] = new int[]{13,24,56};
        int sum = 0;
        for (int i = 0; i < n; i++)
            sum += iar[2];
        time1 = System.currentTimeMillis() - time1;

        long time2 = System.currentTimeMillis();
        IntArray ia = new IntArray(new int[]{13,24,56});
        sum = 0;
        for (int i = 0; i < n; i++)
            sum += ia.get(2);
        time2 = System.currentTimeMillis() - time2;

        String2.log("[] time=" + time1 + " IntArray time=" + time2);


        //** raf tests
        ByteArray   bar = new ByteArray(new byte[]{2,4,6,6,6,8});
        CharArray   car = new CharArray(new char[]{'\u0002','\u0004','\u0006','\u0006','\u0006','\u0008'});
        DoubleArray dar = new DoubleArray(new double[]{2,4,6,6,6,8});
        FloatArray  far = new FloatArray(new float[]{2,4,6,6,6,8});
        IntArray    Iar = new IntArray(new int[]{2,4,6,6,6,8});
        LongArray   lar = new LongArray(new long[]{2,4,6,6,6,8});
        ShortArray  sar = new ShortArray(new short[]{2,4,6,6,6,8});
        StringArray Sar = new StringArray(new String[]{"22","4444","666666","666666","666666","88888888"});

        Test.ensureEqual(bar.indexOf("6"), 2, "");
        Test.ensureEqual(car.indexOf("6"), 2, "");
        Test.ensureEqual(dar.indexOf("6"), 2, "");
        Test.ensureEqual(far.indexOf("6"), 2, "");
        Test.ensureEqual(Iar.indexOf("6"), 2, "");
        Test.ensureEqual(lar.indexOf("6"), 2, "");
        Test.ensureEqual(sar.indexOf("6"), 2, "");
        Test.ensureEqual(Sar.indexOf("666666"), 2, "");

        Test.ensureEqual(bar.indexOf("a"), -1, "");
        Test.ensureEqual(car.indexOf("a"), -1, "");
        Test.ensureEqual(dar.indexOf("a"), -1, "");
        Test.ensureEqual(far.indexOf("a"), -1, "");
        Test.ensureEqual(Iar.indexOf("a"), -1, "");
        Test.ensureEqual(lar.indexOf("a"), -1, "");
        Test.ensureEqual(sar.indexOf("a"), -1, "");
        Test.ensureEqual(Sar.indexOf("a"), -1, "");

        Test.ensureEqual(bar.indexOf("6", 3), 3, "");
        Test.ensureEqual(car.indexOf("6", 3), 3, "");
        Test.ensureEqual(dar.indexOf("6", 3), 3, "");
        Test.ensureEqual(far.indexOf("6", 3), 3, "");
        Test.ensureEqual(Iar.indexOf("6", 3), 3, "");
        Test.ensureEqual(lar.indexOf("6", 3), 3, "");
        Test.ensureEqual(sar.indexOf("6", 3), 3, "");
        Test.ensureEqual(Sar.indexOf("666666", 3), 3, "");

        Test.ensureEqual(bar.lastIndexOf("6"), 4, "");
        Test.ensureEqual(car.lastIndexOf("6"), 4, "");
        Test.ensureEqual(dar.lastIndexOf("6"), 4, "");
        Test.ensureEqual(far.lastIndexOf("6"), 4, "");
        Test.ensureEqual(Iar.lastIndexOf("6"), 4, "");
        Test.ensureEqual(lar.lastIndexOf("6"), 4, "");
        Test.ensureEqual(sar.lastIndexOf("6"), 4, "");
        Test.ensureEqual(Sar.lastIndexOf("666666"), 4, "");

        Test.ensureEqual(bar.lastIndexOf("a"), -1, "");
        Test.ensureEqual(car.lastIndexOf("a"), -1, "");
        Test.ensureEqual(dar.lastIndexOf("a"), -1, "");
        Test.ensureEqual(far.lastIndexOf("a"), -1, "");
        Test.ensureEqual(Iar.lastIndexOf("a"), -1, "");
        Test.ensureEqual(lar.lastIndexOf("a"), -1, "");
        Test.ensureEqual(sar.lastIndexOf("a"), -1, "");
        Test.ensureEqual(Sar.lastIndexOf("a"), -1, "");

        Test.ensureEqual(bar.lastIndexOf("6", 3), 3, "");
        Test.ensureEqual(car.lastIndexOf("6", 3), 3, "");
        Test.ensureEqual(dar.lastIndexOf("6", 3), 3, "");
        Test.ensureEqual(far.lastIndexOf("6", 3), 3, "");
        Test.ensureEqual(Iar.lastIndexOf("6", 3), 3, "");
        Test.ensureEqual(lar.lastIndexOf("6", 3), 3, "");
        Test.ensureEqual(sar.lastIndexOf("6", 3), 3, "");
        Test.ensureEqual(Sar.lastIndexOf("666666", 3), 3, "");

        //raf test2
        String raf2Name = File2.getSystemTempDirectory() + "PrimitiveArrayRaf2Test.bin";
        String2.log("raf2Name=" + raf2Name);
        File2.delete(raf2Name);
        Test.ensureEqual(File2.isFile(raf2Name), false, "");

        RandomAccessFile raf2 = new RandomAccessFile(raf2Name, "rw");
        long bStart = raf2.getFilePointer();
        rafWriteDouble(raf2, byte.class, 1.0);
        rafWriteDouble(raf2, byte.class, Double.NaN);
        long cStart = raf2.getFilePointer();
        rafWriteDouble(raf2, char.class, 2.0);
        rafWriteDouble(raf2, char.class, Double.NaN);
        long dStart = raf2.getFilePointer();
        rafWriteDouble(raf2, double.class, 3.0);
        rafWriteDouble(raf2, double.class, Double.NaN);
        long fStart = raf2.getFilePointer();
        rafWriteDouble(raf2, float.class, 4.0);
        rafWriteDouble(raf2, float.class, Double.NaN);
        long iStart = raf2.getFilePointer();
        rafWriteDouble(raf2, int.class, 5.0);
        rafWriteDouble(raf2, int.class, Double.NaN);
        long lStart = raf2.getFilePointer();
        rafWriteDouble(raf2, long.class, 6.0);
        rafWriteDouble(raf2, long.class, Double.NaN);
        long sStart = raf2.getFilePointer();
        rafWriteDouble(raf2, short.class, 7.0);
        rafWriteDouble(raf2, short.class, Double.NaN);
        //read in reverse order
        Test.ensureEqual(rafReadDouble(raf2, short.class,  sStart, 1), Double.NaN, "");
        Test.ensureEqual(rafReadDouble(raf2, short.class,  sStart, 0), 7.0, "");

        Test.ensureEqual(rafReadDouble(raf2, long.class,   lStart, 1), Double.NaN, "");
        Test.ensureEqual(rafReadDouble(raf2, long.class,   lStart, 0), 6.0, "");

        Test.ensureEqual(rafReadDouble(raf2, int.class,    iStart, 1), Double.NaN, "");
        Test.ensureEqual(rafReadDouble(raf2, int.class,    iStart, 0), 5.0, "");

        Test.ensureEqual(rafReadDouble(raf2, float.class,  fStart, 1), Double.NaN, "");
        Test.ensureEqual(rafReadDouble(raf2, float.class,  fStart, 0), 4.0, "");

        Test.ensureEqual(rafReadDouble(raf2, double.class, dStart, 1), Double.NaN, "");
        Test.ensureEqual(rafReadDouble(raf2, double.class, dStart, 0), 3.0, "");

        Test.ensureEqual(rafReadDouble(raf2, char.class,   cStart, 1), Double.NaN, "");
        Test.ensureEqual(rafReadDouble(raf2, char.class,   cStart, 0), 2.0, "");

        Test.ensureEqual(rafReadDouble(raf2, byte.class,   bStart, 1), Double.NaN, "");
        Test.ensureEqual(rafReadDouble(raf2, byte.class,   bStart, 0), 1.0, "");

        raf2.close();

        //raf test
        String rafName = File2.getSystemTempDirectory() + "PrimitiveArrayRafTest.bin";
        String2.log("rafName=" + rafName);
        File2.delete(rafName);
        Test.ensureEqual(File2.isFile(rafName), false, "");

        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
            new FileOutputStream(rafName)));
        long barStart = 0;
        long carStart = barStart + 6*bar.writeDos(dos);
        long darStart = carStart + 6*car.writeDos(dos);
        long farStart = darStart + 6*dar.writeDos(dos);
        long IarStart = farStart + 6*far.writeDos(dos);
        long larStart = IarStart + 6*Iar.writeDos(dos);
        long sarStart = larStart + 6*lar.writeDos(dos);
        long SarStart = sarStart + 6*sar.writeDos(dos);
        Test.ensureEqual(Sar.writeDos(dos), 10, "");
        int nBytesPerS = 9;
        //String2.log(File2.hexDump(dosName, 500));

        dos.close();

        //test rafReadDouble 
        RandomAccessFile raf = new RandomAccessFile(rafName, "rw");
        Test.ensureEqual(rafReadDouble(raf, byte.class,   barStart, 0), 2, "");
        Test.ensureEqual(rafReadDouble(raf, byte.class,   barStart, 5), 8, "");
        Test.ensureEqual(rafReadDouble(raf, char.class,   carStart, 0), 2, "");
        Test.ensureEqual(rafReadDouble(raf, char.class,   carStart, 5), 8, "");
        Test.ensureEqual(rafReadDouble(raf, double.class, darStart, 0), 2, "");
        Test.ensureEqual(rafReadDouble(raf, double.class, darStart, 5), 8, "");
        Test.ensureEqual(rafReadDouble(raf, float.class,  farStart, 0), 2, "");
        Test.ensureEqual(rafReadDouble(raf, float.class,  farStart, 5), 8, "");
        Test.ensureEqual(rafReadDouble(raf, int.class,    IarStart, 0), 2, "");
        Test.ensureEqual(rafReadDouble(raf, int.class,    IarStart, 5), 8, "");
        Test.ensureEqual(rafReadDouble(raf, long.class,   larStart, 0), 2, "");
        Test.ensureEqual(rafReadDouble(raf, long.class,   larStart, 5), 8, "");
        Test.ensureEqual(rafReadDouble(raf, short.class,  sarStart, 0), 2, "");
        Test.ensureEqual(rafReadDouble(raf, short.class,  sarStart, 5), 8, "");
        //Test.ensureEqual(StringArray.rafReadString(raf,   SarStart, 0, nBytesPerS), "22", "");
        //Test.ensureEqual(StringArray.rafReadString(raf,   SarStart, 5, nBytesPerS), "88888888", "");

        //test rafBinarySearch
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 5, 2), 0, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 5, 4), 1, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 5, 6), 2, ""); //2,3,4
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 5, 8), 5, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 5, 1), -1, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 5, 3), -2, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 5, 5), -3, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 5, 7), -6, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 5, 9), -7, "");

        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 4, 2), 0, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 4, 4), 1, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 4, 6), 4, ""); //any of 2,3,4
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 4, 1), -1, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 4, 3), -2, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 4, 5), -3, "");
        Test.ensureEqual(rafBinarySearch(raf, float.class, farStart, 0, 4, 7), -6, "");

        //test rafFirstGE
        Test.ensureEqual(rafFirstGE(raf, float.class, farStart, 0, 5, 2), 0, "");
        Test.ensureEqual(rafFirstGE(raf, float.class, farStart, 0, 5, 4), 1, "");
        Test.ensureEqual(rafFirstGE(raf, float.class, farStart, 0, 5, 6), 2, ""); //first
        Test.ensureEqual(rafFirstGE(raf, float.class, farStart, 0, 5, 8), 5, "");
        Test.ensureEqual(rafFirstGE(raf, float.class, farStart, 0, 5, 1), 0, "");
        Test.ensureEqual(rafFirstGE(raf, float.class, farStart, 0, 5, 3), 1, "");
        Test.ensureEqual(rafFirstGE(raf, float.class, farStart, 0, 5, 5), 2, "");
        Test.ensureEqual(rafFirstGE(raf, float.class, farStart, 0, 5, 7), 5, "");
        Test.ensureEqual(rafFirstGE(raf, float.class, farStart, 0, 5, 9), 6, "");

        //test rafFirstGAE5
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 2), 0, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 2.0000001), 0, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 1.9999999), 0, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 4), 1, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 6), 2, ""); //first
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 6.0000001), 2, ""); 
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 5.9999999), 2, ""); 
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 8), 5, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 1), 0, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 3), 1, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 3.0000001), 1, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 2.9999999), 1, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 5), 2, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 7), 5, "");
        Test.ensureEqual(rafFirstGAE5(raf, float.class, farStart, 0, 5, 9), 6, "");

        //test rafLastLE
        Test.ensureEqual(rafLastLE(raf, float.class, farStart, 0, 5, 2), 0, "");
        Test.ensureEqual(rafLastLE(raf, float.class, farStart, 0, 5, 4), 1, "");
        Test.ensureEqual(rafLastLE(raf, float.class, farStart, 0, 5, 6), 4, ""); //last
        Test.ensureEqual(rafLastLE(raf, float.class, farStart, 0, 5, 8), 5, "");
        Test.ensureEqual(rafLastLE(raf, float.class, farStart, 0, 5, 1), -1, "");
        Test.ensureEqual(rafLastLE(raf, float.class, farStart, 0, 5, 3), 0, "");
        Test.ensureEqual(rafLastLE(raf, float.class, farStart, 0, 5, 5), 1, "");
        Test.ensureEqual(rafLastLE(raf, float.class, farStart, 0, 5, 7), 4, "");
        Test.ensureEqual(rafLastLE(raf, float.class, farStart, 0, 5, 9), 5, "");

        //test rafLastLAE5
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 2), 0, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 2.0000001), 0, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 1.9999999), 0, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 4), 1, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 6), 4, ""); //last
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 6.0000001), 4, ""); 
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 5.9999999), 4, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 8), 5, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 1), -1, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 3), 0, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 3.0000001), 0, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 2.9999999), 0, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 5), 1, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 7), 4, "");
        Test.ensureEqual(rafLastLAE5(raf, float.class, farStart, 0, 5, 9), 5, "");
        raf.close();

        //test binarySearch
        //FloatArray  far = new FloatArray(new float[]{2,4,6,6,6,8});
        Test.ensureEqual(far.binarySearch(0, 5, 2), 0, "");
        Test.ensureEqual(far.binarySearch(0, 5, 4), 1, "");
        Test.ensureEqual(far.binarySearch(0, 5, 6), 2, ""); //2,3,4
        Test.ensureEqual(far.binarySearch(0, 5, 8), 5, "");
        Test.ensureEqual(far.binarySearch(0, 5, 1), -1, "");
        Test.ensureEqual(far.binarySearch(0, 5, 3), -2, "");
        Test.ensureEqual(far.binarySearch(0, 5, 5), -3, "");
        Test.ensureEqual(far.binarySearch(0, 5, 7), -6, "");
        Test.ensureEqual(far.binarySearch(0, 5, 9), -7, "");

        Test.ensureEqual(far.binarySearch(0, 4, 2), 0, "");
        Test.ensureEqual(far.binarySearch(0, 4, 4), 1, "");
        Test.ensureEqual(far.binarySearch(0, 4, 6), 4, ""); //any of 2,3,4
        Test.ensureEqual(far.binarySearch(0, 4, 1), -1, "");
        Test.ensureEqual(far.binarySearch(0, 4, 3), -2, "");
        Test.ensureEqual(far.binarySearch(0, 4, 5), -3, "");
        Test.ensureEqual(far.binarySearch(0, 4, 7), -6, "");

        //test binaryFindFirstGE
        Test.ensureEqual(far.binaryFindFirstGE(0, 5, 2), 0, "");
        Test.ensureEqual(far.binaryFindFirstGE(0, 5, 4), 1, "");
        Test.ensureEqual(far.binaryFindFirstGE(0, 5, 6), 2, ""); //first
        Test.ensureEqual(far.binaryFindFirstGE(0, 5, 8), 5, "");
        Test.ensureEqual(far.binaryFindFirstGE(0, 5, 1), 0, "");
        Test.ensureEqual(far.binaryFindFirstGE(0, 5, 3), 1, "");
        Test.ensureEqual(far.binaryFindFirstGE(0, 5, 5), 2, "");
        Test.ensureEqual(far.binaryFindFirstGE(0, 5, 7), 5, "");
        Test.ensureEqual(far.binaryFindFirstGE(0, 5, 9), 6, "");

        //test binaryFindFirstGAE5
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 2), 0, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 2.0000001), 0, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 1.9999999), 0, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 4), 1, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 6), 2, ""); //first
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 6.0000001), 2, ""); 
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 5.9999999), 2, ""); 
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 8), 5, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 1), 0, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 3), 1, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 3.0000001), 1, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 2.9999999), 1, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 5), 2, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 7), 5, "");
        Test.ensureEqual(far.binaryFindFirstGAE5(0, 5, 9), 6, "");

        //test binaryFindLastLE
        //FloatArray  far = new FloatArray(new float[]{2,4,6,6,6,8});
        Test.ensureEqual(far.binaryFindLastLE(0, 5, 2), 0, "");
        Test.ensureEqual(far.binaryFindLastLE(0, 5, 4), 1, "");
        Test.ensureEqual(far.binaryFindLastLE(0, 5, 6), 4, ""); //last
        Test.ensureEqual(far.binaryFindLastLE(0, 5, 8), 5, "");
        Test.ensureEqual(far.binaryFindLastLE(0, 5, 1), -1, "");
        Test.ensureEqual(far.binaryFindLastLE(0, 5, 3), 0, "");
        Test.ensureEqual(far.binaryFindLastLE(0, 5, 5), 1, "");
        Test.ensureEqual(far.binaryFindLastLE(0, 5, 7), 4, "");
        Test.ensureEqual(far.binaryFindLastLE(0, 5, 9), 5, "");

        //test binaryFindLastLAE5
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 2), 0, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 2.0000001), 0, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 1.9999999), 0, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 4), 1, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 6), 4, ""); //last
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 6.0000001), 4, ""); 
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 5.9999999), 4, ""); 
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 8), 5, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 1), -1, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 3), 0, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 3.0000001), 0, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 2.9999999), 0, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 5), 1, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 7), 4, "");
        Test.ensureEqual(far.binaryFindLastLAE5(0, 5, 9), 5, "");

        //test binaryFindClosest
        //FloatArray  far = new FloatArray(new float[]{2,4,6,6,6,8});
        Test.ensureEqual(far.binaryFindClosest(2),   0, "");
        Test.ensureEqual(far.binaryFindClosest(2.1), 0, "");
        Test.ensureEqual(far.binaryFindClosest(1.9), 0, "");
        Test.ensureEqual(far.binaryFindClosest(4),   1, "");
        Test.ensureEqual(far.binaryFindClosest(6),   2, ""); //by chance
        Test.ensureEqual(far.binaryFindClosest(5.9), 2, ""); 
        Test.ensureEqual(far.binaryFindClosest(6.1), 4, ""); //since between 6 and 8
        Test.ensureEqual(far.binaryFindClosest(8),   5, "");
        Test.ensureEqual(far.binaryFindClosest(1),   0, "");
        Test.ensureEqual(far.binaryFindClosest(2.9), 0, "");
        Test.ensureEqual(far.binaryFindClosest(3.1), 1, "");
        Test.ensureEqual(far.binaryFindClosest(5.1), 2, "");
        Test.ensureEqual(far.binaryFindClosest(7.1), 5, "");
        Test.ensureEqual(far.binaryFindClosest(9),   5, "");

        //test linearFindClosest
        //FloatArray  far = new FloatArray(new float[]{2,4,6,6,6,8});
        Test.ensureEqual(far.linearFindClosest(2),   0, "");
        Test.ensureEqual(far.linearFindClosest(2.1), 0, "");
        Test.ensureEqual(far.linearFindClosest(1.9), 0, "");
        Test.ensureEqual(far.linearFindClosest(4),   1, "");
        Test.ensureEqual(far.linearFindClosest(6),   2, ""); //unspecified
        Test.ensureEqual(far.linearFindClosest(5.9), 2, ""); //unspecified
        Test.ensureEqual(far.linearFindClosest(6.1), 2, ""); //unspecified
        Test.ensureEqual(far.linearFindClosest(8),   5, "");
        Test.ensureEqual(far.linearFindClosest(1),   0, "");
        Test.ensureEqual(far.linearFindClosest(2.9), 0, "");
        Test.ensureEqual(far.linearFindClosest(3.1), 1, "");
        Test.ensureEqual(far.linearFindClosest(5.1), 2, ""); //unspecified
        Test.ensureEqual(far.linearFindClosest(7.1), 5, "");
        Test.ensureEqual(far.linearFindClosest(9),   5, "");

        //strideWillFind
        Test.ensureEqual(strideWillFind(5, 1), 5, "");
        Test.ensureEqual(strideWillFind(5, 2), 3, "");
        Test.ensureEqual(strideWillFind(5, 3), 2, "");
        Test.ensureEqual(strideWillFind(5, 4), 2, "");
        Test.ensureEqual(strideWillFind(5, 5), 1, "");

        //scaleAddOffset
        ia = new IntArray(new int[]{0,1,2,3,Integer.MAX_VALUE});
        ia.scaleAddOffset(1.5, 10);
        Test.ensureEqual(ia.toString(), "10, 12, 13, 15, 2147483647", "");

        String2.log("PrimitiveArray.testBasic finished successfully.");
    }


    /** 
     * @throws RuntimeException if trouble
     */
    public static void testTestValueOpValue() {
        String2.log("\n*** PrimitiveArray.testTestValueOpValue()");

        //numeric Table.testValueOpValue
        //"!=", PrimitiveArray.REGEX_OP, "<=", ">=", "=", "<", ">"}; 
        long   lnan = Long.MAX_VALUE;
        float  fnan = Float.NaN;
        double dnan = Double.NaN;
        Test.ensureEqual(testValueOpValue(1,    "=",  1), true,  "");
        Test.ensureEqual(testValueOpValue(1,    "=",  2), false, "");
        Test.ensureEqual(testValueOpValue(1,    "=",  lnan), false, "");
        Test.ensureEqual(testValueOpValue(lnan, "=",  1), false, "");
        Test.ensureEqual(testValueOpValue(lnan, "=",  lnan), true, "");

        Test.ensureEqual(testValueOpValue(1f,   "=",  1f), true,  "");
        Test.ensureEqual(testValueOpValue(1f,   "=",  2f), false, "");
        Test.ensureEqual(testValueOpValue(1f,   "=",  fnan), false, "");
        Test.ensureEqual(testValueOpValue(fnan, "=",  1f), false, "");
        Test.ensureEqual(testValueOpValue(fnan, "=",  fnan), true, "");

        Test.ensureEqual(testValueOpValue(1d,   "=",  1d), true,  "");
        Test.ensureEqual(testValueOpValue(1d,   "=",  2d), false, "");
        Test.ensureEqual(testValueOpValue(1d,   "=",  dnan), false, "");
        Test.ensureEqual(testValueOpValue(dnan, "=",  1d), false, "");
        Test.ensureEqual(testValueOpValue(dnan, "=",  dnan), true, "");

        Test.ensureEqual(testValueOpValue(1,    "!=", 1), false,  "");
        Test.ensureEqual(testValueOpValue(1,    "!=", 2), true, "");
        Test.ensureEqual(testValueOpValue(1,    "!=", lnan), true, "");
        Test.ensureEqual(testValueOpValue(lnan, "!=", 1), true, "");
        Test.ensureEqual(testValueOpValue(lnan, "!=", lnan), false, "");

        Test.ensureEqual(testValueOpValue(1f,   "!=", 1f), false,  "");
        Test.ensureEqual(testValueOpValue(1f,   "!=", 2f), true, "");
        Test.ensureEqual(testValueOpValue(1f,   "!=", fnan), true, "");
        Test.ensureEqual(testValueOpValue(fnan, "!=", 1f), true, "");
        Test.ensureEqual(testValueOpValue(fnan, "!=", fnan), false, "");

        Test.ensureEqual(testValueOpValue(1d,   "!=", 1d), false,  "");
        Test.ensureEqual(testValueOpValue(1d,   "!=", 2d), true, "");
        Test.ensureEqual(testValueOpValue(1d,   "!=", dnan), true, "");
        Test.ensureEqual(testValueOpValue(dnan, "!=", 1d), true, "");
        Test.ensureEqual(testValueOpValue(dnan, "!=", dnan), false, "");

        Test.ensureEqual(testValueOpValue(1,    "<=", 1), true,  "");
        Test.ensureEqual(testValueOpValue(1,    "<=", 2), true, "");
        Test.ensureEqual(testValueOpValue(2,    "<=", 1), false, "");
        Test.ensureEqual(testValueOpValue(1,    "<=", lnan), false, "");
        Test.ensureEqual(testValueOpValue(lnan, "<=", 1), false, "");
        Test.ensureEqual(testValueOpValue(lnan, "<=", lnan), false, "");

        Test.ensureEqual(testValueOpValue(1f,   "<=", 1f), true,  "");
        Test.ensureEqual(testValueOpValue(1f,   "<=", 2f), true, "");
        Test.ensureEqual(testValueOpValue(2f,   "<=", 1f), false, "");
        Test.ensureEqual(testValueOpValue(1f,   "<=", fnan), false, "");
        Test.ensureEqual(testValueOpValue(fnan, "<=", 1f), false, "");
        Test.ensureEqual(testValueOpValue(fnan, "<=", fnan), false, "");

        Test.ensureEqual(testValueOpValue(1d,   "<=", 1d), true,  "");
        Test.ensureEqual(testValueOpValue(1d,   "<=", 2d), true, "");
        Test.ensureEqual(testValueOpValue(2d,   "<=", 1d), false, "");
        Test.ensureEqual(testValueOpValue(1d,   "<=", dnan), false, "");
        Test.ensureEqual(testValueOpValue(dnan, "<=", 1d), false, "");
        Test.ensureEqual(testValueOpValue(dnan, "<=", dnan), false, "");

        Test.ensureEqual(testValueOpValue(1,    "<",  1), false,  "");
        Test.ensureEqual(testValueOpValue(1,    "<",  2), true, "");
        Test.ensureEqual(testValueOpValue(1,    "<",  lnan), false, "");
        Test.ensureEqual(testValueOpValue(lnan, "<",  1), false, "");
        Test.ensureEqual(testValueOpValue(lnan, "<",  lnan), false, "");

        Test.ensureEqual(testValueOpValue(1f,   "<",  1f), false,  "");
        Test.ensureEqual(testValueOpValue(1f,   "<",  2f), true, "");
        Test.ensureEqual(testValueOpValue(1f,   "<",  fnan), false, "");
        Test.ensureEqual(testValueOpValue(fnan, "<",  1f), false, "");
        Test.ensureEqual(testValueOpValue(fnan, "<",  fnan), false, "");

        Test.ensureEqual(testValueOpValue(1d,   "<",  1d), false,  "");
        Test.ensureEqual(testValueOpValue(1d,   "<",  2d), true, "");
        Test.ensureEqual(testValueOpValue(1d,   "<",  dnan), false, "");
        Test.ensureEqual(testValueOpValue(dnan, "<",  1d), false, "");
        Test.ensureEqual(testValueOpValue(dnan, "<",  dnan), false, "");

        Test.ensureEqual(testValueOpValue(1,    ">=", 1), true,  "");
        Test.ensureEqual(testValueOpValue(1,    ">=", 2), false, "");
        Test.ensureEqual(testValueOpValue(2,    ">=", 1), true, "");
        Test.ensureEqual(testValueOpValue(1,    ">=", lnan), false, "");
        Test.ensureEqual(testValueOpValue(lnan, ">=", 1), false, "");
        Test.ensureEqual(testValueOpValue(lnan, ">=", lnan), false, "");

        Test.ensureEqual(testValueOpValue(1f,   ">=", 1f), true,  "");
        Test.ensureEqual(testValueOpValue(1f,   ">=", 2f), false, "");
        Test.ensureEqual(testValueOpValue(2f,   ">=", 1f), true, "");
        Test.ensureEqual(testValueOpValue(1f,   ">=", fnan), false, "");
        Test.ensureEqual(testValueOpValue(fnan, ">=", 1f), false, "");
        Test.ensureEqual(testValueOpValue(fnan, ">=", fnan), false, "");

        Test.ensureEqual(testValueOpValue(1d,   ">=", 1d), true,  "");
        Test.ensureEqual(testValueOpValue(1d,   ">=", 2d), false, "");
        Test.ensureEqual(testValueOpValue(2d,   ">=", 1d), true, "");
        Test.ensureEqual(testValueOpValue(1d,   ">=", dnan), false, "");
        Test.ensureEqual(testValueOpValue(dnan, ">=", 1d), false, "");
        Test.ensureEqual(testValueOpValue(dnan, ">=", dnan), false, "");

        Test.ensureEqual(testValueOpValue(2,    ">",  1), true,  "");
        Test.ensureEqual(testValueOpValue(1,    ">",  2), false, "");
        Test.ensureEqual(testValueOpValue(1,    ">",  lnan), false, "");
        Test.ensureEqual(testValueOpValue(lnan, ">",  1), false, "");
        Test.ensureEqual(testValueOpValue(lnan, ">",  lnan), false, "");

        Test.ensureEqual(testValueOpValue(2f,   ">",  1f), true,  "");
        Test.ensureEqual(testValueOpValue(1f,   ">",  2f), false, "");
        Test.ensureEqual(testValueOpValue(1f,   ">",  fnan), false, "");
        Test.ensureEqual(testValueOpValue(fnan, ">",  1f), false, "");
        Test.ensureEqual(testValueOpValue(fnan, ">",  fnan), false, "");

        Test.ensureEqual(testValueOpValue(2d,   ">",  1d), true,  "");
        Test.ensureEqual(testValueOpValue(1d,   ">",  2d), false, "");
        Test.ensureEqual(testValueOpValue(1d,   ">",  dnan), false, "");
        Test.ensureEqual(testValueOpValue(dnan, ">",  1d), false, "");
        Test.ensureEqual(testValueOpValue(dnan, ">",  dnan), false, "");

        //regex tests always via testValueOpValue(string)

        //string testValueOpValue
        //"!=", PrimitiveArray.REGEX_OP, "<=", ">=", "=", "<", ">"}; 
        String s = "";
        Test.ensureEqual(testValueOpValue("a", "=",  "a"), true,  "");
        Test.ensureEqual(testValueOpValue("a", "=",  "B"), false, "");
        Test.ensureEqual(testValueOpValue("a", "=",  s), false, "");
        Test.ensureEqual(testValueOpValue(s,   "=",  "a"), false, "");
        Test.ensureEqual(testValueOpValue(s,   "=",  s), true, "");

        Test.ensureEqual(testValueOpValue("a", "!=", "a"), false,  "");
        Test.ensureEqual(testValueOpValue("a", "!=", "B"), true, "");
        Test.ensureEqual(testValueOpValue("a", "!=", s), true, "");
        Test.ensureEqual(testValueOpValue(s,   "!=", "a"), true, "");
        Test.ensureEqual(testValueOpValue(s,   "!=", s), false, "");

        Test.ensureEqual(testValueOpValue("a", "<=", "a"), true,  "");
        Test.ensureEqual(testValueOpValue("a", "<=", "B"), true, "");
        Test.ensureEqual(testValueOpValue("B", "<=", "a"), false, "");
        Test.ensureEqual(testValueOpValue("a", "<=", s), false, "");
        Test.ensureEqual(testValueOpValue(s,   "<=", "a"), true, "");
        Test.ensureEqual(testValueOpValue(s,   "<=", s), true, "");

        Test.ensureEqual(testValueOpValue("a", "<",  "a"), false,  "");
        Test.ensureEqual(testValueOpValue("a", "<",  "B"), true, "");

        Test.ensureEqual(testValueOpValue("a", ">=", "a"), true,  "");
        Test.ensureEqual(testValueOpValue("a", ">=", "B"), false, "");
        Test.ensureEqual(testValueOpValue("B", ">=", "a"), true, "");

        Test.ensureEqual(testValueOpValue("B", ">",  "a"), true,  "");
        Test.ensureEqual(testValueOpValue("a", ">",  "B"), false, "");

        Test.ensureEqual(testValueOpValue("12345", PrimitiveArray.REGEX_OP, "[0-9]+"), true,  "");
        Test.ensureEqual(testValueOpValue("12a45", PrimitiveArray.REGEX_OP, "[0-9]+"), false, "");

        //test speed
        long tTime = System.currentTimeMillis();
        int n = 1000000;
        for (int i = 0; i < n; i++) {
            Test.ensureEqual(testValueOpValue("abcdefghijk", "=",  "abcdefghijk"), true,  "");
            Test.ensureEqual(testValueOpValue("abcdefghijk", "!=", "abcdefghijk"), false,  "");
            Test.ensureEqual(testValueOpValue("abcdefghijk", "<=", "abcdefghijk"), true, "");
            Test.ensureEqual(testValueOpValue("abcdefghijk", "<",  "abcdefghijk"), false,  "");
            Test.ensureEqual(testValueOpValue("abcdefghijk", ">=", "abcdefghijk"), true,  "");
            Test.ensureEqual(testValueOpValue("abcdefghijk", ">",  "abcdefghijk"), false,  "");
        }
        String2.log("time for " + (6 * n) + " testValueOpValue(string): " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 312ms, 2012-06-29: 1859 ms)");

        //regex simple
        for (int i = 0; i < n; i++) {
            Test.ensureEqual(testValueOpValue("12345", PrimitiveArray.REGEX_OP, "[0-9]+"), true,  "");
        }
        String2.log("time for " + n + " regex testValueOpValue(string, regex): " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 718ms, 2012-06-29: 4453 ms)");

        //long
        tTime = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            Test.ensureEqual(testValueOpValue(1, "=",  1), true,  "");
            Test.ensureEqual(testValueOpValue(1, "!=", 1), false,  "");
            Test.ensureEqual(testValueOpValue(1, "<=", 1), true,  "");
            Test.ensureEqual(testValueOpValue(1, "<",  1), false,  "");
            Test.ensureEqual(testValueOpValue(1, ">=", 1), true,  "");
            Test.ensureEqual(testValueOpValue(2, ">",  1), true,  "");
            Test.ensureEqual(testValueOpValue(1, ">",  2), false, "");
            //regex tests always via testValueOpValue(string)
        }
        String2.log("time for " + (7 * n) + " testValueOpValue(long): " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 78ms, 2012-06-29: 328 ms)");

        //float
        tTime = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            Test.ensureEqual(testValueOpValue(1, "=",  1f), true,  "");
            Test.ensureEqual(testValueOpValue(1, "!=", 1f), false,  "");
            Test.ensureEqual(testValueOpValue(1, "<=", 1f), true,  "");
            Test.ensureEqual(testValueOpValue(1, "<",  1f), false,  "");
            Test.ensureEqual(testValueOpValue(1, ">=", 1f), true,  "");
            Test.ensureEqual(testValueOpValue(2, ">",  1f), true,  "");
            Test.ensureEqual(testValueOpValue(1, ">",  2f), false, "");
            //regex tests always via testValueOpValue(string)
        }
        String2.log("time for " + (7 * n) + " testValueOpValue(float): " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 109ms, 2012-06-29: 328 ms)");

        //double
        tTime = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            Test.ensureEqual(testValueOpValue(1, "=",  1d), true,  "");
            Test.ensureEqual(testValueOpValue(1, "!=", 1d), false,  "");
            Test.ensureEqual(testValueOpValue(1, "<=", 1d), true,  "");
            Test.ensureEqual(testValueOpValue(1, "<",  1d), false,  "");
            Test.ensureEqual(testValueOpValue(1, ">=", 1d), true,  "");
            Test.ensureEqual(testValueOpValue(2, ">",  1d), true,  "");
            Test.ensureEqual(testValueOpValue(1, ">",  2d), false, "");
            //regex tests always via testValueOpValue(string)
        }
        String2.log("time for " + (7 * n) + " testValueOpValue(double): " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 78ms, 2012-06-29: 329 ms)");

        tTime = System.currentTimeMillis();
        for (int i = 0; i < 7*n; i++) {
            Test.ensureEqual(testValueOpValue(1, "<=",  1), true,  "");
        }
        String2.log("time for " + (7 * n) + " testValueOpValue(double <=): " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 62ms, 2012-06-29: 234 ms)");

        //**********  test applyConstraint
        PrimitiveArray pa;
        BitSet keep;

        //regex
        pa = factory(int.class, n, "5");
        pa.addInt(10);
        pa.addString("");
        keep = new BitSet();
        keep.set(0, pa.size());
        tTime = System.currentTimeMillis();
        pa.applyConstraint(keep, "=~", "(10|zztop)");
        pa.justKeep(keep);
        Test.ensureEqual(pa.size(), 1, "");
        Test.ensureEqual(pa.getDouble(0), 10, "");
        String2.log("time for applyConstraint(regex) n=" + n + ": " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 139ms, 2012-06-29: 500 ms)");

        //string
        pa = factory(String.class, n, "Apple");
        pa.addString("Nate");
        pa.addString("");
        keep = new BitSet();
        keep.set(0, pa.size());
        tTime = System.currentTimeMillis();
        pa.applyConstraint(keep, ">=", "hubert");  //>= uses case insensitive test
        pa.justKeep(keep);
        Test.ensureEqual(pa.size(), 1, "");
        Test.ensureEqual(pa.getString(0), "Nate", "");
        String2.log("time for applyConstraint(String) n=" + n + ": " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 93ms, 2012-06-29: 406 ms)");

        //float
        pa = factory(float.class, n, "5");
        pa.addInt(10);
        pa.addString("");
        keep = new BitSet();
        keep.set(0, pa.size());
        tTime = System.currentTimeMillis();
        pa.applyConstraint(keep, ">=", "9");
        pa.justKeep(keep);
        Test.ensureEqual(pa.size(), 1, "");
        Test.ensureEqual(pa.getDouble(0), 10, "");
        String2.log("time for applyConstraint(float) n=" + n + ": " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 93ms, 2012-06-29: 140 ms)");

        //double
        pa = factory(double.class, n, "5");
        pa.addInt(10);
        pa.addString("");
        keep = new BitSet();
        keep.set(0, pa.size());
        tTime = System.currentTimeMillis();
        pa.applyConstraint(keep, ">=", "9");
        pa.justKeep(keep);
        Test.ensureEqual(pa.size(), 1, "");
        Test.ensureEqual(pa.getDouble(0), 10, "");
        String2.log("time for applyConstraint(double) n=" + n + ": " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 31ms, 2012-06-29: 125 ms)");

        //int
        pa = factory(int.class, n, "5");
        pa.addInt(10);
        pa.addString("");
        keep = new BitSet();
        keep.set(0, pa.size());
        tTime = System.currentTimeMillis();
        pa.applyConstraint(keep, ">=", "9");
        pa.justKeep(keep);
        Test.ensureEqual(pa.size(), 1, "");
        Test.ensureEqual(pa.getDouble(0), 10, "");
        String2.log("time for applyConstraint(int) n=" + n + ": " + 
            (System.currentTimeMillis() - tTime) + " (Java 1.7M4700 16ms, 2012-06-29: 141 ms)");

    }

    /**
     * This tests the methods of this class.
     *
     * @throws Exception if trouble.
     */
    public static void test() throws Throwable {
        String2.log("*** PrimitiveArray.test");
        testBasic();
        testTestValueOpValue();
    }


    /**
     * This runs test.
     */
    public static void main(String args[]) throws Throwable {
        test();
    }

}

