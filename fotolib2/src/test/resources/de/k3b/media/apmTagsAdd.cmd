@echo off
rem apmTagsAdd.cmd
rem ms-windows batch to add tags to jpg (IPTC:Keywords) and xmp (XMP-dc:Subject)

rem p=preserve-filedate
rem set exe="C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93\exiftool.exe" -P -overwrite_original -ignoreMinorErrors -quiet 
if "%exifdir%"=="" set exifdir=C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93
set exe="%exifdir%\exiftool.exe" -P -overwrite_original 

set image="%~dpnx1"
set xmp="%~dpn1.xmp"
set bindir=%~dp0

if NOT EXIST %image% goto fileNotFound

IF "%~n2"=="" goto show

set iptcParams=
set xmpParams=

rem if xmp does not exist yet create it with the content of the jpg file
if NOT EXIST %xmp% set xmpParams=-tagsFromFile %image% -@ "%bindir%apmJpg2xmp.args" 


:loop
	set iptcParams=%iptcParams% "-IPTC:Keywords-=%~n2" "-IPTC:Keywords+=%~n2"
	set xmpParams=%xmpParams% "-xmp-dc:subject-=%~n2" "-xmp-dc:subject+=%~n2"

	shift
IF NOT "%2"=="" goto loop

%exe% %image% %iptcParams%
%exe% %xmp% %xmpParams%

goto end

:show
if NOT EXIST %xmp% set xmp=
%exe% -IPTC:Keywords -xmp-dc:subject %image% %xmp%

goto end


:notFound
echo file %image% not found

:usage
echo %~nx0 
echo .
echo ms-windows batch to add tags to jpg (IPTC:Keywords) and xmp (XMP-dc:Subject)
echo .
echo usage:
echo %0 [fullpath [lat lon]]
echo example %0 "/path/to/image.jpg" 50.2 -1.2

:end
