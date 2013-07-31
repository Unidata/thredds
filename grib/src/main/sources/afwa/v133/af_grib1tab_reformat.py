

gribTable = "./wrf_param_output_V133.txt"
f = open(gribTable)
tab = f.readlines()
f.close()

parsedTab = []
for i in range(5,len(tab)):
    #reset parsed variables
    tmp = ''
    name = ""
    comment = ""
    param = ""
    unit = ""
    # get new line from table
    line = tab[i]
    # j is the index into the string
    j = 0
    if ((line != "\n") and (line[0] !="#")):
        entry = 0
        while (entry < 6):
            # variable white spaced delimited file, so read "entry" is defined
            # as consecutive non white-spaced chars
            while (line[j] != " "):
                tmp = tmp + line[j]
                j += 1
                if j == len(line):
                    break
            if entry == 1:
                name = tmp
            elif entry == 2:
                param = tmp
            elif entry == 5:
                unit = tmp
                if "\xb3" in unit:
                    # table had a superscript of 3 for a third power...replace
                    # with 3
                    unit = unit.replace("\xb3", "3")
                if "#" in  unit:
                    # table used # as number, or count, so replace with 1
                    unit = unit.replace("#","1")

            # go ahead and scan through the white space contained in the string
            while (line[j] == " "):
                j += 1
                if j == len(line):
                    break
            tmp = ""
            entry += 1

        # for the comment (third member of the list), just replace underscores
        # in var name with spaces and add the unit in square brackets
        comment = name.replace("_"," ") +" [{}]".format(unit)
        parsedTab.append([param, name, comment])

# write out the reformatted table
f = open("afwa_133.tab","w")
f.write("-1:57:1:133\n")
for pt in parsedTab:
    f.write("{}:{}:{}\n".format(pt[0], pt[1], pt[2]))
f.close()
