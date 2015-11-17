package de.haw;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--help")) {
            System.out.println("Usage: MailFile.jar <recipient mail address> <attachment>");
        } else if (args.length == 2 && !args[0].isEmpty() && !args[1].isEmpty()) {
            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            MailFile mf = new MailFile(args[0], args[1]);
            mf.sendMail();
//            MailFile mf = new MailFile("fabian.pfaff@haw-hamburg.de", s + "/docker.png");
//            MailFile mf = new MailFile("tim.hartig@haw-hamburg.de", s + "/docker.png");
        } else {
            System.out.println("Illegal arguments. Script aborting.");
        }
    }
}
