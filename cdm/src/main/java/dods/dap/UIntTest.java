package dods.dap; // JC-CHANGED
import java.io.*;

class UIntTest {


        UIntTest(){
        }


    public void sendIt(DataOutputStream fp)throws Exception{

            short   s;
            byte    b;
            int     i;
            long    l;

            s = ((short)65500);
            System.out.println("\nShort assigned to 65500.    System thinks of it as: " + s);
            fp.writeShort(s);
            System.out.println("Wrote it to disk. ");

            s = ((short)65537);
            System.out.println("\nShort assigned to 65537.    System thinks of it as: " + s);
            fp.writeShort(s);
            System.out.println("Wrote it to disk. ");



            i = ((int)4294967040L);
            System.out.println("\nInt assigned to 4294967040. System thinks of it as: " + i);
            fp.writeInt(i);
            System.out.println("Wrote it to disk. ");

            i = ((int)4294967298L);
            System.out.println("\nInt assigned to 4294967298. System thinks of it as: " + i);
            fp.writeInt(i);
            System.out.println("Wrote it to disk. ");

        }


     public void getIt(DataInputStream fp)throws Exception{

            short   s;
            int     i1, i2;
            long    l;


            System.out.println("\nReading data...");
            s  = fp.readShort();
            System.out.println("System read short from file as: " + s);
            i1 = ((int)s);
            System.out.println("Converted short to int: " + i1);
            i1 = i1 & 0xFFFF;
            System.out.println("And'd with 0xFFFF (represented as an int in memory): " + i1);

            System.out.println("\nReading data...");
            s  = fp.readShort();
            System.out.println("System read short from file as: " + s);
            i1 = ((int)s);
            System.out.println("Converted short to int: " + i1);
            i1 = i1 & 0xFFFF;
            System.out.println("And'd with 0xFFFF (represented as an int in memory): " + i1);




            System.out.println("\nReading data...");
            i2 = fp.readInt();
            System.out.println("\nSystem read int from file as: " + i2);
            l = ((long)i2);
            System.out.println("Converted int to long: " + l);
            l = l & 0xFFFFFFFFL;
            System.out.println("And'd with 0xFFFFFFFFL (represented as a long in memory): " + l);

            System.out.println("\nReading data...");
            i2 = fp.readInt();
            System.out.println("\nSystem read int from file as: " + i2);
            l = ((long)i2);
            System.out.println("Converted int to long: " + l);
            l = l & 0xFFFFFFFFL;
            System.out.println("And'd with 0xFFFFFFFFL (represented as a long in memory): " + l);




        }





        static public void main(String args[]) throws Exception {

            UIntTest b = new UIntTest();
            File f = new File("UIntTest.bin");
            FileOutputStream fp = new FileOutputStream(f);
            DataOutputStream sink = new DataOutputStream(fp);

            b.sendIt(sink);
            sink.close();

            FileInputStream ifp = new FileInputStream(f);
            DataInputStream source = new DataInputStream(ifp);

            b.getIt(source);
            source.close();

        }

}

