package thredds.crawlabledataset;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CrawlableDatasetAmazonS3 extends CrawlableDatasetFile
{
    static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CrawlableDatasetAmazonS3.class);

    private final Object configObject;
    private final String path;
    private ThreddsS3Object s3Object = null;

    private static final String EHCACHE_S3_KEY = "S3";

    public CrawlableDatasetAmazonS3(String path, Object configObject)
    {
        super(path, configObject);
        this.path = path;
        this.configObject = configObject;
    }

    private CrawlableDatasetAmazonS3(CrawlableDatasetAmazonS3 parent, ThreddsS3Object s3Object)
    {
        this(S3Helper.concat(parent.getPath(), s3Object.key), null);
        this.s3Object = s3Object;
    }

    @Override
    public Object getConfigObject()
    {
        return configObject;
    }

    @Override
    public String getPath()
    {
        return path;
    }

    @Override
    public String getName()
    {
        return new File(path).getName();
    }

    @Override
    public File getFile()
    {
        return S3Helper.getS3File(path);
    }

    @Override
    public CrawlableDataset getParentDataset()
    {
        return new CrawlableDatasetAmazonS3(S3Helper.parent(path), configObject);
    }

    @Override
    public boolean exists()
    {
        return true;
    }

    @Override
    public boolean isCollection()
    {
        if (null == s3Object)
            return true;
        else
            return s3Object.type == ThreddsS3Object.Type.DIR;
    }

    @Override
    public CrawlableDataset getDescendant(String relativePath)
    {
        if (relativePath.startsWith("/"))
            throw new IllegalArgumentException("Path must be relative <" + relativePath + ">.");

        ThreddsS3Object obj = new ThreddsS3Object(relativePath, ThreddsS3Object.Type.DIR);
        return new CrawlableDatasetAmazonS3(this, obj);
    }

    @Override
    public List<CrawlableDataset> listDatasets() throws IOException
    {
        if (!this.isCollection())
        {
            String tmpMsg = "This dataset <" + this.getPath() + "> is not a collection dataset.";
            log.error( "listDatasets(): " + tmpMsg);
            throw new IllegalStateException(tmpMsg);
        }

        List<ThreddsS3Object> listing = S3Helper.listS3Dir(this.path);

        if (listing.isEmpty())
        {
            log.error("listDatasets(): the underlying file [" + this.path + "] exists, but is empty");
            return Collections.emptyList();
        }

        List<CrawlableDataset> list = new ArrayList<CrawlableDataset>();
        for (ThreddsS3Object s3Object : listing)
        {
            CrawlableDatasetAmazonS3 crDs = new CrawlableDatasetAmazonS3(this, s3Object);
            list.add(crDs);
        }

        return list;
    }

    @Override
    public long length()
    {
        if (null != s3Object)
            return s3Object.size;

        throw new RuntimeException(String.format("Uknown size for object '%s'", path));
    }

    @Override
    public Date lastModified()
    {
        if (null != s3Object)
            return s3Object.lastModified;

        throw new RuntimeException(String.format("Uknown lastModified for object '%s'", path));
    }

    public static class ThreddsS3Object
    {
        public final String key;
        public final long size;
        public final Date lastModified;
        public enum Type {
            DIR,
            FILE
        }
        public final Type type;

        public ThreddsS3Object(String key, long size, Date lastModified, Type type)
        {
            this.key = key;
            this.size = size;
            this.lastModified = lastModified;
            this.type = type;
        }

        public ThreddsS3Object(String key, Type type)
        {
            this(key, -1, null, type);
        }
    }

    private static class S3Helper
    {
        private static String S3_PREFIX = "s3://";
        private static String S3_DELIMITER = "/";

        public static String concat(String parent, String child)
        {
            if (child.isEmpty())
                return parent;
            else
                return parent + S3_DELIMITER + removeTrailingSlash(child);
        }

        public static String parent(String uri)
        {
            int delim = uri.lastIndexOf(S3_DELIMITER);
            return uri.substring(0, delim);
        }

        public static String[] s3UriParts(String uri) throws Exception
        {
            if (uri.startsWith(S3_PREFIX))
            {
                uri = uri.replaceFirst(S3_PREFIX, "");
                String[] parts = new String[2];
                int delim = uri.indexOf(S3_DELIMITER);
                parts[0] = uri.substring(0, delim);
                parts[1] = uri.substring(Math.min(delim + 1, uri.length()), uri.length());
                return parts;
            }
            else
                throw new IllegalArgumentException(String.format("Not a valid s3 uri: %s", uri));
        }

        private static String stripPrefix(String key, String prefix)
        {
            return key.replaceFirst(prefix, "");
        }

        private static String removeTrailingSlash(String str)
        {
            if (str.endsWith(S3_DELIMITER))
                str = str.substring(0, str.length() - 1);

            return str;
        }

        private static AmazonS3Client getS3Client()
        {
            // Use HTTP, it's much faster
            AmazonS3Client s3Client = new AmazonS3Client();
            s3Client.setEndpoint("http://s3.amazonaws.com");
            return  s3Client;
        }

        public static File getS3File(String uri)
        {
            log.debug(String.format("S3 Downloading '%s'", uri));

            try
            {
                String[] uriParts = s3UriParts(uri);
                String s3Bucket = uriParts[0];
                String s3Key = uriParts[1];

                S3Object object = getS3Client().getObject(new GetObjectRequest(s3Bucket, s3Key));

                Path tmpDir = Files.createTempDirectory("S3Download_");
                String fileBasename = new File(uri).getName();
                File tmpFile = new File(tmpDir.toFile(), fileBasename);

                log.info(String.format("S3 Downloading 's3://%s/%s' to '%s'", s3Bucket, s3Key, tmpFile.toString()));
                OutputStream os = new FileOutputStream(tmpFile);
                InputStream is = object.getObjectContent();

                IOUtils.copy(is, os);

                return tmpFile;
            }
            catch (Exception e)
            {
                log.error(String.format(String.format("S3 Error downloading '%s'", uri)));
                e.printStackTrace();
            }

            return null;
        }

        public static List<ThreddsS3Object> listS3Dir(String uri)
        {
            List<ThreddsS3Object> listing = new ArrayList<ThreddsS3Object>();

            log.debug(String.format("S3 Listing '%s'", uri));

            try
            {
                String[] uriParts = s3UriParts(uri);
                String s3Bucket = uriParts[0];
                String s3Key = uriParts[1];

                if (!s3Key.endsWith(S3_DELIMITER))
                    s3Key += S3_DELIMITER;

                final ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(s3Bucket)
                        .withPrefix(s3Key)
                        .withDelimiter(S3_DELIMITER);

                final ObjectListing objectListing = getS3Client().listObjects(listObjectsRequest);

                for (final S3ObjectSummary objectSummary : objectListing.getObjectSummaries())
                {
                    listing.add(new ThreddsS3Object(
                            stripPrefix(objectSummary.getKey(), s3Key),
                            objectSummary.getSize(),
                            objectSummary.getLastModified(),
                            ThreddsS3Object.Type.FILE
                    ));
                }

                for (final String commonPrefix : objectListing.getCommonPrefixes())
                {
                    String key = stripPrefix(commonPrefix, s3Key);
                    key = removeTrailingSlash(key);

                    listing.add(new ThreddsS3Object(key, ThreddsS3Object.Type.DIR));
                }
            }
            catch (Exception e)
            {
                log.error(String.format(String.format("S3 Error listing '%s'", uri)));
                e.printStackTrace();
            }

            return listing;
        }
    }

}
