@echo off
rem apmXmp2Jpg.cmd
if "%exifdir%"=="" set exifdir=C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93
set exe="%exifdir%\exiftool.exe" -P -overwrite_original 

set jpg=%1
set xmp="%~dpn1.xmp"

echo updating %jpg%
%exe% -tagsFromFile %xmp% -@ apmXmp2Jpg.args %jpg%

