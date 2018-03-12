@echo off
rem apmMoveDir.cmd
rem ms-windows batch to rename folder (with or without images)

if "%1" == "" goto usage

set destDir="%~dp2"
set imageSrc="%~dpnx1"
set imageDst="%~dpnx2"

if NOT EXIST %imageSrc% goto fileNotFound

if EXIST %imageDst% goto fileAlreadyExists
 
move %imageSrc% %imageDst%  > nul

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
echo ms-windows batch to rename folder (with or without images)
echo .
echo usage:
echo %0 path\to\src.jpg other\path\dst.jpg

:end

