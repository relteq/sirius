function [D] = readAuroraOutput(inputfile,maxpointspercurve,scope)

if(nargin<3)
    maxpointspercurve = 200;
end

if(nargin<4)
    scope.timeon = true;
    scope.time = [0 24];
end
    
[pathname,inputfile] = fileparts(inputfile);
ready = false;
time = [];

name = [fullfile(pathname,inputfile) '.csv'];
fid = fopen(name);
if(fid<0)
    name = regexprep(name, '\\', '\\\\');
    error('ARG:User',['File does not exist: ' name])
end
havevehtypes = false;
while 1
    tline = fgetl(fid);
    if ~ischar(tline), break, end

    if(strfind(tline,'Vehicle Type') & ~havevehtypes)
        tline = fgetl(fid);
        c=0;
        while ~isempty(tline)
            row = splitbydelimiter(tline,',');
            c = c+1;
            vt(c).name = row{1};
            vt(c).weight = str2num(row{2});
            tline = fgetl(fid);
        end

        havevehtypes = true;
    end

    if(strfind(tline,'Entry Format'))
        % Read format for vehicle types
        vehicletypes = ReadVehicleTypes(fid);

        % order weights as vehicletypes
        for i=1:length(vehicletypes)
            ind = strcmp(vehicletypes(i),{vt.name});
            vehicletypeweight(i) = vt(ind).weight;
        end
        clear vt ind i
        numvehicletypes = size(vehicletypes,1);
        continue
    end

    if(strfind(tline,'Sampling Period'))
        simT = ReadSamplingPeriod(fid);
        continue
    end
    if(strfind(tline,'Description'))
        description = ReadDescription(fid);
        continue
    end
    if(strfind(tline,'Routes'))
        routes = ReadRoutes(fid);
        continue
    end
    if(strfind(tline,'Link ID'))
        Links = ReadLinkConfig(fid,tline);
        continue
    end
    if(strfind(tline,'Time Step'))
        ready = true;
        c=0;
        start = ftell(fid);
        continue
    end

    if(~ready)
        continue
    end

    % Read data line
    row = splitbydelimiter(tline,',');
    newtime = str2num(row{1});
    c = c+1;
    time(c) = newtime;
end
clear newtime c row LinkId havevehtypes ready tline

skip = ceil(length(time)/maxpointspercurve);
clear time
c = 0;
n = -1;
fseek(fid,start,'bof');
while 1
    tline = fgetl(fid);
    if ~ischar(tline), break, end

    n = n+1;
    if(mod(n,skip)~=0)
        continue
    end

    % Read data line
    row = splitbydelimiter(tline,',');
    newtime = str2num(row{1});

    if(scope.timeon & newtime*simT/3600<scope.time(1))
        continue;
    end
    
    if(scope.timeon & newtime*simT/3600>scope.time(2))
        break;
    end
    
    row = {row{2:length(row)}};
    
    c = c+1;
    time(c) = newtime;
    for i=1:size(row,2)
        celldata = splitbydelimiter(row{i},';');
        dens    = splitbydelimiter(celldata{1},':'); 
        outflow = splitbydelimiter(celldata{3},':');
        fd      = splitbydelimiter(celldata{4},':'); 
        qlimit  = splitbydelimiter(celldata{5},':');
        weaving = splitbydelimiter(celldata{6},':');
        for j=1:numvehicletypes
            Density(c,i,j) = str2num(dens{j});
            OutFlow(c,i,j) = str2num(outflow{j});
        end
        Capacity(c,i) = str2num(fd{1});
        Critical_Density(c,i) = str2num(fd{2});
        Jam_Density(c,i) = str2num(fd{3});
        QueueLimit(c,i) = str2num(qlimit{1});
        WeavingFactor(c,i) = str2num(weaving{1});
    end

end
time = time'*simT/3600;
fclose(fid);

clear dens ans c celldata fd fid
clear i inflow j newtime numvehicletypes outflow
clear ready tline plottimestep row saveLinkTypes savelinks
clear saveRoutes n name qlimit weaving 
clear skip start maxpointspercurve

D.description = description;
D.Links = Links;
D.vehicletypes = vehicletypes;
D.vehicletypeweight = vehicletypeweight;
D.time = time;
D.Density =Density;
D.OutFlow = OutFlow;
D.routes = routes;
D.Capacity = Capacity;
D.Critical_Density = Critical_Density;
D.WeavingFactor = WeavingFactor;
D.Jam_Density = Jam_Density;
D.QueueLimit = QueueLimit;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [simT] = ReadSamplingPeriod(fid)
tline = '';
while(isempty(strfind(tline,'seconds')))
    tline = fgetl(fid);
end
row = splitbydelimiter(tline,',');
simT = str2num(row{1});
simT = round(simT*100)/100;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [vehicletypes] = ReadVehicleTypes(fid)
tline = fgetl(fid);
row = splitbydelimiter(tline,',');
vehicletypes = splitbydelimiter(row{2},':');

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [description] = ReadDescription(fid)
tline = fgetl(fid);
description = [];
while ~isempty(tline)
    row = splitbydelimiter(tline,',');
    description = strvcat(description,row{1});
    tline = fgetl(fid);
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [route] = ReadRoutes(fid)
tline = fgetl(fid);
c = 0;
while ~isempty(tline)
    row = splitbydelimiter(tline,',');
    c = c+1;
    route(c).name = row{1};
    route(c).links = [];
    for i=2:size(row,1)
        n = str2num(row{i});
        if(~isempty(n))
            route(c).links(i-1) = n;
        else
            route(c).links(i-1) = nan;
        end
    end
    route(c).links(isnan(route(c).links)) = [];
    tline = fgetl(fid);
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [Link] = ReadLinkConfig(fid,tline)
Id = splitbydelimiter(tline,',');
tline = fgetl(fid);
Name = splitbydelimiter(tline,',');
tline = fgetl(fid);
Type = splitbydelimiter(tline,',');
tline = fgetl(fid);
Length = splitbydelimiter(tline,',');
tline = fgetl(fid);
Lanes = splitbydelimiter(tline,',');
tline = fgetl(fid);
Source = splitbydelimiter(tline,',');
for i=2:size(Id,1)
    Link(i-1).id = str2num(Id{i});
    Link(i-1).name= Name{i};
    switch Type{i}
        case 'Freeway'
            Link(i-1).type = 1;
        case 'On-Ramp'
            Link(i-1).type = 2;
        case 'Off-Ramp'
            Link(i-1).type = 3;
        case 'HOV'
            Link(i-1).type = 4;
        otherwise
            Link(i-1).type = NaN;
    end
    Link(i-1).length = str2num(Length{i});
    Link(i-1).lanes = str2num(Lanes{i});
    Link(i-1).issource = strcmp(Source{i},'Yes');
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [x] = splitbydelimiter(str,d)
if(isempty(str))
    x = NaN;
    return
end
x = textscan(str,'%s','delimiter',d);
x = x{1};