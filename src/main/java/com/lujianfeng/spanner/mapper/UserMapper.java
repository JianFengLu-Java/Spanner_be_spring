package com.lujianfeng.spanner.mapper;

import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.vo.user.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "account", ignore = true),
            @Mapping(target = "password", ignore = true),
            @Mapping(target = "roles", ignore = true)
    })
    UserEntity toUserEntity(UserRegisterRequestDTO dto);

    @Mappings({
            @Mapping(target = "isVip", expression = "java(entity != null && entity.getVipExpireAt() != null && entity.getVipExpireAt().isAfter(java.time.LocalDateTime.now()))"),
            @Mapping(target = "growthValue", expression = "java(entity == null || entity.getGrowthValue() == null ? 0L : entity.getGrowthValue())"),
            @Mapping(target = "vipLevel", expression = "java(entity == null || entity.getUserLevel() == null || entity.getUserLevel() < 1 ? 1 : entity.getUserLevel())"),
            @Mapping(target = "userLevel", expression = "java(entity == null || entity.getUserLevel() == null || entity.getUserLevel() < 1 ? 1 : entity.getUserLevel())")
    })
    UserVO toUserVO(UserEntity entity);
}
