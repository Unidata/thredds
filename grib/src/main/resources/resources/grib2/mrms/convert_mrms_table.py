#!/usr/bin/env python
from __future__ import print_function
import csv
from collections import namedtuple

def parse_file(fname):
    with open(fname, 'rU') as infile:
        reader = csv.reader(infile)
        ret = []
        cols = reader.next()
        cols = [c.replace(' ', '_') for c in cols]
        RowEntry = namedtuple('Row', cols)
        for row in reader:
            ret.append(RowEntry(*row))
        return ret

def fix_item(item):
    # Fix units that have AGL/MSL in them. Add to description instead
    if item.Unit.endswith(' MSL') or item.Unit.endswith(' AGL'):
        item = item._replace(Unit=item.Unit[:-4],
                Description=item.Description + item.Unit[-4:])
    elif item.Unit == 'non-dim' or item.Unit == 'flag': # Change to cf-compliant string
        item = item._replace(Unit='dimensionless')

    # Fix spelling errors for reflectivity
    if 'Reflectivty' in item.Description:
        item = item._replace(Description=item.Description.replace('Reflectivty', 'Reflectivity'))
    if 'Reflectivty' in item.Name:
        item = item._replace(Name=item.Name.replace('Reflectivty', 'Reflectivity'))

    return item

def make_java(info):
    for i in info:
        i = fix_item(i)
        print('add({0.Discipline}, {0.Category}, {0.Parameter}, "{0.Name}", '
                '"{0.Description}", "{0.Unit}", {0.No_Coverage}, '
                '{0.Missing});'.format(i))

if __name__ == '__main__':
    import argparse
    import sys

    parser = argparse.ArgumentParser(description="Convert MRMS grib table to Java code")
    parser.add_argument('fname', type=str, nargs=1, help="Source CSV file")
    args = parser.parse_args()

    if not args.fname:
        parser.show_help()
        sys.exit()

    info = parse_file(args.fname[0])
    make_java(info)
