/*
 * Copyright (c) 2018-2020 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */

/**
 * Backup to Zip implementation
 * <p>
 * * {@link de.k3b.android.androFotoFinder.backup.BackupActivity}: Define Filter and output-zip file
 * * {@link de.k3b.android.androFotoFinder.backup.BackupProgressActivity} : Showing prgress while compression is executed
 * * {@link de.k3b.android.androFotoFinder.backup.BackupAsyncTask} : Executes {@link de.k3b.android.androFotoFinder.backup.Backup2ZipService} in Background
 * * {@link de.k3b.android.androFotoFinder.backup.Backup2ZipService} : Collects Files to backed up from database via Filter
 * * {@link de.k3b.android.androFotoFinder.backup.ApmZipCompressJob} : Executes the compression (with android specific Filesystem)
 * * {@link de.k3b.zip.CompressJob} : Executes the compression (with android independant implementation)
 * * {@link de.k3b.android.androFotoFinder.backup.BackupProgressActivity} : Data containing compression progress
 * * {@link de.k3b.io.IProgessListener} : android independant compression progress
 **/
package de.k3b.android.androFotoFinder.backup;

/*

This is a PlantUML diagram


@startuml
title Backup to Zip

[*] --> Gallery
[*] --> Filter
[*] --> DirPicker

Gallery --> Backup_to_zip : < selected\n files
Gallery --> Backup_to_zip : current\n filter
Filter --> Backup_to_zip
DirPicker --> Backup_to_zip : contextmenu\n current directory
@enduml

@startuml
title Backup to Zip

[*] --> BackupActivity

BackupActivity --> BackupProgressActivity : start
BackupActivity :  Zip File
BackupActivity : Filter
note right of BackupActivity : Edit parameters

BackupProgressActivity --> BackupAsyncTask : start asynch
BackupProgressActivity : Cancel Button
BackupProgressActivity : Progressbar
BackupProgressActivity : Status Message (ProgressData)
BackupAsyncTask --> Backup2ZipService  : start
BackupAsyncTask: IProgessListener -> ProgressData

Backup2ZipService --> ApmZipCompressJob : Photo Files
Backup2ZipService : Database + Filter --> Photo Files


ApmZipCompressJob -[#blue]-> Backup2ZipService : IProgessListener
ApmZipCompressJob : Zip File with Photo Files

Backup2ZipService -[#blue]-> BackupAsyncTask : IProgessListener
BackupAsyncTask -[#blue]-> BackupProgressActivity : async ProgressData

BackupProgressActivity -[#blue]-> BackupActivity : finish

@enduml

*/