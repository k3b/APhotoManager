/*
 * Copyright 2018 The Android Open Source Project
 * Copyright 2020 Tom Geiselmann <tomgapplicationsdevelopment@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.tommygeenexus.exifinterfaceextended;

import androidx.annotation.NonNull;

// A class for indicating EXIF rational type.
// TODO: b/308978831 - Migrate to android.util.Rational when the min API is 21.
class Rational {

    private final long mNumerator;
    private final long mDenominator;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Rational(long numerator, long denominator) {
        // Handle erroneous case
        if (denominator == 0) {
            mNumerator = 0;
            mDenominator = 1;
            return;
        }
        mNumerator = numerator;
        mDenominator = denominator;
    }

    /**
     * Creates a new {@code Rational} which approximates the provided {@code double} value by
     * using <a href="https://en.wikipedia.org/wiki/Continued_fraction">continued fractions</a>.
     */
    @NonNull
    public static Rational createFromDouble(double value) {
        if (value >= Long.MAX_VALUE || value <= Long.MIN_VALUE) {
            // value is too large to represent as a long, so just return the max/min value.
            return new Rational(
                    /* numerator= */ value > 0 ? Long.MAX_VALUE : Long.MIN_VALUE,
                    /* denominator= */ 1);
        }

        double absoluteValue = Math.abs(value);
        double threshold = 0.00000001 * absoluteValue;
        double remainingValue = absoluteValue;
        long numerator = 1;
        long previousNumerator = 0;
        long denominator = 0;
        long previousDenominator = 1;
        do {
            double remainder = remainingValue % 1;
            long wholePart = (long) (remainingValue - remainder);
            long tmp = numerator;
            numerator = wholePart * numerator + previousNumerator;
            previousNumerator = tmp;

            tmp = denominator;
            denominator = wholePart * denominator + previousDenominator;
            previousDenominator = tmp;

            remainingValue = 1 / remainder;
        } while ((Math.abs(absoluteValue - numerator / (double) denominator) > threshold));

        return new Rational(value < 0 ? -numerator : numerator, denominator);
    }

    @Override
    @NonNull
    public String toString() {
        return mNumerator + "/" + mDenominator;
    }

    public double calculate() {
        return (double) mNumerator / mDenominator;
    }

    public long getNumerator() {
        return mNumerator;
    }

    public long getDenominator() {
        return mDenominator;
    }
}
