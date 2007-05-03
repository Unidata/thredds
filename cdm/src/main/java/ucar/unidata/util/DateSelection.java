/*
 * $Id: DateSelection.java,v 1.4 2007/05/03 22:23:17 jeffmc Exp $
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


import java.util.ArrayList;
import java.util.Date;
import java.util.List;



/**
 * Holds state for constructing time based queries.
 */

public class DateSelection {

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


    /** Mode for constructing set */
    public static int[] TIMEMODES = { TIMEMODE_FIXED, TIMEMODE_CURRENT,
                                      TIMEMODE_RELATIVE, TIMEMODE_DATA };

    /** Mode for constructing set */
    public static String[] STARTMODELABELS = { "Fixed", "Current Time (Now)",
            "Relative to End Time", "From Data" };

    /** Mode for constructing set */
    public static String[] ENDMODELABELS = { "Fixed", "Current Time (Now)",
                                             "Relative to Start Time",
                                             "From Data" };

    /** Start mode */
    private int startMode = TIMEMODE_FIXED;

    /** End mode */
    private int endMode = TIMEMODE_FIXED;


    /** The start fixed time  in seconds */
    private long startFixedTime = Long.MAX_VALUE;

    /** The end fixed time  in seconds */
    private long endFixedTime = Long.MAX_VALUE;

    /** Start offset */
    private double startOffset = 0.0;

    /** End offset */
    private double endOffset = 0.0;

    /** The range before before the interval mark */
    private double preRange = Double.NaN;

    /** The range after the interval mark */
    private double postRange = Double.NaN;

    /** Interval time */
    private double interval = Double.NaN;

    /** Minutes to round to */
    private double roundTo = 0.0;

    /** The total count of times we want */
    private int count = Integer.MAX_VALUE;

    /** How many times do we choose within a given interval range */
    private int numTimesInRange = 1;


    /** This can hold a set of absolute times. If non-null then these times override any of the query information */
    private List times;



    /**
     * ctor
     */
    public DateSelection() {}

    /**
     * ctor
     *
     * @param startTime start time
     * @param endTime end time
     */
    public DateSelection(Date startTime, Date endTime) {
        this.startFixedTime = startTime.getTime();
        this.endFixedTime   = endTime.getTime();
        startMode           = TIMEMODE_FIXED;
        endMode             = TIMEMODE_FIXED;
        interval            = 0.0;
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

        this.startOffset    = that.startOffset;
        this.endOffset      = that.endOffset;


        this.postRange      = that.postRange;
        this.preRange       = that.preRange;

        this.interval       = that.interval;
        this.roundTo        = that.roundTo;
        this.count          = that.count;
    }



    /**
     * Apply this date seelction query to the list of DatedThing-s
     *
     * @param datedObjects input list of DatedThing-s
     *
     * @return The filtered list
     */
    public List apply(List datedObjects) {
        //TBD
        return datedObjects;

    }


    /**
     * Construct and return the start and end time range
     *
     * @return time range
     */
    public Date[] getRange() {
        //TBD
        return new Date[] {};
    }



    /**
     * Utility to round the given seconds
     *
     * @param seconds time to round
     *
     * @return Rounded value
     */
    private double round(double seconds) {
        return roundTo(roundTo, seconds);
    }


    /**
     * Utility to round the given seconds
     *
     *
     * @param roundTo round to
     * @param seconds time to round
     *
     * @return Rounded value
     */
    public static double roundTo(double roundTo, double seconds) {
        int roundToSeconds = (int) (roundTo * 60);
        if (roundToSeconds == 0) {
            return seconds;
        }
        return seconds - ((int) seconds) % roundToSeconds;
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
        long       now = (long) (System.currentTimeMillis() / 1000.0);
        double     startSeconds = 0.0;
        double     endSeconds   = 0.0;
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
    }


    /**
     * set property
     *
     * @param d property
     */
    public void setEndFixedTime(Date d) {
        endFixedTime = d.getTime();
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
        if (startFixedTime == Long.MAX_VALUE) {
            startFixedTime = System.currentTimeMillis();
        }
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
        if (endFixedTime == Long.MAX_VALUE) {
            endFixedTime = System.currentTimeMillis();
        }
        return endFixedTime;
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
               && (this.postRange == that.postRange)
               && (this.preRange == that.preRange)
               && (this.interval == that.interval)
               && (this.roundTo == that.roundTo)
               && (this.numTimesInRange == that.numTimesInRange)
               && (this.count == that.count);

    }



}

