function []=attach_sensors(original,outfile,link_types)
% attach sensors with link type linktype to the neart link
% with the same type

addpath([fileparts(fileparts(mfilename('fullpath'))) filesep 'utils_geonorm']);
addpath([fileparts(fileparts(mfilename('fullpath'))) filesep 'utils_polyline']);
addpath([fileparts(fileparts(mfilename('fullpath'))) filesep 'xml_io_tools_2007_07']);

% read node list and directions cache
disp('Reading configuration file')
scenario = xml_read(original);

% decode directions
disp('Decoding directions')
polyline=decodepolyline(scenario);

% clean out the directions cache
disp('Mapping links to directions')
link2Dir = makeLink2DirectionsMap(scenario);

% extract sensor positions
for i=1:length(scenario.network.SensorList.sensor)
    SensorLatLng(i,1) = scenario.network.SensorList.sensor(i).position.point.ATTRIBUTE.lat;
    SensorLatLng(i,2) = scenario.network.SensorList.sensor(i).position.point.ATTRIBUTE.lng;
end

% add links field if not present
if(~isfield(scenario.network.SensorList.sensor,'links'))
    for i=1:length(scenario.network.SensorList.sensor)
        scenario.network.SensorList.sensor(i).links=[];
    end
end

%  find polyline, segment, and link for each sensor
for i=1:length(scenario.network.SensorList.sensor)
    
    S = scenario.network.SensorList.sensor(i);
    sensorpos = SensorLatLng(i,:);

    if(~any(strcmp(S.ATTRIBUTE.link_type,link_types)))
        mylink(i) = nan;
        continue
    end
    
    disp(['Attaching sensor ' num2str(i) ' of ' num2str(length(scenario.network.SensorList.sensor))])
    if(~isempty(S.links))
        mylink(i) = nan;
        continue
    end

    for j=1:length(scenario.network.LinkList.link)
        if(~strcmp(scenario.network.LinkList.link(j).ATTRIBUTE.type,S.ATTRIBUTE.link_type))
            distance(j) = inf;
        else
            distance(j) = point2polydistance(polyline(link2Dir(j)).points,sensorpos);
        end
    end
    
    % find closest link
    [~,mylinkind] = min(distance);

    % save corresponding link
    if(~isempty(mylinkind))
        mylink(i) = mylinkind;
        
        % project the position of the sensor onto the polyline
        mypolyline = polyline(link2Dir(mylinkind));
        [~,segmentindex]=point2polydistance(mypolyline.points,sensorpos);
        p0=mypolyline.points(segmentindex,:);
        p1=mypolyline.points(segmentindex+1,:);
        dp = p1-p0;
        b = [dot(sensorpos,dp);-det([p0;dp])];
        A = [ dp ; [p0(2)-p1(2) p1(1)-p0(1)] ];
        q = (A\b)';
        
        % check that sensor is placed between p0 and p1
        z=dot(q-p0,dp)/ sum(dp.^2);
        if(z>1)
            q = p1;
        end
        if(z<0)
            q = p0;
        end
        newsensorpos(i,:) = q;
    else
        mylink(i) = nan;
        newsensorpos(i,:) = [nan nan];
    end
    
    
end

for i=1:length(scenario.network.SensorList.sensor)
    if(isempty(scenario.network.SensorList.sensor(i).links) && ~isnan(mylink(i)) )
        scenario.network.SensorList.sensor(i).links = scenario.network.LinkList.link(mylink(i)).ATTRIBUTE.id;
        scenario.network.SensorList.sensor(i).position.point.ATTRIBUTE.lat = newsensorpos(i,1);
        scenario.network.SensorList.sensor(i).position.point.ATTRIBUTE.lng = newsensorpos(i,2);
        scenario.network.SensorList.sensor(i).display_position.point.ATTRIBUTE.lat = newsensorpos(i,1);
        scenario.network.SensorList.sensor(i).display_position.point.ATTRIBUTE.lng = newsensorpos(i,2);
    end
end

disp('Writing output')
writeToNetworkEditor(outfile,scenario)

disp('done')
