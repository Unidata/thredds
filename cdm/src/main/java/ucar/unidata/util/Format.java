/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

// $Id: Format.java 64 2006-07-12 22:30:50Z edavis $

package ucar.unidata.util;


/**
 * static formatting utilities. Replace with standard java library when possible.
 * @author John Caron
 */
public class Format {

    /**
     * Blank fill sbuff with blanks, until position tabStop.
     * @param sbuff StringBuffer to manipulate
     * @param tabStop pad out to here
     * @param alwaysOne true if you want to guarentee at least one space.
     */
    public static void tab(StringBuffer sbuff, int tabStop, boolean alwaysOne) {
        int len = sbuff.length();
        if (tabStop > len) {
            sbuff.setLength(tabStop);
            for (int i = len; i < tabStop; i++) {
                sbuff.setCharAt(i, ' ');
            }
        } else if (alwaysOne) {
            sbuff.setLength(len + 1);
            sbuff.setCharAt(len, ' ');
        }
    }

      /**
     * Blank fill sbuff with blanks, until position tabStop.
     * @param sbuff StringBuilder to manipulate
     * @param tabStop pad out to here
     * @param alwaysOne true if you want to guarentee at least one space.
     */
    public static void tab(StringBuilder sbuff, int tabStop, boolean alwaysOne) {
        int len = sbuff.length();
        if (tabStop > len) {
            sbuff.setLength(tabStop);
            for (int i = len; i < tabStop; i++) {
                sbuff.setCharAt(i, ' ');
            }
        } else if (alwaysOne) {
            sbuff.setLength(len + 1);
            sbuff.setCharAt(len, ' ');
        }
    }

    /**
     * Create a new string by padding the existing one with blanks to specified width.
     * Do nothing if length is already greater or equal to width.
     *
     * @param s string to pad
     * @param width length of return string
     * @return padded string
     */
    public static String s(String s, int width) {
        return pad(s, width, false);
    }

    /**
     * Create a new string by padding the existing one with blanks to specified width.
     * Do nothing if length is already greater or equal to width.
     *
     * @param s string to pad
     * @param width length of return string
     * @param rightJustify if true, right justify, else left justify
     * @return padded string
     */
    public static String pad(String s, int width, boolean rightJustify) {
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sbuff = new StringBuilder(width);
        int          need  = width - s.length();
        sbuff.setLength(need);
        for (int i = 0; i < need; i++) {
            sbuff.setCharAt(i, ' ');
        }

        if (rightJustify) {
            sbuff.append(s);
        } else {
            sbuff.insert(0, s);
        }

        return sbuff.toString();
    }

    /**
     * Format an integer value.
     * @param v : value
     * @param width pad to this width
     * @return formatted string
     */
    public static String i(int v, int width) {
        return pad(Integer.toString(v), width, true);
    }

    /**
     * Format a long value.
     * @param v : value
     * @param width pad to this width
     * @return formatted string
     */
    public static String l(long v, int width) {
        return pad(Long.toString(v), width, true);
    }

    /**
     *  Double value formatting with minimum number of significant figures in a minimum width.
     *  This will try to do a reasonable job of getting
     *  a representation that has min_sigfig significant figures in minimum width.
     *
     *  @param d the number to format.
     *  @param min_sigfig minimum number of significant figures
     *  @return string representation
     */
    public static String d(double d, int min_sigfig) {
        return formatDouble(d, min_sigfig, -1).trim();
    }

    /**
     * Double value formatting with minimum number of significant figures in a specified width.
     * This will try to do a reasonable job of getting
     * a representation that has min_sigfig significant figures in the specified width.
     * Right now, all it does is call d( double d, int min_sigfig) and left pad out to width chars.
     *
     * @param d the number to format.
     * @param min_sigfig minimum number of significant figures
     * @param width width of the result
     * @return string representation, right justified in field of specified width
     */
    public static String d(double d, int min_sigfig, int width) {
        String s = Format.d(d, min_sigfig);
        return pad(s, width, true);
    }

    /**
     * Double value formatting with fixed number of digits to the right of the decimal point.
     *
     * @param d the number to format.
     * @param fixed_decimals number of digits to the right of the decimal point
     * @return string representation, with specified number of decimal places
     */
    public static String dfrac(double d, int fixed_decimals) {
        return formatDouble(d, 100, fixed_decimals).trim();
        //String s = Double.toString( d);
        //s = sigfigFix( s, 100, num_dec);
        //return s.trim();
    }

    /* This dorks with Double.toString():
     *
     * From Double.toString() (m = magnitude of the number):
     *
     *  If m is greater than or equal to 10^-3 but less than 10^7, then it is represented as the
     *  integer part of m, in decimal form with no leading zeroes, followed by '.' (.),
     *  followed by one or more decimal digits representing the fractional part of m.
     *
     *  If m is less than 10^-3 or greater than 10^7, then it is represented in scientific notation.
     *  Let n be the unique integer such that 10n<=m<10n+1; then let a be the mathematically exact
     *  quotient of m and 10n so that 1<=a<10. The magnitude is then represented as the integer part
     *  of a, as a single decimal digit, followed by '.' (.), followed by decimal digits representing
     *  the fractional part of a, followed by the letter 'E' (E), followed by a representation of n
     *  as a decimal integer, as produced by the method Integer.toString(int).
     *
     *  How many digits must be printed for the fractional part of m or a? There must be
     *  at least one digit to represent the fractional part, and beyond that as many,
     *  but only as many, more digits as are needed to uniquely distinguish the argument
     *  value from adjacent values of type double. That is, suppose that x is the exact
     *  mathematical value represented by the decimal representation produced by this method
     *  for a finite nonzero argument d. Then d must be the double value nearest to x; or if
     *  two double values are equally close to x, then d must be one of them and the least
     *  significant bit of the significand of d must be 0.
     */

    /**
     * Format a double value
     *
     * @param d   value to format
     * @param min_sigFigs minimum significant figures
     * @param fixed_decimals number of fixed decimals
     *
     * @return double formatted as a string
     */
    public static String formatDouble(double d, int min_sigFigs,
                                      int fixed_decimals) {

        String s = Double.toString(d);

        // extract the sign
        String sign;
        String unsigned;
        if (s.startsWith("-") || s.startsWith("+")) {
            sign     = s.substring(0, 1);
            unsigned = s.substring(1);
        } else {
            sign     = "";
            unsigned = s;
        }

        // deal with exponential notation
        String mantissa;
        String exponent;
        int    eInd = unsigned.indexOf('E');
        if (eInd == -1) {
            eInd = unsigned.indexOf('e');
        }
        if (eInd == -1) {
            mantissa = unsigned;
            exponent = "";
        } else {
            mantissa = unsigned.substring(0, eInd);
            exponent = unsigned.substring(eInd);
        }

        // deal with decimal point
        StringBuilder number, fraction;
        int          dotInd = mantissa.indexOf('.');
        if (dotInd == -1) {
            number   = new StringBuilder(mantissa);
            fraction = new StringBuilder("");
        } else {
            number   = new StringBuilder(mantissa.substring(0, dotInd));
            fraction = new StringBuilder(mantissa.substring(dotInd + 1));
        }

        // number of significant figures
        int numFigs  = number.length();
        int fracFigs = fraction.length();

        // can do either fixed_decimals or min_sigFigs
        if (fixed_decimals != -1) {
            if (fixed_decimals == 0) {
                fraction.setLength(0);
            } else if (fixed_decimals > fracFigs) {
                int want = fixed_decimals - fracFigs;
                for (int i = 0; i < want; i++) {
                    fraction.append("0");
                }
            } else if (fixed_decimals < fracFigs) {
                int chop = fracFigs - fixed_decimals;  // LOOK should round !!
                fraction.setLength(fraction.length() - chop);
            }
            fracFigs = fixed_decimals;

        } else {
            // Don't count leading zeros in the fraction, if no number
            if (((numFigs == 0) || number.toString().equals("0"))
                    && (fracFigs > 0)) {
                numFigs = 0;
                number  = new StringBuilder("");
                for (int i = 0; i < fraction.length(); ++i) {
                    if (fraction.charAt(i) != '0') {
                        break;
                    }
                    --fracFigs;
                }
            }
            // Don't count trailing zeroes in the number if no fraction
            if ((fracFigs == 0) && (numFigs > 0)) {
                for (int i = number.length() - 1; i > 0; i--) {
                    if (number.charAt(i) != '0') {
                        break;
                    }
                    --numFigs;
                }
            }
            // deal with min sig figures
            int sigFigs = numFigs + fracFigs;
            if (sigFigs > min_sigFigs) {
                // Want fewer figures in the fraction; chop (should round? )
                int chop = Math.min(sigFigs - min_sigFigs, fracFigs);
                fraction.setLength(fraction.length() - chop);
                fracFigs -= chop;
            }
        }


        /*int sigFigs = numFigs + fracFigs;
        if (sigFigs > max_sigFigs) {

          if (numFigs >= max_sigFigs) {  // enough sig figs in just the number part
            fraction.setLength( 0 );
            for ( int i=max_sigFigs; i<numFigs; ++i )
              number.setCharAt( i, '0' );  // should round?
          } else {

            // Want fewer figures in the fraction; chop (should round? )
            int chop = sigFigs - max_sigFigs;
            fraction.setLength( fraction.length() - chop );
          }
        }


        /* may want a fixed decimal place
        if (dec_places != -1) {

          if (dec_places == 0) {
            fraction.setLength( 0 );
            fracFigs = 0;
          } else if (dec_places > fracFigs) {
            int want = dec_places - fracFigs;
            for (int i=0; i<want; i++)
              fraction.append("0");
          } else if (dec_places < fracFigs) {
            int chop = fracFigs - dec_places;
            fraction.setLength( fraction.length() - chop );
            fracFigs = dec_places;
          }

        } */

        if (fraction.length() == 0) {
            return sign + number + exponent;
        } else {
            return sign + number + "." + fraction + exponent;
        }
    }


    /**
     * Nicely formatted representation of bytes, eg turn 5.636E7 into
     * @param size the size in bytes
     * @return formatted string
     */
    public static String formatByteSize(double size) {
        String unit = null;
        if (size > 1.0e15) {
            unit = "Pbytes";
            size *= 1.0e-15;
        } else if (size > 1.0e12) {
            unit = "Tbytes";
            size *= 1.0e-12;
        } else if (size > 1.0e9) {
            unit = "Gbytes";
            size *= 1.0e-9;
        } else if (size > 1.0e6) {
            unit = "Mbytes";
            size *= 1.0e-6;
        } else if (size > 1.0e3) {
            unit = "Kbytes";
            size *= 1.0e-3;
        } else {
            unit = "bytes";
        }

        return Format.d(size, 4) + " " + unit;
    }

    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Show the value of a double to the significant figures
     *
     * @param d  double value
     * @param sigfig number of significant figures
     */
    private static void show(double d, int sigfig) {
        System.out.println("Format.d(" + d + "," + sigfig + ") == "
                           + Format.d(d, sigfig));
    }

    /**
     * Show the value of a double with specified number of decimal places
     *
     * @param d  double value
     * @param dec_places number of decimal places
     */
    private static void show2(double d, int dec_places) {
        System.out.println("Format.dfrac(" + d + "," + dec_places + ") == "
                           + Format.dfrac(d, dec_places));
    }

    /**
     * Main method for testing
     *
     * @param argv ignored
     */
    public static void main(String argv[]) {
        /*   show(.00123, 3);
           show(.001234, 3);
           show(.001234, 2);
           show(1.0001234, 3);
           show(.000123, 3);
           show(.0001234, 3);
           show(.0001234, 2);
           show(1.0001234, 3);
           show(.0000000123, 3);
           show(123456.0, 3);
           show(1234567.0, 3);
           show(12345678.0, 3); */
        show(1.2345678E-6, 3);

        /*show2(.00123, 3);
        show2(.001234, 3);
        show2(.001234, 2);
        show2(1.0001234, 3);
        show2(.000123, 3);
        show2(.0001234, 3);
        show2(.0001234, 2);
        show2(1.0001234, 3);
        show2(.0000000123, 3);
        show2(123456.0, 3);
        show2(1234567.0, 3);
        show2(12345678.0, 3); */
    }

}

/*
 *  Change History:
 *  $Log: Format.java,v $
 *  Revision 1.8  2006/05/05 19:19:34  jeffmc
 *  Refactor some of the tabbedpane border methods.
 *  Also, since I ran jindent on everything to test may as well caheck it all in
 *
 *  Revision 1.7  2005/08/11 16:51:16  jeffmc
 *  jindent
 *
 *  Revision 1.6  2005/07/29 00:30:06  caron
 *  add formatByteSize() to make nice print of size in bytes
 *
 *  Revision 1.5  2005/03/10 18:40:06  jeffmc
 *  jindent and javadoc
 *
 *  Revision 1.4  2005/01/12 15:29:44  dmurray
 *  jindent and javadoc
 *
 *  Revision 1.3  2004/12/22 13:28:16  dmurray
 *  Jindent.  somebody else should fix the _more_'s.
 *
 *  Revision 1.2  2004/09/25 00:09:45  caron
 *  add images, thredds tab
 *
 *  Revision 1.1  2004/09/22 13:55:16  caron
 *  move to ucar.unidata.util
 *
 *  Revision 1.3  2004/07/12 23:40:19  caron
 *  2.2 alpha 1.0 checkin
 *
 *  Revision 1.2  2004/07/06 19:28:13  caron
 *  pre-alpha checkin
 *
 *  Revision 1.1.1.1  2003/12/04 21:05:28  caron
 *  checkin 2.2
 *
 *  Revision 1.3  2003/07/12 23:08:57  caron
 *  add cvs headers, trailers
 *
 *  Revision 1.2  2003/04/08 19:03:41  caron
 *  add header
 *
 *  Revision 1.1  2003/04/08 15:06:34  caron
 *  nc2 version 2.1
 *
 *  Revision 1.1.1.1  2002/11/23 17:49:48  caron
 *  thredds reorg
 *
 *  Revision 1.1.1.1  2002/02/15 00:01:49  caron
 *  import sources
 *
 */

/**
 * Double value formatting.
 *  @param d the number to format.
 *  @param width width of the result (not currently implemented).
 *  @param sigfig the number of significant figures.
 *  @deprecated use d
 *
 * public static String formatDouble( double d, int width, int sigfig) {
 *  return formatDouble( d, sigfig, width).trim();
 * }
 *
 * private static final double LOG10 = Math.log(10.0);
 * double lr = Math.log(d)/ LOG10;
 * if ((lr > sigfig) && (lr < width))
 * sprintf(s, "%*.0f", width, d);
 * else
 * sprintf(s, "%*.*g", width, sigfig, d);
 */

// some of this code was derived from
// Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

/*
 *  Change History:
 *  $Log: Format.java,v $
 *  Revision 1.8  2006/05/05 19:19:34  jeffmc
 *  Refactor some of the tabbedpane border methods.
 *  Also, since I ran jindent on everything to test may as well caheck it all in
 *
 *  Revision 1.7  2005/08/11 16:51:16  jeffmc
 *  jindent
 *
 *  Revision 1.6  2005/07/29 00:30:06  caron
 *  add formatByteSize() to make nice print of size in bytes
 *
 *  Revision 1.5  2005/03/10 18:40:06  jeffmc
 *  jindent and javadoc
 *
 *  Revision 1.4  2005/01/12 15:29:44  dmurray
 *  jindent and javadoc
 *
 *  Revision 1.3  2004/12/22 13:28:16  dmurray
 *  Jindent.  somebody else should fix the _more_'s.
 *
 *  Revision 1.2  2004/09/25 00:09:45  caron
 *  add images, thredds tab
 *
 *  Revision 1.1  2004/09/22 13:55:16  caron
 *  move to ucar.unidata.util
 *
 *  Revision 1.3  2004/07/12 23:40:19  caron
 *  2.2 alpha 1.0 checkin
 *
 *  Revision 1.2  2004/07/06 19:28:13  caron
 *  pre-alpha checkin
 *
 *  Revision 1.1.1.1  2003/12/04 21:05:28  caron
 *  checkin 2.2
 *
 *  Revision 1.3  2003/07/12 23:08:57  caron
 *  add cvs headers, trailers
 *
 */

