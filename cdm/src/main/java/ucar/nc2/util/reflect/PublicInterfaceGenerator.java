package ucar.nc2.util.reflect;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

public class PublicInterfaceGenerator {

  static void generate( Class c, boolean doAllMethods, PrintStream out) throws SecurityException {

    Class sc = c.getSuperclass();

    out.print( "public class "+ makeClassName(c));
    if (sc != Object.class)
      out.print(" extends "+makeClassName(sc));
    out.println(" {");

    ArrayList allMethods = new ArrayList();
    addAllMethods( allMethods, c, doAllMethods);
    Collections.sort( allMethods, new MethodComparator());
    for (int i=0; i < allMethods.size(); i++) {
       genMethod( (Method) allMethods.get(i), out);
    }

    out.println( "}");
  }

  private static void addAllMethods( ArrayList allMethods, Class c, boolean doAllMethods) {
    if (c == null) return;
    if (c == Object.class) return;
    Method[] methodsArray = doAllMethods ? c.getMethods() : c.getDeclaredMethods();
    allMethods.addAll( Arrays.asList(methodsArray));

    //addAllMethods( allMethods, c.getSuperclass());
  }

  static void genMethod( Method m, PrintStream out) throws SecurityException {
    int mods = m.getModifiers();
    if (Modifier.isPrivate( mods) || Modifier.isProtected( mods))
      return;

    out.print( "  "+Modifier.toString(mods));
    out.print( " "+makeClassName(m.getReturnType()));
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

    out.println( ";");
  }

   static String makeClassName( Class c) {
     if (c.isArray()) {
       return makeClassName(c.getComponentType()) +"[]";
     }

     String name = c.getName();
     Package p = c.getPackage();
     if (p == null) return name;
     String packageName = p.getName();
     return name.substring( packageName.length()+1);
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
    generate( ucar.nc2.dataset.grid.GeoGrid.class, false, System.out);
  }

}