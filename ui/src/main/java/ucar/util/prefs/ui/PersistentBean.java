// $Id: PersistentBean.java,v 1.3 2006/05/08 02:47:23 caron Exp $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.util.prefs.ui;

import java.util.prefs.*;
import ucar.util.prefs.*;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Manages mapping of PrefPanel fields to java beans.
 *
 * @author John Caron
 * @version $Revision: 1.3 $ $Date: 2006/05/08 02:47:23 $
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
    return (value == null) ? def : ((Boolean) value).booleanValue();
  }

  public void putBoolean(String key, boolean value) {
    putObject( key, new Boolean(value));
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
    private HashMap beanMaps = new HashMap(); // nested BeanMap
    private Object[] args = new Object[1];

    BeanMap( Object bean){
      this.bean = bean;
      this.pmap = PropertyMap.getParser( bean.getClass());
    }

    private void checkExist(String name) {
      // non-nested
      PropertyDescriptor prop = pmap.findProperty(name);
      if (prop != null) return;

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

      BeanMap nested = (BeanMap) beanMaps.get( parentName);
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

      BeanMap nested = (BeanMap) beanMaps.get( parentName);
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
      return;
    }
  }

  // helper class
  private class ProxyProp {
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
    private static HashMap parsers = new HashMap();

    static PropertyMap getParser( Class beanClass) {
      PropertyMap parser;
      if (null == (parser = (PropertyMap) parsers.get( beanClass))) {
        parser = new PropertyMap( beanClass);
        parsers.put( beanClass, parser);
      }
      return parser;
    }

    private LinkedHashMap properties = new LinkedHashMap();

    PropertyMap( Class beanClass) {

      // get bean info
      BeanInfo info = null;
      try {
        info = Introspector.getBeanInfo(beanClass, Object.class);
      } catch (IntrospectionException e) {
        e.printStackTrace();
      }

      if (debugBeanParser)
        System.out.println( "Bean "+beanClass.getName());

      // properties must have read method
      PropertyDescriptor[] pds = info.getPropertyDescriptors();
      for (int i=0; i< pds.length; i++) {
        PropertyDescriptor prop = pds[i];
        Class propClass = prop.getPropertyType();

        if ((prop.getReadMethod() != null)) { // && (prop.getWriteMethod() != null)) {
          properties.put( prop.getName(), prop);
          if (debugBeanParser) System.out.println( " read/write property "+prop.getName()+" "+
                propClass.getName()+" prim= "+propClass.isPrimitive());

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
        for (int i=0; i< pds.length; i++) {
          String name = pds[i].getName();
          Class type = pds[i].getPropertyType();
          Method rm = pds[i].getReadMethod();
          Method wm = pds[i].getWriteMethod();
          System.out.println( "  "+name+" "+type.getName()+" read= "+rm+" write= "+wm+" "+pds[i].isPreferred());
          System.out.println( "     displayname= "+pds[i].getDisplayName());
        }
      }
    }

    Iterator getProperties() { return properties.values().iterator(); }

    PropertyDescriptor findProperty( String name) {
      return (PropertyDescriptor) properties.get(name);
    }
 }

}

/* Change History:
   $Log: PersistentBean.java,v $
   Revision 1.3  2006/05/08 02:47:23  caron
   cleanup code for 1.5 compile
   modest performance improvements
   dapper reading, deal with coordinate axes as structure members
   improve DL writing
   TDS unit testing

   Revision 1.2  2005/09/13 15:51:19  caron
   *** empty log message ***

   Revision 1.1  2004/11/04 00:45:27  caron
   no message

*/