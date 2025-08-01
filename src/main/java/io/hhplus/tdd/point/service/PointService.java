package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.ExceptionCode;
import io.hhplus.tdd.point.model.TransactionType;
import io.hhplus.tdd.point.validator.UserPointValidator;
import io.hhplus.tdd.point.model.PointHistory;
import io.hhplus.tdd.point.model.UserPoint;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private final UserPointTable userPointRepository;
    private final PointHistoryTable pointHistoryRepository;

    public UserPoint point(long id){
        return userPointRepository.selectById(id);
    }

    public List<PointHistory> pointHistory(long userId) { return pointHistoryRepository.selectAllByUserId(userId); }

    public UserPoint charge(long id, long amount){
        if (!UserPointValidator.isValidRequestAmount(amount)){
            throw new IllegalArgumentException(ExceptionCode.INVALID_AMOUNT.message());
        }

        UserPoint beforeUserPoint = userPointRepository.selectById(id);

        if (!UserPointValidator.canAdd(beforeUserPoint.point(), amount)){
            throw new IllegalArgumentException(ExceptionCode.EXCEED_MAX_BALANCE.message());
        }

        long requestAmount = beforeUserPoint.point() + amount;
        UserPoint updatedUserPoint = userPointRepository.insertOrUpdate(id, requestAmount);
        pointHistoryRepository.insert(updatedUserPoint.id(), amount, TransactionType.CHARGE, System.currentTimeMillis());

        return updatedUserPoint;
    }

    public UserPoint use(long id, long amount){
        if (!UserPointValidator.canUse(amount)){
            throw new IllegalArgumentException(ExceptionCode.INVALID_AMOUNT.message());
        }

        UserPoint beforeUserPoint = userPointRepository.selectById(id);

        if (beforeUserPoint.point() < amount){
            throw new IllegalArgumentException(ExceptionCode.INSUFFICIENT_BALANCE.message());
        }

        long requestAmount = beforeUserPoint.point() - amount;
        UserPoint updatedUserPoint = userPointRepository.insertOrUpdate(id, requestAmount);
        pointHistoryRepository.insert(updatedUserPoint.id(), amount, TransactionType.USE, System.currentTimeMillis());

        return updatedUserPoint;
    }

}
