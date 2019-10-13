/*
 * Copyright (c) 2019 by k3b.
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
package de.k3b.zip;

import java.util.Date;

/**
 * Displayed Textmessage with progress information while "Backup to zip" is active
 * <p>
 * Example: 870/1126 00:00:17/00:00:22
 * Meaning 870 of 1126 files processed; 0 hours o minutes and 17 seconds of estimated 22 seconds
 */
public class ProgressFormatter {
    private final long timeStart;

    // xxxLooping is set when count parameter in format is not 0 for the first time
    private long timeLoopingStart;
    private long countLoopingStart;
    private long durationLoopingStart;

    public ProgressFormatter() {
        this.timeStart = nowInMillisecs();
    }

    /**
     * scope protected to ease unittesting
     *
     * @param durationInSecondsSinceLoopStart time spend excluding durationLoopingStart
     * @param count how many items have already been processed (including countLoopingStart)
     * @param total (how many item will be processed)
     * @param durationLoopingStart (how much time has gone by before estimated total calculation started)
     * @param countLoopingStart (how many items where processed before estimated total calculation started)
     * @return formatted string
     */
    protected static CharSequence format(long durationInSecondsSinceLoopStart, int count, int total, long durationLoopingStart, long countLoopingStart) {
        StringBuilder result = new StringBuilder();

        result.append(count).append("/").append(total);

        if ((count > countLoopingStart) && (durationInSecondsSinceLoopStart > 10)) {
            long totalSeconds = ((durationInSecondsSinceLoopStart) * (total - countLoopingStart) / (count - countLoopingStart));
            addTime(result, "  ", durationInSecondsSinceLoopStart + durationLoopingStart);
            addTime(result, "/", totalSeconds + durationLoopingStart);
        }
        return result;
    }

    private static void addTime(StringBuilder result, String prefix, long seconds) {
        result.append(prefix);

        int index = 0;
        long factor = 3600;
        long value;
        String delimiter = "";
        while (index < 3) {
            value = seconds / factor;
            result.append(delimiter);
            if (value < 10) result.append("0");
            result.append(value);
            seconds -= (factor * value);
            index++;
            factor /= 60;
            delimiter = ":";
        }
    }

    public CharSequence format(int count, int total) {
        long now = nowInMillisecs();
        long durationInSecondsSinceLoopStart = 0;

        if (count > 0) {
            if (this.countLoopingStart == 0) {
                this.countLoopingStart = count;
                this.timeLoopingStart = now;
                this.durationLoopingStart = (now - timeStart) / 1000;
            }
            durationInSecondsSinceLoopStart = (now - durationLoopingStart) / 1000;
        }
        return format(durationInSecondsSinceLoopStart, count, total, this.durationLoopingStart, this.countLoopingStart);
    }

    protected long nowInMillisecs() {
        return new Date().getTime();
    }
}
