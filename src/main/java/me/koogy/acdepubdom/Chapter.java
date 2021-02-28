package me.koogy.acdepubdom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Chapter {

    // chapter types
    public static final int PREFIX          = 1;
    public static final int CHAPTER         = 2;
    public static final int ACT             = 3;
    public static final int PART_CHAPTER    = 4;
    public static final int APPENDIX        = 5;
    public static final int FOOTNOTE        = 6;

    Logger logger = LoggerFactory.getLogger(Chapter.class);

    int index;
    String numbering;   // like  "Book The First" or "I" or "one"
    String filename;    // like ch001.html or ch02001.html or app001.html or act001.html
    String title;
    String subtitle;

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
