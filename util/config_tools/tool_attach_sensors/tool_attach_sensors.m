function []=tool_attach_sensors(original,outfile)

addpath([pwd filesep '../utils/xml_io_tools_2007_07'])
addpath([pwd filesep '../utils'])

% original = [pwd filesep 'test_singlelink.xml'];
% outfile = [pwd filesep 'test_singlelink_attached.xml'];

% read node list and directions cache
disp('Reading configuration file')
scenario = xml_read(original);

% clean out the directions cache
disp('Mapping links to directions')
[link2Dir,dirused] = makeLink2DirectionsMap(scenario);

% decode directions
disp('Decoding directions')
polyline=decodepolyline(scenario.network.DirectionsCache.DirectionsCacheEntry(dirused));

% extract sensor positions
for i=1:length(scenario.network.SensorList.sensor)
    SensorLatLng(i,1) = scenario.network.SensorList.sensor(i).position.point.ATTRIBUTE.lat;
    SensorLatLng(i,2) = scenario.network.SensorList.sensor(i).position.point.ATTRIBUTE.lng;
end

dirusedindex = 1:length(scenario.network.DirectionsCache.DirectionsCacheEntry);
dirusedindex = dirusedindex(dirused);

% add links field if not present
if(~isfield(scenario.network.SensorList.sensor,'links'))
    for i=1:length(scenario.network.SensorList.sensor)
        scenario.network.SensorList.sensor(i).links=[];
    end
end


%  find polyline, segment, and link for each sensor
for i=1:length(scenario.network.SensorList.sensor)
    
    disp(['Attaching sensor ' num2str(i) ' of ' num2str(length(scenario.network.SensorList.sensor))])
    if(~isempty(scenario.network.SensorList.sensor(i).links))
        mylink(i) = nan;
        continue;
    end
       
    for j=1:length(polyline)
        distance(j) = point2polydistance(polyline(j).points,SensorLatLng(i,:));
    end
    % find closest polyline
    [~,ind] = min(distance);
    mypolyline(i) = dirusedindex(ind);
    clear distance;
    
    ind = find(link2Dir==mypolyline(i));
    if(~isempty(ind))
        mylink(i) = ind;
    else
        mylink(i) = nan;
    end
end

for i=1:length(scenario.network.SensorList.sensor)
    if(isempty(scenario.network.SensorList.sensor(i).links) )
        scenario.network.SensorList.sensor(i).links = scenario.network.LinkList.link(mylink(i)).ATTRIBUTE.id;
    end
end


disp('Writing output')
writeToNetworkEditor(outfile,scenario)

disp('done')

