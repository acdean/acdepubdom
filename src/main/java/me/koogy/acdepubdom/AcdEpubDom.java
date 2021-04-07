package me.koogy.acdepubdom;

import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class AcdEpubDom {

    private static final Logger LOG = LoggerFactory.getLogger(AcdEpubDom.class);
    private static final String XSD_FILENAME = "schema/acdepub.xsd";

    public static void main(String[] args) throws Exception {

        String filename = args[0];

        LOG.info("Working Directory [{}]", System.getProperty("user.dir"));
        LOG.info("Filename [{}]", filename);

        try {
            File xmlFile = new File(filename);

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Source schemaSource = new StreamSource(AcdEpubDom.class.getClassLoader().getResourceAsStream(XSD_FILENAME));
            Schema schema = schemaFactory.newSchema(schemaSource);

            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xmlFile));

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            docFactory.setSchema(schema);
            docFactory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document document = docBuilder.parse(xmlFile);
            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();
            Book book = new Book(filename, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // just a class that logs errors
    static class MyErrorHandler implements ErrorHandler {
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            LOG.error("fatalError: ", exception);
        }
        @Override
        public void error(SAXParseException exception) throws SAXException {
            LOG.error("error: ", exception);
        }
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            LOG.warn("warning: ", exception);
        }
    }
}
