@echo off
echo %0 %*

rem apmTagsRemove.cmd
rem ms-windows batch to Remove tags from jpg (IPTC:Keywords) and xmp (XMP-dc:Subject)

rem p=preserve-filedate
rem set exe="C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93\exiftool.exe" -P -overwrite_original -ignoreMinorErrors -quiet 
if "%exifdir%"=="" set exifdir=C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93
set exe="%exifdir%\exiftool.exe" 

set image="%~dpnx1"
set xmp="%~dpn1.xmp"
set all="%~dpn1.*"

if NOT EXIST %image% goto fileNotFound

IF "%~n2"=="" goto show

rem if xmp does not exist yet create it with the content of the jpg file
rem if NOT EXIST %xmp% echo apmTagsAdd-createxmp %exe% %xmp% -tagsFromFile %image% -@ "%bindir%apmJpg2xmp.args" 
if NOT EXIST %xmp% %exe% %xmp% -tagsFromFile %image% -@ "%bindir%apmJpg2xmp.args" > nul 2> nul

rem -tagsFromFile may have failed, if jpg has no matching meta inside: copy empty xmp
if NOT EXIST %xmp% copy "%bindir%empty.xmp" %xmp% > nul 2> nul

set iptcParams=
set xmpParams=
:loop
	set iptcParams=%iptcParams% "-IPTC:Keywords-=%~n2" 
	set xmpParams=%xmpParams% "-xmp-dc:subject-=%~n2"

	shift
IF NOT "%~n2"=="" goto loop

rem update tags in xmp file directly
echo apmTagsRemove-updateXmp %exe% -overwrite_original %xmp% %xmpParams%
%exe% -overwrite_original %xmp% %xmpParams%   > nul 2> nul

rem alternative 1
rem update tags in jpg file directly
rem %exe% -P -overwrite_original %image% %iptcParams%

rem alternative 2: use only if xmp is more acurate than jpg (i.e. when using JPhotoTagger)
rem copy tags from xmp file to jpg file
rem echo apmTagsRemove-copyXmp2Jpg %exe% -P -overwrite_original  %image% -tagsFromFile %xmp% "-IPTC:Keywords < XMP-dc:Subject"
%exe% -P -overwrite_original  %image% -tagsFromFile %xmp% "-IPTC:Keywords < XMP-dc:Subject"

goto end

:show
if NOT EXIST %xmp% set xmp=
%exe% -IPTC:Keywords -xmp-dc:subject %image% %xmp%

goto end


:fileNotFound
echo ##### %0 %*
echo file %image% not found
echo ##### %0 %* >> apmBatchErrors.log
echo file %image% not found >> apmBatchErrors.log
goto end

:usage
echo %~nx0 
echo .
echo ms-windows batch to Remove tags from jpg (IPTC:Keywords) and xmp (XMP-dc:Subject)
echo .
echo usage:
echo %0 fullpath.jpg keyword Keyword ... 
echo example %0 "/path/to/image.jpg" job "conference"

:end
rem echo %exe%  %all% -Keywords -Subject
rem %exe%  %all%  -Keywords -Subject
echo .
