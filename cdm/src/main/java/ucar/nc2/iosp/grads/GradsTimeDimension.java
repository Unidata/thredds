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

    /** The initial time as a TimeStruct */
    private GradsTimeStruct initialTime = null;

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
        GradsTimeStruct initialTime = makeTimeStruct(calendar);
        initialTime.year = calendar.get(Calendar.YEAR);
        initialTime.month = calendar.get(Calendar.MONTH) + 1;  // MONTH is zero based
        initialTime.day    = calendar.get(Calendar.DAY_OF_MONTH);
        initialTime.hour   = calendar.get(Calendar.HOUR_OF_DAY);
        initialTime.minute = calendar.get(Calendar.MINUTE);
        //System.out.println("initial time = " + initialTime);
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
    public GradsTimeStruct makeTimeStruct(int timeIndex) {
        double   tVal     = getValues()[timeIndex];
        Date     d        = DateUnit.getStandardDate(tVal + " " + getUnit());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        calendar.setTime(d);
        return makeTimeStruct(calendar);
    }

    /**
     * Make a GradsTimeStruct from the calendar state
     *
     * @param calendar  the calendar
     *
     * @return the corresponding GradsTimeStruct
     */
    private GradsTimeStruct makeTimeStruct(Calendar calendar) {
        GradsTimeStruct ts = new GradsTimeStruct();
        ts.year   = calendar.get(Calendar.YEAR);
        ts.month  = calendar.get(Calendar.MONTH) + 1;  // MONTH is zero based
        ts.day    = calendar.get(Calendar.DAY_OF_MONTH);
        ts.hour   = calendar.get(Calendar.HOUR_OF_DAY);
        ts.minute = calendar.get(Calendar.MINUTE);
        ts.jday   = calendar.get(Calendar.DAY_OF_YEAR);
        return ts;
    }

    /**
     * Replace the time template parameters in a filename
     *
     * @param filespec  the file template
     * @param timeIndex the time index
     *
     * @see http://www.iges.org/grads/gadoc/templates.html
     *
     * @return  the filled in template
     */
    public String replaceFileTemplate(String filespec, int timeIndex) {

        GradsTimeStruct ts        = makeTimeStruct(timeIndex);
        String          retString = filespec;
        String          format;
        while (retString.indexOf("%") >= 0) {
            // initial time
            if (retString.indexOf("%ix1") >= 0) {
                retString = retString.replace("%ix1",
                        String.format("%d", initialTime.year / 10));
            }
            if (retString.indexOf("%ix3") >= 0) {
                retString = retString.replace("%ix3",
                        String.format("%03d", initialTime.year / 10));
            }
            if (retString.indexOf("%iy2") >= 0) {
                int cent = initialTime.year / 100;
                int val  = initialTime.year - cent * 100;
                retString = retString.replace("%iy2",
                        String.format("%02d", val));
            }
            if (retString.indexOf("%iy4") >= 0) {
                retString = retString.replace("%iy4",
                        String.format("%d", initialTime.year));
            }
            if (retString.indexOf("%im1") >= 0) {
                retString = retString.replace("%im1",
                        String.format("%d", initialTime.month));
            }
            if (retString.indexOf("%im2") >= 0) {
                retString = retString.replace("%im2",
                        String.format("%02d", initialTime.month));
            }
            if (retString.indexOf("%imc") >= 0) {
                retString = retString.replace("%imc",
                        GradsTimeStruct.months[initialTime.month - 1]);
            }
            if (retString.indexOf("%id1") >= 0) {
                retString = retString.replace("%id1",
                        String.format("%d", initialTime.day));
            }
            if (retString.indexOf("%id2") >= 0) {
                retString = retString.replace("%id2",
                        String.format("%02d", initialTime.day));
            }
            if (retString.indexOf("%ih1") >= 0) {
                retString = retString.replace("%ih1",
                        String.format("%d", initialTime.hour));
            }
            if (retString.indexOf("%ih2") >= 0) {
                retString = retString.replace("%ih2",
                        String.format("%02d", initialTime.hour));
            }
            if (retString.indexOf("%ih3") >= 0) {
                retString = retString.replace("%ih3",
                        String.format("%03d", initialTime.hour));
            }
            if (retString.indexOf("%in2") >= 0) {
                retString = retString.replace("%in2",
                        String.format("%02d", initialTime.minute));
            }
            // any time
            // decade
            if (retString.indexOf("%x1") >= 0) {
                retString = retString.replace("%x1",
                        String.format("%d", ts.year / 10));
            }
            if (retString.indexOf("%x3") >= 0) {
                retString = retString.replace("%x3",
                        String.format("%03d", ts.year / 10));
            }
            // year
            if (retString.indexOf("%y2") >= 0) {
                int cent = ts.year / 100;
                int val  = ts.year - cent * 100;
                retString = retString.replace("%y2",
                        String.format("%02d", val));
            }
            if (retString.indexOf("%y4") >= 0) {
                retString = retString.replace("%y4",
                        String.format("%d", ts.year));
            }
            // month
            if (retString.indexOf("%m1") >= 0) {
                retString = retString.replace("%m1",
                        String.format("%d", ts.month));
            }
            if (retString.indexOf("%m2") >= 0) {
                retString = retString.replace("%m2",
                        String.format("%02d", ts.month));
            }
            if (retString.indexOf("%mc") >= 0) {
                retString = retString.replace("%mc",
                        GradsTimeStruct.months[ts.month - 1]);
            }
            // day
            if (retString.indexOf("%d1") >= 0) {
                retString = retString.replace("%d1",
                        String.format("%d", ts.day));
            }
            if (retString.indexOf("%d2") >= 0) {
                retString = retString.replace("%d2",
                        String.format("%02d", ts.day));
            }
            // hour
            if (retString.indexOf("%h1") >= 0) {
                retString = retString.replace("%h1",
                        String.format("%d", ts.hour));
            }
            if (retString.indexOf("%h2") >= 0) {
                retString = retString.replace("%h2",
                        String.format("%02d", ts.hour));
            }
            if (retString.indexOf("%h3") >= 0) {
                retString = retString.replace("%h3",
                        String.format("%03d", ts.hour));
            }
            // minute
            if (retString.indexOf("%n2") >= 0) {
                retString = retString.replace("%n2",
                        String.format("%02d", ts.minute));
            }
            // julian day
            if (retString.indexOf("%j3") >= 0) {
                retString = retString.replace("%j3",
                        String.format("%03d", ts.jday));
            }
            // time index (1 based)
            if (retString.indexOf("%t1") >= 0) {
                retString = retString.replace("%t1",
                        String.format("%d", timeIndex + 1));
            }
            if (retString.indexOf("%t2") >= 0) {
                retString = retString.replace("%t2",
                        String.format("%02d", timeIndex + 1));
            }
            if (retString.indexOf("%t3") >= 0) {
                retString = retString.replace("%t3",
                        String.format("%03d", timeIndex + 1));
            }
            if (retString.indexOf("%t4") >= 0) {
                retString = retString.replace("%t4",
                        String.format("%04d", timeIndex + 1));
            }
            if (retString.indexOf("%t5") >= 0) {
                retString = retString.replace("%t5",
                        String.format("%05d", timeIndex + 1));
            }
            if (retString.indexOf("%t6") >= 0) {
                retString = retString.replace("%t6",
                        String.format("%06d", timeIndex + 1));
            }
            // time index (0 based)
            if (retString.indexOf("%tm1") >= 0) {
                retString = retString.replace("%tm1",
                        String.format("%d", timeIndex));
            }
            if (retString.indexOf("%tm2") >= 0) {
                retString = retString.replace("%tm2",
                        String.format("%02d", timeIndex));
            }
            if (retString.indexOf("%tm3") >= 0) {
                retString = retString.replace("%tm3",
                        String.format("%03d", timeIndex));
            }
            if (retString.indexOf("%tm4") >= 0) {
                retString = retString.replace("%tm4",
                        String.format("%04d", timeIndex));
            }
            if (retString.indexOf("%tm5") >= 0) {
                retString = retString.replace("%tm5",
                        String.format("%05d", timeIndex));
            }
            if (retString.indexOf("%tm6") >= 0) {
                retString = retString.replace("%tm6",
                        String.format("%06d", timeIndex));
            }
            // forecast hours
            if (retString.indexOf("%f") >= 0) {
                int mins = (int) getValues()[timeIndex] * 60;
                int tdif;
                if (retString.indexOf("%f2") >= 0) {
                    format = "%02d";
                    tdif   = mins / 60;
                    if (tdif > 99) {
                        format = "%d";
                    }
                    retString = retString.replace("%f2",
                            String.format(format, tdif));
                }
                if (retString.indexOf("%f3") >= 0) {
                    format = "%03d";
                    tdif   = mins / 60;
                    if (tdif > 999) {
                        format = "%d";
                    }
                    retString = retString.replace("%f3",
                            String.format(format, tdif));
                }
                if (retString.indexOf("%fn2") >= 0) {
                    format = "%02d";
                    if (mins > 99) {
                        format = "%d";
                    }
                    retString = retString.replace("%fn2",
                            String.format(format, mins));
                }
                if (retString.indexOf("%fhn2") >= 0) {
                    tdif = mins;
                    int hrs = tdif / 60;
                    int mns = tdif - (hrs * 60);
                    format = "%02d%02d";
                    if (hrs > 99) {
                        format = "%d%02d";
                    }
                    retString = retString.replace("%fhn2",
                            String.format(format, hrs, mns));
                }
                if (retString.indexOf("%fdhn2") >= 0) {
                    tdif = mins;
                    int dys = tdif / 1440;
                    int hrs = (tdif - (dys * 1440)) / 60;
                    int mns = tdif - (dys * 1440) - (hrs * 60);
                    format = "%02d%02d%02d";
                    if (dys > 99) {
                        format = "%d%02d%02d";
                    }
                    retString = retString.replace("%fdhn2",
                            String.format(format, dys, hrs, mns));
                }
            }
        }
        return retString;

    }

}

