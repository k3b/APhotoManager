/*
 * Copyright (c) 2021 by k3b.
 *
 * This file is part of AndroFotoFinder.
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

package de.k3b.android.util;

import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.IMediaRepositoryApi;
import de.k3b.io.IProgessListener;

public class ScannerTaskFactory {
    public static RecursivePhotoPropertiesMediaFilesScannerAsyncTask createScannerTask(
            String why, PhotoPropertiesMediaFilesScanner scanner,
            boolean fullScan, boolean rescanNeverScannedByAPM, boolean scanForDeleted,
            IProgessListener progessListener) {
        RecursivePhotoPropertiesMediaFilesScannerAsyncTask newScanner;
        if (rescanNeverScannedByAPM && Global.useAo10MediaImageDbReplacement && FotoSql.getMediaLocalDatabase() != null) {
            IMediaRepositoryApi mediaDBApi = FotoSql.getMediaLocalDatabase();
            Date dateLastAdded = Ao10DbUpdateOnlyPhotoPropertiesMediaFilesScannerAsyncTask.loadDateLastAdded(scanner.mContext);
            newScanner = new Ao10DbUpdateOnlyPhotoPropertiesMediaFilesScannerAsyncTask(
                    mediaDBApi, scanner, scanner.mContext, why,
                    dateLastAdded, progessListener);
        } else {
            newScanner = new RecursivePhotoPropertiesMediaFilesScannerAsyncTask(
                    scanner, scanner.mContext, why,
                    fullScan, rescanNeverScannedByAPM, scanForDeleted, progessListener);
        }
        return newScanner;
    }

}
