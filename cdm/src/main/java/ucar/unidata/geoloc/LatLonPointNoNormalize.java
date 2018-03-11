package ucar.unidata.geoloc;

import ucar.nc2.util.Misc;

import java.util.Objects;

/**
 * Similar to {@link LatLonPoint}, but this class does not normalize its latitude and longitude, even for comparison.
 * That is, latitudes may lie outside of [-90, 90] and longitudes may lie outside of [-180, 180].
 *
 * @author cwardgar
 * @since 2018-03-08
 */
public class LatLonPointNoNormalize {
    /** East latitude in degrees, not necessarily in [-90, 90]. */
    private final double lat;

    /** North longitude in degrees, not necessarily in [-180, 180]. */
    private final double lon;

    public LatLonPointNoNormalize(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Returns the latitude, not necessarily in [-90, 90].
     *
     * @return latitude (degrees)
     */
    public double getLatitude() {
        return lat;
    }

    /**
     * Returns the longitude, not necessarily in [-180, 180].
     *
     * @return longitude (degrees)
     */
    public double getLongitude() {
        return lon;
    }

    /**
     * Returns the result of {@link #nearlyEquals(LatLonPointNoNormalize, double)}, with
     * {@link Misc#defaultMaxRelativeDiffDouble}.
     */
    public boolean nearlyEquals(LatLonPointNoNormalize that) {
        return nearlyEquals(that, Misc.defaultMaxRelativeDiffDouble);
    }

    /**
     * Returns {@code true} if this point is nearly equal to {@code that}. The "near equality" of points is determined
     * using {@link Misc#nearlyEquals(double, double, double)}, with the specified maxRelDiff.
     *
     * @param that    the other point to check.
     * @param maxRelDiff  the maximum {@link Misc#relativeDifference relative difference} the two points may have.
     * @return {@code true} if this point is nearly equal to {@code that}.
     */
    public boolean nearlyEquals(LatLonPointNoNormalize that, double maxRelDiff) {
        return Misc.nearlyEquals(this.getLatitude(), that.getLatitude(), maxRelDiff) &&
               Misc.nearlyEquals(this.getLongitude(), that.getLongitude(), maxRelDiff);
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LatLonPointNoNormalize that = (LatLonPointNoNormalize) o;

        return Double.compare(this.getLatitude(),  that.getLatitude())  == 0 &&
               Double.compare(this.getLongitude(), that.getLongitude()) == 0;
    }

    @Override public int hashCode() {
        return Objects.hash(lat, lon);
    }

    @Override public String toString() {
        return String.format("(%.4f, %.4f)", lat, lon);
    }
}
