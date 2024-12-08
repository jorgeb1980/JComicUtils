package comics.utils;

import test.Sandbox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.fail;

public class Tools {

    public enum TestLevel { SERVICE, COMMAND; }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    public static Sandbox sandbox() {
        var sb = Sandbox.sandbox();
        // Terrible hacks used to avoid having to use mockito
        System.setProperty("user.home", sb.getSandbox().getAbsolutePath());
        return sb;
    }

    public static String md5(File f) {
        var ret = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(Files.readAllBytes(f.toPath()));
            byte[] digest = md.digest();
            ret = Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            fail(e);
        }
        return ret;
    }

    public static void createNewFile(File f) throws IOException {
        if (!f.createNewFile()) fail(String.format("Could not create file %s", f));
    }

    public static void mkdir(File f) throws IOException {
        if (!f.mkdir()) fail(String.format("Could not create directory %s", f));
    }
}
