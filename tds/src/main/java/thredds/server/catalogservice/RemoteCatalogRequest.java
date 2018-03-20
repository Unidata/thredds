/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalogservice;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * Command object for catalog service requests on remote catalogs.
 * Uses JSR-303 Validation
 */
public class RemoteCatalogRequest {
  public enum Command { SHOW, SUBSET, VALIDATE }

  private URI catalogUri;

  @NotNull
  private String catalog;
  private String dataset;

  // these are automatically converted
  private Command command;
  private boolean verbose;
  private boolean htmlView = true;

  public URI getCatalogUri() {
    return catalogUri;
  }

  public void setCatalogUri(URI catalogUri) {
    this.catalogUri = catalogUri;
  }

  public String getCatalog() {
    return catalog;
  }

  public void setCatalog(String catalog) {
    this.catalog = catalog;
  }

  public Command getCommand() {
    return command;
  }

  public void setCommand(Command command) {
    this.command = command;
  }

  public String getDataset() {
    return dataset;
  }

  public void setDataset(String dataset) {
    this.dataset = dataset;
  }

  public boolean isVerbose() {
    return verbose;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public boolean isHtmlView() {
    return htmlView;
  }

  public void setHtmlView(boolean htmlView) {
    this.htmlView = htmlView;
  }
}