package thredds.server.metadata;

import javax.validation.constraints.NotNull;

/**
 * Describe
 *
 * @author caron
 * @since 6/5/12
 */

public class MetadataRequestParameterBean {

  @NotNull
  private String metadata;
  private String accept;

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  public String getAccept() {
    return accept;
  }

  public void setAccept(String accept) {
    this.accept = accept;
  }
}
