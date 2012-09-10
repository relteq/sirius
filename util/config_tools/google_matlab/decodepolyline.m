function [P]=decodepolyline(instruct)

c = 0;
while(~isempty(instruct))
    
    I = instruct(1);
    if(strcmp(I.Name,'#text'))
        instruct = instruct(2:end);
        continue
    end
    
    Z = getchild(I,{'C','C'},{'From','ALatLng'});
    ind = strcmp({Z.Attributes.Name},'lat');
    fromlat = str2double(Z.Attributes(ind).Value);
    ind = strcmp({Z.Attributes.Name},'lng');
    fromlng = str2double(Z.Attributes(ind).Value);
    
    Z = getchild(I,{'C','C'},{'To','ALatLng'});
    ind = strcmp({Z.Attributes.Name},'lat');
    tolat = str2double(Z.Attributes(ind).Value);
    ind = strcmp({Z.Attributes.Name},'lng');
    tolng = str2double(Z.Attributes(ind).Value);

    Z = getchild(I,{'C','C','C'},{'EncodedPolyline','Points','#text'});
    str = Z.Data;

    c = c+1;
    
    P(c).points = decodeLine(str);
    P(c).from = [fromlat fromlng];
    P(c).to = [tolat tolng];
    
    instruct = instruct(2:end);
    
end



%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [array]=decodeLine(encoded)

len = length(encoded);
index = 1;
array = [];
lat = 0;
lng = 0;

while (index <= len)
    shift = uint8(0);
    result = uint64(0);
    while(1)
        b = encoded(index) - 63;
        index = index+1;
        z = bitsll( cast(bitand(b,uint8(31)),'int64') , shift);
        result =  bitor(result,uint64(z)) ;
        shift = shift + 5;
        if(b<32)
            break
        end
    end
    
    if( bitand(result,uint64(1)) )
        dlat = -double(bitsrl(result,uint64(1)))-1;
    else
        dlat = double(bitsrl(result,uint64(1)));
    end
    
    lat = lat + dlat;
    
    shift = uint8(0);
    result = uint64(0);
    while(1)
        b = encoded(index) - 63;
        index = index + 1;
        z = bitsll( cast(bitand(b,uint8(31)),'int64') , shift);
        result =  bitor(result,uint64(z)) ;
        shift = shift + 5;
        if(b<32)
            break
        end
    end
    
    if(bitand(result,uint64(1)))
        dlng = -double(bitsrl(result,uint64(1)))-1;
        
    else
        dlng = double(bitsrl(result,uint64(1)));
        
    end
    
    lng = lng + dlng;
    
    array = [array ;[ ([lat * 1e-5, lng * 1e-5])]];
    
end

