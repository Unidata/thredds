package thredds.crawlabledataset;

/**
 * @author cwardgar
 * @since 2015/08/24
 */
public class S3URI {
    public static final String S3_PREFIX = "s3://";
    public static final String S3_DELIMITER = "/";

    private final String bucket, key;

    /**
     * Creates an S3URI by extracting the S3 bucket and key names from {@code uri}.
     *
     * @param uri  an S3 URI in the form {@code s3://<bucket>/<key>}.
     * @throws IllegalArgumentException  if {@code uri} is not valid.
     */
    public S3URI(String uri) {
        if (uri.startsWith(S3_PREFIX)) {
            uri = stripPrefix(uri, S3_PREFIX);
            int delimPos = uri.indexOf(S3_DELIMITER);

            if (delimPos == -1) {  // Handle case where uri includes bucket but no key, e.g. "s3://bucket".
                this.bucket = uri;
                this.key = null;
            } else if (delimPos < 3) {
                throw new IllegalArgumentException(String.format(
                        "Bucket names must be at least 3 characters: '%s'", uri));
            } else {
                this.bucket = uri.substring(0, delimPos);
                if (delimPos == uri.length() - 1) {
                    this.key = "";  // uri ends with slash and has empty string as its value, e.g. "s3://bucket/".
                } else {
                    assert uri.length() - delimPos > 1;
                    this.key = uri.substring(delimPos + 1, uri.length());
                }
            }
        } else {
            throw new IllegalArgumentException(String.format(
                    "S3 URI '%s' does not start with the expected prefix '%s'.", uri, S3_PREFIX));
        }
    }

    /**
     * Returns the bucket.
     *
     * @return  the bucket.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Returns the key. May be {@code null} if the URI did not include a key, e.g. "s3://bucket".
     *
     * @return  the key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the key, adding a trailing {@link #S3_DELIMITER delimiter} if it's not already present.
     * Returns {@code null} if the key is {@code null}.
     *
     * @return  the key, with a trailing delimiter.
     */
    public String getKeyWithTrailingDelimiter() {
        if (key == null || key.endsWith(S3_DELIMITER)) {
            return key;
        } else {
            return key + S3_DELIMITER;
        }
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(S3_PREFIX);
        strBuilder.append(bucket);

        if (key != null) {
            strBuilder.append(S3_DELIMITER);
            strBuilder.append(key);
        }

        return strBuilder.toString();
    }

    public static String stripPrefix(String key, String prefix) {
        return key.replaceFirst(prefix, "");
    }
}
