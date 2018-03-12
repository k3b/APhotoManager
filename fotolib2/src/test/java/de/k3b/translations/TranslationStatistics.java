/*
 * Copyright (c) 2018 by k3b.
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
 *
 * for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package de.k3b.translations;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import de.k3b.io.DateUtil;
import de.k3b.io.FileUtils;
import de.k3b.io.ListUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Creates a translation statistics in Markdown-format for the app from
 *
 * * ".../app/src/main/res/values-* /strings.xml" and html-pages.xml
 * * ".../app/src/debug/res/values-* /fdroid.xml"
 * * ".../fastlane/metadata/android/ * /full_description.txt"
 *
 * Created by k3b on 15.01.2018.
 */

public class TranslationStatistics {
    private static final String defaultLocaleFolderName = "values-";
    static final Pattern PATTERN_ANDROID_RES_STRING_LOCALE = Pattern.compile(defaultLocaleFolderName+".*");
    private static final XPathExpression stringNodesExpression = getStringsXPathExpression("/resources/string/@name");
    public final File root = getAppRootFolder(Pattern.compile("^app$"));
    private final File iniFile = new File(root,"translation-history.ini");

    private final File resRoot = new File(root, "app/src/main/res");
    private final File fdroidRoot = new File(root, "app/src/debug/res");
    private final File fastlaneRoot = new File(root, "fastlane/metadata/android");
    final LocaleInfo english = getLocaleInfo(new File(resRoot, "values"));

    final Properties lastLocales = new Properties();
    final Date fileLimitDate;

    public final Formatter formatterIni = new Formatter("",";","", "\n", true){
        @Override public CharSequence toString(LocaleInfo item, LocaleInfo reference) {
            String result = super.toString(item, reference).toString();
            return result.replaceFirst(";","=");
        }

        @Override public CharSequence toString(LocaleInfos infos, LocaleInfo reference) {
            StringBuilder result = new StringBuilder()
                    .append("# ").append(super.toString(infos, reference))
                    .append(formatterIni.newLine).append(formatterIni.newLine)
                    .append("ignore").append("=").append(DateUtil.toIsoDateString(new Date()))
                    ;
            return result;
        }
    };


    public static final Formatter formatterMarkdown = new Formatter("| "," | "," |", "\n", false){
        @Override public CharSequence createHeader() {
            String underscore = "---";
            return super.createHeader() + newLine + super.toString(underscore,underscore,underscore,underscore,underscore,underscore,underscore);
        }

    } ;

    public static class Formatter {
        private final String prefix  ;
        private final String infix   ;
        private final String postfix ;
        public final String newLine;
        private final boolean addMissing;

        public Formatter(String prefix, String infix, String postfix, String newLine, boolean addMissing) {
            this.prefix  = prefix  ;
            this.infix   = infix   ;
            this.postfix = postfix ;
            this.newLine = newLine;
            this.addMissing = addMissing;
        }

        public CharSequence createHeader() {
            return toString("language","changed","translated by","app","aboutbox","fdroid", "missing");
        }

        public CharSequence toString(LocaleInfo item, LocaleInfo reference) {
            if (reference == null) reference = new LocaleInfo();

            int fdroid = item.fdroid + item.fastlane;
            int fdroidExpected = 2 + 1; // assume short_description.txt+full_description.txt as complete even if title.txt is missing

            String[] diffArray = null;
            if (addMissing) {
                ArrayList<String> diff = new ArrayList<String>();

                for (String s : reference.stringNames) {
                    if (item.stringNames.indexOf(s) < 0) diff.add(s);
                }

                diffArray = ListUtils.asStringArray(diff);
                if (diffArray != null) Arrays.sort(diffArray);
            }
            return toString(item.locale, DateUtil.toIsoDateString(item.lastModified),
                    item.translators, asValue(item.strings, reference.strings), item.html,
                    asValue(fdroid, fdroidExpected), ListUtils.toString(", ", (Object[]) diffArray));
        }

        public CharSequence toString(LocaleInfos infos, LocaleInfo reference) {
            StringBuilder result = new StringBuilder();
            result.append(createHeader()).append(newLine);
            // System.out.printf("**" + this.getClass().getSuperclass().getName() + "**\n" + HEADER);

            String[] keys = infos.keySet().toArray(new String[infos.size()]);
            Arrays.sort(keys);
            for (String key : keys) {
                LocaleInfo info = infos.get(key);
                result.append(toString(info, reference)).append(newLine);
            }

            return result;
        }

        private CharSequence toString(Object locale, Object lastModified, Object translators, Object strings, Object html, Object fdroid, Object missing) {
            return prefix +
                    locale + infix +
                    lastModified + infix +
                    strings + infix +
                    fdroid + infix +
                    html + infix +
                    translators +
                    (addMissing ? infix +missing : "")+
                    postfix
                    ;
        }

        private String asValue(int value, int refVaulue) {
            if (refVaulue > 0) {
                int percent = value * 100 / refVaulue;
                if (percent >= 100)
                    return "100%";
                return percent + "% ("+value+"/"+refVaulue+")";
            }
            return "" + value;
        }
    }

    private static class LocaleInfo {
        String locale = null;
        Date lastModified = null;
        String translators = "";
        int strings = 0;
        int html = 0;
        int fdroid = 0;
        int fastlane = 0;
        List<String> stringNames = new ArrayList<String>();
    }

    private static class LocaleInfos extends HashMap<String, LocaleInfo>{
        public LocaleInfos include(LocaleInfo info) {
            if (info != null) put(info.locale, info);
            return this;
        }
    }

    private static File getAppRootFolder(final Pattern fileOrDirThatMustBeInTheRoot) {
        File current = new File(".").getAbsoluteFile();

        while (current != null) {
            File[] found = FileUtils.listFiles(current, fileOrDirThatMustBeInTheRoot);

            if ((found != null) && (found.length > 0)) return current;

            current = current.getParentFile();
        }
        return null;
    }

    private static Document getXml(File file) {
        if ((file != null) && file.exists() && file.isFile()) {
            InputStream stream = null;
            try {
                stream = new FileInputStream(file);
                final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                documentBuilderFactory.setNamespaceAware(true);
                Document dom = documentBuilderFactory.newDocumentBuilder().parse(stream);


                stream.close();
                return dom;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                FileUtils.close(stream, "");
            }
        }
        return null;
    }

    public TranslationStatistics() {
        Date fileLimitDate = null;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(iniFile);
            lastLocales.load(inputStream);

            fileLimitDate = getModifyDateProperty("ignore");
        } catch (IOException ex) {
        } finally {
            FileUtils.close(inputStream,"TranslationStatistics.load(" + iniFile + ")");
        }
        this.fileLimitDate = fileLimitDate;
    }

    protected String getCommentProperty(String name) {
        Object val = (lastLocales == null) ? null : lastLocales.get(name);
        String[] parts = (val != null) ? val.toString().split(";") : null;
        if ((parts != null) && (parts.length > 1)) {
            return parts[1];
        }
        return null;
    }

    protected Date getModifyDateProperty(String name) {
        Object ignore = lastLocales.get(name);
        String[] parts = (ignore != null) ? ignore.toString().split(";") : null;
        if ((parts != null) && (parts.length > 0)) {
            return DateUtil.parseIsoDate(parts[0]);
        }
        return null;
    }

    private LocaleInfo getLocaleInfo(File resDir) {
        LocaleInfo result = null;
        final String localeFolderName = resDir.getName();

        final File stringsFile = new File(resDir, "strings.xml");
        Document strings = getXml(stringsFile);
        Document html = getXml(new File(resDir, "html-pages.xml"));
        File fdroidDir = new File(fdroidRoot, localeFolderName);
        Document fdroid = getXml(new File(fdroidDir, "fdroid.xml"));

        if (strings != null) {
            result = new LocaleInfo();

            if (localeFolderName.length() > defaultLocaleFolderName.length())
                result.locale = localeFolderName.substring(defaultLocaleFolderName.length());
            else
                result.locale = "en";

            NodeList stringNodes = getStrings(strings);
            if (stringNodes != null) {
                result.strings = stringNodes.getLength();
                for(int i=0;i < result.strings; i++) {
                    result.stringNames.add(stringNodes.item(i).getNodeValue());
                }
            }
            result.html = countStrings(html);
            result.fdroid = countStrings(fdroid);

            result.fastlane = getFastlaneCount(result.locale);

            result.lastModified = new Date(stringsFile.lastModified());
            if ((fileLimitDate != null) && (result.lastModified.before(fileLimitDate))) {
                Date dateFromProperties = getModifyDateProperty(result.locale);
                if (dateFromProperties != null) result.lastModified = dateFromProperties;
            }
            String translators = getCommentProperty(result.locale);
            if (translators != null) result.translators = translators;
        }
        return result;
    }

    static Pattern getFastlanePattern(String localeString) {
        String[] languages = localeString.split("-");
        String language = languages[0];

        if (language.compareTo("in") == 0) language = "id";
        return Pattern.compile(language + ".*");
    }

    private int getFastlaneCount(String localeString) {
        if (localeString != null) {
            File[] fastlaneLanguageDirs = FileUtils.listFiles(fastlaneRoot, getFastlanePattern(localeString));
            if (fastlaneLanguageDirs != null) return fastlaneLanguageDirs.length;
        }
        return 0;
    }

    private static NodeList getStrings(Document xml) {
        if (xml != null) {
            try {
                NodeList nodeList = (NodeList) stringNodesExpression.evaluate(xml, XPathConstants.NODESET);
                return nodeList;
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static int countStrings(Document xml) {
        NodeList nodeList = getStrings(xml);
        if (nodeList != null) {
            return nodeList.getLength();
        }
        return 0;
    }

    private static XPathExpression getStringsXPathExpression(String expression) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        XPathExpression stringNodesExpression = null;
        try {
            stringNodesExpression = xpath.compile(expression);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return stringNodesExpression;
    }

    LocaleInfos getLocaleInfos() {
        LocaleInfos result = new LocaleInfos().include(english);

        File[] resFiles = FileUtils.listFiles(resRoot, PATTERN_ANDROID_RES_STRING_LOCALE); // .replace("-","\\-")));
        for (File res : resFiles) {
            result.include(getLocaleInfo(res));
        }

        return result;
    }
}
