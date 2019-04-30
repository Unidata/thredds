package ucar.nc2.grib.coord;

import java.util.Formatter;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.util.Misc;

@Immutable
public class EnsCoordValue implements Comparable<EnsCoordValue> {
  private final int code;
  private final int ensMember;

  public EnsCoordValue(int code, int ensMember) {
    this.code = code;
    this.ensMember = ensMember;
  }

  public int getCode() {
    return code;
  }

  public int getEnsMember() {
    return ensMember;
  }

  @Override
  public int compareTo(@Nonnull EnsCoordValue o) {
    int r = Misc.compare(code, o.code);
    if (r != 0) return r;
    return Misc.compare(ensMember, o.ensMember);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnsCoordValue that = (EnsCoordValue) o;
    return code == that.code &&
        ensMember == that.ensMember;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * ensMember;
    result += 31 * code;
    return result;
  }

  public String toString() {
    try (Formatter out = new Formatter()) {
      out.format("(%d %d)", code, ensMember);
      return out.toString();
    }
  }
}
