package me.koogy.acdepubdom;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Book {

    private static final String TOC_FILE = "toc.ncx";
    private static final String TOC_TMPL = "toc.vm";
    private static final String CONTENT_FILE = "content.opf";
    private static final String CONTENT_TMPL = "content.vm";
    private static final String TITLE_FILE = "title_page.html";
    private static final String CHAPTER_TMPL = "chapter.vm";
    private static final String TITLE_TMPL = "title_page.vm";
    private static final String PART_TMPL = "part.vm";

    public static final Logger logger = LoggerFactory.getLogger(Book.class);

    File directory;
    int partNumber;

    public Book(String filename, Element root) throws Exception {

        // clear out and create working directory
        directory = makeTempDirectory(filename);
        logger.info("Directory [{}]", directory);

        // setup velocity
        VelocityEngine velocity = new VelocityEngine();
        Properties p = new Properties();
        p.setProperty("resource.loaders", "class");
        p.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(p);

        // parse book info
        Info bookInfo = new Info(root.getElementsByTagName("info").item(0), 0);
        logger.info("Info [{}]", bookInfo);

        // Title Page
        writeTemplate(TITLE_TMPL, "title_page.html", bookInfo);

        // prefixes
        NodeList prefixes = root.getElementsByTagName("prefix");
        logger.info("Prefixes [{}]", prefixes.getLength());
        process(bookInfo, prefixes);

        // parts
        NodeList parts = root.getElementsByTagName("part");
        logger.info("Parts [{}]", parts.getLength());
        process(bookInfo, parts);

        // acts
        NodeList acts = root.getElementsByTagName("act");
        logger.info("Acts [{}]", acts.getLength());
        process(bookInfo, acts);

        // chapters
        NodeList chapters = root.getElementsByTagName("chapter");
        logger.info("Chapters [{}]", chapters.getLength());
        process(bookInfo, chapters);

        // appendixes
        NodeList appendixes = root.getElementsByTagName("appendix");
        logger.info("Appendixes [{}]", appendixes.getLength());
        process(bookInfo, appendixes);

        close();
    }

    private final void process(Info bookInfo, NodeList nodes) {
        String nodeName;
        for (int i = 0 ; i < nodes.getLength() ; i++) {
            Node node = nodes.item(i);
            nodeName = node.getNodeName().toLowerCase();
            switch(nodeName) {
                case "part":
                    processPart(bookInfo, node, i);
                    break;
                case "prefix":
                    processChapter(bookInfo, node, i, Chapter.PREFIX);
                    break;
                case "chapter":
                    if (partNumber != 0) {
                        // this is a chapter within a part
                        processChapter(bookInfo, node, i, Chapter.PART_CHAPTER);
                    } else {
                        processChapter(bookInfo, node, i, Chapter.CHAPTER);
                    }
                    break;
                case "appendix":
                    processChapter(bookInfo, node, i, Chapter.APPENDIX);
                    break;
                default:
                    logger.info("Node [{}]", nodeName);
                    break;
            }
        }
    }

    void processPart(Info bookInfo, Node node, int index) {
        // setting part number here for included chapters to use
        partNumber = index + 1;
        String filename = String.format("pt%02d.html", index + 1);
        Info partInfo = new Info(node, index);
        writeTemplate(PART_TMPL, filename, bookInfo, partInfo);
        // process all the children (which are chapters)
        process(bookInfo, node.getChildNodes());
        logger.info("Part done");
    }

    void processChapter(Info bookInfo, Node node, int index, int type) {
        String filename = filenameFromType(type, index + 1);
        File f = new File(filename);
        Info info = new Info(node, index);
        writeTemplate(CHAPTER_TMPL, filename, bookInfo, info);

        logger.info("Chapter [{}]{", type);

        //process(node.getChildNodes());
        logger.info("}Chapter");
    }

    String filenameFromType(int type, int index) {
        String filename = null;
        switch (type) {
            case Chapter.PREFIX:
                filename = String.format("pre%03d.html", index);
                break;
            case Chapter.PART_CHAPTER:
                filename = String.format("ch%02d%03d.html", partNumber, index);
                break;
            case Chapter.CHAPTER:
                filename = String.format("ch%03d.html", index);
                break;
            case Chapter.APPENDIX:
                filename = String.format("app%03d.html", index);
                break;
            case Chapter.FOOTNOTE:
                filename = String.format("notes%03d.html", index);
                break;
        }
        return filename;
    }
    
    final void close() {
        logger.info("Closing...");
    }

    /*
    ** Expand the given template into given file, with the relevant info
    */
    void writeTemplate(String templateName, String filename, Info bookInfo) {
        writeTemplate(templateName, filename, bookInfo, null);
    }
    void writeTemplate(String templateName, String filename, Info bookInfo, Info info) {
        Writer writer = null;
        VelocityContext context = new VelocityContext();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        context.put("date", format.format(new Date()));
        context.put("book", bookInfo);
        if (info != null) {
            context.put("info", info);
        }

        try {
            writer = new FileWriter(new File(directory, filename));
            Velocity.mergeTemplate("velocity" + File.separator + templateName, "UTF-8", context, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            logger.error("Error: ", e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                logger.error("Error: ", e);
            }
        }
    }

    // temp directory based on name of input file
    static File makeTempDirectory(String filename) {
        File file = new File(filename);
        File dir = new File("/tmp/acdepub_" + file.getName().replace(".xml", ""));
        dir.mkdirs();
        logger.info("Dir [{}]", dir);
        // clear it out
        File[] files = dir.listFiles();
        for (File ls : files) {
            logger.debug("Deleting: {}", ls);
            ls.delete();
        }
        return dir;
    }
}
