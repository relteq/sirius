function []=subdivide_cells(original,outfile,types,maxlengthinfeet)
% subdivide links that exceed a given length, and with type in types.

addpath([fileparts(fileparts(mfilename('fullpath'))) filesep 'utils_polyline']);
addpath([fileparts(fileparts(mfilename('fullpath'))) filesep 'xml_io_tools_2007_07']);

maxlengthinmiles = maxlengthinfeet/5280;
linkstruct = struct('description','',...
    'begin',struct('CONTENT',[],'ATTRIBUTE',struct('node_id','')),...
    'xEnd',struct('CONTENT',[],'ATTRIBUTE',struct('node_id','')),...
    'fd',struct('CONTENT',[],'ATTRIBUTE',struct('densityCritical',nan,'densityJam',nan,'flowMax',nan)),...
    'dynamics',struct('CONTENT',[],'ATTRIBUTE',struct('type','CTM')),...
    'qmax',100,...
    'ATTRIBUTE',struct('id','','lanes',nan,'length',nan,'name','','record','false','road_name','','type',''));

nodestruct = struct('outputs',struct('output',struct('CONTENT',[],'ATTRIBUTE',struct('link_id',nan))),...
    'inputs',struct('input',struct('CONTENT',[],'ATTRIBUTE',struct('link_id',nan))),...
    'position',struct('point',struct('CONTENT',[],'ATTRIBUTE',struct('lat',nan,'lng',nan))),...
    'ATTRIBUTE',struct('id',nan,'name','','type',''));

% read node list and directions cache
disp('Reading configuration file')
scenario = xml_read(original);

% decode directions
disp('Decoding directions')
[polyline,link2Dir,dirused]=decodepolyline(scenario);

% gather link ids
min_link_id = inf;
for i=1:length(scenario.network.LinkList.link)
    min_link_id = min([scenario.network.LinkList.link(i).ATTRIBUTE.id min_link_id]);
end

% gather node ids
nodeid = [];
for i=1:length(scenario.network.NodeList.node)
    nodeid(i) = scenario.network.NodeList.node(i).ATTRIBUTE.id;
end
min_node_id = min(nodeid);

allnewlinks = [];
allnewnodes = [];
removelinks = [];
for i=1:length(scenario.network.LinkList.link)
    link = scenario.network.LinkList.link(i);
    
    if(link.ATTRIBUTE.length>maxlengthinmiles  ...
            && any(strcmp(link.ATTRIBUTE.type,types)))
        
        numcells = ceil(link.ATTRIBUTE.length/maxlengthinmiles);
        
        removelinks = [removelinks i];
        
        new_length = link.ATTRIBUTE.length/numcells;
        
        start_node_index = find(link.begin.ATTRIBUTE.node_id==nodeid);
        end_node_index = find(link.xEnd.ATTRIBUTE.node_id==nodeid);
                
        start_node = scenario.network.NodeList.node(start_node_index);
        end_node = scenario.network.NodeList.node(end_node_index);
        
        start_node_lat = start_node.position.point.ATTRIBUTE.lat;
        start_node_lng = start_node.position.point.ATTRIBUTE.lng;
        
        end_node_lat = end_node.position.point.ATTRIBUTE.lat;
        end_node_lng = end_node.position.point.ATTRIBUTE.lng;
        
        % index of this link in start and end nodes
        if(~isempty(start_node.outputs))
            for j=1:length(start_node.outputs.output)
                if(start_node.outputs.output(j).ATTRIBUTE.link_id==link.ATTRIBUTE.id)
                    start_node_link_index = j;
                    break;
                end
            end
        end
        
        if(~isempty(end_node.inputs))
            for j=1:length(end_node.inputs.input)
                if(end_node.inputs.input(j).ATTRIBUTE.link_id==link.ATTRIBUTE.id)
                    end_node_link_index = j;
                    break;
                end
            end
        end
        
        
        for j=1:numcells
            
            if(j>1)
                start_node_link_index = 1;
            end
            
            % create new link
            [newlink,min_link_id] = assign_link(linkstruct,link,min_link_id,new_length);
            
            % connect to start_node
            if(~strcmp(start_node.ATTRIBUTE.type,'T'))
                if(j==1)
                    scenario.network.NodeList.node(start_node_index).outputs.output(start_node_link_index).ATTRIBUTE.link_id = ...
                        newlink.ATTRIBUTE.id;
                else
                    start_node.outputs.output(start_node_link_index).ATTRIBUTE.link_id = ...
                        newlink.ATTRIBUTE.id;
                end
            end
            newlink.begin.ATTRIBUTE.node_id = start_node.ATTRIBUTE.id;
            
            % store start node
            if(j>1)
                allnewnodes = [allnewnodes start_node];
            end
            
            if(j<numcells)
                
                % create new end node
                pos = position_along_polyline(j/numcells,polyline(i));
                new_node_lat = pos(1);
                new_node_lng = pos(2);
                [newnode,min_node_id]=assign_node(nodestruct,start_node,min_node_id,new_node_lat,new_node_lng);
                
                % conect
                newlink.begin.ATTRIBUTE.node_id = start_node.ATTRIBUTE.id;
                newlink.xEnd.ATTRIBUTE.node_id = newnode.ATTRIBUTE.id;
                newnode.inputs.input.ATTRIBUTE.link_id = newlink.ATTRIBUTE.id;
                
                % store link
                allnewlinks = [allnewlinks newlink];
                
                % move forward
                start_node = newnode;
            end
            
        end
        
        % connect last new link to end_node
        newlink.xEnd.ATTRIBUTE.node_id = end_node.ATTRIBUTE.id;
        if(~strcmp(end_node.ATTRIBUTE.type,'T'))
            scenario.network.NodeList.node(end_node_index).inputs.input(end_node_link_index).ATTRIBUTE.link_id = ...
                newlink.ATTRIBUTE.id;
        end
        
        % store
        allnewlinks = [allnewlinks newlink];

    end
    
end

% remove links and nodes
scenario.network.LinkList.link(removelinks) = [];

% add links and nodes
scenario.network.LinkList.link = [scenario.network.LinkList.link allnewlinks];
scenario.network.NodeList.node = [scenario.network.NodeList.node allnewnodes];

% remove directions
scenario.network.DirectionsCache = [];

% remove inputs and outputs from terminal nodes
for i=1:length(scenario.network.NodeList.node)
    if(strcmp(scenario.network.NodeList.node(i).ATTRIBUTE.type,'T'))
        scenario.network.NodeList.node(i).outputs = [];
        scenario.network.NodeList.node(i).inputs = [];
    end
end

% write output
disp('Writing output')
writeToNetworkEditor(outfile,scenario)

disp('done')

% =====================================================================

function [newlink,new_id]=assign_link(newlink,link,min_link_id,new_length)
new_id=min_link_id-1;
newlink.description = link.description;
newlink.fd = link.fd;
newlink.ATTRIBUTE.id = new_id;
newlink.ATTRIBUTE.lanes = link.ATTRIBUTE.lanes;
newlink.ATTRIBUTE.length = new_length;
newlink.ATTRIBUTE.name = link.ATTRIBUTE.name;
newlink.ATTRIBUTE.record = link.ATTRIBUTE.record;
newlink.ATTRIBUTE.road_name = link.ATTRIBUTE.road_name;
newlink.ATTRIBUTE.type = link.ATTRIBUTE.type;

function [newnode,new_id]=assign_node(newnode,node,min_node_id,lat,lng)
new_id=min_node_id-1;
newnode.ATTRIBUTE.id = new_id;
if(strcmp(node.ATTRIBUTE.type,'T'))
    newnode.ATTRIBUTE.type = 'F';
else
    newnode.ATTRIBUTE.type = node.ATTRIBUTE.type;
end
newnode.position.point.ATTRIBUTE.lat = lat;
newnode.position.point.ATTRIBUTE.lng = lng;