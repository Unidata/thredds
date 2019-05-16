/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.event;

/** Change events for UI objects.
 * @author John Caron
 */
public class UIChangeEvent extends java.util.EventObject {
  private String property;
  private Object objectChanged;
  private Object newValue;

  public UIChangeEvent(Object source, String property, Object changed, Object newValue) {
    super(source);
    this.property = property;
    this.objectChanged = changed;
    this.newValue = newValue;
  }

  public String getChangedProperty() { return property; }
  public Object getChangedObject() { return objectChanged; }
  public Object getNewValue() { return newValue; }

  public String toString() {
    return "UIChangeEvent: "+ property+ " objectChanged: "+ objectChanged+ "  newValue: "+ newValue;
  }
}
