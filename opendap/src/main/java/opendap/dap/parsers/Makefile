# Define how to build the parser(s)

.PHONEY: dap ce

all:: Dap2Parser.java

clean::
	rm -f DapParser.java Dap2Parser.java

Dap2Parser.java:: dap2.y
	bison -v -t -L java -pDap2 dap2.y -o Dap2Parser.java

# Obsolete
DapParser.java:: dap.y
	bison -v -t -L java -pDap dap.y -o DapParser.java
