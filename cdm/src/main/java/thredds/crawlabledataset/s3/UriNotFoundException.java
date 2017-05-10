package thredds.crawlabledataset.s3;

/**
 * Created by craigj on 11/05/17.
 */
public class UriNotFoundException extends Exception {
    public UriNotFoundException(S3URI s3uri) {
        super(s3uri.toString());
    }
}
