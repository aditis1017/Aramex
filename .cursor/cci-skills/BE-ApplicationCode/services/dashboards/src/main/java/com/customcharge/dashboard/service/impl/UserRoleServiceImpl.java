package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.UserRoleConstants;
import com.customcharge.dashboard.constants.UserRoleLogMessages;
import com.customcharge.dashboard.dto.RoleOptionDTO;
import com.customcharge.dashboard.dto.UpdateUserRoleRequestDTO;
import com.customcharge.dashboard.dto.UserRoleDetailsDTO;
import com.customcharge.persistencelayer.dto.UserSummaryDTO;
import com.customcharge.persistencelayer.repository.ModuleRepository;
import com.customcharge.persistencelayer.repository.RoleRepository;
import com.customcharge.persistencelayer.repository.UserRepository;
import com.customcharge.dashboard.service.UserRoleService;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import com.customcharge.persistencelayer.entities.Module;
import com.customcharge.persistencelayer.entities.Role;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ModuleRepository moduleRepository;

    @Override
    @Transactional(readOnly = false)
    public void createDefaultUsers() {

        if (userRepository.count() > 0) {
            log.debug(UserRoleLogMessages.SKIP_DEFAULT_USER_CREATION);
            return;
        }

        Role defaultRole = roleRepository.findByCode(UserRoleConstants.DEFAULT_ROLE_CODE)
                .or(() -> roleRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new IllegalStateException(UserRoleConstants.NO_ROLES_FOR_DEFAULT_USERS));

        List<User> users = UserRoleConstants.DEFAULT_USERS.stream()
                .map(row -> createUser(row[0], row[1], defaultRole))
                .toList();

        userRepository.saveAll(users);
        log.info(UserRoleLogMessages.CREATED_DEFAULT_USERS, users.size());
    }

    private User createUser(String username, String email, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(UserRoleConstants.DEFAULT_USER_PASSWORD); // TODO: encode in real system
        user.setActive(true);
        user.setRole(role);
        return user;
    }

    @Override
    @Transactional(readOnly = false)
    public void createModules() {

        for (String[] row : UserRoleConstants.DEFAULT_MODULES) {
            String code = row[0];
            String display = row[1];
            boolean exists = moduleRepository.findByCode(code).isPresent();
            if (!exists) {
                Module module = new Module();
                module.setCode(code);
                module.setDisplayName(display);
                moduleRepository.save(module);
            }
        }
    }

    @Override
    public List<UserSummaryDTO> getUserOverview() {
        return userRepository.fetchUserSummary();
    }

    @Override
    public UserRoleDetailsDTO getUserRoleDetails(Long userId) {

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(UserRoleConstants.USER_NOT_FOUND));

            List<Role> roles = roleRepository.findAll();

            List<RoleOptionDTO> roleOptions = roles.stream()
                    .map(RoleOptionDTO::new)
                    .toList();

            return new UserRoleDetailsDTO(user, roleOptions);

        } catch (AbstractApiException ex) {
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = false)
    public MessageResponse updateUserRole(Long userId, UpdateUserRoleRequestDTO request) {
        if (Objects.isNull(userId)) {
            throw new BadRequestException(UserRoleConstants.USER_ID_CANNOT_BE_NULL);
        }
        if (Objects.isNull(request) || Objects.isNull(request.getRoleId())) {
            throw new BadRequestException(UserRoleConstants.ROLE_ID_REQUIRED);
        }
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(UserRoleConstants.USER_NOT_FOUND));
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException(UserRoleConstants.ROLE_NOT_FOUND));
            user.setRole(role);
            userRepository.save(user);
            log.info(UserRoleLogMessages.USER_ROLE_UPDATED, userId, request.getRoleId());
            return new MessageResponse(UserRoleConstants.SUCCESS_RESPONSE);
        } catch (AbstractApiException ex) {
            throw ex;
        }
    }
}
