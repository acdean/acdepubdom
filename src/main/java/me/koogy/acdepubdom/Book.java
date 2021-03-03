package me.koogy.acdepubdom;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Book {

    // book parts
    public static final int BOOK            = 0;
    public static final int PART            = 1;
    public static final int PREFIX          = 2;
    public static final int CHAPTER         = 3;
    public static final int ACT             = 4;
    public static final int PART_CHAPTER    = 5;
    public static final int APPENDIX        = 6;
    public static final int FOOTNOTE        = 7;

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
    int tocIndex = 1;   // titlepage means this starts at 1
    int partNumber;
    int chapterNumber;
    StringBuilder contents = new StringBuilder();
    Writer tocWriter;
    Writer contentWriter;
    String uid = UUID.randomUUID().toString();
    List<String> items = new ArrayList<>();

    public Book(String filename, Element root) throws Exception {

        // clear out and create working directory
        directory = makeTempDirectory(filename);
        logger.info("Directory [{}]", directory);
        tocWriter = new FileWriter(new File(directory, TOC_FILE));

        // setup velocity
        VelocityEngine velocity = new VelocityEngine();
        Properties p = new Properties();
        p.setProperty("resource.loaders", "class");
        p.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(p);

        // parse book info
        Info bookInfo = Info.findInfo(root, BOOK, 0);
        logger.info("Info [{}]", bookInfo);
        initToc(bookInfo.getTitle(), uid);

        // Title Page
        writeTemplate(TITLE_TMPL, "title_page.html", bookInfo);

        NodeList nodeList = root.getChildNodes();
        logger.info("Nodes [{}]", nodeList.getLength());
        process(bookInfo, nodeList);

        closeToc();
        close();

        writeTemplate(CONTENT_TMPL, "content.opf", bookInfo, null, items);
    }

    private final void process(Info bookInfo, NodeList nodes) {
        logger.info("Processing...");
        String nodeName;
        for (int i = 0 ; i < nodes.getLength() ; i++) {
            logger.info("Index [{}]", i);
            Node node = nodes.item(i);
            nodeName = node.getNodeName().toLowerCase();
            logger.info("NodeName [{}]", nodeName);
            switch(nodeName) {
                case "#text":
                    if (!node.getTextContent().trim().isEmpty()) {
                        logger.info("TEXT[{}]", node.getTextContent());
                    }
                    String txt = node.getTextContent()
                            .replaceAll("--", "—")  // mdash
                            ;
                    add(txt);   // mdash
                    break;
                case "part":
                    processPart(bookInfo, node);
                    break;
                case "prefix":
                    processChapter(bookInfo, node, PREFIX);
                    break;
                case "act":
                    processChapter(bookInfo, node, ACT);
                    break;
                case "chapter":
                    if (partNumber != 0) {
                        // this is a chapter within a part
                        processChapter(bookInfo, node, PART_CHAPTER);
                    } else {
                        processChapter(bookInfo, node, CHAPTER);
                    }
                    break;
                case "appendix":
                    processChapter(bookInfo, node, APPENDIX);
                    break;

                /*
                ** SMALLER BITS
                */
                case "title":
                    // this is parsed as part of part or chapter parsing
                    // so do nothing here
                    logger.info("Title tag [{}]", node.getTextContent());
                    break;
                case "break":
                    processBreak(bookInfo, node);
                    break;
                case "centre":
                case "center":
                    processDiv(bookInfo, node, "centre");
                    break;
                case "em":
                case "i":
                    processTag(bookInfo, node, "em");
                    break;
                case "fixed":
                    processTag(bookInfo, node, "pre");
                    break;
                case "hr":
                    processHr(bookInfo, node);
                    break;
                case "letter":
                    processDiv(bookInfo, node, "letter");
                    break;
                case "p":
                    processP(bookInfo, node);
                    break;
                case "p0":
                case "p1":
                case "p2":
                case "p3":
                case "p4":
                    processP(bookInfo, node, nodeName);
                    break;
                case "right":
                    processDiv(bookInfo, node, "right");
                    break;
                case "sc":
                    processSpan(bookInfo, node, "smallcaps");
                    break;
                case "smallcaps":
                    processSmallCaps(node);
                    break;
                case "section":
                    processTag(bookInfo, node, "h3");
                    break;

                // poem bits
                case "poem":
                case "poem1":
                case "poem2":
                case "poem3":
                case "poem4":
                case "poem5":
                    processP(bookInfo, node, nodeName);
                    break;

                // play bits
                case "direction":
                    processSpan(bookInfo, node, "direction");
                    break;
                case "line":
                    processSpan(bookInfo, node, "line");
                    break;
                case "speaker":
                    processP(bookInfo, node, "p0 speaker");
                    break;
                case "speech":
                    processP(bookInfo, node, "p0 speech");
                    break;

                default:
                    logger.info("ignored");
                    break;
            }
        }
        logger.info("Processed");
    }

    void processPart(Info bookInfo, Node node) {
        partNumber++;
        tocIndex++;
        // reset chapter number unless we have continuous chapters
        if (Info.getOption(bookInfo, Info.CHAPTER_NUMBERS_CONTINUOUS).equalsIgnoreCase("false")) {
            chapterNumber = 0;
        }
        logger.info("Part [{}][{}]", partNumber, tocIndex);

        String filename = String.format("pt%02d.html", partNumber);
        items.add(filename.replaceFirst(".html", ""));
        // part might have info node
        Info info = Info.findInfo(node, PART, partNumber);
        writeTemplate(PART_TMPL, filename, bookInfo, info);
        startToc(info.getTitle(), "part", filename);

        process(bookInfo, node.getChildNodes());

        endToc();
        logger.info("Part [{}][{}] done", partNumber, tocIndex);
    }

    void processChapter(Info bookInfo, Node node, int type) {
        chapterNumber++;
        tocIndex++;
        logger.info("Chapter[{}][{}][{}] type[{}]", partNumber, chapterNumber, tocIndex, type);
        String filename = filenameFromType(type, chapterNumber);
        File f = new File(filename);
        items.add(filename.replaceFirst(".html", ""));
        // chapter may have info node (is this true? epilogue?)
        Info info = Info.findInfo(node, type, chapterNumber);
        startToc(info.getTitle(), "chapter", filename);

        process(bookInfo, node.getChildNodes());

        endToc();
        info.setContents(contents.toString());
        writeTemplate(CHAPTER_TMPL, filename, bookInfo, info);
        logger.info("Chapter done");
    }

    void processBreak(Info bookInfo, Node node) {
        add("<p><br /></p>\n");
    }
    void processHr(Info bookInfo, Node node) {
        add("<div class='hr'>~</div>\n");
    }
    void processP(Info bookInfo, Node node) {
        add("<p>");
        process(bookInfo, node.getChildNodes());
        add("</p>\n");
    }
    // special case
    void processSmallCaps(Node node) {
        add(toSmallCaps(node.getChildNodes().item(0).getTextContent()));
    }

    // generic versions
    void processTag(Info bookInfo, Node node, String tag) {
        add("<" + tag + ">");
        process(bookInfo, node.getChildNodes());
        add("</" + tag + ">");
    }
    void processP(Info bookInfo, Node node, String css) {
        add("<p class='" + css + "'>");
        process(bookInfo, node.getChildNodes());
        add("</p>\n");
    }
    void processDiv(Info bookInfo, Node node, String css) {
        add("<div class='" + css + "'>");
        process(bookInfo, node.getChildNodes());
        add("</div>");
    }
    void processSpan(Info bookInfo, Node node, String css) {
        add("<span class='" + css + "'>");
        process(bookInfo, node.getChildNodes());
        add("</span>");
    }

    // cpnvenience method to append to contents
    void add(String s) {
        contents.append(s);
    }

    String filenameFromType(int type, int index) {
        String filename = null;
        switch (type) {
            case PREFIX:
                filename = String.format("pre%03d.html", index);
                break;
            case PART_CHAPTER:
                filename = String.format("ch%02d%03d.html", partNumber, index);
                break;
            case CHAPTER:
                filename = String.format("ch%03d.html", index);
                break;
            case APPENDIX:
                filename = String.format("app%03d.html", index);
                break;
            case FOOTNOTE:
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
        writeTemplate(templateName, filename, bookInfo, info, null);
    }
    void writeTemplate(String templateName, String filename, Info bookInfo, Info info, List<String> items) {
        Writer writer = null;
        VelocityContext context = new VelocityContext();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        context.put("date", format.format(new Date()));
        context.put("book", bookInfo);
        if (info != null) {
            context.put("info", info);
        }
        if (items != null) {
            context.put("items", items);
        }
        context.put("uid", uid);

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

    /* 
    ** Kobo rendering engine doesn't really do smallcaps
    ** so replace with spans of proper class
    ** ie <smallcaps>Charles Dexter Ward</smallcaps>
    ** -> C<span class=sc>HARLES</span> D<span class=sc>EXTER</span> W<span class=span>ARD</span>
    */
    public static String toSmallCaps(String input) {
        StringBuilder output = new StringBuilder();
        boolean upper = true;
        for (int i = 0 ; i < input.length() ; i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                if (!upper) {
                   // was lower, so end span
                   output.append("</span>");
                }
                upper = true;
            }
            if (Character.isLowerCase(c)) {
                c = Character.toUpperCase(c);
                if (upper) {
                    // was upper so start span
                    output.append("<span class=\"sc\">");
                }
                upper = false;
            }
            output.append(c);
        }
        // done
        if (!upper) {
            // close the current span
            output.append("</span>");
        }
        return output.toString();
    }

    VelocityContext tocContext = new VelocityContext();
    void initToc(String bookTitle, String uid) {
        tocContext.put("bookTitle", bookTitle);
        tocContext.put("uid", uid);
        Velocity.mergeTemplate("velocity" + File.separator + "toc_header.vm", "UTF-8", tocContext, tocWriter);
    }
    void startToc(String title, String cls, String filename) {
        tocContext.put("title", title);
        tocContext.put("class", cls);
        tocContext.put("filename", filename);
        tocContext.put("index", tocIndex);
        Velocity.mergeTemplate("velocity" + File.separator + "toc_entry_start.vm", "UTF-8", tocContext, tocWriter);
    }
    void endToc() {
        tocContext.put("index", tocIndex);
        Velocity.mergeTemplate("velocity" + File.separator + "toc_entry_end.vm", "UTF-8", tocContext, tocWriter);
    }
    void closeToc() throws Exception {
        Velocity.mergeTemplate("velocity" + File.separator + "toc_footer.vm", "UTF-8", tocContext, tocWriter);
        tocWriter.close();
    }
}
