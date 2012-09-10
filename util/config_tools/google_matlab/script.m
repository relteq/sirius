
close all

doplot = false;

% casedir = [pwd filesep 'arterytest' filesep];
% offset = 300;
% step = 600;

casedir = [pwd filesep 'freewaytest' filesep];
offset = 500;
step = 1000;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

global gps2feet

gps2feet = 326395.2094273205; 
addpath([pwd filesep 'xml_io_tools_2007_07'])
onelink = [casedir 'onelink.xml'];
tworoutes_nodir = [casedir 'tworoutes_nodir.xml'];
tworoutes_withdir = [casedir 'tworoutes_withdir.xml'];
newnodes = [casedir 'newnodes.xml'];
newnodesmod = [casedir 'newnodes_mod.xml'];
outfile = [casedir 'outfile.xml'];

mainroute = nodelinkflood(offset,step,onelink,tworoutes_nodir);

newintersection = findintersections(mainroute,tworoutes_withdir,casedir,doplot);

writeXMLnetwork(newintersection,[],newnodes)

joinnewnodes(onelink,newnodesmod,outfile)
