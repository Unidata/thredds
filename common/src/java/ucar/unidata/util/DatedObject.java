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
package ucar.unidata.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;



/**
 * A utility class that implements DatedThing
 */

public class DatedObject implements DatedThing {

    /** The date */
    private Date date;

    /** The object */
    private Object object;

    /**
     * Default ctor
     */
    public DatedObject() {}

    /**
     * Construct this object with just a date
     *
     * @param date the date
     */
    public DatedObject(Date date) {
        this(date, null);
    }


    /**
     * Construct this object with  a date and an object
     *
     * @param date the date
     * @param object The object
     */
    public DatedObject(Date date, Object object) {
        this.date   = date;
        this.object = object;
    }


    /**
     * Select and return the DatedThings taht have dates between the two given dates.
     *
     * @param startDate  Start date
     * @param endDate End date
     * @param datedThings DatedThing-s to look at
     *
     * @return List of DatedThing-s that are between the given dates
     */
    public static List select(Date startDate, Date endDate,
                              List datedThings) {
        if (startDate.getTime() > endDate.getTime()) {
            Date tmp = startDate;
            startDate = endDate;
            endDate   = tmp;
        }
        long t1       = startDate.getTime();
        long t2       = endDate.getTime();
        List selected = new ArrayList();
        for (int i = 0; i < datedThings.size(); i++) {
            DatedThing datedThing = (DatedThing) datedThings.get(i);
            long       time       = datedThing.getDate().getTime();
            if ((time >= t1) && (time <= t2)) {
                selected.add(datedThing);
            }
        }
        return selected;
    }



    /**
     * A utility method that takes a list of dates and returns a list of DatedObjects
     *
     * @param dates List of dates to wrap
     * @return A list of DatedObjects
     */
    public static List wrap(List dates) {
        List result = new ArrayList();
        for (int i = 0; i < dates.size(); i++) {
            result.add(new DatedObject((Date) dates.get(i)));
        }
        return result;
    }

    /**
     * A utility method that takes a list of DatedThing-s and returns a list of Date-s
     *
     * @param datedThings List of dates to unwrap
     * @return A list of Dates
     */
    public static List unwrap(List datedThings) {
        List result = new ArrayList();
        for (int i = 0; i < datedThings.size(); i++) {
            result.add(((DatedThing) datedThings.get(i)).getDate());
        }
        return result;
    }

    /**
     * A utility method that takes a list of DatedObjects-s and returns a list of the objects
     *
     * @param datedObjects List of objects
     * @return A list of the objects the datedobjects hold
     */
    public static List getObjects(List datedObjects) {
        List result = new ArrayList();
        if (datedObjects == null) {
            return result;
        }
        for (int i = 0; i < datedObjects.size(); i++) {
            result.add(((DatedObject) datedObjects.get(i)).getObject());
        }
        return result;
    }


    /**
     * Sort the given list of DatedThing-s
     *
     * @param datedThings list to sort
     * @param ascending sort order
     *
     * @return sorted list
     */
    public static List sort(List datedThings,
                                        final boolean ascending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                DatedThing a1     = (DatedThing) o1;
                DatedThing a2     = (DatedThing) o2;
                int        result = a1.getDate().compareTo(a2.getDate());
                if ( !ascending) {
                    result = -result;
                }
                return result;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };

        Object[] array = datedThings.toArray();
        Arrays.sort(array, comp);
        List result = Arrays.asList(array);
        datedThings = new ArrayList();
        datedThings.addAll(result);
        return (List<DatedThing>) datedThings;
    }

    /**
     * equals method
     *
     * @param o object to check
     *
     * @return equals
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DatedObject)) {
            return false;
        }
        DatedObject that = (DatedObject) o;
        if ( !this.date.equals(that.date)) {
            return false;
        }
        if (this.object == null) {
            return that.object == null;
        }
        if (that.object == null) {
            return this.object == null;
        }
        return this.object.equals(that.object);
    }





    /**
     * Set the Date property.
     *
     * @param value The new value for Date
     */
    public void setDate(Date value) {
        date = value;
    }

    /**
     * Get the Date property.
     *
     * @return The Date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Set the Object property.
     *
     * @param value The new value for Object
     */
    public void setObject(Object value) {
        object = value;
    }

    /**
     * Get the Object property.
     *
     * @return The Object
     */
    public Object getObject() {
        return object;
    }


    /**
     * to string
     *
     * @return to string
     */
    public String toString() {
        if (object != null) {
            return "" + object;
        } else {
            return "";
        }
    }

}

