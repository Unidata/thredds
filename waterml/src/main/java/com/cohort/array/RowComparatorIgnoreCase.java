/* This file is part of the EMA project and is 
 * Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.array;

import com.cohort.util.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * This is used by PrimitiveArray.rankIgnoreCase to rank a table of data stored as 
 * a PrimitiveArray[].
 * StringArrays are compared in a case-insensitive way.
 */
public class RowComparatorIgnoreCase extends RowComparator {


    /**
     * A constructor used by PrimitiveArray.rank.
     *
     * @param table a List of PrimitiveArrays
     * @param keys an array of the key column numbers 
     *    (each is 0..nColumns-1, the first key is the most important)
     *    which are used to determine the sort order
     * @param ascending an array of booleans corresponding to the keys
     *    indicating if the arrays are to be sorted by a given key in 
     *    ascending or descending order.
     * @throws Exception if trouble
     */
    public RowComparatorIgnoreCase(List table, int keys[], boolean[] ascending) {
        super(table, keys, ascending);
    }

    /**
     * This is required for the Comparator interface.
     *
     * @param o1 an Integer with the row number (0 ... size-1)
     * @param o2 an Integer with the row number (0 ... size-1)
     * @return returns a negative integer, zero, or a positive integer if the 
     *   value at index1 is less than, equal to, or greater than 
     *   the value at index2.  
     *   Think "o1 - o2".
     */
    public int compare(Object o1, Object o2) {
        for (int k = 0; k < keys.length; k++) {
            int result = ((PrimitiveArray)table.get(keys[k])).compareIgnoreCase(
                ((Integer)o1).intValue(), ((Integer)o2).intValue());
            if (result != 0) 
                return ascending[k]? result : -result;
        }
        return 0;
    }

    /**
     * This is required for the Comparator interface.
     *
     * @param obj usually another RowComparatorIgnoreCase
     */
    public boolean equals(Object obj) {
        if (obj instanceof RowComparatorIgnoreCase) {
        } else return false;

        //make sure all the keys and ascending values are the same
        RowComparatorIgnoreCase other = (RowComparatorIgnoreCase)obj;
        if (other.table.size() != table.size() ||
            other.keys.length != keys.length ||
            other.ascending.length != ascending.length)
            return false;
        for (int t = 0; t < table.size(); t++) {
            if (other.table.get(t) != table.get(t))  //use != to test if they are the same object
                return false;
        }
        for (int k = 0; k < keys.length; k++) {
            if (other.keys[k] != keys[k] ||  //use != to test if they are the same object
                other.ascending[k] != ascending[k])
                return false;
        }
        return true;
    }

}

