# Define how to build the parser(s)

.PHONEY: ce

all:: CeParser.java

clean::
	rm -f CeParser.java CeParser.output ce.y

CeParser.java:: ce.y
	bison -v -t -L java -pCe ce.y -o CeParser.java

# Process oc/dap.y to make it usable for a java parser */

#dap.y: ~/nc/oc/dap.y dap.y.hdr
#	rm ./dap.yc
#	cp ~/nc/oc/dap.y  ./dap.yc


ce.y::
	sed -e '1,/\/[*]DO NOT DELETE THIS LINE[*]\//d' \
	    -e 's|YYABORT|return YYABORT|' \
	    -e 's|YYACCEPT|return YYACCEPT|' \
		<./ce.yc >/tmp/ce.y.core
	cat ce.y.hdr /tmp/ce.y.core >ce.y


