
DAP4TARGET=../d4core/src/main/java/dap4/core/dmr/parser/Dap4ParserBody.java
CETARGET=../d4servletshared/src/main/java/dap4/ce/parser/CEParserBody.java

.PHONEY: bison

all: bison

# Requires bison 3.0 or later
bison:
	bison  -v dap4.y -o Dap4ParserBody.java
	bison  -v ce.y -o CEParserBody.java
	if test -f ${DAP4TARGET} ; then rm ${DAP4TARGET}; fi
	if test -f ${CETARGET} ; then rm ${CETARGET}; fi
	cp Dap4ParserBody.java ${DAP4TARGET}
	cp CEParserBody.java ${CETARGET}


clean::
	rm -fr Dap4ParserBody.java Dap4ParserBody.output




