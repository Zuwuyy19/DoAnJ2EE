package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Category;
import Nhom100.DoAnJ2EE.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    @Autowired
    private CategoryRepository repository;

    @GetMapping
    public List<Category> getAll() { return repository.findAll(); }

    @GetMapping("/{id}")
    public Category getById(@PathVariable Long id) { return repository.findById(id).orElse(null); }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Category create(@RequestBody Category entity) { return repository.save(entity); }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Category update(@PathVariable Long id, @RequestBody Category entity) {
        Category existing = repository.findById(id).orElse(null);
        if (existing != null) { 
            existing.setName(entity.getName()); 
            return repository.save(existing); 
        }
        return null;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) { 
        repository.deleteById(id); 
        return "Deleted"; 
    }
}
