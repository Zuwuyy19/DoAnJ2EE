package Nhom100.DoAnJ2EE.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import Nhom100.DoAnJ2EE.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
