package me.koogy.acdepubdom;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
** Template code
*/
public class Template {

    Logger logger = LoggerFactory.getLogger(Template.class);

    private final Info bookInfo;
    private final File directory;

    public Template(File directory, Info bookInfo) {
        this.directory = directory;
        this.bookInfo = bookInfo;

        // setup velocity
        VelocityEngine velocity = new VelocityEngine();
        Properties p = new Properties();
        p.setProperty("resource.loaders", "class");
        p.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(p);
    }

    /*
    ** Expand the given template into given file, with the relevant info
    */
    void write(String templateName, String filename) {
        Template.this.write(templateName, filename, null);
    }
    void write(String templateName, String filename, Info info) {
        Template.this.write(templateName, filename, info, null);
    }
    void write(String templateName, String filename, Info info, List<String> items) {
        write(templateName, filename, info, items, null);
    }
    void write(String templateName, String filename, Info info, List<String> items, List<String> images) {
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
        if (images != null) {
            logger.info("Adding images [{}]", images.size());
            context.put("images", images);
        }

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
}
