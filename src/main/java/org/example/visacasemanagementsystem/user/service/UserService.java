package org.example.visacasemanagementsystem.user.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.example.visacasemanagementsystem.audit.UserEventType;
import org.example.visacasemanagementsystem.audit.service.UserLogService;
import org.example.visacasemanagementsystem.exception.UnauthorizedException;
import org.example.visacasemanagementsystem.user.UserAuthorization;
import org.example.visacasemanagementsystem.user.dto.CreateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UpdateUserDTO;
import org.example.visacasemanagementsystem.user.dto.UserDTO;
import org.example.visacasemanagementsystem.user.entity.User;
import org.example.visacasemanagementsystem.user.mapper.UserMapper;
import org.example.visacasemanagementsystem.user.repository.UserRepository;
import org.example.visacasemanagementsystem.user.security.UserPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@PreAuthorize("isAuthenticated()")
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private static final String USER_NOT_FOUND = "User not found";
    private final PasswordEncoder passwordEncoder;
    private final UserLogService userLogService;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       UserLogService userLogService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.userLogService = userLogService;
    }

    @PreAuthorize("hasRole('SYSADMIN')")
    public List<UserDTO> findAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .toList();
    }

    public Optional<UserDTO> findById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO);
    }

    public Optional<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDTO);
    }

    @PreAuthorize("permitAll()")
    @Transactional
    public UserDTO createUser(@Valid CreateUserDTO dto) {
        User user = userMapper.toEntity(dto);
        if (dto.password().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setUserAuthorization(UserAuthorization.USER);
        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("A user with this email already exists", e);
        }
        // For self-creation via signup the actor is the new user themselves.
        // If admin-initiated user creation is added later, introduce an overload that
        // accepts an explicit actorUserId.
        userLogService.createUserLog(
                savedUser.getId(),
                savedUser.getId(),
                UserEventType.CREATED,
                "User account created via signup."
        );
        return userMapper.toDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUser(UpdateUserDTO dto, Long actorUserId) {
        validateProfileAccess(getUserPrincipal(), dto.id());
        // Check if user and email exists
        User user = userRepository.findById(dto.id())
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        if (!dto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        User savedUser;
        try {
            userMapper.updateEntityFromDTO(dto, user);
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Email is no longer mutable on this path, but other unique/integrity constraints
            // (now or in the future) could still trip here, so the message stays generic.
            throw new IllegalArgumentException("Data integrity violation while updating user", e);
        }
        userLogService.createUserLog(
                actorUserId,
                savedUser.getId(),
                UserEventType.UPDATED,
                "User profile updated (fullName/password)."
        );
        return userMapper.toDTO(savedUser);
    }

    /**
     * OAuth login lookup-or-create. Used by OauthSuccessHandler to keep the persistence work
     * inside a single short-lived transaction that commits before the success handler builds
     * the SecurityContext and issues the redirect.
     */
    @PreAuthorize("permitAll()")
    @Transactional
    public User findOrCreateOauthUser(String email, String fullName) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setFullName(fullName);
            newUser.setUsername(email);
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setUserAuthorization(UserAuthorization.USER);
            try {
                User savedUser = userRepository.saveAndFlush(newUser);
                userLogService.createUserLog(
                        savedUser.getId(),
                        savedUser.getId(),
                        UserEventType.CREATED,
                        "User account created via OAUTH2."
                );
                return savedUser;
            } catch (DataIntegrityViolationException e) {
                // Race: another concurrent OAuth login created the row first. Re-read.
                return userRepository.findByEmail(email).orElseThrow();
            }
        });
    }

    @PreAuthorize("hasRole('SYSADMIN')")
    @Transactional
    public UserDTO updateUserAuthorization(Long actorUserId, Long targetUserId, UserAuthorization newAuth) {
        if (newAuth == null) {
            throw new IllegalArgumentException("New authorization cannot be null");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        UserAuthorization previousAuth = user.getUserAuthorization();
        user.setUserAuthorization(newAuth);
        User savedUser = userRepository.save(user);
        userLogService.createUserLog(
                actorUserId,
                savedUser.getId(),
                UserEventType.AUTHORIZATION_CHANGED,
                "Authorization changed: " + previousAuth + " -> " + newAuth
        );
        return userMapper.toDTO(savedUser);
    }

    @PreAuthorize("hasRole('SYSADMIN')")
    @Transactional
    public void deleteUser(Long actorUserId, Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
        userRepository.delete(user);
        userLogService.createUserLog(
                actorUserId,
                targetUserId,
                UserEventType.DELETED,
                "User account deleted."
        );
    }

    public void validateProfileAccess(UserPrincipal principal, Long userId) {
        boolean isOwnProfile = principal.getUserId().equals(userId);
        boolean isSysAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_SYSADMIN"));

        if (!isOwnProfile && !isSysAdmin) {
            throw new UnauthorizedException("You do not have permission to edit this profile.");
        }
    }

    private static UserPrincipal getUserPrincipal() {
        // Class-level @PreAuthorize("isAuthenticated()") guarantees an authentication is present.
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new IllegalStateException(
                    "Expected UserPrincipal in SecurityContext but got: "
                            + (principal == null ? "null" : principal.getClass().getName()));
        }
        return userPrincipal;
    }
}
