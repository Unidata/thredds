package ucar.nc2.grib.coord;

import java.util.Formatter;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.util.Misc;

@Immutable
public class VertCoordValue implements Comparable<VertCoordValue> {
  private final double value1;
  private final double value2;
  private final double mid;
  private final boolean isLayer;

  public VertCoordValue(double value1) {
    this.value1 = value1;
    this.value2 = GribNumbers.UNDEFINEDD;
    this.mid = value1;
    this.isLayer = false;
  }

  public VertCoordValue(double value1, double value2) {
    this.value1 = value1;
    this.value2 = value2;
    this.mid = (Double.compare(value2, 0.0) == 0 || GribNumbers.isUndefined(value2)) ? value1 : (value1 + value2) / 2;
    this.isLayer = true;
  }

  public double getValue1() {
    return value1;
  }

  public double getValue2() {
    return value2;
  }

  public double getMid() {
    return mid;
  }

  public boolean isLayer() {
    return isLayer;
  }

  @Override
  public int compareTo(@Nonnull VertCoordValue o) {
    return Double.compare(mid, o.mid);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VertCoordValue that = (VertCoordValue) o;
    return Double.compare(that.value1, value1) == 0 &&
        Double.compare(that.value2, value2) == 0 &&
        Double.compare(that.mid, mid) == 0 &&
        isLayer == that.isLayer;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value1, value2, mid, isLayer);
  }

  // cannot do approx equals and be consistent with hashCode, so make seperate call
  public boolean nearlyEquals(VertCoordValue other) {
    return Misc.nearlyEquals(value1, other.value1) && Misc.nearlyEquals(value2, other.value2);
  }

  public String toString() {
    try (Formatter out = new Formatter()) {
      if (isLayer) {
        out.format("(%f,%f)", value1, value2);
      } else {
        out.format("%f", value1);
      }
      return out.toString();
    }
  }

  public String toString(boolean isLayer) {
    try (Formatter out = new Formatter()) {
      if (isLayer) {
        out.format("(%f,%f)", value1, value2);
      } else {
        out.format("%f", value1);
      }
      return out.toString();
    }
  }
}
