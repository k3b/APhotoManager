@echo off
rem ms-windows batch um per exiftool gps lat und lon zu setzen

rem setgps.cmd ["/path/to/image.jpg" [lat lon]]
rem setgps.cmd ["/path/to/image.jpg" [53.2 8.3]]

rem p=preserve-filedate
set exe="C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93\exiftool.exe" -P -overwrite_original -ignoreMinorErrors -quiet 

set image=%1
set lat=%2
set lon=%3

rem drive+path+file without original extension
set xmp="%~dpn1.xmp"
if NOT EXIST %xmp% set xmp=

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
%exe% -c "%%+.6f" -GPSPosition %image% %xmp%

goto end


:notFound
echo file %image% not found

:usage
echo usage:
echo setgps [fullpath [lat lon]]
echo example setgps "/path/to/image.jpg" 50.2 -1.2

:end

rem test data
rem D:\prj\eve\android\prj\fotos-android.wrk\FotoGallery\doc\t.jpg
rem t D:\prj\eve\android\prj\fotos-android.wrk\FotoGallery\doc\t.jpg 53.4 2.3
rem t /prj/eve/android/prj/fotos-android.wrk/FotoGallery/doc/t.jpg 53.4 2.3
