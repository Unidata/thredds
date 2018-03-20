/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.bufr;

import ucar.nc2.iosp.bufr.tables.BufrTables;
import ucar.nc2.iosp.bufr.tables.TableB;
import ucar.nc2.iosp.bufr.tables.TableD;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;

/**
 * Encapsolates lookup into the BUFR Tables.
 *
 * @author caron
 * @since Jul 14, 2008
 */
@Immutable
public class TableLookup {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableLookup.class);
  static private final boolean showErrors = false;

  /////////////////////////////////////////
  private final TableB localTableB;
  private final TableD localTableD;

  private final TableB wmoTableB;
  private final TableD wmoTableD;
  private final BufrTables.Mode mode;

  public TableLookup(int center, int subcenter, int masterTableVersion, int local, int cat) throws IOException {
    this.wmoTableB = BufrTables.getWmoTableB(masterTableVersion);
    this.wmoTableD = BufrTables.getWmoTableD(masterTableVersion);

    BufrTables.Tables tables = BufrTables.getLocalTables(center, subcenter, masterTableVersion, local, cat);
    if (tables != null) {
      this.localTableB = tables.b;
      this.localTableD = tables.d;
      this.mode = (tables.mode == null) ? BufrTables.Mode.localOverride : tables.mode;
    } else {
      this.localTableB = null;
      this.localTableD = null;
      this.mode = BufrTables.Mode.localOverride;
    }
  }

  public TableLookup(BufrIdentificationSection ids, TableB b, TableD d) throws IOException {
    this.wmoTableB = BufrTables.getWmoTableB(ids.getMasterTableVersion());
    this.wmoTableD = BufrTables.getWmoTableD(ids.getMasterTableVersion());
    this.localTableB = b;
    this.localTableD = d;
    this.mode = BufrTables.Mode.localOverride;
  }

  public String getWmoTableBName() {
    return wmoTableB.getName();
  }

  public String getLocalTableBName() {
    return localTableB == null ? "none" : localTableB.getName();
  }

  public String getLocalTableDName() {
    return localTableD == null ? "none" : localTableD.getName();
  }

  public String getWmoTableDName() {
    return wmoTableD.getName();
  }

  public BufrTables.Mode getMode() {
    return mode;
  }

  public TableB getLocalTableB() {
    return localTableB;
  }

  public TableD getLocalTableD() {
    return localTableD;
  }

  public TableB.Descriptor getDescriptorTableB(short fxy) {
    TableB.Descriptor b = null;
    boolean isWmoRange = Descriptor.isWmoRange(fxy);

    if (isWmoRange && (mode == BufrTables.Mode.wmoOnly)) {
      b = wmoTableB.getDescriptor(fxy);

    } else if (isWmoRange && (mode == BufrTables.Mode.wmoLocal)) {
      b = wmoTableB.getDescriptor(fxy);
      if ((b == null) && (localTableB != null))
        b = localTableB.getDescriptor(fxy);

    } else if (isWmoRange && (mode == BufrTables.Mode.localOverride)) {
      if (localTableB != null)
        b = localTableB.getDescriptor(fxy);
      if (b == null)
        b = wmoTableB.getDescriptor(fxy);
      else
        b.setLocalOverride(true);

    } else if (!isWmoRange) {
      if (localTableB != null)
        b = localTableB.getDescriptor(fxy);
    }

    if (b == null) {  // look forward in standard WMO table; often the version number of the message is wrong
      b = BufrTables.getWmoTableBlatest().getDescriptor(fxy);
    }

    if (b == null && showErrors)
      log.warn(" TableLookup cant find Table B descriptor = {} in tables {}, {} mode={}", Descriptor.makeString(fxy),
              getLocalTableBName(), getWmoTableBName(), mode);
    return b;
  }

  public TableD.Descriptor getDescriptorTableD(short fxy) {
    TableD.Descriptor d = null;
    boolean isWmoRange = Descriptor.isWmoRange(fxy);

    if (isWmoRange && (mode == BufrTables.Mode.wmoOnly)) {
      d = wmoTableD.getDescriptor(fxy);

    } else if (isWmoRange && (mode == BufrTables.Mode.wmoLocal)) {
      d = wmoTableD.getDescriptor(fxy);
      if ((d == null) && (localTableD != null))
        d = localTableD.getDescriptor(fxy);

    } else if (isWmoRange && (mode == BufrTables.Mode.localOverride)) {
      if (localTableD != null)
        d = localTableD.getDescriptor(fxy);
      if (d == null)
        d = wmoTableD.getDescriptor(fxy);
      else
        d.setLocalOverride(true);

    } else {
      if (localTableD != null)
        d = localTableD.getDescriptor(fxy);
    }

    if (d == null) {  // look forward in standard WMO table; often the version number of the message is wrong
      d = BufrTables.getWmoTableDlatest().getDescriptor(fxy);
    }

    if (d == null && showErrors)
      System.out.printf(" TableLookup cant find Table D descriptor %s in tables %s,%s mode=%s%n", Descriptor.makeString(fxy),
              getLocalTableDName(), getWmoTableDName(), mode);
    return d;
  }

}
