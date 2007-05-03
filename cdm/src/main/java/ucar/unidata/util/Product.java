package ucar.unidata.util;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: May 3, 2007
 * Time: 11:40:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class Product {
    private String id;
    private String name;

    public Product() {
        
    }

    public Product(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getID() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean equals(Object oo) {

        if ( !(oo instanceof Product)) {
            return false;
        }
        Product that = (Product) oo;

        return this.id.equals(that.id);
    }

}
