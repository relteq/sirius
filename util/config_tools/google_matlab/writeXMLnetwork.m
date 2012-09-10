function [] = writeXMLnetwork(allnodes,alllinks,outputfile)

AuroraRNM.network.ATTRIBUTE.id=-1;
AuroraRNM.network.ATTRIBUTE.name='none';
AuroraRNM.network.ATTRIBUTE.top='true';
AuroraRNM.network.ATTRIBUTE.controlled='true';
AuroraRNM.network.ATTRIBUTE.tp= 0;
AuroraRNM.network.description = 'none';
AuroraRNM.network.position.point.ATTRIBUTE.x = 0;
AuroraRNM.network.position.point.ATTRIBUTE.y = 0;
AuroraRNM.network.position.point.ATTRIBUTE.z = 0;
%AuroraExport.VehType(1)={'SOV'};
AuroraRNM.network.LinkList = [];
AuroraRNM.network.MonitorList = [];
AuroraRNM.network.SensorList = [];
AuroraRNM.settings.display.ATTRIBUTE.tp = 0;
AuroraRNM.settings.display.ATTRIBUTE.timeout = 50;
AuroraRNM.settings.display.ATTRIBUTE.tsMax = 50000;
AuroraRNM.settings.display.ATTRIBUTE.timeMax = 24;
AuroraRNM.settings.VehicleTypes(1).vtype(1).ATTRIBUTE.name = 'SOV';
AuroraRNM.settings.VehicleTypes(1).vtype(1).ATTRIBUTE.weight = '1.0';

%%% NodeList
for i=1:size(allnodes,1)
    n.ATTRIBUTE.type = 'F';
    n.ATTRIBUTE.id    = num2str(i);
    n.ATTRIBUTE.name  = ['node ' num2str(i)];
    n.ATTRIBUTE.description  = ['node ' num2str(i)];
    n.postmile = 0;
    n.position.point.ATTRIBUTE.x = allnodes(i,2);
    n.position.point.ATTRIBUTE.y = -allnodes(i,1);
    n.position.point.ATTRIBUTE.z = 0;
    AuroraRNM.network.NodeList.node(i) = n;
    clear n
end

%%% LinkList
for i=1:size(alllinks,1)
    id = num2str(i+2000);
    l.ATTRIBUTE.name = ['Link ' id];
    l.ATTRIBUTE.lanes = '1';
    l.ATTRIBUTE.length = '1';
    l.ATTRIBUTE.type = 'FW';
    l.ATTRIBUTE.id = id;
    l.begin.ATTRIBUTE.id = num2str(alllinks(i,1));
    l.end.ATTRIBUTE.id = num2str(alllinks(i,2));
    l.fd.ATTRIBUTE.densityCritical =  '1';
    l.fd.ATTRIBUTE.flowMax =  '1';
    l.fd.ATTRIBUTE.densityJam =  '1';
    l.density.CONTENTS =  '0';
    l.dynamics.ATTRIBUTE.type = 'CTM';
    l.position.point(1).ATTRIBUTE.x = '0';
    l.position.point(1).ATTRIBUTE.y = '0';
    l.position.point(1).ATTRIBUTE.z =  '0';
    l.position.point(2).ATTRIBUTE.x =  '0';
    l.position.point(2).ATTRIBUTE.y =  '0';
    l.position.point(2).ATTRIBUTE.z =  '0';
    l.qmax.CONTENTS =  '0';
    AuroraRNM.network.LinkList.link(i) = l;
    clear l
end

xml_write(outputfile, AuroraRNM);