package thredds.crawlabledataset.s3;

public class AmazonS3ClientOptions {
    public static final String DEFAULT_SERVICE_ENDPOINT = "http://s3.amazonaws.com";
    public static final int DEFAULT_MAX_LISTING_PAGES = Integer.MAX_VALUE;

    private String serviceEndpoint;
    private int maxListingPages;
    private AmazonS3ConnectionOptions objectRequestConnectionOptions;
    private AmazonS3ConnectionOptions listingRequestConnectionOptions;

    public AmazonS3ClientOptions() {
        this.serviceEndpoint = DEFAULT_SERVICE_ENDPOINT;
        this.maxListingPages = DEFAULT_MAX_LISTING_PAGES;
        this.objectRequestConnectionOptions = new AmazonS3ConnectionOptions();
        this.listingRequestConnectionOptions = new AmazonS3ConnectionOptions();
    }

    public void setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }

    public void setMaxListingPages(int maxListingPages) {
        this.maxListingPages = maxListingPages;
    }

    public int getMaxListingPages() {
        return maxListingPages;
    }

    public void setObjectRequestConnectionOptions(AmazonS3ConnectionOptions objectRequestConnectionOptions) {
        this.objectRequestConnectionOptions = objectRequestConnectionOptions;
    }

    public AmazonS3ConnectionOptions getObjectRequestConnectionOptions() {
        return objectRequestConnectionOptions;
    }

    public void setListingRequestConnectionOptions(AmazonS3ConnectionOptions listingRequestConnectionOptions) {
        this.listingRequestConnectionOptions = listingRequestConnectionOptions;
    }

    public AmazonS3ConnectionOptions getListingRequestConnectionOptions() {
        return listingRequestConnectionOptions;
    }
}
