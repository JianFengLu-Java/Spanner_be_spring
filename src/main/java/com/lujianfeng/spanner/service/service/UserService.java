package com.lujianfeng.spanner.service.service;

import com.lujianfeng.spanner.dto.user.UserLoginRequestDTO;
import com.lujianfeng.spanner.dto.user.UserGrowthChangeRequestDTO;
import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.dto.user.UserUpdateProfileRequestDTO;
import com.lujianfeng.spanner.dto.user.VipPurchaseRequestDTO;
import com.lujianfeng.spanner.dto.user.WalletAmountChangeRequestDTO;
import com.lujianfeng.spanner.dto.user.WalletTransferAcceptRequestDTO;
import com.lujianfeng.spanner.dto.user.WalletSecurityPasswordUpdateRequestDTO;
import com.lujianfeng.spanner.dto.user.WalletTransferRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.vo.user.LoginVO;
import com.lujianfeng.spanner.vo.user.PageResultVO;
import com.lujianfeng.spanner.vo.user.UserVO;
import com.lujianfeng.spanner.vo.user.VipOrderItemVO;
import com.lujianfeng.spanner.vo.user.VipPlanVO;
import com.lujianfeng.spanner.vo.user.VipProfileVO;
import com.lujianfeng.spanner.vo.user.VipPurchaseResultVO;
import com.lujianfeng.spanner.vo.user.WalletAccountVO;
import com.lujianfeng.spanner.vo.user.WalletChangeResultVO;
import com.lujianfeng.spanner.vo.user.WalletFlowItemVO;
import com.lujianfeng.spanner.vo.user.WalletTransferApplyResultVO;
import com.lujianfeng.spanner.vo.user.WalletTransferResultVO;
import org.springframework.stereotype.Service;

import java.util.List;

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

    WalletTransferApplyResultVO transferMyWallet(WalletTransferRequestDTO requestDTO);

    WalletTransferResultVO acceptMyWalletTransfer(WalletTransferAcceptRequestDTO requestDTO);

    PageResultVO<WalletFlowItemVO> listMyWalletFlows(Integer page, Integer size, String changeType);

    WalletAccountVO updateMyWalletSecurityPassword(WalletSecurityPasswordUpdateRequestDTO requestDTO);

    List<VipPlanVO> listVipPlans();

    VipProfileVO getMyVipProfile();

    VipPurchaseResultVO purchaseVip(VipPurchaseRequestDTO requestDTO);

    VipProfileVO addMyGrowth(UserGrowthChangeRequestDTO requestDTO);

    PageResultVO<VipOrderItemVO> listMyVipOrders(Integer page, Integer size);

}
