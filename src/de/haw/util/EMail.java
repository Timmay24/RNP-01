package de.haw.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class EMail {

    private String mailAddress;
    private static final String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";

    private EMail() {
        super();
    }

    public EMail(String eMailAddress) {
        if 	(!verifyCorrectnessOf(eMailAddress)) {
            throw new IllegalArgumentException(eMailAddress + " is not a valid E-Mail address.");
        }
        this.mailAddress = eMailAddress;
    }

    private boolean verifyCorrectnessOf(String eMailAddress) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(eMailAddress);
        return matcher.matches();
    }

    public String getMailAddress() {
        return mailAddress;
    }

    private void setMailAddress(String eMailAddress) {
        this.mailAddress = eMailAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((mailAddress == null) ? 0 : mailAddress.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EMail other = (EMail) obj;
        if (mailAddress == null) {
            if (other.mailAddress != null)
                return false;
        } else if (!mailAddress.equals(other.mailAddress))
            return false;
        return true;
    }

}
