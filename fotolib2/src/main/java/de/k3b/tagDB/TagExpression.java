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

package de.k3b.tagDB;

/**
 * Created by k3b on 23.07.2017.
 */

public class TagExpression {
    // i.e. includePaths(...,"a,b/c");
    private static final String DELIMITER_SUB_EXPR = ",;:|";
    static final String DELIMITER_PATH = "\\/";
    private static final String REGEXP_SUB_EXPR = "[" + DELIMITER_SUB_EXPR + "]+";
    private static final String REGEXP_PATH_SPLIT = "[" + DELIMITER_PATH + "]+";

    static String[] getSubExpressions(String expressions) {
        return (expressions != null) ? expressions.split(REGEXP_SUB_EXPR) : null;
    }

    static String[] getPathElemens(String fullPath) {
        return (fullPath != null) ? fullPath.split(REGEXP_PATH_SPLIT) : null;
    }

    public static String[] getPathElemensFromLastExpr(String expressionsString) {
        String[] expressionArray = (expressionsString != null) ? getSubExpressions(expressionsString) : null;
        String[] pathArray = ((expressionArray != null) && (expressionArray.length > 0))  ? getPathElemens(expressionArray[expressionArray.length - 1]) : null;
        return ((pathArray != null) && (pathArray.length > 0)) ? pathArray : null;
    }


}
