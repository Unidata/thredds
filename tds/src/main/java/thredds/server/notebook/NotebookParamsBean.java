package thredds.server.notebook;

public class NotebookParamsBean {
  protected String catalog;

  public NotebookParamsBean() {}

  public NotebookParamsBean(NotebookParamsBean from) {
    this.catalog = from.catalog;
  }

  public String getCatalog() {
    return catalog;
  }

  public void setCatalog(String catalog) {
    this.catalog = catalog;
  }
}
