package thredds.crawlabledataset.s3;

import java.util.Date;

/**
 * Summary metadata for the virtual directory or S3 object at a specified Amazon S3 URI.
 *
 * @author jonescc
 * @since 2016/09/12
 */
public interface ThreddsS3Metadata {
    S3URI getS3uri();
}
