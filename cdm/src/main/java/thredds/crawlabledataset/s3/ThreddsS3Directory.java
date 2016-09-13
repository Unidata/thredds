package thredds.crawlabledataset.s3;

import java.util.Date;
import java.util.Objects;

/**
 * Summary metadata for the "virtual directory" at the specified Amazon S3 URI
 *
 * @author jonescc
 * @since 2016/09/12
 */
public class ThreddsS3Directory extends AbstractThreddsS3Metadata {
    public ThreddsS3Directory(S3URI s3uri) {
        super(s3uri);
    }

    //////////////////////////////////////// Object ////////////////////////////////////////

    @Override
    public String toString() {
        return String.format("ThreddsS3Directory{'%s'}", getS3uri());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ThreddsS3Directory that = (ThreddsS3Directory) other;
        return Objects.equals(this.getS3uri(), that.getS3uri());
    }

    @Override
    public int hashCode() {
        return getS3uri().hashCode();
    }
}
