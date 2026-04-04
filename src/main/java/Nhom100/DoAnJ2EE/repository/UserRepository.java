package Nhom100.DoAnJ2EE.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import Nhom100.DoAnJ2EE.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
