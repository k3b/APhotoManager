@echo off
rem apmDateTimeOriginal.cmd
rem ms-windows batch to set photo DateTimeOriginal in jpg and/or xmp

rem apmDateTimeOriginal.cmd ["/path/to/image.jpg" [dateInIsoFormat]]
rem example apmDateTimeOriginal.cmd "/path/to/image.jpg" 20171224T193100

rem -dateFormat "%%Y%%m%%dT%%H%%M%%S" 
rem p=preserve-filedate
rem set exe="C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93\exiftool.exe" -P -overwrite_original -ignoreMinorErrors -quiet 
if "%exifdir%"=="" set exifdir=C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93
set exe="%exifdir%\exiftool.exe" -P -overwrite_original -dateFormat "%%Y%%m%%dT%%H%%M%%S" 

set image="%~dpnx1"
set newValue=%2

rem drive+path+file without original extension
set xmp="%~dpn1.xmp"
if NOT EXIST %xmp% set xmp=
if "%~x1"==".xmp" set xmp=

set xmp2="%~dpnx1.xmp"
if NOT EXIST %xmp2% set xmp2=
if "%~x1"==".xmp" set xmp2=

if "%1" == "" goto usage

if NOT EXIST %image% goto notFound

if "%newValue%"=="" goto show

:apmDateTimeOriginal
%exe% -MWG:DateTimeOriginal=%newValue% %image% %xmp% %xmp2%  > nul 2> nul
goto end

:show
echo %image% 
%exe% -MWG:DateTimeOriginal %image% %xmp% %xmp2%

goto end


:notFound
echo ##### file %image% not found >> apmBatchErrors.log
echo ##### %0 %* >> apmBatchErrors.log

echo file %image% not found

:usage
echo %~nx0 
echo .
echo ms-windows batch to set photo DateTimeOriginal in jpg and/or xmp
echo .
echo usage:
echo %0 [fullpath [DateTimeInIsoFormat]]
echo example %0 "/path/to/image.jpg" 20171224T193100

:end
