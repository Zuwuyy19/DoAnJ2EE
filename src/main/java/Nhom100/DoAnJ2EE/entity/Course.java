package Nhom100.DoAnJ2EE.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String title;
    private String description;
    private String image;
    private Double price;

    public Course() {}

    public Course(Long id, String name, String title, String description, String image, Double price) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.description = description;
        this.image = image;
        this.price = price;
    }
}