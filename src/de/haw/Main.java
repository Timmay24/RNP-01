package de.haw;

public class Main {

    public static void main(String[] args) {
//        MailFile mf = new MailFile(args[0], args[1]);
        MailFile mf = new MailFile("tim.hartig@haw-hamburg.de", "foobar.txt");
        mf.sendMail();
    }
}
