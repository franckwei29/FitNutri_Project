package com.fitnutri.backend.user;
import java.util.Locale;
public final class EmailCanonicalizer {
    private EmailCanonicalizer() {}
    public static String canonical(String email) {return email==null?null:email.trim().toLowerCase(Locale.ROOT);}
}
