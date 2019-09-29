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

public class ProgressFormatter {
    private final long startTime;

    public ProgressFormatter() {
        this(new Date());
    }

    protected ProgressFormatter(Date startTime) {
        this.startTime = startTime.getTime();
    }

    /**
     * scope protexted to ease unittesting
     */
    protected static CharSequence format(long durationInSeconds, int count, int total) {
        StringBuilder result = new StringBuilder();

        result.append(count).append("/").append(total);

        if (durationInSeconds > 10) {
            long totalSeconds = (durationInSeconds * total / count);
            addTime(result, "  ", durationInSeconds);
            addTime(result, "/", totalSeconds);
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
        long durationInSeconds = (new Date().getTime() - startTime) / 1000;
        return format(durationInSeconds, count, total);
    }
}
