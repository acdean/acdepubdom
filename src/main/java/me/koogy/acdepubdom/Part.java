package me.koogy.acdepubdom;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
** Part
*/
public class Part {

    Logger logger = LoggerFactory.getLogger(Part.class);

    int index;
    String numbering;   // like  "Book The First" or "I" or "one"
    String filename;    // like ch001.html or ch02001.html or app001.html or act001.html
    String author;      // parts can have authors (think various artists)
    String title;
    String subtitle;
}
