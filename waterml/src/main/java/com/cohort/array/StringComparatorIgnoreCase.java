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
 * This is used by StringArray to do a case insensited sort 
 * (better than String.CASE_INSENSITIVE_ORDER).
 */
public class StringComparatorIgnoreCase implements Comparator {


    /**
     * This is required for the Comparator interface.
     *
     * @param o1 
     * @param o2 
     * @return returns a negative integer, zero, or a positive integer if the 
     *   value at index1 is less than, equal to, or greater than 
     *   the value at index2.  
     *   Think "o1 - o2".
     */
    public int compare(Object o1, Object o2) {
        if (o1 == null)
            return o2 == null? 0 : -1;
        if (o2 == null) 
            return 1;
        String s1 = (String)o1;
        String s2 = (String)o2;
//        int c = s1.toUpperCase().compareTo(s2.toUpperCase());
        int c = s1.compareToIgnoreCase(s2);
        if (c != 0) 
            return c;
        return s1.compareTo(s2);
    }

    /**
     * This is required for the Comparator interface.
     *
     * @param obj usually another RowComparator
     */
    public boolean equals(Object obj) {
        return obj == this;
    }

}

