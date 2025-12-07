package com.lujianfeng.spanner.mapper;

import com.lujianfeng.spanner.dto.UserDTO;
import com.lujianfeng.spanner.entity.UserEntity;
import com.lujianfeng.spanner.vo.UserVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper{

    UserEntity toUserEntity(UserDTO userDTO);

    UserVO toUserVO(UserEntity userEntity);

}
