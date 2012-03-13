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

