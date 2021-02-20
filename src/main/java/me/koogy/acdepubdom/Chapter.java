package me.koogy.acdepubdom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Chapter {

    // chapter types
    public static final int PREFIX = 1;
    public static final int CHAPTER = 2;
    public static final int PART_CHAPTER = 3;
    public static final int APPENDIX = 4;
    public static final int FOOTNOTE = 5;

    Logger logger = LoggerFactory.getLogger(Chapter.class);

    int number;
    String title;

//    public Chapter(Node node) {
//        NodeList children = node.getChildNodes();
//        for (int i = 0 ; i < children.getLength() ; i++) {
//            Node child = children.item(i);
//            switch(child.getNodeName()) {
//                default:
//                    logger.info("Child: [{}][{}]", child.getNodeName(), child.getNodeType());
//                    break;
//            }
//        }
//    }
}
