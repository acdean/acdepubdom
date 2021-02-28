package me.koogy.acdepubdom;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
** Info including options
*/
public class Info {

    private static final Logger logger = LoggerFactory.getLogger(Info.class);

    // Option names
    public static String PART_TITLE_ENABLED_PROPERTY    = "part.titles";
    public static String PART_TITLE_TEXT_PROPERTY       = "part.title_text";
    public static String PART_NUMBER_STYLE_PROPERTY     = "part.number_style";
    public static String CHAPTER_TITLE_ENABLED_PROPERTY = "chapter.titles";
    public static String CHAPTER_TITLE_TEXT_PROPERTY    = "chapter.title_text";
    public static String CHAPTER_NUMBER_STYLE_PROPERTY  = "chapter.number_style";
    public static String CHAPTER_NUMBER_IN_TOC_PROPERTY = "chapter.number_in_toc";
    public static String CHAPTER_NUMBERS_CONTINUOUS     = "chapter.numbers_continuous";
    // option value
    public static String CHAPTER_TITLE_TEXT_NONE        = "none";

    private String author;
    private String tocTitle;
    private String title;
    private String subtitle;
    private String date;
    private Map<String, String> options = new HashMap();
    private int index;
    private String numbering;  // number string, like "Part I" or "Chapter The First" // TODO
    private String contents;

    public Info(int index) {
        this.index = index;
        setDefaults();
        numbering = Numbers.numbering(options.get(CHAPTER_TITLE_TEXT_PROPERTY), options.get(CHAPTER_NUMBER_STYLE_PROPERTY), index);
    }

    // this is an info node
    public Info(Node infoNode, int index) {
        this.index = index;
        setDefaults();
        NodeList nodes = infoNode.getChildNodes();
        for (int i = 0 ; i < nodes.getLength() ; i++) {
            Node node = nodes.item(i);
            logger.info("Current Element [{}]", node.getNodeName());
            switch(node.getNodeName().toLowerCase()) {
                case "author":
                    author = node.getTextContent();
                    logger.info("author [{}]", author);
                    break;
                case "tocTitle":
                    tocTitle = node.getTextContent();
                    logger.info("tocTitle [{}]", tocTitle);
                    break;
                case "title":
                    title = node.getTextContent();
                    logger.info("title [{}]", title);
                    break;
                case "subtitle":
                    subtitle = node.getTextContent();
                    logger.info("subtitle [{}]", subtitle);
                    break;
                case "date":
                    date = node.getTextContent();
                    logger.info("date [{}]", date);
                    break;
                case "option":
                    setOption(node);
                default:
                    logger.info("Unknown [{}]", node.getNodeName());
                    break;
            }
        }
        numbering = Numbers.numbering(options.get(CHAPTER_TITLE_TEXT_PROPERTY), options.get(CHAPTER_NUMBER_STYLE_PROPERTY), index);
    }

    void setDefaults() {
        options.put(PART_TITLE_ENABLED_PROPERTY,    "true");
        options.put(PART_TITLE_TEXT_PROPERTY,       "Part");
        options.put(PART_NUMBER_STYLE_PROPERTY,     "I");
        options.put(CHAPTER_TITLE_ENABLED_PROPERTY, "true");
        options.put(CHAPTER_TITLE_TEXT_PROPERTY,    "Chapter");
        options.put(CHAPTER_NUMBER_STYLE_PROPERTY,  "I");
        options.put(CHAPTER_NUMBER_IN_TOC_PROPERTY, "true");
        options.put(CHAPTER_NUMBERS_CONTINUOUS,     "false");
    }

    // this is like
    // <option name="part.title_text" value="Book The"/>
    public final void setOption(Node option) {
        NamedNodeMap m = option.getAttributes();
        String name = m.getNamedItem("name").getTextContent();
        String value = m.getNamedItem("value").getTextContent();
        options.put(name, value);
        logger.info("Option [{}]:[{}]", name, value);
    }

    public String getAuthor() {
        return author;
    }

    public String getTocTitle() {
        return tocTitle;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDate() {
        return date;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public int getIndex() {
        return index;
    }

    public String getNumbering() {
        return numbering;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    @Override
    public String toString() {
        return "Info{"
                + "author=" + author
                + ", tocTitle=" + tocTitle
                + ", title=" + title
                + ", subtitle=" + subtitle
                + ", date=" + date
                + ", options=" + options
                + "}";
    }

    // given a nodelist, find the info, or null
    static Info findInfo(Node parent, int index) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("info")) {
                Info info = new Info(node, index);
                logger.info("FindInfo [{}]", info);
                return info;
            }
        }
        logger.info("FindInfo none");
        return new Info(index);
    }
}
