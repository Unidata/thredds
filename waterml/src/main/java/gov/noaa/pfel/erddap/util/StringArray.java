/* Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package gov.noaa.pfel.erddap.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * StringArray is a thin shell over a String[] with methods like ArrayList's 
 * methods; it extends PrimitiveArray.
 * All of the methods which add strings to StringArray (e.g., add()),
 * use String2.canonical(), to ensure that canonical Strings are stored (to save memory
 * if there are duplicates).
 *
 * <p>This class uses "" to represent a missing value (NaN).
 */
public class StringArray {
    /** The number of active values (which may be different from the array's capacity). */
    protected int size = 0;

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

    public static StringArray fromInputStream(InputStream inputStream, String charset) throws IOException {
        StringArray sa = new StringArray();
        InputStreamReader isr = charset == null || charset.length() == 0 ?
                new InputStreamReader(inputStream) :
                new InputStreamReader(inputStream, charset);
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
     * Return the number of elements in the array.
     *
     * @return the number of elements in the array.
     */
    public int size() {
        return size;
    }

    /**
     * This adds an item to the array (increasing 'size' by 1).
     *
     * @param value the value to be added to the array
     */
    public void add(String value) {
        if (size == array.length) //if we're at capacity
            ensureCapacity(size + 1L);
        array[size++] = value;
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

            String[] newArray = new String[newCapacity];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray; //do last to minimize concurrency problems
        }
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
}
