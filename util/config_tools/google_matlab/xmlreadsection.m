function [nodeStruct]=xmlreadsection(xmlfile,sectionname)

names = strread(sectionname,'%s','delimiter','>');
A = xmlread(xmlfile);
X = drillintonode(A,names);
nodeStruct = xmldom2struct(X);

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function X = drillintonode(A,names)

if(isempty(names))
    X = A;
    return    
end
    
X = [];
C = A.getChildNodes;
for i = 0:C.getLength-1
    if(strcmp(C.item(i).getNodeName,names{1}))
        X = drillintonode(C.item(i),names(2:end));
    end
end
