package com.fitnutri.backend.admin;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/users")
public class AdminController {
    private final AdminService service;
    public AdminController(AdminService service){this.service=service;}
    @GetMapping public ResponseEntity<AdminUsersResponse> listUsers(Authentication auth,@RequestParam(required=false) String query,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return noStore(service.searchAccounts(auth,query,page,size));}
    @PatchMapping("/{id}/deactivate") public ResponseEntity<AdminUserSummary> deactivate(Authentication auth,@PathVariable Long id,@Valid @RequestBody ChangeAccountStatusRequest request){return noStore(service.changeStatus(auth,id,request,false));}
    @PatchMapping("/{id}/reactivate") public ResponseEntity<AdminUserSummary> reactivate(Authentication auth,@PathVariable Long id,@Valid @RequestBody ChangeAccountStatusRequest request){return noStore(service.changeStatus(auth,id,request,true));}
    @PostMapping("/{id}/reset-password") public ResponseEntity<Void> resetPassword(Authentication auth,@PathVariable Long id,@Valid @RequestBody ResetPasswordRequest request){service.resetPassword(auth,id,request);return ResponseEntity.noContent().header("Cache-Control","no-store").build();}
    private static <T> ResponseEntity<T> noStore(T body){return ResponseEntity.ok().header("Cache-Control","no-store").body(body);}
}
