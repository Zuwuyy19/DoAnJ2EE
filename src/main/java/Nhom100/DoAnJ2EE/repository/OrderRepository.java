package Nhom100.DoAnJ2EE.repository;

import Nhom100.DoAnJ2EE.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    Optional<Order> findByPaymentCode(String paymentCode);
}
