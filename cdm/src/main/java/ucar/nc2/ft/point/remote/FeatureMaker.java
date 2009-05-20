package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.PointFeature;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Describe
 *
 * @author caron
 * @since May 19, 2009
 */
public interface FeatureMaker {
  PointFeature make(byte[] rawBytes) throws InvalidProtocolBufferException;
}
