package me.koogy.acdepubdom;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
** Info, mostly for Book, can be part of Part
*/
public class Info {

    private static final Logger logger = LoggerFactory.getLogger(Info.class);

    private String author;
    private String tocTitle;
    private String title;
    private String subtitle;
    private String date;
    private Map<String, String> options = new HashMap();
    private String number;  // number string, like "Part I" or "Chapter The First" // TODO

    // this is an info node
    public Info(Node infoNode, int index) {
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
        number = "[Number " + index + "]"; // TODO calculate based on options and number
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

    public String getNumber() {
        return number;
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
        return null;
    }
}
