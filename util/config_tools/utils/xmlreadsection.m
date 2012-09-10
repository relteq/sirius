function [nodeStruct]=xmlreadsection(xmlfile,sectionname)
% read given section from an xml file.

names = strread(sectionname,'%s','delimiter','>');
A = xmlread(xmlfile);
X = drillintonode(A,names);
nodeStruct = xmldom2struct(X);
