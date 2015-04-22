/* This is part of the netCDF package. Copyright 2008 University
   Corporation for Atmospheric Research/Unidata See COPYRIGHT file for
   conditions of use. See www.unidata.ucar.edu for more info.

   Create a test file with default fill values for variables of each type.

   $Id: tst_fills.c,v 1.12 2009/03/17 01:22:42 ed Exp $
*/

#include <stdlib.h>
#include <stdio.h>
#include <netcdf.h>

#define FILE_NAME "tst_fills.nc" 
#define VAR1 "uv8"
#  define TYPE1 NC_UBYTE
#  define DATA1 240
#define VAR2 "v16"
#  define TYPE2 NC_SHORT
#  define DATA2 32700
#define VAR3 "uv32"
#  define TYPE3 NC_INT
#  define FILL3 17
#  define DATA3 111000

#define ERR abort()


int
main(int argc, char **argv) 
{
    int ncid;
    int varid1, varid2, varid3;
    int fill3 = FILL3;

    printf("\n*** Testing fill values.\n");

    if (nc_create(FILE_NAME, NC_NETCDF4, &ncid)) ERR;
    if (nc_def_var(ncid, VAR1, TYPE1, 0, NULL, &varid1)) ERR;
    if (nc_def_var(ncid, VAR2, TYPE2, 0, NULL, &varid2)) ERR;
    if (nc_def_var(ncid, VAR3, TYPE3, 0, NULL, &varid3)) ERR;
      if (nc_def_var_fill(ncid, varid3, 0, &fill3)) ERR;
    
    if (nc_enddef(ncid)) ERR;    

    {unsigned char data = DATA1; if (nc_put_var(ncid,varid1,&data)) ERR;}
    {short data = DATA2; if (nc_put_var(ncid,varid2,&data)) ERR;}
    {unsigned int data = DATA3; if (nc_put_var(ncid,varid3,&data)) ERR;}
}

    
