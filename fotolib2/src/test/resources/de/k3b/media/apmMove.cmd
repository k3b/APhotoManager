@echo off
rem apmMove.cmd
rem ms-windows batch to move images (jpg together with xmp file)

if "%1" == "" goto usage

set destDir="%~dp2"
set imageSrc="%~dpnx1"
set imageDst="%~dpnx2"

if NOT EXIST %imageSrc% goto fileNotFound

if EXIST %imageDst% goto fileAlreadyExists
 
if NOT EXIST %destDir% mkdir %destDir%  > nul

move %imageSrc% %imageDst%  > nul

set xmpSrc="%~dpn1.xmp"
set xmpDst="%~dpn2.xmp"

if EXIST %xmpSrc% move %xmpSrc% %xmpDst% > nul

set xmpSrc="%~dpnx1.xmp"
set xmpDst="%~dpnx2.xmp"

if EXIST %xmpSrc% move %xmpSrc% %xmpDst% > nul

echo moved %2
goto end

:fileAlreadyExists
echo ##### file alreay exists: %imageDst% >> apmBatchErrors.log
echo ##### %0 %* >> apmBatchErrors.log
echo file alreay exists: %imageDst%
echo .

goto usage

:fileNotFound
echo ##### file not found: %imageSrc% >> apmBatchErrors.log
echo ##### %0 %* >> apmBatchErrors.log
echo file not found: %imageSrc%
echo .

:usage
echo %~nx0 
echo .
echo ms-windows batch to move images (jpg together with xmp file)
echo .
echo usage:
echo %0 path\to\src.jpg other\path\dst.jpg

:end

