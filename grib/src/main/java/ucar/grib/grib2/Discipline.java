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

// $Id: Discipline.java,v 1.13 2005/12/13 22:58:47 rkambic Exp $

/**
 * Discipline.java
 * @author Robert Kambic 10/10/03.
 */
package ucar.grib.grib2;


import java.util.*;


/**
 * Class which represents a discipline from a parameter table. Each discipline has a
 * set of categories associated with it.
 * A parameter consists of a discipline( ie Meteorological_products),
 * a Category( ie Temperature ) and a number that refers to a name( ie Temperature)
 *
 * see <a href="../../Parameters.txt">Parameters.txt</a>
 */

public final class Discipline {

    /**
     * each discipline has a unique number.
     */
    private int number;

    /**
     * each discipline has a name.
     */
    private String name;

    /**
     * category - stores array of Category objests.
     */
    private final HashMap<String,Category> category;

    /**
     * Constructor for Discipline.
     */
    public Discipline() {
        number   = -1;
        name     = "undefined";
        category = new HashMap<String,Category>();
    }

    /**
     * returns number of this Discipline.
     * @return number
     */
    public final int getNumber() {
        return number;
    }

    /**
     * returns name of this Discipline.
     * @return name
     */
    public final String getName() {
        return name;
    }

    /**
     * returns a Category for this Discipline given a Category number.
     * @param cat category one wants
     * @return Category
     */
    public final Category getCategory(int cat) {
        if (category.containsKey(Integer.toString(cat))) {
            return category.get(Integer.toString(cat));
        } else {
            return null;
        }
    }

    /**
     * sets number of this Discipline.
     * @param number of this Discipline
     */
    public final void setNumber(int number) {
        this.number = number;
    }

    /**
     * sets name of this Discipline.
     * @param name of this Discipline
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * adds a Category to this Discipline.
     * @param cat of this Discipline
     */
    public final void setCategory(Category cat) {
        category.put(Integer.toString(cat.getNumber()), cat);
    }
}

