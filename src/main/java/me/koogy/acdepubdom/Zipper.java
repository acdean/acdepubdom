package me.koogy.acdepubdom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
** Zipper - create the final .epub
*/
public class Zipper {

    private static final Logger LOGGER = LoggerFactory.getLogger(Zipper.class);

    static void write(File dir, String filename) {
        ZipOutputStream zos = null;
        try {
            // zip everything to file.epub
            File zipFile = new File(filename.replace(".xml", ".epub"));
            FileOutputStream fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);

            // mimetype is first and uncompressed
            zos.setLevel(Deflater.NO_COMPRESSION);              // not compressed
            writeOne(zos, "mimetype", dir + "/mimetype", null); // always 20

            // META_INF/container.xml is only thing in a subdirectory
            zos.setMethod(ZipOutputStream.DEFLATED);
            zos.setLevel(Deflater.DEFAULT_COMPRESSION);
            writeOne(zos, "META-INF/container.xml", dir + "/META-INF/container.xml", null);

            // everything else
            File[] list = dir.listFiles();
            Arrays.sort(list);
            for (File f : list) {
                // ignore the things we've already zipped
                if (f.isDirectory() || f.getName().equals("mimetype")) {
                    LOGGER.info("File [{}] - ignoring", f.getAbsoluteFile());
                } else {
                    // zip all the other files
                    writeOne(zos, f.getAbsolutePath());
                }
            }
            LOGGER.info("Written [{}]", zipFile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (zos != null) {
                try {zos.close();} catch (Exception e) {e.printStackTrace();}
            }
        }
    }

    static void writeOne(ZipOutputStream zos, String filename) {
        LOGGER.info("WriteOne [{}]", filename);
        int index = filename.lastIndexOf("/");
        writeOne(zos, filename.substring(index + 1), filename, null);
    }
    static void writeOne(ZipOutputStream zos, String entryName, String filename, Integer size) {
        LOGGER.info("WriteOne [{}] - [{}]", entryName, filename);
        FileInputStream in = null;
        try {
            // name the file inside the zip file 
            ZipEntry zipEntry = new ZipEntry(entryName);
            if (size != null) {
                zipEntry.setSize(size);
            }
            zos.putNextEntry(zipEntry); 

            // buffer size
            byte[] b = new byte[1024];
            int count;

            in = new FileInputStream(filename);
            while ((count = in.read(b)) > 0) {
                zos.write(b, 0, count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try{in.close();} catch (Exception e) {e.printStackTrace();};
            }
        }
    }
}
