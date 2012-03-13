function [] = writeToNetworkEditor(outputfilename,scenario)

disp(['Writing ' outputfilename])

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
xml_write(outputfilename,scenario)

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

system(['copy /Y "' outputfilename '" tempfile.xml']);
fin=fopen('tempfile.xml');
fout=fopen(outputfilename,'w+');
while 1
    tline = fgetl(fin);
    if ~ischar(tline), break, end
    for i=1:length(replace)
        if(~isempty(strfind(tline,replace(i).from)))
            tline=strrep(tline,replace(i).from,replace(i).to);
        end
    end
    fwrite(fout,sprintf('%s\n',tline));
end

fclose(fin);
fclose(fout);

system('del tempfile.xml');