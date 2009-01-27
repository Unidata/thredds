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
package ucar.nc2.dataset;

/**
 * A Variable decorator that handles Coordinates Systems and "standard attributes" and adds them to the object model.
 * Specifically, this:
 * <ul>
 * <li> adds a list of <b>CoordinateSystem</b>.
 * <li> adds <b>unitString</b> from the standard attribute <i>units</i>
 * <li> adds <b>description</b> from the standard attributes <i> long_name, description or title</i>
 * </ul>
 * if those "standard attributes" are present.
 *
 * @author caron
 */

public interface Enhancements {

 /** Get the description of the Variable, or null if none.
  * @return description of the Variable, or null
  */
  public String getDescription();

  /** Get the Unit String for the Variable, or null if none.
   * @return Unit String for the Variable, or null
   */
  public String getUnitsString();

  /**
   * Get the list of Coordinate Systems for this Variable.
   * @return list of type CoordinateSystem; may be empty but not null.
   */
  public java.util.List<CoordinateSystem> getCoordinateSystems();

  /** Add a CoordinateSystem to the dataset.
   * @param cs add this Coordinate System
   */
  public void addCoordinateSystem( CoordinateSystem cs);

  /** Remove a CoordinateSystem from the dataset.
   * @param cs remove this coordinate system
   */
  public void removeCoordinateSystem( CoordinateSystem cs);

}
