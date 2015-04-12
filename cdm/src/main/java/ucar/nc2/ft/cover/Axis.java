/* Copyright */
package ucar.nc2.ft.cover;

/**
 * Describe
 *
 * @author caron
 * @since 4/7/2015
 */
public interface Axis<T> {

  T getStart();
  T getEnd();
  T getResolution();
  int getNpoints();

}
