
close all

doplot = false;
casedir = [pwd filesep 'freewaytest' filesep];
offset = 500;
step = 1000;

global gps2feet

gps2feet = 326395.2094273205; 
addpath([pwd filesep '../utils/xml_io_tools_2007_07'])
addpath([pwd filesep '../utils'])

onelink = [casedir 'original_network.xml'];
flooded_nodir = [casedir 'flooded_nodir.xml'];
flooded_withdir = [casedir 'flooded_withdir.xml'];
newnodes = [casedir 'newnodes.xml'];
outfile = [casedir 'outfile.xml'];


scenario = xml_read(onelink);

% 1) add a bunch of links and nodes
mainroute = nodelinkflood(scenario,offset,step,flooded_nodir);

% 
disp('load into NE, save as flooded_withdir with populated directions cache')
pause

% 3) find intersection nodes
newintersection = findintersections(mainroute,flooded_withdir,casedir,doplot);

% 4) save to config file
writeNodesAndLinksToNetwork(newintersection,[],newnodes)

% add in old boundary nodes and save
joinnewnodes(onelink,newnodes,outfile)

disp('done')

