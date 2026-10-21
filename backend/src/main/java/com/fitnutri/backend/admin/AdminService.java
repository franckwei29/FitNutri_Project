package com.fitnutri.backend.admin;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.*;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class AdminService {
    private final UserRepository users;private final CurrentUserService current;private final PasswordEncoder passwords;private final Validator validator;private final Clock clock;
    public AdminService(UserRepository users,CurrentUserService current,PasswordEncoder passwords,Validator validator,Clock clock){this.users=users;this.current=current;this.passwords=passwords;this.validator=validator;this.clock=clock;}
    public AdminUsersResponse searchAccounts(Authentication auth,String query,int page,int size){
        if(page<0||(long)page*Math.max(size,1)>Integer.MAX_VALUE)throw BusinessException.invalid("page","Choose a supported nonnegative page.");
        if(size<1||size>100)throw BusinessException.invalid("size","Choose a page size from 1 to 100.");
        if(query!=null&&query.length()>200)throw BusinessException.invalid("query","Use at most 200 characters.");
        requireAdmin(auth);
        String value=query==null?"":query.strip().toLowerCase(Locale.ROOT);
        Specification<User> spec=(root,cq,cb)->value.isEmpty()?cb.conjunction():cb.or(cb.like(cb.lower(root.get("email")),"%"+escape(value)+"%",'\\'),cb.like(cb.lower(root.get("displayName")),"%"+escape(value)+"%",'\\'));
        var found=users.findAll(spec,PageRequest.of(page,size,Sort.by("id")));
        return new AdminUsersResponse(found.getContent().stream().map(AdminUserSummary::from).toList(),found.getTotalElements(),page,size,found.hasNext());
    }
    @Transactional public AdminUserSummary changeStatus(Authentication auth,Long id,ChangeAccountStatusRequest request,boolean enabled){
        validate(request);validateId(id);
        // All admin writes acquire this same ordered set before the actor/target read.
        // The second concurrent request rechecks active authority after waiting for the lock.
        var admins=users.lockAdministrators();User actor=requireAdmin(auth);User target=target(id);
        requireVersion(target,request.expectedVersion());
        if(!enabled&&target.getRole()==User.Role.ADMIN&&(actor.getId().equals(id)||admins.stream().filter(User::isEnabled).count()<=1))
            throw new BusinessException(HttpStatus.CONFLICT,"ACCOUNT_PROTECTED","You cannot deactivate yourself or the last active administrator.");
        if(target.isEnabled()!=enabled){target.setEnabled(enabled);target.setUpdatedAt(clock.instant());users.flush();}
        return AdminUserSummary.from(target);
    }
    @Transactional public void resetPassword(Authentication auth,Long id,ResetPasswordRequest request){
        validate(request);validateId(id);
        if(request.newPassword().getBytes(StandardCharsets.UTF_8).length>72)throw BusinessException.invalid("newPassword","Use at most 72 UTF-8 bytes.");
        users.lockAdministrators();requireAdmin(auth);User target=target(id);requireVersion(target,request.expectedVersion());
        target.setPasswordHash(passwords.encode(request.newPassword()));target.setUpdatedAt(clock.instant());users.flush();
    }
    private User requireAdmin(Authentication auth){User actor=current.requireActive(auth);if(actor.getRole()!=User.Role.ADMIN)throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","This operation requires an administrator account.");return actor;}
    private User target(Long id){return users.lockById(id).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Account not found."));}
    private void validate(Object request){if(request==null)throw BusinessException.invalid("body","Enter the required account change.");if(!validator.validate(request).isEmpty())throw BusinessException.invalid("body","Enter a valid version and account change.");}
    private static void validateId(Long id){if(id==null||id<1)throw BusinessException.invalid("id","Choose a valid account identity.");}
    private static void requireVersion(User target,long expected){if(target.getVersion()!=expected)throw new BusinessException(HttpStatus.CONFLICT,"VERSION_CONFLICT","Reload and review the latest account state.");}
    private static String escape(String value){return value.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
}
