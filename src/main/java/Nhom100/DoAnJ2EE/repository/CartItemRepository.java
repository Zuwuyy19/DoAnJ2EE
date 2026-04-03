package Nhom100.DoAnJ2EE.repository;

import Nhom100.DoAnJ2EE.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
