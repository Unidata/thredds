/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap.test;
//import java.util.Enumeration;
import java.util.Vector;
import dods.dap.*;
//import dods.dap.parser.*;

public class dap_test {
//    private static final String prompt = "dds-test: ";
//    private static final String version = "version 0.1";

    public static void main(String args[]) {
        test_das();
        test_dds();
    }

    private static void test_das() {
        System.out.println("DAS test:");

        DAS table = new DAS();
        AttributeTable at = new AttributeTable("table1");
        try {
            at.appendAttribute("Authors", Attribute.STRING, "jehamby");
            at.appendAttribute("Authors", Attribute.STRING, "jimg");
            at.addAlias("Creators", "Authors");
            AttributeTable cont = at.appendContainer("Container");
            cont.appendAttribute("Numbers", Attribute.INT32, "123");
            cont.appendAttribute("Floats", Attribute.FLOAT64, "456.0");
            table.addAttributeTable("table1", at);
            at = new AttributeTable("at2");
            at.appendAttribute("another", Attribute.BYTE, "12");
            table.addAttributeTable("table2", at);
        }
        catch (DASException e) {
            System.out.println("Error constructing DAS: " + e);
        }

        // print the table
        table.print(System.out);

        // get a name from the table
        String author2 = table.getAttributeTable("table1").getAttribute("Creators").getValueAt(1);
        System.out.println("author2 = " + author2);

        // test Cloneable interface
        DAS table2 = (DAS)table.clone();
        System.out.println("\nCloned table:");
        table2.print(System.out);

        // add an attribute to table1 and verify it's not in table 2
        at = new AttributeTable("empty");
        table.addAttributeTable("empty", at);
        if (table2.getAttributeTable("empty") != null)
            System.out.println("DAS clone failed");
        else
            System.out.println("DAS clone passed");
    }

    private static void test_dds() {
        System.out.println("DDS test:");

        DataDDS table = new DataDDS(new ServerVersion(2,16));
        table.setName("test_table");

        // add variables to it
        DUInt32 myUInt = new DUInt32("myUInt");
        myUInt.setValue(42);
        table.addVariable(myUInt);

        // note that arrays and lists take their name from the addVariable method
        DArray myArray = new DArray();
        myArray.addVariable(new DByte("myArray"));
        myArray.appendDim(10, "dummy");
        myArray.setLength(10);
        BytePrimitiveVector bpv = (BytePrimitiveVector)myArray.getPrimitiveVector();
        for(int i=0; i<10; i++)
            bpv.setValue(i, (byte)(i*10));
        table.addVariable(myArray);

        DList myList = new DList();
        myList.addVariable(new DURL("myList"));
        myList.setLength(10);
        BaseTypePrimitiveVector btpv = (BaseTypePrimitiveVector)myList.getPrimitiveVector();
        for(int i=0; i<10; i++) {
            DURL testURL = new DURL();
            testURL.setValue("http://" + i);
            btpv.setValue(i, testURL);
        }
        table.addVariable(myList);

        DStructure myStructure = new DStructure("myStructure");
        DFloat64 structFloat = new DFloat64("structFloat");
        structFloat.setValue(42.0);
        myStructure.addVariable(structFloat);
        DString structString = new DString("structString");
        structString.setValue("test value");
        myStructure.addVariable(structString);
        table.addVariable(myStructure);

        DGrid myGrid = new DGrid("myGrid");
        DArray gridArray = (DArray)myArray.clone();
        gridArray.setName("gridArray");
        myGrid.addVariable(gridArray, DGrid.ARRAY);
        DArray gridMap = new DArray();
        gridMap.addVariable(new DInt32("gridMap"));
        gridMap.appendDim(10, "dummy");
        gridMap.setLength(10);
        Int32PrimitiveVector ipv = (Int32PrimitiveVector)gridMap.getPrimitiveVector();
        for(int i=0; i<10; i++)
            ipv.setValue(i, i*10);
        myGrid.addVariable(gridMap, DGrid.MAPS);
        table.addVariable(myGrid);

        // this is the one case where two DODS variables can have the same name:
        // each row should have the same exact variables (name doesn't matter)
        DSequence mySequence = new DSequence("mySequence");
        mySequence.addVariable(new DInt32("seqInt32"));
        mySequence.addVariable(new DString("seqString"));
        Vector seqRow = new Vector();  // a row of the sequence
        DInt32 seqVar1 = new DInt32("seqInt32");
        seqVar1.setValue(1);
        seqRow.addElement(seqVar1);
        DString seqVar2 = new DString("seqString");
        seqVar2.setValue("string");
        seqRow.addElement(seqVar2);
        mySequence.addRow(seqRow);
        seqRow = new Vector();  // add a second row
        seqVar1 = new DInt32("seqInt32");
        seqVar1.setValue(3);
        seqRow.addElement(seqVar1);
        seqVar2 = new DString("seqString");
        seqVar2.setValue("another string");
        seqRow.addElement(seqVar2);
        mySequence.addRow(seqRow);
        table.addVariable(mySequence);

        try {
            table.checkSemantics();
            System.out.println("DDS passed semantic check");
        }
        catch (BadSemanticsException e) {
            System.out.println("DDS failed semantic check:\n" + e);
        }

        try {
            table.checkSemantics(true);
            System.out.println("DDS passed full semantic check");
        }
        catch (BadSemanticsException e) {
            System.out.println("DDS failed full semantic check:\n" + e);
        }

        // print the declarations
        System.out.println("declarations:");
        table.print(System.out);

        // print the data
        System.out.println("\nData:");
        table.printVal(System.out);
        System.out.println();

        // and read it programmatically
        try {
            int testValue1 = ((DUInt32)table.getVariable("myUInt")).getValue();
            System.out.println("myUInt = " + testValue1);
            byte testValue2 = ((BytePrimitiveVector)((DArray)table.getVariable("myArray")).getPrimitiveVector()).getValue(5);
            System.out.println("myArray[5] = " + testValue2);
            String testValue3 = ((DString)((BaseTypePrimitiveVector)((DList)table.getVariable("myList")).getPrimitiveVector()).getValue(5)).getValue();
            System.out.println("myList[5] = " + testValue3);
            double testValue4 = ((DFloat64)((DStructure)table.getVariable("myStructure")).getVariable("structFloat")).getValue();
            System.out.println("myStructure.structFloat = " + testValue4);
            int testValue5 = ((Int32PrimitiveVector)((DArray)((DGrid)table.getVariable("myGrid")).getVariable("gridMap")).getPrimitiveVector()).getValue(5);
            System.out.println("myGrid.gridMap[5] = " + testValue5);
            String testValue7 = ((DString)((DSequence)table.getVariable("mySequence")).getVariable(0, "seqString")).getValue();
            System.out.println("mySequence[0].seqString = " + testValue7);
        }
        catch (NoSuchVariableException e) {
            System.out.println("Error getting variable:\n" + e);
        }
        System.out.println();

        DataDDS table2 = (DataDDS)table.clone();  // test Cloneable interface
        try {
            table2.checkSemantics();
            System.out.println("DDS passed semantic check");
        }
        catch (BadSemanticsException e) {
            System.out.println("DDS failed semantic check:\n" + e);
        }

        try {
            table2.checkSemantics(true);
            System.out.println("DDS passed full semantic check");
        }
        catch (BadSemanticsException e) {
            System.out.println("DDS failed full semantic check:\n" + e);
        }

        // print the declarations and data
        System.out.println("clone declarations:");
        table2.print(System.out);
        System.out.println("\nData:");
        table2.printVal(System.out);
        System.out.println();

        // add some values to the original table
        DInt32 myNewInt = new DInt32("myNewInt");
        myNewInt.setValue(420);
        table.addVariable(myNewInt);

        // verify that they aren't in the cloned table
        try {
            DInt32 testInt = (DInt32)table2.getVariable("myNewInt");
            System.out.println("Error: value from table in table2");
        }
        catch (NoSuchVariableException e) {
            System.out.println("Variable cloning looks good");
        }
    }
}
