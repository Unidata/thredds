package ucar.nc2.grib.grib2.table;

import com.google.common.base.MoreObjects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Grib2TablesId {
  public enum Type {wmo, cfsr, gempak, gsd, kma, ncep, ndfd, mrms, nwsDev, eccodes}

  public final int center, subCenter, masterVersion, localVersion, genProcessId;

  public Grib2TablesId(int center, int subCenter, int masterVersion, int localVersion, int genProcessId) {
    this.center = center;
    this.subCenter = subCenter;
    this.masterVersion = masterVersion;
    this.localVersion = localVersion;
    this.genProcessId = genProcessId;
  }

  boolean match(Grib2TablesId id) {
    if (id.center != center) return false; // must match center
    if (subCenter != -1 && id.subCenter != subCenter) return false;
    if (masterVersion != -1 && id.masterVersion != masterVersion) return false;
    if (localVersion != -1 && id.localVersion != localVersion) return false;
    if (genProcessId != -1 && id.genProcessId != genProcessId) return false;
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Grib2TablesId that = (Grib2TablesId) o;

    if (center != that.center) return false;
    if (genProcessId != that.genProcessId) return false;
    if (localVersion != that.localVersion) return false;
    if (masterVersion != that.masterVersion) return false;
    if (subCenter != that.subCenter) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = center;
    result = 31 * result + subCenter;
    result = 31 * result + masterVersion;
    result = 31 * result + localVersion;
    result = 31 * result + genProcessId;
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("center", center)
        .add("subCenter", subCenter)
        .add("masterVersion", masterVersion)
        .add("localVersion", localVersion)
        .add("genProcessId", genProcessId)
        .toString();
  }
}
