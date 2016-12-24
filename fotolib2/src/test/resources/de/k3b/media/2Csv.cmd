@echo off
rem 2Csv.cmd
rem batch to export comment title tags to csv

set exifexe="C:\Progs.Portable\Multimedia\Picture\Image-ExifTool-6.93\exiftool" -quiet 

%exifexe% -c "%%+.6f" -dateFormat "%%Y%%m%%dT%%H%%M%%S" -FileModifyDate -Subject -Description -Title -ImageUniqueID -ImageUniqueID -DocumentID -Rating -csv -r -ext .xmp -if "($Description or $Title or $Subject)" . > media.csv

