clear
close all

infile = 'C:\Users\gomes\workspace\sirius-0.2\data\config\smalltest.xml';
outfile = 'C:\Users\gomes\workspace\sirius-0.2\data\config\_smalltest.xml';
schemaTranslate(infile,outfile)

infile = 'C:\Users\gomes\workspace\sirius-0.2\data\config\scenario_2009_02_12.xml';
outfile = 'C:\Users\gomes\workspace\sirius-0.2\data\config\_scenario_2009_02_12.xml';
schemaTranslate(infile,outfile)

infile = 'C:\Users\gomes\workspace\sirius-0.2\data\config\scenario_constantsplits.xml';
outfile = 'C:\Users\gomes\workspace\sirius-0.2\data\config\_scenario_constantsplits.xml';
schemaTranslate(infile,outfile)

infile = 'C:\Users\gomes\workspace\sirius-0.2\data\config\scenario_constantsplits.xml';
outfile = 'C:\Users\gomes\workspace\sirius-0.2\data\config\_scenario_constantsplits.xml';
schemaTranslate(infile,outfile)

disp('done')