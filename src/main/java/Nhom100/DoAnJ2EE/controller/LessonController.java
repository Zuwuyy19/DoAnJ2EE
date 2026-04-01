package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Lesson;
import Nhom100.DoAnJ2EE.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    @Autowired
    private LessonRepository repository;

    @GetMapping
    public List<Lesson> getAll() { return repository.findAll(); }

    @GetMapping("/{id}")
    public Lesson getById(@PathVariable Long id) { return repository.findById(id).orElse(null); }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Lesson create(@RequestBody Lesson entity) { return repository.save(entity); }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Lesson update(@PathVariable Long id, @RequestBody Lesson entity) {
        Lesson existing = repository.findById(id).orElse(null);
        if (existing != null) { 
            existing.setTitle(entity.getTitle());
            existing.setVideoUrl(entity.getVideoUrl());
            if(entity.getChapter() != null) existing.setChapter(entity.getChapter());
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
