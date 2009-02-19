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

package ucar.nc2.ft;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;
import java.util.Formatter;

/**
 * Interface for factories that wrap a NetcdfDataset with a FeatureDataset.
 * Class must have a no-arg Constructor.
 * Implementations must be thread-safe
 *
 * @author caron
 * @since Mar 19, 2008
 */
public interface FeatureDatasetFactory {

  /**
   * Determine if the factory can open this dataset as an instance of the given feature type
   * @param wantFeatureType can factory open as this feature type? If null, can factory open as any feature type?
   * @param ncd examine this NetcdfDataset.
   * @param errlog place errors here
   * @return "analysis object" - null if cannot open, else an Object that is passed back into FeatureDatasetFactory.open().
   *   This allows expensive analysis results to be reused
   * @throws java.io.IOException on read error
   */
  public Object isMine( FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException;

  /**
   * Open a NetcdfDataset as a FeatureDataset.
   * Should only be called if isMine() returns non-null.
   *
   * @param ftype open as this feature type. If null, open as any feature type.
   * @param ncd an already opened NetcdfDataset.
   * @param analysis the object returned from isMine(). Likely given to a different instance of FeatureDatasetFactory
   * @param task user may cancel, may be null
   * @param errlog write error messages here, may be null
   * @return a subclass of FeatureDataset
   * @throws java.io.IOException on error
   */
  public FeatureDataset open( FeatureType ftype, NetcdfDataset ncd, Object analysis, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException;

  /**
   * This Factory can open these types of Feature datasets.
   * @return array of FeatureType
   */
  public FeatureType[] getFeatureType();
}
