/*
Note: The bison parser has been replaced by a DOM parser+walker.
The bison code is left in the build tree in case it is useful
elsewhere.
*.

DAP4TARGET=../d4core/src/main/java/dap4/core/dmr/parser/bison/Dap4BisonParser.java
CETARGET=../d4core/src/main/java/dap4/core/ce/parser/CEBisonParser.java

.PHONEY: bison

all: bison

# Requires bison 3.0 or later
bison:
	bison  -v dap4.y -o Dap4BisonParser.java
	bison  -v ce.y -o CEBisonParser.java
	if test -f ${DAP4TARGET} ; then rm ${DAP4TARGET}; fi
	if test -f ${CETARGET} ; then rm ${CETARGET}; fi
	cp Dap4BisonParser.java ${DAP4TARGET}
	cp CEBisonParser.java ${CETARGET}


clean::
	rm -fr Dap4BisonParser.java Dap4BisonParser.output




