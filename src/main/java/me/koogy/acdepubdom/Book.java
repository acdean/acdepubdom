package me.koogy.acdepubdom;

import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    public static final String TOC_FILE = "toc.ncx";
    private static final String CONTAINER_FILE = "META-INF/container.xml";
    private static final String CONTAINER_TMPL = "container.vm";
    private static final String CONTENT_FILE = "content.opf";
    private static final String CONTENT_TMPL = "content.vm";
    private static final String COVER_FILE = "cover.xhtml";
    private static final String COVER_TMPL = "cover.vm";
    private static final String COVER_IMAGE_FILE = "cover-image.jpg";
    private static final String TITLE_FILE = "title_page.xhtml";
    private static final String TITLE_TMPL = "title_page.vm";
    private static final String MIMETYPE_FILE = "mimetype";
    private static final String MIMETYPE_TMPL = "mimetype.vm";
    private static final String STYLESHEET_FILE = "stylesheet.css";
    private static final String STYLESHEET_TMPL = "stylesheet.vm";

    private static final String CHAPTER_TMPL = "chapter.vm";
    private static final String PART_TMPL = "part.vm";

    public static final Logger logger = LoggerFactory.getLogger(Book.class);

    int tocIndex = 1;   // titlepage means this starts at 1
    int partNumber;
    int chapterNumber;
    int prefixNumber;
    int appendixNumber;
    StringBuilder contents = new StringBuilder();
    Writer contentWriter;
    String uid = UUID.randomUUID().toString();
    List<String> items = new ArrayList<>();
    Template template;
    Toc toc;
    // counts for footnote links and definitions
    int note = 0;
    int footnote = 0;
    List<String> images = new ArrayList<>();    // images are per-book and are only in content.opf

    public Book(String filename, Element root) throws Exception {

        // clear out and create working directory
        File directory = makeTempDirectory(filename);
        logger.info("Directory [{}]", directory);

        // parse book info
        Info bookInfo = Info.findInfo(root, BOOK, 0);
        bookInfo.setUid(uid);
        File imageFile = new File(".", COVER_IMAGE_FILE);
        logger.info("Image File [{}]", imageFile.getCanonicalPath());
        if (imageFile.exists()) {
            logger.info("Cover image found");
            bookInfo.setCoverImage(COVER_IMAGE_FILE);
        }
        logger.info("CoverImage [{}]", bookInfo.getCoverImage());
        logger.info("Info [{}]", bookInfo);
        template = new Template(directory, bookInfo);
        toc = new Toc(bookInfo, directory);

        NodeList nodeList = root.getChildNodes();
        logger.info("Nodes [{}]", nodeList.getLength());
        process(bookInfo, nodeList);

        toc.close();
        close();

        // write all the other bits, zip into final epub
        template.write(CONTAINER_TMPL, CONTAINER_FILE);
        template.write(CONTENT_TMPL, CONTENT_FILE, null, items, images);
        template.write(MIMETYPE_TMPL, MIMETYPE_FILE);
        template.write(STYLESHEET_TMPL, STYLESHEET_FILE);
        template.write(TITLE_TMPL, TITLE_FILE, bookInfo);
        logger.info("Filename [{}]", filename);
        logger.info("Path [{}]", Paths.get(filename));
        logger.info("Parent [{}]", Paths.get(filename).getParent());
        //String srcDir = Paths.get(filename).getParent().toString();
        //logger.info("SrcDir [{}]", srcDir);
        if (bookInfo.getCoverImage() != null) {
            template.write(COVER_TMPL, COVER_FILE);
            // copy image over
            copy(".", directory.toString(), COVER_IMAGE_FILE);
        }
        for (String image : images) {
            // copy images over
            copy(".", directory.toString(), image + ".jpg");
        }
        Zipper.write(directory, filename);
    }

    private final void process(Info bookInfo, NodeList nodes) {
        //logger.info("Processing...");
        String nodeName;
        for (int i = 0 ; i < nodes.getLength() ; i++) {
            //logger.info("Index [{}]", i);
            Node node = nodes.item(i);
            nodeName = node.getNodeName().toLowerCase();
            //logger.info("NodeName [{}]", nodeName);
            switch(nodeName) {
                case "#text":
                    if (!node.getTextContent().trim().isEmpty()) {
                        //logger.info("TEXT[{}]", node.getTextContent());
                    }
                    String txt = node.getTextContent()
                            .replaceAll("--", "—")      // mdash
                            .replaceAll("&", "&amp;")   // ampersand
                            ;
                    add(txt);   // mdash
                    break;
                case "part":
                    processPart(bookInfo, node);
                    break;
                case "prefix":
                    processPrefix(bookInfo, node);
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
                    processAppendix(bookInfo, node);
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
                    processDiv(bookInfo, node, nodeName);
                    break;

                // play bits
                case "personae":
                    processTag(bookInfo, node, "dl");
                    break;
                case "person":
                    processPerson(bookInfo, node);
                    break;
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

                // TODO footnotes are links within the current file
                // that way it's easy to link back to the note
                case "note":
                    processNote();
                    break;
                case "footnote":
                    processFootnote(bookInfo, node);
                    break;

                case "table":
                    processTable(bookInfo, node);
                    break;
                case "tr":
                    processTr(bookInfo, node);
                    break;
                case "td":
                    processTd(bookInfo, node);
                    break;

                case "image":
                    processImage(bookInfo, node);
                    break;

                default:
                    logger.info("ignored");
                    break;
            }
        }
        //logger.info("Processed");
    }

    void processPart(Info bookInfo, Node node) {
        partNumber++;
        tocIndex++;
        // reset chapter number unless we have continuous chapters
        if (Info.getOption(bookInfo, Info.CHAPTER_NUMBERS_CONTINUOUS).equalsIgnoreCase("false")) {
            chapterNumber = 0;
        }
        logger.info("Part [{}][{}]", partNumber, tocIndex);

        String filename = filenameFromType(Book.PART, partNumber);
        items.add(filename.replaceFirst(".xhtml", ""));
        // part might have info node
        Info info = Info.findInfo(node, PART, partNumber);
        template.write(PART_TMPL, filename, info);
        toc.start(info, "part", filename, tocIndex);

        process(bookInfo, node.getChildNodes());

        toc.end();
        logger.info("Part [{}][{}] done", partNumber, tocIndex);
    }

    // prefix and appendix are numbered differently, everything else uses chapter numbers
    void processPrefix(Info bookInfo, Node node) {
        prefixNumber++;
        processChapter(bookInfo, node, PREFIX, prefixNumber);
    }
    void processAppendix(Info bookInfo, Node node) {
        appendixNumber++;
        processChapter(bookInfo, node, APPENDIX, appendixNumber);
    }
    void processChapter(Info bookInfo, Node node, int type) {
        chapterNumber++;
        processChapter(bookInfo, node, type, chapterNumber);
    }
    void processChapter(Info bookInfo, Node node, int type, int number) {
        tocIndex++;
        note = 0;
        footnote = 0;
        logger.info("Chapter[{}][{}][{}] type[{}]", partNumber, number, tocIndex, type);
        String filename = filenameFromType(type, number);
        File f = new File(filename);
        items.add(filename.replaceFirst(".xhtml", ""));
        // clear contents
        contents = new StringBuilder();
        // chapter may have info node (is this true? epilogue?)
        Info info = Info.findInfo(node, type, number);
        toc.start(info, "chapter", filename, tocIndex);

        // process all children into contents
        process(bookInfo, node.getChildNodes());

        toc.end();

        // fix the ampersands (more?)
        //String str = Pattern.compile("&").matcher(contents).replaceAll("&amp;");
        info.setContents(contents.toString());
        template.write(CHAPTER_TMPL, filename, info);
        logger.info("Chapter done");
    }

    void processBreak(Info bookInfo, Node node) {
        add("<p><br /></p>\n");
    }
    void processHr(Info bookInfo, Node node) {
        add("<div class=\"hr\">~</div>\n");
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
        add("<p class=\"" + css + "\">");
        process(bookInfo, node.getChildNodes());
        add("</p>\n");
    }
    void processDiv(Info bookInfo, Node node, String css) {
        add("<div class=\"" + css + "\">");
        process(bookInfo, node.getChildNodes());
        add("</div>\n");
    }
    void processSpan(Info bookInfo, Node node, String css) {
        add("<span class=\"" + css + "\">");
        process(bookInfo, node.getChildNodes());
        add("</span>");
    }
    // handle person tabs, inclding attributes - as dt / dd pairs
    // <person name="Elfride Swancourt" description="a young Lady"/>
    void processPerson(Info bookInfo, Node node) {
        add("<dt>");
        add(node.getAttributes().getNamedItem("name").getTextContent());
        add("</dt>");
        add("<dd>");
        add(node.getAttributes().getNamedItem("description").getTextContent());
        add("</dd>\n");
    }

    // a link to a footnote
    void processNote() {
        note++;
        logger.info("Note [{}]", note);
        add("<a id=\"note_" + note + "\"/>");
        add("<a href=\"#footnote_" + note + "\">[note " + note + "]</a>");
    }
    // the footnote contents
    void processFootnote(Info bookInfo, Node node) {
        footnote++;
        if (footnote == 1) {
            // end of text, start of footnotes - full width
            add("<hr/>\n");
        }
        add("<div class=\"footnote\" id=\"footnote_" + footnote + "\">\n");
        add("Note " + footnote + ": ");
        // link back to anchor
        add("<a href=\"#note_" + footnote + "\">[Back]</a>\n");
        // recursively process all the children
        process(bookInfo, node.getChildNodes());
        logger.info("Footnote [{}]: [{}]", footnote, node.getTextContent());
        add("</div>");
        processBreak(bookInfo, node);
    }

    void processTable(Info bookInfo, Node node) {
        add("<table>\n");
        process(bookInfo, node.getChildNodes());
        add("</table>\n");
    }
    void processTr(Info bookInfo, Node node) {
        add("<tr>\n");
        process(bookInfo, node.getChildNodes());
        add("</tr>\n");
    }
    void processTd(Info bookInfo, Node node) {
        add("<td>\n");
        process(bookInfo, node.getChildNodes());
        add("</td>\n");
    }

    // images are like
    // <image src="sowster"> // NB no extension
    // jpg only at the moment and they should be 600x800 (or at least 6:8)
    void processImage(Info bookInfo, Node node) {
        String imageName = node.getAttributes().getNamedItem("src").getTextContent();
        images.add(imageName);
        logger.info("Image: [{}]", imageName);
        add("<div>\n");
        add("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                + "version=\"1.1\" width=\"100%\" height=\"100%\" viewBox=\"0 0 600 800\" "
                + "preserveAspectRatio=\"xMidYMid meet\">\n");
        add("<image width=\"600\" height=\"800\" xlink:href=\"" + imageName + ".jpg\"/>");
        add("</svg>\n");
        add("</div>\n");
    }

    // convenience method to append to contents
    void add(String s) {
        contents.append(s);
    }

    String filenameFromType(int type, int index) {
        String filename = null;
        switch (type) {
            case PREFIX:
                filename = String.format("pre%03d.xhtml", index);
                break;
            case PART:
                filename = String.format("pt%02d.xhtml", partNumber);
                break;
            case PART_CHAPTER:
                filename = String.format("ch%02d%03d.xhtml", partNumber, index);
                break;
            case CHAPTER:
                filename = String.format("ch%03d.xhtml", index);
                break;
            case APPENDIX:
                filename = String.format("app%03d.xhtml", index);
                break;
            case FOOTNOTE:
                filename = String.format("notes%03d.xhtml", index);
                break;
        }
        return filename;
    }
    
    final void close() {
        logger.info("Closing...");
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
        // also need a meta-inf subdir
        File metadir = new File(dir, "META-INF");
        metadir.mkdirs();
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

    void copy(String srcDirectory, String dstDirectory, String filename) throws Exception {
        Path src = Paths.get(srcDirectory, filename);
        Path dst = Paths.get(dstDirectory, filename);
        logger.info("Copying [{}] to [{}]", src, dst);
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }
}
