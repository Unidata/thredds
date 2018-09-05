package thredds.crawlabledataset.s3;

public class AmazonS3ConnectionOptions {
    public static final int DEFAULT_CONNECT_TIME_OUT = 50 * 1000;
    public static final int DEFAULT_SOCKET_TIME_OUT = 50 * 1000;

    private int connectTimeOut;
    private int socketTimeOut;

    public AmazonS3ConnectionOptions() {
        this.connectTimeOut = DEFAULT_CONNECT_TIME_OUT;
        this.socketTimeOut = DEFAULT_SOCKET_TIME_OUT;
    }

    public void setConnectTimeOut(int connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    public void setSocketTimeOut(int socketTimeOut) {
        this.socketTimeOut = socketTimeOut;
    }

    public int getSocketTimeOut() {
        return socketTimeOut;
    }
}
