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
package ucar.unidata.geoloc;

import ucar.unidata.util.Format;

import java.awt.geom.Rectangle2D;

import java.io.*;

/**
 * Bounding box for ProjectionPoint's.
 * This is a subclass of java.awt.geom.Rectangle2D.Double.
 * Note that getX() getY() really means getMinX(), getMinY(), rather than
 * "upper left point" of the rectangle.
 *
 * @author John Caron
 */
public class ProjectionRect extends java.awt.geom.Rectangle2D.Double implements java.io.Serializable {

  /**
   * default constructor, initialized to center (0,0) and width (10000, 10000)
   */
  public ProjectionRect() {
    this(-5000, -5000, 5000, 5000);
  }

  /**
   * Copy Constructor
   *
   * @param r rectangle to copy
   */
  public ProjectionRect(Rectangle2D r) {
    this(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
  }

  /**
   * construct a MapArea from any two opposite corner points
   *
   * @param x1 x coord of any corner of the bounding box
   * @param y1 y coord of the same corner as x1
   * @param x2 x coord of opposite corner from x1,y1
   * @param y2 y coord of same corner as x2
   */
  public ProjectionRect(double x1, double y1, double x2, double y2) {
    double wx0 = 0.5 * (x1 + x2);
    double wy0 = 0.5 * (y1 + y2);
    double width = Math.abs(x1 - x2);
    double height = Math.abs(y1 - y2);
    setRect(wx0 - width / 2, wy0 - height / 2, width, height);
  }


  /**
   * Get the Lower Right Point
   *
   * @return the Lower Right Point
   */
  public ProjectionPoint getLowerRightPoint() {
    return new ProjectionPointImpl(getMaxPoint().getX(),
        getMinPoint().getY());
  }

  /**
   * Get the Upper Left Point (same as getMaxPoint)
   *
   * @return the Upper Left Point
   */
  public ProjectionPoint getUpperRightPoint() {
    return getMaxPoint();
  }

  /**
   * Get the Lower Right Point (same as getMinPoint)
   *
   * @return the Lower Right Point
   */
  public ProjectionPoint getLowerLeftPoint() {
    return getMinPoint();
  }

  /**
   * Get the Upper Left Point
   *
   * @return the Upper Left Point
   */
  public ProjectionPoint getUpperLeftPoint() {
    return new ProjectionPointImpl(getMinPoint().getX(),
        getMaxPoint().getY());
  }

  /**
   * Get the minimum corner of the bounding box.
   *
   * @return minimum corner of the bounding box
   */
  public ProjectionPoint getMinPoint() {
    return new ProjectionPointImpl(getX(), getY());
  }

  /**
   * Get the maximum corner of the bounding box.
   *
   * @return maximum corner of the bounding box
   */
  public ProjectionPoint getMaxPoint() {
    return new ProjectionPointImpl(getX() + getWidth(),
        getY() + getHeight());
  }

  /**
   * Get a String representation of this object.
   *
   * @return a String representation of this object.
   */
  public String toString() {
    return "min: " + Format.d(getX(), 3) + " " + Format.d(getY(), 3)
        + " size: " + Format.d(getWidth(), 3) + " "
        + Format.d(getHeight(), 3);
  }

  // bean serialization

  /**
   * set minimum X
   *
   * @param x minimum x
   */
  public void setX(double x) {
    setRect(x, getY(), getWidth(), getHeight());
  }

  /**
   * set minimum Y
   *
   * @param y minimum y
   */
  public void setY(double y) {
    setRect(getX(), y, getWidth(), getHeight());
  }

  /**
   * set X width
   *
   * @param w x width
   */
  public void setWidth(double w) {
    setRect(getX(), getY(), w, getHeight());
  }

  /**
   * set Y height
   *
   * @param h Y height
   */
  public void setHeight(double h) {
    setRect(getX(), getY(), getWidth(), h);
  }

  // serialization

  /**
   * Read the object from the input stream of the serialized object
   *
   * @param s stream to read
   * @throws ClassNotFoundException couldn't file the class
   * @throws IOException            Problem reading from stream
   */
  private void readObject(ObjectInputStream s)
      throws IOException, ClassNotFoundException {
    double x = s.readDouble();
    double y = s.readDouble();
    double w = s.readDouble();
    double h = s.readDouble();
    setRect(x, y, w, h);
  }

  /**
   * Wrtie the object to the output stream
   *
   * @param s stream to write
   * @throws IOException Problem writing to stream
   */
  private void writeObject(ObjectOutputStream s) throws IOException {
    s.writeDouble(getX());
    s.writeDouble(getY());
    s.writeDouble(getWidth());
    s.writeDouble(getHeight());
  }
}