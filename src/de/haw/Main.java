package de.haw;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
//        MailFile mf = new MailFile(args[0], args[1]);
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        MailFile mf = new MailFile("fabian.pfaff@haw-hamburg.de", s + "/docker.png");
        mf.sendMail();
    }
}
