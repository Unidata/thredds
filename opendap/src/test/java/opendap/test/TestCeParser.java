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

package opendap.test;

import opendap.dap.*;
import opendap.dap.parsers.*;
import opendap.servers.*;
import opendap.servers.parsers.CeParser;
import org.junit.Assert;
import org.junit.Test;
import ucar.unidata.util.test.UnitTestCommon;
//import opendap.dts.*;

import java.util.*;
import java.io.*;

// Test that the Constraint parsing is correct


public class TestCeParser extends UnitTestCommon
{
  static final boolean DEBUGPARSER = false;

  static boolean generate = true;

  // Constraint list produced by GenerateConstraints.java
  static final String[] xconstraints = {
          //"v1&g.a[0][0][2]={sq.f3[2][2][0],37.0}&st.f1[0]!=101"
  };
  static final String[] xexpected = {
    "v1&g.a[0][0][2]={sq.f3[2][2][0],37.0}&st.f1[0]!=101"
  };

  static final String[] constraints = {
          "v1&g.a[0][0][2]={sq.f3[2][2][0],37.0}&st.f1[0]!=101",
          "v2[2:2:9][0],g.a[2:2:9][1:9][2:2:9]",
          "v2[1:9][1:9]&st.f1[2]<=101&v1[0]>=\"string\"",
          "st.f3[1:9][1:9][1:9]&g.m1[0]<=101",
          "v3[2:2:9][0][2:2:9],g.m3[0]&st.f0>\"string\"",
          "st.f0",
          "g",
          "sq.f2[0][0]&v1[2]<=\"string\"&g.a[0][2][2]<=37.0",
          "sq.f2[1:9][0]&g.a[0][2][2]<{37.0,37.0}&g.m2[9]>101",
          "sq.f3[2:2:9][0][0],sq.f0&v2[9][9]=101&g.a[2][9][2]>=37.0",
          "&sq.f3[2][2][2]=37.0",
          "v0",
          "&g.m3[0]<=101&st.f2[2][2]<=101",
          "sq.f2[0][0],sq.f1[0]&g.m2[0]!={101,101,101,101}&g.m3[0]={101,101,g.m1[9]}",
          "v0",
          "st.f2[1:9][0],st.f0&st.f3[2][9][0]!=101&sq.f1[2]>=101",
          "v2&g.m1[2]<101&v1[2]>{\"string\",\"string\",\"string\"}",
          "sq.f1[1:9]",
          "v2[2:2:9][1:9]",
          "g.a[1:9][0][0]&g.m3[2]<=101&st.f0>=\"string\"",
          "sq.f3[1:9][2:2:9][0]&st.f2[2][9]<101&sq.f1[2]<101",
          "sq.f3[1:9][1:9][1:9]&v2[0][0]!={101,101}&g.m1[2]>101",
          "st&g.m3[0]!=101",
          "sq.f2[2:2:9][1:9]&v2[2][9]=101",
          "st",
          "v1[2:2:9],sq",
          "v0,st.f0&st.f2[2][2]>=g.m3[2]&g.m1[0]<=101",
          "v0&sq.f2[9][9]>=101",
          "sq.f3[1:9][1:9][2:2:9],sq.f2[0][0]&g.a[2][0][2]>=37.0&st.f2[2][2]<101",
          "st.f1[1:9],sq.f0&g.m3[9]={101,101,101,101}",
          "st.f3[1:9][1:9][0]&g.m1[0]<{101,101,101,101}&sq.f2[2][9]=101",
          "st.f0",
          "v0,sq.f0&g.m3[0]<=101&sq.f3[9][2][9]>=37.0",
          "v3&g.m2[0]>=101&v2[9][0]={101,g.m3[2],101}",
          "v3&sq.f3[2][0][2]<37.0&v1[0]<st.f0",
          "v2[0][0],st.f3[2:2:9][1:9][0]&g.m2[2]>={101,101,101,101}&v0<101",
          "st.f1[1:9],sq.f2[0][2:2:9]&st.f2[2][0]=101&sq.f0<={101,101,101}",
          "v1[0],st.f1[0]",
          "v2[2:2:9][1:9],st.f3[2:2:9][1:9][0]&sq.f1[9]<101&g.m1[2]<=101",
          "st.f1[1:9],g.m3[1:9]&v3[0][2][9]=101",
          "v3[0][2:2:9][1:9]",
          "g",
          "v2&g.m1[0]=g.m3[0]&v0!=101",
          "st&sq.f0>{101,101,101}&g.m2[9]>={101,101,101,g.m3[2]}",
          "sq.f1[1:9],sq.f0",
          "st.f0&v0<101",
          "v2[2:2:9][2:2:9],g.m3[1:9]",
          "st.f1[0]",
          "st.f1[0]",
          "st.f0,sq&st.f3[2][2][2]<=101",
          "st,sq&v2[9][2]<=101&v3[0][2][0]<={101,101}",
          "g.m2[0],g.m1[1:9]&g.m3[2]<101",
          "sq.f0,g.a[1:9][2:2:9][2:2:9]&st.f3[9][9][2]>=g.m3[9]",
          "v2[2:2:9][0]&st.f3[9][2][9]>{101,g.m3[9],101}&sq.f3[2][2][9]<=37.0",
          "v0&sq.f1[2]>=101",
          "st&g.m2[2]!=101&g.a[0][2][2]={37.0,37.0}",
          "sq.f3[0][1:9][1:9]",
          "sq.f2[0][0]&v3[9][9][9]<=101",
          "&v0<101&st.f1[0]=101",
          "st.f0&st.f2[2][9]=101&sq.f0=101",
          "sq,g.m2[0]&g.a[9][9][0]=37.0",
          "st.f1[2:2:9],sq.f2[1:9][2:2:9]&sq.f0={101,101}",
          "v2[0][0],sq.f2[1:9][0]&st.f1[2]<=101&sq.f0!=101",
          "st",
          "sq.f0",
          "v1[0],sq&st.f2[9][2]!={101,101,101}",
          "v0",
          "v0&sq.f3[9][9][0]<g.a[0][2][2]&st.f3[0][0][0]>={101,101,101}",
          "sq&st.f3[9][2][2]>=101&v0>101",
          "sq.f1[1:9]&v1[9]>{\"string\",\"string\"}&g.m2[9]<=101",
          "g.m3[0]&v1[9]!=\"string\"",
          "g.a[1:9][0][0]&sq.f3[2][9][0]>37.0",
          "v1[0],g.m2[1:9]",
          "g.a[2:2:9][0][0]&g.m2[2]>={101,101,101}",
          "st&v0>=g.m3[0]&sq.f0!=101",
          "v2[2:2:9][0]",
          "v1&sq.f3[9][9][0]<=37.0",
          "sq&g.m1[9]>101&v3[0][2][9]>=101",
          "sq&st.f3[0][9][9]>101&st.f1[2]<101",
          "v1[1:9],g.m3[0]&st.f1[2]<=101",
          "g&sq.f0=101",
          "st.f2[2:2:9][1:9]",
          "v3[1:9][1:9][1:9],sq.f1[0]",
          "v3[0][1:9][1:9],g.m2[2:2:9]",
          "sq.f1[0]&g.m1[9]<{101,101,101}&st.f0>=\"string\"",
          "g.m2[2:2:9]",
          "v1[0],sq.f3[2:2:9][2:2:9][1:9]&st.f1[0]=101&st.f2[2][9]>=101",
          "g.a[1:9][0][2:2:9]",
          "g&v0<sq.f2[0][0]",
          "sq.f3[0][0][0],g.a[1:9][1:9][1:9]",
          "st.f3[1:9][2:2:9][2:2:9]",
          "st.f2[1:9][0]",
          "st,g&v3[2][0][9]<101&sq.f0>{101,101,101,101}",
          "st.f0,sq.f3[1:9][2:2:9][1:9]&g.m3[2]<=g.m2[0]&sq.f1[2]>={101,101}",
          "g",
          "v0&sq.f2[9][2]<101",
          "st.f1[0],g.m1[1:9]&st.f0>=\"string\"&v3[9][9][9]<101",
          "v3[0][1:9][0],sq.f2[1:9][0]",
          "v2[0][0],v1[0]",
          "sq.f3[1:9][1:9][1:9]&sq.f2[2][2]>=101&g.m2[9]>g.m3[9]",
          "1-hour[0:1:0][0:1:0][0:1:0]"
  };


  static final String[] expected = {
          "v1[0:9]&g.a[0][0][2]={sq.f3[2][2][0],37.0}&st.f1[0]!=101",
          "v2[2:2:9][0],g.a[2:2:9][1:9][2:2:9],g",
          "v2[1:9][1:9]&st.f1[2]<=101&v1[0]>=\"string\"",
          "st.f3[1:9][1:9][1:9],st&g.m1[0]<=101",
          "v3[2:2:9][0][2:2:9],g.m3[0],g&st.f0>\"string\"",
          "st.f0,st",
          "g.m3[0:9],g.m2[0:9],g.m1[0:9],g.a[0:9][0:9][0:9],g",
          "sq.f2[0][0],sq&v1[2]<=\"string\"&g.a[0][2][2]<=37.0",
          "sq.f2[1:9][0],sq&g.a[0][2][2]<{37.0,37.0}&g.m2[9]>101",
          "sq.f3[2:2:9][0][0],sq.f0,sq&v2[9][9]=101&g.a[2][9][2]>=37.0",
          "v3[0:9][0:9][0:9],v2[0:9][0:9],v1[0:9],v0,st.f3[0:9][0:9][0:9],st.f2[0:9][0:9],st.f1[0:9],st.f0,st,sq.f3[2][2][2],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq,g.m3[0:9],g.m2[0:9],g.m1[0:9],g.a[0:9][0:9][0:9],g&sq.f3[2][2][2]=37.0",
          "v0",
          "v3[0:9][0:9][0:9],v2[0:9][0:9],v1[0:9],v0,st.f3[0:9][0:9][0:9],st.f2[2][2],st.f1[0:9],st.f0,st,sq.f3[0:9][0:9][0:9],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq,g.m3[0],g.m2[0:9],g.m1[0:9],g.a[0:9][0:9][0:9],g&g.m3[0]<=101&st.f2[2][2]<=101",
          "sq.f2[0][0],sq.f1[0],sq&g.m2[0]!={101,101,101,101}&g.m3[0]={101,101,g.m1[9]}",
          "v0",
          "st.f2[1:9][0],st.f0,st&st.f3[2][9][0]!=101&sq.f1[2]>=101",
          "v2[0:9][0:9]&g.m1[2]<101&v1[2]>{\"string\",\"string\",\"string\"}",
          "sq.f1[1:9],sq",
          "v2[2:2:9][1:9]",
          "g.a[1:9][0][0],g&g.m3[2]<=101&st.f0>=\"string\"",
          "sq.f3[1:9][2:2:9][0],sq&st.f2[2][9]<101&sq.f1[2]<101",
          "sq.f3[1:9][1:9][1:9],sq&v2[0][0]!={101,101}&g.m1[2]>101",
          "st.f3[0:9][0:9][0:9],st.f2[0:9][0:9],st.f1[0:9],st.f0,st&g.m3[0]!=101",
          "sq.f2[2:2:9][1:9],sq&v2[2][9]=101",
          "st.f3[0:9][0:9][0:9],st.f2[0:9][0:9],st.f1[0:9],st.f0,st",
          "v1[2:2:9],sq.f3[0:9][0:9][0:9],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq",
          "v0,st.f0,st&st.f2[2][2]>=g.m3[2]&g.m1[0]<=101",
          "v0&sq.f2[9][9]>=101",
          "sq.f3[1:9][1:9][2:2:9],sq.f2[0][0],sq&g.a[2][0][2]>=37.0&st.f2[2][2]<101",
          "st.f1[1:9],st,sq.f0,sq&g.m3[9]={101,101,101,101}",
          "st.f3[1:9][1:9][0],st&g.m1[0]<{101,101,101,101}&sq.f2[2][9]=101",
          "st.f0,st",
          "v0,sq.f0,sq&g.m3[0]<=101&sq.f3[9][2][9]>=37.0",
          "v3[0:9][0:9][0:9]&g.m2[0]>=101&v2[9][0]={101,g.m3[2],101}",
          "v3[0:9][0:9][0:9]&sq.f3[2][0][2]<37.0&v1[0]<st.f0",
          "v2[0][0],st.f3[2:2:9][1:9][0],st&g.m2[2]>={101,101,101,101}&v0<101",
          "st.f1[1:9],st,sq.f2[0][2:2:9],sq&st.f2[2][0]=101&sq.f0<={101,101,101}",
          "v1[0],st.f1[0],st",
          "v2[2:2:9][1:9],st.f3[2:2:9][1:9][0],st&sq.f1[9]<101&g.m1[2]<=101",
          "st.f1[1:9],st,g.m3[1:9],g&v3[0][2][9]=101",
          "v3[0][2:2:9][1:9]",
          "g.m3[0:9],g.m2[0:9],g.m1[0:9],g.a[0:9][0:9][0:9],g",
          "v2[0:9][0:9]&g.m1[0]=g.m3[0]&v0!=101",
          "st.f3[0:9][0:9][0:9],st.f2[0:9][0:9],st.f1[0:9],st.f0,st&sq.f0>{101,101,101}&g.m2[9]>={101,101,101,g.m3[2]}",
          "sq.f1[1:9],sq.f0,sq",
          "st.f0,st&v0<101",
          "v2[2:2:9][2:2:9],g.m3[1:9],g",
          "st.f1[0],st",
          "st.f1[0],st",
          "st.f0,st,sq.f3[0:9][0:9][0:9],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq&st.f3[2][2][2]<=101",
          "st.f3[0:9][0:9][0:9],st.f2[0:9][0:9],st.f1[0:9],st.f0,st,sq.f3[0:9][0:9][0:9],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq&v2[9][2]<=101&v3[0][2][0]<={101,101}",
          "g.m2[0],g.m1[1:9],g&g.m3[2]<101",
          "sq.f0,sq,g.a[1:9][2:2:9][2:2:9],g&st.f3[9][9][2]>=g.m3[9]",
          "v2[2:2:9][0]&st.f3[9][2][9]>{101,g.m3[9],101}&sq.f3[2][2][9]<=37.0",
          "v0&sq.f1[2]>=101",
          "st.f3[0:9][0:9][0:9],st.f2[0:9][0:9],st.f1[0:9],st.f0,st&g.m2[2]!=101&g.a[0][2][2]={37.0,37.0}",
          "sq.f3[0][1:9][1:9],sq",
          "sq.f2[0][0],sq&v3[9][9][9]<=101",
          "v3[0:9][0:9][0:9],v2[0:9][0:9],v1[0:9],v0,st.f3[0:9][0:9][0:9],st.f2[0:9][0:9],st.f1[0],st.f0,st,sq.f3[0:9][0:9][0:9],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq,g.m3[0:9],g.m2[0:9],g.m1[0:9],g.a[0:9][0:9][0:9],g&v0<101&st.f1[0]=101",
          "st.f0,st&st.f2[2][9]=101&sq.f0=101",
          "sq.f3[0:9][0:9][0:9],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq,g.m2[0],g&g.a[9][9][0]=37.0",
          "st.f1[2:2:9],st,sq.f2[1:9][2:2:9],sq&sq.f0={101,101}",
          "v2[0][0],sq.f2[1:9][0],sq&st.f1[2]<=101&sq.f0!=101",
          "st.f3[0:9][0:9][0:9],st.f2[0:9][0:9],st.f1[0:9],st.f0,st",
          "sq.f0,sq",
          "v1[0],sq.f3[0:9][0:9][0:9],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq&st.f2[9][2]!={101,101,101}",
          "v0",
          "v0&sq.f3[9][9][0]<g.a[0][2][2]&st.f3[0][0][0]>={101,101,101}",
          "sq.f3[0:9][0:9][0:9],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq&st.f3[9][2][2]>=101&v0>101",
          "sq.f1[1:9],sq&v1[9]>{\"string\",\"string\"}&g.m2[9]<=101",
          "g.m3[0],g&v1[9]!=\"string\"",
          "g.a[1:9][0][0],g&sq.f3[2][9][0]>37.0",
          "v1[0],g.m2[1:9],g",
          "g.a[2:2:9][0][0],g&g.m2[2]>={101,101,101}",
          "st.f3[0:9][0:9][0:9],st.f2[0:9][0:9],st.f1[0:9],st.f0,st&v0>=g.m3[0]&sq.f0!=101",
          "v2[2:2:9][0]",
          "v1[0:9]&sq.f3[9][9][0]<=37.0",
          "sq.f3[0:9][0:9][0:9],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq&g.m1[9]>101&v3[0][2][9]>=101",
          "sq.f3[0:9][0:9][0:9],sq.f2[0:9][0:9],sq.f1[0:9],sq.f0,sq&st.f3[0][9][9]>101&st.f1[2]<101",
          "v1[1:9],g.m3[0],g&st.f1[2]<=101",
          "g.m3[0:9],g.m2[0:9],g.m1[0:9],g.a[0:9][0:9][0:9],g&sq.f0=101",
          "st.f2[2:2:9][1:9],st",
          "v3[1:9][1:9][1:9],sq.f1[0],sq",
          "v3[0][1:9][1:9],g.m2[2:2:9],g",
          "sq.f1[0],sq&g.m1[9]<{101,101,101}&st.f0>=\"string\"",
          "g.m2[2:2:9],g",
          "v1[0],sq.f3[2:2:9][2:2:9][1:9],sq&st.f1[0]=101&st.f2[2][9]>=101",
          "g.a[1:9][0][2:2:9],g",
          "g.m3[0:9],g.m2[0:9],g.m1[0:9],g.a[0:9][0:9][0:9],g&v0<sq.f2[0][0]",
          "sq.f3[0][0][0],sq,g.a[1:9][1:9][1:9],g",
          "st.f3[1:9][2:2:9][2:2:9],st",
          "st.f2[1:9][0],st",
          "st.f3[0:9][0:9][0:9],st.f2[0:9][0:9],st.f1[0:9],st.f0,st,g.m3[0:9],g.m2[0:9],g.m1[0:9],g.a[0:9][0:9][0:9],g&v3[2][0][9]<101&sq.f0>{101,101,101,101}",
          "st.f0,st,sq.f3[1:9][2:2:9][1:9],sq&g.m3[2]<=g.m2[0]&sq.f1[2]>={101,101}",
          "g.m3[0:9],g.m2[0:9],g.m1[0:9],g.a[0:9][0:9][0:9],g",
          "v0&sq.f2[9][2]<101",
          "st.f1[0],st,g.m1[1:9],g&st.f0>=\"string\"&v3[9][9][9]<101",
          "v3[0][1:9][0],sq.f2[1:9][0],sq",
          "v2[0][0],v1[0]",
          "sq.f3[1:9][1:9][1:9],sq&sq.f2[2][2]>=101&g.m2[9]>g.m3[9]",
          "1-hour[0:1:0][0:1:0][0:1:0]"
  };

  //////////////////////////////////////////////////

  static final String TITLE = "DAP Constraint Parser Tests";

  static final String testDDS =
          "Dataset {\n"
                  + "int32 v0;\n"
                  + "String v1[10];\n"
                  + "int32 v2[10][10];\n"
                  + "int32 v3[10][10][10];\n"
                  + "Structure {\n"
                  + "String f0;\n"
                  + "int32 f1[10];\n"
                  + "int32 f2[10][10];\n"
                  + "int32 f3[10][10][10];\n"
                  + "} st;\n"
                  + "Sequence {\n"
                  + "int32 f0;\n"
                  + "int32 f1[10];\n"
                  + "int32 f2[10][10];\n"
                  + "float32 f3[10][10][10];\n"
                  + "} sq;\n"
                  + "Grid {\n"
                  + "Array:\n"
                  + "float32 a[d1=10][d2=10][d3=10];\n"
                  + "Maps:\n"
                  + "int32 m1[d1=10];\n"
                  + "int32 m2[d2=10];\n"
                  + "int32 m3[d3=10];\n"
                  + "} g;\n"
                  + "int32 1-hour;\n" // test number versus name check
                  + "} TestCeParser;\n";

  //////////////////////////////////////////////////

  // Control the generation of constraint strings
  int NPROJECTIONS = 3;  // 0..NPROJECTIONS-1
  int NSELECTIONS = 3;  // 0..NSELECTIONS-1
  int MAXRHSSIZE = 4;  // 1..MAXRHSSIZE

  List<BaseType> allnodes = null;
  List<BaseType> leaves = null;
  List<BaseType> constructors = null;

  //////////////////////////////////////////////////
  // Constructors + etc.

  protected void setUp() {
  }

  //////////////////////////////////////////////////

  void collectnodesr(BaseType bt, List<BaseType> preorder) {
    try {
      // collect the preorder list of non-DArray nodes
      if (!(bt instanceof DDS || bt instanceof DArray)) {
        preorder.add(bt); // leave off the root dataset node
      }
      if (bt instanceof DArray)
        collectnodesr(((DArray) bt).getContainerVar(), preorder);
      else if (bt instanceof DConstructor) {
        int nvars = ((DConstructor) bt).getVarCount();
        for (int i = 0; i < nvars; i++)
          collectnodesr(((DConstructor) bt).getVar(i), preorder);
      }
    } catch (NoSuchVariableException nsve) {
    }
  }

  void collectnodes(DDS dds) {
    allnodes = new ArrayList<BaseType>();
    leaves = new ArrayList<BaseType>();
    constructors = new ArrayList<BaseType>();
    collectnodesr(dds, allnodes);
    // Separate out leaves from constructors, keeping order
    for (int i = 0; i < allnodes.size(); i++) {
      BaseType bt = allnodes.get(i);
      if (bt instanceof DConstructor)
        constructors.add(bt);
      else
        leaves.add(bt);
    }
  }

  List<BaseType> getPath(BaseType bt) {
    assert (bt != null);
    List<BaseType> path = new ArrayList<BaseType>();
    BaseType parent = bt;
    while (parent != null) {
      path.add(0, parent);
      parent = (BaseType) parent.getParent();
      if (parent != null && parent instanceof DArray)
        parent = (BaseType) parent.getParent();
    }
    return path;
  }

  BaseType getTrueParent(BaseType bt) {
    if (bt.getParent() instanceof DArray)
      return (BaseType) bt.getParent().getParent();
    return (BaseType) bt.getParent();
  }

  // Sort a list of BaseType by getLongName()

  List<BaseType> sort(List<BaseType> list) {
    List<BaseType> sorted = new ArrayList<BaseType>();
    while (list.size() > 0) {
      BaseType bt0 = list.remove(0);
      String name = bt0.getLongName();
      int pos = sorted.size();
      for (int i = 0; i < sorted.size(); i++) {
        BaseType bt1 = sorted.get(i);
        if (name.compareTo(bt1.getLongName()) > 0) {
          pos = i;
          break;
        }
      }
      sorted.add(pos, bt0);
    }
    return sorted;
  }

  // return true if the ctor is marked
  // which means that all children are marked

  boolean ctormarked(BaseType bt) {
    if (!(bt instanceof DConstructor)) return false;
    try {

      DConstructor ctor = (DConstructor) bt;
      for (int i = 0; i < ctor.getVarCount(); i++) {
        BaseType var = ctor.getVar(i);
        if (!((ServerMethods) var).isProject()) return false;
      }

    } catch (NoSuchVariableException nsve) {
      return false;
    }
    return true;
  }

  // Return true if the variable is whole
  boolean iswholevariable(BaseType bt) {
    DArray a = null;
    if (bt instanceof DArray)
      a = (DArray) bt;
    else {
      if (!(bt.getParent() instanceof DArray)) return false;
      a = (DArray) bt.getParent();
    }
    for (Enumeration e = a.getDimensions(); e.hasMoreElements(); ) {
      DArrayDimension dim = (DArrayDimension) e.nextElement();
      // test if this is a whole dimension slice
      if (dim.getStride() != 1
              || dim.getStart() != 0
              || dim.getStop() != (dim.getSize() - 1)) return false;
    }
    return true;
  }

  boolean isprimitive(BaseType bt) {
    return !(bt instanceof DConstructor)
            || bt instanceof DArray;
  }

  //////////////////////////////////////////////////
  // Dump the contents of the CEEvaluator object

  void dumpPath(List<BaseType> path, PrintWriter os) {
    for (int i = 0; i < path.size(); i++) {
      BaseType bt = path.get(i);
      if (i > 0) os.print('.');
      os.print(bt.getEncodedName());
      if (bt.getParent() instanceof SDArray
//		&& !iswholevariable(bt.getParent())
              ) {
        SDArray sa = (SDArray) bt.getParent();
        if (!sa.isProject()) continue;
        for (int j = 0; j < sa.numDimensions(); j++) {
          dumpDimension(sa, j, os);
        }
      }
      if (bt.getParent() instanceof SDGrid) {
        SDGrid sg = (SDGrid) bt.getParent();
      }
    }
  }

  void dumpDimension(SDArray var, int i, PrintWriter os) {
    os.print("[");

    try {

      os.print(var.getStart(i));

      if (var.getStride(i) != 1) {
        os.print(":");
        os.print(var.getStride(i));
      }
      if (var.getStride(i) != 1 || var.getStop(i) != var.getStart(i)) {
        os.print(":");
        os.print(var.getStop(i));
      }

    } catch (InvalidDimensionException ipe) {
      os.print('?');
    }

    os.print("]");
  }

  void dumpEvaluator(CEEvaluator cev, PrintWriter os) {
    // Get the variables and grids in the DDS and print their projections
    List<BaseType> dumplist = new ArrayList<BaseType>();
    for (int i = 0; i < allnodes.size(); i++) {
      BaseType node = allnodes.get(i);
      if (!((ServerMethods) node).isProject()) continue;
//	    // suppress all children of marked ctor
//	    if(ctormarked(getTrueParent(node))) continue;
//	    // suppress all non-marked ctors
//	    if(node instanceof DConstructor && !ctormarked(node)) continue;
      dumplist.add(node);
    }
    // Sort dumplist
    dumplist = sort(dumplist);
    // Dump dumplist
    for (int i = 0; i < dumplist.size(); i++) {
      BaseType node = dumplist.get(i);
      if (i > 0) os.print(",");
      List<BaseType> path = getPath((BaseType) node);
      node.printConstraint(os);
      //dumpPath(path,os);
    }
    Enumeration ec = cev.getClauses();
    while (ec.hasMoreElements()) {
      Clause c = (Clause) ec.nextElement();
      os.print('&');
      c.printConstraint(os);
    }
  }

  //////////////////////////////////////////////////

  @Test
  public void testCeParser() throws Exception {
    boolean pass = true;
    String[] constraintlist = (xconstraints.length > 0 ? xconstraints : constraints);
    String[] expectlist = (xconstraints.length > 0 ? xexpected : expected);
    int nconstraints = constraintlist.length;
    for (int i = 0; i < nconstraints; i++) {
      // Parse the DDS to produce a ServerDDS object
      ServerDDS sdds = new ServerDDS(new test_SDFactory());
      StringBufferInputStream teststream = new StringBufferInputStream(testDDS);
      if (!sdds.parse(teststream))
        throw new ParseException("Cannot parse DDS");
      collectnodes(sdds);
      pass = false;
      String constraint = constraintlist[i];
      String expected = (generate ? null : expectlist[i]);
      System.out.flush();
      try {
	    CeParser.DEBUG = DEBUGPARSER;
        CEEvaluator ceEval = new CEEvaluator(sdds);
        ceEval.parseConstraint(constraint, null);
        StringWriter ss = new StringWriter();
        PrintWriter os = new PrintWriter(ss);
        dumpEvaluator(ceEval, os);
        String result = ss.toString();
        if (generate) {
          System.out.println(result);
        } else {
          pass = result.equals(expected);
          System.out.println("Testing constraint " + i + ": " + constraint);
          System.out.println("constraint:" + constraint);
          System.out.println("expected:  " + expected);
          System.out.println("result:    " + result);
          if (!pass) {
            System.out.println("***Fail: " + constraint);
          } else
            System.out.println("***Pass:" + constraint);
        }
      } catch (Exception e) {
        System.out.println("FAIL: TestCeParser: " + e.toString());
      }
      if (!generate && !pass) break;
    }
    Assert.assertTrue("TestCeParser", pass || generate);
  }

}

