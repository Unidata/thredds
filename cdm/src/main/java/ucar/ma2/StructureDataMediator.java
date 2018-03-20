/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ma2;

/**
 * see StructureDataIteratorMediated
 *
 * @author caron
 * @since 7/9/2014
 */
public interface StructureDataMediator {
  StructureData modify(StructureData sdata);
}
