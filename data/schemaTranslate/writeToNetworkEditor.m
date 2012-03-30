function [] = writeToNetworkEditor(outputfilename,scenario)

% properly format all numeric values

% % split ratio profile
% if(hassplits)
%     for i=1:length(scenario.SplitRatioProfileSet.splitratioProfile)
%         SR = scenario.SplitRatioProfileSet.splitratioProfile(i);
%         newSR = SR;
%         newSR.splitratio = [];
%         multupletimesteps = iscell(SR.splitratio);
%         if(multupletimesteps)
%             for j=1:length(SR.splitratio)
%                 newSR.splitratio{j} = class_Utils.writecommaformat(SR.splitratio(j));
%             end
%         else
%             newSR.splitratio{1} = class_Utils.writecommaformat(SR.splitratio);
%         end
%         scenario.SplitRatioProfileSet.splitratioProfile(i) = newSR;
%     end
% end

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

