package me.koogy.acdepubdom;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
** Toc
*/
public class Toc {

    private static final Logger logger = LoggerFactory.getLogger(Toc.class);
    public static final String TOC_TMPL = "toc.vm";

    VelocityContext tocContext = new VelocityContext();
    Writer tocWriter;

    Toc(Info bookInfo, File directory) throws Exception {
        tocWriter = new FileWriter(new File(directory, Book.TOC_FILE));
        tocContext.put("book", bookInfo);
        merge("toc_header.vm");
    }

    void start(String title, String cls, String filename, int tocIndex) {
        tocContext.put("title", title);
        tocContext.put("class", cls);
        tocContext.put("filename", filename);
        tocContext.put("index", tocIndex);
        merge("toc_entry_start.vm");
    }

    void end() {
        merge("toc_entry_end.vm");
    }

    void close() throws Exception {
        merge("toc_footer.vm");
        tocWriter.close();
    }

    private void merge(String filename) {
        Velocity.mergeTemplate("velocity" + File.separator + filename, "UTF-8", tocContext, tocWriter);
    }
}
