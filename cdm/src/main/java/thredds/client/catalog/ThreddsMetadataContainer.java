/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import java.util.List;

/**
 * Container of ThreddsMetadata: Dataset or ThreddsMetadata
 *
 * @author caron
 * @since 1/11/2015
 */
public interface ThreddsMetadataContainer {

  Object getLocalField(String fldName);
  List getLocalFieldAsList(String fldName);

}
