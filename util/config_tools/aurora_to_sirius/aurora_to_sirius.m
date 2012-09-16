function []=aurora_to_sirius(aurorafile,siriusfile)
% Translate a file from the aurora format to the sirius format

addpath([fileparts(fileparts(mfilename('fullpath'))) filesep 'xml_io_tools_2007_07']);

scenario = xml_read(aurorafile);

hassensors          = hasfields(scenario.network,'SensorList','sensor');
hascontrollers      = hasfields(scenario,'ControllerSet','controller');
hasevents           = hasfields(scenario,'EventSet','event');
hassplits           = hasfields(scenario,'SplitRatioProfileSet','splitratios');
hasdemandprofile    = hasfields(scenario,'DemandProfileSet','demand');
hascapacityprofile  = hasfields(scenario,'CapacityProfileSet','capacity');
hassignal           = hasfields(scenario.network,'SignalList','signal');

blankscenarioElement = struct('ATTRIBUTE',struct('type','','id',nan));

% removes ===========================================================

% remove ODList
scenario.network=safermfield(scenario.network,'ODList');

% remove network.NetworkList
scenario.network=safermfield(scenario.network,'NetworkList');

% remove MonitorList
scenario.network=safermfield(scenario.network,'MonitorList');

% remove qmax
scenario.network.LinkList.link = safermfield(scenario.network.LinkList.link,'qmax');

% remove LinkGeometry
scenario.network.LinkList.link = safermfield(scenario.network.LinkList.link,'LinkGeometry');


% remove: ml_cotrol q_control
scenario.network.ATTRIBUTE =safermfield(scenario.network.ATTRIBUTE,'ml_control');
scenario.network.ATTRIBUTE =safermfield(scenario.network.ATTRIBUTE,'q_control');

% TEMPORARY: REMOVE WEAVING FACTORS
scenario = safermfield(scenario,'WeavingFactorsProfile');

% remove: everything related to monitors
% NOT DONE YET

% renames ===========================================================

% rename InitialDensityProfile -> InitialDensitySet
if(isfield(scenario,'InitialDensityProfile'))
    scenario = renamefield(scenario,'InitialDensityProfile','InitialDensitySet');
end

% rename: demand -> demandProfile
if(hasdemandprofile)
    scenario.DemandProfileSet = renamefield(scenario.DemandProfileSet,'demand','demandProfile');
end

% rename: splitratios -> splitratioProfile
if(hassplits)
    scenario.SplitRatioProfileSet = renamefield(scenario.SplitRatioProfileSet,'splitratios','splitratioProfile');
end

% rename: capacity -> capacityProfile
if(hascapacityprofile)
    scenario = renamefield(scenario,'CapacityProfileSet','DownstreamBoundaryCapacityProfileSet');
    scenario.DownstreamBoundaryCapacityProfileSet = renamefield(scenario.DownstreamBoundaryCapacityProfileSet,'capacity','capacityProfile');
end

if(hasfields(scenario,'settings','VehicleTypes'))
    scenario.settings.VehicleTypes=renamefield(scenario.settings.VehicleTypes,'vtype','vehicle_type');
end

% remove the DirectionsCache
scenario.network = safermfield(scenario.network,'DirectionsCache');

% remove weaving factors
for i=1:length(scenario.network.NodeList.node)
    if(~isempty(scenario.network.NodeList.node(i).inputs))
        for j=1:length(scenario.network.NodeList.node(i).inputs.input)
            X(j) = safermfield(scenario.network.NodeList.node(i).inputs.input(j),'weavingfactors');
        end
        scenario.network.NodeList.node(i).inputs.input = X;
        clear X
    end
end


% reorganize ===========================================================

% put network in NetworkList
scenario.NetworkList.network = scenario.network;
scenario =safermfield(scenario,'network');

% Move sensors to scenario
if(hassensors)
    scenario.SensorList = scenario.NetworkList.network.SensorList;
end
scenario.NetworkList.network = safermfield(scenario.NetworkList.network,'SensorList');

% Move signals to scenario
if(hassignal)
    scenario.SignalList = scenario.NetworkList.network.SignalList;
end
scenario.NetworkList.network = safermfield(scenario.NetworkList.network,'SignalList');


% collect ids ===========================================================

% collect node ids
for i=1:length(scenario.NetworkList.network.NodeList.node)
    nodeid(i) = scenario.NetworkList.network.NodeList.node(i).ATTRIBUTE.id;
end


% Fix settings ========================================================

% display paramers put in simulation
if(hasfields(scenario,'settings','display'))
    %     if(isfield(scenario.settings.display.ATTRIBUTE,'timeMax'))
    %         scenario.settings.simulation.ATTRIBUTE.timeMax = scenario.settings.display.ATTRIBUTE.timeMax;
    %     end
    %
    %     if(isfield(scenario.settings.display.ATTRIBUTE,'timeInitial'))
    %         scenario.settings.simulation.ATTRIBUTE.timeInitial = scenario.settings.display.ATTRIBUTE.timeInitial;
    %     end
    %
    %     if(isfield(scenario.settings.display.ATTRIBUTE,'dt'))
    %         scenario.settings.simulation.ATTRIBUTE.outputDt = scenario.settings.display.ATTRIBUTE.dt;
    %     end
    scenario.settings = safermfield(scenario.settings,'display');
end

% Fix NetworkList ========================================================

for i=1:length(scenario.NetworkList.network.NodeList.node)
    scenario.NetworkList.network.NodeList.node(i) = adjustNodeType(scenario.NetworkList.network.NodeList.node(i));
end

for i=1:length(scenario.NetworkList.network.LinkList.link)
    scenario.NetworkList.network.LinkList.link(i) = adjustLinkType(scenario.NetworkList.network.LinkList.link(i));
end

% Fix FundamentalDiagramProfileSet ========================================================

% create FundamentalDiagramProfileSet
for i=1:length(scenario.NetworkList.network.LinkList.link)
    
    if(~isfield(scenario.NetworkList.network.LinkList.link(i),'fd'))
        continue
    end
    FDp(i).fundamentalDiagram = convertFDtoFundamentalDiagram(scenario.NetworkList.network.LinkList.link(i).fd);
    FDp(i).ATTRIBUTE.link_id = scenario.NetworkList.network.LinkList.link(i).ATTRIBUTE.id;
    
end
scenario.NetworkList.network.LinkList.link = safermfield(scenario.NetworkList.network.LinkList.link,'fd');
scenario.FundamentalDiagramProfileSet.fundamentalDiagramProfile = FDp;
clear fundamentalDiagramProfile FD x FDp

% Fix SensorList ========================================================

% convert sensor links to link_reference
if(hassensors)
    for i=1:length(scenario.SensorList.sensor)
        
        % display_position
        scenario.SensorList.sensor(i).display_position = scenario.SensorList.sensor(i).position;
        
        % adjust type
        scenario.SensorList.sensor(i) = adjustSensorType(scenario.SensorList.sensor(i));
        
        % links -> link_reference
        if(length(scenario.SensorList.sensor(i).links)>1)
            error('Sensors may only attach to single links');
        end
        clear link_reference
        link_reference.ATTRIBUTE.id = scenario.SensorList.sensor(i).links;
        scenario.SensorList.sensor(i).link_reference = link_reference;
        
        % data_sources
        if(hasfields(scenario.SensorList.sensor(i),'data_sources','source'))
            for j=1:length(scenario.SensorList.sensor(i).data_sources.source)
                scenario.SensorList.sensor(i).data_sources.source(j)=adjustDataSourceFormat(scenario.SensorList.sensor(i).data_sources.source(j));
            end
        end
        
        % rename source -> data_source
        if(isfield(scenario.SensorList.sensor(i),'data_sources'))
            scenario.SensorList.sensor(i).data_sources = ...
                renamefield(scenario.SensorList.sensor(i).data_sources,'source','data_source');
        end
        
    end
    scenario.SensorList.sensor = safermfield(scenario.SensorList.sensor,'links');
end


% Fix EventSet ==========================================================

if(hasevents)
    for i=1:length(scenario.EventSet.event)
        
        scenario.EventSet.event(i).ATTRIBUTE.id = i-1;
        
        % adjust type
        scenario.EventSet.event(i) = adjustEventType(scenario.EventSet.event(i));
        
        % adjust fundamental diagram events
        if(strcmp(scenario.EventSet.event(i).ATTRIBUTE.type,'fundamental_diagram'))
            scenario.EventSet.event(i).fundamentalDiagram=convertFDtoFundamentalDiagram(scenario.EventSet.event(i).fd);
        end
        
        if(strcmp(scenario.EventSet.event(i).ATTRIBUTE.type,'link_lanes'))
            scenario.EventSet.event(i).ATTRIBUTE.reset_to_nominal = scenario.EventSet.event(i).lane_count_change.ATTRIBUTE.reset_to_nominal;
            scenario.EventSet.event(i).lane_count_change.ATTRIBUTE = safermfield(scenario.EventSet.event(i).lane_count_change.ATTRIBUTE,'reset_to_nominal');
        end
        
        % convert event node_id/link_id/network_id to targetElements
        A = scenario.EventSet.event(i).ATTRIBUTE;
        clear scenarioElement
        if(isfield(A,'link_id'))
            scenarioElement.ATTRIBUTE.type = 'link';
            scenarioElement.ATTRIBUTE.id = A.link_id;
            scenario.EventSet.event(i).ATTRIBUTE = safermfield(scenario.EventSet.event(i).ATTRIBUTE,'link_id');
        end
        if(isfield(A,'node_id'))
            scenarioElement.ATTRIBUTE.type = 'node';
            scenarioElement.ATTRIBUTE.id = num2str(A.node_id);
            scenario.EventSet.event(i).ATTRIBUTE = safermfield(scenario.EventSet.event(i).ATTRIBUTE,'node_id');
        end
        scenario.EventSet.event(i).targetElements.scenarioElement=scenarioElement;
        
    end
    
    if(isfield(scenario.EventSet.event,'fd'))
        scenario.EventSet.event = safermfield(scenario.EventSet.event,'fd');
    end
end

% Fix ControllerSet ==============================================

% convert controller node_id/link_id/network_id to targetElements
if(hascontrollers)
    
    % remove unwanted stuff from ControllerSet
    scenario.ControllerSet.controller=safermfield(scenario.ControllerSet.controller,'qcontroller');
    scenario.ControllerSet.controller=safermfield(scenario.ControllerSet.controller,'limits');
    
    
    for i=1:length(scenario.ControllerSet.controller)
        
        % adjust type
        scenario.ControllerSet.controller(i) = adjustControllerType(scenario.ControllerSet.controller(i));
        
        % add id
        scenario.ControllerSet.controller(i).ATTRIBUTE.id = i-1;
        
        A = scenario.ControllerSet.controller(i).ATTRIBUTE;
        clear scenarioElement
        if(isfield(A,'link_id'))
            scenarioElement.ATTRIBUTE.type = 'link';
            scenarioElement.ATTRIBUTE.id = A.link_id;
            scenario.ControllerSet.controller(i).ATTRIBUTE = safermfield(scenario.ControllerSet.controller(i).ATTRIBUTE,'link_id');
        end
        if(isfield(A,'node_id'))
            scenarioElement.ATTRIBUTE.type = 'node';
            scenarioElement.ATTRIBUTE.id = A.node_id;
            scenario.ControllerSet.controller(i).ATTRIBUTE = safermfield(scenario.ControllerSet.controller(i).ATTRIBUTE,'node_id');
        end
        
        % case signal controller, targets are signals
        
        if(strcmp(scenario.ControllerSet.controller(i).ATTRIBUTE.type(1:3),'SIG'))
            targetnodes = [];
            for j=1:length(scenario.ControllerSet.controller(i).PlanList.plan)
                P = scenario.ControllerSet.controller(i).PlanList.plan(j);
                for k=1:length(P.intersection)
                    targetnodes = [targetnodes P.intersection(k).ATTRIBUTE.node_id];
                end
            end
            targetnodes = unique(targetnodes);
            
            if(~all(ismember(targetnodes,signal2node)))
                error('not all nodes that appear in signal plans have signals')
            end
            
            if(~all(ismember(targetnodes,nodeid)))
                error('target node id not found in the node list')
            end
            
            scenarioElement = repmat(blankscenarioElement,1,length(targetnodes));
            for j=1:length(targetnodes)
                scenarioElement(j).ATTRIBUTE.type = 'signal';
                scenarioElement(j).ATTRIBUTE.id = scenario.SignalList.signal(signal2node==targetnodes(j)).ATTRIBUTE.id;
            end
            
        end
        
        % transitition_delay should not be NaN
        if(isfield(scenario.ControllerSet.controller(i),'PlanSequence'))
            if(isnan(scenario.ControllerSet.controller(i).PlanSequence.ATTRIBUTE.transition_delay))
                scenario.ControllerSet.controller(i).PlanSequence.ATTRIBUTE.transition_delay=0;
            end
        end
        
        scenario.ControllerSet.controller(i).targetElements.scenarioElement=scenarioElement;
    end
end

% remove usesensors from controller
if(hascontrollers)
    for i=1:length(scenario.ControllerSet.controller)
        scenario.ControllerSet.controller(i).ATTRIBUTE=safermfield(scenario.ControllerSet.controller(i).ATTRIBUTE,'usesensors');
    end
end

% Fix SignalSet ====================================================

% fix signals
if(hassignal)
    signal2node = nan(1,length(scenario.SignalList.signal));
    networkid = scenario.NetworkList.network.ATTRIBUTE.id;
    for i=1:length(scenario.SignalList.signal)
        signal2node(i) = scenario.SignalList.signal(i).ATTRIBUTE.node_id;
        
        % add signal id
        scenario.SignalList.signal(i).ATTRIBUTE.id = -i;
        
        % links -> link_reference
        for j=1:length(scenario.SignalList.signal(i).phase)
            if(~isempty(scenario.SignalList.signal(i).phase(j).links))
                links = scenario.SignalList.signal(i).phase(j).links;
                link_reference = repmat(struct('ATTRIBUTE',struct('id',nan)),1,length(links));
                for k=1:length(links)
                    link_reference(k).ATTRIBUTE.id = links(k);
                end
                scenario.SignalList.signal(i).phase(j).links = [];
                scenario.SignalList.signal(i).phase(j).links.link_reference = link_reference;
            end
        end
    end
end

% Fix SplitRatioProfileSet ===============================================

% added: link_in and link_out to splitratio
if(hassplits)
    for i=1:length(scenario.SplitRatioProfileSet.splitratioProfile)
        srP = scenario.SplitRatioProfileSet.splitratioProfile(i);
        if(~isfield(srP,'srm'))
            continue;
        end
        myNode = scenario.NetworkList.network.NodeList.node(nodeid==srP.ATTRIBUTE.node_id);
        numin = length(myNode.inputs.input);
        for j=1:numin
            inlink(j) = myNode.inputs.input(j).ATTRIBUTE.link_id;
        end
        numout = length(myNode.outputs.output);
        for j=1:length(myNode.outputs.output)
            outlink(j) = myNode.outputs.output(j).ATTRIBUTE.link_id;
        end
        X=readMatrix(srP.srm,numin,numout);
        clear splitratio
        c=1;
        for iin = 1:numin
            for iout = 1:numout
                splitratio(c).ATTRIBUTE.link_in = inlink(iin);
                splitratio(c).ATTRIBUTE.link_out = outlink(iout);
                splitratio(c).CONTENT = writecommaformat(reshape(X(iin,iout,:),1,size(X,3)));
                c = c+1;
            end
        end
        scenario.SplitRatioProfileSet.splitratioProfile(i).splitratio = splitratio;
    end
    scenario.SplitRatioProfileSet.splitratioProfile=safermfield(scenario.SplitRatioProfileSet.splitratioProfile,'srm');
    
end
clear srP splitratio X myNode A scenarioElement inlink outlink c i numin numout iin iout

% Fix DemandProfileSet ================================================

% demandProfile link_id -> link_id_origin
if(hasdemandprofile)
    for i=1:length(scenario.DemandProfileSet.demandProfile)
        scenario.DemandProfileSet.demandProfile(i).ATTRIBUTE = renamefield(...
            scenario.DemandProfileSet.demandProfile(i).ATTRIBUTE,'link_id','link_id_origin');
    end
end

% make demands into comma separated string
if(hasdemandprofile)
    for i=1:length(scenario.DemandProfileSet.demandProfile)
        scenario.DemandProfileSet.demandProfile(i).CONTENT = ...
            writecommaformat(scenario.DemandProfileSet.demandProfile(i).CONTENT);
    end
end

writeToNetworkEditor(siriusfile,scenario);

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
function [x]=adjustEventType(x)
switch x.ATTRIBUTE.type
    case 'FD'
        newtype = 'fundamental_diagram';
    case 'SRM'
        newtype = 'node_split_ratio';
    case 'LC'
        newtype = 'link_lanes';
    otherwise
        warning('unsupported event type')
end
x.ATTRIBUTE.type = newtype;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [x]=adjustDataSourceFormat(x)
switch x.ATTRIBUTE.format
    case {'pems','PeMS Data Clearinghouse',''}
        newtype = 'PeMS Data Clearinghouse';
    case {'dbx','Caltrans DBX'}
        newtype = 'Caltrans DBX';
    case 'bhl'
        newtype = 'BHL';
    otherwise
        warning(['unsupported data source type ' x.ATTRIBUTE.format])
end
x.ATTRIBUTE.format = newtype;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [x]=adjustControllerType(x)
switch x.ATTRIBUTE.type
    case 'ALINEA'
        newtype = 'IRM_alinea';
    case 'TOD'
        newtype = 'IRM_time_of_day';
    case 'TR'
        newtype = 'IRM_traffic_responsive';
    case 'PRETIMED'
        newtype = 'SIG_pretimed';
    case 'ACTUADED'
        newtype = 'SIG_actuated';
    case 'SWARM'
        newtype = 'CRM_swarm';
    case 'HERO'
        newtype = 'CRM_hero';
    case 'VSLTOD'
        newtype = 'VSL_time_of_day';
    otherwise
        warning('unsupported controller type')
end
x.ATTRIBUTE.type = newtype;

if(isfield(x,'qcontroller'))
    switch x.qcontroller.ATTRIBUTE.type
        case 'QUEUEOVERRIDE'
            newtype = 'queue_override';
        case 'PROPORTIONAL'
            newtype = 'proportional';
        case 'PI'
            newtype = 'proportional_integral';
        case 'null'
            newtype = 'none';
        otherwise
            warning('unsupported queue controller type')
    end
    x.qcontroller.ATTRIBUTE.type = newtype;
    
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [x]=adjustSensorType(x)
switch x.ATTRIBUTE.type
    case 'loop'
        newtype = 'static_point';
    case {'radar','camera'}
        newtype = 'static_area';
    case 'sensys'
        newtype = 'moving_point';
    otherwise
        warning('unsupported sensor type')
end
x.ATTRIBUTE.type = newtype;

switch x.ATTRIBUTE.link_type
    case 'FW'
        newtype = 'freeway';
    case 'HOV'
        newtype = 'HOV';
    case 'OR'
        newtype = 'onramp';
    case 'offramp'
        newtype = 'offramp';
    otherwise
        newtype = 'other';
end

x.ATTRIBUTE.link_type = newtype;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [x]=adjustNodeType(x)
if isfield(x.ATTRIBUTE, 'type')
    switch x.ATTRIBUTE.type
        case {'F','H'}
            newtype = 'simple';
        case 'S'
            newtype = 'signalized_intersection';
        case 'T'
            newtype = 'terminal';
        otherwise
            warning('unsupported node type')
    end
    x.ATTRIBUTE.type = newtype;
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [x]=adjustLinkType(x)
if isfield(x.ATTRIBUTE, 'type')
    switch x.ATTRIBUTE.type
        case {'FW','HW'}
            newtype = 'freeway';
        case {'HOV','HV','ETC'}
            newtype = 'HOV';
        case 'OR'
            newtype = 'onramp';
        case 'FR'
            newtype = 'offramp';
        case 'IC'
            newtype = 'freeway_connector';
        case 'ST'
            newtype = 'street';
        case 'HOT'
            newtype = 'HOT';
        otherwise
            warning('unsupported link type')
    end
    x.ATTRIBUTE.type = newtype;
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [SR]=readMatrix(A,numin,numout)

if(~iscell(A))
    SR=A;
    return
end
for k=1:length(A)
    for row=1:numin
        for col=1:numout
            SR(row,col,k) = A{k}(row,col);
        end
    end
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [b]=hasfields(X,a,b)
if(~isfield(X,a))
    b=false;
    return
end
b=isfield(X.(a),b);

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [FD]=convertFDtoFundamentalDiagram(fd)

if(isempty(fd))
    FD=[];
    return
end

x = fd.ATTRIBUTE;

if(isfield(x,'flowMax'))
    capacity = x.flowMax;
else
    capacity = 2400;
end

if(isfield(x,'densityJam'))
    densityJam = x.densityJam;
else
    densityJam = 160;
end

if(isfield(x,'densityCritical'))
    densityCritical = x.densityCritical;
else
    densityCritical = 40;
end

if(isfield(x,'capacityDrop'))
    capacityDrop = x.capacityDrop;
else
    capacityDrop = 0;
end

FD.ATTRIBUTE.capacity = capacity;
FD.ATTRIBUTE.jam_density = densityJam;
FD.ATTRIBUTE.capacity_drop = capacityDrop;
FD.ATTRIBUTE.congestion_speed = capacity/(densityJam-densityCritical);
FD.ATTRIBUTE.free_flow_speed = capacity/densityCritical;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [str]=writecommaformat(a,f)
% translate a 1,2,or 3 dimensional array into a string

str = '';

if(~exist('f','var'))
    f = '%f';
end

% unpack cell singleton
if(iscell(a) && length(a)==1)
    a = a{1};
end

% try to interpret a string
if(ischar(a))
    a = readcommaformat(a);
    if(isnan(a))
        return
    end
end

% a must be numeric and non-empty. f must be a string.
if(~isnumeric(a) || ~ischar(f) || isempty(a) )
    return
end

for i=1:size(a,1)
    clear strj
    for j=1:size(a,2)
        % colon separate a(i,j,:)
        strj{j} = sprintf(f,a(i,j,1));
        for k =2:size(a,3)
            strj{j} = [strj{j} ':' sprintf('%f',a(i,j,k))];
        end
    end
    row = strj{1};
    for j=2:size(a,2)
        row = [row ',' strj{j}];
    end
    if i==1
        str = row;
    else
        str = [str ';' row];
    end
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [X] = readcommaformat(str)
% translate a string into a 1,2,or 3 dimensional array

A  = strread(str,'%s','delimiter',';');
numin = length(A);
temp  = strread(A{1},'%s','delimiter',',');
numout = length(temp);
temp = strread(temp{1},'%s','delimiter',':');
numtypes = length(temp);
clear temp;

X = nan(numin,numout,numtypes);

for i=1:numin
    B = strread(A{i},'%s','delimiter',',');
    for j=1:numout
        C = strread(B{j},'%s','delimiter',':');
        for k=1:numtypes
            X(i,j,k) = str2double(C{k});
        end
    end
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [] = writeToNetworkEditor(outputfilename,scenario)

%  write it
disp('Serializing XML');
dom = xml_write([], scenario);
str = xmlwrite(dom);

i=0;

i=i+1;
replace(i).from = '<begin/>';
replace(i).to   = '';

i=i+1;
replace(i).from = '<xEnd/>';
replace(i).to   = '';

i=i+1;
replace(i).from = 'xEnd';
replace(i).to   = 'end';

for i = 1:length(replace)
    str = strrep(str, replace(i).from, replace(i).to);
end

disp(['Writing ' outputfilename])
[file, msg] = fopen(outputfilename, 'w');
if msg ~= '', error(msg); end
fprintf(file, '%s', str);
fclose(file);


