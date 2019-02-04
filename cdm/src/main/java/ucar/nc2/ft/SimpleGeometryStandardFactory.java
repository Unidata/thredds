package ucar.nc2.ft;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.simpgeometry.SimpleGeometryFeatureDataset;
import ucar.nc2.ft2.simpgeometry.adapter.SimpleGeometryCSBuilder;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.Formatter;

/**
 * Standard factory for Simple Geometry datatypes. Forked from GridDatasetStandardFactory.java
 * 
 * @author wchen@usgs.gov
 * @since 8/22/2018
 */
public class SimpleGeometryStandardFactory implements FeatureDatasetFactory {

  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException {
    SimpleGeometryCSBuilder sgCoverage = SimpleGeometryCSBuilder.classify(ncd, errlog);
    if (sgCoverage == null || sgCoverage.getFeatureType() == null) return null;
    if (!match(wantFeatureType, sgCoverage.getFeatureType())) return null;
    return sgCoverage;
  }

  private boolean match(FeatureType wantFeatureType, FeatureType covType) {
    if (wantFeatureType == null || wantFeatureType == FeatureType.ANY) return true;
    return true;
  }

  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, Object analysis, CancelTask task, Formatter errlog) throws IOException {

	  return new SimpleGeometryFeatureDataset(ncd);
  }

  public FeatureType[] getFeatureTypes() {
    return new FeatureType[] {FeatureType.SIMPLE_GEOMETRY};
  }
}
