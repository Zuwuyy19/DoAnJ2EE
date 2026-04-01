package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Chapter;
import Nhom100.DoAnJ2EE.repository.ChapterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {
    @Autowired
    private ChapterRepository repository;

    @GetMapping
    public List<Chapter> getAll() { return repository.findAll(); }

    @GetMapping("/{id}")
    public Chapter getById(@PathVariable Long id) { return repository.findById(id).orElse(null); }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Chapter create(@RequestBody Chapter entity) { return repository.save(entity); }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Chapter update(@PathVariable Long id, @RequestBody Chapter entity) {
        Chapter existing = repository.findById(id).orElse(null);
        if (existing != null) { 
            existing.setTitle(entity.getTitle());
            if(entity.getCourse() != null) existing.setCourse(entity.getCourse());
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
