package Nhom100.DoAnJ2EE.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "order_details")
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private Course course;

    private Double price;

    public OrderDetail() {
    }

    public OrderDetail(Order order, Course course, Double price) {
        this.order = order;
        this.course = course;
        this.price = price;
    }
}
