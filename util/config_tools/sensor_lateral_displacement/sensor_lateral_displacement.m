function []=sensor_lateral_displacement(original,outfile,offset)
global gps2feet
gps2feet = 326395.2094273205; 

addpath([pwd filesep '../utils/xml_io_tools_2007_07'])
addpath([pwd filesep '../utils'])

% original = [pwd filesep 'test_singlelink_attached.xml'];
% outfile = [pwd filesep 'test_singlelink_attached_displaced.xml'];

disp('Reading configuration file')
scenario = xml_read(original);

disp('Mapping links to directions')
[~,dirused] = makeLink2DirectionsMap(scenario);

disp('Decoding directions')
polyline=decodepolyline(scenario.network.DirectionsCache.DirectionsCacheEntry(dirused));

% extract sensor positions
for i=1:length(scenario.network.SensorList.sensor)
    SensorLatLng(i,1) = scenario.network.SensorList.sensor(i).position.point.ATTRIBUTE.lat;
    SensorLatLng(i,2) = scenario.network.SensorList.sensor(i).position.point.ATTRIBUTE.lng;
end

dirusedindex = 1:length(scenario.network.DirectionsCache.DirectionsCacheEntry);
dirusedindex = dirusedindex(dirused);

%  find polyline, segment, and link for each sensor
for i=1:length(scenario.network.SensorList.sensor)
    
    if(isempty(scenario.network.SensorList.sensor(i).links))
        disp('unattached sensor found')
        continue;
    end

    for j=1:length(polyline)
        [distance(j),seg(j)] = point2polydistance(polyline(j).points,SensorLatLng(i,:));
    end
    % find closest polyline
    [~,ind] = min(distance);
    mysegment = polyline(ind).points([seg(ind):seg(ind)+1],:);
    clear distance;
    
    xa = mysegment(1,1);
    ya = mysegment(1,2);
    xb = mysegment(2,1);
    yb = mysegment(2,2);
    xs = SensorLatLng(i,1);
    ys = SensorLatLng(i,2);
    
    alpha = (yb-ya)/(xb-xa);
    xc = (xs-alpha*(ya-ys)+alpha^2*xa)/(1+alpha^2);
    yc = ya + alpha*(xc-xa);
    
    beta = -1/alpha;
    
    xm = xc + sign(ya-yb) * offset/gps2feet/sqrt(beta^2+1);
    ym = yc + beta*(xm-xc);
    
    scenario.network.SensorList.sensor(i).display_position.point.ATTRIBUTE.lat = xm;
    scenario.network.SensorList.sensor(i).display_position.point.ATTRIBUTE.lng = ym;
    
end

disp('Writing output')
writeToNetworkEditor(outfile,scenario)

disp('done')