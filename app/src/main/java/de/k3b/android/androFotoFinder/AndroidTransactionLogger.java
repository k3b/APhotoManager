/*
 * Copyright (c) 2017 by k3b.
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

package de.k3b.android.androFotoFinder;

import android.app.Activity;

import java.io.Closeable;
import java.io.IOException;

import de.k3b.android.util.AndroidFileCommands;
import de.k3b.transactionlog.MediaTransactionLogEntryType;
import de.k3b.transactionlog.TransactionLoggerBase;

/**
 * Android specific implementation: Writes change infos into log (bat-file and database).
 *
 * Created by k3b on 02.07.2017.
 */

public class AndroidTransactionLogger extends TransactionLoggerBase implements Closeable {
    private AndroidFileCommands execLog;

    public AndroidTransactionLogger(Activity ctx, long now, AndroidFileCommands execLog) {
        super(execLog, now);

        this.execLog = execLog;
    }

    @Override
    protected void addChanges(MediaTransactionLogEntryType command, String parameter, boolean quoteParam) {
        super.addChanges(command, parameter, quoteParam);
        execLog.addTransactionLog(id, path, now, command, parameter);
    }

    @Override
    public void close() throws IOException {
        super.close();
        execLog = null;
    }
}
