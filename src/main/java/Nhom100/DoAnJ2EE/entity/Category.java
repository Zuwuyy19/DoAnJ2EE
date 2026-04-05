package Nhom100.DoAnJ2EE.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private List<Course> courses;

    @PreRemove
    private void preRemove() {
        if (courses != null) {
            for (Course course : courses) {
                course.setCategory(null);
            }
        }
    }
}
