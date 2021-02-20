package me.koogy.acdepubdom;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class AcdEpubDom {

    private static final Logger logger = LoggerFactory.getLogger(AcdEpubDom.class);

    public static void main(String[] args) throws Exception {
        String filename = args[0];
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setValidating(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new MyErrorHandler());
        Document document = builder.parse(new File(filename));
        document.getDocumentElement().normalize();
        logger.info("Document [{}]", document);

        Element root = document.getDocumentElement();

        // creates and writes files
        Book book = new Book(filename, root);
    }

    // just a class that logs errors
    static class MyErrorHandler implements ErrorHandler {
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            logger.error("fatalError: ", exception);
        }
        @Override
        public void error(SAXParseException exception) throws SAXException {
            logger.error("error: ", exception);
        }
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            logger.warn("warning: ", exception);
        }
    }
}
