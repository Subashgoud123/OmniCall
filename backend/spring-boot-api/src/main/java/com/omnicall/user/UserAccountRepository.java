package com.omnicall.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByPhoneNumber(String phoneNumber);

    Optional<UserAccount> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select user from UserAccount user
            where user.deletedAt is null
              and user.id <> :self
              and lower(user.displayName) like concat('%', :name, '%')
            """)
    List<UserAccount> searchByName(UUID self, String name);
}
