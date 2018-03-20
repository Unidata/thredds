/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.bufr;

import java.util.List;

/**
 * Abstraction for BUFR field.
 * Used in writing index, so we can make changes in BufrCdmIndexPanel
 *
 * @author caron
 * @since 8/20/13
 */
public interface BufrField {
  public String getName();
  public String getDesc();
  public String getUnits();

  public short getFxy();
  public String getFxyName();

  public BufrCdmIndexProto.FldAction getAction();
  public BufrCdmIndexProto.FldType getType() ;

  public boolean isSeq();
  public int getMin();
  public int getMax();

  public int getScale();
  public int getReference();
  public int getBitWidth();

  public List<? extends BufrField> getChildren();

}
