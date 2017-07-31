@echo off
rem apmGps.cmd
rem ms-windows batch to set gps lat/lon in jpg or xmp

rem setgps.cmd ["/path/to/image.jpg" [lat lon]]
rem setgps.cmd ["/path/to/image.jpg" [53.2 8.3]]

rem p=preserve-filedate
rem set exe="C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93\exiftool.exe" -P -overwrite_original -ignoreMinorErrors -quiet 
if "%exifdir%"=="" set exifdir=C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93
set exe="%exifdir%\exiftool.exe" -P -overwrite_original 

set lat=%2
set lon=%3

set image="%~dpnx1"

set xmp="%~dpn1.xmp"
rem if NOT EXIST %xmp% set xmp=
rem if "%~x1"==".xmp" set xmp=
set xmp2="%~dpnx1.xmp"
if NOT EXIST %xmp2% set xmp2=
if "%~x1"==".xmp" set xmp2=

IF "%lon:~0,1%"=="0" IF "%lat:~0,1%"=="0" goto removegps 

rem lat/lon starts with "-"
IF "%lat:~0,1%"=="-" (SET latPrefix=S) ELSE (SET latPrefix=N)
IF "%lon:~0,1%"=="-" (SET lonPrefix=W) ELSE (SET lonPrefix=E)

rem echo %image% %xmp% %latPrefix% %lonPrefix%

if "%1" == "" goto usage

if NOT EXIST %image% goto notFound

if "%lon%"=="" goto show

:setgps
rem if xmp does not exist yet create it with the content of the jpg file
rem if NOT EXIST %xmp% echo apmTagsAdd-createxmp %exe% %xmp% -tagsFromFile %image% -@ "%bindir%apmJpg2xmp.args" 
if NOT EXIST %xmp% %exe% %xmp% -tagsFromFile %image% -@ "%bindir%apmJpg2xmp.args" > nul 2> nul

rem -tagsFromFile may have failed, if jpg has no matching meta inside: copy empty xmp
if NOT EXIST %xmp% copy "%bindir%empty.xmp" %xmp% > nul 2> nul

%exe% -c "%%+.6f" -GPSLatitude=%lat% -GPSLatitudeRef=%latPrefix%  -GPSLongitude=%lon% -GPSlongitudeRef=%lonPrefix% %image% %xmp% %xmp2% > nul 2> nul
goto end

:removegps
%exe%  -gps:all=   %image% %xmp% %xmp2%  > nul 2> nul
goto end

:show
echo %image% 
%exe% -c "%%+.6f" -GPSPosition %image% %xmp% %xmp2%

goto end



:notFound
echo ##### file %image% not found >> apmBatchErrors.log
echo ##### %0 %* >> apmBatchErrors.log

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
