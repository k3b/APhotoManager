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
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package de.k3b.translations;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import de.k3b.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
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
    private static final Pattern PATTERN_ANDROID_RES_STRING_LOCALE = Pattern.compile(defaultLocaleFolderName+".*");
    private static final XPathExpression stringNodesExpression = getStringsXPathExpression("/resources/string");
    private static final File root = getAppRootFolder(Pattern.compile("^app$"));
    private static final File resRoot = new File(root, "app/src/main/res");
    private static final File fdroidRoot = new File(root, "app/src/debug/res");
    private static final File fastlaneRoot = new File(root, "fastlane/metadata/android");
    private static final LocaleInfo english = getLocaleInfo(new File(resRoot, "values"));

    private static final String HEADER = "\n" +
            "| language | app | aboutbox | fdroid |\n" +
            "| --- | --- | --- | --- | --- |\n";

    private static class LocaleInfo {
        String locale = null;
        int strings = 0;
        int html = 0;
        int fdroid = 0;
        int fastlane = 0;

        public String dumpMD(LocaleInfo reference) {
            if (reference == null) reference = new LocaleInfo();

			int fdroid = this.fdroid + this.fastlane;
			int fdroidExpected = 2 + 1; // assume short_description.txt+full_description.txt as complete even if title.txt is missing
            return "| " +
                    this.locale +" | " +
                    asValue(this.strings, reference.strings) +" | " +
                    this.html +" | " +
                    asValue(fdroid, fdroidExpected) +" | " +
                    this.fastlane +" | "
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

    private static LocaleInfo getLocaleInfo(File resDir) {
        LocaleInfo result = null;
        final String localeFolderName = resDir.getName();

        Document strings = getXml(new File(resDir, "strings.xml"));
        Document html = getXml(new File(resDir, "html-pages.xml"));
        File fdroidDir = new File(fdroidRoot, localeFolderName);
        Document fdroid = getXml(new File(fdroidDir, "fdroid.xml"));

        if (strings != null) {
            result = new LocaleInfo();

            if (localeFolderName.length() > defaultLocaleFolderName.length())
                result.locale = localeFolderName.substring(defaultLocaleFolderName.length());
            else
                result.locale = "en";

            result.strings = countStrings(strings);
            result.html = countStrings(html);
            result.fdroid = countStrings(fdroid);

            result.fastlane = getFastlaneCount(result.locale);

        }
        return result;
    }

    private static Pattern getFastlanePattern(String localeString) {
        String[] languages = localeString.split("-");
        return Pattern.compile(languages[0] + ".*");
    }

    private static int getFastlaneCount(String localeString) {
        if (localeString != null) {
            File[] fastlaneLanguageDirs = FileUtils.listFiles(fastlaneRoot, getFastlanePattern(localeString));
            if (fastlaneLanguageDirs != null) return fastlaneLanguageDirs.length;
        }
        return 0;
    }

    private static int countStrings(Document xml) {
        if (xml != null) {
            try {
                NodeList nodeList = (NodeList) stringNodesExpression.evaluate(xml, XPathConstants.NODESET);
                if (nodeList != null) return nodeList.getLength();
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
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

    private static LocaleInfos getLocaleInfos() {
        LocaleInfos result = new LocaleInfos().include(english);

        File[] resFiles = FileUtils.listFiles(resRoot, PATTERN_ANDROID_RES_STRING_LOCALE); // .replace("-","\\-")));
        for (File res : resFiles) {
            result.include(getLocaleInfo(res));
        }

        return result;
    }

    private void dumpMD(LocaleInfos infos, LocaleInfo reference) {

        System.out.printf("**" + this.getClass().getSuperclass().getName() + "**\n" + HEADER);

        String[] keys = infos.keySet().toArray(new String[infos.size()]);
        Arrays.sort(keys);
        for (String key : keys) {
            LocaleInfo info = infos.get(key);
            System.out.printf(info.dumpMD(reference).replace("%","%%") + "\n");

        }
    }


    @Test
    public void shouldMatchFastlane() {
        Assert.assertEquals(true, getFastlanePattern("en-US").matcher("en-US").matches());
    }

    @Test
    public void shouldMatchString_de() {
        Assert.assertEquals(true, PATTERN_ANDROID_RES_STRING_LOCALE.matcher("values-de").matches());
    }

    @Test
    public void dumpAsMD() {
        dumpMD(getLocaleInfos(), english);
    }


}
