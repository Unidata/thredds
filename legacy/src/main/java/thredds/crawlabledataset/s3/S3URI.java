package thredds.crawlabledataset.s3;

import java.io.File;

import com.google.common.base.Preconditions;

/**
 * An identifier for objects stored in Amazon S3. Ultimately, the identifier is composed solely of a bucket name and
 * a key, and an object of this class can be constructed with those two items. However, this class also supports URIs
 * rendered in a path-like form: {@code s3://<bucket>/<key>}.
 * <p>
 * Instances of this class are immutable.
 *
 * @author cwardgar
 * @since 2015/08/24
 */
public class S3URI {
    public static final String S3_PREFIX = "s3://";
    public static final String S3_DELIMITER = "/";
    public static final File S3ObjectTempDir = new File(System.getProperty("java.io.tmpdir"), "S3Objects");

    private final String bucket, key;

    /**
     * Creates a S3URI by extracting the S3 bucket and key names from {@code uri}. If the key has a trailing
     * {@link #S3_DELIMITER delimiter}, it will be removed.
     *
     * @param uri  an S3 URI in the form {@code s3://<bucket>/<key>}.
     * @throws IllegalArgumentException  if {@code uri} is not in the expected form or if the bucket or key are invalid.
     * @see #S3URI(String, String)
     */
    public S3URI(String uri) {
        if (uri.startsWith(S3_PREFIX)) {
            uri = uri.substring(S3_PREFIX.length(), uri.length());

            int delimPos = uri.indexOf(S3_DELIMITER);
            if (delimPos == -1) {  // Handle case where uri includes bucket but no key, e.g. "s3://bucket".
                this.bucket = checkBucket(uri);
                this.key = checkKey(null);
            } else {
                this.bucket = checkBucket(uri.substring(0, delimPos));
                this.key = checkKey(uri.substring(delimPos + 1, uri.length()));
            }
        } else {
            throw new IllegalArgumentException(String.format(
                    "S3 URI '%s' does not start with the expected prefix '%s'.", uri, S3_PREFIX));
        }
    }

    /**
     * Creates a S3URI from the specified bucket and key. If the key has a trailing
     * {@link #S3_DELIMITER delimiter}, it will be removed.
     *
     * @param bucket  a bucket name. Must be non-{@code null} and at least 3 characters.
     * @param key  a key. May be {@code null} but cannot be the empty string. Also, it may not contain consecutive
     *             delimiters.
     * @throws IllegalArgumentException  if either argument fails the requirements.
     */
    public S3URI(String bucket, String key) throws IllegalArgumentException {
        this.bucket = checkBucket(bucket);
        this.key = checkKey(key);
    }

    private static String checkBucket(String bucket) throws IllegalArgumentException {
        Preconditions.checkNotNull(bucket, "Bucket must be non-null.");
        if (bucket.length() < 3) {
            throw new IllegalArgumentException(String.format(
                    "Bucket name '%s' must be at least 3 characters.", bucket));
        }
        return bucket;
    }

    private static String checkKey(String key) throws IllegalArgumentException {
        if (key == null) {
            return null;
        } else if (key.equals("")) {
            throw new IllegalArgumentException("Key may not be the empty string.");
        } else {
            if (key.contains(S3_DELIMITER + S3_DELIMITER)) {  // Key contains consecutive delimiters.
                throw new IllegalArgumentException(String.format("Key '%s' contains consecutive delimiters.", key));
            }

            if (key.endsWith(S3_DELIMITER)) {
                return key.substring(0, key.length() - 1);  // Remove trailing delimiter.
            } else {
                return key;
            }
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
     * Returns the key. May be {@code null} if the URI did not include a key, e.g. "s3://bucket". If not null,
     * any trailing {@code #S3_DELIMITER delimiter} the key had when passed to the constructor will have been stripped.
     *
     * @return  the key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the key, adding a trailing {@link #S3_DELIMITER delimiter}.
     * Returns {@code null} if the key is {@code null}.
     *
     * @return  the key, with a trailing delimiter.
     */
    public String getKeyWithTrailingDelimiter() {
        if (key == null) {
            return null;
        } else {
            assert !key.endsWith(S3_DELIMITER) : "Didn't we strip this in the ctor?";
            return key + S3_DELIMITER;
        }
    }

    /**
     * Returns the base name of the file or directory denoted by this URI. This is just the last name in the
     * key's name sequence. If the key is {@code null}, {@code null} is returned.
     *
     * @return  the base name.
     */
    public String getBaseName() {
        if (key == null) {
            return null;
        } else {
            return new File(key).getName();
        }
    }

    /**
     * Returns the parent URI of this URI. The determination is completely text-based, using the
     * {@link #S3_DELIMITER delimiter}. If the key is {@code null}, {@code null} is returned. If it is non-{@code null},
     * but doesn't have a logical parent, the returned URI will have a {@code null} key (but the same bucket).
     * For example, the parent of {@code s3://my-bucket/my-key} (bucket="my-bucket", key="my-key") will be
     * {@code s3://my-bucket} (bucket=="my-bucket", key=null).
     *
     * @return the parent URI of this URI.
     */
    public S3URI getParent() {
        if (key == null) {
            return null;
        }

        int lastDelimPos = key.lastIndexOf(S3_DELIMITER);
        if (lastDelimPos == -1) {
            return new S3URI(bucket, null);
        } else {
            return new S3URI(bucket, key.substring(0, lastDelimPos));
        }
    }

    /**
     * Creates a new URI by resolving the specified path relative to {@code this}. If {@code key == null}, the key
     * of the returned URI will simply be {@code relativePath}.
     *
     * @param relativePath  a path relative to {@code this}. Must be non-null.
     * @return  the child URI.
     * @throws IllegalArgumentException  if the path starts with a {@link #S3_DELIMITER delimiter}.
     *      The path must be relative.
     */
    public S3URI getChild(String relativePath) throws IllegalArgumentException {
        Preconditions.checkNotNull(relativePath, "relativePath must be non-null.");

        if (relativePath.isEmpty()) {
            return this;
        } else if (relativePath.startsWith(S3_DELIMITER)) {
            throw new IllegalArgumentException(String.format(
                    "Path '%s' should be relative but begins with the delimiter string '%s'.",
                    relativePath, S3_DELIMITER));
        }

        if (key == null) {
            return new S3URI(bucket, relativePath);
        } else {
            return new S3URI(bucket, key + S3_DELIMITER + relativePath);
        }
    }

    /**
     * Gets a temporary file to which the content of the S3Object that this URI points to can be downloaded.
     * The path of the file is {@code ${java.io.tmpdir}/S3Objects/${hashCode()}/${getBaseName()}}.
     * This method does not cause the file to be created; we're just returning a suitable path.
     *
     * @return a temporary file to which the content of the S3Object that this URI points to can be downloaded.
     */
    public File getTempFile() {
        // To avoid collisions of files with the same name, create a parent dir named after the S3URI's hashCode().
        File parentDir = new File(S3ObjectTempDir, String.valueOf(hashCode()));
        return new File(parentDir, getBaseName());
    }

    //////////////////////////////////////// Object ////////////////////////////////////////

    /**
     * Returns a string representation of the URI in the form {@code s3://<bucket>/<key>}.
     *
     * @return  a string representation of the URI.
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3URI other = (S3URI) o;
        return this.bucket.equals(other.bucket) &&
               this.key == null ? other.key == null : this.key.equals(other.key);
    }

    @Override
    public int hashCode() {
        int result = bucket.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
