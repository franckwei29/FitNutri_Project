package com.fitnutri.backend.admin;
import java.util.List;
public record AdminUsersResponse(List<AdminUserSummary> results,long totalMatches,int page,int size,boolean hasMore) {
    public AdminUsersResponse{results=List.copyOf(results);}
}
