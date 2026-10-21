package com.fitnutri.backend.records;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
@Component
public class RecordMutationGuard {
    private final UserRepository users;private final ZoneResolver zones;
    public RecordMutationGuard(UserRepository users,ZoneResolver zones){this.users=users;this.zones=zones;}
    public void lockOwner(Long owner){var u=users.lockById(owner).filter(User::isEnabled).orElseThrow(()->new BusinessException(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Sign in to continue."));if(u.getRole()!=User.Role.USER)throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","This operation requires a user account.");}
    public void id(Long id){if(id==null||id<1)throw BusinessException.invalid("id","Choose a valid record identity.");}
    public void versionInput(Long expected){if(expected==null||expected<0)throw BusinessException.invalid("expectedVersion","Use the current record version.");}
    public void version(BaseRecord row,Long expected){versionInput(expected);if(row.getVersion()!=expected)throw new BusinessException(HttpStatus.CONFLICT,"VERSION_CONFLICT","Reload and compare the current record before confirming changes.");}
    public void zone(String zone){zones.resolve(zone);}
    public BusinessException missing(){return new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Record not found.");}
}
