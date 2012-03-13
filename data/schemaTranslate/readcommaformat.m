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
end