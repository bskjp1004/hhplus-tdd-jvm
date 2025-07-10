package io.hhplus.tdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.hhplus.tdd.exception.ExceptionCode;
import io.hhplus.tdd.point.model.PointHistory;
import io.hhplus.tdd.point.model.UserPoint;
import java.util.List;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * 포인트 인수테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PointAcceptanceTest {
    @Autowired
    TestRestTemplate restTemplate;

    @BeforeEach
    void setUp(){
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    @DisplayName("포인트 조회 요청 시 포인트 금액을 확인 할 수 있다")
    void get_point_pass(){
        // given
        long userId = 1L;
        long initialAmount = 0L;

        // when - 포인트 조회 api 호출
        ResponseEntity<UserPoint> getResponse = restTemplate
            .getForEntity("/point/" + userId, UserPoint.class);

        // then
        assertEquals(initialAmount, getResponse.getBody().point());
    }

    @Test
    @DisplayName("포인트 내역 조회 요청 시 포인트 내역을 확인 할 수 있다")
    void get_pointHistory_pass(){
        // given
        long userId = 1L;
        long requestAmount = 1000L;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Long> chargeRequest = new HttpEntity<>(requestAmount, headers);
        restTemplate.exchange("/point/" + userId + "/charge", HttpMethod.PATCH, chargeRequest, UserPoint.class);
        restTemplate.exchange("/point/" + userId + "/charge", HttpMethod.PATCH, chargeRequest, UserPoint.class);

        // when
        ResponseEntity<PointHistory[]> getResponse = restTemplate
            .getForEntity("/point/" + userId + "/histories", PointHistory[].class);

        // then
        List<Object> historyList = Arrays.asList(getResponse.getBody());
        assertEquals(2, historyList.size());
    }

    @Test
    @DisplayName("포인트 충전 후 조회하면 금액이 증가된다")
    void charge_point_pass(){
        // given
        long userId = 1L;
        long requestAmount = 1000L;

        // when - 포인트 충전 api 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Long> request = new HttpEntity<>(requestAmount, headers);
        ResponseEntity<UserPoint> chargeResponse = restTemplate
            .exchange("/point/" + userId + "/charge", HttpMethod.PATCH, request, UserPoint.class);

        // then - 충전 결과 확인
        assertEquals(HttpStatus.OK, chargeResponse.getStatusCode());
        assertEquals(userId, chargeResponse.getBody().id());
        assertEquals(requestAmount, chargeResponse.getBody().point());
            // 포인트 조회 api 호출
        ResponseEntity<UserPoint> getResponse = restTemplate
            .getForEntity("/point/" + userId, UserPoint.class);

        assertEquals(requestAmount, getResponse.getBody().point());
    }

    @Test
    @DisplayName("유효하지 않은 포인트 금액으로 포인트 충전 시 에러 코드를 받는다")
    void charge_point_fail(){
        // given
        long userId = 1L;
        long requestAmount = -500L;

        // when - 포인트 충전 api 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Long> request = new HttpEntity<>(requestAmount, headers);
        ResponseEntity<String> chargeResponse = restTemplate
            .exchange("/point/" + userId + "/charge", HttpMethod.PATCH, request, String.class);

        // then - 충전 결과 확인
        assertEquals(HttpStatus.BAD_REQUEST, chargeResponse.getStatusCode());
        assertTrue(chargeResponse.getBody().contains(ExceptionCode.INVALID_AMOUNT.message()));
            // 포인트 조회 api 호출
        ResponseEntity<UserPoint> getResponse = restTemplate
            .getForEntity("/point/" + userId, UserPoint.class);

        assertEquals(0, getResponse.getBody().point());
    }

    @Test
    @DisplayName("포인트 사용 후 조회하면 금액이 감소된다")
    void use_point_pass(){
        // given
        long userId = 1L;
        long initialAmount  = 1000L;
        long useAmount  = 1000L;

            // 포인트 사용 전 충전 api 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Long> chargeRequest = new HttpEntity<>(initialAmount, headers);
        ResponseEntity<UserPoint> chargeResponse = restTemplate
            .exchange("/point/" + userId + "/charge", HttpMethod.PATCH, chargeRequest, UserPoint.class);

        // when - 포인트 사용 api 호출
        HttpEntity<Long> useRequest = new HttpEntity<>(useAmount, headers);
        ResponseEntity<UserPoint> useResponse = restTemplate
            .exchange("/point/" + userId + "/use", HttpMethod.PATCH, useRequest, UserPoint.class);

        // then
        assertEquals(HttpStatus.OK, useResponse.getStatusCode());
        assertEquals(initialAmount - useAmount, useResponse.getBody().point());
    }

    @Test
    @DisplayName("유효하지 않은 포인트 금액 사용 시 에러 코드를 받는다")
    void use_point_fail_INVALID_AMOUNT(){
        // given
        long userId = 1L;
        long initialAmount  = 1000L;
        long useAmount  = -500L;

        // 포인트 사용 전 충전 api 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Long> chargeRequest = new HttpEntity<>(initialAmount, headers);
        ResponseEntity<UserPoint> chargeResponse = restTemplate
            .exchange("/point/" + userId + "/charge", HttpMethod.PATCH, chargeRequest, UserPoint.class);

        // when - 포인트 사용 api 호출
        HttpEntity<Long> useRequest = new HttpEntity<>(useAmount, headers);
        ResponseEntity<String> useResponse = restTemplate
            .exchange("/point/" + userId + "/use", HttpMethod.PATCH, useRequest, String.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, useResponse.getStatusCode());
        assertTrue(useResponse.getBody().contains(ExceptionCode.INVALID_AMOUNT.message()));
        // 포인트 조회 api 호출
        ResponseEntity<UserPoint> getResponse = restTemplate
            .getForEntity("/point/" + userId, UserPoint.class);

        assertEquals(initialAmount, getResponse.getBody().point());
    }

    @Test
    @DisplayName("잔고 부족 상태에서 포인트 사용 시 에러 코드를 받는다")
    void use_point_fail_insufficient_balance(){
        // given
        long userId = 1L;
        long initialAmount  = 500L;
        long useAmount  = 1000L;

        // 포인트 사용 전 충전 api 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Long> chargeRequest = new HttpEntity<>(initialAmount, headers);
        ResponseEntity<UserPoint> chargeResponse = restTemplate
            .exchange("/point/" + userId + "/charge", HttpMethod.PATCH, chargeRequest, UserPoint.class);

        // when - 포인트 사용 api 호출
        HttpEntity<Long> useRequest = new HttpEntity<>(useAmount, headers);
        ResponseEntity<String> useResponse = restTemplate
            .exchange("/point/" + userId + "/use", HttpMethod.PATCH, useRequest, String.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, useResponse.getStatusCode());
        assertTrue(useResponse.getBody().contains(ExceptionCode.INSUFFICIENT_BALANCE.message()));
        // 포인트 조회 api 호출
        ResponseEntity<UserPoint> getResponse = restTemplate
            .getForEntity("/point/" + userId, UserPoint.class);

        assertEquals(initialAmount, getResponse.getBody().point());
    }
}
