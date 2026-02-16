package com.lujianfeng.spanner.service.service;

import com.lujianfeng.spanner.dto.user.UserLoginRequestDTO;
import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.dto.user.UserUpdateProfileRequestDTO;
import com.lujianfeng.spanner.dto.user.WalletAmountChangeRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.vo.user.LoginVO;
import com.lujianfeng.spanner.vo.user.PageResultVO;
import com.lujianfeng.spanner.vo.user.UserVO;
import com.lujianfeng.spanner.vo.user.WalletAccountVO;
import com.lujianfeng.spanner.vo.user.WalletChangeResultVO;
import com.lujianfeng.spanner.vo.user.WalletFlowItemVO;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    UserVO register(UserRegisterRequestDTO userRegisterRequestDTO);

    LoginVO login(UserLoginRequestDTO userLoginRequestDTO);

    LoginVO refreshToken(String refreshToken);

    UserVO getUserInfo();

    UserVO updateUserInfo(UserUpdateProfileRequestDTO userUpdateProfileRequestDTO);

    UserEntity getCurrentUserEntity();

    WalletAccountVO getMyWallet();

    WalletChangeResultVO rechargeMyWallet(WalletAmountChangeRequestDTO requestDTO);

    WalletChangeResultVO consumeMyWallet(WalletAmountChangeRequestDTO requestDTO);

    PageResultVO<WalletFlowItemVO> listMyWalletFlows(Integer page, Integer size, String changeType);

}
