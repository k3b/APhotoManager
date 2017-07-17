@echo off
rem apmDescription.cmd ["/path/to/image.jpg" [description]]
rem example apmDescription.cmd "/path/to/image.jpg" "some detailed description"

rem p=preserve-filedate
rem set exe="C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93\exiftool.exe" -P -overwrite_original -ignoreMinorErrors -quiet 
if "%exifdir%"=="" set exifdir=C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93
set exe="%exifdir%\exiftool.exe" -P -overwrite_original 

set image="%~dpnx1"
set newValue=%~n2

rem drive+path+file without original extension
set xmp="%~dpn1.xmp"
rem if NOT EXIST %xmp% set xmp=
rem if "%~x1"==".xmp" set xmp=
set xmp2="%~dpnx1.xmp"
if NOT EXIST %xmp2% set xmp2=

if "%1" == "" goto usage

if NOT EXIST %image% goto notFound

if "%newValue%"=="" goto show

:apmDescription

rem if xmp does not exist yet create it with the content of the jpg file
rem if NOT EXIST %xmp% echo apmTagsAdd-createxmp %exe% %xmp% -tagsFromFile %image% -@ "%bindir%apmJpg2xmp.args" 
if NOT EXIST %xmp% %exe% %xmp% -tagsFromFile %image% -@ "%bindir%apmJpg2xmp.args" > nul 2> nul

rem -tagsFromFile may have failed, if jpg has no matching meta inside: copy empty xmp
if NOT EXIST %xmp% copy "%bindir%empty.xmp" %xmp% > nul 2> nul


%exe% "-MWG:Description=%newValue%" "-Comment=%newValue%" "-XPComment=%newValue%" %image% %xmp% %xmp2% > nul 2> nul
goto end

:show
if NOT EXIST %xmp% set xmp=
echo %image% 
%exe% -MWG:Description -Comment -XPComment %image% %xmp% %xmp2%

goto end


:notFound
echo ##### file %image% not found >> apmBatchErrors.log
echo ##### %0 %* >> apmBatchErrors.log

echo file %image% not found

:usage
echo %~nx0 
echo .
echo ms-windows batch to set photo Description in jpg and/or xmp
echo .
echo usage:
echo %0 [fullpath [description]]
echo example %0 "/path/to/image.jpg" "some detailed description"

:end
