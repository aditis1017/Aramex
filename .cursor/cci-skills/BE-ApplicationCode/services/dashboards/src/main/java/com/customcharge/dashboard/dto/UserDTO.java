package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.dto.AbstractDto;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO extends AbstractDto {
    private UUID id;
    private String name;
    private String email;
    private Set<String> roles;
}