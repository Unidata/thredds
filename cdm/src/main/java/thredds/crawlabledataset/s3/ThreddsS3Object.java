package thredds.crawlabledataset.s3;

import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.Date;
import java.util.Objects;

/**
 * Summary metadata for the S3 object at a specified Amazon S3 URI.
 *
 * @author jonescc
 * @since 2016/09/12
 */
public class ThreddsS3Object extends AbstractThreddsS3Metadata {
    private final Date lastModified;
    private final long length;

    public ThreddsS3Object(S3URI s3uri, Date lastModified, long length) {
        super(s3uri);
        this.lastModified = lastModified;
        this.length = length;
    }

    public ThreddsS3Object(S3ObjectSummary objectSummary) {
        super(new S3URI(objectSummary.getBucketName(), objectSummary.getKey()));
        lastModified = objectSummary.getLastModified();
        length = objectSummary.getSize();
    }

    public Date getLastModified() {
        return lastModified;
    }

    public long getLength() {
        return length;
    }

    //////////////////////////////////////// Object ////////////////////////////////////////

    @Override
    public String toString() {
        return String.format("ThreddsS3Object{'%s'}", getS3uri());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ThreddsS3Object that = (ThreddsS3Object) other;
        return Objects.equals(getS3uri(), that.getS3uri()) &&
                this.getLength() == that.getLength() &&
                Objects.equals(this.getLastModified(), that.getLastModified());
    }

    @Override
    public int hashCode() {
        return getS3uri().hashCode();
    }
}
