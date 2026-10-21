package com.fitnutri.backend.user;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.common.FieldViolation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final Validator validator;
    private final Clock clock;
    public AuthService(UserRepository users,PasswordEncoder passwords,Validator validator,Clock clock) {
        this.users=users;this.passwords=passwords;this.validator=validator;this.clock=clock;
    }
    @Transactional
    public UserResponse register(RegisterRequest request) {
        validate(request);
        if(users.findByCanonicalEmail(request.email()).isPresent()) throw duplicate();
        return create(request,User.Role.USER);
    }
    @Transactional
    public UserResponse bootstrapAdmin(RegisterRequest request) {
        validate(request);
        var existing=users.findByCanonicalEmail(request.email());
        if(existing.isPresent()) {
            if(existing.get().getRole()!=User.Role.ADMIN) throw new BusinessException(HttpStatus.CONFLICT,"ADMIN_BOOTSTRAP_CONFLICT","The configured identity is already a user account; no role was changed.");
            return UserResponse.from(existing.get());
        }
        return create(request,User.Role.ADMIN);
    }
    private UserResponse create(RegisterRequest request,User.Role role) {
        User user=new User();user.setEmail(request.email());user.setDisplayName(request.displayName());user.setRole(role);
        user.setPasswordHash(passwords.encode(request.password()));user.setCreatedAt(clock.instant());user.setUpdatedAt(clock.instant());
        try {return UserResponse.from(users.saveAndFlush(user));}
        catch(DataIntegrityViolationException e) {throw duplicate();} // Exits/rolls back transaction; never continues a failed transaction.
    }
    private void validate(RegisterRequest request) {
        if(request==null) throw BusinessException.invalid("body","Enter the required registration details.");
        var errors=validator.validate(request);
        if(!errors.isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Check the highlighted fields.",errors.stream().map(e->new FieldViolation(e.getPropertyPath().toString(),null,"Enter a valid value for this field.")).toList());
        if(request.password().getBytes(StandardCharsets.UTF_8).length>72) throw BusinessException.invalid("password","Use a password of at most 72 UTF-8 bytes.");
    }
    private static BusinessException duplicate() {return new BusinessException(HttpStatus.CONFLICT,"EMAIL_ALREADY_REGISTERED","This email is already registered.");}
}
