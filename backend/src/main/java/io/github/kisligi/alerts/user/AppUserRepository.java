package io.github.kisligi.alerts.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    List<AppUser> findAllByOrderByIdAsc();
}
