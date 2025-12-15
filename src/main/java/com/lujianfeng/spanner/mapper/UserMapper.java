package com.lujianfeng.spanner.mapper;

import com.lujianfeng.spanner.dto.UserRegisterRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.vo.UserVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserEntity toUserEntity(UserRegisterRequestDTO userRegisterRequestDTO);

    UserVO toUserVO(UserEntity userEntity);

}
