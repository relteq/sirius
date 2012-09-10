clear
close all

maxsize = 0.1;  %  miles

addpath([pwd filesep '../utils/xml_io_tools_2007_07'])
addpath([pwd filesep '../utils'])

original = [pwd filesep 'test_singlelink_attached.xml'];

% read node list and directions cache
disp('Reading configuration file')
scenario = xml_read(original);
