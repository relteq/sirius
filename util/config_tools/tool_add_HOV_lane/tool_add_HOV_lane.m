%function tool_add_HOV_lane()

% Assumes old schema (Network Editor v20)

clear
close all

addpath([pwd filesep '../utils/xml_io_tools_2007_07'])
addpath([pwd filesep '../utils'])

original = [pwd filesep 'newI80_6.xml'];
outfile = [pwd filesep 'newI80_withHOV.xml'];

% read node list and directions cache
disp('Reading configuration file')
scenario = xml_read(original);

% find terminal nodes
isterminal = [];
for i=1:length(scenario.network.NodeList.node)
    if(isempty(scenario.network.NodeList.node(i).inputs))
        isterminal = [isterminal scenario.network.NodeList.node(i).ATTRIBUTE.id];
    end
    nodeid(i) = scenario.network.NodeList.node(i).ATTRIBUTE.id;
end

% find mainline links with terminal begin node
sourceFWid = [];
fwlink2beginnode = [];
for i=1:length(scenario.network.LinkList.link)
    link = scenario.network.LinkList.link(i);
    linkid(i) = link.ATTRIBUTE.id;
    if(strcmp(link.ATTRIBUTE.type,'FW'))
        begin_node = link.begin.ATTRIBUTE.node_id;
        fwlink2beginnode = [fwlink2beginnode;[linkid(i) begin_node]];
        if(ismember(begin_node,isterminal))
            sourceFWid = [sourceFWid begin_node];
        end
    end
end

% check that there is exactly one of these
if(length(sourceFWid)~=1)
   disp('This works on networks with exactly one mainline source');
   return
end

% contruct freeway network
cNodeId = sourceFWid;
fwlinks = [];
while(1)
   
    currentNode = scenario.network.NodeList.node(nodeid==cNodeId);
    
    % find list of freeway output links
    outFwLinks = find(cNodeId==fwlink2beginnode(:,2));
    
    if(isempty(outFwLinks))
        disp('end reached')
        break;
    end
    
    if(length(outFwLinks)>1)
        disp('freeway diverge connectors not supported')
        break;
    end
    nextlinkid = fwlink2beginnode(outFwLinks,1);
    fwlinks = [fwlinks nextlinkid];
    nextlink = scenario.network.LinkList.link(linkid==nextlinkid);
    cNodeId = nextlink.xEnd.ATTRIBUTE.node_id;
end

% create new HOV links
newid = min(linkid)-1;
newlinks = [];
for i=1:length(fwlinks)
    
    newlink = struct(...
                'description','',...
                'begin',struct('CONTENT',[],'ATTRIBUTE',struct('node_id',nan)),...
                'xEnd',struct('CONTENT',[],'ATTRIBUTE',struct('node_id',nan)),...
                'fd',struct('CONTENT',[],'ATTRIBUTE',struct('densityCritical',nan,'densityJam',nan,'flowMax',nan)),...
                'dynamics',struct('CONTENT',[],'ATTRIBUTE',struct('type','CTM')),...
                'qmax',100,...
                'ATTRIBUTE',struct('id',nan,'lanes',1,'length',nan,'name','','record','true','road_name','','type','HOV'));

            
    L = scenario.network.LinkList.link(fwlinks(i)==linkid);
    
    newlink.begin.ATTRIBUTE.node_id = L.begin.ATTRIBUTE.node_id;
    newlink.xEnd.ATTRIBUTE.node_id = L.xEnd.ATTRIBUTE.node_id;
    newlink.fd = L.fd;
    newlink.ATTRIBUTE.id = newid;
    newlink.ATTRIBUTE.length = L.ATTRIBUTE.length;
    newid = newid-1;
    
    % add new link to begin node list
    if(~ismember(newlink.begin.ATTRIBUTE.node_id,isterminal))
        node =  scenario.network.NodeList.node(newlink.begin.ATTRIBUTE.node_id==nodeid);
        newoutput = node.outputs.output(1);
        newoutput.ATTRIBUTE.link_id = newlink.ATTRIBUTE.id;
        node.outputs.output = [node.outputs.output newoutput];
        scenario.network.NodeList.node(newlink.begin.ATTRIBUTE.node_id==nodeid) = node;   
    end
    
    % add new link to end node list
    if(~ismember(newlink.xEnd.ATTRIBUTE.node_id,isterminal))
        node =  scenario.network.NodeList.node(newlink.xEnd.ATTRIBUTE.node_id==nodeid);
        newinput = node.inputs.input(1);
        newinput.ATTRIBUTE.link_id = newlink.ATTRIBUTE.id;
        node.inputs.input = [node.inputs.input newinput];
        scenario.network.NodeList.node(newlink.xEnd.ATTRIBUTE.node_id==nodeid) = node;
    end
    
    newlinks = [newlinks newlink];
end
    
% add new links to scenario
scenario.network.LinkList.link = [scenario.network.LinkList.link newlinks];

% remove weaving factors
for i=1:length(scenario.network.NodeList.node)
    if(~isempty(scenario.network.NodeList.node(i).inputs))
        scenario.network.NodeList.node(i).inputs.input = ...
            safermfield(scenario.network.NodeList.node(i).inputs.input,'weavingfactors');
    end
end

disp('Writing output')
writeToNetworkEditor(outfile,scenario)

disp('done')

