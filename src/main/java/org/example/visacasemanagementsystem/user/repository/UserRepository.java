package org.example.visacasemanagementsystem.user.repository;

import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByEmail(String email, Sort type);


}
