package com.nationalteammanager.service;

import com.nationalteammanager.dto.RegistrationForm;
import com.nationalteammanager.entity.User;
import com.nationalteammanager.enums.SupportedLanguage;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.UserRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationForm form) {
        String username = form.getUsername().trim();
        String email = form.getEmail().trim();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessRuleException("error.user.usernameTaken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("error.user.emailTaken");
        }
        String passwordHash = passwordEncoder.encode(form.getPassword());
        String language = SupportedLanguage.fromCode(LocaleContextHolder.getLocale().getLanguage())
                .orElse(SupportedLanguage.DEFAULT).getCode();
        return userRepository.save(new User(form.getFullName().trim(), username, email, passwordHash, language));
    }

    @Transactional
    public void changeLanguage(Long id, SupportedLanguage language) {
        getById(id).changeLanguage(language.getCode());
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound"));
    }
}
