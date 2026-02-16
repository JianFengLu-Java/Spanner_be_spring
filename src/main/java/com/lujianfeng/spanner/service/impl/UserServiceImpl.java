package com.lujianfeng.spanner.service.impl;

import com.lujianfeng.spanner.dto.user.UserLoginRequestDTO;
import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.dto.user.UserUpdateProfileRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.WalletAccountEntity;
import com.lujianfeng.spanner.entity.user.WalletFlowEntity;
import com.lujianfeng.spanner.mapper.UserMapper;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.repository.WalletAccountRepository;
import com.lujianfeng.spanner.repository.WalletFlowRepository;
import com.lujianfeng.spanner.security.SecurityUser;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.util.JwtUtil;
import com.lujianfeng.spanner.dto.user.WalletAmountChangeRequestDTO;
import com.lujianfeng.spanner.vo.user.LoginVO;
import com.lujianfeng.spanner.vo.user.PageResultVO;
import com.lujianfeng.spanner.vo.user.UserVO;
import com.lujianfeng.spanner.vo.user.WalletAccountVO;
import com.lujianfeng.spanner.vo.user.WalletChangeResultVO;
import com.lujianfeng.spanner.vo.user.WalletFlowItemVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/31
 * @since 1.0
 */

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletFlowRepository walletFlowRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtUtil jwtUtil;


    public UserServiceImpl(UserRepository userRepository,
                           WalletAccountRepository walletAccountRepository,
                           WalletFlowRepository walletFlowRepository,
                           UserMapper userMapper,
                           BCryptPasswordEncoder bCryptPasswordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletFlowRepository = walletFlowRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserVO register(UserRegisterRequestDTO userRegisterRequestDTO) {
        //UserEntity是用户实体类，保存了所有的用户信息
        log.info("userRegisterRequestDTO={}", userRegisterRequestDTO.toString());
        UserEntity userEntity = userMapper.toUserEntity(userRegisterRequestDTO);
        log.info("userEntity={}", userEntity.toString());
        Long userAccount = userRepository.nextAccount();
        log.info("userAccount={}", userAccount);
        log.info("userAccountToString={}", userAccount.toString());
        userEntity.setAccount(userAccount.toString());
        userEntity.setPassword(bCryptPasswordEncoder.encode(userRegisterRequestDTO.getPassword()));
        UserEntity user = userRepository.save(userEntity);
        createWalletIfAbsent(user);
        return userMapper.toUserVO(user);
    }

    @Override
    public UserVO getUserInfo() {
        UserEntity user = getCurrentUserEntity();
        if (user == null) {
            return null;
        }
        return userMapper.toUserVO(user);
    }

    @Override
    public UserVO updateUserInfo(UserUpdateProfileRequestDTO userUpdateProfileRequestDTO) {
        UserEntity user = getCurrentUserEntity();
        if (user == null || userUpdateProfileRequestDTO == null) {
            return null;
        }

        if (userUpdateProfileRequestDTO.getRealName() != null && !userUpdateProfileRequestDTO.getRealName().isBlank()) {
            user.setRealName(userUpdateProfileRequestDTO.getRealName());
        }
        if (userUpdateProfileRequestDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(userUpdateProfileRequestDTO.getAvatarUrl());
        }
        if (userUpdateProfileRequestDTO.getGender() != null) {
            user.setGender(userUpdateProfileRequestDTO.getGender());
        }
        if (userUpdateProfileRequestDTO.getEmail() != null) {
            user.setEmail(userUpdateProfileRequestDTO.getEmail());
        }
        if (userUpdateProfileRequestDTO.getPhone() != null) {
            user.setPhone(userUpdateProfileRequestDTO.getPhone());
        }
        if (userUpdateProfileRequestDTO.getAddress() != null) {
            user.setAddress(userUpdateProfileRequestDTO.getAddress());
        }
        if (userUpdateProfileRequestDTO.getSignature() != null) {
            user.setSignature(userUpdateProfileRequestDTO.getSignature());
        }
        if (userUpdateProfileRequestDTO.getAge() != null) {
            user.setAge(userUpdateProfileRequestDTO.getAge());
        }

        UserEntity updated = userRepository.save(user);
        return userMapper.toUserVO(updated);
    }

    @Override
    public UserEntity getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser securityUser)) {
            return null;
        }
        return securityUser.userEntity();
    }

    @Override
    public LoginVO login(UserLoginRequestDTO userLoginRequestDTO) {
        String account = userLoginRequestDTO.getAccount();
        log.info(account);
        UserEntity user = userRepository.findByAccount(account);
        if (user == null) {
            return LoginVO.builder()
                    .code(401L)
                    .token(null)
                    .refreshToken(null)
                    .message("用户不存在")
                    .build();
        }
        boolean isPass = bCryptPasswordEncoder.matches(userLoginRequestDTO.getPassword(), user.getPassword());
        if (isPass) {

            UserVO userVO = userMapper.toUserVO(user);

            String accessToken = jwtUtil.generateAccessToken(account);
            String refreshToken = jwtUtil.generateRefreshToken(account);
            return LoginVO.builder()
                    .code(200L)
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(jwtUtil.getAccessTokenExpiresInSeconds())
                    .message("登录成功！")
                    .data(userVO)
                    .build();

        } else {
            return LoginVO.builder()
                    .token(null)
                    .refreshToken(null)
                    .code(403L)
                    .message("密码错误")
                    .build();
        }


    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return LoginVO.builder()
                    .code(401L)
                    .token(null)
                    .refreshToken(null)
                    .message("缺少 refreshToken")
                    .build();
        }

        String account = jwtUtil.extractUsernameFromRefreshToken(refreshToken);
        if (account == null) {
            return LoginVO.builder()
                    .code(401L)
                    .token(null)
                    .refreshToken(null)
                    .message("refreshToken 无效或已过期")
                    .build();
        }

        UserEntity user = userRepository.findByAccount(account);
        if (user == null) {
            return LoginVO.builder()
                    .code(401L)
                    .token(null)
                    .refreshToken(null)
                    .message("用户不存在")
                    .build();
        }

        String newAccessToken = jwtUtil.generateAccessToken(account);
        String newRefreshToken = jwtUtil.generateRefreshToken(account);
        return LoginVO.builder()
                .code(200L)
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresIn(jwtUtil.getAccessTokenExpiresInSeconds())
                .message("刷新成功")
                .build();
    }

    @Override
    public WalletAccountVO getMyWallet() {
        UserEntity user = getCurrentUserEntity();
        if (user == null) {
            return null;
        }
        WalletAccountEntity wallet = createWalletIfAbsent(user);
        return toWalletVO(wallet);
    }

    @Override
    @Transactional
    public WalletChangeResultVO rechargeMyWallet(WalletAmountChangeRequestDTO requestDTO) {
        UserEntity user = getCurrentUserEntity();
        if (user == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        BigDecimal amount = normalizeAmount(requestDTO);
        WalletAccountEntity wallet = walletAccountRepository.findByUserIdForUpdate(user.getId());
        if (wallet == null) {
            wallet = createWalletIfAbsent(user);
            wallet = walletAccountRepository.findByUserIdForUpdate(user.getId());
        }

        String businessNo = resolveBusinessNo(requestDTO);
        BigDecimal beforeBalance = wallet.getBalance();
        BigDecimal afterBalance = beforeBalance.add(amount).setScale(2);
        wallet.setBalance(afterBalance);
        WalletAccountEntity saved = walletAccountRepository.save(wallet);
        WalletFlowEntity flow = saveWalletFlow(saved, "RECHARGE", businessNo, requestDTO, amount, beforeBalance, afterBalance);

        return buildWalletChangeResult("RECHARGE", businessNo, requestDTO, amount, beforeBalance, afterBalance, saved, flow);
    }

    @Override
    @Transactional
    public WalletChangeResultVO consumeMyWallet(WalletAmountChangeRequestDTO requestDTO) {
        UserEntity user = getCurrentUserEntity();
        if (user == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        BigDecimal amount = normalizeAmount(requestDTO);
        WalletAccountEntity wallet = walletAccountRepository.findByUserIdForUpdate(user.getId());
        if (wallet == null) {
            wallet = createWalletIfAbsent(user);
            wallet = walletAccountRepository.findByUserIdForUpdate(user.getId());
        }

        String businessNo = resolveBusinessNo(requestDTO);
        BigDecimal beforeBalance = wallet.getBalance();
        if (beforeBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("钱包余额不足");
        }
        BigDecimal afterBalance = beforeBalance.subtract(amount).setScale(2);
        wallet.setBalance(afterBalance);
        WalletAccountEntity saved = walletAccountRepository.save(wallet);
        WalletFlowEntity flow = saveWalletFlow(saved, "CONSUME", businessNo, requestDTO, amount, beforeBalance, afterBalance);

        return buildWalletChangeResult("CONSUME", businessNo, requestDTO, amount, beforeBalance, afterBalance, saved, flow);
    }

    @Override
    public PageResultVO<WalletFlowItemVO> listMyWalletFlows(Integer page, Integer size, String changeType) {
        UserEntity user = getCurrentUserEntity();
        if (user == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        String normalizedType = normalizeChangeType(changeType);

        Pageable pageable = PageRequest.of(normalizedPage - 1, normalizedSize);
        Page<WalletFlowEntity> resultPage;
        if (normalizedType == null) {
            resultPage = walletFlowRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        } else {
            resultPage = walletFlowRepository.findByUserIdAndChangeTypeOrderByCreatedAtDesc(user.getId(), normalizedType, pageable);
        }

        List<WalletFlowItemVO> records = resultPage.getContent().stream().map(this::toWalletFlowItemVO).toList();
        return PageResultVO.<WalletFlowItemVO>builder()
                .records(records)
                .page(normalizedPage)
                .size(normalizedSize)
                .total(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .hasMore(normalizedPage < resultPage.getTotalPages())
                .build();
    }

    private WalletChangeResultVO buildWalletChangeResult(String changeType,
                                                         String businessNo,
                                                         WalletAmountChangeRequestDTO requestDTO,
                                                         BigDecimal amount,
                                                         BigDecimal beforeBalance,
                                                         BigDecimal afterBalance,
                                                         WalletAccountEntity wallet,
                                                         WalletFlowEntity flow) {
        WalletChangeResultVO result = new WalletChangeResultVO();
        result.setChangeType(changeType);
        result.setAmount(amount);
        result.setBeforeBalance(beforeBalance);
        result.setAfterBalance(afterBalance);
        result.setBusinessNo(businessNo);
        result.setRemark(requestDTO == null ? null : requestDTO.getRemark());
        result.setChangeTime(flow.getCreatedAt());
        result.setWallet(toWalletVO(wallet));
        return result;
    }

    private WalletFlowEntity saveWalletFlow(WalletAccountEntity wallet,
                                            String changeType,
                                            String businessNo,
                                            WalletAmountChangeRequestDTO requestDTO,
                                            BigDecimal amount,
                                            BigDecimal beforeBalance,
                                            BigDecimal afterBalance) {
        WalletFlowEntity flow = new WalletFlowEntity();
        flow.setWalletId(wallet.getId());
        flow.setWalletNo(wallet.getWalletNo());
        flow.setUserId(wallet.getUserId());
        flow.setBusinessNo(businessNo);
        flow.setChangeType(changeType);
        flow.setAmount(amount);
        flow.setBeforeBalance(beforeBalance);
        flow.setAfterBalance(afterBalance);
        flow.setRemark(requestDTO == null ? null : requestDTO.getRemark());
        return walletFlowRepository.save(flow);
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            throw new IllegalArgumentException("page 必须从 1 开始");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return 20;
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size 必须在 1 到 100 之间");
        }
        return size;
    }

    private String normalizeChangeType(String changeType) {
        if (changeType == null || changeType.isBlank()) {
            return null;
        }
        String normalized = changeType.trim().toUpperCase(Locale.ROOT);
        if (!"RECHARGE".equals(normalized) && !"CONSUME".equals(normalized)) {
            throw new IllegalArgumentException("changeType 仅支持 RECHARGE/CONSUME");
        }
        return normalized;
    }

    private BigDecimal normalizeAmount(WalletAmountChangeRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getAmount() == null) {
            throw new IllegalArgumentException("amount 不能为空");
        }
        if (requestDTO.getAmount().scale() > 2) {
            throw new IllegalArgumentException("amount 最多保留 2 位小数");
        }
        BigDecimal amount = requestDTO.getAmount().setScale(2, RoundingMode.UNNECESSARY);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount 必须大于 0");
        }
        return amount;
    }

    private String resolveBusinessNo(WalletAmountChangeRequestDTO requestDTO) {
        if (requestDTO != null && requestDTO.getBusinessNo() != null && !requestDTO.getBusinessNo().isBlank()) {
            return requestDTO.getBusinessNo();
        }
        return "BIZ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private WalletAccountEntity createWalletIfAbsent(UserEntity user) {
        WalletAccountEntity existed = walletAccountRepository.findByUserId(user.getId());
        if (existed != null) {
            return existed;
        }
        WalletAccountEntity wallet = new WalletAccountEntity();
        wallet.setUserId(user.getId());
        wallet.setWalletNo("W" + user.getAccount());
        wallet.setBalance(BigDecimal.ZERO.setScale(2));
        wallet.setCurrency("CNY");
        wallet.setStatus("ACTIVE");
        return walletAccountRepository.save(wallet);
    }

    private WalletAccountVO toWalletVO(WalletAccountEntity wallet) {
        WalletAccountVO vo = new WalletAccountVO();
        vo.setWalletNo(wallet.getWalletNo());
        vo.setBalance(wallet.getBalance());
        vo.setCurrency(wallet.getCurrency());
        vo.setStatus(wallet.getStatus());
        vo.setUpdatedAt(wallet.getUpdatedAt());
        return vo;
    }

    private WalletFlowItemVO toWalletFlowItemVO(WalletFlowEntity flow) {
        WalletFlowItemVO vo = new WalletFlowItemVO();
        vo.setWalletNo(flow.getWalletNo());
        vo.setBusinessNo(flow.getBusinessNo());
        vo.setChangeType(flow.getChangeType());
        vo.setAmount(flow.getAmount());
        vo.setBeforeBalance(flow.getBeforeBalance());
        vo.setAfterBalance(flow.getAfterBalance());
        vo.setRemark(flow.getRemark());
        vo.setCreatedAt(flow.getCreatedAt());
        return vo;
    }
}
