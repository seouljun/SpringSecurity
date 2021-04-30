package net.bitnine.jwtsample.controller;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.bitnine.jwtsample.domain.AuthRequest;
import net.bitnine.jwtsample.util.cookie.CookieUtil;
import net.bitnine.jwtsample.util.jwt.JwtUtil;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class JwtController {

  private AuthenticationManager authenticationManager;
  private JwtUtil jwtUtil;
  private CookieUtil cookieUtil;
  private StringRedisTemplate redisTemplate;

  @Autowired
  public JwtController(
      AuthenticationManager authenticationManager, JwtUtil jwtUtil,
      CookieUtil cookieUtil, StringRedisTemplate redisTemplate) {
    this.authenticationManager = authenticationManager;
    this.jwtUtil = jwtUtil;
    this.cookieUtil = cookieUtil;
    this.redisTemplate = redisTemplate;
  }

  @GetMapping("/a")
  public String welcome() {
    return "hi";
  }

  /**
   * ACCESS Token , REFRESH Token(구현예정)
   * @param authRequest ID / PWD
   * @param response    Cookie 저장
   * @return 로그인 성공 화면
   */
  @PostMapping("/authenticate")
  public String generateToken(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(authRequest.getUserName(),
            authRequest.getPassword())
    );

    // CREATE TOKEN
    String accessToken = jwtUtil.generateToken(authRequest.getUserName(), jwtUtil.getACCESS_TOKEN());
    String refreshToken = jwtUtil.generateToken(authRequest.getUserName(), jwtUtil.getREFRESH_TOKEN());

    // SPLIT TOKEN
    String[] jwtToken = accessToken.split("\\.");

    // CREATE COOKIE
    Cookie hp = cookieUtil.createCookie("Header_Payload", jwtToken[0] + "." + jwtToken[1] + ".");
    Cookie signature = cookieUtil.createCookie("Signature", jwtToken[2]);

    // SAVE TOKEN
    saveRefreshToken(authRequest.getUserName(), refreshToken);
    response.addCookie(hp);
    response.addCookie(signature);

    return "/a";
  }
  public void saveRefreshToken(String userName,String token){
    SetOperations<String, String> redisSetOp =  redisTemplate.opsForSet();
    redisSetOp.add(userName,token);
  }

}
