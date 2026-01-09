package com.lujianfeng.spanner.security;

import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author mac
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;

    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String account) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByAccount(account);
        return new SecurityUser(user);
    }
}
