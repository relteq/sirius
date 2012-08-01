clear
close all

addpath([pwd filesep '../utils/xml_io_tools_2007_07'])
addpath([pwd filesep '../utils'])
addpath([pwd filesep 'get_google_map'])
addpath([pwd filesep 'plot_google_map'])
addpath([pwd filesep 'deg2utm'])

original = [pwd filesep 'test_singlelink_attached.xml'];

% read node list and directions cache
disp('Reading configuration file')
scenario = xml_read(original);

% extract sensor positions
numsensors=length(scenario.network.SensorList.sensor);
for i=1:numsensors
    S = scenario.network.SensorList.sensor(i);
    SensorLatLng(i,1) = S.position.point.ATTRIBUTE.lat;
    SensorLatLng(i,2) =S.position.point.ATTRIBUTE.lng;
    
    for j=1:length(S.parameters.parameter)
        if(strcmp(S.parameters.parameter(j).ATTRIBUTE.name,'vds'))
            vds(i) = S.parameters.parameter(j).ATTRIBUTE.value;
            continue;
        end
        if(strcmp(S.parameters.parameter(j).ATTRIBUTE.name,'lanes'))
            lanes(i) = S.parameters.parameter(j).ATTRIBUTE.value;
            continue;
        end
    end
end
clear S


colormap(colormap_redyellowgreen());
scatter(SensorLatLng(:,2),SensorLatLng(:,1),50,LoopHealth,'filled')
hold on
scatter(SensorLatLng(:,2),SensorLatLng(:,1),50,[0 0 0])
plot_google_map('maptype','roadmap')
