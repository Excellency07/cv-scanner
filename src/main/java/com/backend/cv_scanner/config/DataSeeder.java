package com.backend.cv_scanner.config;

import com.backend.cv_scanner.dao.entity.Account;
import com.backend.cv_scanner.dao.repository.jpa.AccountRepository;
import com.backend.cv_scanner.model.enums.AccountRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder implements CommandLineRunner {
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;

    public DataSeeder(PasswordEncoder passwordEncoder, AccountRepository accountRepository) {
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        String email = "admin@cvs.com";
        if (!accountRepository.existsByEmail(email)) {
            String password = "Admin.123";
            Account admin =  new Account(
                    "Admin",
                    email,
                    passwordEncoder.encode(password)

            );
            admin.setEnabled(true);
            admin.addRole(AccountRole.ADMIN);
            admin.addRole(AccountRole.HR);
            admin.addRole(AccountRole.DEFAULT);

            accountRepository.save(admin);
        }
    }
}
