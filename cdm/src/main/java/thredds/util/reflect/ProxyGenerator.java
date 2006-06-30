package thredds.util.reflect;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

public class ProxyGenerator {

  static void generate( Class c, PrintStream out) throws SecurityException {

    String proxyName = c.getName()+"Proxy";

    out.println();
    out.println( "public class "+proxyName+" extends "+ c.getName()+ "{");
    out.println( "  private "+c.getName()+" proxy;\n");
    out.println( "  public "+proxyName+" ( "+c.getName()+" proxy) {");
    out.println( "    this.proxy = proxy;");
    out.println( "  }\n");

    ArrayList allMethods = new ArrayList();
    addAllMethods( allMethods, c);
    Collections.sort( allMethods, new MethodComparator());
    for (int i=0; i < allMethods.size(); i++) {
       genMethod( (Method) allMethods.get(i), out);
    }

    out.println( "}");
  }

  private static void addAllMethods( ArrayList allMethods, Class c) {
    if (c == null) return;
    if (c == Object.class) return;
    Method[] methodsArray = c.getDeclaredMethods();
    allMethods.addAll( Arrays.asList(methodsArray));

    addAllMethods( allMethods, c.getSuperclass());
  }

  static void genMethod( Method m, PrintStream out) throws SecurityException {
    int mods = m.getModifiers();
    if (Modifier.isFinal( mods) || Modifier.isPrivate( mods) || Modifier.isStatic( mods))
      return;

    out.print( "  "+Modifier.toString(mods));
    out.print( " "+m.getReturnType().getName());
    out.print( " "+m.getName()+"(");

    Class[] params = m.getParameterTypes();
    for (int i=0; i < params.length; i++) {
      if (i > 0) out.print(", ");
      out.print( makeClassName(params[i])+" p"+i);
    }
    out.print( ")");

    Class[] ex = m.getExceptionTypes();
    if (ex.length > 0) {
      out.print(" throws ");
      for (int i = 0; i < ex.length; i++) {
        if (i > 0) out.print(", ");
        out.print(ex[i].getName());
      }
    }

    out.println( " {");

    out.print("    ");
    if (!m.getReturnType().getName().equals("void"))
    out.print("return ");

    out.print("proxy."+m.getName()+"(");
    for (int i=0; i < params.length; i++) {
      if (i > 0) out.print(",");
      out.print(" p"+i);
    }
    out.println( ");");
    out.println( "  }\n");
  }

   static String makeClassName( Class c) {
     if (c.isArray()) {
       return makeClassName(c.getComponentType()) +"[]";
     }
     return c.getName();
   }

  public static class MethodComparator implements java.util.Comparator {
    public int compare(Object o1, Object o2) {
      Method t1 = (Method) o1;
      Method t2 = (Method) o2;
      return t1.getName().compareTo(t2.getName());
    }

    public boolean equals(Object obj) {
      return obj == this;
    }
  }

  static public void showMethods( Class c, PrintStream out) {

    out.println( "Methods for class "+ c.getName());

    Method[] methodsArray = c.getDeclaredMethods();

    for (int i=0; i < methodsArray.length; i++) {
       Method m = methodsArray[i];
       System.out.println(" "+m.getName());
    }
  }


  public static void main(String[] args)  throws SecurityException {
    generate( thredds.catalog.InvDatasetImpl.class, System.out);
  }

}