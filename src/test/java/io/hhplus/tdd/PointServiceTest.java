package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.model.PointHistory;
import io.hhplus.tdd.point.service.PointService;
import io.hhplus.tdd.point.model.TransactionType;
import io.hhplus.tdd.point.model.UserPoint;
import io.hhplus.tdd.point.validator.UserPointValidator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 단위 테스트
 * */
@ExtendWith(MockitoExtension.class)
public class PointServiceTest {
    @Mock
    private UserPointTable userPointRepository;
    @Mock
    private PointHistoryTable pointHistoryRepository;
    private PointService pointService;

    private static Stream<Arguments> provideAmountsForValidation() {
        return Stream.of(
            Arguments.of(1L, true),
            Arguments.of(10000L, true),
            Arguments.of(0L, false),
            Arguments.of(-100L, false)
        );
    }

    private static Stream<Arguments> provideValidChargeCases() {
        return Stream.of(
            Arguments.of(500L),
            Arguments.of(1000L),
            Arguments.of(100000L)
        );
    }

    private static Stream<Arguments> provideInvalidChargeCases() {
        return Stream.of(
            Arguments.of(-1000L),
            Arguments.of(0L)
        );
    }

    private static Stream<Arguments> provideValidUseCases() {
        return Stream.of(
            Arguments.of(1000L, 500L),
            Arguments.of(1000L, 1000L)
        );
    }

    private static Stream<Arguments> provideInvalidUseCases() {
        return Stream.of(
            Arguments.of(1000L, 1500L),
            Arguments.of(0L, 1000L)
        );
    }

    private static Stream<Arguments> provideInvalidPointAmountUseCases() {
        return Stream.of(
            Arguments.of(-1000L),
            Arguments.of(0L)
        );
    }

    @BeforeEach
    void setUp(){
        pointService = new PointService(userPointRepository, pointHistoryRepository);
    }

    @Test
    @DisplayName("특정 유저의 포인트를 조회할 수 있다")
    void point(){
        // given - 테스트 데이터 준비, 목업 동작 설정
        long userId = 1L;
        UserPoint mockUserPoint = UserPoint.empty(userId);
        when(userPointRepository.selectById(userId)).thenReturn(mockUserPoint);

        // when - 포인트 조회 수행
        UserPoint userPoint = pointService.point(userId);

        // then - 결과 검증
        assertNotNull(userPoint);
        assertEquals(userId, userPoint.id());
    }

    @Test
    @DisplayName("특정 유저의 포인트 내역을 조회할 수 있다")
    void pointHistory() {
        // given
        long userId = 1L;
        List<PointHistory> mockPointHistories = List.of(
            new PointHistory(1L, userId, 1000L, TransactionType.CHARGE,
                System.currentTimeMillis()),
            new PointHistory(2L, userId, 500L, TransactionType.USE,
                System.currentTimeMillis())
        );
        when(pointHistoryRepository.selectAllByUserId(userId)).thenReturn(mockPointHistories);

        // when - 포인트 내역 조회 수행
        List<PointHistory> pointHistories = pointService.pointHistory(userId);

        // then
        assertNotNull(pointHistories);
        assertEquals(mockPointHistories.size(), pointHistories.size());
    }

    @ParameterizedTest
    @MethodSource("provideAmountsForValidation")
    @DisplayName("유효한 포인트 금액만 충전이 가능하다")
    void add_point_pass(long amount, boolean expected){
        boolean canAdd = UserPointValidator.canAdd(amount);
        assertEquals(expected, canAdd);
    }

    @ParameterizedTest
    @MethodSource("provideValidChargeCases")
    @DisplayName("특정 유저의 포인트를 충전할 수 있다")
    void charge_pass(long requestAmount){
        // given - 테스트 데이터 준비, 목업 동작 설정
        long userId = 1L;
        long beforeUpdateAmount = 0L;
        long updatedAmount = beforeUpdateAmount + requestAmount;

            // 특정 유저의 포인트 정보 목업 준비
        UserPoint beforeMockUserPoint = new UserPoint(userId, beforeUpdateAmount, System.currentTimeMillis());
        UserPoint updatedMockUserPoint = new UserPoint(userId, updatedAmount, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(beforeMockUserPoint);
        when(userPointRepository.insertOrUpdate(userId, updatedAmount)).thenReturn(updatedMockUserPoint);

        // when - 포인트 충전 수행
        UserPoint userPoint = pointService.charge(userId, requestAmount);

        // then - 결과 검증
        assertNotNull(userPoint);
        assertEquals(userId, userPoint.id());
        assertEquals(updatedAmount, userPoint.point());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidChargeCases")
    @DisplayName("특정 유저의 포인트 충전 시 0 이하 금액은 예외가 발생한다")
    void charge_fail(long requestAmount){
        // given - 테스트 데이터 준비
        long userId = 1L;

        // then - 0 이하 금액 포인트 충전 시도 예외 검증
        assertThrows(IllegalArgumentException.class, () -> pointService.charge(userId, requestAmount));
    }

    @ParameterizedTest
    @MethodSource("provideValidChargeCases")
    @DisplayName("특정 유저의 포인트 충전 성공 시 포인트 내역이 기록된다")
    void charge_pass_after_pointHistory_insert_pass(long requestAmount){
        // given - 테스트 데이터 준비, 목업 동작 설정
        long userId = 1L;
        long beforeUpdateAmount = 0L;
        long updatedAmount = beforeUpdateAmount + requestAmount;

            // 특정 유저의 포인트 정보 목업 준비
        UserPoint beforeMockUserPoint = new UserPoint(userId, beforeUpdateAmount, System.currentTimeMillis());
        UserPoint updatedMockUserPoint = new UserPoint(userId, updatedAmount, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(beforeMockUserPoint);
        when(userPointRepository.insertOrUpdate(userId, updatedAmount)).thenReturn(updatedMockUserPoint);

        // when - 포인트 충전 수행
        UserPoint userPoint = pointService.charge(userId, requestAmount);

        // then - 결과 검증
        assertNotNull(userPoint);
        assertEquals(userId, userPoint.id());
        assertEquals(updatedAmount, userPoint.point());
        verify(pointHistoryRepository).insert(eq(userId), eq(requestAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @ParameterizedTest
    @MethodSource("provideAmountsForValidation")
    @DisplayName("유효한 포인트 금액만 사용 가능하다")
    void use_point_pass(long amount, boolean expected){
        boolean canUse = UserPointValidator.canUse(amount);
        assertEquals(expected, canUse);
    }

    @ParameterizedTest
    @MethodSource("provideValidUseCases")
    @DisplayName("특정 유저의 포인트를 사용할 수 있다")
    void use_pass(long beforeUpdateAmount, long requestAmount){
        // given - 테스트 데이터 준비, 목업 동작 설정
        long userId = 1L;
        long updatedAmount = beforeUpdateAmount - requestAmount;

        // 특정 유저의 포인트 정보 목업 준비
        UserPoint beforeMockUserPoint = new UserPoint(userId, beforeUpdateAmount, System.currentTimeMillis());
        UserPoint updatedMockUserPoint = new UserPoint(userId, updatedAmount, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(beforeMockUserPoint);
        when(userPointRepository.insertOrUpdate(userId, updatedAmount)).thenReturn(updatedMockUserPoint);

        // when - 포인트 사용 수행
        UserPoint userPoint = pointService.use(userId, requestAmount);

        // then - 결과 검증
        assertNotNull(userPoint);
        assertEquals(userId, userPoint.id());
        assertEquals(updatedAmount, userPoint.point());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidPointAmountUseCases")
    @DisplayName("특정 유저의 포인트 사용 시 0 이하 금액은 예외가 발생한다")
    void use_fail(long requestAmount){
        // given - 테스트 데이터 준비
        long userId = 1L;

        // when & then - 0 이하 금액 포인트 사용 시도 예외 검증
        assertThrows(IllegalArgumentException.class, () -> pointService.use(userId, requestAmount));
    }

    @ParameterizedTest
    @MethodSource("provideValidUseCases")
    @DisplayName("특정 유저의 포인트 사용 성공 시 포인트 내역이 기록된다")
    void use_pass_after_pointHistory_insert_pass(long beforeUpdateAmount, long requestAmount){
        // given - 테스트 데이터 준비, 목업 동작 설정
        long userId = 1L;
        long updatedAmount = beforeUpdateAmount - requestAmount;

            // 특정 유저의 포인트 정보 목업 준비
        UserPoint beforeMockUserPoint = new UserPoint(userId, beforeUpdateAmount, System.currentTimeMillis());
        UserPoint updatedMockUserPoint = new UserPoint(userId, updatedAmount, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(beforeMockUserPoint);
        when(userPointRepository.insertOrUpdate(userId, updatedAmount)).thenReturn(updatedMockUserPoint);

        // when - 포인트 사용 수행
        UserPoint userPoint = pointService.use(userId, requestAmount);

        // then - 결과 검증
        assertNotNull(userPoint);
        assertEquals(userId, userPoint.id());
        assertEquals(updatedAmount, userPoint.point());
        verify(pointHistoryRepository).insert(eq(userId), eq(requestAmount), eq(TransactionType.USE), anyLong());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUseCases")
    @DisplayName("특정 유저의 포인트 사용 시 잔고 부족이면 사용 실패한다")
    void use_fail_insufficient_balance(long beforeUpdateAmount, long requestAmount){
        // given - 테스트 데이터 준비, 목업 동작 설정
        long userId = 1L;

            // 특정 유저의 포인트 정보 목업 준비
        UserPoint beforeMockUserPoint = new UserPoint(userId, beforeUpdateAmount, System.currentTimeMillis());
        when(userPointRepository.selectById(userId)).thenReturn(beforeMockUserPoint);

        // when & then - 포인트 사용 수행 후 잔고 부족 에러 검증
        assertThrows(IllegalArgumentException.class, () -> pointService.use(userId, requestAmount));
    }
}
