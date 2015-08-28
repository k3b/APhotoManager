/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of LocationMapViewer.
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

package de.k3b.util;

import java.util.ListIterator;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Engine that replaces all occurences of ${name} with corresponding values from callback.<br/>
 * new StringTemplate(valueResolver).format("hello ${first.name}!") will produce
 * "hello world!" if valueResolver.get("first","name") returns "world"
 * becomes "hello world!"
 *
 * Created by k3b on 01.04.2015.
 */
public class StringTemplateEngine {
    protected IValueResolver valueResolver = null;
    protected Stack<String> debugStack = null; // new Stack<String>();
    private StringBuilder errors = null;


    // ${a.b}
    private static final Pattern pattern = Pattern.compile("\\$\\{([^ \\.\\}]+)\\.([^ }]+)\\}");

    /** return strue, if value contains tempate parameters */
    protected boolean hasParameters(String value) {
        return ((value != null) && (value.contains("${")));
    }

    public StringTemplateEngine sedDebugEnabled(boolean enabled) {
        if (enabled) {
            this.debugStack = new Stack<String>();
            errors = new StringBuilder();
        } else {
            this.debugStack = null;
            errors = null;
        }
        return this;
    }

    public interface IValueResolver {
        String get(String className, String propertyName, String templateParameter);
    }

    public StringTemplateEngine(IValueResolver valueResolver) {
        this.valueResolver = valueResolver;
    }

    public String format(String template) {
        if (template == null) {
            return null;
        }

        final StringBuffer buffer = new StringBuffer();
        final Matcher match = pattern.matcher(template);
        while (match.find()) {
            final String templateParameter = match.group(0);
            final String className = match.group(1);
            final String propertyName = match.group(2);
            debugPush(templateParameter);
            String resolverResult = valueResolver.get(className, propertyName, templateParameter);
            resolverResult = onResolverResult(templateParameter, resolverResult);
            if (resolverResult != null) {
                match.appendReplacement(buffer, resolverResult);
            } else {
                match.appendReplacement(buffer, "");
            }
            debugPop();
        }
        match.appendTail(buffer);
        return buffer.toString();
    }

    protected String onResolverResult(String templateParameter, String resolverResult) {
        // log error if resolve failed
        if ((errors != null) && (resolverResult == null)) {
            this.errors
                    .append(templateParameter)
                    .append(" not found in ")
                    .append(getDebugStackTrace())
                    .append("\n");
        }

        return resolverResult;
    }

    protected void debugPush(String parameter) {
        if (debugStack != null) {
            if (debugStack.contains(parameter)) {
                final String errorMessage = this.getClass().toString() +
                        ".format() : " + parameter +
                        " - endless recursion detected " + getDebugStackTrace();
                if (errors != null) {
                    this.errors
                            .append(errorMessage)
                            .append("\n");
                }
                throw new StackOverflowError(errorMessage);
            }
            debugStack.push(parameter);
        }
    }

    protected void debugPop() {
        if (debugStack != null) {
            debugStack.pop();
        }
    }

    protected String getDebugStackTrace() {
        StringBuilder result = new StringBuilder();
        if (debugStack != null) {
            ListIterator<String> iter = debugStack.listIterator();
            if (iter.hasNext()) {
                result.append(" > ").append(iter.next());
            }
        }
        return result.toString();
    }

    public String getErrors() {
        if (errors == null) return null;

        final String result = errors.toString();
        errors.setLength(0);
        return result;
    }
}
