clear
close all


addpath([pwd filesep 'geodetic295\geodetic'])

infile = 'C:\Gabriel\ICM\bayarea\101_North_1.xml';
outfolder = 'C:\Gabriel\PATH_TOPL\NetworkEditorTools\tool_NE2DynusT\Test';

scenario = xml_read(infile);

numzones = 1;
numnodes = length(scenario.network.NodeList.node);
numlinks = length(scenario.network.LinkList.link);
mystery1 = 1;
mystery2 = 0;

for i=1:numnodes
    N=scenario.network.NodeList.node(i);
    node(i).id = i;
    node(i).lat = N.position.point.ATTRIBUTE.lat;
    node(i).lng = N.position.point.ATTRIBUTE.lng;
    [n,e]=ell2utm(node(i).lat,node(i).lng);
    node(i).x = e;
    node(i).y = n;
    node(i).zone = 1;
    allnodeid(i) = N.ATTRIBUTE.id;
end

for i=1:numlinks
    L=scenario.network.LinkList.link(i);
    link(i).startnode = find(allnodeid==L.begin.ATTRIBUTE.node_id);
    link(i).endnode = find(allnodeid==L.xEnd.ATTRIBUTE.node_id);
    link(i).leftturnbays = 0;
    link(i).rightturnbays = 0;
    link(i).length = round(L.ATTRIBUTE.length*5280);
    link(i).lanes = L.ATTRIBUTE.lanes;
    link(i).trafficflowmodel = 1;
    link(i).speedadjustmentfactor = 0;
    link(i).speedlimit =  round(L.fd.ATTRIBUTE.flowMax/L.fd.ATTRIBUTE.densityCritical);
    link(i).serviceflow = L.fd.ATTRIBUTE.flowMax;
    link(i).saturationflow = L.fd.ATTRIBUTE.flowMax;
    link(i).type = 1; % FIX THIS.
    link(i).grade = 0;
end

fid = fopen([outfolder filesep 'xy.dat'],'w+');
for i=1:numnodes
    fprintf(fid,'%7d%16.3f%16.3f\n',node(i).id,node(i).x,node(i).y);
end
fclose(fid);


fid = fopen([outfolder filesep 'network.dat'],'w+');
fprintf(fid,'%7d%7d%7d%7d%7d\n',numzones,numnodes,numlinks,mystery1,mystery2);
for i=1:numnodes
    fprintf(fid,'%7d%5d\n',node(i).id,node(i).zone);
end

for i=1:numlinks
    fprintf(fid,'%7d%7d%5d%5d%7d%3d%7d  +%1d%4d%6d%6d%3d  +%1d\n',...
                link(i).startnode , ...
                link(i).endnode,...
                link(i).leftturnbays , ...
                link(i).rightturnbays , ...
                link(i).length, ...
                link(i).lanes ,...
                link(i).trafficflowmodel,...
                link(i).speedadjustmentfactor,...
                link(i).speedlimit,...
                link(i).serviceflow , ...
                link(i).saturationflow , ...
                link(i).type , ...
                link(i).grade );
end
fclose(fid);

