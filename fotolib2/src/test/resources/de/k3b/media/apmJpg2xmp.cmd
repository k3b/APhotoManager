@echo off
rem jpg2xmp.cmd
if "%exifdir%"=="" set exifdir=C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93
set exe="%exifdir%\exiftool.exe" -P -overwrite_original 

set jpg=%1
set xmp="%~dpn1.xmp"

echo updating %xmp%
%exe% -tagsFromFile %jpg% -@ jpg2xmp.args %xmp%

