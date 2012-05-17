function []=aurora2sirius(aurorafile,siriusfile)
% To do
% + fix split ratios for multiple vehicle types

scenario = xml_read(aurorafile);

hassensors          = hasfields(scenario.network,'SensorList','sensor');
hascontrollers      = hasfields(scenario,'ControllerSet','controller');
hasevents           = hasfields(scenario,'EventSet','event');
hassplits           = hasfields(scenario,'SplitRatioProfileSet','splitratios');
hasdemandprofile    = hasfields(scenario,'DemandProfileSet','demand');
hascapacityprofile  = hasfields(scenario,'CapacityProfileSet','capacity');
hassignal           = hasfields(scenario.network,'SignalList','signal');

blankscenarioElement = struct('ATTRIBUTE',struct('type','','id',nan));
            
% % remove terminal nodes
% removethisid = [];
% for i=1:length(scenario.network.NodeList.node)
%     removethis(i) = strcmpi(scenario.network.NodeList.node(i).ATTRIBUTE.type,'T');
%     if(removethis(i))
%         removethisid = [removethisid scenario.network.NodeList.node(i).ATTRIBUTE.id];
%     end
% end
% scenario.network.NodeList.node(removethis)=[];
% for i=1:length(scenario.network.LinkList.link)
%     if(ismember(scenario.network.LinkList.link(i).begin.ATTRIBUTE.node_id,removethisid))
%         scenario.network.LinkList.link(i).begin = [];
%     end
%     if(ismember(scenario.network.LinkList.link(i).xEnd.ATTRIBUTE.node_id,removethisid))
%         scenario.network.LinkList.link(i).xEnd = [];
%     end
% end
% clear removethis removethisid

% remove ODList
scenario.network=safermfield(scenario.network,'ODList');

% remove MonitorList
scenario.network=safermfield(scenario.network,'MonitorList');

% remove NetworkList
scenario.network=safermfield(scenario.network,'NetworkList');

% collect node ids
for i=1:length(scenario.network.NodeList.node)
    nodeid(i) = scenario.network.NodeList.node(i).ATTRIBUTE.id;
end

% rename: demand -> demandProfile
if(hasdemandprofile)
    scenario.DemandProfileSet = renamefield(scenario.DemandProfileSet,'demand','demandProfile');
end

% rename: splitratios -> splitratioProfile
if(hassplits)
    scenario.SplitRatioProfileSet = renamefield(scenario.SplitRatioProfileSet,'splitratios','splitratioProfile');
end

% rename: CapacityProfileSet -> DownstreamBoundaryCapacitySet
% rename: capacity -> capacityProfile
if(hascapacityprofile)
    scenario = renamefield(scenario,'CapacityProfileSet','DownstreamBoundaryCapacitySet');
    scenario.DownstreamBoundaryCapacitySet = renamefield(scenario.DownstreamBoundaryCapacitySet,'capacity','capacityProfile');
end

% create FundamentalDiagramProfileSet
for i=1:length(scenario.network.LinkList.link)
    
    if(~isfield(scenario.network.LinkList.link(i),'fd'))
        continue
    end
    FDp(i).fundamentalDiagram = convertFDtoFundamentalDiagram(scenario.network.LinkList.link(i).fd);
    FDp(i).ATTRIBUTE.link_id = scenario.network.LinkList.link(i).ATTRIBUTE.id;
    
end
scenario.network.LinkList.link = safermfield(scenario.network.LinkList.link,'fd');
scenario.FundamentalDiagramProfileSet.fundamentalDiagramProfile = FDp;
clear fundamentalDiagramProfile FD x FDp

% remove qmax
scenario.network.LinkList.link = safermfield(scenario.network.LinkList.link,'qmax');

% remove: ml_cotrol q_control
scenario.network.ATTRIBUTE =safermfield(scenario.network.ATTRIBUTE,'ml_control');
scenario.network.ATTRIBUTE =safermfield(scenario.network.ATTRIBUTE,'q_control');

% convert sensor links to link_reference
if(hassensors)
    for i=1:length(scenario.network.SensorList.sensor)
        
        if(length(scenario.network.SensorList.sensor(i).links)>1)
            error('Sensors may only attach to single links');
        end
        clear link_reference  
        link_reference.ATTRIBUTE.id = scenario.network.SensorList.sensor(i).links;
        scenario.network.SensorList.sensor(i).link_reference = link_reference;
    end
    scenario.network.SensorList.sensor = safermfield(scenario.network.SensorList.sensor,'links');
end

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

% remove: everything related to monitors
% NOT DONE YET

for i=1:length(scenario.network.NodeList.node)
    scenario.network.NodeList.node(i) = adjustNodeType(scenario.network.NodeList.node(i));
end

for i=1:length(scenario.network.LinkList.link)
    scenario.network.LinkList.link(i) = adjustLinkType(scenario.network.LinkList.link(i));
end

if(hassensors)
    for i=1:length(scenario.network.SensorList.sensor)
        scenario.network.SensorList.sensor(i) = adjustSensorType(scenario.network.SensorList.sensor(i));
        if(hasfields(scenario.network.SensorList.sensor(i),'data_sources','source'))
            for j=1:length(scenario.network.SensorList.sensor(i).data_sources.source)
                scenario.network.SensorList.sensor(i).data_sources.source(j)=adjustDataSourceFormat(scenario.network.SensorList.sensor(i).data_sources.source(j));
            end
        end
    end
end

if(hasevents)
    for i=1:length(scenario.EventSet.event)
        scenario.EventSet.event(i) = adjustEventType(scenario.EventSet.event(i));
    end
    if(isfield(scenario.EventSet.event,'fd'))
        for i=1:length(scenario.EventSet.event)
            scenario.EventSet.event(i).fundamentalDiagram=convertFDtoFundamentalDiagram(scenario.EventSet.event(i).fd);
        end
        scenario.EventSet.event=safermfield(scenario.EventSet.event,'fd');
    end
end

% remove unwanted stuff from controller
if(hascontrollers)
    scenario.ControllerSet.controller=safermfield(scenario.ControllerSet.controller,'qcontroller');
    scenario.ControllerSet.controller=safermfield(scenario.ControllerSet.controller,'limits');
end

% remove: event->network_id (can't think of use case for network specific event)
% NOT DONE YET

% remove: controller->network_id (controllers work on collections of nodes and links).
% NOT DONE YET

% rename: source -> data_source
if(hassensors)
    for i=1:length(scenario.network.SensorList.sensor)
        if(isfield(scenario.network.SensorList.sensor(i),'data_sources'))
            scenario.network.SensorList.sensor(i).data_sources = ...
                renamefield(scenario.network.SensorList.sensor(i).data_sources,'source','data_source');
        end
    end
end

% convert event node_id/link_id/network_id to targetElements
if(hasevents)
    for i=1:length(scenario.EventSet.event)
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
end

if(hascontrollers)
    for i=1:length(scenario.ControllerSet.controller)
        scenario.ControllerSet.controller(i) = adjustControllerType(scenario.ControllerSet.controller(i));
    end
end

% fix signals
if(hassignal)
    signal2node = nan(1,length(scenario.network.SignalList.signal));
    networkid = scenario.network.ATTRIBUTE.id;
    for i=1:length(scenario.network.SignalList.signal)
        signal2node(i) = scenario.network.SignalList.signal(i).ATTRIBUTE.node_id;
        
        % add signal id
        scenario.network.SignalList.signal(i).ATTRIBUTE.id = -i;
        
        % links -> link_reference
        for j=1:length(scenario.network.SignalList.signal(i).phase)
            if(~isempty(scenario.network.SignalList.signal(i).phase(j).links))
                links = scenario.network.SignalList.signal(i).phase(j).links;
                link_reference = repmat(struct('ATTRIBUTE',struct('network_id',networkid,'id',nan)),1,length(links));
                for k=1:length(links)
                    link_reference(k).ATTRIBUTE.id = links(k);
                end
                scenario.network.SignalList.signal(i).phase(j).links = [];
                scenario.network.SignalList.signal(i).phase(j).links.link_reference = link_reference;
            end
        end
    end
else
    signal2node = [];
end

% convert controller node_id/link_id/network_id to targetElements
if(hascontrollers)
    for i=1:length(scenario.ControllerSet.controller)
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
                scenarioElement(j).ATTRIBUTE.id = scenario.network.SignalList.signal(signal2node==targetnodes(j)).ATTRIBUTE.id;
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

% rename: vtype -> vehicleType

if(hasfields(scenario,'settings','VehicleTypes'))
    scenario.settings.VehicleTypes=renamefield(scenario.settings.VehicleTypes,'vtype','vehicleType');
end

% added element: linkpair
% NOT DONE YET

% added: link_in and link_out to splitratio
if(hassplits)
    for i=1:length(scenario.SplitRatioProfileSet.splitratioProfile)
        srP = scenario.SplitRatioProfileSet.splitratioProfile(i);
        if(~isfield(srP,'srm'))
            continue;
        end
        myNode = scenario.network.NodeList.node(nodeid==srP.ATTRIBUTE.node_id);
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

% demandProfile link_id -> link_id_origin
if(hasdemandprofile)
    for i=1:length(scenario.DemandProfileSet.demandProfile)
        scenario.DemandProfileSet.demandProfile(i).ATTRIBUTE = renamefield(...
            scenario.DemandProfileSet.demandProfile(i).ATTRIBUTE,'link_id','link_id_origin');
    end
end


% remove from event: demandProfile, qmax, lkid, controller, wfm, control

% added to event: on_off_switch, knob

% remove elements: lkid, wfm, control

% added element: on_off_switch

% added element: knob

% remove: SWARM specific elements (components, zones, zone, onramps, onramp). Put them into swarm.xsd

% remove: usesensors from controller (this is taken care of by feedbackElements)

% remove: limits from controller (put this in individual isolated controllers)

% remove element: limits

% remove: qmax

% changed: default delims in weavingfactor from "," to ":"

% changed: default delims in splitratios from ";,:" to ",:"

% changed: default delims in table from ";,:" to ",:"

% 	+ modified element: links
% 		FROM
% 			  <xs:element name="links">
% 				<xs:complexType mixed="true">
% 				  <xs:attribute name="delims" type="xs:string" use="optional" default="," />
% 				  <xs:attribute name="cellType" type="xs:string" use="optional" default="link" />
% 				</xs:complexType>
% 			  </xs:element>
%
% 		TO
% 			  <xs:element name="links">
% 				<xs:complexType>
% 				  <xos:sequence>
% 					<xs:element ref="link_reference" minOccurs="0" maxOccurs="unbounded" />
% 				  </xs:sequence>
% 				</xs:complexType>
% 			  </xs:element>




% add elelement: link_reference	(advantage is that this can be used outside of a network, ie by PathSegment)
%
% 			  <xs:element name="link_reference">
% 				<xs:complexType>
% 				  <xs:attribute name="network_id" type="xs:string" use="optional" />	<!-- optional only if used within a network -->
% 				  <xs:attribute name="id" type="xs:string" use="required" />
% 				</xs:complexType>
% 			  </xs:element>

% remove element: PathList

% remove element: path

% remove: PathList from od

% rename: demandProfile->network_id -> network_id_origin

% rename: demandProfile->link_id -> link_id_origin

% move: reset_to_nominal from lane_count_change to event

% remove: ODList from network

% added: ODList to scenario

% weaving factors
scenario.WeavingFactorsProfile=[];
c=1;
for i=1:length(scenario.network.NodeList.node)
    for j=1:length(scenario.network.NodeList.node(i).inputs)
        for k=1:length(scenario.network.NodeList.node(i).inputs(j).input)
            inp = scenario.network.NodeList.node(i).inputs(j).input(k);
            if(isfield(inp,'weavingfactors'))
                clear weavingfactors
                weavingfactors.ATTRIBUTE.node_id = scenario.network.NodeList.node(i).ATTRIBUTE.id;
                weavingfactors.CONTENT = writecommaformat(inp.weavingfactors);
                scenario.WeavingFactorsProfile.weavingfactors(c)=weavingfactors;
                c=c+1;
            end
        end
        scenario.network.NodeList.node(i).inputs(j).input = safermfield(scenario.network.NodeList.node(i).inputs(j).input,'weavingfactors');

    end
end
clear inp weavingfactors c

% TEMPORARY: REMOVE WEAVING FACTORS
scenario = safermfield(scenario,'WeavingFactorsProfile');



% added element: od_demandProfile

% modify: od

% make demands into comma separated string
if(hasdemandprofile)
    for i=1:length(scenario.DemandProfileSet.demandProfile)
        scenario.DemandProfileSet.demandProfile(i).CONTENT = ...
        writecommaformat(scenario.DemandProfileSet.demandProfile(i).CONTENT)
    end
end

% remove the DirectionsCache
scenario.network = safermfield(scenario.network,'DirectionsCache');

% move: network to NetworkList
scenario.NetworkList.network = scenario.network;
scenario = safermfield(scenario,'network');

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

%%%%%%%%%%%%%%%%%%%%%%%%%
function [b]=hasfields(X,a,b)
if(~isfield(X,a))
    b=false;
    return
end
b=isfield(X.(a),b);

%%%%%%%%%%%%%%%%%%%%%%%%%
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
FD.ATTRIBUTE.densityJam = densityJam;
FD.ATTRIBUTE.capacityDrop = capacityDrop;
FD.ATTRIBUTE.congestion_speed = capacity/(densityJam-densityCritical);
FD.ATTRIBUTE.freeflow_speed = capacity/densityCritical;
