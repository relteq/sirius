function []=sirius2aurora(siriusfile,aurorafile)

% scenario = xml_read(siriusfile);
load aaa

if(length(scenario.NetworkList.network)>1)
    error('works for single networks only')
end

% collect link ids
for i=1:length(scenario.NetworkList.network.LinkList.link)
    linkid(i)=scenario.NetworkList.network.LinkList.link(i).ATTRIBUTE.id;
end

% collect nodeids
for i=1:length(scenario.NetworkList.network.NodeList.node)
    nodeid(i)=scenario.NetworkList.network.NodeList.node(i).ATTRIBUTE.id;
end

% put fundamental diagrams into links
for i=1:length(scenario.FundamentalDiagramProfileSet.fundamentalDiagramProfile)
    link_id = scenario.FundamentalDiagramProfileSet.fundamentalDiagramProfile(i).ATTRIBUTE.link_id;
    fd=scenario.FundamentalDiagramProfileSet.fundamentalDiagramProfile(i).fundamentalDiagram.ATTRIBUTE;
    newfd = struct(...
        'densityCritical',fd.capacity/fd.freeflow_speed,...
        'densityJam',fd.densityJam,...
        'flowMax',fd.capacity);
    scenario.NetworkList.network.LinkList.link(linkid==link_id).fd.ATTRIBUTE = newfd;
end

% rename: splitratioProfile -> splitratios
scenario.SplitRatioProfileSet =renamefield(scenario.SplitRatioProfileSet,'splitratioProfile','splitratios');

% rename: demandProfile -> demand
scenario.DemandProfileSet =renamefield(scenario.DemandProfileSet,'demandProfile','demand');

% add: ml_cotrol q_control
scenario.NetworkList.network.ATTRIBUTE.ml_control = false;
scenario.NetworkList.network.ATTRIBUTE.q_control = false;

% adjust types
for i=1:length(scenario.NetworkList.network.NodeList.node)
    scenario.NetworkList.network.NodeList.node(i) = adjustNodeType(scenario.NetworkList.network.NodeList.node(i));
end

for i=1:length(scenario.NetworkList.network.LinkList.link)
    scenario.NetworkList.network.LinkList.link(i) = adjustLinkType(scenario.NetworkList.network.LinkList.link(i));
end

% DemandProfileSet
for i=1:length(scenario.DemandProfileSet.demand)
    
    D = scenario.DemandProfileSet.demand(i);
    % rename link_id_origin to link_id
    scenario.DemandProfileSet.demand(i).ATTRIBUTE.link_id = D.ATTRIBUTE.link_id_origin;
    scenario.DemandProfileSet.demand(i).ATTRIBUTE = ...
        rmfield(scenario.DemandProfileSet.demand(i).ATTRIBUTE,'link_id_origin');
end


% move network to scenario
scenario.network = scenario.NetworkList.network;

% remove inputs and outputs from terminal nodes
for i=1:length(scenario.NetworkList.network.NodeList.node)
    if(strcmp(scenario.NetworkList.network.NodeList.node(i).ATTRIBUTE.type,'T'))
        scenario.NetworkList.network.NodeList.node(i).outputs = [];
        scenario.NetworkList.network.NodeList.node(i).inputs = [];
    end
end


% remove superfluous fields
scenario = safermfield(scenario,'NetworkList');
scenario = safermfield(scenario,'FundamentalDiagramProfileSet');
scenario = safermfield(scenario,'ControllerSet');
scenario = safermfield(scenario,'InitialDensityProfile');
scenario = safermfield(scenario,'EventSet');
scenario.network = safermfield(scenario.network,'SensorList');


writeToNetworkEditor(aurorafile,scenario);


%%%%%%%%%%%%%%%%%%%%%%%%%
function [b]=hasfields(X,a,b)
if(~isfield(X,a))
    b=false;    return
end
b=isfield(X.(a),b);


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [x]=safermfield(x,name)
if(isfield(x,name))
    x = rmfield(x,name);
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [x]=renamefield(x,origname,newname)
if(isfield(x,origname))
    for i=1:length(x.(origname))
        x.(newname)(i) = x.(origname)(i);
    end
    x = rmfield(x,origname);
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [x]=adjustNodeType(x)
switch x.ATTRIBUTE.type
    case 'simple'
        newtype = 'F';
    case 'signalized intersection'
        newtype = 'S';
    case 'terminal'
        newtype = 'T';
    otherwise
        warning('unsupported node type')
end
x.ATTRIBUTE.type = newtype;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [x]=adjustLinkType(x)
switch x.ATTRIBUTE.type
    case 'freeway'
        newtype = 'FW';
    case 'onramp'
        newtype = 'OR';
    case 'HOV'
        newtype = 'HOV';
    case 'offramp'
        newtype = 'FR';
    case 'freeway connector'
        newtype = 'IC';
    case 'street'
        newtype = 'ST';
    otherwise
        warning('unsupported link type')
end
x.ATTRIBUTE.type = newtype;

