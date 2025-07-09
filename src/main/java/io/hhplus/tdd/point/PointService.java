package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
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

    public UserPoint charge(long id, long amount){
        if (!UserPointValidator.canAdd(amount)){
            throw new IllegalArgumentException(ExceptionCode.INVALID_AMOUNT.message());
        }
        UserPoint beforeUserPoint = userPointRepository.selectById(id);
        long requestAmount = beforeUserPoint.point() + amount;

        UserPoint updatedUserPoint = userPointRepository.insertOrUpdate(id, requestAmount);
        pointHistoryRepository.insert(updatedUserPoint.id(), amount, TransactionType.CHARGE, System.currentTimeMillis());

        return updatedUserPoint;
    }



}
