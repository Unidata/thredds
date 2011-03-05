/*
 * Copyright 1998-2011 University Corporation for Atmospheric Research/Unidata
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

/**
 *
 */
package ucar.nc2.iosp.grads;


import ucar.nc2.units.DateUnit;


import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


/**
 * Class to hold the complexities of the GrADS time dimension
 *
 * @author Don Murray - CU/CIRES
 */
public class GradsTimeDimension extends GradsDimension {

    /** time increment periods */
    private final String[] incStr = { "mn", "hr", "dy", "mo", "yr" };

    /** corresponding Calendar periods */
    private final int[] calIncs = { Calendar.MINUTE, Calendar.HOUR,
                                    Calendar.DATE, Calendar.MONTH,
                                    Calendar.YEAR };

    /** time date formats (input is lowercased so Z is z) */
    private final String[] dateFormats = { "HH:mm'z'ddMMMyyyy",
                                           "HH'z'ddMMMyyyy", "ddMMMyyyy",
                                           "MMMyyyy" };

    /**
     * Create new GradsTimeDimension
     *
     * @param name  the dimension name
     * @param size  the dimension size
     * @param mapping  the dimension mapping type
     */
    public GradsTimeDimension(String name, int size, String mapping) {
        super(name, size, mapping);
    }

    /**
     * Make the level values from the specifications
     *
     * @return the level values
     */
    protected double[] makeLevelValues() {
        List<String> levels = getLevels();
        if (levels == null) {
            return null;
        }
        if (levels.size() != getSize()) {
            // do someting
        }
        // Time is always LINEAR
        int      inc     = 0;
        double[] vals    = new double[getSize()];
        String   tstart  = levels.get(0).trim().toLowerCase();
        String   pattern = null;
        if (tstart.indexOf(":") >= 0) {                     // HH:mmZddMMMyyyy
            pattern = dateFormats[0];
        } else if (tstart.indexOf("Z") >= 0) {              // mmZddMMMyyyy
            pattern = dateFormats[1];
        } else if (Character.isLetter(tstart.charAt(0))) {  // MMMyyyy
            pattern = dateFormats[3];
        } else {
            pattern = dateFormats[2];                       // ddMMMyyyy
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        //sdf.setLenient(true);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        ParsePosition p = new ParsePosition(0);
        Date          d = sdf.parse(tstart, p);
        if (d == null) {
            System.out.println("couldn't parse at " + p.getErrorIndex());
            d = new Date(0);
        }
        //System.out.println("start = " + d);
        // vvkk where
        // vv     =       an integer number, 1 or 2 digits
        // kk     =       mn (minute)
        //                hr (hour)
        //                dy (day)
        //                mo (month)
        //                yr (year) 
        String tinc     = levels.get(1).toLowerCase();
        int    incIndex = 0;
        for (int i = 0; i < incStr.length; i++) {
            int index = tinc.indexOf(incStr[i]);
            if (index < 0) {
                continue;
            }
            sdf.applyPattern("yyyy-MM-dd HH:mm:ss Z");
            setUnit("hours since "
                    + sdf.format(d, new StringBuffer(),
                                 new FieldPosition(0)));
            int numOf = Integer.parseInt(tinc.substring(0, index));
            inc      = numOf;
            incIndex = i;

            break;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        calendar.setTime(d);
        vals[0] = 0;
        for (int i = 1; i < getSize(); i++) {
            int amount = inc;
            calendar.add(calIncs[incIndex], amount);
            // subtract from origin, convert to hours
            double offset = (calendar.getTime().getTime() - d.getTime())
                            / (1000 * 60 * 60);  //millis in an hour
            vals[i] = offset;
        }

        return vals;

    }

    /**
     * Make a time struct from the index.
     *
     * @param timeIndex  the time value index
     *
     * @return the corresponding TimeStruct
     */
    public TimeStruct makeTimeStruct(int timeIndex) {
        double   tVal     = getValues()[timeIndex];
        Date     d        = DateUnit.getStandardDate(tVal + getUnit());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        calendar.setTime(d);
        TimeStruct ts = new TimeStruct();
        ts.year   = calendar.get(Calendar.YEAR);
        ts.month  = calendar.get(Calendar.MONTH) + 1;  // MONTH is zero based
        ts.day    = calendar.get(Calendar.DAY_OF_MONTH);
        ts.hour   = calendar.get(Calendar.HOUR_OF_DAY);
        ts.minute = calendar.get(Calendar.MINUTE);
        return ts;
    }

    /**
     * A class to hold a GrADS time structure.  The full time spec is:
     *
     *        HH:mm'Z'ddMMMyyyy (e.g. 12:04Z05Mar2011)
     * 
     * @author   Don Murray CU-CIRES
     */
    public class TimeStruct {

        /** year field */
        int year = 0;

        /** month field (1 based) */
        int month = 0;

        /** day field */
        int day = 0;

        /** hour field */
        int hour = 0;

        /** minute field */
        int minute = 0;

        /**
         * Create a new time structure
         */
        public TimeStruct() {}
    }

    /**
     * Replace the time template parameters in a filename
     *
     * @param filespec  the file template
     * @param ts  the time to use
     *
     * @return  the filled in templage
     */
    public String replaceFileTemplate(String filespec, TimeStruct ts) {
        return filespec;
    }
    
    

}