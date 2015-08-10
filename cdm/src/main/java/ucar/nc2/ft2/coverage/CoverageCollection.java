package ucar.nc2.ft2.coverage;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Description
 *
 * @author John
 * @since 8/8/2015
 */
public class CoverageCollection implements AutoCloseable {
  private AutoCloseable closer;
  private List<CoverageDataset> datasets;

  public CoverageCollection(AutoCloseable closer, CoverageDataset coverageDataset) {
    this.closer = closer;
    this.datasets = Lists.newArrayList(coverageDataset);
  }

  public CoverageCollection(AutoCloseable closer, List<CoverageDataset> datasets) {
    this.closer = closer;
    this.datasets = datasets;
  }

  @Override
  public void close() throws IOException {
    try {
      closer.close();
    } catch (IOException ioe) {
      throw ioe;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public List<CoverageDataset> getCoverageDatasets() {
    return datasets;
  }

  public void showInfo(Formatter result) {
  }
}
