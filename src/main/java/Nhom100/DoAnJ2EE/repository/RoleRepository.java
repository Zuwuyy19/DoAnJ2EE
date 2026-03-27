package Nhom100.DoAnJ2EE.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import Nhom100.DoAnJ2EE.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}
