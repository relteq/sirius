function [P]=decodepolyline(instruct)
% extract data from Google directions

c = 0;
while(~isempty(instruct))
        
    I = instruct(1);
    instruct = instruct(2:end);

    
    if(isempty(I.From))
        continue
    end
    fromlat = I.From.ALatLng.ATTRIBUTE.lat;
    fromlng = I.From.ALatLng.ATTRIBUTE.lng;
    
    if(isempty(I.To))
        continue
    end
    tolat = I.To.ALatLng.ATTRIBUTE.lat;
    tolng = I.To.ALatLng.ATTRIBUTE.lng;
    

    str = I.EncodedPolyline.Points;
    if(isempty(str))
        continue
    end

    c = c+1;
    
    P(c).points = decodeLine(str);
    P(c).from = [fromlat fromlng];
    P(c).to = [tolat tolng];
        
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

