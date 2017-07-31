@echo off
rem apmTagsAdd.cmd
rem ms-windows batch to add tags to jpg (IPTC:Keywords) and xmp (XMP-dc:Subject)

rem p=preserve-filedate
rem set exe="C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93\exiftool.exe" -P -overwrite_original -ignoreMinorErrors -quiet 
if "%exifdir%"=="" set exifdir=C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93
set exe="%exifdir%\exiftool.exe" -P -overwrite_original 

set image="%~dpnx1"

set xmp="%~dpn1.xmp"
rem if NOT EXIST %xmp% set xmp=
rem if "%~x1"==".xmp" set xmp=
set xmp2="%~dpnx1.xmp"
if NOT EXIST %xmp2% set xmp2=
if "%~x1"==".xmp" set xmp2=

set all="%~dpn1.*"

set bindir=%~dp0

if NOT EXIST %image% goto fileNotFound

IF "%~n2"=="" goto show

rem if xmp does not exist yet create it with the content of the jpg file
rem if NOT EXIST %xmp% echo apmTagsAdd-createxmp %exe% %xmp% -tagsFromFile %image% -@ "%bindir%apmJpg2xmp.args" 
if NOT EXIST %xmp% %exe% %xmp% -tagsFromFile %image% -@ "%bindir%apmJpg2xmp.args" > nul 2> nul

rem -tagsFromFile may have failed, if jpg has no matching meta inside: copy empty xmp
if NOT EXIST %xmp% copy "%bindir%empty.xmp" %xmp% > nul 2> nul

rem from http://stackoverflow.com/questions/42365424/exiftool-prevent-duplicate-iptckeywords-xmp-dcsubject-when-using-add
rem exiftool.exe -if "not $keywords =~ \"holliday\""  -IPTC:Keywords+=holliday -execute -if "not $keywords =~ \"ocean\""  -IPTC:Keywords+=ocean -common_args myImage.jpg

set mwgParams=
:loop
	set mwgParams=%mwgParams% "-MWG:Keywords-=%~n2" "-MWG:Keywords+=%~n2"
	
	shift
IF NOT "%~n2"=="" goto loop

%exe% %mwgParams%  %image% %xmp% %xmp2% > nul 2> nul

goto end

:show
%exe% -MWG:Keywords %image% %xmp% %xmp2% 

goto end


:fileNotFound
echo ##### file %image% not found
echo ##### %0 %* >> apmBatchErrors.log
echo file %image% not found >> apmBatchErrors.log
goto end

:usage
echo %~nx0 
echo .
echo ms-windows batch to add tags to jpg (IPTC:Keywords) and xmp (XMP-dc:Subject)
echo .
echo usage:
echo %0 fullpath.jpg keyword Keyword ... 
echo example %0 "/path/to/image.jpg" job "conference"

:end
rem echo %exe%  %all% -Keywords -Subject
rem %exe%  %all%  -Keywords -Subject

