/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////


package opendap.dap;

import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Vector;

/**
 * The Util class holds static methods used by this package.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 */
public class Util {


    static public Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Compares elements in a <code>Vector</code> of <code>BaseType</code>s and
     * throw a <code>BadSemanticsException</code> if there are any
     * duplicate elements.
     *
     * @param v        The <code>Vector</code> to check
     * @param varName  the name of the variable which called us
     * @param typeName the type name of the variable which called us
     * @throws BadSemanticsException     if there are duplicate elements
     * @throws IndexOutOfBoundsException if size doesn't match the number
     *                                   of elements in the <code>Enumeration</code>
     */
    static void uniqueNames(Vector v, String varName,
                            String typeName) throws BadSemanticsException {
        String[] names = sortedNames(v);
        // DEBUG: print out names
        //for(int i=0; i<names.length; i++) {
        //  LogStream.err.println("names[" + i + "] = " + names[i]);
        //}
        // look for any instance of consecutive names that are ==
        for (int i = 1; i < names.length; i++) {
            if (names[i - 1].equals(names[i])) {
                throw new BadSemanticsException("The variable `" + names[i] +
                        "' is used more than once in " + typeName + " `" +
                        varName + "'");
            }
        }
    }

    /**
     * Takes a <code>Vector</code> of <code>BaseType</code>s, retrieves their
     * names into an array of <code>String</code>s, and performs a Quick Sort
     * on that array.
     *
     * @param v The Vector to check
     * @return a sorted array of <code>String</code>
     * @throws BadSemanticsException if there is an element with no name
     */
    static String[] sortedNames(Vector v) throws BadSemanticsException {
        String[] names = new String[v.size()];
        int count = 0;
        for (Enumeration e = v.elements(); e.hasMoreElements();) {
            BaseType bt = (BaseType) e.nextElement();
            String tempName = bt.getEncodedName();
            if (tempName == null)
                throw new BadSemanticsException(bt.getClass().getName() + " variable with no name");
            names[count++] = tempName;
        }
        // DEBUG: print out names
        //for(int i=0; i<names.length; i++) {
        //  LogStream.err.println("names[" + i + "] = " + names[i]);
        //}
        // assert that size is correct
        if (count != names.length)
            throw new IndexOutOfBoundsException("Vector size changed unexpectedly");
        quickSort(names, 0, names.length - 1);
        return names;
    }

    /**
     * Internal recursive method to perform Quick Sort on name array.
     *
     * @param a   an array of <code>String</code>.
     * @param lo0 the low index to sort.
     * @param hi0 the high index to sort.
     */
    static private void quickSort(String a[], int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        String mid;

        if (hi0 > lo0) {
            // Arbitrarily establishing partition element as the array midpoint */
            //Coverity[FB.IM_AVERAGE_COMPUTATION_COULD_OVERFLOW]
            mid = a[(lo0 + hi0) / 2];

            // loop through the array until indices cross
            while (lo <= hi) {
                // find the first element that is >= the partition element
                // starting from the left index.
                while ((lo < hi0) && (a[lo].compareTo(mid) < 0))
                    ++lo;

                // find an element that is <= the partition element
                // starting from the right index.
                while ((hi > lo0) && (a[hi].compareTo(mid) > 0))
                    --hi;

                // if the indexes have not crossed, swap
                if (lo <= hi) {
                    swap(a, lo, hi);
                    ++lo;
                    --hi;
                }
            }
            // If the right index has not reached the left side of array,
            // sort the left partition.
            if (lo0 < hi)
                quickSort(a, lo0, hi);

            // If the left index has not reached the right side of array,
            // sort the right partition.
            if (lo < hi0)
                quickSort(a, lo, hi0);
        }
    }

    /**
     * Private method to swap two elements in the array
     *
     * @param a an array of <code>String</code>.
     * @param i the index of the first element.
     * @param j the index of the second element.
     */
    static private void swap(String a[], int i, int j) {
        String T;
        T = a[i];
        a[i] = a[j];
        a[j] = T;
    }

    /**
     * This function escapes non-printable characters and quotes.  This is used
     * to make <code>printVal</code> output <code>DString</code> data in the
     * same way as the C++ version.  Since Java supports Unicode, this will
     * need to be altered if it's desired to print <code>DString</code> as
     * UTF-8 or some other character encoding.
     *
     * @param s the input <code>String</code>.
     * @return the escaped <code>String</code>.
     */
    static String escattr(String s) {
        StringBuffer buf = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || (c >= '!' && c <= '~')) {
                // printable ASCII character
                buf.append(c);
            } else {
                // non-printable ASCII character: print as unsigned octal integer
                // padded with leading zeros
                buf.append('\\');
                String numVal = Integer.toString((int) c & 0xFF, 8);
                for (int pad = 0; pad < (3 - numVal.length()); pad++)
                    buf.append('0');
                buf.append(numVal);
            }
        }
        return buf.toString();
    }

    /**
     * Filter out runtime exceptions from other exceptions and re-throw
     */

    static public void check(Exception ex)
	    throws RuntimeException
    {
        if (ex instanceof RuntimeException)
            throw (RuntimeException)ex;
    }



}


