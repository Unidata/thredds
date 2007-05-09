package IO;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.Set;

public class TimeDigest {
  static public void main( String args[]) {
    testDigest();
  }

  static private void testDigest() {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      Provider p = md.getProvider();
      Set<Provider.Service> services = p.getServices();
      for (Provider.Service service : services) {
        System.out.println("Service="+service);
      }
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }
}
