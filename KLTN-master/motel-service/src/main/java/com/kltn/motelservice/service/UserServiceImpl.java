/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kltn.motelservice.service;

import com.kltn.motelservice.entity.Role;
import com.kltn.motelservice.entity.RoleName;
import com.kltn.motelservice.entity.User;
import com.kltn.motelservice.exception.UserException;
import com.kltn.motelservice.exception.WrongPasswordException;
import com.kltn.motelservice.model.AccountDto;
import com.kltn.motelservice.model.UserDTO;
import com.kltn.motelservice.repository.RoleRepository;
import com.kltn.motelservice.repository.UserRepository;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User selectUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public Role selectRoleByName(RoleName name) {
        return roleRepository.findByName(name);
    }

    @Override
    public User changePassword(Long id, String newPassword, String oldPassword, String role) throws Exception {
        User user = selectUserById(id);

        if (role.equals("ROLE_USER")) {
            if (!passwordEncoder.matches(oldPassword, user.getPassword()))
                throw new WrongPasswordException("M???t kh???u kh??ng ????ng");
        }

        user.setPassword(passwordEncoder.encode(newPassword));

        return userRepository.save(user);
    }

    @Override
    public Page<User> selectPageOfUsersInRoles(Pageable page, List<String> rolesString) {
        List<Role> roles = rolesString.stream()
                .map(RoleName::valueOf)
                .map(this::selectRoleByName).collect(Collectors.toList());
        return userRepository.findAllByRolesIn(page, roles);
    }

    @Override
    public User blockUserById(Long id, boolean block) {
        User user = selectUserById(id);
        user.setBlock(block);
        return userRepository.save(user);
    }

    @Override
    public User selectUserById(Long id) {
        return userRepository.findById(id).get();
    }

    @Override
    public User changeProfile(UserDTO userDTO, boolean admin) {
        User user = selectUserById(userDTO.getId());
        user.setPhone(userDTO.getPhone());
        user.setFullName(userDTO.getFullName());
        user.setAddress(userDTO.getAddress());
        user.setFileType(userDTO.getFileType());
        if (!admin)
            user.setB64(userDTO.getB64());

        return userRepository.save(user);
    }

    @Override
    public User changeRole(Long id, List<RoleName> role) {
        User user = selectUserById(id);
        user.setRoles(role.stream().map(this::selectRoleByName).collect(Collectors.toList()));
        return userRepository.save(user);
    }

    @Override
    public User registration(AccountDto accountDto) {
        userRepository.findByEmail(accountDto.getEmail()).ifPresent((user) -> new UserException("Email ???? t???n t???i"));

        User user = new User();
        if (accountDto.getFullName().trim().length() <= 0)
            user.setFullName(user.getEmail());
        else
            user.setFullName(accountDto.getFullName());
        user.setEmail(accountDto.getEmail());
        user.setAddress(accountDto.getAddress());
        user.setPhone(accountDto.getPhone());
        user.setPassword(passwordEncoder.encode(accountDto.getPassword()));
        if (accountDto.getRole() != null && !accountDto.getRole().isEmpty()) {
            user.setRoles(accountDto.getRole().stream().map(this::selectRoleByName).collect(Collectors.toList()));
        } else {
            user.setRoles(Arrays.asList(selectRoleByName(RoleName.ROLE_USER)));
        }
        return userRepository.save(user);
    }

    @Override
    public void changeAvatar(Long id, byte[] fileBytes) {
        User user = userRepository.findById(id).orElseThrow(() -> new ObjectNotFoundException(id, User.class.getSimpleName()));
        user.setB64(Base64.getEncoder().encodeToString(fileBytes));
        userRepository.save(user);
    }


}
