package com.techmarket.api.service.impl;

import com.techmarket.api.config.SecurityConstant;
import com.techmarket.api.constant.UserBaseConstant;
import com.techmarket.api.jwt.UserBaseJwt;
import com.techmarket.api.model.Account;
import com.techmarket.api.repository.AccountRepository;
import com.techmarket.api.repository.GroupRepository;
import com.techmarket.api.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service(value = "userService")
@Slf4j
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) {
        Account user = accountRepository.findAccountByUsername(userId);
        if (user == null) {
            log.error("Invalid username or password.");
            throw new UsernameNotFoundException("Invalid username or password.");
        }
        boolean enabled = true;
        if (user.getStatus() != 1) {
            log.error("User had been locked");
            enabled = false;
        }
        Set<GrantedAuthority> grantedAuthorities = getAccountPermission(user);
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), enabled, true, true, true, grantedAuthorities);
    }

    private Set<GrantedAuthority> getAccountPermission(Account user){
        List<String> roles = new ArrayList<>();
        user.getGroup().getPermissions().stream().filter(f -> f.getPCode() != null).forEach( pName -> roles.add(pName.getPCode()));
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())).collect(Collectors.toSet());
    }

    public OAuth2AccessToken getAccessTokenForCustomType(ClientDetails client, TokenRequest tokenRequest, String username, String password, AuthorizationServerTokenServices tokenServices) throws GeneralSecurityException, IOException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("grantType", SecurityConstant.GRANT_TYPE_PASSWORD);
        String clientId = client.getClientId();
        boolean approved = true;
        Set<String> responseTypes = new HashSet<>();
        responseTypes.add("code");
        Map<String, Serializable> extensionProperties = new HashMap<>();

        UserDetails userDetails = loadUserByUsername(username);
        OAuth2Request oAuth2Request = new OAuth2Request(requestParameters, clientId,
                userDetails.getAuthorities(), approved, client.getScope(),
                client.getResourceIds(), null, responseTypes, extensionProperties);
        org.springframework.security.core.userdetails.User userPrincipal = new org.springframework.security.core.userdetails.User(userDetails.getUsername(), userDetails.getPassword(), userDetails.isEnabled(), userDetails.isAccountNonExpired(), userDetails.isCredentialsNonExpired(), userDetails.isAccountNonLocked(), userDetails.getAuthorities());
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userPrincipal, null, userDetails.getAuthorities());
        OAuth2Authentication auth = new OAuth2Authentication(oAuth2Request, authenticationToken);
        return tokenServices.createAccessToken(auth);
    }

    public OAuth2AccessToken getAccessTokenForUser(ClientDetails client, TokenRequest tokenRequest, String password, String phone, AuthorizationServerTokenServices tokenServices) throws GeneralSecurityException, IOException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("grantType", SecurityConstant.GRANT_TYPE_USER);

        String clientId = client.getClientId();
        boolean approved = true;
        Set<String> responseTypes = new HashSet<>();
        responseTypes.add("code");
        Map<String, Serializable> extensionProperties = new HashMap<>();

        Account user = accountRepository.findAccountByPhone(phone);
        boolean enabled = true;
        try {
            if (user!=null && user.getStatus()!=1) {
                log.error("User had been locked");
                enabled = false;
                throw new UsernameNotFoundException("account has not been activated");
            }
        }catch (UsernameNotFoundException e)
        {
            OAuth2AccessToken invalidPasswordToken = createErrorToken("tk has not been activated", "Tài khoản đã bị khóa hoặc chưa được kích hoạt");
            return invalidPasswordToken;
        }
        try {
            if(user == null){
                log.error("Invalid phone.");
                throw new UsernameNotFoundException("Invalid phone.");
            }else if (!passwordEncoder.matches(password, user.getPassword())) {
                log.error("Invalid password.");
                throw new UsernameNotFoundException("Invalid password.");
            }

        } catch (UsernameNotFoundException e) {
            OAuth2AccessToken invalidPasswordToken = createErrorToken("invalid_password or invalid_phone", "Mật khẩu hoặc tài khoản không hợp lệ");
            return invalidPasswordToken;
        }


        requestParameters.put("phone", user.getPhone());

        Set<GrantedAuthority> grantedAuthorities = getAccountPermission(user);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getPhone(), user.getPassword(), enabled, true, true, true, grantedAuthorities);

        OAuth2Request oAuth2Request = new OAuth2Request(requestParameters, clientId,
                userDetails.getAuthorities(), approved, client.getScope(),
                client.getResourceIds(), null, responseTypes, extensionProperties);
        org.springframework.security.core.userdetails.User userPrincipal = new org.springframework.security.core.userdetails.User(userDetails.getUsername(), userDetails.getPassword(), userDetails.isEnabled(), userDetails.isAccountNonExpired(), userDetails.isCredentialsNonExpired(), userDetails.isAccountNonLocked(), userDetails.getAuthorities());
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userPrincipal, null, userDetails.getAuthorities());
        OAuth2Authentication auth = new OAuth2Authentication(oAuth2Request, authenticationToken);
        return tokenServices.createAccessToken(auth);
    }

    public UserBaseJwt getAddInfoFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            OAuth2AuthenticationDetails oauthDetails =
                    (OAuth2AuthenticationDetails) authentication.getDetails();
            if (oauthDetails != null) {
                Map<String, Object> map = (Map<String, Object>) oauthDetails.getDecodedDetails();
                String encodedData = (String) map.get("additional_info");
                //idStr -> json
                if (encodedData != null && !encodedData.isEmpty()) {
                    return UserBaseJwt.decode(encodedData);
                }
                return null;
            }
        }
        return null;
    }
    private OAuth2AccessToken createErrorToken(String errorCode, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", errorCode);
        response.put("message", errorMessage);

        DefaultOAuth2AccessToken errorToken = new DefaultOAuth2AccessToken(" Invalid credentials");
        errorToken.setAdditionalInformation(response);

        return errorToken;
    }
}
