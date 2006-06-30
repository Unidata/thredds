package thredds.util.net;

import org.apache.commons.httpclient.auth.CredentialsProvider;

public interface CredentialsProviderExt extends CredentialsProvider {
  public void setHttpSession( HttpSession hs);
}
