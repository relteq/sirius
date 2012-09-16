function [sensorinfo]=tool_extract_sensor_info(filename)

sensorinfo = [];
scenario = xml_read(filename);

if(isempty(scenario.network.SensorList))
    return
end
for i=1:length(scenario.network.SensorList.sensor)
    S = scenario.network.SensorList.sensor(i);
    if(~isempty(S.parameters))
        for j=1:length(S.parameters.parameter)
            if(strcmp(S.parameters.parameter(j).ATTRIBUTE.name,'vds'))
                sensorinfo(i).vds = S.parameters.parameter(j).ATTRIBUTE.value;
                break;
            end
        end
    else
        sensorinfo(i).vds = nan;
    end
    sensorinfo(i).lat = scenario.network.SensorList.sensor(i).position.point.ATTRIBUTE.lat;
    sensorinfo(i).lng = scenario.network.SensorList.sensor(i).position.point.ATTRIBUTE.lng;
end