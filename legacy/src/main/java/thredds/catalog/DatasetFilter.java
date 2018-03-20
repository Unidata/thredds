/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.catalog;

import ucar.nc2.constants.FeatureType;

/**
 * Abstract class to filter datasets.
 * @see InvCatalog#filter
 *
 * @author john caron
 */

public interface DatasetFilter {

  /**
   * Decision function as to whether to accept this dataset or not.
   * @param d dataset to filter
   * @return 1 if pass, -1 if fail, 0 is dont know.
   */
  abstract public int accept( InvDataset d);

  /**
   * Filter a Catalog by the access service type.
   */
  static public class ByServiceType implements DatasetFilter {
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
  static public class ByDataType implements DatasetFilter {
    private FeatureType type;
    public ByDataType( FeatureType type) { this.type = type; }

    public int accept( InvDataset d) {
      if (null == d.getDataType()) return 0;
      return (d.getDataType() == type) ? 1 : -1;
    }
  }

}