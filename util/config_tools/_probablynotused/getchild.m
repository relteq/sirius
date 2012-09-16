function [O]=getchild(I,g,str)

O=[];
for i=1:length(str)
    
    ind = [];
    switch g{i}
        case 'C'
            if(~isempty(I.Children))
                ind = strcmp({I.Children.Name},str{i});
            end
        case 'A'
            if(~isempty(I.Attributes))
                ind = strcmp({I.Attributes.Name},str{i});
            end
    end
    if(~any(ind))
        return
    end
    
    switch g{i}
        case 'C'
            I = I.Children(ind);
        case 'A'
            I = I.Attributes(ind);
    end
    
    
end
O = I;
