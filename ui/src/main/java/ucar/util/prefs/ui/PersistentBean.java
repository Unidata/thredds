/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.util.prefs.ui;

import ucar.util.prefs.PersistenceManager;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Manages mapping of PrefPanel fields to java beans.
 *
 * @author John Caron
 */

public class PersistentBean implements PersistenceManager {
  private BeanMap beanMap;
  private boolean debugBean = false;

  public PersistentBean( Object bean) {
    beanMap = new BeanMap( bean);
  }

  // public methods for PersistenceManager interface
  public Object getObject(String name) {
    return beanMap.getObject( name);
  }

  public void putObject(String name, Object value) {
    beanMap.putObject( name, value);
  }

  public void addPreferenceChangeListener(java.util.prefs.PreferenceChangeListener pcl) { }

  public String get(String key, String def) {
    Object value = getObject( key);
    return (value == null) ? def : value.toString();
  }

  public void put(String key, String value) {
    putObject( key, value);
  }

  public double getDouble(String key, double def) {
    Object value = getObject( key);
    return (value == null) ? def : ((Number) value).doubleValue();
  }

  public void putDouble(String key, double value) {
    putObject( key, new Double(value));
  }

  public boolean getBoolean(String key, boolean def) {
    Object value = getObject( key);
    return (value == null) ? def : (Boolean) value;
  }

  public void putBoolean(String key, boolean value) {
    putObject( key, Boolean.valueOf(value));
  }

  public int getInt(String key, int def) {
    Object value = getObject( key);
    return (value == null) ? def : ((Number) value).intValue();
  }

  public void putInt(String key, int value) {
    putObject( key, new Integer(value));
  }

  public long getLong(String key, long def) {
    Object value = getObject( key);
    return (value == null) ? def : ((Number) value).longValue();
  }

  public void putLong(String key, long value) {
    putObject( key, new Long(value));
  }

  public java.util.List getList(String key, java.util.List def)  {
    Object value = getObject( key);
    return (value == null) ? def : (List) value;
  }

  public void putList(String key, java.util.List value) {
    putObject( key, value);
  }

  // one for each bean; handles nested beans
  private class BeanMap {
    private Object bean;
    private PropertyMap pmap;
    private Map<String, BeanMap> beanMaps = new HashMap<>(); // nested BeanMap
    private Object[] args = new Object[1];

    BeanMap( Object bean){
      this.bean = bean;
      this.pmap = PropertyMap.getParser( bean.getClass());
    }

    private ProxyProp getPropertyDescriptor(String name) {
      // non-nested
      PropertyDescriptor prop = pmap.findProperty(name);
      if (prop != null)
        return new ProxyProp( prop, null, null);

      // see if its nested
      int pos = name.indexOf(".");
      if (pos < 0)
        throw new IllegalArgumentException("PersistentBean: no property named "+name);

      // break out first bean name
      String parentName = name.substring(0, pos);
      String childrenName = name.substring(pos+1);
      prop = pmap.findProperty( parentName);
      if (prop == null)
        throw new IllegalArgumentException("PersistentBean: no property named "+parentName);

      BeanMap nested = beanMaps.get( parentName);
      if (nested == null) {
        // first time - create a nested BeanMap
        Object bean = getObject( parentName);
        if (bean == null) {
          // create a new one
          bean = createObject(prop);
          putObject(parentName, bean);
        }
        nested = new BeanMap(bean);
        beanMaps.put(parentName, nested);
      }

      return new ProxyProp( null, nested, childrenName);
    }

    private Object createObject(PropertyDescriptor prop) {
      Class propClass = prop.getPropertyType();
      try {
        return propClass.newInstance();
      } catch (Exception ee) {
        ee.printStackTrace();
        // System.out.println("PersistentBean error createObject: "+prop.getName()+" "+ee.getMessage());
        throw new IllegalArgumentException("PersistentBean error createObject: "+prop.getName()+" "+ee.getMessage());
      }
    }

    public Object getObject(String name) {
      if (debugBean) System.out.println( "PersistentBean read "+name);
      ProxyProp proxy = getPropertyDescriptor(name);
      if (proxy.prop == null) {
        return proxy.nested.getObject(proxy.childrenName);
      }
      PropertyDescriptor prop = proxy.prop;

      Object value = null;
      try {
        Method m = prop.getReadMethod();
        if (m == null) {
          System.out.println("PersistentBean no read method for: "+name);
          return null;
        }
        value = m.invoke( bean, (Object []) null);

      } catch (InvocationTargetException ee) {
        System.out.println("PersistentBean error read: "+name+" "+ee.getCause());
        ee.getCause().printStackTrace();

      } catch (Exception ee) {
        System.out.println("PersistentBean error read: "+name+" "+ee);
        ee.printStackTrace();
      }

      return value;
    }

    public void putObject(String name, Object value) {
      if (debugBean) System.out.println( "PersistentBean write "+name + " = "+value +" "+value.getClass().getName());
      ProxyProp proxy = getPropertyDescriptor(name);
      if (proxy.prop == null) {
        proxy.nested.putObject(proxy.childrenName, value);
        return;
      }
      PropertyDescriptor prop = proxy.prop;

      args[0] = value;
      try {
        Method m = prop.getWriteMethod();
        if (m == null) {
          System.out.println("PersistentBean no write method for: "+name);
          return;
        }
        m.invoke( bean, args);

      } catch (InvocationTargetException ee) {
        System.out.println("PersistentBean error write: "+name+" "+ee.getCause());
        ee.getCause().printStackTrace();

       } catch (Exception ee) {
        System.out.println("PersistentBean error write: "+name+" "+ee);
        ee.printStackTrace();
      }
    }
  }

  // helper class
  private static class ProxyProp {
    private PropertyDescriptor prop;
    private BeanMap nested;
    private String childrenName;

    ProxyProp(PropertyDescriptor prop, BeanMap nested, String childrenName) {
      this.prop = prop;
      this.nested = nested;
      this.childrenName = childrenName;
    }
  }

  // one for each class
  private static class PropertyMap {
    private static boolean debugBeanParser = false, debugBeanParserDetail = false;
    private static Map<Class,PropertyMap> parsers = new HashMap<>();

    static PropertyMap getParser( Class beanClass) {
      PropertyMap parser;
      if (null == (parser = parsers.get( beanClass))) {
        parser = new PropertyMap( beanClass);
        parsers.put( beanClass, parser);
      }
      return parser;
    }

    private Map<String,PropertyDescriptor> properties = new LinkedHashMap<>();

    PropertyMap( Class beanClass) {
      // get bean info
      BeanInfo info;
      try {
        info = Introspector.getBeanInfo(beanClass, Object.class);
      } catch (IntrospectionException e) {
        e.printStackTrace();
        return;
      }

      if (debugBeanParser)
        System.out.println( "Bean "+beanClass.getName());

      // properties must have read method
      PropertyDescriptor[] pds = info.getPropertyDescriptors();
      for (PropertyDescriptor prop : pds) {
        Class propClass = prop.getPropertyType();

        if ((prop.getReadMethod() != null)) { // && (prop.getWriteMethod() != null)) {
          properties.put(prop.getName(), prop);
          if (debugBeanParser) System.out.println(" read/write property " + prop.getName() + " " +
                  propClass.getName() + " prim= " + propClass.isPrimitive());

          /* if (!propClass.isPrimitive() && !(propClass == java.lang.String.class)) {
            PropertyMap nestedParser = PropertyMap.getParser(propClass);
            for (Iterator iter = nestedParser.getProperties(); iter.hasNext(); ) {
              PropertyDescriptor nestedProp = (PropertyDescriptor)iter.next();
              properties.put( prop.getName()+"."+nestedProp.getName(), nestedProp);
              if (debugBeanParser) System.out.println( " -added property "+
                prop.getName()+"."+nestedProp.getName());
           }
          } */
        }
      }

      if (debugBeanParserDetail) {
        System.out.println( " Properties:");
        for (PropertyDescriptor pd : pds) {
          String name = pd.getName();
          Class type = pd.getPropertyType();
          Method rm = pd.getReadMethod();
          Method wm = pd.getWriteMethod();
          System.out.println("  " + name + " " + type.getName() + " read= " + rm + " write= " + wm + " " + pd.isPreferred());
          System.out.println("     displayname= " + pd.getDisplayName());
        }
      }
    }

    Iterator<PropertyDescriptor> getProperties() { return properties.values().iterator(); }

    PropertyDescriptor findProperty( String name) {
      return properties.get(name);
    }
 }

}
