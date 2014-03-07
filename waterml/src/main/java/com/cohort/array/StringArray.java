/* Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.array;

import com.cohort.util.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * StringArray is a thin shell over a String[] with methods like ArrayList's 
 * methods; it extends PrimitiveArray.
 * All of the methods which add strings to StringArray (e.g., add()),
 * use String2.canonical(), to ensure that canonical Strings are stored (to save memory
 * if there are duplicates).
 *
 * <p>This class uses "" to represent a missing value (NaN).
 */
public class StringArray extends PrimitiveArray {

    /**
     * This is the main data structure.
     * This should be private, but is public so you can manipulate it if you 
     * promise to be careful.
     * Note that if the PrimitiveArray's capacity is increased,
     * the PrimitiveArray will use a different array for storage.
     */
    public String[] array;

    /**
     * A constructor for a capacity of 8 elements. The initial 'size' will be 0.
     */
    public StringArray() {
        array = new String[8];
    }

    /**
     * This constructs a StringArray by copying elements from the incoming
     * PrimitiveArray (using append()).
     *
     * @param primitiveArray a primitiveArray of any type 
     */
    public StringArray(PrimitiveArray primitiveArray) {
        array = new String[8];
        append(primitiveArray);
    }

    /**
     * A constructor for a specified number of elements. The initial 'size' will be 0.
     *
     * @param capacity creates an StringArray with the specified initial capacity.
     * @param active if true, size will be set to capacity and all elements 
     *    will equal "", else size = 0.
     * @throws Exception if trouble.
     */
    public StringArray(int capacity, boolean active) {
        Math2.ensureMemoryAvailable(16L * capacity, "StringArray"); //16 is lame estimate of space needed per String
        array = new String[capacity];
        if (active) {
            size = capacity;
            for (int i = 0; i < size; i++)
                array[i] = "";
        }
    }

    /**
     * A constructor which (at least initially) uses the array and all its 
     * elements ('size' will equal anArray.length).
     *
     * @param anArray the array to be used as this object's array.
     */
    public StringArray(String[] anArray) {
        array = anArray;
        size = anArray.length;
        for (int i = 0; i < size; i++)
            array[i] = String2.canonical(array[i]);
    }

    /**
     * A constructor which gets the toString values from the objects from an iterator.
     *
     * @param iterator  which needs to be thread-safe if the backing data store may be
     *    changed by another thread (e.g., use ConcurrentHashMap instead of HashMap).
     */
    public StringArray(Iterator iterator) {
        array = new String[8];
        while (iterator.hasNext()) {
            add(iterator.next().toString());
        }
    }

    /**
     * A constructor which gets the toString values from the objects from an enumeration.
     *
     * @param enumeration  which needs to be thread-safe if the backing data store may be
     *    changed by another thread (e.g., use ConcurrentHashMap instead of HashMap).
     */
    public StringArray(Enumeration enumeration) {
        array = new String[8];
        while (enumeration.hasMoreElements()) {
            add(enumeration.nextElement().toString());
        }
    }

    /* *  probably works, but not tested
     * This makes a StringArray with the contents of a map.
     * Each entry will be from <key>.toString() = <value>.toString().
     *
     * @param map  if it needs to be thread-safe, use ConcurrentHashMap
     * @return the corresponding String, with one entry on each line 
     *    (<key>.toString() = <value>.toString()) unsorted.
     *    Use sort() or sortIgnoreCase() afterwards if desired.
     * /    
    public StringArray(Map map) {
        Set keySet = map.keySet();
        array = new String[keySet.size()];
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            Object key = it.next();
            Object value = map.get(key);
            add(key.toString() + " = " + 
                (value == null? "null" : value.toString());
        }
    } */

    /**
     * This reads the text contents of the specified file using this computer's default charset.
     * 
     * @throws Exception if trouble (e.g., file not found)
     */
    public static StringArray fromFile(String fileName) throws Exception {
   
        return fromFile(fileName, null);
    }


    /**
     * This reads the text contents of the specified file.
     * 
     * @param charset e.g., ISO-8859-1; or "" or null for the default
     * @throws Exception if trouble (e.g., file not found)
     */
    public static StringArray fromFile(String fileName, String charset) throws Exception {
        Math2.ensureMemoryAvailable(File2.length(fileName), "StringArray.fromFile"); //canonical may lessen memory requirement
        StringArray sa = new StringArray();
        FileInputStream fis = new FileInputStream(fileName);
        InputStreamReader isr = charset == null || charset.length() == 0?
            new InputStreamReader(fis) :
            new InputStreamReader(fis, charset);
        BufferedReader bufferedReader = new BufferedReader(isr);
        String s = bufferedReader.readLine();
        while (s != null) { //null = end-of-file
            sa.add(s);
            s = bufferedReader.readLine();
        }
        bufferedReader.close();
        return sa;
    }

    /**
     * Like the other toFile, but uses the default charset and lineSeparator.
     */
    public void toFile(String fileName) throws Exception {
        toFile(fileName, null, null);
    }

    /**
     * This writes the strings to a file.
     *
     * @param fileName is the (usually canonical) path (dir+name) for the file
     * @param charset e.g., UTF-8; or null or "" for the default (ISO-8859-1)
     * @param lineSeparator is the desired lineSeparator for the outgoing file.
     *     e.g., "\n".
     *     null or "" uses String2.lineSeparator (the standard separator for this OS).
     * @throws Exception if trouble (e.g., file can't be created).
     *    If trouble, this will delete any partial file.
     */
    public void toFile(String fileName, String charset, String lineSeparator) 
        throws Exception {
        
        if (lineSeparator == null || lineSeparator.length() == 0)
            lineSeparator = String2.lineSeparator;
        boolean append = false;
        Exception e = null;

        //bufferedWriter is declared outside try/catch so it
        //can be accessed from within either try/catch block.
        BufferedWriter bufferedWriter = null;
        try {
            //open the file
            if (charset == null || charset.length() == 0)
                charset = "ISO-8859-1";
            Writer w = new OutputStreamWriter(new FileOutputStream(fileName, append), charset);
            bufferedWriter = new BufferedWriter(w);
                         
            //write the text to the file
            for (int i = 0; i < size; i++) {
                bufferedWriter.write(array[i]);
                bufferedWriter.write(lineSeparator);
            }

        } catch (Exception e2) {
            e = e2;
        }

        //make sure bufferedWriter is closed
        try {
            if (bufferedWriter != null) {
                bufferedWriter.close();

            }
        } catch (Exception e2) {
            if (e == null)
                e = e2; 
            //else ignore the error (the first one is more important)
        }

        //and delete partial file if error 
        if (e != null) {
            File2.delete(fileName);
            throw e;
        }       
    }


    /**
     * This returns the current capacity (number of elements) of the internal data array.
     * 
     * @return the current capacity (number of elements) of the internal data array.
     */
    public int capacity() {
        return array.length;
    }

    /**
     * This returns the hashcode for this StringArray (dependent only on values,
     * not capacity).
     * WARNING: the algorithm used may change in future versions.
     *
     * @return the hashcode for this byteArray (dependent only on values,
     * not capacity)
     */
    public int hashCode() {
        //see http://download.oracle.com/javase/7/docs/api/java/util/List.html#hashCode()
        //and http://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
        int code = 0;
        for (int i = 0; i < size; i++)
            code = 31*code + array[i].hashCode();
        return code;
    }

    /**
     * This makes a new subset of this PrimitiveArray based on startIndex, stride,
     * and stopIndex.
     *
     * @param startIndex must be a valid index
     * @param stride   must be at least 1
     * @param stopIndex (inclusive) If &gt;= size, it will be changed to size-1.
     * @return a new PrimitiveArray with the desired subset.
     *    It will have a new backing array (same type as this class)
     *    with a capacity equal to its size.
     *    If stopIndex &lt; startIndex, this returns PrimitiveArray with size=0;
     */
    public PrimitiveArray subset(int startIndex, int stride, int stopIndex) {
        if (startIndex < 0)
            throw new IndexOutOfBoundsException(MessageFormat.format(
                ArraySubsetStart, getClass().getSimpleName(), "" + startIndex));
        if (stride < 1)
            throw new IllegalArgumentException(MessageFormat.format(
                ArraySubsetStride, getClass().getSimpleName(), "" + stride));
        if (stopIndex >= size)
            stopIndex = size - 1;
        if (stopIndex < startIndex)
            return new StringArray(new String[0]);

        int willFind = strideWillFind(stopIndex - startIndex + 1, stride);
        Math2.ensureMemoryAvailable(16L * willFind, "StringArray"); //16 is a lame estimate of bytes/element        
        String tar[] = new String[willFind];
        if (stride == 1) {
            System.arraycopy(array, startIndex, tar, 0, willFind);
        } else {
            int po = 0;
            for (int i = startIndex; i <= stopIndex; i+=stride) 
                tar[po++] = array[i];
        }
        return new StringArray(tar);
    }

    /**
     * This returns the class (String.class) of the element type.
     *
     * @return the class (String.class) of the element type.
     */
    public Class elementClass() {
        return String.class;
    }

    /**
     * This returns the class index (CLASS_INDEX_STRING) of the element type.
     *
     * @return the class index (CLASS_INDEX_STRING) of the element type.
     */
    public int elementClassIndex() {
        return CLASS_INDEX_STRING;
    }

    /**
     * This adds an item to the array (increasing 'size' by 1).
     *
     * @param value the value to be added to the array
     */
    public void add(String value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        array[size++] = String2.canonical(value);
    }

    /**
     * Use this for temporary arrays to add an item to the array (increasing 'size' by 1)
     * without using String2.canonical.
     *
     * @param value the value to be added to the array
     */
    public void addNotCanonical(String value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        array[size++] = value;
    }

    /**
     * This makes sure all of the values are the canonical values.
     *
     */
    public void makeCanonical() {
        for (int i = 0; i < size; i++)
            array[i] = String2.canonical(array[i]);
    }

    /**
     * This adds all the strings from sar.
     *
     * @param sar a String[]
     */
    public void add(String sar[]) {
        int otherSize = sar.length; 
        ensureCapacity(size + (long)otherSize);
        for (int i = 0; i < otherSize; i++)
            array[size + i] = String2.canonical(sar[i]);
        size += otherSize;
    }    

    /**
     * This adds n copies of value to the array (increasing 'size' by n).
     *
     * @param n  if less than 0, this throws Exception
     * @param value the value to be added to the array.
     */
    public void addN(int n, String value) {
        if (n == 0) return;
        if (n < 0)
            throw new IllegalArgumentException(MessageFormat.format(
                ArrayAddN, getClass().getSimpleName(), "" + n));
        value = String2.canonical(value);
        ensureCapacity(size + (long)n);
        Arrays.fill(array, size, size + n, value);
        size += n;
    }

    /**
     * This inserts an item into the array (increasing 'size' by 1).
     *
     * @param index the position where the value should be inserted.
     * @param value the value to be inserted into the array
     */
    public void add(int index, String value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        System.arraycopy(array, index, array, index + 1, size - index);
        size++;
        array[index] = String2.canonical(value);
    }

    /**
     * This adds an element to the array at the specified index.
     *
     * @param index 0..
     * @param value the value, as a String.
     */
    public void addString(int index, String value) {
        add(index, String2.canonical(value));
    }

    /**
     * This adds n Strings to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as a String.
     */
    public void addNStrings(int n, String value) {
        addN(n, value);
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a String.
     */
    public void addString(String value) {
        add(value);
    }

    /**
     * This adds an element to the array.
     *
     * @param value the float value
     */
    public void addFloat(float value) {
        add(Math2.isFinite(value)? String.valueOf(value) : "");
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a double.
     */
    public void addDouble(double value) {
        add(Math2.isFinite(value)? String.valueOf(value) : "");
    }

    /**
     * This adds n doubles to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as a double.
     */
    public void addNDoubles(int n, double value) {
        addN(n, Math2.isFinite(value)? String.valueOf(value) : "");
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as an int.
     */
    public void addInt(int value) {
        add(value == Integer.MAX_VALUE? "" : String.valueOf(value));
    }

    /**
     * This adds n ints to the array.
     *
     * @param n the number of times 'value' should be added
     * @param value the value, as an int.
     */
    public void addNInts(int n, int value) {
        addN(n, value == Integer.MAX_VALUE? "" : String.valueOf(value));
    }

    /**
     * This adds an element to the array.
     *
     * @param value the value, as a long.
     */
    public void addLong(long value) {
        add(value == Long.MAX_VALUE? "" : String.valueOf(value));
    }

    /**
     * This adds an element from another PrimitiveArray.
     *
     * @param otherPA
     * @param otherIndex
     */
    public void addFromPA(PrimitiveArray otherPA, int otherIndex) {
        add(otherPA.getString(otherIndex));
    }

    /**
     * This sets an element from another PrimitiveArray.
     *
     * @param index the index to be set
     * @param otherPA
     * @param otherIndex
     */
    public void setFromPA(int index, PrimitiveArray otherPA, int otherIndex) {
        set(index, otherPA.getString(otherIndex));
    }

    /**
     * This removes the specified element.
     *
     * @param index the element to be removed, 0 ... size-1
     * @throws Exception if trouble.
     */
    public void remove(int index) {
        if (index >= size)
            throw new IllegalArgumentException(MessageFormat.format(
                ArrayRemove, getClass().getSimpleName(), "" + index, "" + size));
        System.arraycopy(array, index + 1, array, index, size - index - 1);
        size--;

        //for object types, nullify the object at the end
        array[size] = null;
    }

    /**
     * This removes the specified range of elements.
     *
     * @param from the first element to be removed, 0 ... size
     * @param to one after the last element to be removed, from ... size
     * @throws Exception if trouble.
     */
    public void removeRange(int from, int to) {
        if (to > size)
            throw new IllegalArgumentException(String2.ERROR + " in StringArray.removeRange: to (" + 
                to + ") > size (" + size + ").");
        if (from >= to) {
            if (from == to) 
                return;
            throw new IllegalArgumentException(String2.ERROR + " in StringArray.removeRange: from (" + 
                from + ") > to (" + to + ").");
        }
        System.arraycopy(array, to, array, from, size - to);
        size -= to - from;

        //for object types, nullify the objects at the end
        Arrays.fill(array, size, size + to - from, null);
    }

    /**
     * Moves elements 'first' through 'last' (inclusive)
     *   to 'destination'.
     *
     * @param first  the first to be move
     * @param last  (exclusive)
     * @param destination the destination, can't be in the range 'first+1..last-1'.
     * @throws Exception if trouble
     */
    public void move(int first, int last, int destination) {
        String errorIn = String2.ERROR + " in StringArray.move:\n";

        Test.ensureTrue(first >= 0, 
            errorIn + "first (" + first + ") must be >= 0.");
        Test.ensureTrue(last >= first && last <= size, 
            errorIn + "last (" + last + ") must be >= first (" + first + ") and <= size (" + size + ").");
        Test.ensureTrue(destination >= 0 && destination <= size, 
            errorIn + "destination (" + destination + ") must be between 0 and size (" + size + ").");
        Test.ensureTrue(destination <= first || destination >= last, 
            errorIn + "destination (" + destination + ") must be <= first (" + first + ") or >= last (" + last + ").");
        if (first == last || destination == first || destination == last) 
            return; //nothing to do
        //String2.log("move first=" + first + " last=" + last + " dest=" + destination);
        //String2.log("move initial " + String2.toCSSVString(array));

        //store the range to be moved
        int nToMove = last - first;
        String[] temp = new String[nToMove];
        System.arraycopy(array, first, temp, 0, nToMove);

        //if moving to left...    (draw diagram to visualize this)
        if (destination < first) {
            System.arraycopy(array, destination, array, destination + nToMove, first - destination);
            //String2.log("move after shift " + String2.toCSSVString(array));

            //copy temp data into place
            System.arraycopy(temp, 0, array, destination, nToMove);
        } else {
            //moving to right
            System.arraycopy(array, last, array, first, destination - last);
            //String2.log("move after shift " + String2.toCSSVString(array));

            //copy temp data into place
            System.arraycopy(temp, 0, array, destination - nToMove, nToMove);
        }
        //String2.log("move done " + String2.toCSSVString(array));


    }

    /**
     * This just keeps the rows for the 'true' values in the bitset.
     * Rows that aren't kept are removed.
     * The resulting PrimitiveArray is compacted (i.e., it has a smaller size()).
     *
     * @param bitset
     */
    public void justKeep(BitSet bitset) {
        int newSize = 0;
        for (int row = 0; row < size; row++) {
            if (bitset.get(row)) 
                array[newSize++] = array[row];
        }
        removeRange(newSize, size);
    }    

    /**
     * This ensures that the capacity is at least 'minCapacity'.
     *
     * @param minCapacity the minimum acceptable capacity.
     *    minCapacity is type long, but &gt;= Integer.MAX_VALUE will throw exception.
     */
    public void ensureCapacity(long minCapacity) {
        if (array.length < minCapacity) {
            //ensure minCapacity is < Integer.MAX_VALUE
            Math2.ensureArraySizeOkay(minCapacity, "StringArray");  
            //caller may know exact number needed, so don't double above 2x current size
            int newCapacity = (int)Math.min(Integer.MAX_VALUE - 1, array.length + (long)array.length); 
            if (newCapacity < minCapacity) 
                newCapacity = (int)minCapacity; //safe since checked above
            Math2.ensureMemoryAvailable(8L * newCapacity, "StringArray"); //8 is feeble minimal estimate
            String[] newArray = new String[newCapacity];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray; //do last to minimize concurrency problems
        }
    }

    /**
     * This returns an array (perhaps 'array') which has 'size' elements.
     *
     * @return an array (perhaps 'array') which has 'size' elements.
     */
    public String[] toArray() {
        if (array.length == size)
            return array;
        Math2.ensureMemoryAvailable(8L * size, "StringArray.toArray"); //8L is feeble minimal estimate
        String[] tArray = new String[size];
        System.arraycopy(array, 0, tArray, 0, size);
        return tArray;
    }
   
    /**
     * This returns a primitive[] (perhaps 'array') which has 'size' 
     * elements.
     *
     * @return a primitive[] (perhaps 'array') which has 'size' elements.
     */
    public Object toObjectArray() {
        return toArray();
    }

    /**
     * This returns a double[] (perhaps 'array') which has 'size' elements.
     *
     * @return a double[] (perhaps 'array') which has 'size' elements.
     *    Non-finite values are returned as Double.NaN's.
     */
    public double[] toDoubleArray() {
        Math2.ensureMemoryAvailable(8L * size, "StringArray.toDoubleArray"); //8L is feeble minimal estimate
        double dar[] = new double[size];
        for (int i = 0; i < size; i++)
            dar[i] = String2.parseDouble(array[i]);
        return dar;
    }

    /**
     * This returns a String[] which has 'size' elements.
     *
     * @return a String[] which has 'size' elements.
     */
    public String[] toStringArray() {
        return toArray();
    }

    /**
     * This gets a specified element.
     *
     * @param index 0 ... size-1
     * @throws Exception if trouble.
     */
    public String get(int index) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in StringArray.get: index (" + 
                index + ") >= size (" + size + ").");
        return array[index];
    }

    /**
     * This sets a specified element.
     *
     * @param index 0 ... size-1
     * @param value the value for that element
     * @throws Exception if trouble.
     */
    public void set(int index, String value) {
        if (index >= size)
            throw new IllegalArgumentException(String2.ERROR + " in StringArray.set: index (" + 
                index + ") >= size (" + size + ").");
        array[index] = String2.canonical(value);
    }


    /**
     * Return a value from the array as an int.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as an int. This uses String2.parseInt.
     * @throws Exception if trouble.
     */
    public int getInt(int index) {
        return String2.parseInt(get(index));
    }

    /**
     * Set a value in the array as an int.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. 
     * @throws Exception if trouble.
     */
    public void setInt(int index, int i) {
        set(index, i == Integer.MAX_VALUE? "" : String.valueOf(i));
    }

    /**
     * Return a value from the array as a long.
     * 
     * @param index the index number 0 ... size-1
     * @return the value as a long. This uses String2.parseLong.
     * @throws Exception if trouble.
     */
    public long getLong(int index) {
        return String2.parseLong(get(index));
    }

    /**
     * Set a value in the array as a long.
     * 
     * @param index the index number 0 .. size-1
     * @param i the value. 
     * @throws Exception if trouble.
     */
    public void setLong(int index, long i) {
        set(index, i == Long.MAX_VALUE? "" : String.valueOf(i));
    }


    /**
     * Return a value from the array as a float.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a float. String values are parsed
     *   with String2.parseFloat and so may return Float.NaN.
     * @throws Exception if trouble.
     */
    public float getFloat(int index) {
        return String2.parseFloat(get(index));
    }

    /**
     * Set a value in the array as a float.
     * 
     * @param index the index number 0 .. size-1
     * @param d the value. For numeric PrimitiveArray, it is narrowed 
     *   if needed by methods like Math2.roundToString(d).
     * @throws Exception if trouble.
     */
    public void setFloat(int index, float d) {
        set(index, Math2.isFinite(d)? String.valueOf(d) : "");
    }

    /**
     * Return a value from the array as a double.
     * 
     * @param index the index number 0 .. size-1
     * @return the value as a double. String values are parsed
     *   with String2.parseDouble and so may return Double.NaN.
     * @throws Exception if trouble.
     */
    public double getDouble(int index) {
        return String2.parseDouble(get(index));
    }

    /**
     * Set a value in the array as a double.
     * 
     * @param index the index number 0 .. size-1
     * @param d the value. For numeric PrimitiveArray, it is narrowed 
     *   if needed by methods like Math2.roundToString(d).
     * @throws Exception if trouble.
     */
    public void setDouble(int index, double d) {
        set(index, Math2.isFinite(d)? String.valueOf(d) : "");
    }

    /**
     * Return a value from the array as a String.
     * 
     * @param index the index number 0 .. 
     * @return For numeric types, this returns array[index].
     * @throws Exception if trouble.
     */
    public String getString(int index) {
        return get(index);
    }

    /**
     * Set a value in the array as a String.
     * 
     * @param index the index number 0 .. 
     * @param s the value. For numeric PrimitiveArray's, it is parsed
     *   with String2.parse and narrowed if needed by methods like
     *   Math2.roundToString(d).
     * @throws Exception if trouble.
     */
    public void setString(int index, String s) {
        set(index, s);
    }

    /**
     * This finds the first instance of 'lookFor' starting at index 0.
     *
     * @param lookFor the value to be looked for
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(String lookFor) {
        return indexOf(lookFor, 0);
    }


    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOf(String lookFor, int startIndex) {
        for (int i = startIndex; i < size; i++) 
            if (array[i].equals(lookFor)) 
                return i;
        return -1;
    }

    /**
     * This finds the first value which equals 'lookFor' starting at index 0, ignoring case.
     *
     * @param lookFor the value to be looked for
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOfIgnoreCase(String lookFor) {
        return indexOfIgnoreCase(lookFor, 0);
    }


    /**
     * This finds the first value which equals 'lookFor' starting at index 'startIndex', ignoring case.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int indexOfIgnoreCase(String lookFor, int startIndex) {
        lookFor = lookFor.toLowerCase();
        for (int i = startIndex; i < size; i++) 
            if (array[i].toLowerCase().equals(lookFor)) 
                return i;
        return -1;
    }

    /**
     * This finds the last value which equals 'lookFor' starting at index 'startIndex'.
     *
     * @param lookFor the value to be looked for
     * @param startIndex 0 ... size-1. The search progresses towards 0.
     * @return the index where 'lookFor' is found, or -1 if not found.
     */
    public int lastIndexOf(String lookFor, int startIndex) {
        if (startIndex >= size)
            throw new IllegalArgumentException(String2.ERROR + " in StringArray.get: startIndex (" + 
                startIndex + ") >= size (" + size + ").");
        for (int i = startIndex; i >= 0; i--) 
            if (array[i].equals(lookFor)) 
                return i;
        return -1;
    }


    /**
     * This finds the first value which has the substring 'lookFor',
     * starting at index 'startIndex' and position 'startPo'.
     *
     * @param lookFor the value to be looked for
     * @param start int[2] {0=startIndex 0 ... size-1, 
     *     1=startPo 0... (used on the first line only; startPo=0 is used thereafter)}
     * @return The results are returned in start and here (for convenience), 
     *     [0]=index, [1]=po, where 'lookFor' is found, or {-1,-1} if not found.
     */
    public int[] indexWith(String lookFor, int start[]) {
        int startPo = start[1];
        for (int i = start[0]; i < size; i++) {
            int po = array[i].indexOf(lookFor, startPo);
            if (po >= 0) {
                start[0] = i;
                start[1] = po;
                return start;
            }
            startPo = 0;
        }
        start[0] = -1;
        start[1] = -1;
        return start;
    }


    /**
     * If size != capacity, this makes a new 'array' of size 'size'
     * so capacity will equal size.
     */
    public void trimToSize() {
        array = toArray();
    }

    /**
     * Test if o is an StringArray with the same size and values.
     *
     * @param o
     * @return true if equal.  o=null throws an exception.
     */
    public boolean equals(Object o) {
        return testEquals(o).length() == 0;
    }

    /**
     * Test if o is an StringArray with the same size and values,
     * but returns a String describing the difference (or "" if equal).
     *
     * @param o
     * @return a String describing the difference (or "" if equal).
     *   o=null throws an exception.
     */
    public String testEquals(Object o) {
        if (!(o instanceof StringArray))
            return "The two objects aren't equal: this object is a StringArray; the other is a " + 
                o.getClass().getName() + ".";
        StringArray other = (StringArray)o;
        if (other.size() != size)
            return "The two StringArrays aren't equal: one has " + size + " value" +
               (size == 0? "s" :
                size == 1? " (\"" + array[0] + "\")" : 
                           "s (from \"" + array[0] + "\" to \"" + array[size - 1] + "\")") +
               "; the other has " + other.size() + " value" +
               (other.size == 0? "s" :
                other.size == 1? " (\"" + other.array[0] + "\")" : 
                                 "s (from \"" + other.array[0] + "\" to \"" + other.array[other.size - 1] + "\")") +
               ".";
        for (int i = 0; i < size; i++)
            if (!array[i].equals(other.array[i]))
                return "The two StringArrays aren't equal: this[" + i + "]=\"" + array[i] + 
                                                     "\"; other[" + i + "]=\"" + other.array[i] + "\".";
        return "";
    }


    /** 
     * This converts the elements into a comma-separated (CSV) String.
     * If a value has an internal comma or double quotes, the value is surrounded by 
     * double quotes and the internal quotes are replaced by 2 double quotes.
     *
     * @return a Comma-Separated-Value (not comma space) String representation 
     */
    public String toCSVString() {
        Math2.ensureMemoryAvailable(8L * size, "StringArray.toCSVString"); //8L is lame estimate of bytes/element
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(',');
            String s = array[i];
            if (s == null) {
            } else if (s.indexOf('"') >= 0) { //check for '"' before check for ','
                s = String2.replaceAll(s, "\"", "\"\"");
                sb.append("\"" + s + "\"");
            } else if (s.indexOf(',') >= 0) {
                sb.append("\"" + s + "\"");
            } else {
                sb.append(s);
            }
        }
        return sb.toString(); 
    }

    /** 
     * This converts the elements into a Comma-Space-Separated-Value (CSSV) String.
     * If a value has an internal comma or double quotes, the value is surrounded by 
     * double quotes and the internal quotes are replaced by 2 double quotes.
     *
     * @return a Comma-Space-Separated-Value String representation 
     */
    public String toString() {
        Math2.ensureMemoryAvailable(8L * size, "StringArray.toString"); //8L is lame estimate of bytes/element
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(", ");
            String s = array[i];
            if (s == null) {
            } else if (s.indexOf('"') >= 0) { //check for '"' before check for ','
                s = String2.replaceAll(s, "\"", "\"\"");
                sb.append("\"" + s + "\"");
            } else if (s.indexOf(',') >= 0) {
                sb.append("\"" + s + "\"");
            } else {
                sb.append(s);
            }
        }
        return sb.toString(); 
    }

    /**
     * This overwrites the default PrimitiveArray.toJsonCsvString 
     * so that strings can be stored in "" with backslash encoding of special characters.
     *
     * @return a csv string of the elements.
     */
    public String toJsonCsvString() {
        Math2.ensureMemoryAvailable(8L * size, "StringArray.toJsonCsvString"); //8L is lame estimate of bytes/element
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(String2.toJson(array[i]));
        }
        return sb.toString(); 
    }


    /** 
     * This converts the elements into a newline-separated String.
     *
     * @return the newline-separated String representation of o
     */
    public String toNewlineString() {
        return String2.toNewlineString(toArray()); //toArray() get just 'size' elements
    }

    /** 
     * This sorts the elements in ascending order.
     * To get the elements in reverse order, just read from the end of the list
     * to the beginning.
     */
    public void sort() {
        Arrays.sort(array, 0, size);
    }

    /** 
     * This sorts the elements in ascending order regardless of the case of the letters.
     * To get the elements in reverse order, just read from the end of the list
     * to the beginning.
     * This is more sophisticated than Java's String.CASE_INSENSITIVE_ORDER
     */
    public void sortIgnoreCase() {
        Arrays.sort(array, 0, size, new StringComparatorIgnoreCase());
    }

    /**
     * This compares the values in row1 and row2 for SortComparator,
     * and returns a negative integer, zero, or a positive integer if the 
     * value at index1 is less than, equal to, or greater than 
     * the value at index2.
     * Currently, this does not checking of the range of index1 and index2,
     * so the caller should be careful.
     * Currently this uses String.compareTo, which may not be the desired comparison,
     * but which is easy to mimic in other situations.
     *
     * @param index1 an index number 0 ... size-1
     * @param index2 an index number 0 ... size-1
     * @return returns a negative integer, zero, or a positive integer if the 
     *   value at index1 is less than, equal to, or greater than 
     *   the value at index2.  
     *   Think "array[index1] - array[index2]".
     */
    public int compare(int index1, int index2) {
        return array[index1].compareTo(array[index2]);
    }

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
        String s1 = array[index1];
        String s2 = array[index2];
//        int c = s1.toUpperCase().compareTo(s2.toUpperCase());
        int c = s1.compareToIgnoreCase(s2);
        if (c != 0) 
            return c;
        return s1.compareTo(s2);
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
    public void copy(int from, int to) {
        array[to] = array[from];
    }

    /**
     * This reorders the values in 'array' based on rank.
     *
     * @param rank is an int with values (0 ... size-1) 
     * which points to the row number for a row with a specific 
     * rank (e.g., rank[0] is the row number of the first item 
     * in the sorted list, rank[1] is the row number of the
     * second item in the sorted list, ...).
     */
    public void reorder(int rank[]) {
        int n = rank.length;
        //new length could be n, but I'll keep it the same array.length as before
        String newArray[] = new String[array.length]; 
        for (int i = 0; i < n; i++)
            newArray[i] = array[rank[i]];
        array = newArray;
    }

    /**
     * This writes one element to a DataOutputStream via writeUTF.
     *
     * @param dos the DataOutputStream
     * @param i the index of the element to be written
     * @return the number of bytes used for this element
     *    (for Strings, this varies; for others it is consistent)
     * @throws Exception if trouble
     */
    public int writeDos(DataOutputStream dos, int i) throws Exception {
        int po = dos.size();
        dos.writeUTF(array[i]);
        return dos.size() - po;
    }

    /**
     * This reads/adds n elements from a DataInputStream.
     *
     * @param dis the DataInputStream
     * @param n the number of elements to be read/added
     * @throws Exception if trouble
     */
    public void readDis(DataInputStream dis, int n) throws Exception {
        ensureCapacity(size + (long)n);
        for (int i = 0; i < n; i++)
            array[size++] = String2.canonical(dis.readUTF());
    }

    /**
     * This writes one String to a DataOutputStream in the format DODS
     * wants (see www.opendap.org DAP 2.0 standard, section 7.3.2.1).
     * See also the XDR standard (http://tools.ietf.org/html/rfc4506#section-4.11).
     * Just 8 bits are stored: there is no utf or other unicode support.
     * See DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit.
     *
     * @param dos
     * @param s
     * @throws Exception if trouble
     */
    public static void externalizeForDODS(DataOutputStream dos, String s) throws Exception {
        int n = s.length();
        dos.writeInt(n); //for Strings, just write size once
        for (int i = 0; i < n; i++)
            dos.writeByte(s.charAt(i)); //eek; just 8 bits stored; no utf or other unicode support

        //pad to 4 bytes boundary at end
        while (n++ % 4 != 0)
            dos.writeByte(0);
    }

    /**
     * This writes all the data to a DataOutputStream in the
     * DODS Array format (see www.opendap.org DAP 2.0 standard, section 7.3.2.1).
     * See also the XDR standard (http://tools.ietf.org/html/rfc4506#section-4.11).
     *
     * @param dos
     * @throws Exception if trouble
     */
    public void externalizeForDODS(DataOutputStream dos) throws Exception {
        dos.writeInt(size);
        dos.writeInt(size); //yes, a second time
        for (int i = 0; i < size; i++)
            externalizeForDODS(dos, array[i]);
    }

    /**
     * This writes one element to a DataOutputStream in the
     * DODS Atomic-type format (see www.opendap.org DAP 2.0 standard, section 7.3.2).
     * See also the XDR standard (http://tools.ietf.org/html/rfc4506#section-4.11).
     *
     * @param dos
     * @param i the index of the element to be written
     * @throws Exception if trouble
     */
    public void externalizeForDODS(DataOutputStream dos, int i) throws Exception {
        externalizeForDODS(dos, array[i]);
    }

    /**
     * This reads/appends String values from a StringArray from a DODS DataInputStream,
     * and is thus the complement of externalizeForDODS.
     *
     * @param dis
     * @throws IOException if trouble
     */
    public void internalizeFromDODS(DataInputStream dis) throws java.io.IOException {
        int nStrings = dis.readInt();
        ensureCapacity(size + (long)nStrings);
        dis.readInt(); //skip duplicate of nStrings
        byte buffer[] = new byte[80];
        for (int i = 0; i < nStrings; i++) {
            int nChar = dis.readInt(); 
            if (buffer.length < nChar)
                buffer = new byte[nChar + 10];
            dis.readFully(buffer, 0, nChar);
            add(new String(buffer, 0, nChar));

            //pad to 4 bytes boundary at end
            while (nChar++ % 4 != 0)
                dis.readByte();
        }
    }

    /**
     * This reads one value from a randomAccessFile.
     *
     * @param raf the RandomAccessFile
     * @param start the raf offset of the start of the array
     * @param index the index of the desired value (0..)
     * @param nBytesPer is the number of bytes per string
     * @return the requested value as a double
     * @throws Exception if trouble
     */
    public static String rafReadString(RandomAccessFile raf, long start, int index, 
        int nBytesPer) throws Exception {

        raf.seek(start + nBytesPer * index);
        byte bar[] = new byte[nBytesPer];
        raf.readFully(bar);      
        int po = 0;
        while (po < nBytesPer && bar[po] != 0) 
            po++;
        return new String(bar, 0, po);
    }

    /**
     * This appends the data in another primitiveArray to the current data.
     *
     * @param primitiveArray 
     */
    public void append(PrimitiveArray primitiveArray) {
        int otherSize = primitiveArray.size(); //this avoids infinite loop if primitiveArray == this
        ensureCapacity(size + (long)otherSize);
        if (primitiveArray instanceof StringArray) {
            System.arraycopy(((StringArray)primitiveArray).array, 0, array, size, otherSize);
        } else {
            for (int i = 0; i < otherSize; i++)
                array[size + i] = String2.canonical(primitiveArray.getString(i)); //this converts mv's
        }
        size += otherSize; //do last to minimize concurrency problems
    }    

    /**
     * This returns the length of the longest String.
     *
     * @return the length of the longest String
     */
    public int maxStringLength() {
        int max = 0;
        for (int i = 0; i < size; i++)
            max = Math.max(max, array[i] == null? 0 : array[i].length());
        return max;
    }

    /**
     * This populates 'indices' with the indices (ranks) of the values in this StringArray
     * (ties get the same index). For example, "d", "d", "", "c" returns 1,1,2,0.
     * !!!Currently this uses native sort, so lower case sorts before uppercase;
     * except "" is ranked at end (like missing value).
     *
     * @param indices the intArray that will capture the indices of the values 
     *  (ties get the same index). For example, "d", "d", "", "c" returns 1,1,2,0.
     * @return a PrimitveArray (the same type as this class) with the distinct/unique values, sorted.
     *     If all the values are unique and already sorted, this returns 'this'.
     */
    public PrimitiveArray makeIndices(IntArray indices) {
        indices.clear();
        if (size == 0) {
            return new StringArray();
        }

        //make a hashMap with all the unique values (associated values are initially all dummy)
        Integer dummy = new Integer(-1);
        HashMap hashMap = new HashMap(Math2.roundToInt(1.4 * size));
        String lastValue = array[0]; //since lastValue often equals currentValue, cache it
        hashMap.put(lastValue, dummy);   //special for String
        boolean alreadySorted = true;
        for (int i = 1; i < size; i++) {
            String currentValue = array[i];
            int compare = lastValue.compareTo(currentValue); //special for String,    read "is bigger than"
            if (compare != 0) {    //special for String
                if (compare > 0)   //special for String
                    alreadySorted = false;
                lastValue = currentValue;
                hashMap.put(lastValue, dummy);
            }
        }

        //quickly deal with: all unique and already sorted
        Set keySet = hashMap.keySet();
        int nUnique = keySet.size();
        if (nUnique == size && alreadySorted) {
            indices.ensureCapacity(size);
            for (int i = 0; i < size; i++)
                indices.add(i);           
            //String2.log("StringArray.makeIndices all unique and already sorted.");
            return this; //the PrimitiveArray with unique values
        }

        //store all the elements in an array
        String unique[] = new String[nUnique];
        Iterator iterator = keySet.iterator();
        int count = 0;
        while (iterator.hasNext())
            unique[count++] = (String)iterator.next();
        Test.ensureEqual(nUnique, count, "StringArray.makeRankArray nUnique != count!");

        //sort them
        Arrays.sort(unique); //a variant could use new StringComparatorIgnoreCase());

        //special for StringArray: "" (missing value) sorts highest
        if (((String)unique[0]).length() == 0) {
            System.arraycopy(unique, 1, unique, 0, nUnique - 1);
            unique[nUnique - 1] = "";
        }

        //put the unique values back in the hashMap with the ranks as the associated values
        for (int i = 0; i < count; i++)
            hashMap.put(unique[i], new Integer(i));

        //convert original values to ranks
        int ranks[] = new int[size];
        lastValue = array[0];
        ranks[0] = ((Integer)hashMap.get(lastValue)).intValue();
        int lastRank = ranks[0];
        for (int i = 1; i < size; i++) {
            if (array[i] == lastValue) {
                ranks[i] = lastRank;
            } else {
                lastValue = array[i];
                ranks[i] = ((Integer)hashMap.get(lastValue)).intValue();
                lastRank = ranks[i];
            }
        }

        //store the results in ranked
        indices.append(new IntArray(ranks));

        return new StringArray(unique);

    }

    /**
     * This changes all instances of the first value to the second value.
     *
     * @param from the original value (use "" (not "NaN") for standard missingValue)
     * @param to   the new value (use "" (not "NaN") for standard missingValue)
     * @return the number of values switched
     */
    public int switchFromTo(String from, String to) {
        if (from.equals(to))
            return 0;
        to = String2.canonical(to);
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (array[i].equals(from)) {
                array[i] = to;
                count++;
            }
        }
        return count;
    }

    /**
     * This makes a StringArray with the words and double-quoted phrases from searchFor
     * (which are separated by white space; commas are treated like any other whitespace).
     *
     * @param searchFor
     * @return a StringArray with the words and phrases (no longer double quoted) from searchFor
     *   (which are separated by white space; commas are treated like any other whitespace).
     *   Interior double quotes in double-quoted phrases must be doubled 
     *   (e.g., "a quote "" within a phrase").
     */
    public static StringArray wordsAndQuotedPhrases(String searchFor) {
        StringArray sa = new StringArray();
        if (searchFor == null)
            return sa;
        int po = 0;
        int n = searchFor.length();
        while (po < n) {
            char ch = searchFor.charAt(po);
            if (ch == '"') {
                //a phrase (a quoted string)
                int po2 = po + 1;
                while (po2 < n) {
                    if (searchFor.charAt(po2) == '"') {
                        //is it 2 double quotes?
                        if (po2 + 1 < n && searchFor.charAt(po2+1) == '"') 
                            po2 += 2; //yes, so continue
                        else break; //no, it's the end quote
                    } else {
                        po2++;
                    }
                }
                String s = searchFor.substring(po + 1, po2);
                sa.add(String2.replaceAll(s, "\"\"", "\""));
                po = po2 + 1;
            } else if (String2.isWhite(ch) || ch == ',') {
                //whitespace or comma
                po++;
            } else {
                //a word
                int po2 = po + 1;
                while (po2 < searchFor.length() && 
                    !String2.isWhite(searchFor.charAt(po2)) &&
                    searchFor.charAt(po2) != ',')
                    po2++;
                //String2.log("searchFor=" + searchFor + " wordPo=" + po + " po2=" + po2);
                sa.add(searchFor.substring(po, po2));
                po = po2;
            }
        }
        return sa;
    }


    /**
     * This makes a StringArray with the comma-separated words and double-quoted phrases from searchFor.
     * <br>The double-quoted phrases can have internal double quotes encoded as "" or \".
     * <br>null becomes sa.length() == 0.
     * <br>"" becomes sa.length() == 0.
     * <br>" " becomes sa.length() == 1.
     *
     * @param searchFor
     * @return a StringArray with the words and double-quoted phrases from searchFor.
     *    The items are trim'd.
     */
    public static StringArray fromCSV(String searchFor) {
        return new StringArray(arrayFromCSV(searchFor));
    }

    /**
     * This is like fromCSV, but with any "" elements removed.
     *
     * @param searchFor
     * @return a StringArray with the words and double-quoted phrases from searchFor.
     *    The items are trim'd.
     */
    public static StringArray fromCSVNoBlanks(String searchFor) {
        StringArray sa = new StringArray(arrayFromCSV(searchFor));
        int tSize = sa.size;
        String tArray[] = sa.array;
        BitSet bitset = new BitSet(tSize); //initially all false
        for (int i = 0; i < tSize; i++)
            if (tArray[i].length() > 0) 
                bitset.set(i);
        sa.justKeep(bitset);
        return sa;
    }

    /**
     * This makes a String[] with the comma-separated words and double-quoted phrases from searchFor.
     * <br>This avoids String2.canonical(to), so will be faster if just parsing then discarding
     *   or storing in some other data structure.
     *
     * <p>The double-quoted phrases can have internal double quotes encoded as "" or \".
     * <br>null becomes sa.length() == 0.
     * <br>"" becomes sa.length() == 0.
     * <br>" " becomes sa.length() == 1.
     *
     * @param searchFor
     * @return a String[] with the words and double-quoted phrases from searchFor.
     *    The items are trim'd.
     *   <br>Note that null and "null" return the word "null". No returned element will be null.
     *   <br>backslashed characters are converted to the special character (e.g., double quotes or newline).
     */
    public static String[] arrayFromCSV(String searchFor) {
        if (searchFor == null)
            return new String[0];
        ArrayList al = new ArrayList();
        int po = 0; //next char to be looked at
        StringBuilder word = new StringBuilder();
        int n = searchFor.length();
        while (po < n) {
            char ch = searchFor.charAt(po++);

            if (ch == '"') {
                //a quoted string
                if (word.length() == 0)
                    word.append(' '); //indicate there is something

                int start = po;
                if (po < n) {
                    while (true) {
                        ch = searchFor.charAt(po++);

                        // "" internal quote
                        if (ch == '"' && po < n && searchFor.charAt(po) == '"') {
                            word.append(searchFor.substring(start, po - 1));
                            start = po++;  //the 2nd char " will be the first appended later

                        // backslashed character
                        } else if (ch == '\\' && po < n) {
                            word.append(searchFor.substring(start, po - 1));
                            ch = searchFor.charAt(po++);
                            if      (ch == 't') word.append('\t');
                            else if (ch == 'n') word.append('\n');
                            else if (ch == '\'') word.append('\'');
                            else if (ch == '\"') word.append('\"');
                            else if (ch == '\\') word.append('\\');
                            //else if (ch == '') word.append('');  support \\uxxxx?
                            else word.append("\\" + ch); //or just ch?
                            start = po;  //next char will be the first appended later

                        // the end of the quoted string?
                        } else if (ch == '"') { 
                            word.append(searchFor.substring(start, po - 1));
                            break;

                        // the end of searchFor?
                        } else if (po == n) { 
                            word.append(searchFor.substring(start, po));
                            break;

                        // a letter in the quoted string
                        //} else {
                        //    word.append(ch);
                        }
                    }
                }

            //end of word?
            } else if (ch == ',') {
                al.add(word.toString().trim());
                word.setLength(0);
                word.append(' '); //indicate there is something

            //a character
            } else {
                word.append(ch);
            }
        }
        if (word.length() > 0)
            al.add(word.toString().trim());
        return String2.toStringArray(al.toArray());
    }

    
    /**
     * This makes a StringArray from the comma separated list of strings in csv. 
     * If a string has an internal comma or double quotes, it must have double quotes 
     * at the beginning and end and the internal double quotes must be doubled; otherwise it doesn't.
     *
     * @param csv  e.g., "He said, ""Hi"".", 2nd phrase, "3rd phrase"
     * @return a StringArray with the strings (trimmed) from csv.
     *   csv=null returns StringArray of length 0.
     *   csv="" returns StringArray of length 0.
     */
    /* not bad, but doesn't support \" encoding of internal quote
    public static StringArray fromCSV(String csv) {
        StringArray sa = new StringArray();
        if (csv == null || csv.length() == 0)
            return sa;
        int n = csv.length();
        if (n == 0)
            return sa;
        int po = 0;
        boolean something = false;

        while (po < n) {
            char ch = csv.charAt(po);
            if (ch == ' ') {
                po++;
                something = true;
            } else if (ch == ',') {
                sa.add("");
                po++;
                something = true;
            } else if (ch == '"') {
                //a quoted string
                int po2 = po + 1;
                while (po2 < n) {
                    if (csv.charAt(po2) == '"') {
                        //is it 2 double quotes?
                        if (po2 + 1 < n && csv.charAt(po2+1) == '"') 
                            po2 += 2; //yes, so continue
                        else break; //no, it's the end quote; what if next thing isn't a comma???
                    } else {
                        po2++;
                    }
                }
                String s = csv.substring(po + 1, po2);
                sa.add(String2.replaceAll(s, "\"\"", "\""));

                //should be only spaces till next comma
                //whatever it is, trash it
                po = po2 + 1;
                while (po < n && csv.charAt(po) != ',')
                    po++;
                if (po < n && csv.charAt(po) == ',') {
                    po++; something = true;
                } else {po = n; something = false;
                }
            } else {
                //an unquoted string
                int po2 = csv.indexOf(',', po + 1);
                if (po2 >= 0) {something = true;
                } else {po2 = n; something = false;
                }
                sa.add(csv.substring(po, po2).trim());
                po = po2 + 1;
            }
        }
        if (something)
            sa.add("");
        return sa;
    } */

    /**
     * This tests if the values in the array are sorted in ascending order (tied is ok).
     * The details of this test are geared toward determining if the 
     * values are suitable for binarySearch.
     *
     * @return "" if the values in the array are sorted in ascending order (or tied);
     *   or an error message if not (i.e., if descending or unordered).
     *   If size is 0 or 1 (non-missing value), this returns "".
     *   A null value returns an error message (but "" is ok).
     */
    public String isAscending() {
        if (size == 0)
            return "";
        if (array[0] == null) 
            return MessageFormat.format(ArrayNotAscending, getClass().getSimpleName(),
                "[0]=null");
        for (int i = 1; i < size; i++) {
            if (array[i] == null)
                return MessageFormat.format(ArrayNotAscending, getClass().getSimpleName(),
                    "[" + i + "]=null");
            if (array[i - 1].compareTo(array[i]) > 0) {
                return MessageFormat.format(ArrayNotAscending, getClass().getSimpleName(),
                    "[" + (i-1) + "]=" + String2.toJson(array[i-1]) + 
                     " > [" + i + "]=" + String2.toJson(array[i]));
            }
        }
        return "";
    }

    /**
     * This tests if the values in the array are sorted in descending order (tied is ok).
     *
     * @return "" if the values in the array are sorted in descending order (or tied);
     *   or an error message if not (i.e., if ascending or unordered).
     *   If size is 0 or 1 (non-missing value), this returns "".
     *   A null value returns an error message (but "" is ok).
     */
    public String isDescending() {
        if (size == 0)
            return "";
        if (array[0] == null) 
            return MessageFormat.format(ArrayNotDescending, getClass().getSimpleName(), 
                "[0]=null");
        for (int i = 1; i < size; i++) {
            if (array[i] == null)
                return MessageFormat.format(ArrayNotDescending, getClass().getSimpleName(), 
                    "[" + i + "]=null");
            if (array[i - 1].compareTo(array[i]) < 0) {
                return MessageFormat.format(ArrayNotDescending, getClass().getSimpleName(), 
                    "[" + (i-1) + "]=" + String2.toJson(array[i-1]) + 
                     " < [" + i + "]=" + String2.toJson(array[i]));
            }
        }
        return "";
    }

    /**
     * This tests for adjacent tied values and returns the index of the first tied value.
     * Adjacent NaNs are treated as ties.
     *
     * @return the index of the first tied value (or -1 if none).
     */
    public int firstTie() {
        for (int i = 1; i < size; i++) {
            if (array[i - 1] == null) {
                if (array[i] == null)
                    return i - 1;
            } else if (array[i - 1].equals(array[i])) {
                return i - 1;
            }
        }
        return -1;
    }

    /**
     * This tests if the values in the array are evenly spaced (ascending or descending)
     * (via Math2.almostEqual(9)).
     * This is rarely used because numbers are usually stored in numeric
     * XXXArrays.
     *
     * @return "" if the values in the array are evenly spaced;
     *   or an error message if not.
     *   If size is 0 or 1, this returns "".
     */
    public String isEvenlySpaced() {
        if (size <= 2)
            return "";
        //average is closer to exact than first diff
        //and usually detects not-evenly-spaced anywhere in the array on first test!
        double average = (getDouble(size - 1) - getDouble(0)) / (size - 1.0); 
        for (int i = 1; i < size; i++) {
            if (!Math2.almostEqual(9, getDouble(i) - getDouble(i - 1), average)) {
                return MessageFormat.format(ArrayNotEvenlySpaced, getClass().getSimpleName(),
                    "" + (i - 1), "" + getDouble(i - 1), "" + i, "" + getDouble(i),
                    "" + (getDouble(i) - getDouble(i-1)), "" + average);
            }
        }
        return "";
    }

    /** Thie replaces any instances of 'from' with 'to' within each string. */
    public void intraReplaceAll(String from, String to) {
        for (int i = 0; i < size; i++)
            if (array[i] != null) 
                array[i] = String2.canonical(String2.replaceAll(array[i], from, to));
    }

    /** Thie replaces any instances of 'from' with 'to' within each string, 
     * regardless of 'from's case in the string. */
    public void intraReplaceAllIgnoreCase(String from, String to) {
        for (int i = 0; i < size; i++)
            if (array[i] != null) 
                array[i] = String2.canonical(String2.replaceAllIgnoreCase(array[i], from, to));
    }

    /** This returns the minimum value that can be held by this class. */
    public String minValue() {return "\u0000";}

    /** This returns the maximum value that can be held by this class. */
    public String maxValue() {return "\uFFFF";}

    /**
     * This finds the number of non-missing values, and the index of the min and
     *    max value.
     *
     * @return int[3], [0]=the number of non-missing values, 
     *    [1]=index of min value (if tie, index of last found; -1 if all mv),
     *    [2]=index of max value (if tie, index of last found; -1 if all mv).
     */
    public int[] getNMinMaxIndex() {
        int n = 0, tmini = -1, tmaxi = -1;
        String tmin = "\uFFFF";
        String tmax = "\u0000";
        for (int i = 0; i < size; i++) {
            String s = array[i];
            if (s != null && s.length() > 0) {
                n++;
                if (s.compareTo(tmin) <= 0) {tmini = i; tmin = s; }
                if (s.compareTo(tmax) >= 0) {tmaxi = i; tmax = s; }
            }
        }
        return new int[]{n, tmini, tmaxi};
    }

    /**
     * This returns the min and max of the non-null or "" strings (by simple comparison).
     *
     * @return String[3], 0=""+n (the number of non-null or "" strings), 
     *    1=min, 2=max.  min and max are "" if n=0.
     */
    public String[] getNMinMax() {
        int nmm[] = getNMinMaxIndex();
        if (nmm[0] == 0)
            return new String[]{"0", "", ""};
        return new String[]{"" + nmm[0], array[nmm[1]], array[nmm[2]]};
    }

    /**
     * This compares two text files, line by line, and throws Exception indicating 
     * line where different.
     * nullString == nullString is ok.
     *  
     * @param fileName1 a complete file name
     * @param fileName2 a complete file name
     * @throws Exception if files are different
     */
    public static void diff(String fileName1, String fileName2) throws Exception {
        StringArray sa1 = fromFile(fileName1);
        StringArray sa2 = fromFile(fileName2);
        sa1.diff(sa2);
    }

    /**
     * This repeatedly compares two text files, line by line, and throws Exception indicating 
     * line where different.
     * nullString == nullString is ok.
     *  
     * @param fileName1 a complete file name
     * @param fileName2 a complete file name
     * @throws Exception if files are different
     */
    public static void repeatedDiff(String fileName1, String fileName2) throws Exception {
        while (true) {
            try {
                String2.log("\nComparing " + fileName1 + 
                            "\n      and " + fileName2);
                StringArray sa1 = fromFile(fileName1);
                StringArray sa2 = fromFile(fileName2);
                sa1.diff(sa2);
                String2.log("!!! The files are the same!!!");
                break;
            } catch (Exception e) {
                String2.getStringFromSystemIn(MustBe.throwableToString(e) + 
                    "\nPress ^C to stop or Enter to compare the files again..."); 
            }
        }
    }



    /**
     * This tests the methods of this class.
     *
     * @throws Throwable if trouble.
     */
    public static void test() throws Throwable{
        String2.log("*** Testing StringArray");

        //** test default constructor and many of the methods
        StringArray anArray = new StringArray();
        Test.ensureEqual(anArray.size(), 0, "");
        anArray.add("1234.5");
        Test.ensureEqual(anArray.size(), 1, "");
        Test.ensureEqual(anArray.get(0), "1234.5", "");
        Test.ensureEqual(anArray.getInt(0), 1235, "");
        Test.ensureEqual(anArray.getFloat(0), 1234.5f, "");
        Test.ensureEqual(anArray.getDouble(0), 1234.5, "");
        Test.ensureEqual(anArray.getString(0), "1234.5", "");
        Test.ensureEqual(anArray.elementClass(), String.class, "");
        String tArray[] = anArray.toArray();
        Test.ensureEqual(tArray, new String[]{"1234.5"}, "");

        //intentional errors
        try {anArray.get(1);              throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.set(1, "100");       throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.getInt(1);           throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.setInt(1, 100);      throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.getLong(1);          throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.setLong(1, 100);     throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.getFloat(1);         throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.setFloat(1, 100);    throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.getDouble(1);        throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.setDouble(1, 100);   throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.getString(1);        throw new Throwable("It should have failed.");} catch (Exception e) {}
        try {anArray.setString(1, "100"); throw new Throwable("It should have failed.");} catch (Exception e) {}

        //set NaN returned as NaN
        anArray.setDouble(0, Double.NaN);   Test.ensureEqual(anArray.getDouble(0), Double.NaN, ""); 
        anArray.setDouble(0, -1e300);       Test.ensureEqual(anArray.getDouble(0), -1e300, ""); 
        anArray.setDouble(0, 2.2);          Test.ensureEqual(anArray.getDouble(0), 2.2,          ""); 
        anArray.setFloat( 0, Float.NaN);    Test.ensureEqual(anArray.getFloat(0),  Float.NaN,  ""); 
        anArray.setFloat( 0, -1e33f);       Test.ensureEqual(anArray.getFloat(0),  -1e33f,  ""); 
        anArray.setFloat( 0, 3.3f);         Test.ensureEqual(anArray.getFloat(0),  3.3f,          ""); 
        anArray.setLong(0, Long.MAX_VALUE); Test.ensureEqual(anArray.getLong(0),   Long.MAX_VALUE, ""); 
        anArray.setLong(0, 9123456789L);    Test.ensureEqual(anArray.getLong(0),   9123456789L, ""); 
        anArray.setLong(0, 4);              Test.ensureEqual(anArray.getLong(0),   4, ""); 
        anArray.setInt(0,Integer.MAX_VALUE);Test.ensureEqual(anArray.getInt(0),    Integer.MAX_VALUE, ""); 
        anArray.setInt(0, 1123456789);      Test.ensureEqual(anArray.getInt(0),    1123456789, ""); 
        anArray.setInt(0, 5);               Test.ensureEqual(anArray.getInt(0),    5, ""); 

        //** test capacity constructor, test expansion, test clear
        anArray = new StringArray(2, false);
        Test.ensureEqual(anArray.size(), 0, "");
        for (int i = 0; i < 10; i++) {
            anArray.add(String.valueOf(i));   
            Test.ensureEqual(anArray.get(i), "" + i, "");
            Test.ensureEqual(anArray.size(), i+1, "");
        }
        Test.ensureEqual(anArray.size(), 10, "");
        anArray.clear();
        Test.ensureEqual(anArray.size(), 0, "");

        //active
        anArray = new StringArray(3, true);
        Test.ensureEqual(anArray.size(), 3, "");
        Test.ensureEqual(anArray.get(2), "", "");

        //** test array constructor
        anArray = new StringArray(new String[]{"0","2","4","6","8"});
        Test.ensureEqual(anArray.size(), 5, "");
        Test.ensureEqual(anArray.get(0), "0", "");
        Test.ensureEqual(anArray.get(1), "2", "");
        Test.ensureEqual(anArray.get(2), "4", "");
        Test.ensureEqual(anArray.get(3), "6", "");
        Test.ensureEqual(anArray.get(4), "8", "");

        //test compare
        Test.ensureEqual(anArray.compare(1, 3), -4, "");
        Test.ensureEqual(anArray.compare(1, 1),  0, "");
        Test.ensureEqual(anArray.compare(3, 1),  4, "");

        //test compareIgnoreCase
        StringArray cic = fromCSV("A, a, ABE, abe");
        Test.ensureEqual(cic.compareIgnoreCase(0, 1), -32, "");
        Test.ensureEqual(cic.compareIgnoreCase(1, 2), -2, "");
        Test.ensureEqual(cic.compareIgnoreCase(2, 3), -32, "");

        //test toString
        Test.ensureEqual(anArray.toString(), "0, 2, 4, 6, 8", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{5, 0, 4}, "");
        Test.ensureEqual(anArray.getNMinMax(), new String[]{"5", "0", "8"}, "");

        //test calculateStats
        anArray.addString("");
        double stats[] = anArray.calculateStats();
        anArray.remove(5);
        Test.ensureEqual(stats[STATS_N], 5, "");
        Test.ensureEqual(stats[STATS_MIN], 0, "");
        Test.ensureEqual(stats[STATS_MAX], 8, "");
        Test.ensureEqual(stats[STATS_SUM], 20, "");

        //test indexOf(int) indexOf(String)
        Test.ensureEqual(anArray.indexOf("0", 0),  0, "");
        Test.ensureEqual(anArray.indexOf("0", 1), -1, "");
        Test.ensureEqual(anArray.indexOf("8", 0),  4, "");
        Test.ensureEqual(anArray.indexOf("9", 0), -1, "");

        //test lastIndexOf
        Test.ensureEqual(anArray.lastIndexOf("0", 0),  0, "");
        Test.ensureEqual(anArray.lastIndexOf("0", 0),  0, "");
        Test.ensureEqual(anArray.lastIndexOf("8", 4),  4, "");
        Test.ensureEqual(anArray.lastIndexOf("6", 2), -1, "");
        Test.ensureEqual(anArray.lastIndexOf("6", 3),  3, "");
        Test.ensureEqual(anArray.lastIndexOf("9", 2), -1, "");

        //test remove
        anArray.remove(1);
        Test.ensureEqual(anArray.size(), 4, "");
        Test.ensureEqual(anArray.get(0), "0", "");
        Test.ensureEqual(anArray.get(1), "4", "");
        Test.ensureEqual(anArray.get(3), "8", "");
        Test.ensureEqual(anArray.array[4], null, ""); //can't use get()

        //test add(index, value)    and maxStringLength
        anArray.add(1, "22");
        Test.ensureEqual(anArray.size(), 5, "");
        Test.ensureEqual(anArray.get(0), "0", "");
        Test.ensureEqual(anArray.get(1),"22", "");
        Test.ensureEqual(anArray.get(2), "4", "");
        Test.ensureEqual(anArray.get(4), "8", "");
        Test.ensureEqual(anArray.maxStringLength(), 2, "");
        anArray.remove(1);
        Test.ensureEqual(anArray.maxStringLength(), 1, "");

        //test removeRange
        anArray.removeRange(4, 4); //make sure it is allowed
        anArray.removeRange(1, 3);
        Test.ensureEqual(anArray.size(), 2, "");
        Test.ensureEqual(anArray.get(0), "0", "");
        Test.ensureEqual(anArray.get(1), "8", "");
        Test.ensureEqual(anArray.array[2], null, ""); //can't use get()
        Test.ensureEqual(anArray.array[3], null, "");

        //test (before trimToSize) that toString, toDoubleArray, and toStringArray use 'size'
        Test.ensureEqual(anArray.toString(), "0, 8", "");
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{0, 8}, "");
        Test.ensureEqual(anArray.toStringArray(), new String[]{"0", "8"}, "");

        //test trimToSize
        anArray.trimToSize();
        Test.ensureEqual(anArray.array.length, 2, "");

        //test equals
        StringArray anArray2 = new StringArray();
        anArray2.add("0"); 
        Test.ensureEqual(anArray.testEquals("A String"), 
            "The two objects aren't equal: this object is a StringArray; the other is a java.lang.String.", "");
        Test.ensureEqual(anArray.testEquals(anArray2), 
            "The two StringArrays aren't equal: one has 2 values (from \"0\" to \"8\"); the other has 1 value (\"0\").", "");
        Test.ensureTrue(!anArray.equals(anArray2), "");
        anArray2.addString("7");
        Test.ensureEqual(anArray.testEquals(anArray2), 
            "The two StringArrays aren't equal: this[1]=\"8\"; other[1]=\"7\".", "");
        Test.ensureTrue(!anArray.equals(anArray2), "");
        anArray2.setString(1, "8");
        Test.ensureEqual(anArray.testEquals(anArray2), "", "");
        Test.ensureTrue(anArray.equals(anArray2), "");

        //test toObjectArray
        Test.ensureEqual(anArray.toArray(), anArray.toObjectArray(), "");

        //test toDoubleArray
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{0, 8}, "");

        //test reorder
        int rank[] = {1, 0};
        anArray.reorder(rank);
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{8, 0}, "");

        //** test append and clone
        anArray = new StringArray(new String[]{"1"});
        anArray.append(new ByteArray(new byte[]{5, -5}));
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{1, 5, -5}, "");
        anArray.append(new StringArray(new String[]{"a", "9"}));
        Test.ensureEqual(anArray.toDoubleArray(), new double[]{1, 5, -5, Double.NaN, 9}, "");
        anArray2 = (StringArray)anArray.clone();
        Test.ensureEqual(anArray2.toDoubleArray(), new double[]{1, 5, -5, Double.NaN, 9}, "");

        //test move
        anArray = new StringArray(new String[]{"0","1","2","3","4"});
        anArray.move(1,3,0);
        Test.ensureEqual(anArray.toArray(), new String[]{"1","2","0","3","4"}, "");

        anArray = new StringArray(new String[]{"0","1","2","3","4"});
        anArray.move(1,2,4);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","2","3","1","4"}, "");

        //test reorder
        anArray.reverse();
        Test.ensureEqual(anArray.toArray(), new String[]{"4","1","3","2","0"}, "");


        //move does nothing, but is allowed
        anArray = new StringArray(new String[]{"0","1","2","3","4"});
        anArray.move(1,1,0);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","1","2","3","4"}, "");
        anArray.move(1,2,1);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","1","2","3","4"}, "");
        anArray.move(1,2,2);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","1","2","3","4"}, "");
        anArray.move(5,5,0);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","1","2","3","4"}, "");
        anArray.move(3,5,5);
        Test.ensureEqual(anArray.toArray(), new String[]{"0","1","2","3","4"}, "");

        //makeIndices
        anArray = new StringArray(new String[] {"d", "a", "a", "b"});
        IntArray indices = new IntArray();
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "a, b, d", "");
        Test.ensureEqual(indices.toString(),  "2, 0, 0, 1", "");

        anArray = new StringArray(new String[] {"d", "d", "a", "", "b",});
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "a, b, d, ", "");
        Test.ensureEqual(indices.toString(), "2, 2, 0, 3, 1", "");

        anArray = new StringArray(new String[] {"aa", "ab", "ac", "ad"});
        Test.ensureEqual(anArray.makeIndices(indices).toString(), "aa, ab, ac, ad", "");
        Test.ensureEqual(indices.toString(), "0, 1, 2, 3", "");

        //switchToFakeMissingValue
        anArray = new StringArray(new String[] {"", "1", "2", "", "3", ""});
        Test.ensureEqual(anArray.switchFromTo("", "75"), 3, "");
        Test.ensureEqual(anArray.toString(), "75, 1, 2, 75, 3, 75", "");
        anArray.switchFromTo("75", "");
        Test.ensureEqual(anArray.toString(), ", 1, 2, , 3, ", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{3, 1, 4}, "");

        //addN
        anArray = new StringArray(new String[] {"a"});
        anArray.addN(2, "bb");
        Test.ensureEqual(anArray.toString(), "a, bb, bb", "");
        Test.ensureEqual(anArray.getNMinMaxIndex(), new int[]{3, 0, 2}, "");

        //add array
        anArray.add(new String[]{"17", "19"});
        Test.ensureEqual(anArray.toString(), "a, bb, bb, 17, 19", "");

        //subset
        PrimitiveArray ss = anArray.subset(1, 3, 4);
        Test.ensureEqual(ss.toString(), "bb, 19", "");
        ss = anArray.subset(0, 1, 0);
        Test.ensureEqual(ss.toString(), "a", "");
        ss = anArray.subset(0, 1, -1);
        Test.ensureEqual(ss.toString(), "", "");
        ss = anArray.subset(1, 1, 0);
        Test.ensureEqual(ss.toString(), "", "");

        //wordsAndQuotedPhrases(String searchFor)
        Test.ensureEqual(wordsAndQuotedPhrases(null).toString(), "", "");
        Test.ensureEqual(wordsAndQuotedPhrases("a bb").toString(), "a, bb", "");
        Test.ensureEqual(wordsAndQuotedPhrases(" a bb c ").toString(), "a, bb, c", "");
        Test.ensureEqual(wordsAndQuotedPhrases(",a,bb, c ,d").toString(), "a, bb, c, d", "");
        Test.ensureEqual(wordsAndQuotedPhrases(" a,\"b b\",c ").toString(), "a, b b, c", "");
        Test.ensureEqual(wordsAndQuotedPhrases(" a,\"b b").toString(), "a, b b", ""); //no error for missing "
        Test.ensureEqual(wordsAndQuotedPhrases(" , ").toString(), "", "");
        anArray = wordsAndQuotedPhrases(" a,\"b\"\"b\",c "); //internal quotes
        Test.ensureEqual(anArray.toString(), "a, \"b\"\"b\", c", "");
        Test.ensureEqual(anArray.get(1), "b\"b", "");
        anArray = wordsAndQuotedPhrases(" a \"b\"\"b\" c "); //internal quotes
        Test.ensureEqual(anArray.toString(), "a, \"b\"\"b\", c", "");
        Test.ensureEqual(anArray.get(1), "b\"b", "");
        anArray = wordsAndQuotedPhrases("a \"-bob\" c");
        Test.ensureEqual(anArray.get(1), "-bob", "");
        anArray = wordsAndQuotedPhrases("a -\"bob\" c"); //internal quotes
        Test.ensureEqual(anArray.get(1), "-\"bob\"", "");

        //fromCSV(String searchFor)
        Test.ensureEqual(fromCSV(null).toString(), "", "");
        Test.ensureEqual(fromCSV("a, b b").toString(), "a, b b", "");
        Test.ensureEqual(fromCSV(" a, b b ,c ").toString(), "a, b b, c", "");
        Test.ensureEqual(fromCSV(",a,b b, c ,d,").toString(), ", a, b b, c, d, ", "");
        Test.ensureEqual(fromCSV(" a, \"b b\" ,c ").toString(), "a, b b, c", "");
        Test.ensureEqual(fromCSV(" a,\"b b").toString(), "a, b b", ""); //no error for missing "
        Test.ensureEqual(fromCSV(" , ").toString(), ", ", "");
        anArray = fromCSV(" a,\"b\"\"b\",c "); Test.ensureEqual(anArray.get(1), "b\"b", ""); //internal quotes
        anArray = fromCSV(" a,\"b\\\"b\",c "); Test.ensureEqual(anArray.get(1), "b\"b", ""); //internal quotes
        anArray = fromCSV(" a,\"b\\tb\",c ");  Test.ensureEqual(anArray.get(1), "b\tb", ""); //internal quotes
        anArray = fromCSV(" a,\"b\\nb\",c ");  Test.ensureEqual(anArray.get(1), "b\nb", ""); //internal quotes
        anArray = fromCSV(" a,\"b\\'b\",c ");  Test.ensureEqual(anArray.get(1), "b\'b", ""); //internal quotes
        anArray = fromCSV(" a,\"b\\\"b\",c "); Test.ensureEqual(anArray.get(1), "b\"b", ""); //internal quotes
        anArray = fromCSV(" a \"b\"\"b\" c "); Test.ensureEqual(anArray.get(0), "a b\"b c", ""); //internal quotes

        //evenlySpaced
        anArray = new StringArray(new String[] {"10","20","30"});
        Test.ensureEqual(anArray.isEvenlySpaced(), "", "");
        anArray.set(2, "30.1");
        Test.ensureEqual(anArray.isEvenlySpaced(), 
            "StringArray isn't evenly spaced: [0]=10.0, [1]=20.0, spacing=10.0, average spacing=10.05.", "");
        Test.ensureEqual(anArray.smallestBiggestSpacing(),
            "  smallest spacing=10.0: [0]=10.0, [1]=20.0\n" +
            "  biggest  spacing=10.100000000000001: [1]=20.0, [2]=30.1", "");

        //fromCSV
        Test.ensureEqual(fromCSV(null).toString(), "", "");
        Test.ensureEqual(fromCSV("").toString(), "", "");
        Test.ensureEqual(fromCSV(" ").toString(), "", "");
        Test.ensureEqual(fromCSV(",").toString(), ", ", "");
        Test.ensureEqual(fromCSV(" , ").toString(), ", ", "");
        Test.ensureEqual(fromCSV("a,bb").toString(), "a, bb", "");
        Test.ensureEqual(fromCSV(" a , bb ").toString(), "a, bb", "");
        Test.ensureEqual(fromCSV(" a, bb ,c ").toString(), "a, bb, c", "");
        Test.ensureEqual(fromCSV(",a,bb, c ,").toString(), ", a, bb, c, ", "");
        Test.ensureEqual(fromCSV(" a,\"b b\",c ").toString(), "a, b b, c", "");
        Test.ensureEqual(fromCSV(" a,\"b b").toString(), "a, b b", ""); //no error for missing "
        Test.ensureEqual(fromCSV(" a,\"b \"\"\"\"b\"junk,c ").toString(), "a, \"b \"\"\"\"bjunk\", c", "");
        Test.ensureEqual(fromCSV(" a,\"b \"\"\"\"b\"junk,c ").get(1), "b \"\"bjunk", "");
        Test.ensureEqual(fromCSV(" a,\"b,b\"junk").toString(), "a, \"b,bjunk\"", "");

        //isAscending
        anArray = new StringArray(new String[] {"go","go","hi"});
        Test.ensureEqual(anArray.isAscending(), "", "");
        anArray.set(2, null);
        Test.ensureEqual(anArray.isAscending(), 
            "StringArray isn't sorted in ascending order: [2]=null.", "");
        anArray.set(1, "ga");
        Test.ensureEqual(anArray.isAscending(), 
            "StringArray isn't sorted in ascending order: [0]=\"go\" > [1]=\"ga\".", "");

        //isDescending
        anArray = new StringArray(new String[] {"hi", "go", "go"});
        Test.ensureEqual(anArray.isDescending(), "", "");
        anArray.set(2, null);
        Test.ensureEqual(anArray.isDescending(), 
            "StringArray isn't sorted in descending order: [2]=null.", "");
        anArray.set(1, "pa");
        Test.ensureEqual(anArray.isDescending(), 
            "StringArray isn't sorted in descending order: [0]=\"hi\" < [1]=\"pa\".", "");

        //firstTie
        anArray = new StringArray(new String[] {"hi", "pa", "go"});
        Test.ensureEqual(anArray.firstTie(), -1, "");
        anArray.set(1, "hi");
        Test.ensureEqual(anArray.firstTie(), 0, "");
        anArray.set(1, null);
        Test.ensureEqual(anArray.firstTie(), -1, "");
        anArray.set(2, null);
        Test.ensureEqual(anArray.firstTie(), 1, "");

        //diff
        anArray  = new StringArray(new String[] {"0", "11", "22"});
        anArray2 = new StringArray(new String[] {"0", "11", "22"});
        String s = anArray.diffString(anArray2);  Test.ensureEqual(s, "", "s=" + s);
        anArray2.add("33");
        s = anArray.diffString(anArray2);  Test.ensureEqual(s, "  old [3]=33,\n  new [3]=null.", "s=" + s);
        anArray2.set(2, "23");
        s = anArray.diffString(anArray2);  Test.ensureEqual(s, "  old [2]=23,\n  new [2]=22.", "s=" + s);
        IntArray ia = new IntArray(new int[]{0, 11, 22});
        s = anArray.diffString(ia);  Test.ensureEqual(s, "", "s=" + s);
        ia.set(2, 23);
        s = anArray.diffString(ia);  Test.ensureEqual(s, "  old [2]=23,\n  new [2]=22.", "s=" + s);


        //hashcode
        anArray = new StringArray();
        for (int i = 5; i < 1000; i++)
            anArray.add("" + i);
        String2.log("hashcode1=" + anArray.hashCode());
        anArray2 = (StringArray)anArray.clone();
        Test.ensureEqual(anArray.hashCode(), anArray2.hashCode(), "");
        anArray.add(0, "2");
        Test.ensureTrue(anArray.hashCode() != anArray2.hashCode(), "");

        //justKeep
        BitSet bitset = new BitSet();
        anArray = new StringArray(new String[] {"0", "11", "22", "33", "44"});
        bitset.set(1);
        bitset.set(4);
        anArray.justKeep(bitset);
        Test.ensureEqual(anArray.toString(), "11, 44", "");

        //min max
        anArray = new StringArray();
        anArray.addString(anArray.minValue());
        anArray.addString(anArray.maxValue());
        Test.ensureEqual(anArray.getString(0), anArray.minValue(), "");
        Test.ensureEqual(anArray.getString(0), "\u0000", "");
        Test.ensureEqual(anArray.getString(1), anArray.maxValue(), "");

        //sortIgnoreCase
        anArray = fromCSV("AB, AB, Ab, Ab, aB, aB, ab, ab, ABC, ABc, AbC");
        anArray.sortIgnoreCase();
        Test.ensureEqual(anArray.toString(), "AB, AB, Ab, Ab, aB, aB, ab, ab, ABC, ABc, AbC", "");

        //numbers
        DoubleArray da = new DoubleArray(new double[]{5, Double.NaN});
        da.sort();  //do NaN's sort high?
        Test.ensureEqual(da.toString(), "5.0, NaN", "");
        Test.ensureEqual(da.getString(1), "", "");

        //inCommon
        anArray  = fromCSV("a, b, d");
        anArray2 = fromCSV("a, c, d");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "a, d", "");

        anArray  = fromCSV("a, d");
        anArray2 = fromCSV("b, e");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "", "");

        anArray  = fromCSV("");
        anArray2 = fromCSV("a, c, d");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "", "");

        anArray  = fromCSV("");
        anArray2 = fromCSV("a, c, d");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "", "");

        anArray  = fromCSV("c");
        anArray2 = fromCSV("a, c, d");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "c", "");

        anArray  = fromCSV("a, b, c");
        anArray2 = fromCSV("c");
        anArray.inCommon(anArray2);
        Test.ensureEqual(anArray.toString(), "c", "");

        //fromCSVNoBlanks
        anArray  = fromCSVNoBlanks(", b, ,d,,");
        Test.ensureEqual(anArray.toString(), "b, d", "");
       
    }

}

