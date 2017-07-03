@echo off
rem apmCopy.cmd
rem ms-windows batch to copy images (jpg together with xmp file)

if "%1" == "" goto usage

set destDir="%~dp2"
set imageSrc="%~dpnx1"
set imageDst="%~dpnx2"

if NOT EXIST %imageSrc% goto fileNotFound
 
if NOT EXIST %destDir% mkdir %destDir%  > nul


rem COPY /y "\storage\sdcard1\DCIM\Camera\IMG_20160528_172009.jpg" "\storage\sdcard0\copy\test\IMG_20160528_172009.jpg"
rem /T /E create dir while copy if neccessary
rem /Y overwrite
rem xcopy /T /E /Y "\storage\sdcard1\DCIM\Camera\IMG_20160528_172009.jpg" "\storage\sdcard0\copy\test\IMG_20160528_172009.jpg"

copy /Y %imageSrc% %imageDst%  > nul

set xmpSrc="%~dpn1.xmp"
set xmpDst="%~dpn2.xmp"

if EXIST %xmpSrc% copy /Y %xmpSrc% %xmpDst% > nul

set xmpSrc="%~dpnx1.xmp"
set xmpDst="%~dpnx2.xmp"

if EXIST %xmpSrc% copy /Y %xmpSrc% %xmpDst% > nul

echo copied %2
goto end

:fileNotFound
echo ##### file not found: %imageSrc% >> apmBatchErrors.log
echo ##### %0 %* >> apmBatchErrors.log
echo file not found: %imageSrc%
echo .

:usage
echo %~nx0 
echo .
echo ms-windows batch to copy images (jpg together with xmp file)
echo .
echo usage:
echo %0 path\to\src.jpg other\path\dst.jpg

:end

