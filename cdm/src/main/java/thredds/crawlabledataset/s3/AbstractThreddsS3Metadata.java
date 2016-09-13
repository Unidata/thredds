package thredds.crawlabledataset.s3;

/**
 * Base class for implementing ThreddsS3Metadata
 *
 * @author jonescc
 * @since 2016/09/12
 */
public abstract class AbstractThreddsS3Metadata implements ThreddsS3Metadata {
    private final S3URI s3uri;

    public AbstractThreddsS3Metadata(S3URI s3uri) {
        this.s3uri = s3uri;
    }

    @Override
    public S3URI getS3uri() {
        return s3uri;
    }
}

