package thredds.util.net;

import org.apache.commons.httpclient.auth.CredentialsProvider;

/**
 * @deprecated use ucar.nc2.dataset.HttpClientManager
 */
public interface CredentialsProviderExt extends CredentialsProvider {
  public void setHttpSession( HttpSession hs);
}
