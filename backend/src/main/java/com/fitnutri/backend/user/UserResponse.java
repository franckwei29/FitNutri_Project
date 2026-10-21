package com.fitnutri.backend.user;
public record UserResponse(String id,String email,String displayName,String role,boolean enabled,long version) {
    public static UserResponse from(User user) {return new UserResponse(user.getId().toString(),user.getEmail(),user.getDisplayName(),user.getRole().name(),user.isEnabled(),user.getVersion());}
}
