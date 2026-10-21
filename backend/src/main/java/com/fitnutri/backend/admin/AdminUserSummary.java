package com.fitnutri.backend.admin;
import com.fitnutri.backend.user.User;
/** Deliberately excludes profile, safety answers, records, timestamps and password material. */
public record AdminUserSummary(String id,String email,String displayName,String role,boolean enabled,long version) {
    public static AdminUserSummary from(User user){return new AdminUserSummary(user.getId().toString(),user.getEmail(),user.getDisplayName(),user.getRole().name(),user.isEnabled(),user.getVersion());}
}
