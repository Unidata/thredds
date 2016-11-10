package thredds.crawlabledataset.s3;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Listing of the "virtual directory" at a specified Amazon S3 URI.
 *
 * @author jonescc
 * @since 2016/09/12
 */
public class ThreddsS3Listing {
    private final S3URI s3uri;

    private List<ThreddsS3Metadata> entries = new ArrayList<>();

    public ThreddsS3Listing(S3URI s3uri) {
        this.s3uri = s3uri;
    }

    public S3URI getS3uri() {
        return s3uri;
    }

    public void add(ObjectListing objectListing) {
        if (objectListing == null) {
            return;
        }

        for (String commonPrefix : objectListing.getCommonPrefixes()) {
            entries.add(new ThreddsS3Directory(new S3URI(s3uri.getBucket(), commonPrefix)));
        }

        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            entries.add(new ThreddsS3Object(objectSummary));
        }
    }

    public void add(ThreddsS3Metadata metadata) {
        entries.add(metadata);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<ThreddsS3Metadata> getContents() {
        return new ArrayList<>(entries);
    }

    //////////////////////////////////////// Object ////////////////////////////////////////

    @Override
    public String toString() {
        return String.format("ThreddsS3Listing{'%s'}", s3uri);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ThreddsS3Listing that = (ThreddsS3Listing) other;
        return Objects.equals(this.getS3uri(), that.getS3uri()) &&
                Objects.equals(this.getContents(), that.getContents());
    }

    @Override
    public int hashCode() {
        return s3uri.hashCode();
    }
}
