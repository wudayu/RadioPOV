[filename, pathname] = uigetfile( ...
    {'*.bmp;*.jpg;*.png;*.jpeg', 'Image Files (*.bmp, *.jpg, *.png,*.jpeg)'; '*.*', 'All Files (*.*)'}, 'Pick an image');

fpath = [pathname filename];
img_src = imread(fpath);
temp = size(img_src);                                 %????
wide = temp(1)/2;                                   %   
height = temp(2)/2;
R=fix(img_src(:,:,1)/16);
G=fix(img_src(:,:,2)/16);
B=fix(img_src(:,:,3)/16);
L = min(wide,height);
for i = 1:256
    ang = 0 + i*pi/128;
    for j = 1:32
        l = L*0.2+L*0.8/32*j;
        midata(i,j*3-2)=B(fix(wide-(l-1)*sin(ang))+1,fix(height+(l-1)*cos(ang))+1);
        midata(i,j*3-1)=G(fix(wide-(l-1)*sin(ang))+1,fix(height+(l-1)*cos(ang))+1);
        midata(i,j*3-0)=R(fix(wide-(l-1)*sin(ang))+1,fix(height+(l-1)*cos(ang))+1);
    end
end
for i = 1:256
    for j = 1:16
        temp(1) = midata(i,17-j);
        temp(2) = midata(i,17-j+16);
        temp(3) = midata(i,17-j+32);
        temp(4) = midata(i,17-j+48);
        temp(5) = midata(i,17-j+64);
        temp(6) = midata(i,17-j+80);
        for k = 1:16
            data(i,j,k)=0;
            for t = 1:6
                if(temp(t)>k)data(i,j,k)=data(i,j,k)+1*2^(t-1);
                end
            end
        end
     end
end

[filename, pathname] = uiputfile({'*.txt'}, '1');
if isequal(filename,0) || isequal(pathname,0)
return;
else
fpath=fullfile(pathname, filename);
end

fid=fopen(fpath,'w')
for i=1:256
    for j=1:16
        for k=1:16
        fprintf(fid,'%d',data(i,k,j));
        fprintf(fid,'%c',',');
        end
    end
    fprintf(fid,'\n');
end
fprintf(fid,'%c','};');
fprintf(fid,'\n');
fclose(fid);

