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

    Logger logger = LoggerFactory.getLogger(Info.class);

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
                    logger.info("author");
                    author = node.getTextContent();
                    break;
                case "tocTitle":
                    logger.info("tocTitle");
                    tocTitle = node.getTextContent();
                    break;
                case "title":
                    logger.info("title");
                    title = node.getTextContent();
                    break;
                case "subtitle":
                    logger.info("subtitle");
                    subtitle = node.getTextContent();
                    break;
                case "date":
                    logger.info("date");
                    date = node.getTextContent();
                    break;
                case "option":
                    logger.info("option");
                    setOption(node);
                default:
                    logger.info("Unknown");
                    break;
            }
        }
        number = "[Number " + index + "]"; // TODO calculate based on options and number
    }

    // this is like
    // <option name="part.title_text" value="Book The"/>
    public final void setOption(Node option) {
        NamedNodeMap m = option.getAttributes();
        Node name = m.getNamedItem("name");
        Node value = m.getNamedItem("value");
        options.put(name.getTextContent(), value.getTextContent());
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
}
