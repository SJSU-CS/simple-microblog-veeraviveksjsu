package edu.sjsu.cmpe272.simpleblog.server.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserTable, String> {
    UserTable findByUserName(String userName);
}
