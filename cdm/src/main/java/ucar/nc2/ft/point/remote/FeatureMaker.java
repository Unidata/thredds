package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.PointFeature;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Interface for deserializing a PointFeature.
 *
 * @author caron
 * @since May 19, 2009
 * @see RemotePointFeatureIterator
 */
public interface FeatureMaker {
  PointFeature make(byte[] rawBytes) throws InvalidProtocolBufferException;
}
