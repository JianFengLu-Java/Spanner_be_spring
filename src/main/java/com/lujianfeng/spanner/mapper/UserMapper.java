package com.lujianfeng.spanner.mapper;

import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.vo.user.UserVO;
import org.mapstruct.Mapper;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserEntity toUserEntity(UserRegisterRequestDTO userRegisterRequestDTO);

    UserVO toUserVO(UserEntity userEntity);

}
