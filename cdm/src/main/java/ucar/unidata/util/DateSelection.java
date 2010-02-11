/*
 * $Id: DateSelection.java,v 1.18 2007/05/22 23:33:11 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */




package ucar.unidata.util;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 * Holds state for constructing time based queries.
 */

public class DateSelection {

    /** date formatter */
    private static SimpleDateFormat sdf;

    /** url argument names */
    private static final String ARG_PREFIX = "dateselection.";

    /** url argument names */
    public static final String ARG_STARTMODE = ARG_PREFIX + "start_mode";

    /** url argument names */
    public static final String ARG_ENDMODE = ARG_PREFIX + "end_mode";

    /** url argument names */
    public static final String ARG_STARTFIXEDTIME = ARG_PREFIX
                                                    + "start_fixedtime";

    /** url argument names */
    public static final String ARG_ENDFIXEDTIME = ARG_PREFIX
                                                  + "end_fixedtime";

    /** url argument names */
    public static final String ARG_STARTOFFSET = ARG_PREFIX + "start_offset";

    /** url argument names */
    public static final String ARG_ENDOFFSET = ARG_PREFIX + "end_offset";

    /** url argument names */
    public static final String ARG_POSTRANGE = ARG_PREFIX + "postrange";

    /** url argument names */
    public static final String ARG_PRERANGE = ARG_PREFIX + "prerange";

    /** url argument names */
    public static final String ARG_INTERVAL = ARG_PREFIX + "interval";

    /** url argument names */
    public static final String ARG_ROUNDTO = ARG_PREFIX + "roundto";

    /** url argument names */
    public static final String ARG_COUNT = ARG_PREFIX + "count";

    /** url argument names */
    public static final String ARG_SKIP = ARG_PREFIX + "skip";



    /** debug flag */
    public boolean debug = false;


    /*
      The time modes determine how we define the start and end time.
     */

    /** The mode for when we have an absolute time as a range bounds */
    public static final int TIMEMODE_FIXED = 0;

    /** The mode for when we use the current time */
    public static final int TIMEMODE_CURRENT = 1;

    /** When one of the ranges is relative to another */
    public static final int TIMEMODE_RELATIVE = 2;

    /** Mode for using the first or last time in the data set. Not sure if this will be useful here */
    public static final int TIMEMODE_DATA = 3;


    /** string ids for time modes */
    public static String[] TIMEMODESTRINGS = { "FIXED", "CURRENT", "RELATIVE",
            "DATA" };


    /** Mode for constructing set */
    public static int[] TIMEMODES = { TIMEMODE_FIXED, TIMEMODE_CURRENT,
                                      TIMEMODE_RELATIVE };

    /** Mode for constructing set */
    public static String[] STARTMODELABELS = { "Fixed", "Current Time (Now)",
            "Relative to End Time  " };

    /** Mode for constructing set */
    public static String[] ENDMODELABELS = { "Fixed", "Current Time (Now)",
                                             "Relative to Start Time" };


    /** Start mode */
    private int startMode = TIMEMODE_FIXED;

    /** End mode */
    private int endMode = TIMEMODE_FIXED;

    /** The start fixed time  in milliseconds */
    private long startFixedTime = Long.MAX_VALUE;

    /** The end fixed time  in milliseconds */
    private long endFixedTime = Long.MAX_VALUE;

    /** Start offset */
    private double startOffset = 0;

    /** End offset */
    private double endOffset = 0;

    /** The skip factor */
    private int skip = 0;

    /** The range before before the interval mark */
    private double preRange = Double.NaN;

    /** The range after the interval mark */
    private double postRange = Double.NaN;

    /** Interval time */
    private double interval = Double.NaN;

    /** milliseconds to round to */
    private double roundTo = 0;

    /** The total count of times we want */
    private int count = MAX_COUNT;

    /** maximum count */
    public static final int MAX_COUNT = Integer.MAX_VALUE;

    /** How many times do we choose within a given interval range */
    private int numTimesInRange = 1;


    /** This can hold a set of absolute times. If non-null then these times override any of the query information */
    private List times;

    /** flag for using the latest */
    private boolean doLatest = false;

    /** flag for using all */
    private boolean doAll = false;

    /** now  time */
    private Date nowTime;

    /**
     * ctor
     */
    public DateSelection() {}


    /**
     * ctor for instantiating a DateSelection object from a set of url argument originally created from toUrlString
     *
     * @param args set of name value pairs as created by getUrlArgs
     */
    public DateSelection(Hashtable<String, String> args) {
        String s;

        s = args.get(ARG_STARTMODE);
        if (s != null) {
            boolean ok = false;
            for (int i = 0; i < TIMEMODESTRINGS.length; i++) {
                if (TIMEMODESTRINGS[i].equals(s)) {
                    startMode = TIMEMODES[i];
                    ok        = true;
                    break;
                }
            }
            if ( !ok) {
                throw new IllegalArgumentException("Unknown " + ARG_STARTMODE
                        + " argument:" + s);
            }
        }

        s = args.get(ARG_ENDMODE);
        if (s != null) {
            boolean ok = false;
            for (int i = 0; i < TIMEMODESTRINGS.length; i++) {
                if (TIMEMODESTRINGS[i].equals(s)) {
                    endMode = TIMEMODES[i];
                    ok      = true;
                    break;
                }
            }
            if ( !ok) {
                throw new IllegalArgumentException("Unknown " + ARG_ENDMODE
                        + " argument:" + s);
            }
        }



        s = args.get(ARG_STARTFIXEDTIME);
        if (s != null) {
            try {
                startFixedTime = parseDate(s).getTime();
            } catch (java.text.ParseException pe) {
                throw new IllegalArgumentException(
                    "Incorrect date format for argument "
                    + ARG_STARTFIXEDTIME + " " + s);
            }
        }


        s = args.get(ARG_ENDFIXEDTIME);
        if (s != null) {
            try {
                endFixedTime = parseDate(s).getTime();
            } catch (java.text.ParseException pe) {
                throw new IllegalArgumentException(
                    "Incorrect date format for argument " + ARG_ENDFIXEDTIME
                    + " " + s);
            }
        }


        roundTo     = toDouble(args.get(ARG_ROUNDTO), roundTo);
        interval    = toDouble(args.get(ARG_INTERVAL), interval);
        postRange   = toDouble(args.get(ARG_POSTRANGE), postRange);
        preRange    = toDouble(args.get(ARG_PRERANGE), preRange);
        startOffset = toDouble(args.get(ARG_STARTOFFSET), startOffset);
        endOffset   = toDouble(args.get(ARG_ENDOFFSET), endOffset);
        skip        = (int) toDouble(args.get(ARG_SKIP), skip);
        count       = (int) toDouble(args.get(ARG_COUNT), count);


    }


    /**
     * helper method that converts a string to a double. If s is null then return  dflt
     *
     * @param s the string
     * @param dflt default value
     *
     * @return the double
     */
    private double toDouble(String s, double dflt) {
        if (s == null) {
            return dflt;
        }
        return new Double(s).doubleValue();
    }





    /**
     * ctor
     * @param doLatest Do the count latest ones
     * @param count the count
     */
    public DateSelection(boolean doLatest, int count) {
        this.doLatest = doLatest;
        this.count    = count;
    }


    /**
     * ctor
     * @param doAll Get all available.
     */

    public DateSelection(boolean doAll) {
        this.doAll = doAll;
    }



    /**
     * Construct a DateSelection
     *
     * @param startMode  starting time mode
     * @param startOffset  offset from start time
     * @param endMode end time mode
     * @param endOffset offset from end time
     */
    public DateSelection(int startMode, double startOffset, int endMode,
                         double endOffset) {
        this.startMode   = startMode;
        this.startOffset = startOffset;
        this.endMode     = endMode;
        this.endOffset   = endOffset;
    }

    /**
     * ctor
     *
     * @param startTime start time
     * @param endTime end time
     */
    public DateSelection(Date startTime, Date endTime) {
        if (startTime != null) {
            this.startFixedTime = startTime.getTime();
        }
        if (endTime != null) {
            this.endFixedTime = endTime.getTime();
        }
        startMode = TIMEMODE_FIXED;
        endMode   = TIMEMODE_FIXED;
        interval  = 0;
    }



    /**
     * copy ctor
     *
     * @param that object to copy from
     */
    public DateSelection(DateSelection that) {
        this.startMode      = that.startMode;
        this.endMode        = that.endMode;

        this.startFixedTime = that.startFixedTime;
        this.endFixedTime   = that.endFixedTime;

        this.doLatest       = that.doLatest;
        this.nowTime        = that.nowTime;

        this.startOffset    = that.startOffset;
        this.endOffset      = that.endOffset;

        this.skip           = that.skip;

        this.postRange      = that.postRange;
        this.preRange       = that.preRange;

        this.interval       = that.interval;
        this.roundTo        = that.roundTo;
        this.count          = that.count;
    }



    /**
     * Generate an array of times for the interval
     *
     * @return intervals
     */
    public double[] getIntervalTicks() {
        Date[] range         = getRange();
        long   startTime     = range[0].getTime();
        long   endTime       = range[1].getTime();
        double tickStartTime = startTime - interval;
        double tickEndTime   = endTime + interval;
        double base          = round(tickEndTime);
        //        System.err.println("base:" + new Date((long) base));
        return computeTicks(tickEndTime, tickStartTime, base, interval);
    }


    /**
     * Apply this date selection query to the list of DatedThing-s
     *
     * @param times input list of DatedThing-s
     *
     * @return The filtered list
     */
    public List apply(List times) {

        List<DatedThing> datedThings = DatedObject.sort(times, false);

        List             result      = new ArrayList();
        Date[]           range       = getRange(datedThings);

        long             startTime   = range[0].getTime();
        long             endTime     = range[1].getTime();
        boolean          hasInterval = hasInterval();

        //Get the interval ranges to use
        double beforeRange = getPreRangeToUse();
        double afterRange  = getPostRangeToUse();

        if (debug) {
            System.err.println("range:" + range[0] + " -- " + range[1]);
        }

        double[] ticks = null;
        if (hasInterval) {
            //Pad the times with the interval so we handle the edge cases
            ticks = getIntervalTicks();
            if (ticks == null) {
                return result;
            }
            for (int i = 0; i < ticks.length; i++) {
                if (debug) {
                    System.err.println(
                        "Interval " + i + ": "
                        + new Date((long) (ticks[i] - beforeRange)) + " -- "
                        + new Date((long) (ticks[i])) + " -- "
                        + new Date((long) (ticks[i] + afterRange)));
                }
            }
        }


        int totalThings = 0;
        //        List[] intervalList = new List[ticks.length];
        DatedThing[] closest         = null;
        double[]     minDistance     = null;
        int          currentInterval = 0;

        if (ticks != null) {
            closest         = new DatedThing[ticks.length];
            minDistance     = new double[ticks.length];
            currentInterval = ticks.length - 1;
        }

        //Remember, we're going backwards in time
        int skipCnt = 0;
        for (int i = 0; i < datedThings.size(); i++) {
            //Have we maxed out?
            //TODO: take into account skip
            if (hasInterval) {
                if (totalThings >= count) {
                    if (skip > 0) {
                        if (totalThings / (skip + 1) >= count) {
                            break;

                        }
                    } else {
                        break;
                    }
                }
            }

            DatedThing datedThing = (DatedThing) datedThings.get(i);
            long       time       = datedThing.getDate().getTime();

            //Check the time range bounds
            if (time > endTime) {
                if (debug) {
                    System.err.println("after range:" + datedThing);
                }
                continue;
            }
            //We're done
            if (time < startTime) {
                if (debug) {
                    System.err.println("before range:" + datedThing);
                }
                break;
            }


            //If no interval then just add it
            if ( !hasInterval) {
                if (skip == 0) {
                    result.add(datedThing);
                } else {
                    if (skipCnt == 0) {
                        result.add(datedThing);
                    }
                    skipCnt++;
                    if (skipCnt >= skip + 1) {
                        skipCnt = 0;
                    }
                }
                if (result.size() >= count) {
                    break;
                }
                continue;
            }


            while ((currentInterval >= 0)
                    && (ticks[currentInterval] - beforeRange > time)) {
                currentInterval--;
            }

            //Done
            if (currentInterval < 0) {
                break;
            }


            boolean thingInInterval = ((time
                                        >= (ticks[currentInterval]
                                            - beforeRange)) && (time
                                                <= (ticks[currentInterval]
                                                    + afterRange)));

            if ( !thingInInterval) {
                if (debug) {
                    System.err.println("Not in interval:" + datedThing);
                }
                continue;
            }

            double distance = Math.abs(time - ticks[currentInterval]);
            if ((closest[currentInterval] == null)
                    || (distance < minDistance[currentInterval])) {
                if (closest[currentInterval] == null) {
                    totalThings++;
                }
                closest[currentInterval]     = datedThing;
                minDistance[currentInterval] = distance;
            }

            //            if(intervalList[currentInterval]==null) {
            //                intervalList[currentInterval] = new ArrayList();
            //            }
            //intervalList[currentInterval].add(datedThing);

        }


        //If we had intervals then add them in
        if (closest != null) {
            skipCnt = 0;
            for (int i = closest.length - 1; i >= 0; i--) {
                DatedThing datedThing = closest[i];
                if (datedThing == null) {
                    continue;
                }
                if (skip == 0) {
                    result.add(datedThing);
                } else {
                    if (skipCnt == 0) {
                        result.add(datedThing);
                    }
                    skipCnt++;
                    if (skipCnt >= skip + 1) {
                        skipCnt = 0;
                    }
                }
            }
        }

        return result;


    }



    /**
     * Compute the tick mark values based on the input.  Cut-and-pasted from Misc
     *
     * @param high  highest value of range
     * @param low   low value of range
     * @param base  base value for centering ticks
     * @param interval  interval between ticks
     *
     * @return  array of computed tick values
     */
    private static double[] computeTicks(double high, double low,
                                         double base, double interval) {
        double[] vals = null;

        //        System.err.println ("ticks:" + high + " " + low +" " + base + " " + interval);


        // compute nlo and nhi, for low and high contour values in the box
        long nlo = Math.round((Math.ceil((low - base) / Math.abs(interval))));
        long nhi = Math.round((Math.floor((high - base)
                                          / Math.abs(interval))));

        // how many contour lines are needed.
        int numc = (int) (nhi - nlo) + 1;
        if (numc < 1) {
            return vals;
        }

        vals = new double[numc];

        for (int i = 0; i < numc; i++) {
            vals[i] = base + (nlo + i) * interval;
        }

        return vals;
    }




    /**
     * Construct and return the start and end time range
     *
     * @return time range. If in doLatest or doAll mode this returns null
     */
    public Date[] getRange() {
        return getRange(null);
    }

    /**
     * _more_
     *
     * @param dataTimes _more_
     *
     * @return _more_
     */
    public Date[] getRange(List<DatedThing> dataTimes) {
        if (doLatest) {
            return null;
        }
        if (doAll) {
            return null;
        }
        double now   = (double) (((nowTime != null)
                                  ? nowTime.getTime()
                                  : System.currentTimeMillis()));
        double start = 0;
        double end   = 0;



        if (startMode == TIMEMODE_CURRENT) {
            start = now;
        } else if (startMode == TIMEMODE_FIXED) {
            start = startFixedTime;
        } else if (startMode == TIMEMODE_DATA) {
            if (dataTimes != null) {
                start = dataTimes.get(dataTimes.size()
                                      - 1).getDate().getTime();
            }
        }

        if (endMode == TIMEMODE_CURRENT) {
            end = now;
        } else if (endMode == TIMEMODE_FIXED) {
            end = endFixedTime;
        } else if (startMode == TIMEMODE_DATA) {
            if (dataTimes != null) {
                end = dataTimes.get(0).getDate().getTime();
            }
        }


        if (startMode != TIMEMODE_RELATIVE) {
            start += startOffset;
        }

        if (endMode != TIMEMODE_RELATIVE) {
            end += endOffset;
        }

        if (startMode == TIMEMODE_RELATIVE) {
            start = end + startOffset;
        }

        if (endMode == TIMEMODE_RELATIVE) {
            end = start + endOffset;
        }



        Date startDate = new Date((long) start);
        Date endDate   = new Date((long) end);

        return new Date[] { startDate, endDate };
    }








    /**
     * Utility to round the given seconds
     *
     * @param milliSeconds time to round
     *
     * @return Rounded value
     */
    private double round(double milliSeconds) {
        return roundTo(roundTo, milliSeconds);
    }


    /**
     * Utility to round the given seconds
     *
     *
     * @param roundTo round to
     * @param milliSeconds time to round
     *
     * @return Rounded value
     */
    public static double roundTo(double roundTo, double milliSeconds) {
        if (roundTo == 0) {
            return milliSeconds;
        }
        double seconds   = milliSeconds / 1000;
        double rtSeconds = roundTo / 1000;
        return 1000 * (seconds - ((int) seconds) % rtSeconds);
    }


    /**
     * Create the time set
     *
     * @return The time set
     *
     */
    protected Object makeTimeSet() {
        return null;
        /*
        List       dateTimes    = new ArrayList();
        double       now = (double) (System.currentTimeMillis() / 1000);
        double     startSeconds = 0;
        double     endSeconds   = 0;
        double[][] dataTimeSet  = null;

        //        System.err.println ("makeTimeSet");
        if ((startMode == TIMEMODE_DATA) || (endMode == TIMEMODE_DATA)) {
            Set timeSet = (baseTimes!=null?baseTimes:displayMaster.getAnimationSetFromDisplayables());
            if (timeSet != null) {
                dataTimeSet = timeSet.getDoubles();
            }
            if ((dataTimeSet == null) || (dataTimeSet.length == 0)
                    || (dataTimeSet[0].length == 0)) {
                //                System.err.println ("\tdata is null");
                return null;
            }
        }
        double interval = 60 * getInterval();
        if (interval == 0) {
            return null;
        }

        if (startMode == TIMEMODE_DATA) {
            double minValue = dataTimeSet[0][0];
            for (int i = 1; i < dataTimeSet[0].length; i++) {
                minValue = Math.min(minValue, dataTimeSet[0][i]);
            }
            startSeconds = minValue;
        } else if (startMode == TIMEMODE_CURRENT) {
            startSeconds = now;
        } else if (startMode == TIMEMODE_FIXED) {
            startSeconds = startFixedTime / 1000;
        }

        if (endMode == TIMEMODE_DATA) {
            double maxValue = dataTimeSet[0][0];
            for (int i = 1; i < dataTimeSet[0].length; i++) {
                maxValue = Math.max(maxValue, dataTimeSet[0][i]);
            }
            endSeconds = maxValue;
        } else if (endMode == TIMEMODE_CURRENT) {
            endSeconds = now;
        } else if (endMode == TIMEMODE_FIXED) {
            endSeconds = endFixedTime / 1000;
        }


        if (startMode != TIMEMODE_RELATIVE) {
            startSeconds += startOffset * 60;
            startSeconds = round(startSeconds);
        }
        if (endMode != TIMEMODE_RELATIVE) {
            endSeconds += endOffset * 60;
            //      double foo = endSeconds;
            endSeconds = round(endSeconds);
            //      System.err.println("before:" + ((int)foo) +" after:" + ((int)endSeconds));
        }
        if (startMode == TIMEMODE_RELATIVE) {
            startSeconds = endSeconds + startOffset * 60;
            startSeconds = round(startSeconds);
        }

        if (endMode == TIMEMODE_RELATIVE) {
            endSeconds = startSeconds + endOffset * 60;
            endSeconds = round(endSeconds);
        }

        //      System.err.println("start:" + startSeconds +" end:" + endSeconds);
        //        System.err.println("");


        double cnt = (int) ((double) (endSeconds - startSeconds)) / interval;
        if (cnt > 10000) {
            throw new IllegalStateException("Too many times in animation set:"
                                            + cnt);
        }
        while (startSeconds <= endSeconds) {
            //      System.err.print (" " + startSeconds);
            dateTimes.add(0, new Date(startSeconds));
            startSeconds += interval;
        }
        //      System.err.println ("");
        if (dateTimes.size() == 0) {
            return null;
        }
        return makeTimeSet(dateTimes);
        */
    }




    /**
     * Set the StartMode property.
     *
     * @param value The new value for StartMode
     */
    public void setStartMode(int value) {
        startMode = value;
    }

    /**
     * Get the StartMode property.
     *
     * @return The StartMode
     */
    public int getStartMode() {
        return startMode;
    }

    /**
     * Set the EndMode property.
     *
     * @param value The new value for EndMode
     */
    public void setEndMode(int value) {
        endMode = value;
    }

    /**
     * Get the EndMode property.
     *
     * @return The EndMode
     */
    public int getEndMode() {
        return endMode;
    }


    /**
     * Do we have an interval defined
     *
     * @return Have interval defined
     */
    public boolean hasInterval() {
        return (interval == interval) && (interval > 0);
    }

    /**
     * Do we have a pre range defined
     *
     * @return Is pre-range defined
     */
    public boolean hasPreRange() {
        return preRange == preRange;
    }

    /**
     * Do we have a post range defined
     *
     * @return Is post-range defined
     */
    public boolean hasPostRange() {
        return postRange == postRange;
    }


    /**
     * Set the Interval property.
     *
     * @param value The new value for Interval
     */
    public void setInterval(double value) {
        interval = value;
    }

    /**
     * Get the Interval property.
     *
     * @return The Interval
     */
    public double getInterval() {
        return interval;
    }




    /**
     * Set the StartOffset property.
     *
     * @param value The new value for StartOffset
     */
    public void setStartOffset(double value) {
        startOffset = value;
    }

    /**
     * Get the StartOffset property.
     *
     * @return The StartOffset
     */
    public double getStartOffset() {
        return startOffset;
    }

    /**
     * Set the EndOffset property.
     *
     * @param value The new value for EndOffset
     */
    public void setEndOffset(double value) {
        endOffset = value;
    }

    /**
     * Get the EndOffset property.
     *
     * @return The EndOffset
     */
    public double getEndOffset() {
        return endOffset;
    }

    /**
     * Set the RoundTo property.
     *
     * @param value The new value for RoundTo
     */
    public void setRoundTo(double value) {
        roundTo = value;
    }

    /**
     * Get the RoundTo property.
     *
     * @return The RoundTo
     */
    public double getRoundTo() {
        return roundTo;
    }

    /**
     *  Set the StartFixedTime property.
     *
     *  @param value The new value for StartFixedTime
     */
    public void setStartFixedTime(long value) {
        startFixedTime = value;
    }


    /**
     * set property
     *
     * @param d property
     */
    public void setStartFixedTime(Date d) {
        startFixedTime = d.getTime();
        startMode      = TIMEMODE_FIXED;
    }


    /**
     * set property
     *
     * @param d property
     */
    public void setEndFixedTime(Date d) {
        endFixedTime = d.getTime();
        endMode      = TIMEMODE_FIXED;
    }

    /**
     * get the property
     *
     * @return property
     */
    public Date getStartFixedDate() {
        return new Date(getStartFixedTime());
    }


    /**
     * get the property
     *
     * @return property
     */
    public Date getEndFixedDate() {
        return new Date(getEndFixedTime());
    }

    /**
     *  Get the StartFixedTime property.
     *
     *  @return The StartFixedTime
     */
    public long getStartFixedTime() {
        return startFixedTime;
    }

    /**
     *  Set the EndFixedTime property.
     *
     *  @param value The new value for EndFixedTime
     */
    public void setEndFixedTime(long value) {
        endFixedTime = value;
    }

    /**
     *  Get the EndFixedTime property.
     *
     *  @return The EndFixedTime
     */
    public long getEndFixedTime() {
        return endFixedTime;
    }


    /**
     * A utility method to set the pre and post range symmetrically.
     * Each are set with half of the given value
     *
     * @param value interval range
     */
    public void setIntervalRange(double value) {
        setPreRange(value / 2);
        setPostRange(value / 2);
    }


    /**
     * Set the PreRange property.
     *
     * @param value The new value for PreRange
     */
    public void setPreRange(double value) {
        preRange = value;
    }


    /**
     * Get the pre interval range to use. If we have a preRange then return that, else,
     * return half of the interval.
     *
     * @return The pre range to use
     */
    public double getPreRangeToUse() {
        return (hasPreRange()
                ? preRange
                : interval / 2);
    }


    /**
     * Get the post interval range to use. If we have a postRange then return that, else,
     * return half of the interval.
     *
     * @return The post range to use
     */
    public double getPostRangeToUse() {
        return (hasPostRange()
                ? postRange
                : interval / 2);
    }


    /**
     * Get the PreRange property.
     *
     * @return The PreRange
     */
    public double getPreRange() {
        return preRange;
    }

    /**
     * Set the PostRange property.
     *
     * @param value The new value for PostRange
     */
    public void setPostRange(double value) {
        postRange = value;
    }

    /**
     * Get the PostRange property.
     *
     * @return The PostRange
     */
    public double getPostRange() {
        return postRange;
    }



    /**
     * Set the Count property.
     *
     * @param value The new value for Count
     */
    public void setCount(int value) {
        count = value;
    }

    /**
     * Does this date selection have a valid count
     *
     * @return has a  count
     */
    public boolean hasCount() {
        return count != Integer.MAX_VALUE;
    }


    /**
     * Get the Count property.
     *
     * @return The Count
     */
    public int getCount() {
        return count;
    }


    /**
     * Set the NumTimesInRange property.
     *
     * @param value The new value for NumTimesInRange
     */
    public void setNumTimesInRange(int value) {
        numTimesInRange = value;
    }

    /**
     * Get the NumTimesInRange property.
     *
     * @return The NumTimesInRange
     */
    public int getNumTimesInRange() {
        return numTimesInRange;
    }


    /**
     * Set the Times property.
     *
     * @param value The new value for Times
     */
    public void setTimes(List value) {
        times = value;
    }

    /**
     * Get the Times property.
     *
     * @return The Times
     */
    public List getTimes() {
        return times;
    }




    /**
     * Get the hashcode for this object
     *
     * @return the hashcode
     */
    public int hashCode() {
        int hashCode = 0;
        if (times != null) {
            hashCode ^= times.hashCode();
        }
        return hashCode ^ new Double(this.startMode).hashCode()
               ^ new Double(this.endMode).hashCode()
               ^ new Double(this.startFixedTime).hashCode()
               ^ new Double(this.endFixedTime).hashCode()
               ^ new Double(this.startOffset).hashCode()
               ^ new Double(this.endOffset).hashCode()
               ^ new Double(this.skip).hashCode()
               ^ new Double(this.postRange).hashCode()
               ^ new Double(this.preRange).hashCode()
               ^ new Double(this.interval).hashCode()
               ^ new Double(this.roundTo).hashCode() ^ this.numTimesInRange
               ^ this.count;
    }

    /**
     * equals method
     *
     * @param o object to check
     *
     * @return equals
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DateSelection)) {
            return false;
        }
        DateSelection that = (DateSelection) o;

        if (this.times != that.times) {
            return false;
        }
        if ((this.times != null) && !this.times.equals(that.times)) {
            return false;
        }

        return (this.startMode == that.startMode)
               && (this.endMode == that.endMode)
               && (this.startFixedTime == that.startFixedTime)
               && (this.endFixedTime == that.endFixedTime)
               && (this.startOffset == that.startOffset)
               && (this.endOffset == that.endOffset)
               && (this.skip == that.skip)
               && (this.postRange == that.postRange)
               && (this.preRange == that.preRange)
               && (this.interval == that.interval)
               && (this.roundTo == that.roundTo)
               && (this.numTimesInRange == that.numTimesInRange)
               && (this.count == that.count);

    }

    /**
     * Set the Skip property.
     *
     * @param value The new value for Skip
     */
    public void setSkip(int value) {
        skip = value;
    }

    /**
     * Get the Skip property.
     *
     * @return The Skip
     */
    public int getSkip() {
        return skip;
    }





    /**
     * test
     *
     * @param msg msg to print out
     */
    private void testRange(String msg) {
        Date[] range = getRange();
        if (msg != null) {
            System.err.println(msg);
        }
        System.err.println(range[0] + " --  " + range[1]);
    }

    /**
     *  Set the DoLatest property.
     *
     *  @param value The new value for DoLatest
     */
    public void setDoLatest(boolean value) {
        doLatest = value;
    }


    /**
     * Select the most recent thing
     *
     * @return select the most recent thing
     */
    public boolean isLatest() {
        return doLatest;
    }

    /**
     *  Get the DoLatest property.
     *
     *  @return The DoLatest
     */
    public boolean getDoLatest() {
        return doLatest;
    }

    /**
     * Set the DoAll property.
     *
     * @param value The new value for DoAll
     */
    public void setDoAll(boolean value) {
        doAll = value;
    }

    /**
     * Select all things
     *
     * @return select all things
     */
    public boolean isAll() {
        return doAll;
    }

    /**
     * Get the DoAll property.
     *
     * @return The DoAll
     */
    public boolean getDoAll() {
        return doAll;
    }




    /**
     *  Set the NowTime property.
     *
     *  @param value The new value for NowTime
     */
    public void setNowTime(Date value) {
        nowTime = value;
    }

    /**
     *  Get the NowTime property.
     *
     *  @return The NowTime
     */
    public Date getNowTime() {
        return nowTime;
    }





    /**
     * test main
     *
     * @param cmdLineArgs cmd line args
     *
     * @throws Exception on badness
     */
    public static void main(String[] cmdLineArgs) throws Exception {
        DateSelection dateSelection = new DateSelection();
        List          dates         = new ArrayList();
        long now = dateSelection.parseDate(
                       dateSelection.formatDate(
                           new Date(System.currentTimeMillis()))).getTime();
        for (int i = 0; i < 20; i++) {
            dates.add(new DatedObject(new Date(now
                    + DateUtil.minutesToMillis(20) - i * 10 * 60 * 1000)));
        }

        dateSelection.setEndMode(TIMEMODE_FIXED);
        dateSelection.setEndFixedTime(now);

        //Go 2 hours before start
        dateSelection.setStartMode(TIMEMODE_RELATIVE);
        dateSelection.setStartOffset(DateUtil.hoursToMillis(-2));

        //15 minute interval
        dateSelection.setRoundTo(DateUtil.hoursToMillis(12));

        dateSelection.setInterval(DateUtil.minutesToMillis(15));
        dateSelection.setIntervalRange(DateUtil.minutesToMillis(6));


        dates = dateSelection.apply(dates);
        //        System.err.println("result:" + dates);


        Hashtable<String, String> args = new Hashtable<String, String>();
        dateSelection.getUrlArgs(args);
        System.err.println("url string:" + dateSelection.toUrlString());

        DateSelection dateSelection2 = new DateSelection(args);
        if ( !dateSelection.equals(dateSelection2)) {
            System.err.println("date selection 2 != date selection");
            System.err.println(dateSelection);
            System.err.println(dateSelection2);
        } else {
            System.err.println("date selection 2 == date selection");
        }
    }



    /**
     * tostring
     *
     * @return tostring
     */
    public String toString() {
        return " startMode      =" + startMode + "\n" + " endMode        ="
               + this.endMode + "\n" + " startFixedTime ="
               + this.startFixedTime + "\n" + " endFixedTime   ="
               + this.endFixedTime + "\n" + " startOffset    ="
               + this.startOffset + "\n" + " endOffset      ="
               + this.endOffset + "\n" + " postRange      =" + this.postRange
               + "\n" + " preRange       =" + this.preRange + "\n"
               + " interval       =" + this.interval + "\n"
               + " roundTo        =" + this.roundTo + "\n"
               + " count          =" + count + "\n";

    }



    /**
     * helper method to create a url arg
     *
     * @param name url arg name
     * @param value value
     *
     * @return the name=value string
     */
    private static String urlArg(String name, String value) {
        return "&" + name + "=" + value;
    }

    /**
     * format the date as ISO
     *
     * @param d the date
     *
     * @return the formatted date
     */
    private String formatDate(Date d) {
        if (sdf == null) {
            sdf = new SimpleDateFormat(DateUtil.DateFormatHandler
                .ISO_DATE_TIME.getDateTimeFormatString());
            sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        }
        return sdf.format(d);
    }

    /**
     * parse the iso date
     *
     * @param s date string
     *
     * @return date
     *
     * @throws java.text.ParseException on badness
     */
    private Date parseDate(String s) throws java.text.ParseException {
        if ((s == null) || (s.trim().length() == 0)) {
            return null;
        }
        if (sdf == null) {
            sdf = new SimpleDateFormat(DateUtil.DateFormatHandler
                .ISO_DATE_TIME.getDateTimeFormatString());
            sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        }
        return sdf.parse(s);
    }


    /**
     * This adds to the hashtable the set of name/value pairs that allow us to create a new DateSelection from a url argument string
     *
     *
     * @param args put the name value pairs in here
     */
    public void getUrlArgs(Hashtable<String, String> args) {
        args.put(ARG_STARTMODE, TIMEMODESTRINGS[startMode]);
        args.put(ARG_ENDMODE, TIMEMODESTRINGS[endMode]);
        if (startMode == TIMEMODE_FIXED) {
            args.put(ARG_STARTFIXEDTIME,
                     formatDate(new Date(startFixedTime)));
        }
        if (endMode == TIMEMODE_FIXED) {
            args.put(ARG_ENDFIXEDTIME, formatDate(new Date(endFixedTime)));
        }
        if (startOffset != 0) {
            args.put(ARG_STARTOFFSET, "" + startOffset);
        }
        if (endOffset != 0) {
            args.put(ARG_ENDOFFSET, "" + endOffset);
        }
        if (preRange == preRange) {
            args.put(ARG_PRERANGE, "" + preRange);
        }
        if (postRange == postRange) {
            args.put(ARG_POSTRANGE, "" + postRange);
        }
        if (interval == interval) {
            args.put(ARG_INTERVAL, "" + interval);
        }
        if (roundTo != 0) {
            args.put(ARG_ROUNDTO, "" + roundTo);
        }
        if (count != MAX_COUNT) {
            args.put(ARG_COUNT, "" + count);
        }
        if (skip != 0) {
            args.put(ARG_SKIP, "" + skip);
        }
    }




    /**
     * Create a url string to encode this object
     *
     * @return the url string
     */
    public String toUrlString() {
        StringBuffer              sb   = new StringBuffer();
        Hashtable<String, String> args = new Hashtable<String, String>();
        getUrlArgs(args);
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            String value = args.get(key);
            sb.append(urlArg(key, value));
        }
        return sb.toString();
    }




}

