package Nhom100.DoAnJ2EE.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User saveUser(User user) {
        // If it's a new user or password is changed, we should encode it
        // For this admin CRUD, we'll primarily use it for role updates
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setEnabled(false);
            userRepository.save(user);
        });
    }
}
