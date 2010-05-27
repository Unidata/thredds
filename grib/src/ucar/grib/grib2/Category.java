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

// $Id: Category.java,v 1.15 2005/12/13 22:58:47 rkambic Exp $

/**
 * Category.java 10/10/03
 * @version 1.0
 * @author Robb Kambic
 *
 */

package ucar.grib.grib2;


import ucar.grib.Parameter;
import ucar.grid.GridParameter;

import java.util.HashMap;


/**
 * Class which represents a Category from a parameter table.
 * A parameter consists of a discipline( ie Meteorological_products),
 * a Category( ie Temperature ) and a number that refers to a name( ie Temperature)
 * <p/>
 * see <a href="../../Parameters.txt">Parameters.txt</a>
 */

public final class Category {
  static private org.slf4j.Logger logger =
        org.slf4j.LoggerFactory.getLogger(Category.class);
  /**
   * each category has a unique number.
   */
  private int number;

  /**
   * each category has a name.
   */
  private String name;

  /**
   * Each catagory has a set of parameter associated with it.
   * parameter - a HashMap of Parameters.
   */
  private final HashMap<String,Parameter> parameter;

  /**
   * Constructor for a Category.
   */
  public Category() {
    number = -1;
    name = "undefined";
    //description = "undefined";
    parameter = new HashMap<String,Parameter>();
  }

  /**
   * returns the number of this Category.
   *
   * @return int
   */
  public final int getNumber() {
    return number;
  }

  /**
   * returns name of this Category.
   *
   * @return name
   */
  public final String getName() {
    return name;
  }

  /**
   * given a Parameter number returns GridParameter object for this Category.
   *
   * @param paramNumber
   * @return Parameter
   */
  public final GridParameter getParameter(int paramNumber) {
    if (parameter.containsKey(Integer.toString(paramNumber)))
      return (Parameter) parameter.get(Integer.toString(paramNumber));

    logger.warn("Category: "+ name +" UnknownParameter "+ Integer.toString(paramNumber));
    return null;
  }


  /**
   * number value of this Category.
   *
   * @param number of Category
   */
  public final void setNumber(int number) {
    this.number = number;
  }

  /**
   * sets the name of this Category.
   *
   * @param name of Category
   */
  public final void setName(String name) {
    this.name = name;
  }

  /**
   * add this GridParameter to this Category.
   *
   * @param param object
   */
  public final void setParameter(Parameter param) {
    parameter.put(Integer.toString(param.getNumber()), param);
  }

}  // end Category


