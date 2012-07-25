function []=makenodelist(points)

rootdir = 'C:\Gabriel\PATH_TOPL';
addpath([rootdir '\FreewayModeler\Utils'])
addpath([rootdir '\FreewayModeler\Utils\xml_io_tools_2007_07'])

AuroraRNM.network.ATTRIBUTE.id=-1;
AuroraRNM.network.ATTRIBUTE.name='580';
AuroraRNM.network.ATTRIBUTE.top='true';
AuroraRNM.network.ATTRIBUTE.controlled='true';
AuroraRNM.network.ATTRIBUTE.tp= 0;
AuroraRNM.network.description = 'PeMS mainline VDS';
AuroraRNM.network.position.point.ATTRIBUTE.x = 0;
AuroraRNM.network.position.point.ATTRIBUTE.y = 0;
AuroraRNM.network.position.point.ATTRIBUTE.z = 0;
AuroraExport.VehType(1)={'SOV'};
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

for i=1:size(points,1)
    n.ATTRIBUTE.type = 'F';
    n.ATTRIBUTE.id    = num2str(10000+i);
    n.ATTRIBUTE.name  = ['VDS ' num2str(i)];
    n.ATTRIBUTE.description  = ['VDS ' num2str(i)];
    n.postmile = 0;
    n.position.point.ATTRIBUTE.x = points(i,2);
    n.position.point.ATTRIBUTE.y = -points(i,1);
    n.position.point.ATTRIBUTE.z = 0;
    AuroraRNM.network.NodeList.node(i) = n;
    clear n
end
xml_write('nodelist.xml', AuroraRNM);

%%% SENSORLIST 
% 
% for i=1:length(CFG.vds)
%     switch ceil(3*rand(1))
%         case 1
%             s.ATTRIBUTE.type = 'internal';
%         case 2
%             s.ATTRIBUTE.type = 'source';
%         otherwise
%             s.ATTRIBUTE.type = 'sink';
%     end
%     
%     s.ATTRIBUTE.id    = num2str(50000+i);
%     s.ATTRIBUTE.name  = ['VDS ' num2str(i)];
%     s.postmile = CFG.abspostmile(i);
%     s.position.point.ATTRIBUTE.x = CFG.longitude(i);
%     s.position.point.ATTRIBUTE.y = -CFG.latitude(i);
%     s.position.point.ATTRIBUTE.z = 0;
%     AuroraRNM.network.SensorList.sensor(i) = s;
%     clear n
% end
% xml_write('yyy.xml', AuroraRNM);
