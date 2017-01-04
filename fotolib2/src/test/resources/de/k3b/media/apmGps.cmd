@echo off
rem apmGps.cmd
rem ms-windows batch to set gps lat/lon in jpg or xmp

rem setgps.cmd ["/path/to/image.jpg" [lat lon]]
rem setgps.cmd ["/path/to/image.jpg" [53.2 8.3]]

rem p=preserve-filedate
rem set exe="C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93\exiftool.exe" -P -overwrite_original -ignoreMinorErrors -quiet 
if "%exifdir%"=="" set exifdir=C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93
set exe="%exifdir%\exiftool.exe" -P -overwrite_original 

set image="%~dpnx1"
set lat=%2
set lon=%3

rem drive+path+file without original extension
set xmp="%~dpn1.xmp"
if NOT EXIST %xmp% set xmp=
if "%~x1"==".xmp" set xmp=

rem lat/lon starts with "-"
IF "%lat:~0,1%"=="-" (SET latPrefix=S) ELSE (SET latPrefix=N)
IF "%lon:~0,1%"=="-" (SET lonPrefix=W) ELSE (SET lonPrefix=E)

rem echo %image% %xmp% %latPrefix% %lonPrefix%

if "%1" == "" goto usage

if NOT EXIST %image% goto notFound

if "%lon%"=="" goto show

:setgps
%exe% -c "%%+.6f" -GPSLatitude=%lat% -GPSLatitudeRef=%latPrefix%  -GPSLongitude=%lon% -GPSlongitudeRef=%lonPrefix% %image% %xmp%
goto end

:show
echo %image% 
%exe% -c "%%+.6f" -GPSPosition %image% %xmp%

goto end


:notFound
echo file %image% not found

:usage
echo %~nx0 
echo .
echo ms-windows batch to set gps lat/lon in jpg or xmp
echo .
echo usage:
echo %0 [fullpath [lat lon]]
echo example %0 "/path/to/image.jpg" 50.2 -1.2

:end
