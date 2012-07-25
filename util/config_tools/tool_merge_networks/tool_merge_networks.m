function []=tool_merge_networks(infile1,infile2,outfile)
% INCOMPLETE!
% NOTE: Currently only assigns new ids to nodes and links

addpath([pwd filesep '../utils/xml_io_tools_2007_07'])
addpath([pwd filesep '../utils'])

nodestruct = struct('outputs',struct('output',[]),'inputs',struct('input',[]), ...
    'position',struct('point',[]),'ATTRIBUTE',struct('id',[],'name','','type',''));

linkstruct= struct('description','',...
    'begin',struct('ATTRIBUTE',struct('node_id',[])),...
    'xEnd',struct('ATTRIBUTE',struct('node_id',[])),...
    'fd',struct('ATTRIBUTE',struct('densityCritical',[],'densityJam',[],'flowMax',[])),...
    'dynamics',struct('ATTRIBUTE',struct('type','CTM')),...
    'qmax',[],...
    'ATTRIBUTE',struct('id',[],'lanes',[],'length',[],'name','','record','','road_name','','type',''));

sensorstruct = struct('description','','position',struct('point',[]),...
    'display_position',struct('point',[]),'parameters',[],'links',[],...
    'ATTRIBUTE',struct('id','','link_type','','type',''));




disp('Reading configuration file 1')
scenario1 = xml_read(infile1);

disp('Reading configuration file 2')
scenario2 = xml_read(infile2);

% collect all node ids
nodeidsmap1 = nan(length(scenario1.network.NodeList.node),2);
nodeidsmap2 = nan(length(scenario2.network.NodeList.node),2);
c = 1;
for i=1:length(scenario1.network.NodeList.node)
    nodeidsmap1(i,1) = scenario1.network.NodeList.node(i).ATTRIBUTE.id;
    nodeidsmap1(i,2) = c;
    nodeindexmap1(i) = c;
    c = c+1;
end
for i=1:length(scenario2.network.NodeList.node)
    nodeidsmap2(i,1) = scenario2.network.NodeList.node(i).ATTRIBUTE.id;
    nodeidsmap2(i,2) = c;
    nodeindexmap2(i) = c;
    c = c+1;
end

% collect all link ids
linkidsmap1 = nan(length(scenario1.network.LinkList.link),2);
linkidsmap2 = nan(length(scenario2.network.LinkList.link),2);
c = 1;
for i=1:length(scenario1.network.LinkList.link)
    linkidsmap1(i,1) = scenario1.network.LinkList.link(i).ATTRIBUTE.id;
    linkidsmap1(i,2) = c;
    linkindexmap1(i) = c;
    c = c+1;
end
for i=1:length(scenario2.network.LinkList.link)
    linkidsmap2(i,1) = scenario2.network.LinkList.link(i).ATTRIBUTE.id;
    linkidsmap2(i,2) = c;
    linkindexmap2(i) = c;
    c = c+1;
end

% collect all sensor ids
sensoridsmap1 = nan(length(scenario1.network.SensorList.sensor),2);
sensoridsmap2 = nan(length(scenario2.network.SensorList.sensor),2);
c = 1;
for i=1:length(scenario1.network.SensorList.sensor)
    sensoridsmap1(i,1) = scenario1.network.SensorList.sensor(i).ATTRIBUTE.id;
    sensoridsmap1(i,2) = c;
    sensorindexmap1(i) = c;
    c = c+1;
end
for i=1:length(scenario2.network.SensorList.sensor)
    sensoridsmap2(i,1) = scenario2.network.SensorList.sensor(i).ATTRIBUTE.id;
    sensoridsmap2(i,2) = c;
    sensorindexmap2(i) = c;
    c = c+1;
end




% initialize output scenario
scenario = [];

% for now just grab scenario1's settings and other stuff
scenario.settings = scenario1.settings;
scenario.network.ATTRIBUTE = scenario1.network.ATTRIBUTE;
scenario.network.description = scenario1.network.description;
scenario.network.position = scenario1.network.position;

% Union NodeList
scenario.network.NodeList.node = repmat(nodestruct,1,size(nodeidsmap1,1)+size(nodeidsmap1,1));
c = 1;
for i=1:size(nodeidsmap1,1)
    S = scenario1.network.NodeList.node(i);
    scenario.network.NodeList.node(c).outputs = S.outputs;
    scenario.network.NodeList.node(c).inputs = S.inputs;
    scenario.network.NodeList.node(c).position = S.position;
    scenario.network.NodeList.node(c).ATTRIBUTE = S.ATTRIBUTE;
    scenario.network.NodeList.node(c).ATTRIBUTE.id = nodeidsmap1(i,2);
    c = c+1;
end
for i=1:size(nodeidsmap2,1)
    S = scenario2.network.NodeList.node(i);
    scenario.network.NodeList.node(c).outputs = S.outputs;
    scenario.network.NodeList.node(c).inputs = S.inputs;
    scenario.network.NodeList.node(c).position = S.position;
    scenario.network.NodeList.node(c).ATTRIBUTE = S.ATTRIBUTE;
    scenario.network.NodeList.node(c).ATTRIBUTE.id = nodeidsmap2(i,2);
    c = c+1;
end

% Union LinkList
scenario.network.LinkList.link = repmat(linkstruct,1,size(linkidsmap1,1)+size(linkidsmap2,1));
c = 1;
for i=1:size(linkidsmap1,1)
    S = scenario1.network.LinkList.link(i);
    scenario.network.LinkList.link(c).description = S.description;
    scenario.network.LinkList.link(c).begin = S.begin;
    scenario.network.LinkList.link(c).xEnd = S.xEnd;
    scenario.network.LinkList.link(c).fd = S.fd;
    scenario.network.LinkList.link(c).dynamics = S.dynamics;
    scenario.network.LinkList.link(c).qmax = S.qmax;
    scenario.network.LinkList.link(c).ATTRIBUTE = S.ATTRIBUTE;
    scenario.network.LinkList.link(c).ATTRIBUTE.id = linkidsmap1(i,2);
    c = c+1;
end
for i=1:size(linkidsmap2,1)
    S = scenario2.network.LinkList.link(i);
    scenario.network.LinkList.link(c).description = S.description;
    scenario.network.LinkList.link(c).begin = S.begin;
    scenario.network.LinkList.link(c).xEnd = S.xEnd;
    scenario.network.LinkList.link(c).fd = S.fd;
    scenario.network.LinkList.link(c).dynamics = S.dynamics;
    scenario.network.LinkList.link(c).qmax = S.qmax;
    scenario.network.LinkList.link(c).ATTRIBUTE = S.ATTRIBUTE;
    scenario.network.LinkList.link(c).ATTRIBUTE.id = linkidsmap2(i,2);
    c = c+1;
end

% Union SensorList
scenario.network.SensorList.sensor = repmat(sensorstruct,1,size(sensoridsmap1,1)+size(sensoridsmap2,1));
c = 1;
for i=1:size(sensoridsmap1,1)
    S = scenario1.network.SensorList.sensor(i);
    scenario.network.SensorList.sensor(c).description = S.description;
    scenario.network.SensorList.sensor(c).position = S.position;
    scenario.network.SensorList.sensor(c).display_position = S.display_position;
    scenario.network.SensorList.sensor(c).parameters = S.parameters;
    scenario.network.SensorList.sensor(c).links = S.links;
    scenario.network.SensorList.sensor(c).ATTRIBUTE = S.ATTRIBUTE;
    scenario.network.SensorList.sensor(c).ATTRIBUTE.id = sensoridsmap1(i,2);
    c=c+1;
end

for i=1:size(sensoridsmap2,1)
    S = scenario2.network.SensorList.sensor(i);
    scenario.network.SensorList.sensor(c).description = S.description;
    scenario.network.SensorList.sensor(c).position = S.position;
    scenario.network.SensorList.sensor(c).display_position = S.display_position;
    scenario.network.SensorList.sensor(c).parameters = S.parameters;
    scenario.network.SensorList.sensor(c).links = S.links;
    scenario.network.SensorList.sensor(c).ATTRIBUTE = S.ATTRIBUTE;
    scenario.network.SensorList.sensor(c).ATTRIBUTE.id = sensoridsmap2(i,2);
    c=c+1;
end

% replace node_id in signal ...............................................


% replace node_id in link begin and xEnd ..................................
for i=1:length(scenario1.network.LinkList.link)
    oldnodeid = scenario1.network.LinkList.link(i).begin.ATTRIBUTE.node_id;
    newnodeid = nodeidsmap1(nodeidsmap1(:,1)==oldnodeid,2);
    scenario.network.LinkList.link(linkindexmap1(i)).begin.ATTRIBUTE.node_id = newnodeid;
    
    oldnodeid = scenario1.network.LinkList.link(i).xEnd.ATTRIBUTE.node_id;
    newnodeid = nodeidsmap1(nodeidsmap1(:,1)==oldnodeid,2);
    scenario.network.LinkList.link(linkindexmap1(i)).xEnd.ATTRIBUTE.node_id = newnodeid;
end

for i=1:length(scenario2.network.LinkList.link)
    oldnodeid = scenario2.network.LinkList.link(i).begin.ATTRIBUTE.node_id;
    newnodeid = nodeidsmap2(nodeidsmap2(:,1)==oldnodeid,2);
    scenario.network.LinkList.link(linkindexmap2(i)).begin.ATTRIBUTE.node_id = newnodeid;
    
    oldnodeid = scenario2.network.LinkList.link(i).xEnd.ATTRIBUTE.node_id;
    newnodeid = nodeidsmap2(nodeidsmap2(:,1)==oldnodeid,2);
    scenario.network.LinkList.link(linkindexmap2(i)).xEnd.ATTRIBUTE.node_id = newnodeid;
end

% replace node_id in event ................................................


% replace node_id in splitratios ..........................................


% replace node_id in controller ...........................................


% replace node_id in intersection .........................................

% replace link_id in desnity ..............................................

% replace link_id in demand ...............................................

% replace link_id in output and input .....................................

for i=1:length(scenario1.network.NodeList.node)
    N = scenario1.network.NodeList.node(i);
    if(~isempty(N.outputs))
        for j=1:length(N.outputs.output)
            oldlinkid = N.outputs.output(j).ATTRIBUTE.link_id;
            newlinkid = linkidsmap1(linkidsmap1(:,1)==oldlinkid,2);
            scenario.network.NodeList.node(nodeindexmap1(i)).outputs.output(j).ATTRIBUTE.link_id = newlinkid;
        end
    end
    
    if(~isempty(N.inputs))
        for j=1:length(N.inputs.input)
            oldlinkid = N.inputs.input(j).ATTRIBUTE.link_id;
            newlinkid = linkidsmap1(linkidsmap1(:,1)==oldlinkid,2);
            scenario.network.NodeList.node(nodeindexmap1(i)).inputs.input(j).ATTRIBUTE.link_id = newlinkid;
        end
    end
end


for i=1:length(scenario2.network.NodeList.node)
    N = scenario2.network.NodeList.node(i);
    if(~isempty(N.outputs))
        for j=1:length(N.outputs.output)
            oldlinkid = N.outputs.output(j).ATTRIBUTE.link_id;
            newlinkid = linkidsmap2(linkidsmap2(:,1)==oldlinkid,2);
            scenario.network.NodeList.node(nodeindexmap2(i)).outputs.output(j).ATTRIBUTE.link_id = newlinkid;
        end
    end
    if(~isempty(N.inputs))
        for j=1:length(N.inputs.input)
            oldlinkid = N.inputs.input(j).ATTRIBUTE.link_id;
            newlinkid = linkidsmap2(linkidsmap2(:,1)==oldlinkid,2);
            scenario.network.NodeList.node(nodeindexmap2(i)).inputs.input(j).ATTRIBUTE.link_id = newlinkid;
        end
    end
end


% replace link_id in event ................................................

% replace link_id in controller ...........................................

% replace link_id in capacity .............................................

% replace links in sensors ................................................
for i=1:length(scenario1.network.SensorList.sensor)
    S = scenario1.network.SensorList.sensor(i);
    if(~isempty(S.links))
        oldlinkid = S.links;
        newlinkid = linkidsmap1(linkidsmap1(:,1)==oldlinkid,2);
        scenario.network.SensorList.sensor(sensorindexmap1(i)).links = newlinkid;
    end
end

for i=1:length(scenario2.network.SensorList.sensor)
    S = scenario2.network.SensorList.sensor(i);
    if(~isempty(S.links))
        oldlinkid = S.links;
        newlinkid = linkidsmap2(linkidsmap2(:,1)==oldlinkid,2);
        scenario.network.SensorList.sensor(sensorindexmap2(i)).links = newlinkid;
    end
end

disp('Writing output')
writeToNetworkEditor(outfile,scenario)

disp('done')