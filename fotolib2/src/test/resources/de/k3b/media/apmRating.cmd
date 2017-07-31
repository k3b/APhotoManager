@echo off
rem apmRating.cmd
rem ms-windows batch to set photo Rating in jpg and/or xmp

rem apmRating.cmd ["/path/to/image.jpg" [0 .. 5]]
rem example apmRating.cmd "/path/to/image.jpg" 3

rem p=preserve-filedate
rem set exe="C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93\exiftool.exe" -P -overwrite_original -ignoreMinorErrors -quiet 
if "%exifdir%"=="" set exifdir=C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93
set exe="%exifdir%\exiftool.exe" -P -overwrite_original 

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

:apmRating
%exe% -MWG:Rating=%newValue% %image% %xmp% %xmp2% > nul 2> nul
goto end

:show
echo %image% 
%exe% -MWG:Rating %image% %xmp% %xmp2%

goto end


:notFound
echo ##### file %image% not found >> apmBatchErrors.log
echo ##### %0 %* >> apmBatchErrors.log

echo file %image% not found

:usage
echo %~nx0 
echo .
echo ms-windows batch to set photo Rating in jpg and/or xmp
echo .
echo usage:
echo %0 [fullpath [0 .. 5]]
echo example %0 "/path/to/image.jpg" 3

:end
