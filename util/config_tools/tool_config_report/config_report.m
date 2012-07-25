function []=config_report(siriusfile)
% Configuration summary

scenario = xml_read(siriusfile);

str =  sprintf('Configuration file: %s\n\n',siriusfile);

% param
nodetypes = {'simple','onramp','offramp','signalized_intersection','unsignalized_intersection','terminal','highway','stop_intersection','other'};
linktypes = {'freeway','HOV','HOT','onramp','offramp','freeway_connector','street','intersection_approach','heavy_vehicle','electric_toll'};
sensortypes = {'static_point','static_area','moving_point'};
sensorlinktypes = {'freeway','HOV','onramp','offramp','other'};

% network
str = sprintf('%sNetworks\n',str);
str = sprintf('%s\tnumber of networks: %d\n',str,length(scenario.NetworkList.network));

for i=1:length(scenario.NetworkList.network)
   N =  scenario.NetworkList.network(i);
   str = sprintf('%s\tNetwork %d\n',str,i);
   
   nodetypecount = zeros(1,length(nodetypes));
   for j=1:length(N.NodeList.node)
       node = N.NodeList.node(j);
       nodetypeindex = strcmp(node.ATTRIBUTE.type,nodetypes);
       nodetypecount(nodetypeindex) = nodetypecount(nodetypeindex)+1;
   end
   
   linktypecount = zeros(1,length(linktypes));
   for j=1:length(N.LinkList.link)
       link = N.LinkList.link(j);
       linktypeindex = strcmp(link.ATTRIBUTE.type,linktypes);
       linktypecount(linktypeindex) = linktypecount(linktypeindex)+1;
   end
   
   str = sprintf('%s\t\tnumber of nodes: %d\n',str,length(N.NodeList.node));
   for j=1:length(nodetypes)
       if(nodetypecount(j)>0)
           str = sprintf('%s\t\t\t%s: %d\n',str,nodetypes{j},nodetypecount(j));
       end
   end

   str = sprintf('%s\t\tnumber of links: %d\n',str,length(N.LinkList.link));
   for j=1:length(linktypes)
       if(linktypecount(j)>0)
           str = sprintf('%s\t\t\t%s: %d\n',str,linktypes{j},linktypecount(j));
       end
   end
end
clear N node link nodetypecount linktypecount linktypeindex nodetypeindex

% Sensors

str = sprintf('%sSensors\n',str);
str = sprintf('%s\tnumber of sensors: %d\n',str,length(scenario.SensorList.sensor));

sensortypecount = zeros(1,length(sensortypes));
sensorlinktypecount = zeros(1,length(sensorlinktypes));
attached = 0;
unattached = 0;
for j=1:length(scenario.SensorList.sensor)
    sensor = scenario.SensorList.sensor(j);
    sensortypeindex = strcmp(sensor.ATTRIBUTE.type,sensortypes);
    sensortypecount(sensortypeindex) = sensortypecount(sensortypeindex)+1;
    sensorlinktypeindex = strcmp(sensor.ATTRIBUTE.link_type,sensorlinktypes);
    sensorlinktypecount(sensorlinktypeindex) = sensorlinktypecount(sensorlinktypeindex)+1;
    if(~isempty(sensor.link_reference.ATTRIBUTE.id))
        attached = attached+1;
    else
        unattached = unattached+1;
    end
end
   
str = sprintf('%s\tsensor types\n',str);
for j=1:length(sensortypes)
   if(sensortypecount(j)>0)
       str = sprintf('%s\t\t\t%s: %d\n',str,sensortypes{j},sensortypecount(j));
   end
end

str = sprintf('%s\tsensor link types\n',str);
for j=1:length(sensorlinktypes)
   if(sensorlinktypecount(j)>0)
       str = sprintf('%s\t\t\t%s: %d\n',str,sensorlinktypes{j},sensorlinktypecount(j));
   end
end

str = sprintf('%s\tattached: %d\n',str,attached);
str = sprintf('%s\tunattached: %d\n',str,unattached);

clc
disp(str)
