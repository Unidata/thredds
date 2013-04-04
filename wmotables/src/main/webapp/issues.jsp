<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
    <html>
     <head>
   <title><spring:message code="global.title"/>: BUFR Decoding Issues</title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
   <h3>BUFR Decoding Issues</h3>
   <h4>1. Errors in the <a href="http://www.wmo.int/pages/prog/www/WMOCodes/Operational/BUFR/BufrTabB-11-2007.pdf">WMO Master Table  version 13</a></h4>
   <h5><strong>0-22-39</strong> </h5>
      <p>has bit width 13, apparently it should be bit width 12. </p>
   <h5><strong>0-13-044</strong></h5>
   <p> the units should be C, not K.&nbsp; This was   intended to be corrected as an editorial change a couple of years ago,   but it looks like the change never made it into the actual WMO tables.&nbsp;   See paragraph 3.3.5 (and the corresponding annex) in <a href="http://www.wmo.int/pages/prog/www/ISS/Reports/ET-DRC_EUMETSAT2007.doc">http://www.wmo.int/pages/prog/www/ISS/Reports/ET-DRC_EUMETSAT2007.doc</a>&nbsp;(Jeff Ator at NCEP).</p>
   <h5>3 02 044</h5>
   <pre>3 02 044 (Evaporation data)  
   0 04 024 Time period in hours
   0 02 004 Type of instrument for evaporation or crop type for evapotranspiration
   0 13 003 Relative humidity 
   </pre>
   <p> however it possibly should be: </p>
   <pre>
   0 04 024 Time period in hours
   0 02 004 Type of instrument for evaporation or crop type for evapotranspiration
   <strong>0 13 033 Evaporation /evapotranspiration</strong></pre>
   <hr width="100%" />
   <h4>2. Use of BUFR Tables </h4>
   <h5>1. Use of <strong>standard WMO data descriptor ranges in local tables </strong></h5>
   <p>For both table B and table D, the areas x &lt; 48, and y &lt; 192 are reserved for  standard WMO tables. We sometimes see local tables using descriptors in this range. We assume that this is incorrect, and BUFR records that rely on this kind of <em>local override</em> will fail, at least in the CDM BUFR library, unless  override tables are explicitly configured.</p>
   <p>Its appears that national centers sometimes use non-allocated standard data descriptors, probably simultaneously proposing them as standard entries. However, there is no mechanism for recognizing these elements until they are added to the standard table, and if the FXY has to change due to conflicts, these messages become unuseable. </p>
   <p>We also see centers overriding WMO entries in their local tables. The problem with this practice is that a reader no longer can count on having correct entries, even for those in the standard WMO area. Thus reading any message involves contacting the data producer to get their tables.</p>
   <h5>2.  Incorrect use of master table version numbers </h5>
   <p>We sometimes see BUFR records where the  version number of the master table is incorrectly specified, typically assuming elements from a later version of the master table. We assume that all versions of the master table are backwards compatible up to version 13. Therefore, we use version 13 table no matter what the BUFR record says, and so we tolerate the incorrect encoding of the master table version.</p>
   <p>Starting with version 14, master tables will no longer be backwards compatible, therefore data providers must correctly encode the master table version for versions 14 and greater.</p>
   <hr width="100%" />
   <h4>3. BUFR Record encoding ambiguities</h4>
   <h5>1.Compression with delayed replication</h5>
   <p>From <a href="http://www.wmo.int/pages/prog/www/WMOCodes/Operational/BUFR/FM94REG-11-2007.pdf">FM94REG-11-2007.pdf</a>:  </p>
   <p>&quot;The binary data in compressed form may be described as follows:</p>
   <pre>
   Ro1, NBINC1, I11, I12, . . . I1n
   Ro2, NBINC2, I21, I22, . . . I2n
   ... 
   Ros, NBINCs, Is1, Is2, . . . Isn
   </pre>
   <p>  where Ro1, Ro2, . . . Ros are local reference values for the set of values for each data element (number of bits as Table B). NBINC1 . . . NBINCs contain, as 6-bit quantities, the number of bits occupied by the increments (I11 . . . I1n) . . . (Is1 . . . Isn). s is the number of data elements per data subset and n is the number of data subsets per BUFR message.&quot;    </p>
   <p>What happens if we combine compression with replication?</p>
   <p>Let C be the entire compressed block for one dataset, as above. Then regular replication just repeats C for each dataset in the replication:</p>
   <pre>  C1, C2,... Cm</pre>
   <p>One might guess that delayed replication would look just like regular replication preceded by the replication count, that is:</p>
   <pre>  m, C1, C2,... Cm</pre>
   <p>where m is the replication count using the number of bits specified in the replication descriptor element. However, for some BUFR records, I am seeing an extra 6 bits, that is:</p>
   <pre>  m, x, C1, C2,... Cm</pre>
   <p>Where x appears to be 6 zero bits. </p>
   <p>There is no description of how compression with delayed replication is encoded in <a href="http://www.wmo.int/pages/prog/www/WMOCodes/Operational/BUFR/FM94REG-11-2007.pdf">FM94REG-11-2007.pdf</a>. The <a href="http://www.ecmwf.int/products/data/d/check/">ECMWF BUFR/CREX format checker</a> decodes <a href="bufrData/LNDSYN.bufr">this example of compression with delayed replication</a> correctly, so it must be assuming the extra 6 bits. Do other decoders? This should be clarified in the official documentation.</p>
   <p>Examples - possibly malformed</p>
   <ul>
   <li>IUST53.bufr, IUSV53.bufr, IUSZ53.bufr all from NTWSTG</li>
   </ul>
   <h5>2. BUFR  encoding complexities</h5>
   <p>There are  optional features that can be used to encode BUFR messages that make decoding much more complex. We suggest avoiding these features if you are writing messages that are intended to be decoded by others' software, and especially for messages that may be part of a long-term archive. Alternative encoding methods for this kind of information are available.</p>
   <ul>
    <li>Associated Fields (2 04 Y)</li>
    <li>Data Not Present (2 21 Y)</li>
    <li>Quality Assessment Information (2 22000 through 2 37 255)</li>
    <li>The use of Coordinates (described in 3.1.2.2 of user guide) should be as simple as possible. Avoid Coordinate increments (described in 3.1.2.3 and 3.2.2.3 of user guide).</li>
   </ul>
   <hr />
   <h4>4. Errors in Code / Flag tables</h4>
   <h5>1. Possible error in code flag table 0-21-076 (12/18/2009)</h5>
   <p>From <a href="http://www.wmo.int/pages/prog/www/WMOCodes/CodeFlagTables_112009.doc">http://www.wmo.int/pages/prog/www/WMOCodes/CodeFlagTables_112009.doc</a> :</p>
   <p><strong>0  21 076</strong></p>
   <p><strong><em>Representation  of intensities</em></strong><strong><u> </u></strong></p>

  <table width="351" border="2" cellpadding="0" cellspacing="0">
    <tr>
      <td width="102" valign="top">   <p align="center">Code figure</p></td>
      <td width="241" valign="top">   <p>&nbsp;</p></td>
    </tr>
    <tr>
      <td width="102" valign="top">   <p align="center">1</p></td>
      <td width="241" valign="top">   <p>Linear</p></td>
    </tr>
    <tr>
      <td width="102" valign="top">   <p align="center">2</p></td>
      <td width="241" valign="top">   <p>Logarithmic (base e)</p></td>
    </tr>
    <tr>
      <td width="102" valign="top">   <p align="center">3</p></td>
      <td width="241" valign="top">   <p>Logarithmic (base 10)</p></td>
    </tr>
    <tr>
      <td width="102" valign="top">   <p align="center">3-6</p></td>
      <td width="241" valign="top">   <p>Reserved</p></td>
    </tr>
    <tr>
      <td width="102" valign="top">   <p align="center">7</p></td>
      <td width="241" valign="top">   <p>Missing value</p></td>
    </tr>
  </table>
   <p>But Atsushi Shimazaki wrote:</p>
   <p>In WMO publication it starts from 0 to 2 for the three entries and 3-6 are reserved and 7 is for missing value. I have checked some supplements to the Manual, and confirmed it.</p>
   <blockquote cite="mid:4B2B54870200008D0005E1A6@gateway.wmo.int" type="cite">
     <pre>3072,021076,0,Linear,,
3073,021076,1,Logarithmic (base e),,
3074,021076,2,Logarithmic (base 10),,
3075,021076,3-6,Reserved,,
3076,021076,7,Missing value,,</pre>
   </blockquote>
   <p>0 21 076 is included implicitly or explicitly in 3 12 019, 3 12 020, 3 12 024 and 3 12 025 for wind scatterometer products.<br />
  <br />
  I have one example message that uses 0 21 076 (attached both <a href="bufrData/IOWA55.bufr">binary</a> and an <a href="bufrData/IOWA55.asc">ascii</a> dump). Hard to tell what the right value is.<br />
  <br />
  Representation of intensities =2 CodeTable 0-21-76.<br />
</p>
<blockquote cite="mid:4B2B54870200008D0005E1A6@gateway.wmo.int" type="cite">&nbsp;</blockquote>
   <hr width="100%" />
   <h4>5. Errors in Local Tables</h4>
   <h5>NCEP MEL-BUFR</h5>
<ul>
  <li> Table B element 0-8-79 encoded into the G-Airmet BUFR message: B3M-000-012-B has width 3, standard WMO table has width 4. Corrected Jan, 2010 by upgrading those messages to version 13 and fixing the width in that version.</li>
</ul>
   <h5>ECMWF</h5>
<ul>
  <li></li>
</ul>
   <hr width="100%" />
   <h4><strong>Differences between WMO and ECMWF table D</strong></h4>
   <p>WMO table D is extracted from <a href="http://www.wmo.int/pages/prog/www/WMOCodes/Operational/BUFR/BufrTabD-11-2007.doc">BufrTabD-11-2007.doc</a><br />
  ECMWF table D taken from <a href="http://www.ecmwf.int/products/data/software/download/software_files/bufr_000360.tar.gz">BUFRDC download</a> , file D0000000000098013001.TXT</p>
   <hr />
<strong><br />
</strong>
   <pre><strong>3-1-34 </strong>
 WMO:    [0-1-5, 0-2-1, 3-1-11, 3-1-12, 3-1-23]
 ECMWF:  [0-1-5, 0-2-1, 3-1-11, 3-1-12, 3-1-23, 0-1-5, 0-1-12, 0-1-13, 0-2-1, 3-1-11, 3-1-12, 3-1-23]
 </pre>
   <hr />
   <p><br />
    <strong>3 02 044</strong> (Evaporation data)</p>
   <pre>   0 04 024 Time period in hours
   0 02 004 Type of instrument for evaporation
   <strong>0 13 003</strong> Evaporation /evapotranspiration

ECMWF

 302044  3 004024<br />           002004<br />           <strong>013033</strong></pre>
   <p> But given that:<br />
</p>
   <pre>
0 13 003 Relative humidity %                   0 0 7  %      0 3
0 13 033 Evaporation/evapotranspiration kg m&ndash;2 1 0 10 kg m-2 1 4<br />
</pre>
   <p>Its most likely that 33 is correct, and the WMO table is wrong. <a href="bufrData/LNDSYN.bufr">Here</a> is a message that uses this element, and it only parses correctly if 33 is used.</p>
   <hr />
   <pre><strong>3-2-56 </strong>
   WMO:     [0-2-38, 0-7-63, 0-22-43, 0-7-63]
   ECMWF:   [0-2-38, 0-22-43]
 </pre>
   <hr />
   <p>WMO</p>
   <p><strong>3 07 084</strong> (Sequence for representation of synoptic reports from a fixed land station suitable for SYNOP data in compliance with reporting practices in RA IV)</p>
   <pre>   3 01 090 Fixed surface station identification, time, horizontal and vertical coordinates
   3 02 031 Pressure data
   3 02 035 Basic synoptic &ldquo;instantaneous&rdquo; data
   3 02 036 Clouds with bases below station level
   3 02 047 Direction of cloud drift
   0 08 002 Vertical significance (= missing to cancel the previous value)
   3 02 048 Direction and elevation of cloud
   3 02 037 State of ground, snow depth, ground minimum temperature
   0 20 055 State of sky in tropics
   1 01 000 Delayed replication of 1 descriptor
   <strong>0 31 001</strong> Delayed descriptor replication factor
   2 05 001 Character field of 1 character
   3 02 043 Basic synoptic &ldquo;period&rdquo; data
   3 02 044 Evaporation data
   1 01 002 Replicate next descriptor 2 times
   3 02 045 Radiation data (from 1 hour and/or 24 hour period)
   3 02 046 Temperature change


ECMWF

307084 17 301090 BUFR template for synoptic reports RA IV<br />           302031<br />           302035<br />           302036<br />           302047<br />           008002<br />           302048<br />           302037<br />           020055<br />           101000<br />           <strong>031000</strong><br />           205001<br />           302043<br />           302044<br />           101002<br />           302045<br />           302046<br /> 
1 01 000 says its a delayed replicaton of 1 field

0 31 001 says the number of replications is encoded in an 8-bit field
0 31 000 is just a way to indicate missing data

2 05 001 1 char is optionally inserted


either one might be correct
</pre>
   <hr />
   <pre>

<strong>3-12-6</strong><br />WMO   <br />[0-25-60, 0-25-62, 0-40-1, 0-40-2, 0-21-62, 0-21-151, 0-21-152, 0-21-153, 0-21-154, 0-21-62, <strong>0-21-88</strong>, 0-40-3, 0-40-4, 0-40-5, 0-40-6, 0-40-7, 0-20-65, 0-40-8, 0-40-9, 0-40-10]<br />ECMWF<br />[0-25-60, 0-25-62, 0-40-1, 0-40-2, 0-21-62, 0-21-151, 0-21-152, 0-21-153, 0-21-154, 0-21-62, <strong>0-21-62</strong>, 0-40-3, 0-40-4, 0-40-5, 0-40-6, 0-40-7, 0-20-65, 0-40-8, 0-40-9, 0-40-10]<br />0 21 088 Wet backscatter dB 2 -5000 13 dB 2 4
0 21 062 Backscatter dB     2 &ndash;5000 13 dB 2 4
<br />
name only change<br />
</pre>
   
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
