/* Copyright */
package ucar.nc2.ft2.coverage;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 7/13/2015
 */
public interface CoordAxisReader {

  double[] readValues(CoverageCoordAxis coordAxis) throws IOException;

}
