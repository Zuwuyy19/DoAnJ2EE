package Nhom100.DoAnJ2EE.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import Nhom100.DoAnJ2EE.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
}
