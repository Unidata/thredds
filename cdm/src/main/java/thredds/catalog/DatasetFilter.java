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

package thredds.catalog;

import ucar.nc2.constants.FeatureType;

/**
 * Abstract class to filter datasets.
 * @see InvCatalog#filter
 *
 * @author john caron
 */

public abstract class DatasetFilter {

  /**
   * Decision function as to whether to accept this dataset or not.
   * @param d dataset to filter
   * @return 1 if pass, -1 if fail, 0 is dont know.
   */
  abstract public int accept( InvDataset d);

  /**
   * Filter a Catalog by the access service type.
   */
  static public class ByServiceType extends DatasetFilter {
    private ServiceType type;
    public ByServiceType( ServiceType type) { this.type = type; }

    public int accept( InvDataset d) {
      // check all access for any that has this servicee
      for (InvAccess a : d.getAccess()) {
         if (a.getService().getServiceType() == type) return 1;
       }

       // cant tell about DQC and resolvers !!
       for (InvAccess a : d.getAccess()) {
         if (a.getService().getServiceType() == ServiceType.QC) return 0;
         if (a.getService().getServiceType() == ServiceType.RESOLVER) return 0;
       }

      return -1;
    }
  }

  /**
   * Filter a Catalog by the dataset data type.
   */
  static public class ByDataType extends DatasetFilter {
    private FeatureType type;
    public ByDataType( FeatureType type) { this.type = type; }

    public int accept( InvDataset d) {
      if (null == d.getDataType()) return 0;
      return (d.getDataType() == type) ? 1 : -1;
    }
  }

}