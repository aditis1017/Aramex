package com.customcharge.dashboard.service;

import com.customcharge.dashboard.dto.PermissionMatrixDTO;
import java.util.List;

public interface PermissionService {
    List<PermissionMatrixDTO> getMatrix(Integer page, Integer size);
}