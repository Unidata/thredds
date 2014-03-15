<?xml version="1.0" encoding="UTF-8"?>
<assert
    xmlns="http://purl.oclc.org/dsdl/schematron"
    test="( om:result/@xlink:href | om:result/@xlink:title ) and not( om:result/* | om:result/text() )">result
    model must match gml:ReferenceType</assert>
