@echo off
rem apmMove.cmd
rem ms-windows batch to delete images (jpg together with xmp file)

if "%1" == "" goto usage

set imageSrc="%~dpnx1"

if NOT EXIST %imageSrc% goto fileNotFound

del %imageSrc%  > nul

set xmpSrc="%~dpn1.xmp"

if EXIST %xmpSrc% del %xmpSrc% > nul

set xmpSrc="%~dpnx1.xmp"

if EXIST %xmpSrc% del %xmpSrc% > nul

echo deleted %1
goto end

:fileNotFound
echo ##### file not found: %imageSrc% >> apmBatchErrors.log
echo ##### %0 %* >> apmBatchErrors.log

echo file not found: %imageSrc%
echo .

:usage
echo %~nx0 
echo .
echo 
echo .
echo usage:
echo %0 path\to\src.jpg

:end
