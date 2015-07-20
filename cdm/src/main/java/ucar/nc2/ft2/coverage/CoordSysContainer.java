/* Copyright */
package ucar.nc2.ft2.coverage;

/**
 * Describe
 *
 * @author caron
 * @since 7/20/2015
 */
public interface CoordSysContainer {

  CoverageTransform findCoordTransform(String transformName);

  CoverageCoordAxis findCoordAxis(String axisName);


}
