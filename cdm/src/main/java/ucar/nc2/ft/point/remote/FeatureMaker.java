/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.PointFeature;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Interface for deserializing a PointFeature.
 *
 * @author caron
 * @since May 19, 2009
 * @see PointIteratorStream
 */
public interface FeatureMaker {
  PointFeature make(DsgFeatureCollection dsg, byte[] rawBytes) throws InvalidProtocolBufferException;
}
