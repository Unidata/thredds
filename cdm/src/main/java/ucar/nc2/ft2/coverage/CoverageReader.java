/* Copyright */
package ucar.nc2.ft2.coverage;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 7/13/2015
 */
public interface CoverageReader extends AutoCloseable {

  // List<ArrayWithCoordinates> readData(List<Coverage> coverage, SubsetParams subset) throws IOException;

  GeoReferencedArray readData(Coverage coverage, SubsetParams subset) throws IOException;

}
