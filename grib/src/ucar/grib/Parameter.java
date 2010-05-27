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

// $Id: Parameter.java,v 1.9 2005/12/13 22:59:05 rkambic Exp $

/**
 * Parameter.java
 * @author Robert Kambic 10/10/03
 */
package ucar.grib;

import ucar.grid.GridParameter;


/**
 * Class which represents a parameter from a PDS parameter table.
 * A parameter consists of a discipline( ie Meteorological_products),
 * a Category( ie Temperature ) and a number that refers to a name( ie Temperature),
 * Description( ie Temperature at 2 meters), and Units( ie K ).
 * see <a href="../../Parameters.txt">Parameters.txt</a>
 */

public final class Parameter extends GridParameter {

  /**
   * parameter number.
   */
  //private int number;

  /**
   * name of parameter.
   */
  //private String name;

  /**
   * description of parameter.
   */
  //private String description;

  /**
   * unit of Parameter.
   */
  //private String unit;

  /**
   * constructor.
   */
  public Parameter() {
    super();
  }

  /**
   * constructor.
   *
   * @param number
   * @param name
   * @param description
   * @param unit        of parameter
   */
  public Parameter(int number, String name, String description, String unit) {
    super( number, name, description, unit );
  }

  /**
   * number of parameter.
   * @return number
   */
//    public final int getNumber() {
//        return number;
//    }

  /**
   * name of parameter.
   * @return name
   */
//    public final String getName() {
//        return name;
//    }

  /**
   * description of parameter.
   *
   * @return description
   */
//    public final String getDescription() {
//        return description;
//    }

  /**
   * unit of parameter.
   * @return unit
   */
//    public final String getUnit() {
//        return unit;
//    }

  /**
   * sets number of parameter.
   * @param number of parameter
   */
//    public final void setNumber(int number) {
//        this.number = number;
//    }

  /**
   * sets name of parameter.
   * @param name  of parameter
   */
//    public final void setName(String name) {
//        this.name = name;
//    }

  /**
   * sets description of parameter.
   * @param description of parameter
   */
//    public final void setDescription(String description) {
//        this.description = description;
//    }

  /**
   * sets unit of parameter.
   * @param unit of parameter
   */
//    public final void setUnit(String unit) {
//        this.unit = unit;
//    }
}

