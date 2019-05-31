package ucar.ui.prefs;

import java.util.Date;

public class TestBean {
  private String name, path, sbase, dtype, stype, ddhref;
  private boolean u;
  private int i;
  private Date now = new Date();

  public TestBean() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  // should not be editable
  public String getPath() {
    return path;
  }

  void setPath(String path) {
    this.path = path;
  }

  // should not appear
  String getServerBase() {
    return sbase;
  }

  void setServerBase(String sbase) {
    this.sbase = sbase;
  }

  // should not appear
  String getDataType() {
    return dtype;
  }

  void setDataType(String dtype) {
    this.dtype = dtype;
  }

  public String getServerType() {
    return stype;
  }

  public void setServerType(String stype) {
    this.stype = stype;
  }

  public String getDDref() {
    return ddhref;
  }

  public void setDDref(String ddhref) {
    this.ddhref = ddhref;
  }

  public boolean getUse() {
    return u;
  }

  public void setUse(boolean u) {
    this.u = u;
  }

  public int getII() {
    return i;
  }

  public void setII(int i) {
    this.i = i;
  }

  public Date getNow() {
    return now;
  }

  public void setNow(Date now) {
    this.now = now;
  }


  public static String editableProperties() {
    return "name path serverbase serverType DDref use II now";
  }

}
